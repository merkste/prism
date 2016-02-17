//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

import parser.type.*;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class DeclarationIntView extends DeclarationType
{
	// Min value
	protected Expression low;
	// Max value
	protected Expression high;

	protected List<ExpressionVar> bits = new ArrayList<ExpressionVar>();

	public DeclarationIntView(Expression low, Expression high)
	{
		this.low = low;
		this.high = high;
		// The type stored for a Declaration/DeclarationType object
		// is static - it is not computed during type checking.
		// (But we re-use the existing "type" field for this purpose)
		setType(TypeInt.getInstance());
	}

	public void setLow(Expression l)
	{
		low = l;
	}

	public void setHigh(Expression h)
	{
		high = h;
	}

	public Expression getLow()
	{
		return low;
	}

	public Expression getHigh()
	{
		return high;
	}

	public List<ExpressionVar> getBits()
	{
		return bits;
	}

	public void addBit(ExpressionVar bit)
	{
		bits.add(bit);
	}

	public void setBits(List<ExpressionVar> bits)
	{
		this.bits = bits;
	}

	/**
	 * Return the default start value for a variable of this type.
	 */
	public Expression getDefaultStart()
	{
		return low;
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
	@Override
	public String toString()
	{
		String result = "view (";
		boolean first = true;
		for (ExpressionVar bit : bits) {
			if (!first) result+=",";
			first = false;
			result+=bit.getName();
		}
		result += ") <=> [" + low + ".." + high + "]";
		return result;
	}

	/**
	 * Perform a deep copy.
	 */
	@Override
	public ASTElement deepCopy()
	{
		Expression lowCopy = (low == null) ? null : low.deepCopy();
		Expression highCopy = (high == null) ? null : high.deepCopy();
		DeclarationIntView ret = new DeclarationIntView(lowCopy, highCopy);
		for (ExpressionVar bit : bits) {
			ret.addBit((ExpressionVar) bit.deepCopy());
		}
		ret.setPosition(this);
		return ret;
	}
}
