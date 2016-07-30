package explicit.quantile.dataStructure.previousValues;

import java.util.Arrays;

import explicit.quantile.dataStructure.CalculatedValues;

public abstract class PreviousValuesIndividualArray extends PreviousValuesArray
{
	protected void initialisePreviousValues(final SpecificStates stateToArrayIndex, final int[] highestReferencingRewards)
	{
		//ATTENTION: in contrast to PreviousValuesUniformArray the first index is the state and the second the reward-step
		previousValues = new double[stateToArrayIndex.getNumberOfStates()][];
		for (int state : stateToArrayIndex.getStates()){
			final int index = stateToArrayIndex.getIndexOfState(state);
			previousValues[index] = new double[highestReferencingRewards[state]];
			Arrays.fill(previousValues[index], CalculatedValues.UNDEFINED);
		}
	}

	@Override
	public String getInfoString()
	{
		final int states = previousValues.length;
		int sum = 0;
		for (int state = 0; state < states; state++){
			sum += previousValues[state].length;
		}
		return super.getInfoString() + sum + " elements (" + states + " states with ~ " + (sum*1.0/states) + " rewardSteps per state)";
	}
}