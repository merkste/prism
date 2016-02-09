package quantile;

import jdd.JDD;
import jdd.JDDNode;
import prism.Model;
import prism.ModelType;
import prism.NondetModelChecker;
import prism.PrismException;
import prism.ProbModelChecker;
import prism.StateValuesMTBDD;

public abstract class ReachabilityUpperRewardBound extends ReachabilityQuantile {

	public ReachabilityUpperRewardBound(QuantileCalculator qc, QuantileCalculatorContext qcc) {
		super(qc, qcc);
	}

	@Override
	public JDDNode getOneStates(int i)
	{
		return qcc.getGoalStates();
	}
	
	@Override
	public JDDNode getZeroStates(int i) {
		return JDD.And(qcc.getModel().getReach().copy(),
	                   JDD.And(JDD.Not(qcc.getGoalStates()),
	                           JDD.Not(qcc.getRemainStates())));
	}

	@Override
	public int doSubstraction(int i, int rew) {
		return i-rew;
	}

	@Override
	public JDDNode getProbabilitiesForBase() throws PrismException {
		// set x = 1 for the one states
		JDDNode one = getOneStates(0);
		JDDNode x = one.copy();
		if (qcc.debugDetailed()) qcc.debugDD(x.copy(), "x");

		JDDNode zero = getZeroStates(0);

		JDDNode statesWithKnownValue = JDD.Or(one, zero);

		// do zero reward step to propagate
		JDDNode result = qc.stepZeroReward(x, statesWithKnownValue, 0, min());
		
		return result;
	}

	@Override
	public StateValuesMTBDD getInfinityStateValues() throws PrismException {
		// precomputation

		Model model = qcc.getModel();
		StateValuesMTBDD sv = null;

		JDDNode remain = qcc.getRemainStates();
		JDDNode goal = qcc.getGoalStates();
		
		if (qcc.getModel().getModelType() == ModelType.DTMC) {
			ProbModelChecker mc = (ProbModelChecker) qcc.getModelChecker();
			qc.getLog().println("Precomputation for upper reward bound: P(a U b)");
			sv = mc.checkProbUntil(remain,
			                       goal,
			                       false // quantitative
			                         ).convertToStateValuesMTBDD();
		} else if (qcc.getModel().getModelType() == ModelType.MDP) {
			NondetModelChecker mc = (NondetModelChecker) qcc.getModelChecker();
			qc.getLog().println("Precomputation for upper reward bound: P"+(min()?"min":"max")+"(a U b)");
			sv = mc.checkProbUntil(remain,
			                       goal,
			                       false,  // quantitative
			                       min()
			                      ).convertToStateValuesMTBDD();
		} else {
			throw new PrismException("Quantile only for DTMC/MDP");
		}

		JDD.Deref(remain);
		JDD.Deref(goal);
		
		return sv;
	}
}
