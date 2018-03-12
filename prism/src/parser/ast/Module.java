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

import java.util.*;

import parser.visitor.*;
import prism.PrismLangException;

public class Module extends ASTElement
{
	// Module name
	private String name;
	private ExpressionIdent nameASTElement;
	// Local variables
	private ArrayList<Declaration> decls;
	// Local views
	private ArrayList<Declaration> views;
	// Commands
	private ArrayList<Command> commands;
	// Invariant (PTA models only; optional)
	private Expression invariant;
	// Parent ModulesFile
	private ModulesFile parent;
	// Base module (if was constructed through renaming; null if not)
	private String baseModule;

	// Constructor
	
	public Module(String n)
	{
		name = n;
		decls = new ArrayList<Declaration>();
		views = new ArrayList<Declaration>();
		commands = new ArrayList<Command>();
		invariant = null;
		parent = null;
		baseModule = null;
	}

	// Set methods
	
	public void setName(String n)
	{
		name = n;
	}
	
	public void setNameASTElement(ExpressionIdent e)
	{
		nameASTElement = e;
	}
	
	public void addDeclaration(Declaration d)
	{
		decls.add(d);
	}
	
	public void setDeclaration(int i, Declaration d)
	{
		decls.set(i, d);
	}

	public void addViewDeclaration(Declaration d)
	{
		assert (d.isView());
		views.add(d);
	}

	public void setViewDeclaration(int i, Declaration d)
	{
		assert (d.isView());
		views.set(i, d);
	}

	public void addCommand(Command c)
	{
		commands.add(c);
		c.setParent(this);
	}
	
	public void removeCommand(Command c)
	{
		commands.remove(c);
	}
	
	public void setCommand(int i, Command c)
	{
		commands.set(i, c);
		c.setParent(this);
	}
	
	public void setInvariant(Expression e)
	{
		invariant = e;
	}
	
	public void setParent(ModulesFile mf)
	{
		parent = mf;
	}

	public void setBaseModule(String b)
	{
		baseModule = b;
	}
	
	// Get methods
	
	public String getName()
	{
		return name;
	}
	
	public ExpressionIdent getNameASTElement()
	{
		return nameASTElement;
	}
	
	/**
	 * Get the number of local variable declarations. 
	 */
	public int getNumDeclarations()
	{
		return decls.size();
	}
	
	/**
	 * Get the ith local variable declaration. 
	 */
	public Declaration getDeclaration(int i)
	{
		return decls.get(i);
	}
	
	/**
	 * Get the list of all local view declarations.
	 */
	public ArrayList<Declaration> getViewDeclarations()
	{
		return views;
	}

	/**
	 * Get the number of local view declarations.
	 */
	public int getNumViewDeclarations()
	{
		return views.size();
	}

	/**
	 * Get the i-th local view declaration.
	 */
	public Declaration getViewDeclaration(int i)
	{
		return views.get(i);
	}

	/**
	 * Get the list of all local variable declarations.
	 */
	public List<Declaration> getDeclarations()
	{
		return decls;
	}

	/**
	 * Check for the existence of a local variable (declaration).
	 */
	public boolean isVariableName(String var)
	{
		for (Declaration decl: decls) {
			if (decl.getName().equals(var))
				return true;
		}
		return false;
	}
	
	public int getNumCommands()
	{
		return commands.size();
	}
	
	public Command getCommand(int i)
	{
		return commands.get(i);
	}
	
	public List<Command> getCommands()
	{
		return commands;
	}
	
	public Expression getInvariant()
	{
		return invariant;
	}
	
	public ModulesFile getParent()
	{
		return parent;
	}
	
	public String getBaseModule()
	{
		return baseModule;
	}

	/**
	 * Get the set of synchronising actions of this module, i.e. its alphabet.
	 * Note that the definition of alphabet is syntactic: existence of an a-labelled command in this
	 * module ensures that a is in the alphabet, regardless of whether the guard is true.
	 */
	public Vector<String> getAllSynchs()
	{
		int i, n;
		String s;
		Vector<String> allSynchs = new Vector<String>();
		n = getNumCommands();
		for (i = 0; i < n; i++) {
			s = getCommand(i).getSynch();
			if (!s.equals("") && !allSynchs.contains(s)) allSynchs.add(s);
		}
		return allSynchs;
	}
	
	/**
	 * Check if action label 's' is in the alphabet of this module.
	 */
	public boolean usesSynch(String s)
	{
		return getAllSynchs().contains(s);
	}
	
	public boolean isLocalVariable(String s)
	{
		int i, n;
		
		n = getNumDeclarations();
		for (i = 0; i < n; i++) {
			if (getDeclaration(i).getName().equals(s)) return true;
		}
		return false;
	}
	
	public boolean isLocalView(String s)
	{
		for (Declaration decl : views) {
			if (decl.getName().equals(s)) return true;
		}
		return false;
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
	public String toString()
	{
		String s = "";
		int i, n;
		
		s = s + "module " + name + "\n\n";
		n = getNumDeclarations();
		for (i = 0; i < n; i++) {
			s = s + "\t" + getDeclaration(i) + ";\n";
		}
		if (n > 0) s = s + "\n";
		n = getNumViewDeclarations();
		for (i = 0; i < n; i++) {
			s = s + "\t" + getViewDeclaration(i) + ";\n";
		}
		if (n > 0) s = s + "\n";
		if (invariant != null) {
			s += "\tinvariant " + invariant + " endinvariant\n\n";
		}
		n = getNumCommands();
		for (i = 0; i < n; i++) {
			s = s + "\t" + getCommand(i) + ";\n";
		}
		s = s + "\nendmodule";
		
		return s;
	}

	@Override
	public Module deepCopy(DeepCopy copier) throws PrismLangException
	{
		nameASTElement = copier.copy(nameASTElement);
		invariant      = copier.copy(invariant);
		copier.copyAll(decls);
		copier.copyAll(commands);
		copier.copyAll(views);

		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Module clone()
	{
		Module clone = (Module) super.clone();

		clone.decls    = (ArrayList<Declaration>) decls.clone();
		clone.commands = (ArrayList<Command>) commands.clone();
		clone.views    = (ArrayList<Declaration>) views.clone();

		return clone;
	}
}

//------------------------------------------------------------------------------
