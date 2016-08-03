package explicit.quantile.context.helpers.multiStateSolutions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import explicit.quantile.context.Context;
import explicit.quantile.context.helpers.QuantitativeCalculationHelper;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.RewardWrapper;

public abstract class IterationComputer
{
	protected static Set<Integer> getStatesWithPositiveRewardTransitions(Set<Integer> set, Set<Integer> statesWithMixedTransitionRewards)
	{
		Set<Integer> clone = new HashSet<>(set.size());
		clone.addAll(set);
		clone.retainAll(statesWithMixedTransitionRewards);
		return clone;
	}

	protected static Map<Integer, Double> getValuesForPositiveRewardTransitions(Context context, Set<Integer> statesWithPositiveRewardTransition, CalculatedValues values, int rewardStep)
	{
		final Map<Integer, Double> valuesForPositiveRewardTransitions = new HashMap<>(statesWithPositiveRewardTransition.size());
		final RewardWrapper model = context.getModel();
		for (int state : statesWithPositiveRewardTransition){
			double value = CalculatedValues.UNDEFINED;
			for (int choice = 0, numChoices = model.getNumChoices(state); choice < numChoices; choice++){
				if (model.getTransitionReward(state, choice) > 0){
					value = QuantitativeCalculationHelper.pickReasonableValue(value, context.calculatePositiveRewardTransitionForZeroRewardState(state, choice, values, rewardStep), context.pickMaximum());
				}
			}
			assert (value >= 0);
			valuesForPositiveRewardTransitions.put(state, value);
		}
		return valuesForPositiveRewardTransitions;
	}

	protected static double valueSumZeroRewardTransition(Iterable<Entry<Integer, Double>> successorDistribution, double[] lastIteration, CalculatedValues values, Map<Integer, Integer> stateToArrayIndex)
	{
		double actualValue = 0;
		for (final Map.Entry<Integer, Double> entry : successorDistribution){
			final double referencedValue;
			final Integer successor = entry.getKey();
			final Integer successorInsideSet = stateToArrayIndex.get(successor);
			if (successorInsideSet != null){
				//reference inside current set
				referencedValue = lastIteration[successorInsideSet];
			} else {
				//reference to already known value outside current set
				referencedValue = values.getCurrentValue(successor);
				if (referencedValue == CalculatedValues.UNDEFINED){
					throw new RuntimeException("The topological sorting of the SCCs may be incorrect!\nSuccessor " + successor + " was not yet calculated");
				}
			}
			actualValue += entry.getValue() * referencedValue;
		}
		return actualValue;
	}
}