package prism;


import java.util.List;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

public class RewardCounterProduct<M extends Model> extends Product<M>
{
	private int limit;
	private RewardCounterTransformationAdd transform;

	private RewardCounterProduct(M originalModel,
	                             M productModel,
	                             RewardCounterTransformationAdd transform,
	                             JDDNode productStatesOfInterest,
	                             JDDVars automatonRowVars) {
		super(productModel, originalModel, productStatesOfInterest, automatonRowVars);
		this.transform = transform;
		this.limit = transform.getLimit(); 
	}

	@Override
	public void clear() 
	{
		super.clear();
		transform.clear();
	}
	
	/**
	 * Get the states in the product for a given accumulated reward.
	 * If acc_reward is >= limit, then the states with rewards beyond the
	 * limit are returned.
	 */
	public JDDNode getStatesWithAccumulatedReward(int acc_reward) {
		if (acc_reward >= transform.getLimit()) {
			acc_reward = transform.getLimit();
		}
		return JDD.And(productModel.getReach().copy(),
		               transform.encodeInt(acc_reward, false));
	}

	/**
	 * Get the states in the product inside a given integer bound.
	 */
	public JDDNode getStatesWithAccumulatedRewardInBound(IntegerBound bound) {
		JDDNode result = JDD.Constant(0);
		for (int r=0; r<=bound.getMaximalInterestingValue(); r++) {
			if (bound.isInBounds(r)) {
				result = JDD.Or(result, getStatesWithAccumulatedReward(r));
			}
		}
		return result;
	}

	/**
	 * Generate the product of a MDP with an accumulated reward counter.
	 * The counter has the range [0,limit], with saturation semantics for accumulated
	 * rewards >=limit.
	 * @param originalModel the MDP
	 * @param rewards integer MCRewards
	 * @param limit the saturation value for the counter
	 * @param statesOfInterest the set of state of interest, the starting point for the counters
	 * @return
	 * @throws PrismException
	 */
	static public RewardCounterProduct<NondetModel> generate(PrismComponent parent, NondetModel originalModel, JDDNode trRewards, int limit, JDDNode statesOfInterest) throws PrismException {
		TransitionsByRewardsInfo info = new TransitionsByRewardsInfo(parent, originalModel, JDD.Constant(0), trRewards);
		RewardCounterTransformationAdd transform = new RewardCounterTransformationAdd(originalModel, info, limit, statesOfInterest);
	
		NondetModel transformedModel = originalModel.getTransformed(transform);
		JDDNode productStatesOfInterest = transformedModel.getStart().copy();
		return new RewardCounterProduct<NondetModel>(originalModel, transformedModel, transform, productStatesOfInterest, transform.getExtraRowVars().copy());
	}

	/**
	 * Generate the product of a DTMC with an accumulated reward counter.
	 * The counter has the range [0,limit], with saturation semantics for accumulated
	 * rewards >=limit.
	 * @param originalModel the DTMC
	 * @param rewards integer MCRewards
	 * @param limit the saturation value for the counter
	 * @param statesOfInterest the set of state of interest, the starting point for the counters
	 * @return
	 * @throws PrismException
	 */
	static public RewardCounterProduct<ProbModel> generate(PrismComponent parent, ProbModel originalModel, JDDNode trRewards, int limit, JDDNode statesOfInterest) throws PrismException {
		TransitionsByRewardsInfo info = new TransitionsByRewardsInfo(parent, originalModel, JDD.Constant(0), trRewards);
		RewardCounterTransformationAdd transform = new RewardCounterTransformationAdd(originalModel, info, limit, statesOfInterest);
	
		ProbModel transformedModel = originalModel.getTransformed(transform);
		JDDNode productStatesOfInterest = transformedModel.getStart().copy();
		return new RewardCounterProduct<ProbModel>(originalModel, transformedModel, transform, productStatesOfInterest, transform.getExtraRowVars().copy());
	}

	/**
	 * Get the states in the product DTMC inside the conjunction of integer bound.
	 */
	JDDNode getStatesWithAccumulatedRewardInBoundConjunction(List<IntegerBound> bounds) {
		JDDNode result = JDD.Constant(0);
		for (int r=0; r<=limit; r++) {
			//System.out.println("r="+r+" is in bound?");
			if (IntegerBound.isInBoundForConjunction(bounds, r)) {
				//System.out.println("r="+r+" is in bound");
				JDDNode accStates = getStatesWithAccumulatedReward(r);
				// JDD.PrintMinterms(new PrismFileLog("stdout"), accStates.copy(), "accStates");
				result = JDD.Or(result, accStates);
			}
		}
		return result;
	}
}
