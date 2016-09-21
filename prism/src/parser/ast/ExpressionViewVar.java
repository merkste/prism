//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import java.util.ArrayList;
import java.util.List;

import param.BigRational;
import parser.*;
import parser.visitor.*;
import prism.PrismLangException;
import parser.type.*;

public class ExpressionViewVar extends Expression
{
	// Variable name
	private String name;
	private ArrayList<ExpressionVar> bits = new ArrayList<ExpressionVar>();
	private Expression low;

	// Constructors

	public ExpressionViewVar(String n, Type t)
	{
		setType(t);
		name = n;
	}

	// Set method

	public void setName(String n)
	{
		name = n;
	}

	public void setLow(Expression expression)
	{
		this.low = expression;
	}

	public void addBit(ExpressionVar bit)
	{
		bits.add(bit);
	}

	public void setBit(int i, ExpressionVar bit)
	{
		bits.set(i, bit);
	}

	// Get method

	public String getName()
	{
		return name;
	}

	public Expression getLow()
	{
		return low;
	}

	public List<ExpressionVar> getBits()
	{
		return bits;
	}

	public ExpressionVar getBit(int i)
	{
		return bits.get(i);
	}

	public int getNumBits()
	{
		return bits.size();
	}

	// Methods require for Expression:

	/**
	 * Is this expression constant?
	 */
	public boolean isConstant()
	{
		return false;
	}

	@Override
	public boolean isProposition()
	{
		return true;
	}

	/**
	 * Evaluate this expression, return result.
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		int value = 0;
		for (ExpressionVar v : bits) {
			value = value << 1;
			Integer bit = v.evaluateInt(ec);
			if (bit == null || (int)bit<0 || (int)bit>1)
				throw new PrismLangException("Could not evaluate variable", this);
			value += bit;
		}

		return new Integer(value + low.evaluateInt(ec));
	}

	/**
	 * Evaluate this expression, return result.
	 * Note: assumes that type checking has been done already.
	 */
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		return BigRational.from(evaluateInt(ec));
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	// Methods required for ASTElement:

	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	/**
	 * Convert to string.
	 */
	public String toString()
	{
		return name;
	}

	/**
	 * Perform a deep copy.
	 */
	public Expression deepCopy()
	{
		ExpressionViewVar expr = new ExpressionViewVar(name, type);
		expr.setLow(low);
		expr.setPosition(this);
		for (ExpressionVar bit : bits) {
			expr.addBit((ExpressionVar) bit.deepCopy());
		}
		return expr;
	}

}

//------------------------------------------------------------------------------
