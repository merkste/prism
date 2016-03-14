package explicit.conditional.transformer.legacy;

import java.util.BitSet;

import acceptance.AcceptanceStreett;
import acceptance.AcceptanceType;
import acceptance.AcceptanceStreett.StreettPair;
import common.BitSetTools;
import common.iterable.IterableBitSet;
import parser.State;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.PrismException;
import prism.PrismLangException;
import explicit.LTLModelChecker;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import explicit.ModelCheckerResult;
import explicit.LTLModelChecker.LTLProduct;
import explicit.conditional.transformer.LTLProductTransformer;
import explicit.conditional.transformer.ResetTransformer;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.conditional.transformer.mdp.ConditionalReachabilitiyTransformation;

@Deprecated
public class MDPLTLTransformer extends MDPConditionalTransformer
{
	private LTLProductTransformer<MDP> ltlTransformer;
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
	public ConditionalReachabilitiyTransformation<MDP, MDP> transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		ResetTransformer.checkStatesOfInterest(model, statesOfInterest);

		final Expression condition = expression.getCondition();
		final Expression objective = ((ExpressionProb) expression.getObjective()).getExpression();

		// 1. Product Transformation
		final LTLProduct<MDP> conditionProduct = ltlTransformer.transform(model, condition, statesOfInterest, AcceptanceType.STREETT);
		final MDP conditionModel = conditionProduct.getProductModel();

		final AcceptanceStreett conditionAcceptance = (AcceptanceStreett) conditionProduct.getAcceptance();
		final BitSet conditionStates = ltlModelChecker.findAcceptingECStates(conditionModel, conditionAcceptance);

		// check whether the condition is satisfiable in the state of interest
		assert conditionModel.getNumInitialStates() == 1 : "expected one and only one initial state";
		final BitSet noPathToCondition = modelChecker.prob0(conditionModel, null, conditionStates, false, null);
		if (noPathToCondition.get(conditionModel.getFirstInitialState())) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

		final BitSet conditionStatesOfInterest = BitSetTools.asBitSet(conditionModel.getInitialStates());
		final LTLProduct<MDP> objectiveAndConditionProduct = ltlTransformer.transform(conditionModel, objective, conditionStatesOfInterest,
				AcceptanceType.STREETT);
		assert objectiveAndConditionProduct.getProductModel().getNumInitialStates() == 1 : "expected one and only one initial state";

		// phi & psi Street acceptance
		final AcceptanceStreett conditionAcceptanceLifted = new AcceptanceStreett();
		for (StreettPair streettPair : conditionAcceptance) {
			BitSet R = objectiveAndConditionProduct.liftFromModel(streettPair.getR());
			BitSet G = objectiveAndConditionProduct.liftFromModel(streettPair.getG());
			conditionAcceptanceLifted.add(new StreettPair(R, G));
		}
		final AcceptanceStreett objectiveAndConditionAcceptance = new AcceptanceStreett();
		objectiveAndConditionAcceptance.addAll((AcceptanceStreett) objectiveAndConditionProduct.getAcceptance());
		objectiveAndConditionAcceptance.addAll(conditionAcceptanceLifted);

		// construct target set F
		// compute F aka "objective and condition states"
		final MDP objectiveAndConditionModel = objectiveAndConditionProduct.getProductModel();
		final BitSet objectiveAndConditionGoalStates = ltlModelChecker.findAcceptingECStates(objectiveAndConditionModel, objectiveAndConditionAcceptance);

		// compute B aka "bad states"
		final BitSet badEcStates = ltlModelChecker.findAcceptingECStates(objectiveAndConditionModel, conditionAcceptance.complementToRabin());
		final BitSet r_states = new BitSet();
		for (StreettPair streettPair : conditionAcceptance) {
			r_states.or(streettPair.getR());
		}
		final BitSet badStates = (BitSet) badEcStates.clone();
		badStates.and(r_states);

		// 2. Normal Form Transformation
		// P′(s,goal) = Prmax_M,s(ψ)
		// P′(s,fail) = 1−Prmax_M,s(ψ)

		// compute Pmax(<>E | C)
		final BitSet target = ltlModelChecker.findAcceptingECStates(objectiveAndConditionModel, conditionAcceptance);
		final ModelCheckerResult conditionMaxResult = modelChecker.computeReachProbs(objectiveAndConditionModel, target, false);
		double[] conditionMaxProbs = conditionMaxResult.soln;

		// copy MDP to new MDPSimple
		final MDPSimple transformedModel = new MDPSimple(objectiveAndConditionModel);
		final int resetState = objectiveAndConditionModel.getFirstInitialState();

		// insert states: goalState, failState
		final State init = transformedModel.getStatesList().get(resetState);
		final int goalState = transformedModel.addState();
		transformedModel.getStatesList().add(init);
		final int failState = transformedModel.addState();
		transformedModel.getStatesList().add(init);

		// redirect choices from objective goalState states to goalState or failState
		redirectChoices(transformedModel, objectiveAndConditionGoalStates, goalState, failState, conditionMaxProbs);

		// add failState choice from bad states to failState
		for (Integer state : new IterableBitSet(badStates)) {
			addDiracChoice(transformedModel, state, failState, "fail");
		}

		// add reset choice from failState state to initial state
		addDiracChoice(transformedModel, failState, resetState, "reset");

		// add self-loops to goalState,
		addDiracChoice(transformedModel, goalState, goalState, "goal_loop");

		final Integer[] mapping = new Integer[model.getNumStates()];
		for (Integer productState : objectiveAndConditionProduct.getProductModel().getInitialStates()) {
			// get the state index of the corresponding state in the original model
			final Integer modelState = conditionProduct.getModelState(objectiveAndConditionProduct.getModelState(productState));
			assert modelState != null : "first state should be set";
			assert mapping[modelState] == null : "do not map state twice";
			mapping[modelState] = productState;
		}

		final BitSet goalStates = new BitSet();
		goalStates.set(goalState);

		return new ConditionalReachabilitiyTransformation<MDP, MDP>(model, transformedModel, mapping, goalStates, BitSetTools.asBitSet(resetState));

	}
}