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

package parser.visitor;

import java.util.Vector;

import parser.ast.*;
import prism.PrismLangException;

/**
 * Find all references to variables, replace any identifier objects with variable objects,
 * check variables exist and store their index (as defined by the containing ModuleFile).
 */
public class FindAllViews extends ASTTraverseModify
{
	private Vector<String> viewIdents;
	private Vector<Declaration> viewDeclarations;

	public FindAllViews(Vector<String> varIdents, Vector<Declaration> varDeclarations)
	{
		this.viewIdents = varIdents;
		this.viewDeclarations = varDeclarations;
	}


	// Note that this is done with VisitPost, i.e. after recursively visiting children.
	// This is ok because we can modify rather than create a new object so don't need to return it.
	public void visitPost(Update e) throws PrismLangException
	{
		int i, j, n;
		// For each element of update
		n = e.getNumElements();
		for (i = 0; i < n; i++) {
			// Check variable exists
			j = viewIdents.indexOf(e.getVar(i));
			if (j == -1) {
				// might be a variable, so just ignore, will be handled by FindAllVars
				continue;
			}
			// Store the type
			e.setType(i, viewDeclarations.elementAt(j).getType());

			// set var index to -2 to indicate view
			e.setVarIndex(i, -2);
		}
	}
	
	public Object visit(ExpressionVar e) throws PrismLangException
	{
		ExpressionViewVar result = getView(e.getName());
		if (result != null) {
			result.setPosition(e);
			return result;
		}
		// leave unchanged
		return e;
	}

	public Object visit(ExpressionIdent e) throws PrismLangException
	{
		ExpressionViewVar result = getView(e.getName());
		if (result != null) {
			result.setPosition(e);
			return result;
		}
		// leave unchanged
		return e;
	}
	
	private ExpressionViewVar getView(String name) throws PrismLangException
	{
		int i;
		// See if identifier corresponds to a variable
		i = viewIdents.indexOf(name);
		if (i != -1) {
			// If so, replace it with an ExpressionViewVar object
			ExpressionViewVar expr = new ExpressionViewVar(name, viewDeclarations.elementAt(i).getType());
			DeclarationIntView decl = (DeclarationIntView) viewDeclarations.elementAt(i).getDeclType();
			expr.setLow(decl.getLow());
			for (ExpressionVar bit : decl.getBits()) {
				expr.addBit((ExpressionVar) bit.deepCopy());
			}
			return expr;
		} else {
			return null;
		}
	}
}

