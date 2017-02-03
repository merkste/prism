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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.ToIntFunction;

import common.StopWatch;
import common.iterable.ArrayIterator;
import common.iterable.FunctionalIterator;
import common.iterable.IterableArray;
import common.iterable.IterableBitSet;
import prism.PrismComponent;

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
public class IncomingChoiceRelationSparse extends IncomingChoiceRelation
{
	public static final ToIntFunction<Integer> INT_VALUE = Integer::intValue;

	/**
	 * pre[i] provides the list of incoming choices of the state with index i.
	 */
	protected int[] offsets;
	protected Choice[] choices;

	/**
	 * Constructor. Computes the predecessor relation for the given model
	 * by considering the successors of each state.
	 *
	 * @param model the Model
	 */
	@SuppressWarnings("unchecked")
	public IncomingChoiceRelationSparse(NondetModel model)
	{
		int numStates    = model.getNumStates();
		int countChoices = 0;

		// compute choices
		ArrayList<Choice>[] pre = new ArrayList[numStates];
		for (int state = 0; state < numStates; state++) {
			for (int choice = 0, numChoices = model.getNumChoices(state); choice < numChoices; choice++) {
				Choice newChoice = new Choice(state, choice);

				for (Iterator<Integer> it = model.getSuccessorsIterator(state, choice); it.hasNext();) {
					int successor = it.next();

					// Add the current choice (s,c) to pre[successor].
					ArrayList<Choice> choices = pre[successor];
					if (choices == null) {
						pre[successor] = choices = new ArrayList<Choice>(5);
					}
					choices.add(newChoice);
					countChoices++;
				}
			}
		}

		// copy choices to sparse array
		offsets = new int[numStates + 1];
		choices = new Choice[countChoices];
		for (int state = 0, offset = 0; state < numStates; state++) {
			offsets[state] = offset;
			if (pre[state] == null) {
				continue;
			}
			offset += FunctionalIterator.extend(pre[state]).collectAndCount(choices, offset);
			pre[state] = null;
		}
		offsets[numStates] = countChoices;
	}

	/**
	 * Get an Iterable over the incoming choices of state {@code s}.
	 */
	public Iterable<Choice> getIncomingChoices(int s)
	{
		return new IterableArray.Of<>(choices, offsets[s], offsets[s+1]);
	}

	/**
	 * Get an Iterator over the incoming choices of state {@code s}.
	 */
	public Iterator<Choice> getIncomingChoicesIterator(int s)
	{
		return new ArrayIterator.Of<>(choices, offsets[s], offsets[s+1]);
	}

	/**
	 * Factory method to compute the incoming choices information for the given model.
	 * Logs diagnostic information to the log of the given PrismComponent.
	 *
	 * @param parent a PrismComponent (for obtaining the log and settings)
	 * @param model the non-deterministic model for which the predecessor relation should be computed
	 * @returns the incoming choices information
	 **/
	public static IncomingChoiceRelationSparse forModel(PrismComponent parent, NondetModel model)
	{
		parent.getLog().print("Calculating incoming choice relation (SPARSE) for "+model.getModelType().fullName()+"...  ");
		parent.getLog().flush();

		StopWatch watch = new StopWatch().start();
		IncomingChoiceRelationSparse pre = new IncomingChoiceRelationSparse(model);

		parent.getLog().println("done (" + watch.elapsedSeconds() + " seconds)");

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
						todo.push(p);
					}
				}
			}
		}
		return result;
	}
}
