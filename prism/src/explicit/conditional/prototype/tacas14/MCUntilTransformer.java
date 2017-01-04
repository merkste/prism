package explicit.conditional.prototype.tacas14;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map.Entry;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import prism.PrismException;
import explicit.DTMCModelChecker;
import explicit.Model;
import explicit.conditional.ExpressionInspector;

/**
 * This transformation handles a conditional {@code expression} such that<br/>
 * 1) the condition is convertible to a simple unbounded until formula and
 * 2) the objective is a reward expression other than a steady-state reward.
 * For example: <br/>
 * <code>
 * P=? [ F<50 target ][ invariant U target ]<br/>
 * R=? [ F states][ F target ]
 * </code>
 */
@Deprecated
public class MCUntilTransformer extends MCTwoModeTransformer
{
	public MCUntilTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression)
	{
		Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		if (Expression.isNot(condition)) {
			// can handle negated formula, too
			condition = ((ExpressionUnaryOp) condition).getOperand();
		}
		return ExpressionInspector.isUnboundedSimpleUntilFormula(condition);
	}

	@Override
	protected void addInitialState(final int state, final BitSet modeOne, final BitSet modeTwo)
	{
		if (modeOne.get(state)) {
			transformedModel.addInitialState(mappingToModeOne[state]);
		}
		if (modeTwo.get(state) && pivots.get(state)) {
			transformedModel.addInitialState(mappingToModeTwo[state]);
		}
	}

	@Override
	protected BitSet getPivotStates(final BitSet support, final BitSet target)
	{
		// compute reachable target states == target & support
		final BitSet states = (BitSet) target.clone();
		states.and(support);
		return states;
	}

	@Override
	protected BitSet getStatesAddedToModeOne(final BitSet support)
	{
		// "mode 1" S_1 = {s from S | Pr(s) > 0} \ target

		// compute states added to mode one == support \ pivots
		final BitSet modeOne = (BitSet) support.clone();
		modeOne.andNot(pivots);
		return modeOne;
	}

	@Override
	protected BitSet getTargetStates(final Expression condition) throws PrismException
	{
		final Expression target;
		if (condition instanceof ExpressionUnaryOp) {
			// assume negated until formula
			final ExpressionTemporal temporal = (ExpressionTemporal) ((ExpressionUnaryOp) condition).getOperand();
			target = Expression.Not(Expression.Or(temporal.getOperand1(), temporal.getOperand2()));
		} else {
			// assume non-negated until formula
			target = ((ExpressionTemporal) condition).getOperand2();
		}
		return getModelChecker(originalModel).checkExpression(originalModel, target, null).getBitSet();
	}

	@Override
	protected void transformTransitionsModeOne(final double[] probabilities, final int state)
	{
		if (mappingToModeOne[state] != null) {
			// Pr(state) > 0 & state is no pivot state
			final int mappedState = mappingToModeOne[state];
			for (Iterator<Entry<Integer, Double>> iter = originalModel.getTransitionsIterator(state); iter.hasNext();) {
				// P'(s,v) = P(s,v) * Pr(v) / Pr(s) for all s in S_1
				final Entry<Integer, Double> transition = iter.next();
				final int successor = transition.getKey();
				if (probabilities[successor] > 0) {
					final int mappedSuccessor = pivots.get(successor) ? mappingToModeTwo[successor] : mappingToModeOne[successor];
					final double probability = transition.getValue() * probabilities[successor] / probabilities[state];
					transformedModel.setProbability(mappedState, mappedSuccessor, probability);
				}
			}
		}
	}

	@Override
	protected double[] computeProbabilities(final BitSet target) throws PrismException
	{
		return getModelChecker(originalModel).computeUntilProbs(originalModel, null, target).soln;
	}
}