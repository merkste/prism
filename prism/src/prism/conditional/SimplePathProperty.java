package prism.conditional;

import explicit.conditional.ExpressionInspector;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;
import prism.StateModelChecker;

public abstract class SimplePathProperty
{
	protected boolean negated;
	protected JDDNode goal;



	public SimplePathProperty()
	{
		// Empty constructor to support flexible subclass instantiation.
	}

	public SimplePathProperty(boolean negated, JDDNode goal)
	{
		this.negated = negated;
		this.goal    = goal;
	}

	public JDDNode getGoal()
	{
		return goal;
	}

	public boolean isNegated()
	{
		return negated;
	}

	public void clear()
	{
		JDD.Deref(goal);
	}

	public static SimplePathProperty fromExpression(Expression expression, StateModelChecker modelChecker) throws PrismException
	{
		ExpressionTemporal temporalExpression;
		try {
			temporalExpression = Expression.getTemporalOperatorForSimplePathFormula(expression);
		} catch (PrismLangException e) {
			throw new IllegalArgumentException(e);
		}
		int operator = temporalExpression.getOperator();
		switch (operator) {
		case ExpressionTemporal.P_X:
			return new Next(expression, modelChecker);
		case ExpressionTemporal.P_F:
		case ExpressionTemporal.P_G:
			return new Finally(expression, modelChecker, true);
		case ExpressionTemporal.P_U:
		case ExpressionTemporal.P_R:
		case ExpressionTemporal.P_W:
			return new Until(expression, modelChecker, true);
		default:
			throw new IllegalArgumentException("Unsupported temporal operator: " + operator);
		}
	}

	public static void requireUnboundedSimplePathFormula(Expression expression)
	{
		if (! isUnboundedSimplePathFormula(expression)) {
			throw new IllegalArgumentException("Expression is not an unbounded simple path formula: " + expression);
		}
	}


	public static boolean isUnboundedSimplePathFormula(Expression expression)
	{
		try {
			if (! expression.isSimplePathFormula()) {
				return false;
			}
			ExpressionTemporal temporalExpression = Expression.getTemporalOperatorForSimplePathFormula(expression);
			if (temporalExpression.hasBounds()) {
				return false;
			}
		} catch (PrismLangException e) {
			throw new RuntimeException(e);
		}
		return true;
	}



	public static class Until extends SimplePathProperty
	{
		protected final JDDNode remain;

		public Until(Expression expression, StateModelChecker modelChecker) throws PrismException 
		{
			this(expression, modelChecker, false);
		}

		public Until(Expression expression, StateModelChecker modelChecker, boolean convert) throws PrismException 
		{
			Expression canonical     = normalizeUnboundedSimpleUntilFormula(expression, convert);
			ExpressionTemporal until = Expression.getTemporalOperatorForSimplePathFormula(canonical);

			negated = Expression.isNot(canonical);
			remain  = modelChecker.checkExpressionDD(until.getOperand1(), JDD.Constant(1));
			goal    = modelChecker.checkExpressionDD(until.getOperand2(), JDD.Constant(1));
		}

		public Until(JDDNode remain, JDDNode goal)
		{
			this(false, remain, goal);
		}

		public Until(boolean negated, JDDNode remain, JDDNode goal)
		{
			super(negated, goal);
			this.remain = remain;
		}

		public JDDNode getRemain()
		{
			return remain;
		}

		public void clear()
		{
			super.clear();
			JDD.Deref(remain);
		}

		public static Expression normalizeUnboundedSimpleUntilFormula(Expression expression, boolean convert)
		{
			requireUnboundedSimplePathFormula(expression);
			try {
				Expression canonical;
				if (convert) {
					canonical = Expression.convertSimplePathFormulaToCanonicalForm(expression);
				} else {
					canonical = ExpressionInspector.trimUnaryOperations(expression);
				}
				ExpressionTemporal temporal = Expression.getTemporalOperatorForSimplePathFormula(canonical);
				if (temporal.getOperator() == ExpressionTemporal.P_U) {
					return canonical;
				}
			} catch (PrismLangException e) {
				// throw IllegalArgumentException below
			}
			throw new IllegalArgumentException("Expression cannot be converted to until form: " + expression);
		}
	}



	public static class Finally extends Until
	{
		public Finally(Expression expression, StateModelChecker modelChecker) throws PrismException
		{
			this(expression, modelChecker, false);
		}
		public Finally(Expression expression, StateModelChecker modelChecker, boolean convert) throws PrismException
		{
			super(normalizeUnboundedSimpleFinallyFormula(expression, convert), modelChecker, true);
		}

		public Finally(JDDNode goal)
		{
			super(JDD.Constant(1), goal);
		}

		public Finally(boolean negated, JDDNode goal)
		{
			super(negated, JDD.Constant(1), goal);
		}

		public static Expression normalizeUnboundedSimpleFinallyFormula(Expression expression, boolean convert)
		{
			requireUnboundedSimplePathFormula(expression);

			try {
				ExpressionTemporal temporal = Expression.getTemporalOperatorForSimplePathFormula(expression);
				int operator                = temporal.getOperator();
				if (operator == ExpressionTemporal.P_F) {
					return ExpressionInspector.trimUnaryOperations(expression);
				}
				if (convert) {
					// convert G, U, R, W
					Expression canonical = Expression.convertSimplePathFormulaToCanonicalForm(expression);
					temporal             = Expression.getTemporalOperatorForSimplePathFormula(canonical);
					operator             = temporal.getOperator();
					if (operator == ExpressionTemporal.P_U && Expression.isTrue(temporal.getOperand1())) {
						ExpressionTemporal eventually = Expression.Finally(temporal.getOperand2());
						return Expression.isNot(canonical) ? Expression.Not(eventually) : eventually;
					}
				}
			} catch (PrismLangException e) {
				// throw IllegalArgumentException below
			}
			throw new IllegalArgumentException("Expression cannot be converted to finally form: " + expression);
		}
	}



	public static class Next extends SimplePathProperty
	{
		public Next(Expression expression, StateModelChecker modelChecker) throws PrismException 
		{
			this(expression, modelChecker, false);
		}

		public Next(Expression expression, StateModelChecker modelChecker, boolean convert) throws PrismException 
		{
			Expression canonical    = normalizeUnboundedSimpleNextFormula(expression, convert);
			ExpressionTemporal next = Expression.getTemporalOperatorForSimplePathFormula(canonical);

			negated = Expression.isNot(canonical);
			goal    = modelChecker.checkExpressionDD(next.getOperand2(), JDD.Constant(1));
		}

		public Next(JDDNode goal)
		{
			this(false, goal);
		}

		public Next(boolean negated, JDDNode goal)
		{
			super(negated, goal);
		}

		public static Expression normalizeUnboundedSimpleNextFormula(Expression expression, boolean convert)
		{
			requireUnboundedSimplePathFormula(expression);

			try {
				Expression canonical;
				if (convert) {
					canonical = Expression.convertSimplePathFormulaToCanonicalForm(expression);
				} else {
					canonical = ExpressionInspector.trimUnaryOperations(expression);
				}
				ExpressionTemporal temporal = Expression.getTemporalOperatorForSimplePathFormula(canonical);
				if (temporal.getOperator() == ExpressionTemporal.P_X) {
					return canonical;
				}
			} catch (PrismLangException e) {
				// throw IllegalArgumentException below
			}
			throw new IllegalArgumentException("Expression cannot be converted to next form: " + expression);
		}
	}
}
