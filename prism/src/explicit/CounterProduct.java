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
import java.util.List;

import common.SafeCast;

import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import prism.IntegerBound;
import prism.PrismException;

/**
 * A product of a DTMC or MDP with a reward counter.
 *
 * @param <M> the model type
 */
public class CounterProduct<M extends Model> extends ProductWithProductStates<M>
{
	/** Map from reward r to states with accumulated reward r */
	private ArrayList<BitSet> accRewardToStates;
	/** The highest accumulated reward that is of interest */
	private int limit;

	/**
	 * Constructor for a counter product (called from {@code generate()}).
	 * @param originalModel the original model
	 * @param limit the highest accumulated reward of interest,
	 *        i.e., all accumulated rewards >= limit are identified with limit
	 */
	private CounterProduct(M originalModel, int limit) {
		super(originalModel);
		this.limit = limit;
		accRewardToStates = new ArrayList<BitSet>();
		for (int i=0;i<=limit;i++) {
			accRewardToStates.add(new BitSet());
		}
	}

	/**
	 * Get the states in the product for a given accumulated reward.
	 * If acc_reward is >= limit, then the states with rewards beyond the
	 * limit are returned.
	 */
	BitSet getStatesWithAccumulatedReward(int acc_reward) {
		if (acc_reward >= limit) {
			return accRewardToStates.get(limit);
		} else {
			return accRewardToStates.get(acc_reward);
		}
	}

	/**
	 * Get the states in the product inside a given integer bound.
	 */
	BitSet getStatesWithAccumulatedRewardInBound(IntegerBound bound) {
		BitSet result = new BitSet();
		for (int r=0; r<=bound.getMaximalInterestingValue(); r++) {
			if (bound.isInBounds(r)) {
				result.or(getStatesWithAccumulatedReward(r));
			}
		}
		return result;
	}

	/**
	 * Generate the product of a MDP with an accumulated reward counter.
	 * The counter has the range [0,limit], with saturation semantics for accumulated
	 * rewards >=limit.
	 * @param graph the MDP
	 * @param rewards integer MCRewards
	 * @param limit the saturation value for the counter
	 * @param statesOfInterest the set of state of interest, the starting point for the counters
	 * @return
	 * @throws PrismException
	 */
	static public CounterProduct<MDP> generate(final MDP graph, final MDPRewards rewards, final int limit, BitSet statesOfInterest) throws PrismException {
		final CounterProduct<MDP> result = new CounterProduct<MDP>(graph, limit);

		class RewardProductOperator implements MDPProductOperator {
			@Override
			public ProductState getInitialState(Integer dtmc_state)
			{
				return new ProductState(dtmc_state, 0);
			}

			@Override
			public ProductState getSuccessor(ProductState from_state, int choice_i, Integer dtmc_to_state) throws PrismException
			{
				// the accumulated reward
				int acc_reward = from_state.getSecondState();

				// reward(s) is accumulated on transition *from* s
				int state_reward = SafeCast.toInt(rewards.getStateReward(from_state.getFirstState()));
				// transition reward depends on choice_i
				int transition_reward = SafeCast.toInt(rewards.getTransitionReward(from_state.getFirstState(), choice_i));

				int next_reward = acc_reward + state_reward + transition_reward;
				if (next_reward >= limit) {
					// saturated
					next_reward = limit;
				}
				return new ProductState(dtmc_to_state, next_reward);
			}

			@Override
			public void notify(ProductState state, Integer index) throws PrismException
			{
				// store product state in the corresponding bitset of accRewardToStates
				int reward = state.getSecondState().intValue();
				result.accRewardToStates.get(reward).set(index.intValue());
			}

			@Override
			public MDP getGraph()
			{
				return graph;
			}

			@Override
			public void finish() throws PrismException {
				// nothing to do
			}
		};

		RewardProductOperator product_op = new RewardProductOperator();

		ProductWithProductStates.generate(product_op, result, statesOfInterest);

		return result;
	}


	/**
	 * Get the states in the product DTMC inside the conjunction of integer bounds,
	 * given as a List of IntegerBounds.
	 */
	BitSet getStatesWithAccumulatedRewardInBoundConjunction(List<IntegerBound> bounds) {
		BitSet result = new BitSet();
		for (int r=0; r<=limit; r++) {
			if (IntegerBound.isInBoundForConjunction(bounds, r)) {
				result.or(getStatesWithAccumulatedReward(r));
			}
		}
		return result;
	}

	/**
	 * Generate the product of a DTMC with an accumulated reward counter.
	 * The counter has the range [0,limit], with saturation semantics for accumulated
	 * rewards >=limit.
	 * @param graph the DTMC
	 * @param rewards integer MCRewards
	 * @param limit the saturation value for the counter
	 * @param statesOfInterest the set of state of interest, the starting point for the counters
	 * @return
	 */
	static public CounterProduct<DTMC> generate(final DTMC graph, final MCRewards rewards, final int limit, BitSet statesOfInterest) throws PrismException {
		final CounterProduct<DTMC> result = new CounterProduct<DTMC>(graph, limit);

		class CounterProductOperator implements DTMCProductOperator {
			@Override
			public ProductState getInitialState(Integer dtmc_state)
			{
				return new ProductState(dtmc_state, 0);
			}

			@Override
			public ProductState getSuccessor(ProductState from_state, Integer dtmc_to_state) throws PrismException
			{
				// the accumulated reward
				int acc_reward = from_state.getSecondState();

				// reward(s) is accumulated on transition *from* s
				int state_reward = SafeCast.toInt(rewards.getStateReward(from_state.getFirstState()));

				int next_reward = acc_reward + state_reward;
				if (next_reward >= limit) {
					// saturated
					next_reward = limit;
				}
				return new ProductState(dtmc_to_state, next_reward);
			}

			@Override
			public void notify(ProductState state, Integer index) throws PrismException
			{
				// store product state in the corresponding bitset of accRewardToStates
				int reward = state.getSecondState().intValue();
				result.accRewardToStates.get(reward).set(index.intValue());
			}

			@Override
			public DTMC getGraph()
			{
				return graph;
			}

			@Override
			public void finish() throws PrismException {
				// nothing to do
			}
		};

		CounterProductOperator product_op = new CounterProductOperator();

		ProductWithProductStates.generate(product_op, result, statesOfInterest);

		return result;
	}
}
