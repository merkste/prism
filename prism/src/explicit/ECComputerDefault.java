//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Mateusz Ujma <mateusz.ujma@cs.ox.ac.uk> (University of Oxford)
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import common.iterable.IterableBitSet;
import common.iterable.IterableStateSet;
import prism.PrismComponent;
import prism.PrismException;

/**
 * Explicit maximal end component computer for a nondeterministic model such as an MDP.
 * Implements the algorithm from p.48 of:
 * Luca de Alfaro. Formal Verification of Probabilistic Systems. Ph.D. thesis, Stanford University (1997)
 */
public class ECComputerDefault extends ECComputer
{
	/** The model to compute (M)ECs for **/
	protected NondetModel model;
	

	/**
	 * Build (M)EC computer for a given model.
	 */
	public ECComputerDefault(PrismComponent parent, NondetModel model, ECConsumer consumer) throws PrismException
	{
		super(parent, consumer);
		this.model = model;
	}

	// Methods for ECComputer interface

	@Override
	public void computeMECStates() throws PrismException
	{
		findEndComponents(null, null);
	}

	@Override
	public void computeMECStates(BitSet restrict) throws PrismException
	{
		findEndComponents(restrict, null);
	}

	@Override
	public void computeMECStates(BitSet restrict, BitSet accept) throws PrismException
	{
		findEndComponents(restrict, accept);
	}

	// Computation
	
	/**
	 * Find all accepting maximal end components (MECs) in the submodel obtained
	 * by restricting this one to the set of states {@code restrict},
	 * where acceptance is defined as those which intersect with {@code accept}.
	 * If {@code restrict} is null, we look at the whole model, not a submodel.
	 * If {@code accept} is null, the acceptance condition is trivially satisfied.
	 * @param restrict BitSet for the set of states to restrict to
	 * @param accept BitSet for the set of accepting states
	 */
	protected void findEndComponents(BitSet restrict, BitSet accept) throws PrismException
	{
		// If restrict is null, look within set of all reachable states
		if (restrict == null) {
			restrict = new BitSet();
			restrict.set(0, model.getNumStates());
		}
		// Initialise L with set of all states to look in (if non-empty)
		List<BitSet> L = new ArrayList<BitSet>();
		if (restrict.isEmpty()) {
			consumer.notifyDone();
			return;
		}
		L.add(restrict);
		// Find MECs
		boolean changed = true;
		while (changed) {
			changed = false;
			BitSet E = L.remove(0);
			SubNondetModel submodel = restrict(model, E);
			List<BitSet> sccs = translateStates(submodel, computeSCCs(submodel));
			L = replaceEWithSCCs(L, E, sccs);
			changed = canLBeChanged(L, E);
		}
		// Filter and return those that contain a state in accept
		if (accept != null) {
			int i = 0;
			while (i < L.size()) {
				if (!L.get(i).intersects(accept)) {
					L.remove(i);
				} else {
					i++;
				}
			}
		}
		for (BitSet mec : L) {
			consumer.notifyNextMEC(mec);
		}
		consumer.notifyDone();
	}

	private Set<BitSet> processedSCCs = new HashSet<BitSet>();

	private boolean canLBeChanged(List<BitSet> L, BitSet E)
	{
		processedSCCs.add(E);
		for (int i = 0, size = L.size(); i < size; i++) {
			if (!processedSCCs.contains(L.get(i))) {
				return true;
			}
		}
		return false;
	}

	private List<BitSet> replaceEWithSCCs(List<BitSet> L, BitSet E, List<BitSet> sccs)
	{
		if (sccs.size() > 0) {
			List<BitSet> toAdd = new ArrayList<BitSet>();
			for (int i = 0, size = sccs.size(); i < size; i++) {
				if (!L.contains(sccs.get(i))) {
					toAdd.add(sccs.get(i));
				}
			}
			if (toAdd.size() > 0) {
				L.addAll(toAdd);
			}
		}
		return L;
	}

	private BitSet getActionsRemainingInSet(NondetModel model, BitSet states, int s) {
		BitSet actions = new BitSet();

		for (int j = 0; j < model.getNumChoices(s); j++) {
			if (model.allSuccessorsInSet(s, j, states)) {
				actions.set(j);
			}
		}

		return actions;
	}

	protected SubNondetModel restrict(NondetModel model, BitSet states)
	{
		if (pre != null) {
			return restrictUsingPre(model, states);
		} else {
			return restrictFixpoint(model, states);
		}
	}
	
	protected SubNondetModel restrictFixpoint(NondetModel model, BitSet states)
	{
		Map<Integer, BitSet> actions = new HashMap<Integer, BitSet>();
		BitSet initialStates = new BitSet();
		initialStates.set(states.nextSetBit(0));

		int iterations = 0;
		int checks = 0;
		long start = System.currentTimeMillis();

		boolean changed = true;
		while (changed) {
			iterations++;
			changed = false;
			actions.clear();
			for (int i : new IterableStateSet(states, model.getNumStates())) {
				BitSet act = new BitSet();
				checks++;
				for (int j = 0, numChoices = model.getNumChoices(i); j < numChoices; j++) {
					if (model.allSuccessorsInSet(i, j, states)) {
						act.set(j);
					}
				}
				if (act.isEmpty()) {
					states.clear(i);
					changed = true;
				}
				actions.put(i, act);
			}
		}
		if (verbosity > 0) {
			getLog().println("Restrict precomputations took "+iterations+" iterations, "+checks+" checks and "+(System.currentTimeMillis()-start)+"ms.");
		}

		return new SubNondetModel(model, states, actions, initialStates);
	}

	protected SubNondetModel restrictUsingPre(NondetModel model, BitSet states)
	{
		Map<Integer, BitSet> actions = new HashMap<Integer, BitSet>();
		BitSet initialStates = new BitSet();
		initialStates.set(states.nextSetBit(0));

		Stack<Integer> toCheck = new Stack<Integer>();

		int checks = 0;
		long start = System.currentTimeMillis();

		for (int i : IterableBitSet.getSetBits(states)) {
			checks++;
			BitSet act = getActionsRemainingInSet(model, states, i);
			if (act.isEmpty()) {
				states.clear(i);

				for (int j : pre.getPre(i)) {
					if (states.get(j))
						toCheck.add(j);
				}
			} else {
				actions.put(i, act);
			}
		}

		while (!toCheck.isEmpty()) {
			int i = toCheck.pop();
			if (!states.get(i)) continue;  // already removed

			checks++;
			BitSet act = getActionsRemainingInSet(model, states, i);
			if (act.isEmpty()) {
				states.clear(i);
				actions.remove(i);

				for (int j : pre.getPre(i)) {
					if (states.get(j))
						toCheck.add(j);
				}
			} else {
				actions.put(i, act);
			}
		}

		if (verbosity > 0) {
			getLog().println("Restrict precomputations took "+checks+" checks and "+(System.currentTimeMillis()-start)+"ms.");
		}

		return new SubNondetModel(model, states, actions, initialStates);
	}

	private List<BitSet> computeSCCs(NondetModel model) throws PrismException
	{
		SCCConsumerStore sccs = new SCCConsumerStore(this, model);
		SCCComputer sccc = SCCComputer.createSCCComputer(this, model, sccs);
		sccc.computeSCCs();
		return sccs.getSCCs();
	}

	private List<BitSet> translateStates(SubNondetModel model, List<BitSet> sccs)
	{
		List<BitSet> r = new ArrayList<BitSet>();
		for (int i = 0; i < sccs.size(); i++) {
			BitSet set = sccs.get(i);
			BitSet set2 = new BitSet();
			r.add(set2);
			for (int j = set.nextSetBit(0); j >= 0; j = set.nextSetBit(j + 1)) {
				set2.set(model.translateState(j));

			}
		}
		return r;
	}

	private boolean isMEC(BitSet b)
	{
		if (b.isEmpty())
			return false;

		int state = b.nextSetBit(0);
		while (state != -1) {
			boolean atLeastOneAction = false;
			for (int i = 0, numChoices = model.getNumChoices(state); i < numChoices; i++) {
				if (model.allSuccessorsInSet(state, i, b)) {
					atLeastOneAction = true;
				}
			}
			if (!atLeastOneAction) {
				return false;
			}
			state = b.nextSetBit(state + 1);
		}

		return true;
	}
}
