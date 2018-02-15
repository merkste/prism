/**
 * 
 */
package parser.ast;

import java.util.Objects;

import explicit.MinMax;
import parser.Values;
import parser.visitor.ASTVisitor;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;

/**
 * @author Steffen
 */
public class ExpressionConditional extends ExpressionQuant<ExpressionQuant<?>>
{
	protected Expression condition;

	public ExpressionConditional(ExpressionQuant<?> objective, Expression condition)
	{
		Objects.requireNonNull(objective);
		Objects.requireNonNull(condition);
		this.expression = objective;
		this.condition = condition;
	}

	public ExpressionQuant<?> getObjective()
	{
		return expression;
	}

	public void setObjective(ExpressionQuant<?> objective)
	{
		setExpression(objective);
	}

	public Expression getCondition()
	{
		return condition;
	}

	public void setCondition(Expression condition)
	{
		Objects.requireNonNull(condition);
		this.condition = condition;
	}

	// Methods to be overridden

	@Override
	public MinMax getMinMax()
	{
		return expression.getMinMax();
	}

	@Override
	public RelOp getRelOp()
	{
		return expression.getRelOp();
	}

	@Override
	public Expression getBound()
	{
		return expression.getBound();
	}

	@Override
	public OpRelOpBound getRelopBoundInfo(Values constantValues) throws PrismException
	{
		return expression.getRelopBoundInfo(constantValues);
	}

	@Override
	public boolean isMatchingElement(ASTElement other)
	{
		return other instanceof ExpressionConditional;
	}

	@Override
	public ExpressionConditional deepCopy()
	{
		ExpressionQuant<?> objectiveCopy = expression.deepCopy();
		Expression conditionCopy   = condition.deepCopy();
		ExpressionConditional copy = new ExpressionConditional(objectiveCopy, conditionCopy);

		copy.setPosition(this);
		copy.setType(type);

		return copy;
	}

	@Override
	public Object accept(final ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	protected String operatorToString()
	{
		return expression.operatorToString();
	}

	@Override
	protected String boundsToString()
	{
		return expression.boundsToString();
	}

	@Override
	protected String bodyToString()
	{
		return expression.bodyToString() + " || " + condition.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((condition == null)  ? 0 : condition.hashCode());
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
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
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ExpressionConditional)) {
			return false;
		}
		ExpressionConditional other = (ExpressionConditional) obj;
		if (condition == null) {
			if (other.condition != null) {
				return false;
			}
		} else if (!condition.equals(other.condition)) {
			return false;
		}
		if (expression == null) {
			if (other.expression != null) {
				return false;
			}
		} else if (!expression.equals(other.expression)) {
			return false;
		}
		return true;
	}
}
