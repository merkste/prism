//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package simulator;

import java.util.*;

import common.IterableBitSet;
import common.iterable.FunctionalPrimitiveIterator.OfInt;
import common.iterable.collections.ChainedList;
import parser.*;
import parser.ast.*;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;

public class ChoiceListFlexi implements Choice
{
	// Module/action info, encoded as an integer.
	// For an independent (non-synchronous) choice, this is -i,
	// where i is the 1-indexed module index.
	// For a synchronous choice, this is the 1-indexed action index.
	protected int moduleOrActionIndex;

	// List of multiple updates and associated probabilities/rates
	// Size of list is stored implicitly in target.length
	// Probabilities/rates are already evaluated, target states are not
	// but are just stored as lists of updates (for efficiency)
	protected List<List<Update>> updates;
	protected List<Double> probability;

	/**
	 * Create empty choice.
	 */
	public ChoiceListFlexi()
	{
		updates = new ArrayList<List<Update>>();
		probability = new ArrayList<Double>();
	}

	/**
	 * Copy constructor.
	 * NB: Does a shallow, not deep, copy with respect to references to Update objects.
	 */
	public ChoiceListFlexi(ChoiceListFlexi ch)
	{
		moduleOrActionIndex = ch.moduleOrActionIndex;
		updates = new ArrayList<List<Update>>(ch.updates.size());
		for (List<Update> list : ch.updates) {
			List<Update> listNew = new ArrayList<Update>(list.size()); 
			updates.add(listNew);
			for (Update up : list) {
				listNew.add(up);
			}
		}
		probability = new ArrayList<Double>(ch.size());
		for (double p : ch.probability) {
			probability.add(p);
		}
	}

	// Set methods

	/**
	 * Set the module/action for this choice, encoded as an integer
	 * (-i for independent in ith module, i for synchronous on ith action)
	 * (in both cases, modules/actions are 1-indexed)
	 */
	public void setModuleOrActionIndex(int moduleOrActionIndex)
	{
		this.moduleOrActionIndex = moduleOrActionIndex;
	}

	/**
	 * Add a transition to this choice.
	 * @param probability Probability (or rate) of the transition
	 * @param ups List of Update objects defining transition
	 */
	public void add(double probability, List<Update> ups)
	{
		this.updates.add(ups);
		this.probability.add(probability);
	}

	@Override
	public void scaleProbabilitiesBy(double d)
	{
		int i, n;
		n = size();
		for (i = 0; i < n; i++) {
			probability.set(i, probability.get(i) * d);
		}
	}

	/**
	 * Modify this choice, constructing product of it with another.
	 */
	public void productWith(final ChoiceListFlexi other) throws PrismLangException
	{
		final List<List<Update>> oldUpdates = updates;
		final List<Double> oldProbability = probability;
		updates = new ArrayList<List<Update>>(updates.size() * other.updates.size());
		probability = new ArrayList<Double>(updates.size() * other.updates.size());

		// cross product over updates indices
		for (int i = oldUpdates.size() - 1; i >= 0; i--) {
			for (int j = other.updates.size() - 1; j >= 0; j--) {
				try {
					final List<Update> joined = joinUpdates(oldUpdates.get(i), other.updates.get(j));
					add(oldProbability.get(i) * other.probability.get(j), joined);
				} catch (PrismLangException e) {
					updates = oldUpdates;
					probability = oldProbability;
					throw e;
				}
			}
		}
	}

	private List<Update> joinUpdates(final List<Update> updatesA, final List<Update> updatesB)
			throws PrismLangException
	{
		List<Update> joined = new ChainedList<>(updatesA, updatesB);
		return resolveConflicts(joined);
	}

	private List<Update> resolveConflicts(final List<Update> updates) throws PrismLangException
	{
		BitSet conflicts = getWriteConflicts(updates);
		@SuppressWarnings("unchecked")
		List<Update>[] conflictingUpdates = new List[conflicts.length()];
		List<Update> resolvedUpdates = new ArrayList<Update>();

		// check for each update whether it writes a conflicting variable
		for (Update update : updates) {
			Update resolved = update;
			for (OfInt vars = new IterableBitSet(update.getWrittenVariables()).iterator(); vars.hasNext();) {
				int var = vars.nextInt();
				if (conflicts.get(var)) {
					// create split update in conflicting and non-conflicting writes
					if (resolved == update) {
						resolved = (Update) update.deepCopy();
						resolved.setParent(update.getParent());
					}
					if (conflictingUpdates[var] == null) {
						conflictingUpdates[var] = new ArrayList<>();
					}
					conflictingUpdates[var].add(resolved.split(var));
				}
			}
			resolvedUpdates.add(resolved);
		}
		// create cumulative updates from conflicting writes
		for (int var=0, size=conflictingUpdates.length; var<size; var++) {
			if (conflictingUpdates[var] != null) {
				resolvedUpdates.add(cumulateUpdatesForVariable(conflictingUpdates[var], var));
			}
		}
		return resolvedUpdates;
	}

	private BitSet getWriteConflicts(final List<Update> updates)
	{
		final BitSet allWritten = new BitSet();
		final BitSet allConflicts = new BitSet();
		for (Update update : updates) {
			BitSet written = update.getWrittenVariables();
			BitSet conflicts = (BitSet) written.clone();
			conflicts.and(allWritten);
			allConflicts.or(conflicts);
			allWritten.or(written);
		}
		return allConflicts;
	}

	private Update cumulateUpdatesForVariable(final List<Update> updates, final int variable) throws PrismLangException
	{
		assert updates.size() > 0 : "at least one update expected";

		Update joinedUpdate = null;
		for (Update update : updates) {
			try {
				joinedUpdate = cumulateUpdatesForVariable(joinedUpdate, update, variable);
			} catch (PrismLangException e) {
				e.printStackTrace();
				final ExpressionIdent varIdent = update.getVarIdentFromIndex(variable);
				final String message = "non-cumulative conflicting updates on shared variable in synchronous transition";
				String action = update.getParent().getParent().getSynch();
				action = "".equals(action) ? "" : " [" + action + "]";
				throw new PrismLangException(message + action, varIdent);
			}
		}
		return joinedUpdate;
	}

	private Update cumulateUpdatesForVariable(final Update updateA, final Update updateB, final int variable) throws PrismLangException
	{
		return updateA == null ? updateB : updateA.cummulateUpdatesForVariable(updateB, variable);
	}

	// Get methods

	@Override
	public int getModuleOrActionIndex()
	{
		return moduleOrActionIndex;
	}

	@Override
	public String getModuleOrAction()
	{
		// Action label (or absence of) will be the same for all updates in a choice
		Update u = updates.get(0).get(0);
		Command c = u.getParent().getParent();
		if ("".equals(c.getSynch()))
			return c.getParent().getName();
		else
			return "[" + c.getSynch() + "]";
	}

	@Override
	public int size()
	{
		return probability.size();
	}

	@Override
	public String getUpdateString(int i, State currentState) throws PrismLangException
	{
		int j, n;
		String s = "";
		boolean first = true;
		for (Update up : updates.get(i)) {
			n = up.getNumElements();
			for (j = 0; j < n; j++) {
				if (first)
					first = false;
				else
					s += ", ";
				s += up.getVar(j) + "'=" + up.getExpression(j).evaluate(currentState);
			}
		}
		return s;
	}

	@Override
	public String getUpdateStringFull(int i)
	{
		String s = "";
		boolean first = true;
		for (Update up : updates.get(i)) {
			if (up.getNumElements() == 0)
				continue;
			if (first)
				first = false;
			else
				s += " & ";
			s += up;
		}
		return s;
	}

	@Override
	public State computeTarget(int i, State currentState) throws PrismLangException
	{
		State newState = new State(currentState);
		for (Update up : updates.get(i))
			up.update(currentState, newState);
		return newState;
	}

	@Override
	public void computeTarget(int i, State currentState, State newState) throws PrismLangException
	{
		for (Update up : updates.get(i))
			up.update(currentState, newState);
	}

	@Override
	public double getProbability(int i)
	{
		return probability.get(i);
	}

	@Override
	public double getProbabilitySum()
	{
		double sum = 0.0;
		for (double d : probability)
			sum += d;
		return sum;
	}

	@Override
	public int getIndexByProbabilitySum(double x)
	{
		int i, n;
		double d;
		n = size();
		d = 0.0;
		for (i = 0; x >= d && i < n; i++) {
			d += probability.get(i);
		}
		return i - 1;
	}

	@Override
	public void checkValid(ModelType modelType) throws PrismException
	{
		// Currently nothing to do here:
		// Checks for bad probabilities/rates done earlier.
	}
	
	@Override
	public void checkForErrors(State currentState, VarList varList) throws PrismException
	{
		int i, n;
		n = size();
		for (i = 0; i < n; i++) {
			for (Update up : updates.get(i))
				up.checkUpdate(currentState, varList);
		}
	}
	
	@Override
	public String toString()
	{
		int i, n;
		boolean first = true;
		String s = "";
		n = size();
		for (i = 0; i < n; i++) {
			if (first)
				first = false;
			else
				s += " + ";
			s += getProbability(i) + ":" + updates.get(i);
		}
		return s;
	}
}
