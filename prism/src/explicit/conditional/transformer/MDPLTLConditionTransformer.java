package explicit.conditional.transformer;

import java.util.BitSet;

import common.BitSetTools;
import common.iterable.MappingIterator;
import acceptance.AcceptanceStreett;
import acceptance.AcceptanceStreett.StreettPair;
import acceptance.AcceptanceType;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;
import explicit.LTLModelChecker;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.LTLProductTransformer;
import explicit.conditional.UndefinedTransformationException;
import explicit.conditional.transformer.GoalFailTransformer.GoalFailTransformation;
import explicit.conditional.transformer.MDPResetTransformer.ResetTransformation;

public class MDPLTLConditionTransformer extends MDPConditionalTransformer
{
	private final LTLProductTransformer<MDP> ltlTransformer;
	private LTLModelChecker ltlModelChecker;

	public MDPLTLConditionTransformer(final MDPModelChecker modelChecker) throws PrismException
	{
		super(modelChecker);
		ltlTransformer = new LTLProductTransformer<>(modelChecker);
		ltlModelChecker = new LTLModelChecker(this);
	}

	@Override
	protected boolean canHandleCondition(final MDP model, final ExpressionConditional expression) throws PrismLangException
	{
		return ltlTransformer.canHandle(model, expression.getCondition());
	}

	@Override
	protected boolean canHandleObjective(final MDP model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!super.canHandleObjective(model, expression)) {
			return false;
		}
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final Expression normalized = ExpressionInspector.normalizeExpression(objective.getExpression());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	public ConditionalMDPTransformation transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		checkStatesOfInterest(statesOfInterest);

		// 1) Objective: compute "objective goal states"
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final Expression objectiveGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objective.getExpression())).getOperand2();
		final BitSet objectiveGoalStates = modelChecker.checkExpression(model, objectiveGoal, null).getBitSet();

		// 2) Condition: extract ltl path formula
		final Expression condition = expression.getCondition();

		return transform(model, objectiveGoalStates, condition, statesOfInterest);
	}

	public ConditionalMDPTransformation transform(final MDP model, final BitSet objectiveGoalStates, final Expression condition, final BitSet statesOfInterest)
			throws PrismException
	{
		// FIXME consider moving checks outwards and inserting an assertion
		checkStatesOfInterest(statesOfInterest);

		// 1) Condition: LTL Product Transformation
		final LTLProduct<MDP> conditionProduct = ltlTransformer.transform(model, condition, statesOfInterest, AcceptanceType.STREETT);
		final MDP conditionModel = conditionProduct.getProductModel();

		//    compute "condition goal states"
		// FIXME ALG: LTLModelChecker>>findAcceptingECStates should take the product as argument
		final AcceptanceStreett conditionAcceptance = (AcceptanceStreett) conditionProduct.getAcceptance();
		final BitSet conditionGoalStates = ltlModelChecker.findAcceptingECStates(conditionModel, conditionAcceptance);

		//    check whether the condition is satisfiable in the state of interest
		final BitSet conditionStatesOfInterest = BitSetTools.asBitSet(conditionModel.getInitialStates());
		assert conditionStatesOfInterest.cardinality() == 1 : "expected one and only one state of interest";
		final BitSet noPathToCondition = modelChecker.prob0(conditionModel, null, conditionGoalStates, false, null);
		if (!BitSetTools.areDisjoint(noPathToCondition, conditionStatesOfInterest)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

		// 2) Objective: lift "condition goal states"
		final BitSet objectiveGoalStatesLifted = conditionProduct.liftFromModel(objectiveGoalStates);

		// 3) Normal Form Transformation
		final GoalFailTransformer normalFormTransformer = new GoalFailTransformer(modelChecker);
		final GoalFailTransformation normalFormTransformation = normalFormTransformer.transformModel(conditionModel, objectiveGoalStatesLifted,
				conditionGoalStates);

		//    compute "bad states"
		final BitSet badStates = ltlModelChecker.findAcceptingECStates(conditionModel, conditionAcceptance.complementToRabin());
		final BitSet rStates = BitSetTools.union(new MappingIterator.From<>(conditionAcceptance, StreettPair::getR));
		badStates.and(rStates);
		badStates.set(normalFormTransformation.getFailState());

		//    reset transformation
		final BitSet normalFormStatesOfInterest = normalFormTransformation.mapToTransformedModel(conditionStatesOfInterest);
		assert normalFormStatesOfInterest.cardinality() == 1 : "expected one and only one state of interest";
		final int targetState = normalFormStatesOfInterest.nextSetBit(0);
		final MDPResetTransformer resetTransformer = new MDPResetTransformer(modelChecker);
		final ResetTransformation<MDP> resetTransformation = resetTransformer.transformModel(normalFormTransformation.getTransformedModel(),
				badStates, targetState);

		// FIXME ALG: consider restriction to part reachable from states of interest

		// 4) Create Mapping
		// FIXME ALG: consider ModelExpressionTransformationNested
		// FIXME ALG: refactor to remove tedious code duplication
		final Integer[] mapping = new Integer[model.getNumStates()];
		for (Integer productState : conditionModel.getInitialStates()) {
			// get the state index of the corresponding state in the original model
			final Integer modelState = conditionProduct.getModelState(productState);
			assert modelState != null : "first state should be set";
			assert mapping[modelState] == null : "do not map state twice";
			final Integer normalFormState = normalFormTransformation.mapToTransformedModel(productState);
			final Integer resetState = resetTransformation.mapToTransformedModel(normalFormState);
			mapping[modelState] = resetState;
		}

		final int goalState = normalFormTransformation.getGoalState();
		final BitSet goalStates = BitSetTools.asBitSet(goalState);

		return new ConditionalMDPTransformation(model, resetTransformation.getTransformedModel(), mapping, goalStates);
	}
}