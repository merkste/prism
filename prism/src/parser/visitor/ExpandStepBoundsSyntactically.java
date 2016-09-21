//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package parser.visitor;

import parser.Values;
import parser.ast.*;
import prism.IntegerBound;
import prism.PrismException;
import prism.PrismLangException;


/**
 * Visitor that modifies an AST by expanding bounds on temporal operators in LTL path operators
 * by a syntactic expansion, converting into disjunctions / conjunctions with next-step operators.
 */
public class ExpandStepBoundsSyntactically extends ASTTraverseModify
{
	/** Values for constants in the AST */
	private Values constantValues;

	/**
	 * Constructor.
	 * @param constantValues Values for constants (may be {@code null})xs
	 */
	public ExpandStepBoundsSyntactically(Values constantValues)
	{
		this.constantValues = constantValues;
	}

	@Override
	public Object visit(ExpressionTemporal e) throws PrismLangException
	{
		IntegerBound bounds;
		Integer lowerBound;
		Expression ltl1, ltl2;
		Expression result;

		// no bounds, do default visiting
		if (!e.hasBounds()) return super.visit(e);

		if (e.getOperator() == ExpressionTemporal.P_R) {
			// recurse on the negated until form...
			return e.convertToUntilForm().accept(this);
		}

		try {
			// obtain bounds
			bounds = IntegerBound.fromExpressionTemporal(e, constantValues, true);
		} catch (PrismException exception) {
			throw new PrismLangException(exception.getMessage(), e);
		}

		if (bounds.hasLowerBound()) {
			lowerBound = bounds.getLowestInteger();
		} else {
			lowerBound = 0;
		}

		Integer windowSize = null;  // unbounded
		if (bounds.hasUpperBound()) {
			windowSize = bounds.getHighestInteger() - lowerBound;
		}

		ltl1 = e.getOperand1();
		ltl2 = e.getOperand2();

		if (ltl1 != null) ltl1 = (Expression)ltl1.accept(this);
		if (ltl2 != null) ltl2 = (Expression)ltl2.accept(this);

		if (windowSize == null) {
			// no upper bound, convert to unbounded operator
			result = new ExpressionTemporal(e.getOperator(), ltl1, ltl2);
		} else {
			// upper bound, convert to next-step based
			switch (e.getOperator()) {
			case ExpressionTemporal.P_X:
			case ExpressionTemporal.P_W:
			case ExpressionTemporal.P_R:
				throw new PrismLangException("LTL operator "+e.getOperatorSymbol()+" with bounds is not supported!");
			case ExpressionTemporal.P_F:
			case ExpressionTemporal.P_G:
				int connective = (e.getOperator() == ExpressionTemporal.P_F ? ExpressionBinaryOp.OR : ExpressionBinaryOp.AND);
				result = ltl2.deepCopy();
				for (int i = 0; i < windowSize; i++) {
					Expression tmp = Expression.Parenth(result);
					result = new ExpressionBinaryOp(connective,
					                                Expression.Parenth(ltl2.deepCopy()),
					                                new ExpressionTemporal(ExpressionTemporal.P_X, null, tmp));
				}
				break;
			case ExpressionTemporal.P_U:
				result = ltl2.deepCopy();
				for (int i = 0; i < windowSize; i++) {
					Expression tmp = Expression.Parenth(result);
					tmp = Expression.And(Expression.Parenth(ltl1.deepCopy()),
					                     new ExpressionTemporal(ExpressionTemporal.P_X, null, tmp));
					tmp = Expression.Parenth(tmp);
					result = Expression.Or(Expression.Parenth(ltl2.deepCopy()),
					                       tmp);
				}
				break;
			default:
				throw new PrismLangException("Cannot expand temporal bounds for "+e.getOperatorSymbol());
			}
		}

		// preface result expression with lowerBound next-step operators
		if (lowerBound > 0) {
			result = Expression.Parenth(result);
			for (int i = 0; i < lowerBound; i++) {
				switch (e.getOperator()) {
				case ExpressionTemporal.P_F:
				case ExpressionTemporal.P_G:
					result = new ExpressionTemporal(ExpressionTemporal.P_X, null, result);
					break;
				case ExpressionTemporal.P_U:
					result = Expression.And(Expression.Parenth(ltl1.deepCopy()),
					                        new ExpressionTemporal(ExpressionTemporal.P_X, null, Expression.Parenth(result)));
					break;
				default:
					throw new PrismLangException("Cannot expand temporal bounds for "+e.getOperatorSymbol());
				}
			}
		}

		return Expression.Parenth(result);
	}
}
