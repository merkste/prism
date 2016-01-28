package explicit.quantile.context;

import java.util.BitSet;

import parser.ast.ExpressionQuantileExpNormalForm;
import parser.ast.RelOp;
import explicit.quantile.QuantileUtilities;
import explicit.quantile.context.helpers.QuantitativeCalculationHelper;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.RewardWrapper;

public abstract class Context4ExpressionQuantileExp extends Context
{

	public Context4ExpressionQuantileExp(RewardWrapper theCostModel, RewardWrapper theValueModel,
			BitSet theZeroStateRewardStatesWithZeroRewardTransition, BitSet theZeroStateRewardStatesWithMixedTransitionRewards, BitSet theZeroValueStates,
			BitSet theStatesOfInterest, QuantileUtilities theQuantileUtilities, ExpressionQuantileExpNormalForm theExpressionQuantile)
	{
		super(theCostModel,
				theZeroStateRewardStatesWithZeroRewardTransition, theZeroStateRewardStatesWithMixedTransitionRewards, theZeroValueStates,
				theStatesOfInterest, theQuantileUtilities);
		valueModel = theValueModel;
		expressionQuantile = theExpressionQuantile;
	}

	protected final ExpressionQuantileExpNormalForm expressionQuantile;

	protected final RewardWrapper valueModel;

	public RewardWrapper getValueModel()
	{
		return valueModel;
	}

	@Override
	public int getResultAdjustment()
	{
		//XXX:
		return 0;
	}

	@Override
	public RelOp getRelationOperator()
	{
		//XXX:
		return RelOp.GT;
	}

	@Override
	protected double calculateZeroRewardTransitionForZeroRewardState(final int state, final int choice, final CalculatedValues values)
	{
		return QuantitativeCalculationHelper.valueForZeroRewardTransition(model, valueModel.getStateReward(state)+valueModel.getTransitionReward(state, choice), state, choice, values.getCurrentValues());
	}
}