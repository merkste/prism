package explicit.conditional.transformer;

import java.util.BitSet;

import common.BitSetTools;
import common.iterable.MappingIterator;
import acceptance.AcceptanceStreett;
import acceptance.AcceptanceType;
import acceptance.AcceptanceStreett.StreettPair;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.PrismException;
import prism.PrismLangException;
import explicit.BasicModelTransformation;
import explicit.LTLModelChecker;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.conditional.LTLProductTransformer;
import explicit.conditional.UndefinedTransformationException;

public class MDPLTLTransformer extends MDPConditionalTransformer
{
	private final LTLProductTransformer<MDP> ltlTransformer;
	private LTLModelChecker ltlModelChecker;

	public MDPLTLTransformer(final MDPModelChecker modelChecker) throws PrismException
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
		return ltlTransformer.canHandle(model, objective.getExpression());
	}

	@Override
	public ConditionalMDPTransformation transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		checkStatesOfInterest(statesOfInterest);

		// 1) Condition: LTL Product Transformation
		final Expression condition = expression.getCondition();
		final LTLProduct<MDP> conditionProduct = ltlTransformer.transform(model, condition, statesOfInterest, AcceptanceType.STREETT);
		final MDP conditionModel = conditionProduct.getProductModel();

		//    compute Pmax(<>E | C)
		final AcceptanceStreett conditionAcceptance = (AcceptanceStreett) conditionProduct.getAcceptance();
		final BitSet conditionGoalStates = ltlModelChecker.findAcceptingECStates(conditionModel, conditionAcceptance);

		//    check whether the condition is satisfiable in the state of interest
		final BitSet conditionStatesOfInterest = BitSetTools.asBitSet(conditionModel.getInitialStates());
		assert conditionStatesOfInterest.cardinality() == 1 : "expected one and only one state of interest";
		final BitSet noPathToCondition = modelChecker.prob0(conditionModel, null, conditionGoalStates, false, null);
		if (!BitSetTools.areDisjoint(noPathToCondition, conditionStatesOfInterest)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

		// 2) Objective: LTL Product Transformation
		final Expression objective = ((ExpressionProb) expression.getObjective()).getExpression();
		final LTLProduct<MDP> objectiveAndConditionProduct = ltlTransformer.transform(conditionModel, objective, conditionStatesOfInterest,
				AcceptanceType.STREETT);

		//    phi & psi Street acceptance
		final AcceptanceStreett conditionAcceptanceLifted = new AcceptanceStreett();
		for (StreettPair streettPair : conditionAcceptance) {
			BitSet R = objectiveAndConditionProduct.liftFromModel(streettPair.getR());
			BitSet G = objectiveAndConditionProduct.liftFromModel(streettPair.getG());
			conditionAcceptanceLifted.add(new StreettPair(R, G));
		}
		final AcceptanceStreett objectiveAndConditionAcceptance = new AcceptanceStreett();
		objectiveAndConditionAcceptance.addAll((AcceptanceStreett) objectiveAndConditionProduct.getAcceptance());
		objectiveAndConditionAcceptance.addAll(conditionAcceptanceLifted);

		//    compute "objective and condition goal states"
		final MDP objectiveAndConditionModel = objectiveAndConditionProduct.getProductModel();
		final BitSet objectiveAndConditionGoalStates = ltlModelChecker.findAcceptingECStates(objectiveAndConditionModel, objectiveAndConditionAcceptance);

		// 3) Normal Form Transformation
		//    compute "bad states"
		final BitSet badStates = ltlModelChecker.findAcceptingECStates(objectiveAndConditionModel, conditionAcceptanceLifted.complementToRabin());
		final BitSet rStates = BitSetTools.union(new MappingIterator.From<>(conditionAcceptanceLifted, StreettPair::getR));
		badStates.and(rStates);

		//    reset transformation
		final BitSet objectiveAndConditionStatesOfInterest = BitSetTools.asBitSet(objectiveAndConditionModel.getInitialStates());
		assert objectiveAndConditionStatesOfInterest.cardinality() == 1 : "expected one and only one state of interest";
		final int targetState = objectiveAndConditionStatesOfInterest.nextSetBit(0);
		final MDPResetTransformer resetTransformer = new MDPResetTransformer(modelChecker);
		final BasicModelTransformation<MDP, MDP> resetTransformation = resetTransformer.transformModel(objectiveAndConditionModel, badStates, targetState);

		// FIXME ALG: consider restriction to part reachable from states of interest

		// 4) Create Mapping
		// FIXME ALG: consider ModelExpressionTransformationNested
		// FIXME ALG: refactor to remove tedious code duplication
		final Integer[] mapping = new Integer[model.getNumStates()];
		for (Integer productState : objectiveAndConditionModel.getInitialStates()) {
			// get the state index of the corresponding state in the original model
			final Integer modelState = conditionProduct.getModelState(objectiveAndConditionProduct.getModelState(productState));
			assert modelState != null : "first state should be set";
			assert mapping[modelState] == null : "do not map state twice";
			final Integer transformedState = resetTransformation.mapToTransformedModel(productState);
			mapping[modelState] = transformedState;
		}

		return new ConditionalMDPTransformation(model, resetTransformation.getTransformedModel(), mapping, objectiveAndConditionGoalStates);
	}
}