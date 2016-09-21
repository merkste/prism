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

import java.util.Collections;
import java.util.Vector;

import jdd.JDDVars;
import parser.ast.Declaration;
import parser.ast.Module;
import parser.ast.ModulesFile;
import prism.Model;

/** Visitor that reorders
 *  (1) the modules in a ModulesFile and
 *  (2) the variable declarations in a Module
 * according to the current variable ordering in the MTBDD
 * of the symbolic model.
 */
public class Reorder extends ASTTraverse
{
	/** The model */
	private Model model;

	/* A pair (index,level) that is comparable according to level. */
	private static class IndexWithLevel implements Comparable<IndexWithLevel> {
		public int index;
		public int order;

		public IndexWithLevel(int index, int order)
		{
			this.index = index;
			this.order = order;
		}

		@Override
		public int compareTo(IndexWithLevel o)
		{
			return Integer.compare(this.order, o.order);
		}
	}

	/** Constructor */
	public Reorder(Model model) {
		this.model = model;
	}

	@Override
	public void visitPost(ModulesFile mf) {
		Vector<IndexWithLevel> order = new Vector<IndexWithLevel>();
		Vector<Module> modules = new Vector<Module>();

		for (int i=0;i<mf.getNumModules();i++) {
			int level = getMinLevel(model.getModuleDDRowVars(i));
			order.add(new IndexWithLevel(i, level));
			modules.add(mf.getModule(i));
		}

		Collections.sort(order);

		for (int j=0;j<order.size();j++) {
			mf.setModule(j, modules.get(order.get(j).index));
		}

		// global variables

		order.clear();
		Vector<Declaration> declarations = new Vector<Declaration>();

		for (int i=0;i<mf.getNumGlobals();i++) {
			int varIndex = model.getVarList().getIndexFromDeclaration(mf.getGlobal(i));
			JDDVars varVars = model.getVarDDRowVars(varIndex);

			int level = getMinLevel(varVars);
			order.add(new IndexWithLevel(i, level));
			declarations.add(mf.getGlobal(i));
		}

		Collections.sort(order);

		for (int j=0;j<order.size();j++) {
			mf.setGlobal(j, declarations.get(order.get(j).index));
		}

	}

	@Override
	public void visitPost(Module module) {
		Vector<IndexWithLevel> order = new Vector<IndexWithLevel>();
		Vector<Declaration> declarations = new Vector<Declaration>();
		int moduleIndex=module.getParent().getModuleIndex(module.getName());

		int curDDIndex = 0;
		JDDVars moduleVars = model.getModuleDDRowVars(moduleIndex);
		for (int i=0;i<module.getNumDeclarations();i++) {
			int varIndex = model.getVarList().getIndexFromDeclaration(module.getDeclaration(i));
			int varSize = model.getVarList().getRangeLogTwo(varIndex);

			JDDVars varVars = new JDDVars();
			for (int j=0;j<varSize;j++) {
				varVars.addVar(moduleVars.getVar(curDDIndex + j));
			}

			int level = getMinLevel(varVars);
			order.add(new IndexWithLevel(i, level));
			declarations.add(module.getDeclaration(i));

			curDDIndex += varSize;
		}

		Collections.sort(order);

		for (int j=0;j<order.size();j++) {
			module.setDeclaration(j, declarations.get(order.get(j).index));
		}
	}

	/**
	 * Get the minimum variable level for the JDDVars.
	 * Returns -1 if vars is empty.
	 */
	private static int getMinLevel(JDDVars vars)
	{
		Integer level = null;
		for (int i=0;i<vars.n();i++) {
			int varLevel = vars.getVar(i).getLevel();
			if (level == null || level > varLevel) {
				level = varLevel;
			}
		}

		if (level == null) {
			return level = -1;
		}

		return level;
	}
}
