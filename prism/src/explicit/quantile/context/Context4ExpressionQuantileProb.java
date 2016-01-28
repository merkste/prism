package explicit.quantile.context;

import java.util.BitSet;
import java.util.Set;

import parser.ast.ExpressionQuantileProbNormalForm;
import parser.ast.RelOp;
import explicit.quantile.QuantileUtilities;
import explicit.quantile.context.helpers.QuantitativeCalculationHelper;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.RewardWrapper;

public abstract class Context4ExpressionQuantileProb extends Context
{

	public Context4ExpressionQuantileProb(RewardWrapper theModel,
			BitSet theZeroStateRewardStatesWithZeroRewardTransition, BitSet theZeroStateRewardStatesWithMixedTransitionRewards, BitSet theInvariantStates, BitSet theGoalStates, BitSet theZeroProbabilityStates, double[] theExtremalProbabilities,
			BitSet theStatesOfInterest, QuantileUtilities theQuantileUtilities, ExpressionQuantileProbNormalForm theExpressionQuantile)
	{
		super(theModel,
				theZeroStateRewardStatesWithZeroRewardTransition, theZeroStateRewardStatesWithMixedTransitionRewards, theZeroProbabilityStates,
				theStatesOfInterest, theQuantileUtilities);
		invariantStates = quantileUtilities.getSetFactory().getSet(theInvariantStates);
		goalStates = quantileUtilities.getSetFactory().getSet(theGoalStates);
		expressionQuantile = theExpressionQuantile;
		extremalProbabilities = theExtremalProbabilities;
	}

	protected final ExpressionQuantileProbNormalForm expressionQuantile;

	@Override
	public int getResultAdjustment()
	{
		return expressionQuantile.getResultAdjustment();
	}

	@Override
	public RelOp getRelationOperator()
	{
		return expressionQuantile.getProbabilityRelation();
	}

	@Override
	protected double calculateZeroRewardTransitionForZeroRewardState(final int state, final int choice, final CalculatedValues values)
	{
		return QuantitativeCalculationHelper.valueForZeroRewardTransition(model, 0, state, choice, values.getCurrentValues());
	}

	protected final Set<Integer> invariantStates;

	public Set<Integer> getInvariantStates()
	{
		return invariantStates;
	}

	protected final Set<Integer> goalStates;

	public Set<Integer> getGoalStates()
	{
		return goalStates;
	}

	protected double[] extremalProbabilities;

	public double[] getExtremalProbabilities()
	{
		return extremalProbabilities;
	}

	public abstract double[] calculateQualitativeQuantile(int threshold);
}