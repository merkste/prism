//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package parser.ast;

import param.BigRational;
import parser.EvaluateContext;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class ExpressionTemporal extends Expression
{
	// Operator constants
	public static final int P_X = 1; // Next (for P operator)
	public static final int P_U = 2; // Until (for P operator)
	public static final int P_F = 3; // Future (for P operator)
	public static final int P_G = 4; // Globally (for P operator)
	public static final int P_W = 5; // Weak until (for P operator)
	public static final int P_R = 6; // Release (for P operator)
	public static final int R_C = 11; // Cumulative (for R operator)
	public static final int R_I = 12; // Instantaneous (for R operator)
	public static final int R_F = 13; // Reachability (for R operator) // DEPRECATED: Use P_F
	public static final int R_S = 14; // Steady-state (for R operator)
	// Operator symbols
	public static final String opSymbols[] = { "", "X", "U", "F", "G", "W", "R", "", "", "", "", "C", "I", "F", "S" };

	/** Operator, one of the operator constants above */
	protected int op = 0;

	// Up to two operands (either may be null)
	/** LHS of operator, null for unary operators */
	protected Expression operand1 = null;
	/** RHS of operator, null for nullary operators (e.g., S) */
	protected Expression operand2 = null;


	// the bounds
	protected TemporalOperatorBounds bounds = new TemporalOperatorBounds();

	// Constructors

	/** Constructor */
	public ExpressionTemporal()
	{
	}

	/**
	 * Constructor.
	 * @param op the temporal operator (see constants at ExpressionTemporal)
	 * @param operand1 the LHS operand ({@code null} for unary operators)
	 * @param operand2 the RHS operand
	 */
	public ExpressionTemporal(int op, Expression operand1, Expression operand2)
	{
		this.op = op;
		this.operand1 = operand1;
		this.operand2 = operand2;
	}

	// Set methods

	/** Set the operator to i */
	public void setOperator(int i)
	{
		op = i;
	}

	/** Set the LHS operand to e1 */
	public void setOperand1(Expression e1)
	{
		operand1 = e1;
	}

	/** Set the RHS operand to e2 */
	public void setOperand2(Expression e2)
	{
		operand2 = e2;
	}

	// Get methods

	/** Set the operator */
	public int getOperator()
	{
		return op;
	}

	public String getOperatorSymbol()
	{
		return opSymbols[op];
	}

	/** Get the LHS operand (should be {@code null} for unary operators) */
	public Expression getOperand1()
	{
		return operand1;
	}

	/** Get the RHS operand */
	public Expression getOperand2()
	{
		return operand2;
	}

	/* Get the number of stored operands */
	public int getNumOperands()
	{
		if (operand2 == null)
			return 0;
		else
			return (operand1 == null) ? 1 : 2;
	}

	public boolean hasBounds()
	{
		return bounds.hasBounds();
	}


	// Methods required for Expression:

	@Override
	public boolean isConstant()
	{
		return false;
	}

	@Override
	public boolean isProposition()
	{
		return false;
	}
	
	@Override
	public boolean isMatchingElement(ASTElement other)
	{
		if (!(other instanceof ExpressionTemporal))
			return false;

		ExpressionTemporal otherTemporal = (ExpressionTemporal) other;
		if (this.getOperator() != otherTemporal.getOperator())
			return false;

		// bounds are done recursively

		return true;
	}

	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a temporal operator without a path");
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a temporal operator without a path");
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	// Methods required for ASTElement:

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public Expression deepCopy()
	{
		ExpressionTemporal expr = new ExpressionTemporal();
		expr.setOperator(op);
		if (operand1 != null)
			expr.setOperand1(operand1.deepCopy());
		if (operand2 != null)
			expr.setOperand2(operand2.deepCopy());
		expr.setBounds(bounds.deepCopy());

		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}
	
	public void setBounds(TemporalOperatorBounds bounds) {
		this.bounds = bounds;
	}

	public TemporalOperatorBounds getBounds() {
		return bounds;
	}

	// Standard methods

	@Override
	public String toString()
	{
		String s = "";
		if (operand1 != null)
			s += operand1 + " ";
		s += opSymbols[op];

		if (op == R_I && bounds.hasDefaultBound()) {
			TemporalOperatorBound bound = bounds.getDefaultBound();
			if (!bound.hasLowerBound() && bound.hasUpperBound()) {
				s += "<" + (bound.upperBoundIsStrict() ? "" : "=") + bound.getUpperBound();
			} else {
				s+=bound.toString();
			}
		} else if (bounds.hasBounds()) {
			s+=bounds.toString();
		}
		if (operand2 != null)
			s += " " + operand2;
		return s;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bounds == null) ? 0 : bounds.hashCode());
		result = prime * result + op;
		result = prime * result + ((operand1 == null) ? 0 : operand1.hashCode());
		result = prime * result + ((operand2 == null) ? 0 : operand2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ExpressionTemporal))
			return false;
		ExpressionTemporal other = (ExpressionTemporal) obj;
		if (bounds == null) {
			if (other.bounds != null)
				return false;
		} else if (!bounds.equals(other.bounds))
			return false;
		if (op != other.op)
			return false;
		if (operand1 == null) {
			if (other.operand1 != null)
				return false;
		} else if (!operand1.equals(other.operand1))
			return false;
		if (operand2 == null) {
			if (other.operand2 != null)
				return false;
		} else if (!operand2.equals(other.operand2))
			return false;
		return true;
	}

	// Other useful methods

	/**
	 * Convert (P operator) path formula to untils, using standard equivalences.
	 */
	public Expression convertToUntilForm() throws PrismLangException
	{
		Expression op1, op2;
		ExpressionTemporal exprTemp = null;
		switch (op) {
		case P_U:
			return this;
		case P_F:
			// F a == true U a
			op1 = Expression.True();
			exprTemp = new ExpressionTemporal(P_U, op1, operand2);
			// TODO: Verify for rewards etc
			exprTemp.setBounds(bounds.deepCopy());
			return exprTemp;
		case P_G:
			// G a == !(true U !a)
			op1 = Expression.True();
			op2 = Expression.Not(operand2);
			exprTemp = new ExpressionTemporal(P_U, op1, op2);
			// TODO: Verify for rewards etc
			exprTemp.setBounds(bounds.deepCopy());
			return Expression.Not(exprTemp);
		case P_W:
			// a W b == !(a&!b U !a&!b)
			op1 = Expression.And(operand1, Expression.Not(operand2));
			op2 = Expression.And(Expression.Not(operand1), Expression.Not(operand2));
			exprTemp = new ExpressionTemporal(P_U, op1, op2);
			exprTemp.setBounds(bounds.deepCopy());
			return Expression.Not(exprTemp);
		case P_R:
			// a R b == !(!a U !b)
			op1 = Expression.Not(operand1);
			op2 = Expression.Not(operand2);
			exprTemp = new ExpressionTemporal(P_U, op1, op2);
			exprTemp.setBounds(bounds.deepCopy());
			return Expression.Not(exprTemp);
		}
		throw new PrismLangException("Cannot convert " + getOperatorSymbol() + " to until form");
	}

	// ---- convenience methods for distinguishing top-level temporal operators -----------------

	/**
	 * Returns true if the given expression is an ExpressionTemporal with the next step (X) top-level operator.
	 */
	public static boolean isNext(Expression e)
	{
		return (e instanceof ExpressionTemporal) && ((ExpressionTemporal) e).getOperator() == P_X;
	}

	/**
	 * Returns true if the given expression is an ExpressionTemporal with the Until (U) top-level operator.
	 */
	public static boolean isUntil(Expression e)
	{
		return (e instanceof ExpressionTemporal) && ((ExpressionTemporal) e).getOperator() == P_U;
	}

	/**
	 * Returns true if the given expression is an ExpressionTemporal with the Weak Until (W) top-level operator.
	 */
	public static boolean isWeakUntil(Expression e)
	{
		return (e instanceof ExpressionTemporal) && ((ExpressionTemporal) e).getOperator() == P_W;
	}

	/**
	 * Returns true if the given expression is an ExpressionTemporal with the Release (R) top-level operator.
	 */
	public static boolean isRelease(Expression e)
	{
		return (e instanceof ExpressionTemporal) && ((ExpressionTemporal) e).getOperator() == P_R;
	}

	/**
	 * Returns true if the given expression is an ExpressionTemporal with the finally / eventually (F) top-level operator.
	 */
	public static boolean isFinally(Expression e)
	{
		return (e instanceof ExpressionTemporal) && ((ExpressionTemporal) e).getOperator() == P_F;
	}

	/**
	 * Returns true if the given expression is an ExpressionTemporal with the globally / always (G) top-level operator.
	 */
	public static boolean isGlobally(Expression e)
	{
		return (e instanceof ExpressionTemporal) && ((ExpressionTemporal) e).getOperator() == P_G;
	}

	/**
	 * Returns true if the given expression is an ExpressionTemporal
	 * with a globally finally / always eventually (G followed by F) top-level operator pair.
	 */
	public static boolean isGloballyFinally(Expression e)
	{
		return isGlobally(e) && isFinally(((ExpressionTemporal) e).getOperand2());
	}

	/**
	 * Returns true if the given expression is an ExpressionTemporal
	 * with a finally globally / eventually always (F followed by G) top-level operator pair.
	 */
	public static boolean isFinallyGlobally(Expression e)
	{
		return isFinally(e) && isGlobally(((ExpressionTemporal) e).getOperand2());
	}


	// ---- static constructors for building temporal expressions -----------------

	/** Construct a ExpressionTemporal for "X expr" */
	public static ExpressionTemporal Next(Expression expr)
	{
		return new ExpressionTemporal(P_X, null, expr);
	}

	/** Construct a ExpressionTemporal for "F expr" */
	public static ExpressionTemporal Finally(Expression expr)
	{
		return new ExpressionTemporal(P_F, null, expr);
	}

	/** Construct a ExpressionTemporal for "G expr" */
	public static ExpressionTemporal Globally(Expression expr)
	{
		return new ExpressionTemporal(P_G, null, expr);
	}

	/** Construct a ExpressionTemporal for "expr1 U expr2" */
	public static ExpressionTemporal Globally(Expression expr1, Expression expr2)
	{
		return new ExpressionTemporal(P_U, expr1, expr2);
	}

	/** Construct a ExpressionTemporal for "expr1 W expr2" */
	public static ExpressionTemporal WeakUntil(Expression expr1, Expression expr2)
	{
		return new ExpressionTemporal(P_W, expr1, expr2);
	}

	/** Construct a ExpressionTemporal for "expr1 R expr2" */
	public static ExpressionTemporal Release(Expression expr1, Expression expr2)
	{
		return new ExpressionTemporal(P_R, expr1, expr2);
	}

}

//------------------------------------------------------------------------------
