package quantile;

import parser.ast.RelOp;
import prism.PrismException;
import prism.StateValuesMTBDD;
import jdd.Clearable;
import jdd.JDD;
import jdd.JDDNode;

public abstract class ReachabilityQuantile implements Clearable {
	protected QuantileCalculator qc;
	protected QuantileCalculatorContext qcc;
	
	public ReachabilityQuantile(QuantileCalculator qc, QuantileCalculatorContext qcc)
	{
		this.qc = qc;
		this.qcc = qcc;
	}

	public void clear() {}

	public abstract boolean min();

	public abstract JDDNode getZeroStates(boolean forIterationZero);
	public abstract JDDNode getOneStates(boolean forIterationZero);
	public abstract JDDNode getProbabilitiesForBase() throws PrismException;

	public abstract StateValuesMTBDD getInfinityStateValues() throws PrismException;
	
	public JDDNode getInfinityStates(StateValuesMTBDD infinityValues, RelOp relOp, double threshold) throws PrismException {
		JDDNode result = infinityValues.getBDDFromInterval(relOp, threshold);
		result = JDD.Not(result);
		return result;
	}

	
	/**
	 * Calculate the effect of being in level i and having
	 * an action that has reward rew. May be negative.
	 */
	public abstract int doSubstraction(int i, int rew);
}
