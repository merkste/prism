package quantile;

import jdd.JDD;
import jdd.JDDNode;
import prism.ModelType;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.PrismException;
import prism.StateValuesMTBDD;

public class ReachabilityLowerRewardBoundUniversal extends ReachabilityLowerRewardBound {

	public ReachabilityLowerRewardBoundUniversal(QuantileCalculator qc, QuantileCalculatorContext qcc) {
		super(qc, qcc);
	}

	@Override
	public boolean min() {
		// max
		return false;
	}

	@Override
	public JDDNode getZeroStates(boolean forIterationZero) {
		return JDD.Constant(0.0);
	}

	@Override
	public JDDNode getOneStates(boolean forIterationZero) {
		return JDD.Constant(0.0);
	}


	@Override
	public StateValuesMTBDD getInfinityStateValues() throws PrismException {
		JDDNode remain = qcc.getRemainStates();
		JDDNode goal = qcc.getGoalStates();
		StateValuesMTBDD result;
		
		if (qcc.getModel().getModelType() == ModelType.MDP) {
			NondetModel ndModel = (NondetModel) qcc.getModel();
			NondetModelChecker ndMC = (NondetModelChecker) qcc.getModelChecker();

			qcc.getLog().println("Compute Pmax(a U b)...");
			StateValuesMTBDD svMaxB = ndMC.checkProbUntil(remain,
			                                              goal,
			                                              false, // quantitative
			                                              min()).convertToStateValuesMTBDD();
			
			if (qcc.debugLevel() >= 1) {
				svMaxB.print(qcc.getLog());
			}
			
			JDDNode maxB = svMaxB.getJDDNode().copy();
			svMaxB.clear();

			qcc.getLog().println("Compute MECs for []a & []<>posR...");
			JDDNode C = PositiveRewardMarkerTransformation.computeMECWithPosR(qc, qcc);
			if (qcc.debugLevel() >= 1) {
				StateValuesMTBDD.print(qcc.getLog(),  C, ndModel, "C");
			}

			// probability for the C states of Pmax(a U b)
			JDDNode probGoal = JDD.Apply(JDD.TIMES, maxB, C.copy());
			if (qcc.debugLevel() >= 1) {
				StateValuesMTBDD.print(qcc.getLog(), probGoal, ndModel, "probGoal for C");
			}

			// compute Pmax(a U goal)
			JDDNode pmax = GoalFailTransformation.computeGoalReachability(qcc, C, probGoal, min());
			StateValuesMTBDD svPmax = new StateValuesMTBDD(pmax, ndModel);
			if (qcc.debugLevel() >= 1) {
				qcc.getLog().println("Pmax(a U goal)");
				svPmax.print(qcc.getLog());
			}

			result = svPmax;
		} else {
			// TODO DTMC
			result = null;
		}

		JDD.Deref(remain);
		JDD.Deref(goal);

		return result;
	}

}
