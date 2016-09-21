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

/**
 * A Java wrapper around a CUDD MtrNode pointer.
 *
 * Trees made out of MtrNodes are used by CUDD to
 * specify variable ordering constraints during reordering.
 *<br>
 * Each node represents a group of contiguous variable indizes.
 * The children of an inner node in the tree can be reordered
 * together, and the variables in a leaf node as well.
 * There is a flag 'fixed' that specifies whether reordering
 * is prohibited at the specified level.
 */
public class MtrNode
{
	/** The C++ MtrNode pointer as a long */
	private long ptr;

	private static native long DDM_MakeGroup(boolean fixed, int index, int size);
	private static native void DDM_AddChild(long parent, long child);
	private static native void DDM_CuddSetTree(long root);
	private static native void DDM_FreeTree(long root);

	/** Private constructor from a MtrNode pointer returned from the JNI layer */
	private MtrNode(long ptr)
	{
		this.ptr = ptr;
	}

	/**
	 * Constructor, make a group.
	 * @param fixed is reordering allowed at that level?
	 * @param index the lowest variable index included in the range
	 * @param size the number of variables following index that ar included in the range
	 */
	public static MtrNode makeGroup(boolean fixed, int index, int size)
	{
		long ptr = DDM_MakeGroup(fixed, index, size);
		return new MtrNode(ptr);
	}

	/** Add a child node */
	public void addChild(MtrNode child)
	{
		DDM_AddChild(this.ptr, child.ptr);
	}

	/** Release the tree, freeing the C++ memory for this tree. */
	public void free() {
		DDM_FreeTree(ptr);
		ptr = 0;
	}

	/**
	 * Set a new tree as the current reordering constraint.
	 * Note: A previously set tree will be automatically freed and
	 * should not be accessed from the Java side any more.
	 */
	public static void setCuddTree(MtrNode root) {
		DDM_CuddSetTree(root.ptr);
	}

	/**
	 * Remove the current reordering constraint (if any).
	 * Note: This will automatically free the current reordering
	 * constraint tree, which should not be accessed
	 * from the Java side any more.
	 */
	public static void resetCuddTree() {
		DDM_CuddSetTree(0);
	}
}
