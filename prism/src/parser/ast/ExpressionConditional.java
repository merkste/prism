/**
 * 
 */
package parser.ast;

import parser.EvaluateContext;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

/**
 * @author Steffen
 *
 */
public class ExpressionConditional extends Expression {

	private ExpressionQuant objective;
	private Expression condition;

	public ExpressionConditional(final ExpressionQuant objective, final Expression condition) {
		assert (objective != null) && (condition != null)
				: "objective and condition must not be null";;
		this.objective = objective;
		this.condition = condition;
	}

	public ExpressionQuant getObjective() {
		return objective;
	}

	public Expression getCondition() {
		return condition;
	}

	/* (non-Javadoc)
	 * @see parser.ast.Expression#isConstant()
	 */
	@Override
	public boolean isConstant() {
		return false;
	}

	public boolean isQuantitative() {
		return (objective == null) ? false : Expression.isQuantitative(objective);
	}

	/* (non-Javadoc)
	 * @see parser.ast.Expression#evaluate(parser.EvaluateContext)
	 */
	@Override
	public Object evaluate(final EvaluateContext ec) throws PrismLangException {
		throw new PrismLangException("Cannot evaluate a conditional expression without a model");
	}

	/* (non-Javadoc)
	 * @see parser.ast.Expression#returnsSingleValue()
	 */
	@Override
	public boolean returnsSingleValue() {
		return false;
	}

	/* (non-Javadoc)
	 * @see parser.ast.Expression#deepCopy()
	 */
	@Override
	public ExpressionConditional deepCopy() {
		final ExpressionQuant objectiveCopy = (objective == null) ? null : (ExpressionQuant) objective.deepCopy();
		final Expression conditionCopy      = (condition == null) ? null : condition.deepCopy();
		final ExpressionConditional copy = new ExpressionConditional(objectiveCopy, conditionCopy);
		copy.setPosition(this);
		copy.setType(type);
		return copy;
	}

	/* (non-Javadoc)
	 * @see parser.ast.ASTElement#accept(parser.visitor.ASTVisitor)
	 */
	@Override
	public Object accept(final ASTVisitor v) throws PrismLangException {
		return v.visit(this);
	}

	/* (non-Javadoc)
	 * @see parser.ast.ASTElement#toString()
	 */
	@Override
	public String toString() {
		return objective + "[ " + condition + " ]";
	}

	@Override
	public boolean isProposition() {
		return false;
	}

	@Override
	public boolean isMatchingElement(ASTElement other) {
		return other instanceof ExpressionConditional;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
		result = prime * result + ((objective == null) ? 0 : objective.hashCode());
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
		if (objective == null) {
			if (other.objective != null) {
				return false;
			}
		} else if (!objective.equals(other.objective)) {
			return false;
		}
		return true;
	}
}
