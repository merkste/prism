package explicit.quantile.context;

import java.util.BitSet;
import java.util.Map;

import parser.ast.ExpressionQuantileProbNormalForm;
import prism.PrismException;
import explicit.quantile.QuantileUtilities;
import explicit.quantile.context.helpers.QuantitativeCalculationHelper;
import explicit.quantile.context.helpers.PrecomputationHelper;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.RewardWrapper;

public class Context4ExpressionQuantileProbLowerRewardBound extends Context4ExpressionQuantileProb
{

	public Context4ExpressionQuantileProbLowerRewardBound(RewardWrapper theModel,
			BitSet theZeroStateRewardStatesWithZeroRewardTransition, BitSet theZeroStateRewardStatesWithMixedTransitionRewards, BitSet theInvariantStates, BitSet theGoalStates, BitSet theZeroProbabilityStates, double[] theExtremalProbabilities,
			BitSet theStatesOfInterest, QuantileUtilities theQuantileUtilities, ExpressionQuantileProbNormalForm theExpressionQuantile)
	{
		super(theModel,
				theZeroStateRewardStatesWithZeroRewardTransition, theZeroStateRewardStatesWithMixedTransitionRewards, theInvariantStates, theGoalStates, theZeroProbabilityStates, theExtremalProbabilities,
				theStatesOfInterest, theQuantileUtilities, theExpressionQuantile);
		isExistential = theExpressionQuantile.isExistential();
	}

	private final boolean isExistential;

	@Override
	public boolean pickMaximum()
	{
		return !isExistential;
	}

	@Override
	public Map<Double, BitSet> determineFiniteQuantileStates() throws PrismException
	{
		double[] result;
		if (isExistential){
			result = PrecomputationHelper.existentialReachabilityProbabilitiesLowerRewardBound(this, quantileUtilities.getProbModelChecker());
			PrecomputationHelper.logPrecomputedValues("Pmin(G a & GF b & GF posR)", statesOfInterest, result, quantileUtilities.getLog());
		} else {
			result = PrecomputationHelper.universalReachabilityProbabilitiesLowerRewardBound(this, quantileUtilities.getProbModelChecker());
			PrecomputationHelper.logPrecomputedValues("Pmax(\"can satisfy a U>=r b with ever increasing reward\")", statesOfInterest, result, quantileUtilities.getLog());
		}
		return PrecomputationHelper.finiteQuantileStatesMap(getRelationOperator(), result, quantileUtilities.getThresholds());
	}

	@Override
	public void calculateDerivableStates(CalculatedValues values, int rewardStep)
	{
		assert (rewardStep > 0);
		//states having zero probability  ==>  0
		values.setCurrentValues(zeroValueStates, 0);
		//states having a positive reward
		QuantitativeCalculationHelper.calculatePositiveRewardStates4LowerRewardBoundedQuantile(positiveRewardStates, model, null, values, extremalProbabilities, rewardStep, pickMaximum(), quantileUtilities.getWorkerPoolForPositiveRewardStates());
	}

	@Override
	public double calculatePositiveRewardTransitionForZeroRewardState(final int state, final int choice, final CalculatedValues values, final int rewardStep)
	{
		return QuantitativeCalculationHelper.valueForPositiveRewardTransition4LowerRewardBoundedQuantile(model, 0, values, extremalProbabilities, state, choice, 0, rewardStep);
	}

	@Override
	public int getNumberOfDerivableStates()
	{
		return zeroValueStates.size() + positiveRewardStates.size();
	}

	@Override
	public double[] calculateQualitativeQuantile(int threshold)
	{
		throw new RuntimeException("NOT yet implemented");
	}
}