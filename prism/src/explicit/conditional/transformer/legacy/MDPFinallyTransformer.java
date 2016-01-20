package explicit.conditional.transformer.legacy;

import java.util.BitSet;

import common.BitSetTools;
import common.iterable.IterableBitSet;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import explicit.ModelCheckerResult;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.conditional.transformer.mdp.ConditionalReachabilitiyTransformation;
import explicit.conditional.transformer.mdp.MDPResetTransformer;
import parser.State;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;

/**
 * This transformation handles a conditional {@code expression} such that<br/>
 * 1) the condition is convertible to a simple unbounded until formula and
 * 2) the objective is a reward expression other than a steady-state reward.
 * For example: <br/>
 * <code>
 * P=? [ F<50 target ][ invariant U target ]<br/>
 * R=? [ F states][ F target ]
 * </code>
 * @param expression
 * @return true iff instances of this transformation can handle {@code expression}
 */
@Deprecated
public class MDPFinallyTransformer extends MDPConditionalTransformer
{
	public MDPFinallyTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker);
	}

	//====== Protocol: Conditional Transformation ======

	@Override
	public ConditionalReachabilitiyTransformation<MDP, MDP> transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		MDPResetTransformer.checkStatesOfInterest(statesOfInterest);

		// compute C aka "condition goalState"
		final Expression conditionGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(expression.getCondition())).getOperand2();
		final BitSet conditionStates = modelChecker.checkExpression(model, conditionGoal, null).getBitSet();

		// check whether the condition is satisfiable in the state of interest
		final int resetState = statesOfInterest.nextSetBit(0);
		final BitSet noPathToCondition = modelChecker.prob0(model, null, conditionStates, false, null);
		if (noPathToCondition.get(resetState)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

		// compute E aka "objective goalState"
		final ExpressionProb objectiveProb = (ExpressionProb) expression.getObjective();
		final Expression objectiveGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objectiveProb.getExpression())).getOperand2();
		final BitSet objectiveGoalStates = modelChecker.checkExpression(model, objectiveGoal, null).getBitSet();

		// compute B aka "bad states" == {s | Pmin=0(<> (E or C))}
		BitSet targetStates = (BitSet) objectiveGoalStates.clone();
		targetStates.or(conditionStates);
		BitSet badStates = modelChecker.prob0(model, null, targetStates, true, null);

		// compute Pmax(<>E | C)
		ModelCheckerResult objectiveMaxResult = modelChecker.computeReachProbs(model, objectiveGoalStates, false);
		double[] objectiveMaxProbs = objectiveMaxResult.soln;

		// compute Pmax(<>C | E)
		ModelCheckerResult conditionMaxResult = modelChecker.computeReachProbs(model, conditionStates, false);
		double[] conditionMaxProbs = conditionMaxResult.soln;

		// copy MDP to new MDPSimple
		final MDPSimple transformedModel = new MDPSimple(model);

		// make stateOfInterst sole initial state
		transformedModel.clearInitialStates();
		transformedModel.addInitialState(resetState);

		// insert states: goalState, failState, stopState
		final State init = transformedModel.getStatesList().get(resetState);
		final int goalState = transformedModel.addState();
		transformedModel.getStatesList().add(init);
		final int failState = transformedModel.addState();
		transformedModel.getStatesList().add(init);
		final int stopState = transformedModel.addState();
		transformedModel.getStatesList().add(init);

		// redirect choices from objective goalState states to goalState or failState
		redirectChoices(transformedModel, objectiveGoalStates, goalState, failState, conditionMaxProbs);
		// redirect choices from condition goalState states to goalState or stopState
		redirectChoices(transformedModel, conditionStates, goalState, stopState, objectiveMaxProbs);

		// add failState choice from bad states to failState
		for (Integer state1 : new IterableBitSet(badStates)) {
			addDiracChoice(transformedModel, state1, failState, "fail");
		}

		// add reset choice from failState state to state of interest
		addDiracChoice(transformedModel, failState, resetState, "reset");

		// add self-loops to goalState, stopState
		addDiracChoice(transformedModel, goalState, goalState, "goal loop");
		addDiracChoice(transformedModel, stopState, stopState, "stop loop");

		final Integer[] mapping = new Integer[model.getNumStates()];
		for (int state = 0; state < mapping.length; state++) {
			mapping[state] = state;
		}

		final BitSet goalStates = new BitSet();
		goalStates.set(goalState);

		return new ConditionalReachabilitiyTransformation<MDP, MDP>(model, transformedModel, mapping, goalStates, BitSetTools.asBitSet(resetState));
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
		final Expression normalized = ExpressionInspector.normalizeExpression(objective.getExpression());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	protected BitSet getTargetStates(final MDP model, final Expression condition) throws PrismException
	{
		// assume non-negated until formula
		final Expression target = ((ExpressionTemporal) condition).getOperand2();
		return modelChecker.checkExpression(model, target, null).getBitSet();
	}
}