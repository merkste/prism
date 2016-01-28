package explicit.quantile.context.helpers;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.RewardWrapper;

public abstract class QuantitativeCalculationHelper
{
	private static double valueSum(final RewardWrapper model, final int additionalValue, final int state, final int choice, final int rewardStep, final CalculatedValues calculated)
	{
		assert (model.getStateReward(state) > 0) | (model.getTransitionReward(state, choice) > 0);
		double actualValue = 0;
		for (Map.Entry<Integer, Double> entry : model.getDistributionIterable(state, choice)) {
			final int successor = entry.getKey();
			final double referencedValue = calculated.getPreviousValue(successor, rewardStep);
			assert (referencedValue != CalculatedValues.UNDEFINED) : "Successor " + successor + " is NOT defined!";
			actualValue += entry.getValue() * referencedValue;
		}
		return actualValue + additionalValue;
	}

	/**
	 * Calculate the value sum of the successors of zero reward states using zero reward transitions in the setting of expectation quantiles.
	 */
	public static double valueForZeroRewardTransition(RewardWrapper model, int additionalValue, int state, int choice, double[] values)
	{
		return valueSum(model, additionalValue, state, choice, values, null, true);
	}

	/**
	 * Calculate the value sum of the successors of zero reward states using zero reward transitions in the setting of probability quantiles.
	 */
	public static double valueForZeroRewardTransition(RewardWrapper model, int state, int choice, double[] values)
	{
		return valueSum(model, 0, state, choice, values, null, true);
	}

	private static double valueSum(RewardWrapper model, int additionalValue, int state, int choice, double[] values, CalculatedValues calculated,
			boolean zeroReward)
	{
		assert ((zeroReward & (model.getStateReward(state) == 0) & (model.getTransitionReward(state, choice) == 0)) |
				(!zeroReward & ((model.getStateReward(state) != 0) | (model.getTransitionReward(state, choice) != 0)))) :
					"The zeroReward-flag was not set properly!";
		double actualValue = 0;
		double selfLoopProbability = 0;
		for (Map.Entry<Integer, Double> entry : model.getDistributionIterable(state, choice)) {
			int successorIndex = entry.getKey();
			double successorProbability = entry.getValue();
			if (zeroReward && (state == successorIndex)) {
				//if the considered edge is a self loop and there is no reward
				selfLoopProbability = successorProbability;
			} else
				actualValue += weightedSuccessorValue(successorIndex, successorProbability, calculated, values);
		}
		if (zeroReward)
			return considerSelfLoop(selfLoopProbability, actualValue, additionalValue);
		return actualValue + additionalValue;
	}

	private static double weightedSuccessorValue(int successorIndex, double successorProbability, CalculatedValues calculated, double[] values)
	{
		//zero reward -> references only inside actual iteration -> no remapping needed
		if (calculated != null)
			//positive reward -> references to already stored values -> index-remapping needed
			successorIndex = calculated.getIndexOfState(successorIndex);
		double referencedValue = values[successorIndex];
		assert (referencedValue != CalculatedValues.UNDEFINED) : "Successor " + successorIndex + " is NOT defined!";
		return successorProbability * referencedValue;
	}

	private static double considerSelfLoop(double selfLoopProbability, double remainingValueMass, int additionalValue)
	{
		if (selfLoopProbability == 1) {
			if (additionalValue > 0)
				//there is a possibility of accumulating arbitrary value
				return Double.POSITIVE_INFINITY;
			//the state has just itself as a successor
			//and as it belongs to Z, it cannot fulfill B, so it will never reach a B-labelled state
			//XXX:
			//XXX: ??? ist 0 wirklich das richtige Ergebnis bei lower reward bounded quantiles ???
			//XXX: zur Einordnung: das hier ist ein state mit state reward 0 und die fragliche aktion hat auch reward 0
			//XXX: fuer upper reward bounds ist das richtig, da der state nicht in B liegt und mit dieser aktion auch niemals das B erreichen kann
			//XXX: ---> deshalb 0
			//XXX: bei lower reward bounds kann der state in B liegen, er hat aber nicht a priori die wahrscheinlichkeit 0
			//XXX: ---> NOCHMAL NACHDENKEN
			//XXX:
			return 0;
		}
		//if the state has a self-loop, use the back reference in order to calculate the questioned value
		// ( additionalValue     => a [corresponds to zero for probability quantiles] )
		// ( selfLoopProbability => p )
		// ( remainingValueMass  => v )
		//given a = u_rew(s) + u_rew(s,alpha)  --  p = prob(s,s)  --  v = \sum_{s'!=s} prob(s,s') * x_{s',r}
		//the equation x_{s,r} = a + p * x_{s,r} + v needs to be solved:
		//     x_{s,r} = (a + v) / (1 - p)
		return (additionalValue + remainingValueMass) / (1 - selfLoopProbability);
	}

	public static double pickReasonableValue(double oldValue, double newValue, boolean pickMaximum)
	{
		assert (newValue >= 0);
		if (oldValue == CalculatedValues.UNDEFINED)
			return newValue;
		//can only happen if the model has nondeterministic choices
		//interested in a maximizing quantile?
		if (pickMaximum && (newValue > oldValue))
			return newValue;
		//interested in a minimizing quantile?
		if (!pickMaximum && (newValue < oldValue))
			return newValue;
		return oldValue;
	}

	/**
	 * Calculates the value for a positive reward transition in the upper reward bounded case.
	 * This means that either the state has a positive reward or that the observed transition has a positive reward!
	 */
	public static double valueForPositiveRewardTransition4UpperRewardBoundedQuantile(RewardWrapper model, int additionalValue, CalculatedValues values, int state, int choice, int stateReward, int rewardStep)
	{
		final int transitionReward = model.getTransitionReward(state, choice);
		if (stateReward + transitionReward > rewardStep)
			// rew(s) + rew(s,a) > r  ==>  0
			return 0;
		// 0 < rew(s) + rew(s,a) <= r  ==>  calculate
		if (values.expensiveArrayGeneration()){
			return valueSum(model, additionalValue, state, choice, rewardStep-stateReward-transitionReward, values);
		}
		return valueSum(model, additionalValue, state, choice, values.getPreviousValues(rewardStep-stateReward-transitionReward), values, false);
	}

	/**
	 * Calculates the value for a positive reward transition in the lower reward bounded case.
	 * This means that either the state has a positive reward or that the observed transition has a positive reward!
	 */
	public static double valueForPositiveRewardTransition4LowerRewardBoundedQuantile(RewardWrapper model, int additionalValue, CalculatedValues values, final double[] extremalValues, int state, int choice, int stateReward, int rewardStep)
	{
		// 0 < rew(s) < r
		final int transitionReward = model.getTransitionReward(state, choice);
		if (rewardStep <= stateReward + transitionReward){
			// r <= rew(s) + rew(s,a)  ==>  reference to extremal probabilities
			return valueSum(model, additionalValue, state, choice, extremalValues, null, false);
		}
		// r > rew(s) + rew(s,a)  ==>  reference to previous values
		if (values.expensiveArrayGeneration()){
			return valueSum(model, additionalValue, state, choice, rewardStep-stateReward-transitionReward, values);
		}
		return valueSum(model, additionalValue, state, choice, values.getPreviousValues(rewardStep-stateReward-transitionReward), values, false);
	}

	public static void calculatePositiveRewardStates4UpperRewardBoundedQuantile(Set<Integer> positiveRewardStates, RewardWrapper model,
			RewardWrapper valueAddition, CalculatedValues values, int rewardStep, boolean pickMaximum, ExecutorService workerPool)
	{
		if (workerPool != null){
			try {
				workerPool.submit(
					() -> positiveRewardStates.parallelStream().forEach(
							state -> values.setCurrentValue(state, getUpperRewardBoundedValueForPositiveRewardState(state, model, valueAddition, values, rewardStep, pickMaximum))
						)
				).get();
			} catch (InterruptedException | ExecutionException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			for (int state : positiveRewardStates){
				values.setCurrentValue(state, getUpperRewardBoundedValueForPositiveRewardState(state, model, valueAddition, values, rewardStep, pickMaximum));
			}
		}
	}

	/**
	 * the given state has a positive reward or each choice has a positive transition reward
	 */
	private static double getUpperRewardBoundedValueForPositiveRewardState(final int state, final RewardWrapper model, final RewardWrapper valueAddition, final CalculatedValues values, final int rewardStep, final boolean pickMaximum)
	{
		final int stateReward = model.getStateReward(state);
		if (rewardStep < stateReward) {
			// rew(s) > r  ==>  0
			return 0;
		}
		// 0 < rew(s) <= r
		double optimalValue = CalculatedValues.UNDEFINED;
		for (int choice = 0, numberOfChoices = model.getNumChoices(state); choice < numberOfChoices; choice++) {
			final int additionalValue = (valueAddition != null) ? (valueAddition.getStateReward(state) + valueAddition.getTransitionReward(state, choice)) : 0;
			optimalValue = pickReasonableValue(
					           optimalValue,
					           valueForPositiveRewardTransition4UpperRewardBoundedQuantile(model, additionalValue, values, state, choice, stateReward, rewardStep),
					           pickMaximum
					       );
			if (pickMaximum && optimalValue == 1)
				return 1;
			if (! pickMaximum && optimalValue == 0)
				return 0;
		}
		assert (optimalValue >= 0);
		return optimalValue;
	}

	public static void calculatePositiveRewardStates4LowerRewardBoundedQuantile(Set<Integer> positiveRewardStates, RewardWrapper model,
			RewardWrapper valueAddition, CalculatedValues values, double[] extremalValues, int rewardStep, boolean pickMaximum, ExecutorService workerPool)
	{
		if (workerPool != null){
			try {
				workerPool.submit(
					() -> positiveRewardStates.parallelStream().forEach(
							state -> values.setCurrentValue(state, getLowerRewardBoundedValueForState(state, model, valueAddition, values, extremalValues, rewardStep, pickMaximum))
						)
				).get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			for (int state : positiveRewardStates){
				values.setCurrentValue(state, getLowerRewardBoundedValueForState(state, model, valueAddition, values, extremalValues, rewardStep, pickMaximum));
			}
		}
	}

	private static double getLowerRewardBoundedValueForState(final int state, final RewardWrapper model, final RewardWrapper valueAddition, final CalculatedValues values, final double[] extremalValues, final int rewardStep, final boolean pickMaximum)
	{
		final int stateReward = model.getStateReward(state);
		double optimalValue = CalculatedValues.UNDEFINED;
		if (rewardStep <= stateReward) {
			// rew(s) >= r  ==>  reference to extremal probabilities
			for (int choice = 0, numberOfChoices = model.getNumChoices(state); choice < numberOfChoices; choice++) {
				final int additionalValue = (valueAddition != null) ? (valueAddition.getStateReward(state) + valueAddition.getTransitionReward(state, choice)) : 0;
				optimalValue = pickReasonableValue(
						           optimalValue,
						           valueSum(model, additionalValue, state, choice, extremalValues, null, false),
						           pickMaximum
						       );
				if (pickMaximum && optimalValue == 1)
					return 1;
				if (! pickMaximum && optimalValue == 0)
					return 0;
			}
			assert (optimalValue >= 0);
			return optimalValue;
		}
		// 0 < rew(s) < r
		for (int choice = 0, numberOfChoices = model.getNumChoices(state); choice < numberOfChoices; choice++) {
			final int additionalValue = (valueAddition != null) ? (valueAddition.getStateReward(state) + valueAddition.getTransitionReward(state, choice)) : 0;
			optimalValue = pickReasonableValue(
					           optimalValue,
					           valueForPositiveRewardTransition4LowerRewardBoundedQuantile(model, additionalValue, values, extremalValues, state, choice, stateReward, rewardStep),
					           pickMaximum
					       );
			if (pickMaximum && optimalValue == 1)
				return 1;
			if (! pickMaximum && optimalValue == 0)
				return 0;
		}
		assert (optimalValue >= 0);
		return optimalValue;
	}
}