//==============================================================================
//	
//	Copyright (c) 2016-
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import prism.PrismComponent;
import common.iterable.IterableBitSet;

/**
 * A class for storing and accessing the incoming choices of an explicit NondetModel.
 * This class can be seen as providing more detailed information than PredecessorRelation,
 * as that class only stores information about the states and not the choices linking them. 
 * <p>
 * As NondetModel only provide easy access to successors of states,
 * the predecessor relation is computed and stored for subsequent efficient access.
 * <p>
 * Note: Naturally, if the NondetModel changes, the predecessor relation
 * has to be recomputed to remain accurate.
 */
public class IncomingChoiceRelation
{
	/** An outgoing choice from a state, i.e., the source state and the choice index */
	public static final class Choice
	{
		/** the source state*/
		private int state;
		/** the choice index */
		private int choice;

		/** Constructor */
		public Choice(int state, int choice)
		{
			this.state = state;
			this.choice = choice;
		}

		/** The source state of this choice */
		public int getState()
		{
			return state;
		}

		/** The choice index of this choice */
		public int getChoice()
		{
			return choice;
		}
		
		@Override
		public String toString()
		{
			return "("+state+","+choice+")";
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + choice;
			result = prime * result + state;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Choice other = (Choice) obj;
			if (choice != other.choice)
				return false;
			if (state != other.state)
				return false;
			return true;
		}

	};
	
	/**
	 * pre[i] provides the list of incoming choices of the state with index i.
	 */
	List<ArrayList<Choice>> pre;

	/**
	 * Constructor. Computes the predecessor relation for the given model
	 * by considering the successors of each state.
	 *
	 * @param model the Model
	 */
	public IncomingChoiceRelation(NondetModel model)
	{
		pre = new ArrayList<ArrayList<Choice>>(model.getNumStates());
		// construct the (empty) array list for all states
		for (int s = 0; s < model.getNumStates(); s++) {
			pre.add(s, new ArrayList<Choice>());
		}

		compute(model);
	}

	/** Compute the predecessor relation using getSuccessorsIterator. */
	private void compute(NondetModel model)
	{
		int n = model.getNumStates();

		for (int s = 0; s < n; s++) {
			for (int c = 0, m = model.getNumChoices(s); c < m; c++) {
				Choice choice = new Choice(s, c);

				Iterator<Integer> it = model.getSuccessorsIterator(s, c);
				while (it.hasNext()) {
					int successor = it.next();

					// Add the current choice s to pre[successor].
					pre.get(successor).add(choice);
				}
			}
		}
	}

	/**
	 * Get an Iterable over the incoming choices of state {@code s}.
	 */
	public Iterable<Choice> getIncomingChoices(int s)
	{
		return pre.get(s);
	}

	/**
	 * Get an Iterator over the incoming choices of state {@code s}.
	 */
	public Iterator<Choice> getIncomingChoicesIterator(int s)
	{
		return getIncomingChoices(s).iterator();
	}

	/**
	 * Static constructor to compute the incoming choices information for the given model.
	 * Logs diagnostic information to the log of the given PrismComponent.
	 *
	 * @param parent a PrismComponent (for obtaining the log and settings)
	 * @param model the non-deterministic model for which the predecessor relation should be computed
	 * @returns the incoming choices information
	 **/
	public static IncomingChoiceRelation forModel(PrismComponent parent, NondetModel model)
	{
		long timer = System.currentTimeMillis();
		
		parent.getLog().print("Calculating incoming choices relation for "+model.getModelType().fullName()+"...  ");
		parent.getLog().flush();

		IncomingChoiceRelation pre = new IncomingChoiceRelation(model);
		
		timer = System.currentTimeMillis() - timer;
		parent.getLog().println("done (" + timer / 1000.0 + " seconds)");

		return pre;
	}


	/**
	 * Computes the set Pre*(target) via a DFS, i.e., all states that
	 * are in {@code target} or can reach {@code target} via one or more transitions
	 * from states contained in {@code remain} and via the enabled choices in {@code enabledChoices}.
	 * <br/>
	 * If the parameter {@code remain} is {@code null}, then
	 * {@code remain} is considered to include all states in the model.
	 * <br/>
	 * If the parameter {@code enabledChoices} is {@code null}, then
	 * {@code enabledChoices} is considered to include all choices in the model.
	 * <br/>
	 * If the parameter {@code absorbing} is not {@code null},
	 * then the states in {@code absorbing} are considered to be absorbing,
	 * i.e., to have a single self-loop, disregarding other outgoing edges.

	 * @param remain restriction on the states that may occur
	 *               on the path to target, {@code null} = all states
	 * @param target The set of target states
	 * @param absorbing (optional) set of states that should be considered to be absorbing,
	 *               i.e., their outgoing edges are ignored, {@code null} = no states
	 * @param enabledChoices a mask providing information which choices are considered to be enabled
	 * @return the set of states Pre*(target)
	 */
	public BitSet calculatePreStar(BitSet remain, BitSet target, BitSet absorbing, ChoicesMask enabledChoices)
	{
		BitSet result;

		// all target states are in Pre*
		result = (BitSet)target.clone();

		// the stack of states whose predecessors have to be considered
		Stack<Integer> todo = new Stack<Integer>();

		// initial todo: all the target states
		for (Integer s : IterableBitSet.getSetBits(target)) {
			todo.add(s);
		};

		// the set of states that are finished
		BitSet done = new BitSet();

		while (!todo.isEmpty()) {
			int s = todo.pop();
			// already considered?
			if (done.get(s)) continue;

			done.set(s);

			// for each predecessor in the graph
			for (Choice choice : getIncomingChoices(s)) {
				// check that choice is actually enabled
				if (enabledChoices != null &&
				    !enabledChoices.isEnabled(choice.getState(), choice.getChoice())) {
					continue;
				}

				int p = choice.getState();
				if (absorbing != null && absorbing.get(p)) {
					// predecessor is absorbing, thus the edge is considered to not exist
					continue;
				}

				if (remain == null || remain.get(p)) {
					// can reach result (and is in remain)
					result.set(p);
					if (!done.get(p)) {
						// add to stack
						todo.add(p);
					}
				}
			}
		}

		return result;
	}

}
