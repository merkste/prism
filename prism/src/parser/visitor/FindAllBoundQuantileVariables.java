//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Joachim Klein
//	* Marcus Daum
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

import parser.ast.ExpressionBoundVariable;
import parser.ast.ExpressionConstant;
import parser.ast.ExpressionIdent;
import parser.ast.ExpressionQuantileProb;
import parser.ast.ExpressionQuantileProbNormalForm;
import parser.type.TypeInt;
import prism.PrismLangException;

/**
 * Find all idents which are constants, replace with ExpressionConstant, return result.
 */
public class FindAllBoundQuantileVariables extends ASTTraverseModify
{
	private String quantileVariableName = null;

	public void visitPre(ExpressionQuantileProb e) throws PrismLangException {
		if (quantileVariableName != null) {
			throw new PrismLangException("Nested quantiles are not supported");
		}
		quantileVariableName = e.getQuantileVariable();
	}
	
	public void visitPost(ExpressionQuantileProb e) {
		quantileVariableName = null;
	}
	
	public void visitPre(ExpressionQuantileProbNormalForm e) throws PrismLangException {
		if (quantileVariableName != null) {
			throw new PrismLangException("Nested quantiles are not supported");
		}
		quantileVariableName = "_q";
	}
	
	public void visitPost(ExpressionQuantileProbNormalForm e) {
		quantileVariableName = null;
	}
	
	
	public Object visit(ExpressionIdent e) throws PrismLangException
	{
		if (quantileVariableName == null) return e;
		
		// See if identifier corresponds to the quantile variable
		if (e.getName().equals(quantileVariableName)) {
			// If so, replace it with an ExpressionBoundVariable object
			ExpressionConstant expr = new ExpressionBoundVariable(e.getName(), TypeInt.getInstance());
			expr.setPosition(e);
			return expr;
		}

		// Otherwise, leave it unchanged
		return e;
	}
}
