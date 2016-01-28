package explicit.quantile.context;

import java.util.BitSet;
import java.util.Map;

import common.BitSetTools;
import parser.ast.ExpressionQuantileProbNormalForm;
import prism.ModelType;
import prism.PrismException;
import explicit.MDPSparse;
import explicit.modelviews.MDPRestricted;
import explicit.quantile.QuantileUtilities;
import explicit.quantile.context.helpers.QuantitativeCalculationHelper;
import explicit.quantile.context.helpers.PrecomputationHelper;
import explicit.quantile.context.helpers.QualitativeCalculationHelper;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.Pair;
import explicit.quantile.dataStructure.RewardWrapper;
import explicit.quantile.dataStructure.RewardWrapperMDP;
import explicit.rewards.MDPRewards;

public class Context4ExpressionQuantileProbUpperRewardBound extends Context4ExpressionQuantileProb
{

	public Context4ExpressionQuantileProbUpperRewardBound(RewardWrapper theModel,
			BitSet theZeroStateRewardStatesWithZeroRewardTransition, BitSet theZeroStateRewardStatesWithMixedTransitionRewards, BitSet theInvariantStates, BitSet theGoalStates, BitSet theZeroProbabilityStates, double[] theExtremalProbabilities,
			BitSet theStatesOfInterest, QuantileUtilities theQuantileUtilities, ExpressionQuantileProbNormalForm theExpressionQuantile)
	{
		super(theModel,
				theZeroStateRewardStatesWithZeroRewardTransition, theZeroStateRewardStatesWithMixedTransitionRewards, theInvariantStates, theGoalStates, theZeroProbabilityStates, theExtremalProbabilities,
				theStatesOfInterest, theQuantileUtilities, theExpressionQuantile);
		isExistential = theExpressionQuantile.isExistential();
		positiveRewardStates.removeAll(goalStates);
	}

	private final boolean isExistential;

	@Override
	public boolean pickMaximum()
	{
		return isExistential;
	}

	@Override
	public Map<Double, BitSet> determineFiniteQuantileStates() throws PrismException
	{
		PrecomputationHelper.logPrecomputedValues("P" + (pickMaximum() ? "max" : "min") + "(a U b)", statesOfInterest, extremalProbabilities, quantileUtilities.getLog());
		final Map<Double, BitSet> result = PrecomputationHelper.finiteQuantileStatesMap(getRelationOperator(), extremalProbabilities, quantileUtilities.getThresholds());
		//not longer needed => clear memory
		extremalProbabilities = null;
		return result;
	}

	@Override
	public void calculateDerivableStates(CalculatedValues values, int rewardStep)
	{
		//states labeled with goal  ==>  1
		values.setCurrentValues(goalStates, 1);
		//states having zero probability  ==>  0
		values.setCurrentValues(zeroValueStates, 0);
		//states having a positive reward
		QuantitativeCalculationHelper.calculatePositiveRewardStates4UpperRewardBoundedQuantile(positiveRewardStates, model, null, values, rewardStep, pickMaximum(), quantileUtilities.getWorkerPoolForPositiveRewardStates());
	}

	@Override
	public double calculatePositiveRewardTransitionForZeroRewardState(final int state, final int choice, final CalculatedValues values, final int rewardStep)
	{
		return QuantitativeCalculationHelper.valueForPositiveRewardTransition4UpperRewardBoundedQuantile(model, 0, values, state, choice, 0, rewardStep);
	}

	@Override
	public int getNumberOfDerivableStates()
	{
		return goalStates.size() + zeroValueStates.size() + positiveRewardStates.size();
	}

	@Override
	public double[] calculateQualitativeQuantile(int threshold)
	{
		if (model.hasTransitionRewards()) {
			assert (model.getModelType() == ModelType.MDP) : "Only MDPs can fulfill the preconditions to enter this code section.";
			Pair<MDPRestricted, MDPRewards> transitionRewardConstruction = QualitativeCalculationHelper.transitionRewards2stateRewards((RewardWrapperMDP) model);
			MDPRestricted transformedModel = transitionRewardConstruction.getFirst();
			if (quantileUtilities.getDebugLevel() > 0) {
				quantileUtilities.getLog().println("\ntransformed model because of transition rewards:");
				quantileUtilities.getLog().println(transformedModel.infoString() + "\n");
			}
			BitSet A = BitSetTools.asBitSet(invariantStates);
			BitSet B = BitSetTools.asBitSet(goalStates);
			int offset = model.getNumStates();
			for (int state = offset, transformedStates = transformedModel.getNumStates(); state < transformedStates; state++) {
				int originalState = transformedModel.mapStateToOriginalModel(state) % offset;
				if (A.get(originalState))
					A.set(state);
				if (B.get(originalState))
					B.set(state);
			}
			double[] result = QualitativeCalculationHelper.calculateQualitativeQuantileUpperRewardBound(new RewardWrapperMDP(model, new MDPSparse(transformedModel), transitionRewardConstruction.getSecond()),
					threshold, A, B, pickMinimum(), quantileUtilities.getProbModelChecker());
			double[] resultForOriginalModel = new double[offset];
			System.arraycopy(result, 0, resultForOriginalModel, 0, offset);
			return resultForOriginalModel;
		}
		return QualitativeCalculationHelper.calculateQualitativeQuantileUpperRewardBound(model, threshold,
				BitSetTools.asBitSet(invariantStates), BitSetTools.asBitSet(goalStates),
				pickMinimum(), quantileUtilities.getProbModelChecker());
	}
}