//==============================================================================
//	
//	Copyright (c) 2017-
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

package parser.visitor;

import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConstant;
import parser.ast.ExpressionExists;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionForAll;
import parser.ast.ExpressionFormula;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionITE;
import parser.ast.ExpressionIdent;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionSS;
import parser.ast.ExpressionStrategy;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.ast.ExpressionVar;
import parser.type.TypeBool;
import prism.PrismLangException;

/**
 * Base class for recursively traversing an Expression, only visiting
 * the top-level LTL operators, i.e., not entering any maximal state subformulas.
 *
 * <br/>
 * Subclasses should not provide visitLTL methods and not override any of the {@code visitNow()}
 * methods of ASTTraverse.
 */
public abstract class ExpressionTraverseLTL extends ASTTraverse
{
	abstract public void visitLTL(ExpressionBinaryOp e) throws PrismLangException;

	abstract public void visitLTL(ExpressionTemporal e) throws PrismLangException;

	abstract public void visitLTL(ExpressionUnaryOp e) throws PrismLangException;

	@Override
	public Object visitNow(ExpressionBinaryOp e) throws PrismLangException
	{
		if (e.getType() == TypeBool.getInstance()) {
			// As type is boolean, this is the root of a maximal state formula;
			// don't visit, don't visit children
			return null;
		} else {
			visitLTL(e);
			return super.visitNow(e);
		}
	}
	@Override
	public Object visitNow(ExpressionConstant e) throws PrismLangException
	{
		// don't visit
		return null;
	}

	@Override
	public Object visitNow(ExpressionExists e) throws PrismLangException
	{
		// don't visit, don't visit children
		return null;
	}

	@Override
	public Object visitNow(ExpressionFilter e) throws PrismLangException
	{
		// don't visit, don't visit children
		return null;
	}

	@Override
	public Object visitNow(ExpressionForAll e) throws PrismLangException
	{
		// don't visit, don't visit children
		return null;
	}

	@Override
	public Object visitNow(ExpressionFormula e) throws PrismLangException
	{
		// don't visit, don't visit children
		return null;
	}

	@Override
	public Object visitNow(ExpressionFunc e) throws PrismLangException
	{
		// don't visit, don't visit children
		return null;
	}

	@Override
	public Object visitNow(ExpressionIdent e) throws PrismLangException
	{
		// don't visit
		return null;
	}

	@Override
	public Object visitNow(ExpressionITE e) throws PrismLangException
	{
		if (e.getType() == TypeBool.getInstance()) {
			// As type is boolean, this is the root of a maximal state formula;
			// don't visit children
			return null;
		} else {
			e.getOperand2().accept(this);
			e.getOperand3().accept(this);
			return this;
		}
	}

	@Override
	public Object visitNow(ExpressionLabel e) throws PrismLangException
	{
		// don't visit, don't visit children
		return null;
	}

	@Override
	public Object visitNow(ExpressionProb e) throws PrismLangException
	{
		// don't visit, don't visit children
		return null;
	}

	@Override
	public Object visitNow(ExpressionReward e) throws PrismLangException
	{
		// don't visit, don't visit children
		return null;
	}

	@Override
	public Object visitNow(ExpressionSS e) throws PrismLangException
	{
		// don't visit, don't visit children
		return null;
	}

	@Override
	public Object visitNow(ExpressionStrategy e) throws PrismLangException
	{
		// don't visit, don't visit children
		return null;
	}

	@Override
	public Object visitNow(ExpressionTemporal e) throws PrismLangException
	{
		visitLTL(e);
		return super.visitNow(e);
	}

	@Override
	public Object visitNow(ExpressionUnaryOp e) throws PrismLangException
	{
		if (e.getType() == TypeBool.getInstance()) {
			// As type is boolean, this is the root of a maximal state formula;
			// don't visit, don't visit children
			return null;
		} else {
			visitLTL(e);
			return super.visitNow(e);
		}
	}

	@Override
	public Object visitNow(ExpressionVar e) throws PrismLangException
	{
		// don't visit
		return null;
	}

}
