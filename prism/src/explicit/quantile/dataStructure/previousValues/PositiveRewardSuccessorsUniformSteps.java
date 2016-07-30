package explicit.quantile.dataStructure.previousValues;

import java.util.BitSet;

public class PositiveRewardSuccessorsUniformSteps extends PreviousValuesUniformArray
{
	protected final SpecificStates stateToArrayIndex;

	public PositiveRewardSuccessorsUniformSteps(final int theSteps, final BitSet thePositiveRewardSuccessors, final boolean considerParallelComputations)
	{
		stateToArrayIndex = new SpecificStates(thePositiveRewardSuccessors, considerParallelComputations);
		initialisePreviousValues(theSteps, thePositiveRewardSuccessors.cardinality());
	}

	@Override
	public void mergeValuesIntoPreviousValues(final int rewardStep, final double[] currentValues)
	{
		final int offset = getOffset(rewardStep);
		for (int state : stateToArrayIndex.getStates())
			previousValues[offset][getIndexOfState(state)] = currentValues[state];
	}

	@Override
	public int getIndexOfState(final int state)
	{
		return stateToArrayIndex.getIndexOfState(state);
	}

	@Override
	public String getInfoString()
	{
		return super.getInfoString() + " + mapping for " + previousValues[0].length + " states";
	}
}