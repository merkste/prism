//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
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

import java.util.BitSet;
import java.util.Iterator;
import java.util.Stack;

import common.iterable.IterableBitSet;


/**
 * This class can be used to conveniently determine reachable sets of states.
 */
public class ReachabilityComputer
{
	private final Model model;

	/**
	 * Constructor, initializes this {@code ReachabilityComputer} with the underlying {@code model}.
	 *
	 * @param model the {@code Model} to be used for the computations
	 */
	public ReachabilityComputer(final Model model)
	{
		this.model = model;
	}

	/**
	 * Compute the set Succ({@code S}), i.e., the set of all direct successors of states in
	 * {@code S} in the model, reachable in a single step.
	 *
	 * @param S a set of states in the model
	 * @return Succ(S)
	 */
	public BitSet computeSucc(final BitSet S)
	{
		final BitSet successors = new BitSet(S.size());
		for (int state : new IterableBitSet(S)) {
			for (Iterator<Integer> iter = model.getSuccessorsIterator(state); iter.hasNext();) {
				successors.set(iter.next());
			}
		}
		return successors;
	}

	/**
	 * Compute the set Pre({@code S}), i.e., the set of all direct predecessors of states in
	 * {@code S} in the model, i.e., all states that can reach {@code S} in a single step.
	 *
	 * @param S a set of states in the model
	 * @return Pre(s)
	 */
	public BitSet computePre(final BitSet S)
	{
		final BitSet predecessors = new BitSet(S.size());
		if (model.hasStoredPredecessorRelation()) {
			final PredecessorRelation predecessorRelation = model.getPredecessorRelation(null, false);
			for (int state : new IterableBitSet(S)) {
				for (int pre : predecessorRelation.getPre(state)) {
					predecessors.set(pre);
				}
			}
		} else {
			for (int state = 0, numStates = model.getNumStates(); state < numStates; state++) {
				if (model.someSuccessorsInSet(state, S)) {
					predecessors.set(state);
				}
			}
		}
		return predecessors;
	}

	/**
	 * Compute the set Succ*(S), the union of S and Succ+(S), i.e.,
	 * the set of states that are members of S or can be reached from
	 * S via a finite number (>=0) of transitions.
	 *
	 * @param S a set of states of the model
	 * @return Succ*(S)
	 */
	public BitSet computeSuccStar(final BitSet S)
	{
		return computeSuccStar(S, null);
	}

	/**
	 * Compute the set Succ*({@code S},{@code remain}), i.e., the set of states
	 * that can be reached from {@code S} while remaining in {@code remain}.<br/>
	 *
	 * Let {@code S'} be the intersection of {@code S} and {@code remain}.
	 * Then Succ*({@code S},{@code remain}) corresponds to the union of
	 * {@code S'} and Succ+({@code S'}, {@code remain}),
	 * see {@code computeSuccStar(BitSet S, BitSet remain)}.
	 *
	 * @param S a set of states of the model
	 * @param remain a set of states that should not be left ({@code null} = no restriction)
	 * @return Succ*({@code S},{@code remain})
	 */
	public BitSet computeSuccStar(final BitSet S, final BitSet remain)
	{
		final BitSet result = (BitSet)S.clone();
		if (remain != null) {
			// S' = S intersects remain
			result.and(remain);
		}
		result.or(this.computeSuccPlus(result, remain));
		return result;
	}

	/**
	 * Compute the set Succ+(S), i.e., the set of states that
	 * can be reached via a finite number (>=1) of transitions.
	 *
	 * @param S a set of states of the model
	 * @return Succ+(S)
	 */
	public BitSet computeSuccPlus(final BitSet S)
	{
		return computeSuccPlus(S, null);
	}

	/**
	 * Compute the set Succ+({@code S},{@code remain}), i.e., the set of states
	 * that can be reached from {@code S} via a finite number (>=1) of transitions
	 * such that all successor states remain in {@code remain}. States in {@code S} do not
	 * have to be contained in {@code remain} for their successors to be considered in
	 * the initial step.
	 * <br/>
	 *
	 *  @param S a set of states of the model
	 *  @param remain a set of states that should not be left ({@code null} = no restriction)
	 *  @return Succ+({@code S},{@code remain})
	 */
	public BitSet computeSuccPlus(final BitSet S, final BitSet remain)
	{
		final BitSet result = new BitSet();

		// the stack of states whose successors have to be considered
		final Stack<Integer> todo = new Stack<Integer>();

		// initial todo: all the S states
		for (Integer s : new IterableBitSet(S)) {
			todo.add(s);
		}

		// the set of states that have been handled
		final BitSet done = new BitSet();

		while (!todo.isEmpty()) {
			final int s = todo.pop();
			// already considered?
			if (done.get(s)) continue;

			done.set(s);

			for (Iterator<Integer> iter = model.getSuccessorsIterator(s); iter.hasNext();) {
				final int succ = iter.next();

				if (remain == null || remain.get(succ)) {
					// direct successor can be reached from S (and is in remain)
					result.set(succ);
					if (!done.get(succ)) {
						// add succ to stack
						todo.add(succ);
					}
				}
			}
		}

		return result;
	}
}
