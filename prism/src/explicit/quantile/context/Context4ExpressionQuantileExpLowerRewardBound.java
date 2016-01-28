package explicit.quantile.context;

import java.util.BitSet;
import java.util.Map;

import parser.ast.ExpressionQuantileExpNormalForm;
import explicit.quantile.QuantileUtilities;
import explicit.quantile.context.helpers.QuantitativeCalculationHelper;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.RewardWrapper;

public class Context4ExpressionQuantileExpLowerRewardBound extends Context4ExpressionQuantileExp
{

	public Context4ExpressionQuantileExpLowerRewardBound(RewardWrapper theCostModel, RewardWrapper theValueModel,
			BitSet theZeroStateRewardStatesWithZeroRewardTransition, BitSet theZeroStateRewardStatesWithMixedTransitionRewards, BitSet theZeroValueStates, double[] theExtremalValues,
			BitSet theStatesOfInterest, QuantileUtilities theQuantileUtilities, ExpressionQuantileExpNormalForm theExpressionQuantile)
	{
		super(theCostModel, theValueModel,
				theZeroStateRewardStatesWithZeroRewardTransition, theZeroStateRewardStatesWithMixedTransitionRewards, theZeroValueStates,
				theStatesOfInterest, theQuantileUtilities, theExpressionQuantile);
		isExistential = theExpressionQuantile.isExistential();
		extremalValues = theExtremalValues;
	}

	private final double[] extremalValues;

	public double[] getExtremalValues()
	{
		return extremalValues;
	}

	private final boolean isExistential;

	@Override
	public boolean pickMaximum()
	{
		return !isExistential;
	}

	@Override
	public Map<Double, BitSet> determineFiniteQuantileStates()
	{
		throw new RuntimeException("not yet supported");
	}

	@Override
	public void calculateDerivableStates(CalculatedValues values, int rewardStep)
	{
		//states having value 0  ==>  0
		values.setCurrentValues(zeroValueStates, 0);
		//states having a positive reward
		QuantitativeCalculationHelper.calculatePositiveRewardStates4LowerRewardBoundedQuantile(positiveRewardStates, model, valueModel, values, extremalValues, rewardStep, pickMaximum(), quantileUtilities.getWorkerPoolForPositiveRewardStates());
	}

	@Override
	public double calculatePositiveRewardTransitionForZeroRewardState(final int state, final int choice, final CalculatedValues values, final int rewardStep)
	{
		return QuantitativeCalculationHelper.valueForPositiveRewardTransition4LowerRewardBoundedQuantile(model, valueModel.getStateReward(state)+valueModel.getTransitionReward(state, choice), values, extremalValues, state, choice, 0, rewardStep);
	}

	@Override
	public int getNumberOfDerivableStates()
	{
		return zeroValueStates.size() + positiveRewardStates.size();
	}
}