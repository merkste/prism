package explicit.quantile.context.helpers;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import explicit.ECComputer;
import explicit.ECComputerOnTheFly;
import explicit.ECConsumer;
import explicit.MDP;
import explicit.MDPSparse;
import explicit.Model;
import explicit.Product;
import explicit.modelviews.MDPDroppedChoices;
import explicit.quantile.dataStructure.RewardWrapperMDP;
import explicit.rewards.MDPRewards;
import prism.PrismException;

public class MecStates extends EcStates
{
	/**
	 * @see #getZeroValueMecStates
	 */
	private final List<BitSet> zeroValueMecStates = new ArrayList<>();
	/**
	 * states that can not accumulate any value in their maximal end component<br/>
	 * a MEC can only be left by taking actions that are not available in the MEC
	 * @return
	 */
	public List<BitSet> getZeroValueMecStates()
	{
		return zeroValueMecStates;
	}

	public MecStates(final RewardWrapperMDP costModel, final RewardWrapperMDP valueModel, final boolean isExistential) throws PrismException
	{
		calculateValueDivergentStates(costModel, valueModel, isExistential);
		calculateZeroValueMecStates(valueModel);
		calculateInfinityValueStates(costModel, valueModel);
	}

	private void calculateValueDivergentStates(final RewardWrapperMDP costModel, final RewardWrapperMDP valueModel, final boolean isExistential) throws PrismException
	{
		costModel.computeEndComponents(new ECConsumer(null, costModel.getModel())
		{
			@Override
			public void notifyNextMEC(BitSet mec) throws PrismException
			{
				if (EndComponentUtilities.positiveRewardExists(valueModel, mec)){
					valueDivergentStates.or(mec);
					if (isExistential && !EndComponentUtilities.positiveRewardExists(costModel, mec)){
						//if interested in the best scheduler then the strategy will be:
						//just stay in this mec, since it can accumulate arbitrary value at no cost
						infinityValueStates.or(mec);
					}
				}
			}
		});
	}

	private void calculateZeroValueMecStates(final RewardWrapperMDP valueModel) throws PrismException
	{
		//XXX: mit den Methoden aus RewardWrapperMDP ausdruecken
		final MDP zeroValueSubMDP = new MDPSparse(valueModel.dropPositiveRewards());
		ECComputer ecComputer = new ECComputerOnTheFly(valueModel, zeroValueSubMDP, new ECConsumer(valueModel, zeroValueSubMDP){
			@Override
			public void notifyNextMEC(BitSet mec)
			{
				zeroValueMecStates.add(mec);
			}
		});
		ecComputer.computeMECStates();
	}

	private void calculateInfinityValueStates(final RewardWrapperMDP costModel, final RewardWrapperMDP valueModel) throws PrismException
	{
		//XXX: mit den Methoden aus RewardWrapperMDP ausdruecken
		final MDPDroppedChoices dropPositiveRewards = costModel.dropPositiveRewards();
		final MDPSparse zeroCostSubMDP = new MDPSparse(dropPositiveRewards);
		final RewardWrapperMDP valueWrapperForZeroCostSubMDP = new RewardWrapperMDP(costModel, zeroCostSubMDP, new MDPRewards(){
			@Override
			public double getStateReward(final int state)
			{
				return valueModel.getStateReward(state);
			}
			@Override
			public double getTransitionReward(final int state, final int choice)
			{
				final int originalChoice = dropPositiveRewards.mapChoiceToOriginalModel(state, choice);
				return valueModel.getTransitionReward(state, originalChoice);
			}
			@Override
			public MDPRewards liftFromModel(Product<? extends Model> product)
			{
				return null;
			}
			@Override
			public boolean hasTransitionRewards()
			{
				//XXX: das sollte man noch optimieren!!!
				/*if (! valueModel.hasTransitionRewards()){
					return false;
				}*/
				return valueModel.hasTransitionRewards();
			}
		});
		
		ECComputer ecComputer = new ECComputerOnTheFly(costModel, zeroCostSubMDP, new ECConsumer(costModel, zeroCostSubMDP){
			@Override
			public void notifyNextMEC(BitSet mec)
			{
				if (EndComponentUtilities.positiveRewardExists(valueWrapperForZeroCostSubMDP, mec)){
					infinityValueStates.or(mec);
				}
			}
		});
		ecComputer.computeMECStates();
	}
}