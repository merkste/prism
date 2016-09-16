package quantile;

import jdd.JDD;
import jdd.JDDNode;
import prism.Model;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLog;
import prism.StateModelChecker;
import prism.StateValuesMTBDD;
import prism.TransitionsByRewardsInfo;

public class QuantileCalculatorContext extends TransitionsByRewardsInfo {
	private JDDNode goalStates;
	private JDDNode remainStates;
	private StateModelChecker mc;
	private int debugLevel = 0;

	public QuantileCalculatorContext(PrismComponent parent, Model model, StateModelChecker mc, JDDNode stateRewards, JDDNode transRewards, JDDNode goalStates, JDDNode remainStates) throws PrismException
	{
		super(parent, model, stateRewards, transRewards);
		this.mc = mc;
		this.goalStates = goalStates;
		this.remainStates = remainStates;
	}
	
	public void clear()
	{
		super.clear();
		if (goalStates != null) JDD.Deref(goalStates);
		if (remainStates != null) JDD.Deref(remainStates);
	}

	public JDDNode getGoalStates() {
		return goalStates.copy();
	}

	public JDDNode getRemainStates() {
		return remainStates.copy();
	}

	public StateModelChecker getModelChecker() {
		return mc;
	}

	public boolean debugDetailed() {
		return debugLevel >= 2;
	}

	/**
	 *
	 * <br>[DEREFs: dd]
	 */
	public void debugDD(JDDNode dd, String name)
	{
		JDD.PrintMinterms(getLog(), dd, name);
	}
	
	public void debugVector(PrismLog log, JDDNode dd, Model model, String name) throws PrismException
	{
		if (debugLevel >= 2)
			StateValuesMTBDD.print(getLog(), dd.copy(), model, name);
	}


	public int debugLevel()
	{
		return debugLevel;
	}

	public void setDebugLevel(int debugLevel)
	{
		this.debugLevel = debugLevel;
	}
}
