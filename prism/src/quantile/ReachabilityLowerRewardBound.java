package quantile;

import jdd.JDD;
import jdd.JDDNode;
import prism.Model;
import prism.ModelType;
import prism.NondetModelChecker;
import prism.PrismException;
import prism.ProbModelChecker;
import prism.StateValuesMTBDD;

public abstract class ReachabilityLowerRewardBound extends ReachabilityQuantile {

	public ReachabilityLowerRewardBound(QuantileCalculator qc,
			QuantileCalculatorContext qcc) {
		super(qc, qcc);
	}

	@Override
	public int doSubstraction(int i, int rew) {
		int result = i - rew;
		if (result < 0) {
			return 0;
		}
		
		return result;
	}

	@Override
	public JDDNode getProbabilitiesForBase() throws PrismException {
		Model model = qcc.getModel();
		
		StateValuesMTBDD sv = null;

		JDDNode remain = qcc.getRemainStates();
		JDDNode goal = qcc.getGoalStates();
		
		if (model.getModelType() == ModelType.DTMC) {
			
			ProbModelChecker mc = (ProbModelChecker) qcc.getModelChecker();
			qc.getLog().println("Calculate P(a U b) for x_0...");
			sv = mc.checkProbUntil(remain,
			                       goal,
			                       false // quantitative
			                      ).convertToStateValuesMTBDD();
		} else if (model.getModelType() == ModelType.MDP) {
			NondetModelChecker mc = (NondetModelChecker) qcc.getModelChecker();
			qc.getLog().println("Calculate P"+(min()?"min":"max")+"(a U b) for x_0...");
			sv = mc.checkProbUntil(remain,
			                       goal,
			                       false, // quantitative
			                       min()
			                      ).convertToStateValuesMTBDD();
		} else {
			throw new PrismException("Quantile only for DTMC/MDP");
		}
		
		JDD.Deref(remain);
		JDD.Deref(goal);
		
		JDDNode result = sv.getJDDNode().copy();
		sv.clear();

		return result;
	}

}
