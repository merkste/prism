package explicit.quantile.dataStructure.previousValues;
/**
 * Interface to store the values calculated in previous quantile-iterations.
 * @author Marcus Daum (mdaum@tcs.inf.tu-dresden.de)
 */
public interface PreviousValues
{
	default int getIndexOfState(final int state)
	{
		return state;
	}

	public void mergeValuesIntoPreviousValues(final int rewardStep, final double[] currentValues);

	default double getPreviousValue(final int state, final int rewardStep)
	{
		return getPreviousValues(rewardStep)[getIndexOfState(state)];
	}

	public double[] getPreviousValues(final int rewardStep);

	default int getOffset(final int index, final int maxIndex)
	{
		assert (index >= 0);
		assert (maxIndex > 0);
		return (index + maxIndex) % maxIndex;
	}

	default boolean expensiveArrayGeneration()
	{
		return false;
	}

	default String getInfoString()
	{
		return "previously calculated values store ";
	}
}