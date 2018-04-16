//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
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

import parser.EvaluateContext;
import parser.type.TypePathBool;
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
import prism.PrismLangException;
import prism.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * An path expression where the set of paths is determined by
 * a HOA automaton, stored in a file.
 *
 * An optional list of renames allows for atomic propositions in
 * the automaton to be associated with sets of states satisfying an expression
 * in the model. Non-renamed atomic propositions are treated as labels in the model.
 */
public class ExpressionHOA extends Expression
{
	/** The automaton file name */
	private QuotedString automatonFile;

	/**
	 * A list of (AP, expression) pairs, relating atomic propositions
	 * in the automaton with expressions in the model.
	 */
	private ArrayList<Pair<String, Expression>> apRenames;

	/** Constructor. */
	public ExpressionHOA(QuotedString automatonFile)
	{
		this.automatonFile = automatonFile;
		apRenames = new ArrayList<Pair<String, Expression>>();
		setType(TypePathBool.getInstance());
	}

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
		if (!(other instanceof ExpressionHOA))
			return false;

		ExpressionHOA otherHOA = (ExpressionHOA) other;

		return apRenames.equals(otherHOA.apRenames);
	}

	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a HOA path operator without a path");
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	@Override
	public ExpressionHOA deepCopy(DeepCopy copier) throws PrismLangException
	{
		automatonFile = copier.copy(automatonFile);
		// copy pairs individually to new list
		ArrayList<Pair<String, Expression>> oldRenames = apRenames;
		apRenames = new ArrayList<>(apRenames.size());
		for (Pair<String, Expression> rename : oldRenames) {
			addRename(rename.getKey(), copier.copy(rename.getValue()));
		}

		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ExpressionHOA clone()
	{
		ExpressionHOA clone = (ExpressionHOA) super.clone();

		clone.apRenames = (ArrayList<Pair<String, Expression>>) apRenames.clone();

		return clone;
	}

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("HOA: { ");
		sb.append(automatonFile != null ? automatonFile : "");
		for (Pair<String, Expression> rename : apRenames) {
			sb.append(", \"");
			sb.append(rename.getKey());
			sb.append("\" <- ");
			sb.append(rename.getValue());
		}
		sb.append(" }");
		return sb.toString();
	}

	/** Get the automaton filename. */
	public QuotedString getAutomatonFile()
	{
		return automatonFile;
	}

	/** Set the automaton filename. */
	public void setAutomatonFile(QuotedString automatonFile)
	{
		this.automatonFile = automatonFile;
	}

	/** Add a rename from an atomic proposition to an expression in the model */
	public void addRename(String ap, Expression expression)
	{
		apRenames.add(new Pair<String, Expression>(ap, expression));
	}

	/** Get the list of renames */
	public List<Pair<String, Expression>> getRenames()
	{
		return apRenames;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((automatonFile == null) ? 0 : automatonFile.shallowHashCode());
		result = prime * result + ((apRenames == null) ? 0 : apRenames.hashCode());
		return result;
	}

	@Override
	public int shallowHashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((automatonFile == null) ? 0 : automatonFile.getClass().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ExpressionHOA))
			return false;
		ExpressionHOA other = (ExpressionHOA) obj;
		if (automatonFile == null) {
			if (other.automatonFile != null)
				return false;
		} else if (!automatonFile.equals(other.automatonFile))
			return false;
		if (apRenames == null) {
			if (other.apRenames != null)
				return false;
		} else if (!apRenames.equals(other.apRenames))
			return false;
		return true;
	}

}
