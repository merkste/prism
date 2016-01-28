package explicit.quantile.dataStructure;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map.Entry;

import prism.PrismComponent;
import prism.PrismException;
import explicit.DTMC;
import explicit.SCCComputer;
import explicit.SCCConsumer;
import explicit.rewards.MCRewards;

public class RewardWrapperDTMC extends RewardWrapper
{

	private DTMC dtmc;
	private MCRewards rewards;

	public RewardWrapperDTMC(PrismComponent parent, DTMC dtmc, MCRewards rewards)
	{
		super(parent);
		this.dtmc = dtmc;
		this.rewards = rewards;
	}

	@Override
	public DTMC getModel()
	{
		return dtmc;
	}

	@Override
	public MCRewards getRewards()
	{
		return rewards;
	}

	@Override
	public int getNumChoices(int state)
	{
		// DTMC only has one (virtual) choice
		return 1;
	}

	@Override
	public int getStateReward(int state)
	{
		return (int) rewards.getStateReward(state);
	}

	@Override
	public int getTransitionReward(int state, int choice)
	{
		// DTMC has no transition rewards (yet)
		return 0;
	}

	@Override
	public Iterable<Entry<Integer, Double>> getDistributionIterable(final int state, int choice)
	{
		assert (choice == 0) : "Choice " + choice + " is invalid for DTMC";

		return new Iterable<Entry<Integer, Double>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> iterator()
			{
				return dtmc.getTransitionsIterator(state);
			}
		};
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		assert (choice == 0) : "Choice " + choice + " is invalid for DTMC";

		return dtmc.getSuccessorsIterator(state);
	}

	public void computeStronglyConnectedComponents(SCCConsumer sccConsumer) throws PrismException
	{
		//XXX: write on-the-fly SCCComputer like for ECs in MDPs
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, getModel(), sccConsumer);
		sccComputer.computeSCCs();
	}

	/**
	 * Computes the set of states s such that the intersection of <code>successors</code> and the successors of s are non-empty.
	 * @param successors
	 * @return
	 */
	public BitSet statesWithSomeSuccessors(BitSet successors)
	{
		BitSet states = new BitSet();
		for (int state = 0, numStates = dtmc.getNumStates(); state < numStates; state++) {
			if (dtmc.someSuccessorsInSet(state, successors))
				states.set(state);
		}
		return states;
	}

	/**
	 * Computes the set of states s such that the successors of s are contained in <code>successors</code>.
	 * @param successors
	 * @return
	 */
	public BitSet statesWithAllSuccessors(BitSet successors)
	{
		BitSet states = new BitSet();
		for (int state = 0, numStates = dtmc.getNumStates(); state < numStates; state++) {
			if (dtmc.allSuccessorsInSet(state, successors))
				states.set(state);
		}
		return states;
	}

	@Override
	public boolean allSuccessorsInSet(final int state, final int choice, final BitSet successors)
	{
		assert (choice == 0) : "Choice " + choice + " is invalid for DTMC";

		return dtmc.allSuccessorsInSet(state, successors);
	}
}
