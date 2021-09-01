//==============================================================================
//
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
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

import java.util.Objects;

import param.BigRational;
import parser.EvaluateContext;
import parser.Values;
import parser.visitor.ASTVisitor;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;

public class ExpressionLongRun extends ExpressionQuant<Expression>
{
	protected Expression states;

	// Constructors

	public ExpressionLongRun(Expression expression, Expression states, String relOpString, Expression bound)
	{
		super(expression, relOpString, bound);
		this.states = Objects.requireNonNull(states);
	}

	public ExpressionLongRun(Expression expression, Expression states, RelOp relOp, Expression bound)
	{
		super(expression, relOp, bound);
		this.states = Objects.requireNonNull(states);
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
			return new OpRelOpBound("L", getRelOp(), boundValue);
		} else {
			return new OpRelOpBound("L", getRelOp(), null);
		}
	}

	@Override
	public String getResultName()
	{
		return (getBound() == null) ? expression.getResultName() : "Result";
	}

//	// Test methods
//
//	// FIXME LR: Override to true in ExpressionLongRun?
//	@Override
//	public boolean returnsSingleValue()
//	{
//		return true;
//	}

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public ExpressionLongRun deepCopy()
	{
		Expression expressionCopy = expression == null ? null : expression.deepCopy();
		Expression statesCopy = states == null ? null : states.deepCopy();
		Expression boundCopy = bound == null ? null : bound.deepCopy();
		ExpressionLongRun copy = new ExpressionLongRun(expressionCopy, statesCopy, relOp, boundCopy);

		copy.setFilter(getFilter() == null ? null : (Filter)getFilter().deepCopy());
		copy.setType(type);
		copy.setPosition(this);
		return copy;
	}

	// Standard methods

	@Override
	public String toString()
	{
		// FIXME LR: Why do we not print relop and bounds?
		return operatorToString() + "[" + bodyToString() + "]";
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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		result = prime * result + ((states == null) ? 0 : states.hashCode());
		return result;
	}

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
