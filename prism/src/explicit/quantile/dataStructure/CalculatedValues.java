package explicit.quantile.dataStructure;

import java.util.Arrays;
import java.util.Set;

import common.iterable.collections.AdaptiveSet;
import explicit.quantile.dataStructure.previousValues.PreviousValues;
import explicit.ExportIterations;
import explicit.quantile.dataStructure.previousValues.AllStatesUniformSteps;
import explicit.quantile.dataStructure.previousValues.PositiveRewardSuccessorsUniformSteps;
import explicit.quantile.dataStructure.previousValues.PositiveRewardSuccessorsIndividualSteps;
import prism.PrismException;

/**
 * This class is responsible for the storage of each value calculated during the quantile computation:
 * <ul>
 * <li>a vector with the calculated values so far inside the actual iteration</li>
 * <li>a list with the calculated values for positive reward transitions of zero-reward states inside the actual iteration</li>
 * <li>the calculated values of previous iterations</li>
 * </ul>
 * @author Marcus Daum (mdaum@tcs.inf.tu-dresden.de)
 */
public class CalculatedValues
{
	public static final int UNDEFINED = -1;

	public enum PreviousValuesStorage {
		ALL_STATES {
			@Override
			public PreviousValues getPreviousValuesStorage(final RewardWrapper model, final boolean considerParallelComputations) throws PrismException
			{
				return new AllStatesUniformSteps(model.getHighestReward(), model.getNumStates());
			}
		},
		POS_REWARD_SUCCS_UNIFORM {
			@Override
			public PreviousValues getPreviousValuesStorage(final RewardWrapper model, final boolean considerParallelComputations) throws PrismException
			{
				return new PositiveRewardSuccessorsUniformSteps(model.getHighestReward(), model.getPositiveRewardSuccessors(), considerParallelComputations);
			}
		},
		POS_REWARD_SUCCS_INDIVIDUAL {
			public PreviousValues getPreviousValuesStorage(final RewardWrapper model, final boolean considerParallelComputations) throws PrismException
			{
				return new PositiveRewardSuccessorsIndividualSteps(model.getHighestReferencingReward(), model.getPositiveRewardSuccessors(), considerParallelComputations);
			}
		};

		public abstract PreviousValues getPreviousValuesStorage(final RewardWrapper model, final boolean considerParallelComputations) throws PrismException;
	}

	private final PreviousValues previousValues;
	private final double[] currentValues;

	public CalculatedValues(final RewardWrapper model, final String storage, final boolean considerParallelComputations) throws PrismException
	{
		previousValues = PreviousValuesStorage.valueOf(storage).getPreviousValuesStorage(model, considerParallelComputations);
		currentValues = new double[model.getNumStates()];
		Arrays.fill(currentValues, UNDEFINED);
	}
	
	public void exportCurrentValues(ExportIterations export)
	{
		export.exportVector(currentValues);
	}

	public double[] getCurrentValues()
	{
		return currentValues;
	}

	public double getCurrentValue(final int state)
	{
		return getCurrentValues()[state];
	}

	public void setCurrentValues(final Iterable<Integer> states, final double value)
	{
		for (int state : states)
			currentValues[state] = value;
	}

	public void replaceCurrentValues(final double[] values)
	{
		assert (currentValues.length == values.length): "The length of currentValues and values differs!";
		System.arraycopy(values, 0, currentValues, 0, values.length);
	}

	public void setCurrentValue(final int state, final double value)
	{
		assert (currentValues[state] == CalculatedValues.UNDEFINED);
		assert (value != CalculatedValues.UNDEFINED);
		currentValues[state] = value;
	}

	public Set<Integer> getUndefinedStates()
	{
		Set<Integer> undefinedStates = new AdaptiveSet(2);
		int state = 0;
		for (double value : currentValues) {
			if (value == UNDEFINED)
				undefinedStates.add(state);
			state++;
		}
		return undefinedStates;
	}

	public boolean allStatesAreDefined()
	{
		for (double value : currentValues)
			if (value == UNDEFINED)
				return false;
		return true;
	}

	public boolean allStatesAreDefined(final Iterable<Integer> states)
	{
		for (int state : states)
			if (currentValues[state] == UNDEFINED)
				return false;
		return true;
	}

	public double getMinimalValue()
	{
		double minimalValue = Double.POSITIVE_INFINITY;
		for (double value : currentValues) {
			if (value == 0)
				return 0;
			if (value != UNDEFINED && value < minimalValue)
				minimalValue = value;
		}
		if (minimalValue == Double.POSITIVE_INFINITY){
			//so far, only infinity values are known
			//this means that the states have only references to unknown states
			return 0;
		}
		return minimalValue;
	}

	public double getMaximalValue()
	{
		double maximalValue = 0;
		for (double value : currentValues) {
			if (value == Double.POSITIVE_INFINITY)
				return Double.POSITIVE_INFINITY;
			if (value != UNDEFINED && value > maximalValue)
				maximalValue = value;
		}
		return maximalValue;
	}

	public void mergeCurrentValuesIntoPreviousValues(final int rewardStep)
	{
		previousValues.mergeValuesIntoPreviousValues(rewardStep, currentValues);
		//current values are merged in --> prepare array for the next iteration
		Arrays.fill(currentValues, UNDEFINED);
	}

	public boolean expensiveArrayGeneration()
	{
		return previousValues.expensiveArrayGeneration();
	}

	public double getPreviousValue(final int state, final int rewardStep)
	{
		return previousValues.getPreviousValue(state, rewardStep);
	}

	public double[] getPreviousValues(final int rewardStep)
	{
		return previousValues.getPreviousValues(rewardStep);
	}

	public int getIndexOfState(final int state)
	{
		return previousValues.getIndexOfState(state);
	}

	public String previousValuesInfoString()
	{
		return previousValues.getInfoString();
	}

	public String toString()
	{
		return "currentValues: " + Arrays.toString(currentValues);
	}
}