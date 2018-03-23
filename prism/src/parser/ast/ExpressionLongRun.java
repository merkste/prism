package parser.ast;

import java.util.Objects;

import parser.Values;
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;

/**
 * @author Steffen
 */
public class ExpressionLongRun extends ExpressionQuant<Expression>
{
	protected Expression states;

	public ExpressionLongRun(Expression expression, Expression states, String relOpString, Expression bound)
	{
		this(expression, states, RelOp.parseSymbol(relOpString), bound);
	}

	public ExpressionLongRun(Expression expression, Expression states, RelOp relOp, Expression bound)
	{
		Objects.requireNonNull(expression);
		Objects.requireNonNull(states);
		Objects.requireNonNull(relOp);
		this.expression = expression;
		this.states     = states;
		this.relOp      = relOp;
		this.bound      = bound;
	}

	public Expression getStates()
	{
		return states;
	}

	public void setStates(Expression states)
	{
		Objects.requireNonNull(states);
		this.states = states;
	}

	@Override
	public OpRelOpBound getRelopBoundInfo(Values constantValues) throws PrismException
	{
		if (getBound() != null) {
			double boundValue = getBound().evaluateDouble(constantValues);
			return new OpRelOpBound("L", minMax, getRelOp(), boundValue);
		} else {
			return new OpRelOpBound("L", minMax);
		}
	}

	@Override
	public boolean isMatchingElement(ASTElement other)
	{
		return other instanceof ExpressionLongRun;
	}

	@Override
	public String getResultName()
	{
		return (getBound() == null) ? expression.getResultName() : "Result";
	}

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public ExpressionLongRun deepCopy(DeepCopy copier) throws PrismLangException
	{
		super.deepCopy(copier);
		states = copier.copy(states);

		return this;
	}

	@Override
	public ExpressionLongRun clone()
	{
		return (ExpressionLongRun) super.clone();
	}

	@Override
	protected String operatorToString()
	{
		return "L";
	}

	@Override
	protected String bodyToString()
	{
		return getExpression() + " , " + getStates();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		result = prime * result + ((states == null) ? 0 : states.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof ExpressionLongRun)) {
			return false;
		}
		ExpressionLongRun other = (ExpressionLongRun) obj;
		if (expression == null) {
			if (other.expression != null) {
				return false;
			}
		} else if (!expression.equals(other.expression)) {
			return false;
		}
		if (states == null) {
			if (other.states != null) {
				return false;
			}
		} else if (!states.equals(other.states)) {
			return false;
		}
		return true;
	}
}
