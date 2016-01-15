package explicit.conditional.transformer.mdp;

import java.util.BitSet;

import common.BitSetTools;

import acceptance.AcceptanceStreett;
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
import explicit.conditional.transformer.LTLProductTransformer;
import explicit.conditional.transformer.mdp.GoalStopTransformer.GoalStopTransformation;
import explicit.conditional.transformer.mdp.MDPResetTransformer.ResetTransformation;

// FIXME ALG: prove correctness of transformation
public class MDPLTLObjectiveTransformer extends MDPConditionalTransformer
{
	private final LTLProductTransformer<MDP> ltlTransformer;
	private LTLModelChecker ltlModelChecker;

	public MDPLTLObjectiveTransformer(final MDPModelChecker modelChecker) throws PrismException
	{
		super(modelChecker);
		ltlTransformer = new LTLProductTransformer<>(modelChecker);
		ltlModelChecker = new LTLModelChecker(this);
	}

	@Override
	protected boolean canHandleCondition(final MDP model, final ExpressionConditional expression)
	{
		final Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	protected boolean canHandleObjective(final MDP model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!super.canHandleObjective(model, expression)) {
			return false;
		}
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		return ltlTransformer.canHandle(model, objective.getExpression());
	}

	@Override
	public ConditionalMDPTransformation transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		checkCanHandle(model, expression);
		MDPResetTransformer.checkStatesOfInterest(statesOfInterest);

		// 1) Condition: compute "condition goal states"
		final Expression conditionGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(expression.getCondition())).getOperand2();
		final BitSet conditionGoalStates = modelChecker.checkExpression(model, conditionGoal, null).getBitSet();

		// 2) Objective: extract ltl path formula
		final ExpressionProb objective = ((ExpressionProb) expression.getObjective());

		return transform(model, objective, conditionGoalStates, statesOfInterest);
	}

	protected ConditionalMDPTransformation transform(final MDP model, final ExpressionProb objective, final BitSet conditionGoalStates,
			final BitSet statesOfInterest) throws PrismException
	{
		checkSatisfiability(model, conditionGoalStates, statesOfInterest);

		// 1) Objective: LTL Product Transformation
		final Expression objectivePath = objective.getExpression();
		final LTLProduct<MDP> objectiveProduct = ltlTransformer.transform(model, objectivePath, statesOfInterest, AcceptanceType.STREETT);
		final MDP objectiveModel = objectiveProduct.getProductModel();
		final BitSet conditionGoalStatesLifted = objectiveProduct.liftFromModel(conditionGoalStates);

		//    compute "objective goal states"
		// FIXME ALG: LTLModelChecker>>findAcceptingECStates should take the product as argument
		final AcceptanceStreett objectiveAcceptance = (AcceptanceStreett) objectiveProduct.getAcceptance();
		final BitSet objectiveGoalStates = ltlModelChecker.findAcceptingECStates(objectiveModel, objectiveAcceptance);

		// 2) Normal Form Transformation
		final GoalStopTransformer normalFormTransformer = new GoalStopTransformer(modelChecker);
		final GoalStopTransformation normalFormTransformation = normalFormTransformer.transformModel(objectiveModel, objectiveGoalStates,
				conditionGoalStatesLifted);

		//    compute "bad states" == {s | Pmin=0[<> (Cond)]}
		final BitSet badStates = modelChecker.prob0(objectiveModel, null, conditionGoalStatesLifted, true, null);

		//    reset transformation
		final BitSet objectiveStatesOfInterest = BitSetTools.asBitSet(objectiveModel.getInitialStates());
		final BitSet normalFormStatesOfInterest = normalFormTransformation.mapToTransformedModel(objectiveStatesOfInterest);
		final MDPResetTransformer resetTransformer = new MDPResetTransformer(modelChecker);
		final ResetTransformation<MDP> resetTransformation = resetTransformer.transformModel(normalFormTransformation.getTransformedModel(),
				badStates, normalFormStatesOfInterest);

		// FIXME ALG: consider restriction to part reachable from states of interest

		// 3) Create Mapping
		// FIXME ALG: consider ModelExpressionTransformationNested
		// FIXME ALG: refactor to remove tedious code duplication
		final Integer[] mapping = new Integer[model.getNumStates()];
		for (Integer productState : objectiveModel.getInitialStates()) {
			// get the state index of the corresponding state in the original model
			final Integer modelState = objectiveProduct.getModelState(productState);
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