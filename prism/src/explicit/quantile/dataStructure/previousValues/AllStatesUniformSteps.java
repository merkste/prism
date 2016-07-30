package explicit.quantile.dataStructure.previousValues;

public class AllStatesUniformSteps extends PreviousValuesUniformArray
{
	public AllStatesUniformSteps(final int theSteps, final int numberOfStates)
	{
		initialisePreviousValues(theSteps, numberOfStates);
	}

	@Override
	public void mergeValuesIntoPreviousValues(final int rewardStep, final double[] currentValues)
	{
		final int offset = getOffset(rewardStep);
		System.arraycopy(currentValues, 0, previousValues[offset], 0, currentValues.length);
	}
}