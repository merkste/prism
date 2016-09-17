package quantile;

import mtbdd.PrismMTBDD;
import prism.ECComputer;
import prism.Model;
import prism.ModelType;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.StateValuesMTBDD;
import jdd.JDD;
import jdd.JDDNode;

public class ReachabilityLowerRewardBoundExistential extends ReachabilityLowerRewardBound {
	JDDNode zeroStates = null;

	public ReachabilityLowerRewardBoundExistential(QuantileCalculator qc, QuantileCalculatorContext qcc) throws PrismException {
		super(qc, qcc);
		
		Model model = qcc.getModel();
		
		JDDNode remain = qcc.getRemainStates();
		JDDNode goal = qcc.getGoalStates();
		
		JDDNode canAvoidGoal = null;
		JDDNode canAvoidPosR = null;
		if (model.getModelType() == ModelType.CTMC) {
			// nothing to do
			canAvoidGoal = JDD.Constant(0);
			canAvoidPosR = JDD.Constant(0);
		} else if (model.getModelType() == ModelType.DTMC) {
			qc.getLog().println("Determine P(a U b)=0 to identify states that can never reach the goal...");
			canAvoidGoal = PrismMTBDD.Prob0(model.getTrans01(),
			                                model.getReach(),
			                                model.getAllDDRowVars(),
			                                model.getAllDDColVars(),
			                                remain,
			                                goal);
			qc.getLog().println("Found "+JDD.GetNumMintermsString(canAvoidGoal, model.getNumDDRowVars())+" states.");

			// for DTMCs at the moment, when transitions rewards are not supported,
			// transition rewards = state rewards 
			JDDNode posRStates = qcc.getTransRewards();
			posRStates = JDD.GreaterThan(posRStates, 0.0);

			qc.getLog().println("Determine P(a U posR)=0 to identify states that will never see positive rewards...");
			canAvoidPosR = PrismMTBDD.Prob0(model.getTrans01(),
			                                model.getReach(),
			                                model.getAllDDRowVars(),
			                                model.getAllDDColVars(),
			                                remain,
			                                posRStates);
			JDD.Deref(posRStates);
			qc.getLog().println("Found "+JDD.GetNumMintermsString(canAvoidGoal, model.getNumDDRowVars())+" states.");
		} else if (model.getModelType() == ModelType.MDP) {
			NondetModel ndModel = (NondetModel) model;
			qc.getLog().println("Determine Pmin(a U b)=0 to identify states that can avoid reaching the goal...");
			canAvoidGoal = PrismMTBDD.Prob0E(ndModel.getTrans01(),
			                                 ndModel.getReach(),
			                                 ndModel.getNondetMask(),
			                                 ndModel.getAllDDRowVars(),
			                                 ndModel.getAllDDColVars(),
			                                 ndModel.getAllDDNondetVars(),
			                                 remain,
			                                 goal);
			qc.getLog().println("Found "+JDD.GetNumMintermsString(canAvoidGoal, model.getNumDDRowVars())+" states.");
			
			qc.getLog().println("Determine Pmin(a U posR)=0 to identify states that can avoid reaching a positive reward...");
			canAvoidPosR = PositiveRewardReachabilityTransformation.computeProb0EReachPosR(qcc);
		} else if (model.getModelType() == ModelType.CTMC) {
			// nothing to do
		} else {
			throw new PrismException("Quantile only for DTMC/MDP");
		}

		JDD.Deref(remain);
		JDD.Deref(goal);
		zeroStates = JDD.Or(canAvoidGoal, canAvoidPosR);
		String operator = (model.getModelType() == ModelType.MDP) ? "Pmin" : "P"; 
		qc.getLog().println(JDD.GetNumMintermsString(zeroStates, model.getNumDDRowVars())+" states satisfy " + operator + "(a U posR)=0 or " + operator + "(a U b)=0.");
	}
	
	public void clear()
	{
		if (zeroStates != null) JDD.Deref(zeroStates);
		zeroStates = null;
	}

	@Override
	public boolean min() {
		// min
		return true;
	}

	@Override
	public JDDNode getZeroStates(boolean forIterationZero) {
		if (!forIterationZero) {
			return zeroStates.copy();
		}
		return JDD.Constant(0.0);
	}
	

	@Override
	public JDDNode getOneStates(boolean forIterationZero) {
		return JDD.Constant(0.0);
	}

	@Override
	public StateValuesMTBDD getInfinityStateValues() throws PrismException {
		if (qcc.getModel().getModelType() == ModelType.DTMC ||
		    qcc.getModel().getModelType() == ModelType.CTMC) {
			return getInfinityStateValuesForMC();
		} else if (qcc.getModel().getModelType() == ModelType.MDP) {
			NondetModel ndModel = (NondetModel) qcc.getModel();
			NondetModelChecker ndMC = (NondetModelChecker) qcc.getModelChecker();
			
			// quantile = P[ a U>=? b ] < p
			// infinity <=> Pmin([]a & []<>b & []<>posR) >= p
			//              1 - Pmax(<> (!a | []!b | []!posR) >= p
			//              1-  Pmax(<> D) >= p
		
			JDDNode D = JDD.Not(qcc.getRemainStates()); // !a
			D = JDD.And(D, ndModel.getReach().copy());
			
			ECComputer computer = ECComputer.createECComputer(qc, ndModel);
			JDDNode notGoal = JDD.Not(qcc.getGoalStates());
			notGoal = JDD.And(notGoal, ndModel.getReach().copy());
			computer.computeMECStates(notGoal);
			for (JDDNode mec : computer.getMECStates()) {
				D = JDD.Or(D, mec); // MEC with []!b
			}
			JDD.Deref(notGoal);
			
			D = JDD.Or(D, PositiveRewardMarkerTransformation.computeMECWithAlwaysNotPosR(qc, qcc));
			
			// calculate Pmax(<> D)
			qcc.getLog().println("Compute Pmax(<> D)...");
			StateValuesMTBDD svMaxD = ndMC.checkProbUntil(ndModel.getReach(), // remain = true
			                                              D,
			                                              false, // quantitative
			                                              false  // max
			                                             ).convertToStateValuesMTBDD();

			JDD.Deref(D);

			svMaxD.subtractFromOne();
			return svMaxD;
		} else {
			throw new PrismNotSupportedException("Can not compute infinite states for " + qcc.getModel().getModelType());
		}
	}
}
