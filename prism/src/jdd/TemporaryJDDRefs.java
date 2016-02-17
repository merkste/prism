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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * AutoClosable container for keeping track of active JDDNode references
 * and other Clearable objects.
 * <br>
 * Use scenario:
 * <br>
 * try(TemporaryJDDRefs refs = new TemporaryJDDRefs()) {<br>
 *   refs.register(node);<br>
 *   ...<br>
 * }<br>
 * <br>
 * The node will be dereferenced when leaving the scope, i.e., when exiting
 * normally or when exiting due to an exception.
 * <br>
 * Supports tracking and clearing of objects that implement Clearable as well.
 */
public class TemporaryJDDRefs implements AutoCloseable
{
	/** The active JDDNode references */
	private List<JDDNode> activeRefs = new LinkedList<JDDNode>();
	/** The active Clearable references */
	private List<Clearable> activeClearables = new LinkedList<Clearable>();

	@Override
	public void close()
	{
		closeNodes();
		closeClearables();
	}

	/** Close (deref) all active node references */
	private void closeNodes()
	{
		Iterator<JDDNode> it = activeRefs.iterator();
		while (it.hasNext()) {
			JDDNode node = it.next();
			JDD.Deref(node);
			it.remove();
		}
	}

	/** Close (clear) all active Clearable references */
	private void closeClearables()
	{
		Iterator<Clearable> it = activeClearables.iterator();
		while (it.hasNext()) {
			Clearable clearable = it.next();
			clearable.clear();
			it.remove();
		}
	}

	/** Register a JDDNode reference */
	public void register(JDDNode node) {
		if (node == null) return;
		activeRefs.add(node);
	}

	/** Register a Clearable reference */
	public void register(Clearable clearable) {
		if (clearable == null) return;
		activeClearables.add(clearable);
	}

	/**
	 * Release multiple JDDNode references.
	 * <br>
	 * Releasing a JDDNode does not call {@code JDD.Deref},
	 * it simply notifies the container that the reference
	 * is no longer active.
	 */
	public void release(JDDNode... nodes)
	{
		for (JDDNode node : nodes)
			release(node);
	}

	/**
	 * Release a JDDNode reference.
	 * <br>
	 * Releasing a JDDNode does not call {@code JDD.Deref},
	 * it simply notifies the container that the reference
	 * is no longer active.
	 */
	public void release(JDDNode node)
	{
		Iterator<JDDNode> it = activeRefs.iterator();
		while (it.hasNext()) {
			if (it.next() == node) {
				it.remove();
				return;
			}
		}

		throw new UnsupportedOperationException("Can not release node that has not been registered: "+node);
	}

	/**
	 * Release multiple Clearable references.
	 * <br>
	 * Releasing a Clearable does not call {@code clear()},
	 * it simply notifies the container that the reference
	 * is no longer active.
	 */
	public void release(Clearable... clearables)
	{
		for (Clearable clearable : clearables)
			release(clearable);
	}

	/**
	 * Release a Clearable reference.
	 * <br>
	 * Releasing a Clearable does not call {@code clear()},
	 * it simply notifies the container that the reference
	 * is no longer active.
	 */
	public void release(Clearable clearable)
	{
		Iterator<Clearable> it = activeClearables.iterator();
		while (it.hasNext()) {
			if (it.next() == clearable) {
				it.remove();
				return;
			}
		}

		throw new UnsupportedOperationException("Can not release Clearable that has not been registered");
	}

}
