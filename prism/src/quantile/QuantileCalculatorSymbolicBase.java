package quantile;

import common.StopWatch;
import jdd.JDD;
import jdd.JDDNode;
import prism.Model;
import prism.PrismComponent;
import prism.PrismException;
import prism.StateModelChecker;
import prism.StateValues;
import prism.StateValuesMTBDD;

public abstract class QuantileCalculatorSymbolicBase extends QuantileCalculator {

	
	public QuantileCalculatorSymbolicBase(PrismComponent parent, StateModelChecker mc, Model model, JDDNode stateRewards, JDDNode transRewards, JDDNode goalStates, JDDNode remainStates) throws PrismException
	{
		super(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);
	}

	public abstract JDDNode step(CalculatedProbabilities x, int i) throws PrismException;

	@Override
	public StateValues computeForBound(int bound) throws PrismException {
		Model model = qcc.getModel();

		StopWatch timer = new StopWatch(mainLog);

		CalculatedProbabilities x = new CalculatedProbabilities();

		int iteration = 0;

		getLog().println("\nStarting iterations...");

		for (iteration = 0; iteration <= bound; iteration++) {
			getLog().println("\nComputing x_s,i for i = " + iteration);

			timer.start("iteration "+iteration);

			JDDNode x_i = step(x, iteration);
			x.storeProbabilities(iteration, x_i.copy());
			x.advanceWindow(iteration, qcc.getMaxReward());
			if (qcc.debugLevel() >= 1) {
				StateValuesMTBDD.print(getLog(), x_i.copy(), model, "x_"+iteration);
			}
			JDD.Deref(x_i);
			timer.stop();
		}

		JDDNode result = x.getProbabilities(bound);
		x.clear();

		return new StateValuesMTBDD(result, model);
	}

}
