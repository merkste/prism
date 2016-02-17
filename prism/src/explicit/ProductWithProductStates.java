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

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.Map.Entry;

import common.iterable.IterableStateSet;
import parser.State;
import prism.PrismException;

/**
 * Provides functionality for generating a Product via a
 * DTMCProductOperator / MDPProductOperator.
 * <br>
 * The initial states in the product correspond to the lifted
 * states of interest in the original model.
 *
 * @param <M> the model type
 */
public class ProductWithProductStates<M extends Model> extends Product<M> {
	/** Mapping index to ProductState */
	protected Vector<ProductState> prod_states = new Vector<ProductState>();

	/** Constructor */
	protected ProductWithProductStates(M originalModel)
	{
		super(originalModel);
	}

	/** Get the product state information for a given state index in the product */
	public ProductState getState(int index)
	{
		return prod_states.get(index);
	}

	@Override
	public int getModelState(int productState)
	{
		return getState(productState).getFirstState();
	}

	@Override
	public int getAutomatonState(int productState)
	{
		return getState(productState).getSecondState();
	}

	/**
	 * Generates the DTMC for a {@code ProductWithProductStates<DTMC>}
	 * using the operator {@code op}, storing the result in
	 * {@product}.
	 * @param op the ProductOperator
	 * @param product for storing the result
	 * @param statesOfInterest the states of interest in the DTMC, null = all states
	 */
	static public void generate(final DTMCProductOperator op, final ProductWithProductStates<DTMC> product, BitSet statesOfInterest) throws PrismException
	{
		final DTMC dtmc = op.getGraph();
		final DTMCSimple dtmcProduct = new DTMCSimple();
		final ArrayList<State> statesList = new ArrayList<State>();
		final Vector<ProductState> prod_states = new Vector<ProductState>();

		@SuppressWarnings("serial")
		class ProductStateMap extends HashMap<ProductState, Integer>
		{
			public Integer findOrAdd(ProductState state) throws PrismException
			{
				Integer index = get(state);
				if (index == null) {
					index = Integer.valueOf(dtmcProduct.addState());
					put(state, index);
					if (index != prod_states.size()) {
						throw new PrismException("Implementation error in product construction!");
					}
					prod_states.add(state);
					statesList.add(dtmc.getStatesList().get(state.getFirstState()));

					// notify operator of the index
					op.notify(state, index);
				}
				return index;
			}
		};

		ProductStateMap mapping = new ProductStateMap();
		HashSet<ProductState> expanded = new HashSet<ProductState>();
		LinkedList<ProductState> todo = new LinkedList<ProductState>();

		for (int index : new IterableStateSet(statesOfInterest, dtmc.getNumStates())) {
			ProductState prod_initial = op.getInitialState(index);
			Integer s = mapping.findOrAdd(prod_initial);
			todo.add(prod_initial);
			dtmcProduct.addInitialState(s);
		}

		while (!todo.isEmpty()) {
			ProductState cur = todo.remove(0);
			if (expanded.contains(cur)) {
				// cur has been expanded in the time between insertion into todo and now
				continue;
			}
			// mark as expanded
			expanded.add(cur);

			Integer from_index = mapping.get(cur);

			// expand cur
			Iterator<Entry<Integer, Double>> transitions = dtmc.getTransitionsIterator(cur.getFirstState());
			while (transitions.hasNext()) {
				Entry<Integer, Double> transition = transitions.next();

				ProductState to_state = op.getSuccessor(cur, transition.getKey());
				if (!expanded.contains(to_state)) {
					todo.add(to_state);
				}
				Integer to_index = mapping.findOrAdd(to_state);

				dtmcProduct.addToProbability(from_index.intValue(), to_index.intValue(), transition.getValue());
			}
		}

		op.finish();

		dtmcProduct.setStatesList(statesList);
		product.productModel = dtmcProduct;
		product.prod_states = prod_states;

		// lift labels
		for (String label : dtmc.getLabels()) {
			dtmcProduct.addLabel(label, product.liftFromModel(dtmc.getLabelStates(label)));
		}
	}

	/**
	 * Generates the MDP for a {@code ProductWithProductStates<MDP>}
	 * using the operator {@code op}, storing the result in
	 * {@product}.
	 * @param op the ProductOperator
	 * @param product for storing the result
	 * @param statesOfInterest the states of interest in the MDP, null = all states
	 */
	static public void generate(final MDPProductOperator op, final ProductWithProductStates<MDP> product, BitSet statesOfInterest) throws PrismException
	{
		final MDP mdp = op.getGraph();
		final MDPSimple mdpProduct = new MDPSimple();
		final ArrayList<State> statesList = new ArrayList<State>();
		final Vector<ProductState> prod_states = new Vector<ProductState>();

		@SuppressWarnings("serial")
		class ProductStateMap extends HashMap<ProductState, Integer>
		{
			public Integer findOrAdd(ProductState state) throws PrismException
			{
				Integer index = get(state);
				if (index == null) {
					index = Integer.valueOf(mdpProduct.addState());
					put(state, index);
					if (index != prod_states.size()) {
						throw new PrismException("Implementation error in product construction!");
					}
					prod_states.add(state);
					statesList.add(mdp.getStatesList().get(state.getFirstState()));

					// notify operator of the index
					op.notify(state, index);
				}
				return index;
			}
		};

		ProductStateMap mapping = new ProductStateMap();
		HashSet<ProductState> expanded = new HashSet<ProductState>();
		LinkedList<ProductState> todo = new LinkedList<ProductState>();

		for (int index : new IterableStateSet(statesOfInterest, mdp.getNumStates())) {
			ProductState prod_initial = op.getInitialState(index);
			Integer s = mapping.findOrAdd(prod_initial);
			todo.add(prod_initial);
			mdpProduct.addInitialState(s);
		}

		while (!todo.isEmpty()) {
			ProductState cur = todo.remove(0);
			if (expanded.contains(cur)) {
				// cur has been expanded in the time between insertion into todo and now
				continue;
			}
			// mark as expanded
			expanded.add(cur);

			Integer from_index = mapping.get(cur);

			// expand cur
			int choices = mdp.getNumChoices(cur.getFirstState());
			for (int choice_i = 0; choice_i<choices; choice_i++) {
				Distribution successors = new Distribution();
				Iterator<Entry<Integer, Double>> transitions = mdp.getTransitionsIterator(cur.getFirstState(), choice_i);
				while (transitions.hasNext()) {
					Entry<Integer, Double> transition = transitions.next();

					ProductState to_state = op.getSuccessor(cur, choice_i, transition.getKey());
					if (!expanded.contains(to_state)) {
						todo.add(to_state);
					}
					Integer to_index = mapping.findOrAdd(to_state);

					successors.add(to_index.intValue(), transition.getValue());
				}

				mdpProduct.addChoice(from_index.intValue(), successors);
			}
		}

		op.finish();

		mdpProduct.setStatesList(statesList);
		product.productModel = mdpProduct;
		product.prod_states = prod_states;

		// lift labels
		for (String label : mdp.getLabels()) {
			mdpProduct.addLabel(label, product.liftFromModel(mdp.getLabelStates(label)));
		}
	}
}
