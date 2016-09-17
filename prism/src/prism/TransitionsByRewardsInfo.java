package prism;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import common.SafeCast;
import jdd.JDD;
import jdd.JDDNode;

public class TransitionsByRewardsInfo extends PrismComponent
{
	protected Model model;
	protected TreeMap<Integer, JDDNode> rewToTrans = new TreeMap<Integer, JDDNode>();
	protected JDDNode stateRewardsOriginal;
	protected JDDNode transRewardsOriginal;
	protected JDDNode transRewards;
	private Integer maxReward = null;

	public TransitionsByRewardsInfo(PrismComponent parent, Model model, JDDNode stateRewards, JDDNode transRewards) throws PrismException
	{
		super(parent);
		this.model = model;
		this.stateRewardsOriginal = stateRewards;
		this.transRewardsOriginal = transRewards;
		this.transRewards = JDD.Apply(JDD.PLUS, stateRewards.copy(), transRewards.copy());

		splitTransitionMatrix(false);
	}

	public Model getModel()
	{
		return model;
	}

	public JDDNode getStateRewardsOriginal()
	{
		return stateRewardsOriginal.copy();
	}

	public JDDNode getTransRewardsOriginal()
	{
		return transRewardsOriginal.copy();
	}

	public JDDNode getTransRewards()
	{
		return transRewards.copy();
	}

	private void putTransitionsWithReward(int rew, JDDNode tr)
	{
		JDDNode old = rewToTrans.put(rew, tr);
		if (old != null) JDD.Deref(old);
	}

	public Set<Integer> getOccuringRewards()
	{
		return rewToTrans.keySet();
	}

	public JDDNode getTransitionsWithReward(int rew)
	{
		JDDNode result = rewToTrans.get(rew);
		if (result != null) {
			result = result.copy();
		}
		return result;
	}

	public JDDNode getTransitionsWithPosReward()
	{
		JDDNode tr01_pos = JDD.GreaterThan(transRewards.copy(), 0.0);
		return JDD.Apply(JDD.TIMES, tr01_pos, model.getTrans().copy());
	}

	public JDDNode getTransitionsWithZeroReward()
	{
		JDDNode tr01_pos = JDD.GreaterThan(transRewards.copy(), 0.0);
		JDDNode tr01_zero = JDD.And(JDD.Not(tr01_pos), model.getTrans01().copy());
		return JDD.Apply(JDD.TIMES, tr01_zero, model.getTrans().copy());
	}

	public JDDNode getStatesWithPosRewardTransitions()
	{
		JDDNode tr01_pos = JDD.GreaterThan(transRewards.copy(), 0.0);
		tr01_pos = JDD.And(tr01_pos, model.getTrans01().copy());
		if (model.getModelType() == ModelType.MDP) {
			tr01_pos = JDD.ThereExists(tr01_pos, ((NondetModel)model).getAllDDNondetVars());
		}
		JDDNode states_with_pos_tr = JDD.ThereExists(tr01_pos, model.getAllDDColVars());
		states_with_pos_tr = JDD.And(states_with_pos_tr, model.getReach().copy());
	
		return states_with_pos_tr;
	
	}

	public JDDNode getStatesWithZeroRewardTransitions()
	{
		JDDNode tr01_pos = JDD.GreaterThan(transRewards.copy(), 0.0);
		JDDNode tr01_zero = JDD.And(JDD.Not(tr01_pos), model.getTrans01().copy());
		if (model.getModelType() == ModelType.MDP) {
			tr01_zero = JDD.ThereExists(tr01_zero, ((NondetModel)model).getAllDDNondetVars());
		}
		JDDNode states_with_zero_tr = JDD.ThereExists(tr01_zero, model.getAllDDColVars());
		states_with_zero_tr = JDD.And(states_with_zero_tr, model.getReach().copy());
	
		return states_with_zero_tr;
	}

	public JDDNode getTransitions01WithPosReward()
	{
		JDDNode trZero = getTransitionsWithReward(0);
		if (trZero == null) trZero = JDD.Constant(0.0);
		JDDNode trZero01 = JDD.GreaterThan(trZero, 0.0);
		
		JDDNode trPos01 = JDD.And(getModel().getTrans01().copy(), JDD.Not(trZero01));
		
		return trPos01;
	}

	public Iterable<Entry<Integer, JDDNode>> getTransitionsWithReward()
	{
		return rewToTrans.entrySet();
	}

	private void setMaxReward(int maxReward)
	{
		this.maxReward = maxReward;
	}

	public int getMaxReward()
	{
		return maxReward;
	}
	
	private void splitTransitionMatrix(boolean debug) throws PrismException
	{
		Model model = getModel();
		JDDNode transRewards = getTransRewards(); 
		
		if (debug) JDD.PrintMinterms(getLog(), model.getTrans().copy(), "tr");
		if (debug) JDD.PrintMinterms(getLog(), transRewards.copy(), "transRewards");
		
		// zero reward

		JDDNode tr01ZeroRew = JDD.Equals(transRewards.copy(), 0.0);
		JDDNode trZeroRew = JDD.Apply(JDD.TIMES,  model.getTrans().copy(),  tr01ZeroRew);
		if (debug) JDD.PrintMinterms(getLog(), trZeroRew.copy(), "trZeroRew");
		putTransitionsWithReward(0, trZeroRew);

		int maxReward = 0;
		
		while (!transRewards.equals(JDD.ZERO)) {
			// find maximal occurring reward
			double rew = JDD.FindMax(transRewards);
			int rewInt = SafeCast.toInt(rew);

			// track maximal reward
			if (rewInt > maxReward) maxReward = rewInt;

			// get set of transitions with this reward
			JDDNode tr01WithRew = JDD.Equals(transRewards.copy(), rew);
			JDDNode trWithRew = JDD.Apply(JDD.TIMES, model.getTrans().copy(), tr01WithRew.copy());
			JDDNode remaining = JDD.Not(tr01WithRew);
			
			if (debug) JDD.PrintMinterms(getLog(), trWithRew.copy(), "trWithRew_"+rewInt);
			putTransitionsWithReward(rewInt, trWithRew);

			// set tRew to 0 for the transitions in tr01WithRew
			transRewards = JDD.Apply(JDD.TIMES, transRewards, remaining);
		}
		JDD.Deref(transRewards);
		
		setMaxReward(maxReward);
	}

	public void clear()
	{
		if (stateRewardsOriginal != null) JDD.Deref(stateRewardsOriginal);
		if (transRewardsOriginal != null) JDD.Deref(transRewardsOriginal);
		if (transRewards != null) JDD.Deref(transRewards);

		for (JDDNode tr : rewToTrans.values()) {
			JDD.Deref(tr);
		}
	}
}