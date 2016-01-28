package explicit.quantile.dataStructure;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import common.iterable.collections.AdaptiveSet;
import explicit.DTMC;
import explicit.MDP;
import explicit.Model;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;
import explicit.rewards.StateRewardsConstant;

public abstract class RewardWrapper extends PrismComponent
{

	RewardWrapper(PrismComponent parent)
	{
		super(parent);
	}

	private int maximumReward = 0;

	/** Returns the wrapped model. */
	public abstract Model getModel();

	/** Returns the wrapped Rewards. */
	public abstract Rewards getRewards();

	/** Returns the model type. */
	public ModelType getModelType()
	{
		return getModel().getModelType();
	}

	/** Returns the number of states. */
	public int getNumStates()
	{
		return getModel().getNumStates();
	}

	/** Returns the number of choices for {@code state}. 
	 * In case of a wrapped DTMC, return 1 (virtual choice)
	 */
	public abstract int getNumChoices(int state);

	/** Get the state reward for state {@code state} */
	public abstract int getStateReward(int state);

	/** Get the transition reward for state {@code state} and choice {@code choice}. */
	public abstract int getTransitionReward(int state, int choice);

	private static void checkIfPositive(int value) throws PrismException
	{
		if (value < 0)
			throw new PrismException("Only rewards greater or equals 0 are supported");
	}

	public int getHighestReward() throws PrismException
	{
		if (maximumReward == 0) {
			if (getRewards() instanceof StateRewardsConstant){
				maximumReward = getStateReward(0);
				checkIfPositive(maximumReward);
				return maximumReward;
			}
			for (int state = 0, states = getNumStates(); state < states; state++){
				final int stateReward = getStateReward(state);
				checkIfPositive(stateReward);
				for (int choice = 0, numberOfChoices = getNumChoices(state); choice < numberOfChoices; choice++){
					final int transitionReward = getTransitionReward(state, choice);
					checkIfPositive(transitionReward);
					final int sumOfRewards = stateReward + transitionReward;
					if (sumOfRewards > maximumReward)
						maximumReward = sumOfRewards;
				}
			}
			if (maximumReward == 0)
				throw new PrismException("The given reward structure does not define any reward at all, so the calculation will be aborted");
		}
		return maximumReward;
	}

	public int[] getHighestReferencingReward() throws PrismException
	{
		final int numStates = getNumStates();
		final int[] rewards = new int[numStates];
		if (getRewards() instanceof StateRewardsConstant){
			final int highestReward = getStateReward(0);
			checkIfPositive(highestReward);
			Arrays.fill(rewards, highestReward);
			return rewards;
		}
		for (int state = 0; state < numStates; state++){
			final int stateReward = getStateReward(state);
			checkIfPositive(stateReward);
			for (int choice = 0, numberOfChoices = getNumChoices(state); choice < numberOfChoices; choice++){
				final int transitionReward = getTransitionReward(state, choice);
				checkIfPositive(transitionReward);
				final int sumOfRewards = stateReward + transitionReward;
				if (sumOfRewards > 0){
					for (Iterator<Integer> successorsIterator = getSuccessorsIterator(state, choice); successorsIterator.hasNext();){
						final int successor = successorsIterator.next();
						if (sumOfRewards > rewards[successor]){
							rewards[successor] = sumOfRewards;
						}
					}
				}
			}
		}
		return rewards;
	}

	public abstract boolean allSuccessorsInSet(final int state, final int choice, final BitSet successors);

	public Set<Integer> getZeroRewardChoices(int state)
	{
		//XXX: make it adaptable
		Set<Integer> zeroRewardChoices = new AdaptiveSet(2);
		for (int choice = 0, choices = getNumChoices(state); choice < choices; choice++) {
			if (getTransitionReward(state, choice) == 0)
				zeroRewardChoices.add(choice);
		}
		return zeroRewardChoices;
	}

	public Set<Integer> getPositiveRewardChoices(int state)
	{
		//XXX: make it adaptable
		Set<Integer> positiveRewardChoices = new AdaptiveSet(2);
		for (int choice = 0, choices = getNumChoices(state); choice < choices; choice++) {
			if (getTransitionReward(state, choice) > 0)
				positiveRewardChoices.add(choice);
		}
		return positiveRewardChoices;
	}

	public BitSet getZeroStateRewardStates()
	{
		BitSet result = new BitSet();
		for (int state = 0, states = getNumStates(); state < states; state++)
			if (getStateReward(state) == 0)
				result.set(state);
		return result;
	}

	public BitSet getPositiveStateRewardStates()
	{
		BitSet result = new BitSet();
		for (int state = 0, states = getNumStates(); state < states; state++)
			if (getStateReward(state) > 0)
				result.set(state);
		return result;
	}

	public BitSet getZeroRewardForAtLeastOneChoiceStates()
	{
		BitSet result = new BitSet();
		for (int state = 0, states = getNumStates(); state < states; state++) {
			if (!getZeroRewardChoices(state).isEmpty())
				result.set(state);
		}
		return result;
	}

	public BitSet getZeroRewardForAllChoicesStates()
	{
		BitSet result = new BitSet();
		for (int state = 0, states = getNumStates(); state < states; state++) {
			if (getZeroRewardChoices(state).size() == getNumChoices(state))
				result.set(state);
		}
		return result;
	}

	public BitSet getPositiveRewardForAllChoicesStates()
	{
		BitSet result = new BitSet();
		for (int state = 0, states = getNumStates(); state < states; state++) {
			if (getZeroRewardChoices(state).isEmpty())
				result.set(state);
		}
		return result;
	}

	public BitSet getMixedChoicesRewardStates()
	{
		BitSet result = new BitSet();
		for (int state = 0, states = getNumStates(); state < states; state++) {
			int numberOfZeroRewardChoices = getZeroRewardChoices(state).size();
			if ((0 < numberOfZeroRewardChoices) && (numberOfZeroRewardChoices < getNumChoices(state)))
				result.set(state);
		}
		return result;
	}

	public BitSet getPositiveRewardForAtLeastOneChoiceStates()
	{
		BitSet result = new BitSet();
		for (int state = 0, states = getNumStates(); state < states; state++) {
			if (!getPositiveRewardChoices(state).isEmpty())
				result.set(state);
		}
		return result;
	}

	public Set<Integer> getZeroRewardSuccessors(int state)
	{
		Set<Integer> successors = new HashSet<Integer>();
		addZeroRewardSuccessors(state, successors, null);
		return successors;
	}

	public Set<Integer> getZeroRewardSuccessors(Set<Integer> states)
	{
		Set<Integer> successors = new HashSet<Integer>();
		for (int state : states)
			addZeroRewardSuccessors(state, successors, states);
		return successors;
	}

	public Set<Integer> getZeroRewardSuccessors(Set<Integer> states, Set<Integer> allowedStates)
	{
		Set<Integer> successors = new HashSet<Integer>();
		for (int state : states){
			for (int choice : getZeroRewardChoices(state)){
				final Set<Integer> currentSuccessors = new HashSet<Integer>();
				for (Iterator<Integer> successorsIterator = getSuccessorsIterator(state, choice); successorsIterator.hasNext();) {
					final int successor = successorsIterator.next();
					if (allowedStates.contains(successor) && !states.contains(successor)){
						currentSuccessors.add(successor);
					}
				}
				successors.addAll(currentSuccessors);
			}
		}
		return successors;
	}

	public Set<Integer> getZeroRewardSuccessors(Set<Integer> states, Set<Integer> allowedStates, final int[] lut)
	{
		Set<Integer> successors = new HashSet<Integer>();
		for (int state : states){
			for (int choice : getZeroRewardChoices(state)){
				final Set<Integer> currentSuccessors = new HashSet<Integer>();
				for (Iterator<Integer> successorsIterator = getSuccessorsIterator(state, choice); successorsIterator.hasNext();) {
					final int successor = successorsIterator.next();
					if (allowedStates.contains(successor) && !states.contains(successor)){
						currentSuccessors.add(lut[successor]);
					}
				}
				successors.addAll(currentSuccessors);
			}
		}
		return successors;
	}

	private void addZeroRewardSuccessors(int state, Set<Integer> successors, Set<Integer> states)
	{
		Set<Integer> zeroRewardChoices = getZeroRewardChoices(state);
		for (int choice : zeroRewardChoices) {
			Set<Integer> currentSuccessors = new HashSet<Integer>();
			for (Iterator<Integer> successorsIterator = getSuccessorsIterator(state, choice); successorsIterator.hasNext();) {
				int successor = successorsIterator.next();
				if (states == null || !states.contains(successor))
					//if just interested in a state on its own, then each successor needs to be added
					//if interested in a state inside a given SCC, then only add successors not inside the SCC
					currentSuccessors.add(successor);
			}
			successors.addAll(currentSuccessors);
		}
	}

	private void getPositiveRewardSuccessors(int state, BitSet positiveRewardSuccessors)
	{
		if (getStateReward(state) > 0) {
			//positive state reward
			//  ->  each successor is a positive reward successor
			for (int choice = 0, choices = getNumChoices(state); choice < choices; choice++)
				for (Iterator<Integer> successorsIterator = getSuccessorsIterator(state, choice); successorsIterator.hasNext();)
					positiveRewardSuccessors.set(successorsIterator.next());
			return;
		}
		//state reward is 0
		//  ->  only successors of positive reward transitions are positive reward successors
		for (int choice : getPositiveRewardChoices(state))
			for (Iterator<Integer> successorsIterator = getSuccessorsIterator(state, choice); successorsIterator.hasNext();)
				positiveRewardSuccessors.set(successorsIterator.next());
		return;
	}

	public BitSet getPositiveRewardSuccessors()
	{
		long start = System.currentTimeMillis();
		BitSet positiveRewardSuccessors = new BitSet();
		for (int state = 0, states = getNumStates(); state < states; state++)
			getPositiveRewardSuccessors(state, positiveRewardSuccessors);
		mainLog.print(
				"\nThere are " + positiveRewardSuccessors.cardinality() + " successors of positive rewards and their calculation took "
						+ (System.currentTimeMillis() - start) / 1000.0 + " seconds.");
		return positiveRewardSuccessors;
	}

	/** Returns an iterable over the distribution for state {@code state} and choice {@code choice}. */
	public abstract Iterable<Map.Entry<Integer, Double>> getDistributionIterable(final int state, final int choice);

	public abstract Iterator<Integer> getSuccessorsIterator(final int state, final int choice);

	/** Returns true if there might be some transition with positive reward. */
	public boolean hasTransitionRewards()
	{
		return getRewards().hasTransitionRewards();
	}

	public static RewardWrapper wrapModelAndReward(PrismComponent parent, Model model, Rewards rewards) throws PrismException
	{
		switch (model.getModelType()) {
		case DTMC:
			return new RewardWrapperDTMC(parent, (DTMC) model, (MCRewards) rewards);
		case MDP:
			return new RewardWrapperMDP(parent, (MDP) model, (MDPRewards) rewards);
		default:
			throw new PrismException("Quantiles do not support model type " + model.getModelType());
		}
	}
}
