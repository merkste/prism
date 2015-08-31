package explicit.conditional;

import parser.ast.Expression;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import prism.PrismLangException;

//FIXME ALG: add comment
public class ExpressionInspector
{
	// (expr = remain U goal) without bounds
	public static boolean isSimpleUntilFormula(final Expression expression)
	{
		try {
			if (!(expression instanceof ExpressionTemporal && expression.isSimplePathFormula())) {
				// can handle simple path conditions only
				return false;
			}
		} catch (PrismLangException e) {
			// expression cannot be checked whether it is a simple formula
			return false;
		}
		final ExpressionTemporal temporal = (ExpressionTemporal) expression;
		if (!(temporal.getOperator() == ExpressionTemporal.P_U)) {
			// can handle until conditions only
			return false;
		}
		if (temporal.hasBounds()) {
			// can handle unbounded conditions only
			return false;
		}

		return true;
	}

	// either (expr = F goal) or (expr = true U goal) 
	public static boolean isSimpleFinallyFormula(final Expression expression)
	{
		if (!(expression instanceof ExpressionTemporal)) {
			return false;
		}
		final Expression until;
		try {
			until = ((ExpressionTemporal) expression).convertToUntilForm();
		} catch (PrismLangException e) {
			// cannot convert expression to until form
			return false;
		}
		if (!isSimpleUntilFormula(until)) {
			// expression is not a simple until formula
			return false;
		}
		return Expression.isTrue(((ExpressionTemporal) until).getOperand1());
	}

	public static boolean isReachablilityReward(final Expression expression)
	{
		if (expression instanceof ExpressionReward) {
			final Expression subexpression = ((ExpressionReward) expression).getExpression();
			if (subexpression instanceof ExpressionTemporal) {
				return ((ExpressionTemporal) subexpression).getOperator() == ExpressionTemporal.R_F;
			}
		}
		return false;
	}

	public static boolean isSteadyStateReward(final Expression expression)
	{
		if (expression instanceof ExpressionReward) {
			final Expression subexpression = ((ExpressionReward) expression).getExpression();
			if (subexpression instanceof ExpressionTemporal) {
				return ((ExpressionTemporal) subexpression).getOperator() == ExpressionTemporal.R_S;
			}
		}
		return false;
	}

	/**
	 * Trim surrounding parentheses, double negations and double minuses.
	 * Yields {@code null} for {@code null} operands in double negations
	 * and double minuses, e.g., {@code Not(Not(null)) == null}.
	 * 
	 * @param expression
	 * @return trimmed expression
	 */
	public static Expression trimUnaryOperations(final Expression expression)
	{
		if (expression instanceof ExpressionUnaryOp) {
			final Expression operand = trimUnaryOperations(((ExpressionUnaryOp) expression).getOperand());

			// omit double negation
			if (Expression.isNot(expression)) {
				return Expression.isNot(operand) ? ((ExpressionUnaryOp) operand).getOperand() : Expression.Not(operand);
			}
			// omit double minus
			if (Expression.isMinus(expression)) {
				return Expression.isMinus(operand) ? ((ExpressionUnaryOp) operand).getOperand() : Expression.Minus(operand);
			}
			// omit parentheses
			if (Expression.isParenth(expression)) {
				return operand;
			}

			assert false : "unknown unary expression" + expression;
		}

		return expression;
	}

	/**
	 * 1. If possible, convert the outermost temporal operator to until form.
	 * 2. If outermost temporal operator is negated next, move negation to the inner formula.
	 * 3. Trim surrounding parentheses, double negation and double minus.
	 * 
	 * @param expression
	 * @return normalized expression, possibly negated
	 */
	public static Expression normalizeExpression(final Expression expression)
	{
		Expression normalized = expression;

		if (expression instanceof ExpressionTemporal) {
			try {
				normalized = ((ExpressionTemporal) expression).convertToUntilForm();
			} catch (PrismLangException e) {
				// no conversion possible, ignore
			}
		} else if (expression instanceof ExpressionUnaryOp) {
			final Expression operand = normalizeExpression(((ExpressionUnaryOp) expression).getOperand());

			if (Expression.isNot(expression) && Expression.isNext(operand)) {
				// move negation to inner formula
				normalized = Expression.Next(Expression.Not(((ExpressionTemporal) operand).getOperand2()));
			} else {
				// retain unary operation
				normalized = new ExpressionUnaryOp(((ExpressionUnaryOp) expression).getOperator(), operand);
			}
		}

		return trimUnaryOperations(normalized);
	}
}