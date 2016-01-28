package explicit.quantile.context.helpers;

import java.util.Arrays;
import java.util.BitSet;
import java.util.TreeSet;

import common.BitSetTools;
import common.functions.Relation;
import common.iterable.IterableBitSet;
import common.iterable.Support;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ProbModelChecker;
import explicit.Product;
import explicit.modelviews.ChoicesToStates;
import explicit.modelviews.MDPRestricted;
import explicit.quantile.dataStructure.Pair;
import explicit.quantile.dataStructure.RewardWrapper;
import explicit.quantile.dataStructure.RewardWrapperDTMC;
import explicit.quantile.dataStructure.RewardWrapperMDP;
import explicit.rewards.MDPRewards;

public abstract class QualitativeCalculationHelper
{
	protected static double[] initialiseQualitativeResults(int numStates, BitSet states)
	{
		//initialise all states with INFINITY ...
		double[] result = new double[numStates];
		Arrays.fill(result, Double.POSITIVE_INFINITY);
		//... and specific states with 0
		for (int state : new IterableBitSet(states))
			result[state] = 0;
		return result;
	}

	public static Pair<MDPRestricted, MDPRewards> transitionRewards2stateRewards(final RewardWrapperMDP transitionRewardsModel)
	{
		assert (transitionRewardsModel.hasTransitionRewards()) : "This code section is only useful when there are transition rewards defined.";

		MDPRestricted choicesToStates = ChoicesToStates.selectedChoicesToStates(transitionRewardsModel.getModel(), (state, choice) -> transitionRewardsModel.getTransitionReward(state, choice) > 0);
		return new Pair<>(choicesToStates, new MDPRewards()
		{
			final int offset = transitionRewardsModel.getNumStates();

			@Override
			public double getStateReward(int state)
			{
				if (state < offset)
					return transitionRewardsModel.getStateReward(state);
				int mappedIndex = choicesToStates.mapStateToOriginalModel(state);
				return transitionRewardsModel.getTransitionReward(mappedIndex % offset, mappedIndex / offset - 1);
			}

			@Override
			public double getTransitionReward(int state, int choice)
			{
				//XXX: return 0; ???
				throw new RuntimeException("It is not allowed to ask for transition rewards here!!!!");
			}

			@Override
			public MDPRewards liftFromModel(Product<? extends Model> product) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean hasTransitionRewards() {
				return false;
			}
		});
	}

	protected static Pair<MDP, MDPRewards> transitionRewards2stateRewardsForAllChoices(final RewardWrapperMDP transitionRewardsModel)
	{
		assert (transitionRewardsModel.hasTransitionRewards()) : "This code section is only useful when there are transition rewards defined.";

		final MDP choicesToStates = ChoicesToStates.choicesToStates(transitionRewardsModel.getModel());
		return new Pair<>(choicesToStates, new MDPRewards()
		{
			final int offset = transitionRewardsModel.getNumStates();

			@Override
			public double getStateReward(int state)
			{
				if (state < offset)
					return transitionRewardsModel.getStateReward(state);
				return transitionRewardsModel.getTransitionReward(state % offset, state / offset - 1);
			}

			@Override
			public double getTransitionReward(int state, int choice)
			{
				//XXX: return 0; ???
				throw new RuntimeException("It is not allowed to ask for transition rewards here!!!!");
			}

			@Override
			public MDPRewards liftFromModel(Product<? extends Model> product) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean hasTransitionRewards() {
				return false;
			}
		});
	}

	protected static BitSet calculateY(double[] result, int threshold, BitSet X, BitSet Z)
	{
		//pick states having smaller result than the actual least reward, ...
		BitSet Y = new Support(result, Relation.LEQ, threshold).asBitSet();
		//... pick just those which have already been discovered ...
		Y.and(X);
		//... and which do not belong to Z
		Y.andNot(Z);
		return Y;
	}

	protected static BitSet getZeroRewardPredecessors(Model model, int threshold, boolean pickMinimum, BitSet remainStates, BitSet targetStates,
			ProbModelChecker probModelChecker)
	{
		switch (model.getModelType()) {
		case DTMC:
			DTMCModelChecker dtmcModelChecker = (DTMCModelChecker) probModelChecker;
			if (threshold == 0)
				return BitSetTools.complement(model.getNumStates(), dtmcModelChecker.prob0((DTMC) model, remainStates, targetStates));
			assert (threshold == 1) : "The threshold for a qualitative quantile is not allowed to be " + threshold + "!!!";
			return dtmcModelChecker.prob1((DTMC) model, remainStates, targetStates);
		case MDP:
			MDPModelChecker mdpModelChecker = (MDPModelChecker) probModelChecker;
			if (threshold == 0)
				return BitSetTools.complement(model.getNumStates(), mdpModelChecker.prob0((MDP) model, remainStates, targetStates, pickMinimum, null));
			assert (threshold == 1) : "The threshold for a qualitative quantile is not allowed to be " + threshold + "!!!";
			return mdpModelChecker.prob1((MDP) model, remainStates, targetStates, pickMinimum, null);
		default:
			throw new RuntimeException("Other model types than DTMC or MDP or not supported!!!");
		}
	}

	protected static BitSet getImmediatePredecessors(RewardWrapper model, int threshold, boolean pickMinimum, final BitSet states)
	{
		// TODO (JK): refactor, move to ModelWrapper
		switch (model.getModelType()) {
		case DTMC:
			if (threshold == 0)
				return ((RewardWrapperDTMC) model).statesWithSomeSuccessors(states);
			assert (threshold == 1) : "The threshold for a qualitative quantile is not allowed to be " + threshold + "!!!";
			return ((RewardWrapperDTMC) model).statesWithAllSuccessors(states);
		case MDP:
			if (threshold == 0) {
				if (pickMinimum)
					return ((RewardWrapperMDP) model).statesWhereAllChoicesHaveSomeSuccessors(states);
				return ((RewardWrapperMDP) model).statesWhereOneChoiceHasSomeSuccessors(states);
			}
			assert (threshold == 1) : "The threshold for a qualitative quantile is not allowed to be " + threshold + "!!!";
			if (pickMinimum)
				return ((RewardWrapperMDP) model).statesWhereAllChoicesHaveAllSuccessors(states);
			return ((RewardWrapperMDP) model).statesWhereOneChoiceHasAllSuccessors(states);
		default:
			throw new RuntimeException("Other model types than DTMC or MDP or not supported!!!");
		}
	}

	public static double[] calculateQualitativeQuantileUpperRewardBound(RewardWrapper model, int threshold, final BitSet A, final BitSet B,
			boolean pickMinimum, ProbModelChecker probModelChecker)
	{
		assert (!model.hasTransitionRewards()) : "transition rewards are not supported here";
		//Z-states used by the algorithm  ==>  (A \setminus B) \intersect zeroStateRew
		BitSet Z = (BitSet) A.clone();
		Z.and(model.getZeroStateRewardStates());
		Z.andNot(B);
		//initialise the results
		double[] result = initialiseQualitativeResults(model.getNumStates(), B);
		//initialise X with the states fulfilling B
		BitSet X = (BitSet) B.clone();
		//the recently seen reward-values
		TreeSet<Integer> R = new TreeSet<Integer>();
		R.add(0);
		while (!R.isEmpty()) {
			//pick the least reward
			//ATTENTION: dont use R.pollFirst() here
			//later we add elements to R, and to avoid duplications we have to keep the least reward inside the set
			int r = R.first();
			BitSet zeroRewardPredecessors = getZeroRewardPredecessors(model.getModel(), threshold, pickMinimum, Z, calculateY(result, r, X, Z),
					probModelChecker);
			BitSet immediatePredecessors = getImmediatePredecessors(model, threshold, pickMinimum, zeroRewardPredecessors);
			immediatePredecessors.and(A);
			//in order to not set values twice, remove states already known
			immediatePredecessors.andNot(X);
			//set the quantile values
			for (int state : new IterableBitSet(immediatePredecessors)) {
				assert (result[state] == Double.POSITIVE_INFINITY) : "state " + state + " already computed earlier!";
				int value = r + model.getStateReward(state);
				result[state] = value;
				R.add(value);
			}
			//update the discovered states
			X.or(immediatePredecessors);
			//the actual least reward has been considered by now
			R.remove(r);
		}
		return result;
	}
}