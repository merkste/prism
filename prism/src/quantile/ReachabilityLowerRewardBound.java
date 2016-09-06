package quantile;

import jdd.JDD;
import jdd.JDDNode;
import prism.Model;
import prism.ModelType;
import prism.NondetModelChecker;
import prism.PrismException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.SCCComputer;
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

	// for DTMC and CTMC
	public StateValuesMTBDD getInfinityStateValuesForMC() throws PrismException {
		ProbModel model = (ProbModel) qcc.getModel();
		ProbModelChecker mc = (ProbModelChecker) qcc.getModelChecker();
		
		// quantile = P[ a U>=? b ] < p
		// infinity <=> P([]a & []<>b & []<>posR) >= p
		//              1 - P(<> (!a | []!b | []!posR) >= p
		//              1 - P(<> D) >= p
	
		JDDNode D = JDD.Not(qcc.getRemainStates()); // !a
		D = JDD.And(D, model.getReach().copy());

		SCCComputer computer = SCCComputer.createSCCComputer(qc, model);
		computer.computeBSCCs();

		JDDNode goalStates = qcc.getGoalStates();
		for (JDDNode bscc : computer.getBSCCs()) {
			boolean includeInD = false;
			if (!JDD.AreIntersecting(bscc, goalStates)) {
				includeInD = true;
			} else {
				// [] !posR?
				if (model.getModelType() == ModelType.CTMC) {
					// time always goes on, so always posR
					includeInD = false;
				}
				JDDNode statesWithPosRew = qcc.getStatesWithPosRewardTransitions();
				if (!JDD.AreIntersecting(bscc, statesWithPosRew)) {
					includeInD = true;
				}
				JDD.Deref(statesWithPosRew);
			}
			if (includeInD) {
				D = JDD.Or(D, bscc);
			} else {
				JDD.Deref(bscc);
			}
		}
		JDD.Deref(computer.getNotInBSCCs());
		JDD.Deref(goalStates);

		// calculate P(<> D)
		qcc.getLog().println("Compute P(<> D)...");
		StateValuesMTBDD svMaxD =mc.checkProbUntil(model.getReach(), // remain = true
		                                           D,
		                                           false // quantitative
		                                          ).convertToStateValuesMTBDD();

		JDD.Deref(D);

		svMaxD.subtractFromOne();
		return svMaxD;
	}
}
