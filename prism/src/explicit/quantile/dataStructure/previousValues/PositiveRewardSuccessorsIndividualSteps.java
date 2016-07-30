package explicit.quantile.dataStructure.previousValues;

import java.util.BitSet;
import java.util.Set;

public class PositiveRewardSuccessorsIndividualSteps extends PreviousValuesIndividualArray
{
	protected final SpecificStates stateToArrayIndex;

	public PositiveRewardSuccessorsIndividualSteps(final int[] highestReferencingRewards, final BitSet thePositiveRewardSuccessors, final boolean considerParallelComputations)
	{
		stateToArrayIndex = new SpecificStates(thePositiveRewardSuccessors, considerParallelComputations);
		initialisePreviousValues(stateToArrayIndex, highestReferencingRewards);
	}

	@Override
	public double[] getPreviousValues(final int rewardStep)
	{
		final Set<Integer> states = stateToArrayIndex.getStates();
		final double[] result = new double[states.size()];
		for (int state : states){
			final int stateIndex = stateToArrayIndex.getIndexOfState(state);
			result[stateIndex] = previousValues[stateIndex][getOffset(rewardStep, previousValues[stateIndex].length)];
		}
		return result;
	}

	@Override
	public double getPreviousValue(final int state, final int rewardStep)
	{
		final int stateIndex = stateToArrayIndex.getIndexOfState(state);
		return previousValues[stateIndex][getOffset(rewardStep, previousValues[stateIndex].length)];
	}

	@Override
	public void mergeValuesIntoPreviousValues(final int rewardStep, final double[] currentValues)
	{
		for (int state : stateToArrayIndex.getStates()){
			final int stateIndex = stateToArrayIndex.getIndexOfState(state);
			previousValues[stateIndex][getOffset(rewardStep, previousValues[stateIndex].length)] = currentValues[state];
		}
	}

	@Override
	public int getIndexOfState(final int state)
	{
		return stateToArrayIndex.getIndexOfState(state);
	}

	@Override
	public boolean expensiveArrayGeneration()
	{
		return true;
	}

	@Override
	public String getInfoString()
	{
		return super.getInfoString() + " + mapping for " + previousValues.length + " states";
	}
}