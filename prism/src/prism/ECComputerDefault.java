//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Carlos S. Bederian (Universidad Nacional de Cordoba)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

package prism;

import java.util.List;
import java.util.Stack;
import java.util.Vector;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

/**
 * Symbolic maximal end component computer for a nondeterministic model such as an MDP.
 */
@SuppressWarnings("unused")
public class ECComputerDefault extends ECComputer
{
	/**
	 * Build (M)EC computer for a given model.
	 */
	public ECComputerDefault(PrismComponent parent, JDDNode reach, JDDNode trans, JDDNode trans01, JDDVars allDDRowVars, JDDVars allDDColVars,
			JDDVars allDDNondetVars) throws PrismException
	{
		super(parent, reach, trans, trans01, allDDRowVars, allDDColVars, allDDNondetVars);
	}

	// Methods for ECComputer interface

	@Override
	public void computeMECStates() throws PrismException
	{
		mecs = findEndComponents(null, null);
	}

	@Override
	public void computeMECStates(JDDNode restrict) throws PrismException
	{
		mecs = findEndComponents(restrict, null);
	}

	@Override
	public void computeMECStates(JDDNode restrict, JDDNode accept) throws PrismException
	{
		mecs = findEndComponents(restrict, accept);
	}

	// Computation

	/**
	 * Find all accepting maximal end components (MECs) in the submodel obtained
	 * by restricting this one to the set of states {@code restrict},
	 * where acceptance is defined as those which intersect with {@code accept}.
	 * If {@code restrict} is null, we look at the whole model, not a submodel.
	 * If {@code accept} is null, the acceptance condition is trivially satisfied.
	 * @param restrict BDD for the set of states to restrict to
	 * @param accept BDD for the set of accepting states
	 * @return a list of (referenced) BDDs representing the MECs
	 */
	private List<JDDNode> findEndComponents(JDDNode restrict, JDDNode accept) throws PrismException
	{
		Vector<JDDNode> mecs = new Vector<JDDNode>();
		SCCComputer sccComputer;

		// Initial set of candidates for MECs just contains the whole set we are searching
		// (which, if null, is all states)
		if (restrict == null)
			restrict = reach;
		Stack<JDDNode> candidates = new Stack<JDDNode>();
		JDD.Ref(restrict);
		candidates.push(restrict);

		// Go through each candidate set
		while (!candidates.isEmpty()) {
			JDDNode candidate = candidates.pop();
			
			// Compute its maximal stable set
			JDD.Ref(candidate);
			JDDNode stableSet = findMaximalStableSet(candidate);
			
			// Drop empty sets
			if (stableSet.equals(JDD.ZERO)) {
				JDD.Deref(stableSet);
				JDD.Deref(candidate);
				continue;
			}

			if (stableSet.equals(candidate) && JDD.GetNumMinterms(stableSet, allDDRowVars.n()) == 1) {
				mecs.add(candidate);
				JDD.Deref(stableSet);
				continue;
			}

			// Filter bad transitions
			JDDNode stableSetTrans = getStableTransReln(stableSet.copy());

			// Find the maximal SCCs in (stableSet, stableSetTrans)
			sccComputer = SCCComputer.createSCCComputer(this, stableSet, stableSetTrans, allDDRowVars, allDDColVars);
			if (accept != null)
				sccComputer.computeSCCs(accept);
			else
				sccComputer.computeSCCs();
			JDD.Deref(stableSet);
			JDD.Deref(stableSetTrans);
			List<JDDNode> sccs = sccComputer.getSCCs();
			JDD.Deref(sccComputer.getNotInSCCs());
			
			// If there are no SCCs, do nothing 
			if (sccs.size() == 0) {
			}
			// If the whole sub-MDP is one SCC, we found an MEC
			else if (sccs.size() == 1 && sccs.get(0).equals(candidate)) {
				mecs.add(sccs.get(0));
			}
			// Otherwise add SCCs as candidates and proceed
			else {
				candidates.addAll(sccs);
			}
			JDD.Deref(candidate);
		}
		return mecs;
	}

	@Override
	public JDDNode findMaximalStableSet(JDDNode candidateStates)
	{
		// Store two copies to allow check for fixed point 
		JDDNode current = candidateStates;
		JDDNode old = JDD.Constant(0);
		// Fixed point
		while (!current.equals(old)) {
			// Remember last set
			JDD.Deref(old);
			JDD.Ref(current);
			old = current;
			// Find transitions starting in current
			JDD.Ref(trans01);
			JDD.Ref(current);
			JDDNode currTrans = JDD.Apply(JDD.TIMES, trans01, current);
			// Find transitions starting in current *and* ending in current
			current = JDD.PermuteVariables(current, allDDRowVars, allDDColVars);
			JDD.Ref(currTrans);
			JDDNode currTrans2 = JDD.Apply(JDD.TIMES, currTrans, current);
			// Find transitions leaving current
			JDD.Ref(currTrans2);
			currTrans = JDD.And(currTrans, JDD.Not(currTrans2));
			// Find choices leaving current
			currTrans = JDD.ThereExists(currTrans, allDDColVars);
			// Remove leaving choices
			currTrans2 = JDD.ThereExists(currTrans2, allDDColVars);
			currTrans2 = JDD.And(currTrans2, JDD.Not(currTrans));
			// Keep states with at least one choice
			current = JDD.ThereExists(currTrans2, allDDNondetVars);
		}
		JDD.Deref(old);
		return current;
	}

	/**
	 * Returns the maximal stable set of states contained in {@code candidateStates},
	 * i.e. the maximal subset of states which have a choice whose transitions remain in the subset. 
	 * @param candidateStates BDD for a set of states (over allDDRowVars) (dereferenced after calling this function)
	 * @return A referenced BDD with the maximal stable set in c
	 * 
	 * Old version of the code (uses probability sum, which shouldn't be needed).
	 */
	@Deprecated
	private JDDNode findMaximalStableSetOld(JDDNode candidateStates)
	{
		JDDNode old = JDD.Constant(0);
		JDDNode current = candidateStates;

		while (!current.equals(old)) {
			JDD.Deref(old);
			JDD.Ref(current);
			old = current;

			JDD.Ref(current);
			JDD.Ref(trans);
			// Select transitions starting in current
			JDDNode currTrans = JDD.Apply(JDD.TIMES, trans, current);
			// Select transitions starting in current and ending in current
			JDDNode tmp = JDD.PermuteVariables(current, allDDRowVars, allDDColVars);
			tmp = JDD.Apply(JDD.TIMES, currTrans, tmp);
			// Sum all successor probabilities for each (state, action) tuple
			tmp = JDD.SumAbstract(tmp, allDDColVars);
			// If the sum for a (state,action) tuple is 1,
			// there is an action that remains in the stable set with prob 1
			tmp = JDD.GreaterThan(tmp, 1 - sumRoundOff);
			// Without fairness, we just need one action per state
			current = JDD.ThereExists(tmp, allDDNondetVars);
		}
		JDD.Deref(old);
		return current;
	}

	/**
	 * Returns the transition relation of a stable set
	 *
	 * Old version of the code (uses probability sum, which shouldn't be needed),
	 * use getStableTransReln instead.
	 *
	 * @param b BDD of a stable set (dereferenced after calling this function)
	 * @return referenced BDD of the transition relation restricted to the stable set
	 */
	@Deprecated
	private JDDNode maxStableSetTrans(JDDNode b)
	{

		JDD.Ref(b);
		JDD.Ref(trans);
		// Select transitions starting in b
		JDDNode currTrans = JDD.Apply(JDD.TIMES, trans, b);
		JDDNode mask = JDD.PermuteVariables(b, allDDRowVars, allDDColVars);
		// Select transitions starting in current and ending in current
		mask = JDD.Apply(JDD.TIMES, currTrans, mask);
		// Sum all successor probabilities for each (state, action) tuple
		mask = JDD.SumAbstract(mask, allDDColVars);
		// If the sum for a (state,action) tuple is 1,
		// there is an action that remains in the stable set with prob 1
		mask = JDD.GreaterThan(mask, 1 - sumRoundOff);
		// select the transitions starting in these tuples
		JDD.Ref(trans01);
		JDDNode stableTrans01 = JDD.And(trans01, mask);
		// Abstract over actions
		return JDD.ThereExists(stableTrans01, allDDNondetVars);
	}

	/**
	 * Returns the transition relation of a state set,
	 * i.e., a 0-1 MTBDD over row and col variables containing
	 * those pairs (s,t) for which there exists an alpha
	 * with P(s,alpha,t) > 0 and all alpha-successors of
	 * s are again in the state set.
	 *
	 * <br>[REFS: <i>result</i>, DEREFS: stateSet ]
	 */
	public JDDNode getStableTransReln(JDDNode stateSet)
	{
		return JDD.ThereExists(getStableTransitions(stateSet), allDDNondetVars);
	}

	@Override
	public JDDNode getStableTransitions(JDDNode stateSet)
	{
		// trans01 from the state set
		JDDNode transFromSet = JDD.And(stateSet.copy(), trans01.copy());
		// ... and back to the state set
		JDDNode transToSet = JDD.And(transFromSet.copy(), JDD.PermuteVariables(stateSet.copy(), allDDRowVars, allDDColVars));
		// (s,alpha) pairs that have at least one successor back in the set
		JDDNode stateActionsToSet = JDD.ThereExists(transToSet, allDDColVars);

		// those transitions where at least one successor remains in set
		JDDNode candidateTrans = JDD.And(transFromSet, stateActionsToSet);
		// filter: those transitions where at least one successor goes outside the set
		JDDNode transSomewhereElse = JDD.And(candidateTrans.copy(), JDD.Not(JDD.PermuteVariables(stateSet.copy(), allDDRowVars, allDDColVars)));
		// the corresponding (s,alpha) pairs, where a successor is outside the set
		JDDNode stateActionsBad = JDD.ThereExists(transSomewhereElse, allDDColVars);
		// restrict to only those transitions where all successors are in the set
		JDDNode result = JDD.And(candidateTrans, JDD.Not(stateActionsBad));

		JDD.Deref(stateSet);

		return result;
	}

}
