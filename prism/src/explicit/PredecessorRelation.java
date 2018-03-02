//==============================================================================
//	
//	Copyright (c) 2014-
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

package explicit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.ToIntFunction;

import common.StopWatch;
import common.iterable.EmptyIterable;
import common.iterable.EmptyIterator;
import common.iterable.FunctionalIterable;
import common.iterable.FunctionalIterator;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterator.OfInt;
import common.iterable.IterableBitSet;
import prism.PrismComponent;

/**
 * A class for storing and accessing the predecessor relation of an explicit Model.
 * <p>
 * As Model only provide easy access to successors of states,
 * the predecessor relation is computed and stored for subsequent efficient access.
 * <p>
 * Note: Naturally, if the model changes, the predecessor relation
 * has to be recomputed to remain accurate.
 */
public class PredecessorRelation
{
	public static final ToIntFunction<Integer> INT_VALUE = Integer::intValue;

	/**
	 * pre[i] provides the list of predecessors of state with index i.
	 */
	protected ArrayList<Integer>[] pre;

	protected PredecessorRelation() {}

	/**
	 * Constructor. Computes the predecessor relation for the given model
	 * by considering the successors of each state.
	 *
	 * @param model the Model
	 */
	@SuppressWarnings("unchecked")
	public PredecessorRelation(Model model)
	{
		int numStates = model.getNumStates();

		pre = new ArrayList[numStates];

		for (int state = 0; state < numStates; state++) {
			for (Iterator<Integer> it = model.getSuccessorsIterator(state); it.hasNext();) {
				int successor = it.next();

				// Add the current state s to pre[successor].
				//
				// As getSuccessorsIterator guarantees that
				// there are no duplicates in the successors,
				// s will be added to successor exactly once.
				ArrayList<Integer> predecessors = pre[successor];
				if (predecessors == null) {
					pre[successor] = predecessors = new ArrayList<Integer>(5);
				}
				predecessors.add(state);
			}
		}
	}

	/**
	 * Get an Iterable over the predecessor states of {@code s}.
	 */
	public IterableInt getPre(int s)
	{
		return pre[s] == null ? EmptyIterable.OfInt() : FunctionalIterable.extend(pre[s]).mapToInt(INT_VALUE);
	}

	/**
	 * Get an Iterator over the predecessor states of {@code s}.
	 */
	public OfInt getPreIterator(int s)
	{
		return pre[s] == null ? EmptyIterator.OfInt() : FunctionalIterator.extend(pre[s]).mapToInt(INT_VALUE);
	}

	/**
	 * Factory method to compute the predecessor relation for the given model.
	 * Logs diagnostic information to the log of the given PrismComponent.
	 *
	 * @param parent a PrismComponent (for obtaining the log and settings)
	 * @param model the model for which the predecessor relation should be computed
	 * @returns the predecessor relation
	 **/
	public static PredecessorRelation forModel(PrismComponent parent, Model model)
	{
		parent.getLog().print("Calculating predecessor relation for "+model.getModelType().fullName()+"...  ");
		parent.getLog().flush();

		StopWatch watch = new StopWatch().start();
		PredecessorRelation pre = new PredecessorRelation(model);

		parent.getLog().println("done (" + watch.elapsedSeconds() + " seconds)");

		return pre;
	}

	/**
	 * Computes the set Pre*(target) via a DFS, i.e., all states that
	 * are in {@code target} or can reach {@code target} via one or more transitions
	 * from states contained in {@code remain}.
	 * <br/>
	 * If the parameter {@code remain} is {@code null}, then
	 * {@code remain} is considered to include all states in the model.
	 * <br/>
	 * If the parameter {@code absorbing} is not {@code null},
	 * then the states in {@code absorbing} are considered to be absorbing,
	 * i.e., to have a single self-loop, disregarding other outgoing edges.

	 * @param remain restriction on the states that may occur
	 *               on the path to target, {@code null} = all states
	 * @param target The set of target states
	 * @param absorbing (optional) set of states that should be considered to be absorbing,
	 *               i.e., their outgoing edges are ignored, {@code null} = no states
	 * @return the set of states Pre*(target)
	 */
	public BitSet calculatePreStar(BitSet remain, BitSet target, BitSet absorbing)
	{
		int cardinality = target.cardinality();
		// all target states are in Pre*
		BitSet result = (BitSet) target.clone();
		// STACK of states whose predecessors have to be considered
		Deque<Integer> todo = new ArrayDeque<Integer>(cardinality);
		new IterableBitSet(target).collect(todo);
		// set of states that are finished
		BitSet done = new BitSet(cardinality);

		while (!todo.isEmpty()) {
			int s = todo.pop();
			// already considered?
			if (done.get(s)) {
				continue;
			}
			done.set(s);

			// for each predecessor in the graph
			for (OfInt pre = getPreIterator(s); pre.hasNext();) {
				int p = pre.nextInt();
				if (absorbing != null && absorbing.get(p)) {
					// predecessor is absorbing, thus the edge is considered to not exist
					continue;
				}
				if (remain == null || remain.get(p)) {
					// can reach result (and is in remain)
					result.set(p);
					if (!done.get(p)) {
						// add to stack
						todo.push(p);
					}
				}
			}
		}
		return result;
	}
}
