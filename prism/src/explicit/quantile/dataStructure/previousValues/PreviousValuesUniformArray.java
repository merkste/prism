package explicit.quantile.dataStructure.previousValues;

import java.util.Arrays;

import explicit.quantile.dataStructure.CalculatedValues;

public abstract class PreviousValuesUniformArray extends PreviousValuesArray
{
	protected int rewardSteps;

	protected void initialisePreviousValues(final int theSteps, final int numberOfIndizes)
	{
		rewardSteps = theSteps;
		previousValues = new double[rewardSteps][numberOfIndizes];
		for (int step = 0; step < rewardSteps; step++){
			Arrays.fill(previousValues[step], CalculatedValues.UNDEFINED);
		}
	}

	protected int getOffset(final int index)
	{
		return getOffset(index, rewardSteps);
	}

	@Override
	public double[] getPreviousValues(final int rewardStep)
	{
		return previousValues[getOffset(rewardStep)];
	}

	@Override
	public String getInfoString()
	{
		final int states = previousValues[0].length;
		return super.getInfoString() + (states*rewardSteps) + " elements (" + states + " states * " + rewardSteps + " rewardSteps per state)";
	}
}