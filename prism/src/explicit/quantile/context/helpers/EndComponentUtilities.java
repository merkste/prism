package explicit.quantile.context.helpers;

import java.util.BitSet;
import java.util.Set;

import common.iterable.collections.BitSetWrapper;
import explicit.quantile.dataStructure.RewardWrapper;

public final class EndComponentUtilities
{
	/**
	 * Given a set of states forming an end component, checks whether there is a transition from a state in the end component back
	 * to the end component with positive transition reward
	 */
	private static boolean positiveChoiceRewardExistsForState(RewardWrapper model, int state, BitSet endComponent)
	{
		if (!model.hasTransitionRewards()){
			return false;
		}
		for (int choice = 0, choices = model.getNumChoices(state); choice < choices; choice++){
			if (model.getTransitionReward(state, choice) > 0 && model.allSuccessorsInSet(state, choice, endComponent)){
				// positive reward and all successors are in the end component
				//  => the action is a transition inside the end component
				return true;
			}
		}
		return false;
	}

	@SafeVarargs
	private static boolean containsOnlyExpectedStates(BitSet endComponent, Set<Integer> ... expectedStates)
	{
		Set<Integer> reduced = new BitSetWrapper((BitSet) endComponent.clone());
		for (Set<Integer> states : expectedStates){
			reduced.removeAll(states);
		}
		return reduced.isEmpty();
	}

	public static boolean containsOnlyExpectedStates(BitSet endComponent, BitSet ... expectedStates)
	{
		BitSet reduced = (BitSet) endComponent.clone();
		for (BitSet states : expectedStates){
			reduced.andNot(states);
		}
		return reduced.isEmpty();
	}

	static boolean positiveRewardExists(RewardWrapper model, BitSet endComponent)
	{
		for (int state = endComponent.nextSetBit(0); state >= 0; state = endComponent.nextSetBit(state+1)){
			if (model.getStateReward(state) > 0 || positiveChoiceRewardExistsForState(model, state, endComponent)){
				return true;
			}
		}
		return false;
	}

	@SafeVarargs
	static boolean isProperPositiveRewardEndComponent(RewardWrapper model, BitSet endComponent, Set<Integer> ... expectedStates)
	{
		return (containsOnlyExpectedStates(endComponent, expectedStates) && positiveRewardExists(model, endComponent));
	}
}