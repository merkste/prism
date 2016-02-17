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

package jdd;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Vector;

import prism.PrismException;
import prism.PrismLog;

/** */
public class JDDVarsTree
{
	/** 'fixed' flag,
	 * indicating that the order of the children (for an inner node)
	 * or the variable order (for a leaf node) should not be changed.
	 */
	private boolean fixed = false;

	/** The child nodes (for an inner node) */
	private ArrayList<JDDVarsTree> children = null;

	/** The JDDVars (for a leaf node) */
	private JDDVars vars = null;

	/** Description */
	private String description;

	/** A range of variable indizes */
	private static class VarRange {
		/** The low index */
		private int low;
		/** The number of variables in the range, starting with low */
		private int size;

		/** Constructor */
		public VarRange(int low, int size)
		{
			this.low = low;
			this.size = size;
		}

		/** Return the low variable index */
		public int low()
		{
			return low;
		}

		/** Return the size of the range */
		public int size()
		{
			return size;
		}

		/** Return the high variable index */
		public int high() {
			return low+size-1;
		}

		/** Merge with other range.
		 * There can not be a gap between them
		 */
		public void merge(VarRange other) throws PrismException
		{
			if (other.low != this.high()+1) {
				throw new PrismException("VarRange not contiguous");
			}
			this.size = this.size + other.size;
		}

		public String toString()
		{
			return "("+low+","+high()+")";
		}
	}

	/** Constructor */
	private JDDVarsTree(String description)
	{
		this.description = description;
	}

	/**
	 * Constructor for a leaf node, containing a JDDVars container
	 * @param vars the variables
	 * @param description Description for this node
	 */
	public static JDDVarsTree leaf(JDDVars vars, String description)
	{
		JDDVarsTree vt = new JDDVarsTree(description);
		vt.vars = vars;
		return vt;
	}

	/**
	 * Constructor for an inner tree node.
	 * @param description Description for this node
	 */
	public static JDDVarsTree inner(String description)
	{
		JDDVarsTree vt = new JDDVarsTree(description);
		vt.children = new ArrayList<JDDVarsTree>();
		return vt;
	}

	/** Clear (deref) the JDDVars for this (sub)tree */
	public void clear()
	{
		if (children != null) {
			for (JDDVarsTree child : children) {
				child.clear();
			}
		}

		if (vars != null) {
			vars.derefAll();
		}
	}

	/**
	 * Returns 'fixed' flag,
	 * indicating that the order of the children (for an inner node)
	 * or the variable order (for a leaf node) should not be changed.
	 */
	public boolean isFixed()
	{
		return fixed;
	}

	/**
	 * Sets 'fixed' flag,
	 * indicating that the order of the children (for an inner node)
	 * or the variable order (for a leaf node) should not be changed.
	 */
	public void setFixed(boolean fixed)
	{
		this.fixed = fixed;
	}

	/** Returns true if this is a leaf node */
	public boolean isLeafNode()
	{
		return vars != null;
	}

	/** Add a child tree as the right-most sibling */
	public void addChild(JDDVarsTree child)
	{
		children.add(child);
	}

	/**
	 * Calculate a var range for this (sub)tree,
	 * encompassing all the variables of child nodes.
	 */
	public VarRange getVarRange() throws PrismException
	{
		if (isLeafNode()) {
			// easy, get range from the vars
			return getVarRange(vars);
		}

		VarRange result = null;
		// Iterate over children and merge their var ranges
		for (JDDVarsTree child : children) {
			VarRange childRange = child.getVarRange();
			if (childRange == null) {
				continue;  // ignore empty
			}
			if (result == null) {
				result = childRange;
			} else {
				result.merge(childRange);
			}
		}

		return result;
	}

	/**
	 * Get the var range for a JDDVars container.
	 * The variables have to be contiguous.
	 */
	private static VarRange getVarRange(JDDVars vars) throws PrismException
	{
		BitSet indizes = new BitSet();
		for (int i=0;i<vars.n();i++) {
			indizes.set(vars.getVarIndex(i));
		}

		int low = indizes.nextSetBit(0);
		if (low < 0) {
			return null; // empty
		}

		int high = indizes.nextClearBit(low)-1;
		int size = high-low+1;

		if (indizes.cardinality() != size) {
			throw new PrismException("JDDVars not contiguous");
		}

		return new VarRange(low, size);
	}

	/** Convert this tree to an MtrNode */
	public MtrNode convertToMtrNode() throws PrismException
	{
		VarRange vr = getVarRange();
		if (vr == null) return null;
		MtrNode root = MtrNode.makeGroup(isFixed(), vr.low(), vr.size());

		if (!isLeafNode()) {
			for (JDDVarsTree child : children) {
				MtrNode mtrChild = child.convertToMtrNode();
				if (mtrChild != null)
					root.addChild(mtrChild);
			}
		}

		return root;
	}

	/**
	 * Print the tree to the log.
	 * @param log the log
	 * @param varNames (optional) an array of variable names, may be {@code null} */
	public void print(PrismLog log, Vector<String> varNames) throws PrismException
	{
		print(log, varNames, "");
	}

	/**
	 * Print the tree to the log, with additional indentation before each entry.
	 * @param log the log
	 * @param varNames (optional) an array of variable names, may be {@code null}
	 * @param indentation String of spaces, printed before each entry
	 */
	private void print(PrismLog log, Vector<String> varNames, String indentation) throws PrismException
	{
		log.print(indentation);
		if (isLeafNode()) {
			log.print("-> ");
		}
		if (description != null) {
			log.print(description);
			log.print(" ");
		}
		if (isFixed())
			log.print("fixed ");
		VarRange range = getVarRange();
		log.print(range);

		if (isLeafNode() && varNames != null) {
			log.print(", names = ");
			for (int i = range.low(); i <= range.high(); i++) {
				if (varNames.get(i) != null && varNames.get(i) != "") {
					log.print(varNames.get(i) + " ");
				}
			}
		}
		log.println();

		if (!isLeafNode()) {
			for (JDDVarsTree child : children) {
				child.print(log, varNames, indentation+" ");
			}
		}
	}
}
