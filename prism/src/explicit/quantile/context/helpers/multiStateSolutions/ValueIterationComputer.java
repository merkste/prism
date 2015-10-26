package explicit.quantile.context.helpers.multiStateSolutions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import explicit.quantile.context.Context;
import explicit.quantile.context.Context4ExpressionQuantileProb;
import explicit.quantile.context.helpers.QuantitativeCalculationHelper;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.Pair;
import explicit.quantile.dataStructure.RewardWrapper;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;

public class ValueIterationComputer extends IterationComputer
{
	protected static double getExtremalValue(CalculatedValues values, Context context)
	{
		if (context instanceof Context4ExpressionQuantileProb) {
			if (context.pickMaximum()){
				return 0;
			}
			return 1;
		}
		return values.getMinimalValue();
	}

	private static Pair<double[], Map<Integer, Integer>> initialiseValueVectorAndStateToArrayMapping(CalculatedValues values, Set<Integer> set, Context context, Map<Integer, Double> positiveRewardChoiceValues)
	{
		final double extremalValue = getExtremalValue(values, context);
		final int elements = set.size();
		final double[] result = new double[elements];
		final Map<Integer, Integer> stateToArrayIndex = new HashMap<>(elements);
		int index = 0;
		for (int state : set){
			stateToArrayIndex.put(state, index);
			final Double positiveRewardChoiceValue = positiveRewardChoiceValues.get(state);
			if (positiveRewardChoiceValue != null){
				result[index] = positiveRewardChoiceValue;
			} else {
				result[index] = extremalValue;
			}
			index++;
		}
		return new Pair<>(result, stateToArrayIndex);
	}

	public static Pair<double[], Map<Integer, Integer>> valueIteration(Context context, CalculatedValues values, Set<Integer> set, int rewardStep, Set<Integer> statesWithMixedTransitionRewards, int maxIters, double epsilon,
			boolean absoluteConvergence, PrismLog log, int debugLevel) throws PrismException
	{
		//XXX: maximale Tiefe von set berechnen
		//XXX: alle Werte muessen mindestens einmal durchpropagiert werden
		assert (set != null);
		if (debugLevel >= 3)
			log.println("Starting value iteration ...");
		RewardWrapper model = context.getModel();
		final Map<Integer, Double> positiveRewardChoiceValues = getValuesForPositiveRewardTransitions(context, getStatesWithPositiveRewardTransitions(set, statesWithMixedTransitionRewards), values, rewardStep);
		Pair<double[], Map<Integer, Integer>> initialised = initialiseValueVectorAndStateToArrayMapping(values, set, context, positiveRewardChoiceValues);
		double[] lastIteration = initialised.getFirst();
		double[] currentIteration = new double[lastIteration.length];
		final Map<Integer, Integer> stateToArrayIndex = initialised.getSecond();
		boolean finished = false;
		for (int iteration = 1; iteration < maxIters; iteration++){
			if (debugLevel >= 5)
				log.println("\niteration " + iteration + " ...");
			for (int state : set){
				assert (context.getModel().getStateReward(state) == 0);
				final int index = stateToArrayIndex.get(state);
				boolean thisIsTheFirstChoice = true;
				for (int choice = 0, numChoices = model.getNumChoices(state); choice < numChoices; choice++){
					if (model.getTransitionReward(state, choice) == 0){
						double valueForChoice = valueSumZeroRewardTransition(model.getDistributionIterable(state, choice), lastIteration, values, stateToArrayIndex);
						if (thisIsTheFirstChoice){
							thisIsTheFirstChoice = false;
							if (debugLevel >= 5){
								log.println("X_" + state + " = " + valueForChoice);
							}
							currentIteration[index] = valueForChoice;
						} else {
							if (debugLevel >= 5){
								log.println("X_" + state + " = " + (context.pickMaximum() ? "max(" : "min(") + currentIteration[index] + " ; " + valueForChoice + ")");
							}
							currentIteration[index] = QuantitativeCalculationHelper.pickReasonableValue(currentIteration[index], valueForChoice, context.pickMaximum());
						}
					}
				}
				final Double positiveRewardChoiceValue = positiveRewardChoiceValues.get(state);
				if (positiveRewardChoiceValue != null){
					if (debugLevel >= 5)
						log.println("X_" + state + " = " + (context.pickMaximum() ? "max(" : "min(") + currentIteration[index] + " ; " + positiveRewardChoiceValue + ")  (positive reward choice)");
					currentIteration[index] = QuantitativeCalculationHelper.pickReasonableValue(currentIteration[index], positiveRewardChoiceValue, context.pickMaximum());
				}
			}
			if (debugLevel >= 5)
				log.println("result = " + Arrays.toString(currentIteration));
			if (PrismUtils.doublesAreClose(currentIteration, lastIteration, epsilon, absoluteConvergence)) {
				if (debugLevel >= 4)
					log.println("The value iteration approach took " + iteration + " iterations.");
				finished = true;
				break;
			}
			final double[] swapper = currentIteration;
			currentIteration = lastIteration;
			lastIteration = swapper;
		}
		if (!finished){
			throw new PrismException("The value iteration approach did not converge within " + maxIters
					+ " steps. Try a higher iteration bound or relax the termination epsilon");
		}
		return new Pair<>(currentIteration, stateToArrayIndex);
	}
}