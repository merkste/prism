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

import parser.EvaluateContext;
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
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

	// Operator
	protected int op = 0;
	// Up to two operands (either may be null)
	protected Expression operand1 = null; // LHS of operator
	protected Expression operand2 = null; // RHS of operator

	// the bounds
	protected TemporalOperatorBounds bounds = new TemporalOperatorBounds();

	// Constructors

	public ExpressionTemporal()
	{
	}

	public ExpressionTemporal(int op, Expression operand1, Expression operand2)
	{
		this.op = op;
		this.operand1 = operand1;
		this.operand2 = operand2;
	}

	// Set methods

	public void setOperator(int i)
	{
		op = i;
	}

	public void setOperand1(Expression e1)
	{
		operand1 = e1;
	}

	public void setOperand2(Expression e2)
	{
		operand2 = e2;
	}

	// Get methods

	public int getOperator()
	{
		return op;
	}

	public String getOperatorSymbol()
	{
		return opSymbols[op];
	}

	public Expression getOperand1()
	{
		return operand1;
	}

	public Expression getOperand2()
	{
		return operand2;
	}

	public int getNumOperands()
	{
		if (operand1 == null)
			return 0;
		else
			return (operand2 == null) ? 1 : 2;
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
	public ExpressionTemporal deepCopy(DeepCopy copier) throws PrismLangException
	{
		operand1 = copier.copy(operand1);
		operand2 = copier.copy(operand2);
		bounds   = copier.copy(bounds);

		return this;
	}
	
	public void setBounds(TemporalOperatorBounds bounds) {
		this.bounds = bounds;
	}

	public TemporalOperatorBounds getBounds() {
		return bounds;
	}

	@Override
	public ExpressionTemporal clone()
	{
		return (ExpressionTemporal) super.clone();
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
}

//------------------------------------------------------------------------------
