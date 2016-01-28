package explicit.quantile.context.helpers;

import java.util.BitSet;

import explicit.SCCConsumerBSCCs;
import explicit.quantile.dataStructure.RewardWrapperDTMC;
import prism.PrismException;

public class BsccStates extends EcStates
{
	/**
	 * @see #getBsccStates
	 */
	private final BitSet bsccStates = new BitSet();
	/**
	 * any state that is situated inside a BSCC
	 * @return
	 */
	public BitSet getBsccStates()
	{
		return bsccStates;
	}

	/**
	 * @see #getZeroValueBsccStates
	 */
	private final BitSet zeroValueBsccStates = new BitSet();
	/**
	 * states that can not accumulate any value in their bottom strongly connected component<br/>
	 * a BSCC can not be left &rArr; absolutely no value-accumulation is possible for those states
	 * @return
	 */
	public BitSet getZeroValueBsccStates()
	{
		return zeroValueBsccStates;
	}

	public BsccStates(final RewardWrapperDTMC costModel, final RewardWrapperDTMC valueModel) throws PrismException
	{
		costModel.computeStronglyConnectedComponents(new SCCConsumerBSCCs(costModel, costModel.getModel())
		{
			@Override
			public void notifyNextBSCC(final BitSet bscc)
			{
				bsccStates.or(bscc);
				if (EndComponentUtilities.positiveRewardExists(valueModel, bscc)){
					valueDivergentStates.or(bscc);
					if (!EndComponentUtilities.positiveRewardExists(costModel, bscc)){
						infinityValueStates.or(bscc);
					}
				} else {
					zeroValueBsccStates.or(bscc);
				}
			}
		});
	}
}