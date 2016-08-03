package explicit.quantile.context.helpers.multiStateSolutions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import explicit.quantile.context.Context4ExpressionQuantileProb;
import explicit.quantile.context.helpers.QuantitativeCalculationHelper;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.Pair;
import explicit.quantile.dataStructure.RewardWrapper;
import explicit.quantile.dataStructure.Triplet;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import prism.PrismUtils;

public class IntervalIterationComputer extends IterationComputer
{
	private static Triplet<double[], double[], Map<Integer, Integer>> initialiseValueVectorsAndStateToArrayMapping(Context4ExpressionQuantileProb context, CalculatedValues values, Set<Integer> set, Map<Integer, Double> positiveRewardChoiceValues)
	{
		final int elements = set.size();
		final double[] lowerBoundForResult = new double[elements];
		final double[] upperBoundForResult = new double[elements];
		final Map<Integer, Integer> stateToArrayIndex = new HashMap<>(elements);
		int index = 0;
		for (int state : set){
			stateToArrayIndex.put(state, index);
			lowerBoundForResult[index] = 0;
			upperBoundForResult[index] = 1;
			index++;
		}
		return new Triplet<>(lowerBoundForResult, upperBoundForResult, stateToArrayIndex);
	}

	public static Pair<double[], Map<Integer, Integer>> intervalIteration(Context4ExpressionQuantileProb context, CalculatedValues values, Set<Integer> set, int rewardStep, Set<Integer> statesWithMixedTransitionRewards, int maxIters, double epsilon,
			boolean absoluteConvergence, PrismLog log, int debugLevel) throws PrismException
	{
		assert (set != null);
		if (context.pickMaximum()){
			//XXX: unbedingt implementieren!!!
			//XXX: unbedingt implementieren!!!
			//XXX: unbedingt implementieren!!!
			//XXX: unbedingt implementieren!!!
			//XXX: unbedingt implementieren!!!
			throw new PrismNotSupportedException("currently interval iteration is not supported for maximising properties");
		}
		if (debugLevel >= 3)
			log.println("Starting interval iteration ...");
		RewardWrapper model = context.getModel();
		final Map<Integer, Double> positiveRewardChoiceValues = getValuesForPositiveRewardTransitions(context, getStatesWithPositiveRewardTransitions(set, statesWithMixedTransitionRewards), values, rewardStep);
		Triplet<double[], double[], Map<Integer, Integer>> initialised = initialiseValueVectorsAndStateToArrayMapping(context, values, set, positiveRewardChoiceValues);
		double[] lastIterationLowerBound = initialised.getFirst();
		double[] currentIterationLowerBound = new double[lastIterationLowerBound.length];
		double[] lastIterationUpperBound = initialised.getSecond();
		double[] currentIterationUpperBound = new double[lastIterationUpperBound.length];
		final Map<Integer, Integer> stateToArrayIndex = initialised.getThird();
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
						double lowerValueForChoice = valueSumZeroRewardTransition(model.getDistributionIterable(state, choice), lastIterationLowerBound, values, stateToArrayIndex);
						double upperValueForChoice = valueSumZeroRewardTransition(model.getDistributionIterable(state, choice), lastIterationUpperBound, values, stateToArrayIndex);
						if (thisIsTheFirstChoice){
							thisIsTheFirstChoice = false;
							if (debugLevel >= 5){
								log.println("X_" + state + " = " + lowerValueForChoice + "  (lowerBound)");
								log.println("X_" + state + " = " + upperValueForChoice + "  (upperBound)");
							}
							currentIterationLowerBound[index] = lowerValueForChoice;
							currentIterationUpperBound[index] = upperValueForChoice;
						} else {
							if (debugLevel >= 5){
								log.println("X_" + state + " = " + (context.pickMaximum() ? "max(" : "min(") + currentIterationLowerBound[index] + " ; " + lowerValueForChoice + ")  (lowerBound)");
								log.println("X_" + state + " = " + (context.pickMaximum() ? "max(" : "min(") + currentIterationUpperBound[index] + " ; " + upperValueForChoice + ")  (upperBound)");
							}
							currentIterationLowerBound[index] = QuantitativeCalculationHelper.pickReasonableValue(currentIterationLowerBound[index], lowerValueForChoice, context.pickMaximum());
							currentIterationUpperBound[index] = QuantitativeCalculationHelper.pickReasonableValue(currentIterationUpperBound[index], upperValueForChoice, context.pickMaximum());
						}
					}
				}
				final Double positiveRewardChoiceValue = positiveRewardChoiceValues.get(state);
				if (positiveRewardChoiceValue != null){
					if (debugLevel >= 5){
						log.println("X_" + state + " = " + (context.pickMaximum() ? "max(" : "min(") + currentIterationLowerBound[index] + " ; " + positiveRewardChoiceValue + ")  (lowerBound -- positive reward choice)");
						log.println("X_" + state + " = " + (context.pickMaximum() ? "max(" : "min(") + currentIterationUpperBound[index] + " ; " + positiveRewardChoiceValue + ")  (upperBound -- positive reward choice)");
					}
					currentIterationLowerBound[index] = QuantitativeCalculationHelper.pickReasonableValue(currentIterationLowerBound[index], positiveRewardChoiceValue, context.pickMaximum());
					currentIterationUpperBound[index] = QuantitativeCalculationHelper.pickReasonableValue(currentIterationUpperBound[index], positiveRewardChoiceValue, context.pickMaximum());
				}
			}
			if (debugLevel >= 5){
				log.println("result lower bound = " + Arrays.toString(currentIterationLowerBound));
				log.println("result upper bound = " + Arrays.toString(currentIterationUpperBound));
			}
			if (PrismUtils.doublesAreClose(currentIterationLowerBound, currentIterationUpperBound, epsilon, absoluteConvergence)){
				if (debugLevel >= 4)
					log.println("The interval iteration approach took " + iteration + " iterations.");
				finished = true;
				break;
			}
			double[] swapper = currentIterationLowerBound;
			currentIterationLowerBound = lastIterationLowerBound;
			lastIterationLowerBound = swapper;
			swapper = currentIterationUpperBound;
			currentIterationUpperBound = lastIterationUpperBound;
			lastIterationUpperBound = swapper;
		}
		if (!finished){
			throw new PrismException("The interval iteration approach did not converge within " + maxIters
					+ " steps. Try a higher iteration bound or relax the termination epsilon");
		}
		return new Pair<>(currentIterationLowerBound, stateToArrayIndex);
	}
}