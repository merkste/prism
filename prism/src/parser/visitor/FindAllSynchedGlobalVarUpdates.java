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

package parser.visitor;

import java.util.TreeMap;

import parser.ast.*;
import prism.PrismLangException;

/**
 * Find the modules for all global variables that are updated in some synchronizing action.
 * <br>
 * Currently, updating global variables in synchronizing actions is restricted such
 * that for each global variable there is at most one module that updates
 * the variable in a synchronizing action.
 * <br>
 * This visitor constructs a map "global variable name" to "module name", providing
 * the relevant module per global variable.
 */
public class FindAllSynchedGlobalVarUpdates extends ASTTraverse
{
	/** The result map */
	private TreeMap<String,String> globalVarToModule = new TreeMap<String,String>();

	/** In which module are we currently? */
	private Module inModule = null;

	/** Constructor */
	public FindAllSynchedGlobalVarUpdates()
	{
	}

	/** Return the result map */
	public TreeMap<String,String> getGlobalVarToModule()
	{
		return globalVarToModule;
	}

	@Override
	public void visitPre(Module e) throws PrismLangException
	{
		// Register the fact we are entering a module
		inModule = e;
	}

	@Override
	public void visitPost(Module e) throws PrismLangException
	{
		// Register the fact we are leaving a module
		inModule = null;
	}

	@Override
	public void visitPost(Update e) throws PrismLangException
	{
		int i, n;
		String var;
		Command c;
		Module m;
		ModulesFile mf;
		boolean isLocal, isGlobal, isGlobalView;

		// Determine containing command/module/model
		// (mf should coincide with the stored modulesFile)
		c = e.getParent().getParent();
		m = c.getParent();
		mf = m.getParent();
		n = e.getNumElements();
		for (i = 0; i < n; i++) {
			// Check that the update is allowed to modify this variable
			var = e.getVar(i);
			isLocal = m.isLocalVariable(var);
			isGlobal = isLocal ? false : mf.isGlobalVariable(var);
			isGlobalView = isLocal ? false : mf.isGlobalView(var);
			if (!c.getSynch().equals("")) {
				if (isGlobal) {
					if (globalVarToModule.containsKey(var) && globalVarToModule.get(var) != inModule.getName()) {
						throw new PrismLangException("To update global variable "+var+", only one module may update (previously by "+globalVarToModule.get(var)+")",
								e.getVarIdent(i));
					}
					globalVarToModule.put(var, inModule.getName());
				} else if (isGlobalView) {
					// for an update to a global view, we add each underlying variable
					// to the mapping
					Declaration view = mf.getGlobalView(var);
					DeclarationIntView viewType = (DeclarationIntView) view.getDeclType();
					for (ExpressionVar bit : viewType.getBits()) {
						String bitName = bit.getName();
						if (globalVarToModule.containsKey(bitName) && globalVarToModule.get(bitName) != inModule.getName()) {
							throw new PrismLangException("To update global view "+ var +", only one module may update " + bitName + " (previously by "+globalVarToModule.get(bitName)+")",
							                             e.getVarIdent(i));
						}
						globalVarToModule.put(bitName, inModule.getName());
					}
				}
			}
		}
	}
}
