package explicit.conditional.prototype.tacas14;

import java.util.BitSet;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionTemporal;
import prism.PrismLangException;
import explicit.DTMCModelChecker;
import explicit.Model;
import explicit.conditional.ExpressionInspector;

/**
 * This transformation handles a conditional reachability reward {@code expression} such that<br/>
 * 1) the condition is convertible to a simple unbounded until and
 * 2) the reward goal equals the until goal.
 * For example: <br/>
 * <code>
 * R=? [ F target ][ invariant U target]
 * </code>
 * @param expression
 * @return true iff instances of this transformation can handle {@code expression}
 */
// FIXME ALG: adapt comment
@Deprecated
public class MCMatchingFinallyTransformer extends MCUntilTransformer
{
	public MCMatchingFinallyTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	public boolean canHandle(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!super.canHandle(model, expression)) {
			return false;
		}

		final Expression objectiveGoal = ExpressionInspector.trimUnaryOperations(getObjectiveGoal(expression));
		final Expression conditionGoal = ExpressionInspector.trimUnaryOperations(getConditionGoal(expression));
		try {
			return objectiveGoal.syntacticallyEquals(conditionGoal);
		} catch (PrismLangException e) {
			assert false : "The conditional expression is expected to be syntactically correct.";
			return false;
		}
	}

	private Expression getConditionGoal(final ExpressionConditional expression)
	{
		return ((ExpressionTemporal) ExpressionInspector.normalizeExpression(expression.getCondition())).getOperand2();
	}

	private Expression getObjectiveGoal(final ExpressionConditional expression)
	{
		final Expression objective = expression.getObjective();
		final ExpressionTemporal objectiveTemp;
		if (ExpressionInspector.isReachablilityReward(objective)) {
			objectiveTemp = (ExpressionTemporal) ((ExpressionReward) objective).getExpression();
		} else {
			objectiveTemp = (ExpressionTemporal) objective;
		}
		return objectiveTemp.getOperand2();
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression)
	{
		if (!super.canHandleCondition(model, expression)) {
			return false;
		}
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		// can handle non-negated conditions only
		return !Expression.isNot(condition);
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression)
	{
		if (!super.canHandleObjective(model, expression)) {
			return false;
		}
		final Expression objective = expression.getObjective();
		// can handle simple until and reachability reward objectives only
		return ExpressionInspector.isUnboundedSimpleUntilFormula(objective) || ExpressionInspector.isReachablilityReward(objective);
	}

	@Override
	protected BitSet getStatesAddedToModeTwo()
	{
		// "mode 2" S_2 = {s from target | Pr(s) > 0}
		// mode_2 == pivots
		return pivots;
	}

	@Override
	protected void transformTransitionsModeTwo(final int state)
	{
		// transitions in mode 2
		if (mappingToModeTwo[state] != null) {
			// state is in succ*(pivots) == pivots
			// P'(s,s) = 1 for all s in pivot set
			int mappedState = mappingToModeTwo[state];
			transformedModel.setProbability(mappedState, mappedState, 1);
		}
	}
}