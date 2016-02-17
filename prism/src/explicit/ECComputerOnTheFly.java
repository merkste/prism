//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Mateusz Ujma <mateusz.ujma@cs.ox.ac.uk> (University of Oxford)
//  * Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import common.iterable.IterableBitSet;
import prism.PrismComponent;
import prism.PrismException;

/**
 * Explicit maximal end component computer for a nondeterministic model such as an MDP.
 */
public class ECComputerOnTheFly extends ECComputerDefault
{
	/**
	 * Build (M)EC computer for a given model.
	 */
	public ECComputerOnTheFly(PrismComponent parent, NondetModel model, ECConsumer consumer) throws PrismException
	{
		super(parent, model, consumer);
	}

	// Methods for ECComputer interface

	@Override
	public void computeMECStates() throws PrismException
	{
		findEndComponents(null, null);
		consumer.notifyDone();
	}

	@Override
	public void computeMECStates(BitSet restrict) throws PrismException
	{
		findEndComponents(restrict, null);
		consumer.notifyDone();
	}

	// Computation

	// TODO: handle 'accept'
	
	/**
	 * Find all accepting maximal end components (MECs) in the submodel obtained
	 * by restricting this one to the set of states {@code restrict},
	 * where acceptance is defined as those which intersect with {@code accept}.
	 * If {@code restrict} is null, we look at the whole model, not a submodel.
	 * If {@code accept} is null, the acceptance condition is trivially satisfied.
	 * @param restrict BitSet for the set of states to restrict to
	 * @param accept BitSet for the set of accepting states
	 */
	protected void findEndComponents(BitSet restrict, final BitSet accept) throws PrismException
	{
		// If restrict is null, look within set of all reachable states
		if (restrict == null) {
			restrict = new BitSet();
			restrict.set(0, model.getNumStates());
		}

		if (restrict.isEmpty()) {
			return;
		}

		final SubNondetModel submodel = restrict(model, restrict);
		if (verbosity > 0) { 
			getLog().println("Sub-MDP: " + submodel.infoString());
		}
		if (submodel.getNumStates() == 0) {
			return;
		}


		SCCConsumer scc_consumer = new SCCConsumer(this, submodel) {

			@Override
			public void notifyNextSCC(BitSet scc_submodel) throws PrismException {
				if (isMEC(submodel, scc_submodel)) {
					// we have identified an MEC
					if (verbosity > 1) { 
						getLog().println("Found MEC: " + scc_submodel);
					}
					consumer.notifyNextMEC(translateStates(submodel, scc_submodel));
				} else {
					// refine this SCC by finding the MECs inside
					if (verbosity > 1) {
						getLog().println("Recurse into: " + scc_submodel);
					}
					findEndComponents(translateStates(submodel, scc_submodel), accept);
				}
			}

		};
		SCCComputer sccc = SCCComputer.createSCCComputer(this, submodel, scc_consumer);
		sccc.computeSCCs();
	}

	/**
	 * Translate a set of states from the sub-model to the original model.
	 */
	private BitSet translateStates(SubNondetModel submodel, BitSet states) {
		BitSet result = new BitSet();
		for (int j : IterableBitSet.getSetBits(states)) {
			result.set(submodel.translateState(j));
		}
		return result;
	}

	/**
	 * Test whether a given set of states {@code b} constitutes an MEC in the model.
	 */
	private boolean isMEC(NondetModel model, BitSet b)
	{
		if (b.isEmpty())
			return false;

		for (int state : IterableBitSet.getSetBits(b)) {
			boolean atLeastOneAction = false;
			for (int i = 0, numChoices = model.getNumChoices(state); i < numChoices; i++) {
				if (model.allSuccessorsInSet(state, i, b)) {
					atLeastOneAction = true;
				}
			}
			if (!atLeastOneAction) {
				return false;
			}
		}

		return true;
	}
}
