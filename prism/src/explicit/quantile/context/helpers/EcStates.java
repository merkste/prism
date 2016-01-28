package explicit.quantile.context.helpers;

import java.util.BitSet;

import explicit.quantile.dataStructure.RewardWrapper;
import explicit.quantile.dataStructure.RewardWrapperDTMC;
import explicit.quantile.dataStructure.RewardWrapperMDP;
import prism.PrismException;

public abstract class EcStates
{
	/**
	 * @see #getValueDivergentStates
	 */
	protected final BitSet valueDivergentStates = new BitSet();
	/**
	 * states that can accumulate value in their end component
	 * @return
	 */
	public BitSet getValueDivergentStates()
	{
		return valueDivergentStates;
	}

	/**
	 * @see #getInfinityValueStates
	 */
	protected final BitSet infinityValueStates = new BitSet();
	/**
	 * states that can accumulate value in their end component<br/>
	 * at the same time there is no cost in this end component<br/>
	 * &rArr; arbitrary values can be accumulated
	 * @return
	 */
	public BitSet getInfinityValueStates()
	{
		return infinityValueStates;
	}

	public static EcStates computeEcStates(final RewardWrapper costModel, final RewardWrapper valueModel, final boolean isExistential) throws PrismException
	{
		if (costModel instanceof RewardWrapperDTMC){
			assert (valueModel instanceof RewardWrapperDTMC);
			return new BsccStates((RewardWrapperDTMC) costModel, (RewardWrapperDTMC) valueModel);
		}
		assert (costModel instanceof RewardWrapperMDP);
		assert (valueModel instanceof RewardWrapperMDP);
		return new MecStates((RewardWrapperMDP) costModel, (RewardWrapperMDP) valueModel, isExistential);
	}
}