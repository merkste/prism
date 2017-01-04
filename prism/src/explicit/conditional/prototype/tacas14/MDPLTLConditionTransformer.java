package explicit.conditional.prototype.tacas14;

import java.util.BitSet;

import parser.State;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;
import acceptance.AcceptanceStreett;
import acceptance.AcceptanceStreett.StreettPair;
import acceptance.AcceptanceType;
import common.BitSetTools;
import common.iterable.IterableBitSet;
import explicit.LTLModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import explicit.Model;
import explicit.ModelCheckerResult;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.prototype.ConditionalReachabilitiyTransformation;
import explicit.conditional.transformer.LTLProductTransformer;
import explicit.conditional.transformer.ResetTransformer;
import explicit.conditional.transformer.UndefinedTransformationException;

@Deprecated
public class MDPLTLConditionTransformer extends MDPConditionalTransformer
{
	protected LTLProductTransformer<explicit.MDP> ltlTransformer;

	public MDPLTLConditionTransformer(final MDPModelChecker modelChecker) throws PrismException
	{
		super(modelChecker);
		ltlTransformer = new LTLProductTransformer<>(modelChecker);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		return ltlTransformer.canHandle(model, expression.getCondition());
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!super.canHandleObjective(model, expression)) {
			return false;
		}
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final Expression normalized = ExpressionInspector.normalizeExpression(objective.getExpression());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	public ConditionalReachabilitiyTransformation<explicit.MDP, explicit.MDP> transformReachability(final explicit.MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		ResetTransformer.checkStatesOfInterest(model, statesOfInterest);

		// 1. Product Transformation
		final Expression condition = expression.getCondition();
		final LTLModelChecker ltlModelChecker = new LTLModelChecker(this);
		final LTLProduct<explicit.MDP> conditionProduct = ltlTransformer.transform(model, condition, statesOfInterest, AcceptanceType.STREETT);
		final explicit.MDP productModel = conditionProduct.getProductModel();
		MDPModelChecker mc = getModelChecker(productModel);

		// compute Pmax(<>E | C)
		final AcceptanceStreett conditionAcceptance = (AcceptanceStreett) conditionProduct.getAcceptance();
		final BitSet conditionStates = ltlModelChecker.findAcceptingECStates(productModel, conditionAcceptance);

		// check whether the condition is satisfiable in the state of interest
		assert productModel.getNumInitialStates() == 1 : "expected one and only one initial state";
		final BitSet noPathToCondition = mc.prob0(productModel, null, conditionStates, false, null);
		if (noPathToCondition.get(productModel.getFirstInitialState())) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

		// compute E aka "objective goalState"
		final ExpressionProb objectiveProb = (ExpressionProb) expression.getObjective();
		final Expression objectiveGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objectiveProb.getExpression())).getOperand2();
		final BitSet objectiveGoalStates = mc.checkExpression(productModel, objectiveGoal, null).getBitSet();

		// compute B aka "bad states"
		final BitSet badEcStates = ltlModelChecker.findAcceptingECStates(productModel, conditionAcceptance.complementToRabin());

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
		final ModelCheckerResult conditionMaxResult = mc.computeReachProbs(productModel, conditionStates, false);
		final double[] conditionMaxProbs = conditionMaxResult.soln;

		// copy MDP to new MDPSimple
		final MDPSimple transformedModel = new MDPSimple(productModel);

		// insert states: goalState, failState
		assert productModel.getNumInitialStates() == 1 : "exactly one inital state expected";
		final int resetState = productModel.getFirstInitialState();
		final State init = transformedModel.getStatesList().get(resetState);
		final int goalState = transformedModel.addState();
		transformedModel.getStatesList().add(init);
		final int failState = transformedModel.addState();
		transformedModel.getStatesList().add(init);

		// redirect choices from objective goal states to goalState or failState
		redirectChoices(transformedModel, objectiveGoalStates, goalState, failState, conditionMaxProbs);

		// add failState choice from bad states to failState
		for (Integer state : new IterableBitSet(badStates)) {
			addDiracChoice(transformedModel, state, failState, "fail");
		}

		// add reset choice from failState state to state of interest
		addDiracChoice(transformedModel, failState, resetState, "reset");

		// add self-loop to goalState,
		addDiracChoice(transformedModel, goalState, goalState, "goal_loop");

		final Integer[] mapping = new Integer[model.getNumStates()];
		for (Integer productState : productModel.getInitialStates()) {
			// get the state index of the corresponding state in the original model
			final Integer modelState = conditionProduct.getModelState(productState);
			assert modelState != null : "first state should be set";
			assert mapping[modelState] == null : "do not map state twice";
			mapping[modelState] = productState;
		}

		final BitSet goalStates                  = BitSetTools.asBitSet(goalState);
		final BitSet transformedStatesOfInterest = BitSetTools.asBitSet(resetState);
		return new ConditionalReachabilitiyTransformation<explicit.MDP, explicit.MDP>(model, transformedModel, mapping, goalStates, transformedStatesOfInterest);
	}
}