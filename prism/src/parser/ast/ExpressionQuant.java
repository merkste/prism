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
import parser.Values;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;

// FIXME LR: What is minMax for?
/**
 * Abstract class for representing "quantitative" operators (P,R,S),
 * i.e., a superclass of:
 * <ul>
 *     <li>{@link ExpressionLongRun}</li>
 *     <li>{@link ExpressionProb}</li>
 *     <li>{@link ExpressionReward}</li>
 *     <li>{@link ExpressionSS}</li>
 * </ul>
 */
public abstract class ExpressionQuant<E extends Expression> extends Expression
{
	/** Optional "modifier" to specify variants of the P/R/S operator */
	protected String modifier = null;
	/** The attached relational operator (e.g. "&lt;" in "P&lt;0.1"). */
	protected RelOp relOp = null;
	/** The attached (probability/reward) bound, as an expression (e.g. "p" in "P&lt;p"). Null if absent (e.g. "P=?"). */
	protected Expression bound = null;
	/** The main operand of the operator (e.g. "F target=true" in "P&lt;0.1[F target=true]. */
	protected E expression = null;
	/** Optional "old-style" filter. This is just for display purposes since
	  *  the parser creates an (invisible) new-style filter around this expression. */
	protected Filter filter = null;

	// Constructors

	protected ExpressionQuant()
	{
	}

	protected ExpressionQuant(E expression, String relOpString, Expression bound)
	{
		this(expression, RelOp.parseSymbol(relOpString), bound);
	}

	protected ExpressionQuant(E expression, RelOp relOp, Expression bound)
	{
		this.expression = expression;
		this.relOp = relOp;
		this.bound = bound;
	}

	// Set methods

	/**
	 * Set the (optional) "modifier" for this operator.
	 */
	public void setModifier(String modifier)
	{
		this.modifier = modifier;
	}

	/**
	 * Set the attached relational operator (e.g. "&lt;" in "P&lt;0.1").
	 * Uses the enum {@link RelOp}. For example: {@code setRelOp(RelOp.GT);}
	 */
	public void setRelOp(RelOp relOp)
	{
		this.relOp = relOp;
	}

	/**
	 * Set the attached relational operator (e.g. "&lt;" in "P&lt;0.1").
	 * The operator is passed as a string, e.g. "&lt;" or "&gt;=".
	 */
	public void setRelOp(String relOpString)
	{
		relOp = RelOp.parseSymbol(relOpString);
	}

	/**
	 * Set the attached bound, as an expression (e.g. "p" in "P&lt;p"). Should be null if absent (e.g. "P=?").
	 */
	public void setBound(Expression bound)
	{
		this.bound = bound;
	}

	/**
	 * Set the main operand of the operator (e.g. "F target=true" in "P&lt;0.1[F target=true].
	 */
	public void setExpression(E expression)
	{
		this.expression = expression;
	}

	/**
	 * Set the optional "old-style" filter. This is just for display purposes since
	 * the parser creates an (invisible) new-style filter around this expression.
	 */
	public void setFilter(Filter f)
	{
		filter = f;
	}

	// Get methods

	/**
	 * Get the (optional) "modifier" for this operator.
	 */
	public String getModifier()
	{
		return modifier;
	}

	/**
	 * Get a string representing the modifier as a suffix for the operator.
	 */
	public String getModifierString()
	{
		return modifier == null ? "" : "(" + modifier + ")";
	}

	/**
	 * Get the attached relational operator (e.g. "&lt;" in "P&lt;0.1"), as a {@link RelOp}.
	 */
	public RelOp getRelOp()
	{
		return relOp;
	}

	/**
	 * Get the attached bound, as an expression (e.g. "p" in "P&lt;p"). Should be null if absent (e.g. "P=?").
	 */
	public Expression getBound()
	{
		return bound;
	}

	/**
	 * Get the main operand of the operator (e.g. "F target=true" in "P&lt;0.1[F target=true].
	 */
	public E getExpression()
	{
		return expression;
	}

	/**
	 * Get an object storing info about the attached relational operator and bound, after evaluating the bound to a double.
	 * For example "&lt;0.1" in "P&lt;p" where p=0.5 in {@code constantValues}.
	 * Does some checks, e.g., throws an exception if a probability is not in the range [0,1]
	 * 
	 * @param constantValues Values for constants in order to evaluate any bound
	 */
	public abstract OpRelOpBound getRelopBoundInfo(Values constantValues) throws PrismException;

	/**
	 * Get the optional "old-style" filter. This is just for display purposes since
	 * the parser creates an (invisible) new-style filter around this expression.
	 */
	public Filter getFilter()
	{
		return filter;
	}

	// Test methods

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

	public boolean isQuantitative()
	{
		return getBound() == null;
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	// Methods required for Expression:

	@Override
	public Object evaluate(final EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a quantitative expression without a model");
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a quantitative expression without a model");
	}

	// Standard methods

	@Override
	public String toString()
	{
		return operatorToString() + boundsToString() + " [ " + bodyToString() + " ]";
	}

	protected abstract String operatorToString();

	protected String boundsToString()
	{
		String bounds = getBound() == null ? "?" : getBound().toString();
		return getRelOp() + bounds;
	}

	protected String bodyToString()
	{
		String filter = getFilter() == null ? "" : " " + getFilter();
		return getExpression() + filter;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bound == null) ? 0 : bound.hashCode());
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + ((modifier == null) ? 0 : modifier.hashCode());
		result = prime * result + ((relOp == null) ? 0 : relOp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpressionQuant<?> other = (ExpressionQuant<?>) obj;
		if (bound == null) {
			if (other.bound != null)
				return false;
		} else if (!bound.equals(other.bound))
			return false;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.equals(other.expression))
			return false;
		if (filter == null) {
			if (other.filter != null)
				return false;
		} else if (!filter.equals(other.filter))
			return false;
		if (modifier == null) {
			if (other.modifier != null)
				return false;
		} else if (!modifier.equals(other.modifier))
			return false;
		if (relOp != other.relOp)
			return false;
		return true;
	}
}

//------------------------------------------------------------------------------
