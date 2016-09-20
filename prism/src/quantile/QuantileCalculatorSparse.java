package quantile;

import java.util.List;

import common.StopWatch;
import dv.DoubleVector;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.RelOp;
import prism.Model;
import prism.ModelType;
import prism.NondetModel;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNative;
import prism.PrismNotSupportedException;
import prism.StateModelChecker;
import prism.StateValues;
import prism.StateValuesDV;
import prism.StateValuesMTBDD;
import sparse.PrismSparse;

public class QuantileCalculatorSparse extends QuantileCalculatorSymbolicTACAS16
{
	public QuantileCalculatorSparse(PrismComponent parent, StateModelChecker mc, Model model, JDDNode stateRewards, JDDNode transRewards, JDDNode goalStates, JDDNode remainStates)
			throws PrismException
	{
		super(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);
		// qcc.setDebugLevel(2);
	}

	@Override
	public void clear()
	{
		super.clear();

		PrismNative.setDefaultExportIterationsFilename("iterations.html");
	}

	@Override
	public StateValues computeForBound(int bound) throws PrismException
	{
		throw new PrismNotSupportedException("Currently not supported");
	}


	@Override
	public StateValues iteration(JDDNode statesOfInterest, RelOp relOp, List<Double> thresholdsP, int result_adjustment) throws PrismException
	{
		if (qcc.getModel().getModelType() != ModelType.MDP) {
			throw new PrismNotSupportedException("Only for MDPs");
		}
		NondetModel ndModel = (NondetModel) qcc.getModel();

		double[] thresholdValues = new double[thresholdsP.size()];
		for (int i = 0; i< thresholdsP.size(); i++) {
			thresholdValues[i] = thresholdsP.get(i);
		}
		String thresholdOp = relOp.toString();

		StopWatch timer = new StopWatch(mainLog);
		timer.start("precomputation");
		
		getLog().println("\nDetermine states where the quantile equals infinity (precomputation)");
		StateValuesMTBDD infinityStateValues = q.getInfinityStateValues();

		timer.stop();

		JDDNode transPositive = qcc.getTransitionsWithPosReward();
		JDDNode transZero = qcc.getTransitionsWithZeroReward();
		JDDNode stateRews = qcc.getStateRewardsOriginal();
		JDDNode transRews = qcc.getTransRewardsOriginal();

		stateRews = JDD.Times(stateRews, ndModel.getReach().copy());
		transRews = JDD.Times(transRews, ndModel.getTrans01().copy());

		JDDNode base = q.getProbabilitiesForBase();
		JDDNode one = q.getOneStates(false);
		JDDNode zero = q.getZeroStates(false);

		JDDNode maxRewForState = qcc.getTransRewards();
		maxRewForState = JDD.MaxAbstract(maxRewForState, ndModel.getAllDDColVars());
		maxRewForState = JDD.MaxAbstract(maxRewForState, ndModel.getAllDDNondetVars());

		qcc.debugVector(getLog(), stateRews, ndModel, "stateRews");
		qcc.debugVector(getLog(), base, ndModel, "base");
		qcc.debugVector(getLog(), zero, ndModel, "zero");
		qcc.debugVector(getLog(), one, ndModel, "one");
		qcc.debugVector(getLog(), infinityStateValues.getJDDNode(), ndModel, "infinityStateValues");
		qcc.debugVector(getLog(), maxRewForState, ndModel, "maxRewForState");
		qcc.debugVector(getLog(), statesOfInterest, ndModel, "statesOfInterest");

		if (qcc.debugDetailed()) {
			qcc.debugDD(transPositive.copy(), "transPositive");
			qcc.debugDD(transZero.copy(), "transZero");
		}

		boolean printResultsAsTheyHappen = true;

		try {
			DoubleVector soln =
					PrismSparse.NondetProbQuantile(transPositive,
					                               transZero,
					                               ndModel.getODD(),
					                               ndModel.getAllDDRowVars(),
					                               ndModel.getAllDDColVars(),
					                               ndModel.getAllDDNondetVars(),
					                               stateRews,
					                               transRews,
					                               base,
					                               one,
					                               zero,
					                               infinityStateValues.getJDDNode(),
					                               maxRewForState,
					                               statesOfInterest,
					                               thresholdOp,
					                               thresholdValues,
					                               q.min(),
					                               q instanceof ReachabilityLowerRewardBound,
					                               printResultsAsTheyHappen);

			return new StateValuesDV(soln, ndModel);
		} finally {
			JDD.Deref(transPositive);
			JDD.Deref(transZero);
			JDD.Deref(transRews);
			JDD.Deref(stateRews);
			JDD.Deref(base);
			JDD.Deref(zero);
			JDD.Deref(one);
			JDD.Deref(maxRewForState);

			infinityStateValues.clear();
		}
	}


}
