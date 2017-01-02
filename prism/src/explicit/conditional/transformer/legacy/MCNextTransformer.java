package explicit.conditional.transformer.legacy;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map.Entry;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;
import explicit.DTMCModelChecker;
import explicit.Model;
import explicit.ReachabilityComputer;
import explicit.conditional.ExpressionInspector;

/**
 * This transformation handles a conditional {@code expression} such that<br/>
 * 1) the condition is (possibly negated) next formula and
 * 2) the objective is a reward expression other than a steady-state reward.
 * For example: <br/>
 * <code>
 * P=? [ F<50 states ][ X target ]<br/>
 * R=? [ F states][ ! X target ]
 * </code>
 */
// FIXME ALG: adapt comment
@Deprecated
public class MCNextTransformer extends MCTwoModeTransformer
{
	public MCNextTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		try {
			if (!(condition instanceof ExpressionTemporal && condition.isSimplePathFormula())) {
				// can handle simple path conditions only
				return false;
			}
		} catch (PrismLangException e) {
			// condition cannot be checked whether it is a simple formula
			return false;
		}
		final ExpressionTemporal temporal = (ExpressionTemporal) condition;
		if (!(temporal.getOperator() == ExpressionTemporal.P_X)) {
			// can handle next conditions only
			return false;
		}
		if (temporal.hasBounds()) {
			// can handle unbounded conditions only
			return false;
		}
		if (ExpressionInspector.isSteadyStateReward(expression.getObjective())) {
			// cannot handle steady state computation yet
			return false;
		}
		return true;
	}

	@Override
	protected void addInitialState(final int state, final BitSet modeOne, final BitSet modeTwo)
	{
		if (modeOne.get(state)) {
			transformedModel.addInitialState(mappingToModeOne[state]);
		}
	}

	@Override
	protected BitSet getPivotStates(final BitSet support, final BitSet target)
	{
		// compute reachable target states == target & succ(support)
		final BitSet states = new ReachabilityComputer(originalModel).computeSucc(support);
		states.and(target);
		return states;
	}

	@Override
	protected BitSet getStatesAddedToModeOne(final BitSet support)
	{
		// "mode 1" S_1 = {s from S | Pr(s) > 0}

		// compute states added to mode one == support
		return (BitSet) support.clone();
	}

	@Override
	protected BitSet getTargetStates(final Expression condition) throws PrismException
	{
		assert condition instanceof ExpressionTemporal : "ExpressionTemporal expected";

		return modelChecker.checkExpression(originalModel, ((ExpressionTemporal) condition).getOperand2(), null).getBitSet();
	}

	@Override
	protected void transformTransitionsModeOne(final double[] probabilities, final int state)
	{
		if (mappingToModeOne[state] != null) {
			// Pr(state) > 0 & state is no pivot state
			final int mappedState = mappingToModeOne[state];
			for (Iterator<Entry<Integer, Double>> iter = originalModel.getTransitionsIterator(state); iter.hasNext();) {
				// P'(s,v) = P(s,v) / Pr(s) for all s in S', v in target
				final Entry<Integer, Double> transition = iter.next();
				final int successor = transition.getKey();
				if (pivots.get(successor)) {
					final int mappedSuccessor = mappingToModeTwo[successor];
					final double probability = transition.getValue() / probabilities[state];
					transformedModel.setProbability(mappedState, mappedSuccessor, probability);
				}
			}
		}
	}

	@Override
	protected double[] computeProbabilities(final BitSet target) throws PrismException
	{
		return modelChecker.computeNextProbs(originalModel, target).soln;
	}

	@Override
	protected Integer projectToTransformedModel(final int state)
	{
		return mappingToModeOne[state];
	}
}