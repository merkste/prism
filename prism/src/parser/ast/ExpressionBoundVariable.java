package parser.ast;

import parser.type.Type;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class ExpressionBoundVariable extends ExpressionConstant {

	public ExpressionBoundVariable() {
		super();
	}

	public ExpressionBoundVariable(String n, Type t) {
		super(n,t);
	}

	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	/**
	 * Perform a deep copy.
	 */
	public Expression deepCopy()
	{
		Expression ret = new ExpressionBoundVariable(name, type);
		ret.setPosition(this);
		return ret;
	}

}
