package quantile;

import java.util.List;

import common.StopWatch;
import dv.DoubleVector;
import hybrid.PrismHybrid;
import jdd.JDD;
import jdd.JDDNode;
import jdd.SanityJDD;
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

public class QuantileCalculatorHybrid extends QuantileCalculatorSymbolic
{
	public QuantileCalculatorHybrid(PrismComponent parent, StateModelChecker mc, Model model, JDDNode stateRewards, JDDNode transRewards, JDDNode goalStates, JDDNode remainStates)
			throws PrismException
	{
		super(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);
		qcc.setDebugLevel(2);
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

		if (thresholdsP.size() > 1) {
			throw new PrismNotSupportedException("Search for quantile value only supported for a single threshold");
		}

		double threshold = thresholdsP.get(0);
		String thresholdOp = relOp.toString();

		StopWatch timer = new StopWatch(mainLog);
		timer.start("precomputation");
		
		getLog().println("\nDetermine states where the quantile equals infinity (precomputation)");
		StateValuesMTBDD infinityStateValues = q.getInfinityStateValues();

		timer.stop();

		JDDNode todo = statesOfInterest.copy();
		todo = JDD.And(todo, ndModel.getReach().copy());
		qcc.debugVector(getLog(), todo, ndModel, "todo (1)");

		JDDNode infinityStates = q.getInfinityStates(infinityStateValues, relOp, threshold);
		infinityStates = JDD.And(infinityStates, ndModel.getReach().copy());

		JDDNode todoAndInfinityStates = JDD.And(todo.copy(), infinityStates.copy());
		
		getLog().println("States where the quantile equals infinity (threshold "+threshold+"): " 
				+ JDD.GetNumMintermsString(infinityStates, ndModel.getAllDDRowVars().n())
				+ " overall, "
				+ JDD.GetNumMintermsString(todoAndInfinityStates, ndModel.getAllDDRowVars().n())
				+ " of the states of interest.");

		if (qcc.debugDetailed()) qcc.debugDD(infinityStates.copy(), "infinityStates");
		todo = JDD.And(todo, JDD.Not(infinityStates.copy()));
		JDD.Deref(todoAndInfinityStates);
		infinityStateValues.clear();

		if (todo.equals(JDD.ZERO)) {
			JDD.Deref(todo);
			return new StateValuesMTBDD(JDD.ITE(infinityStates, JDD.PLUS_INFINITY.copy(), JDD.Constant(0)), ndModel);
		}

		JDDNode transPositive = qcc.getTransitionsWithPosReward();
		JDDNode transZero = qcc.getTransitionsWithZeroReward();
		JDDNode stateRews = qcc.getStateRewardsOriginal();
		JDDNode transRews = qcc.getTransRewardsOriginal();

		stateRews = JDD.Times(stateRews, ndModel.getReach().copy());
		
		// split transRews into taRews (only depend on alpha) and tsaRews (depends on s and alpha)
		JDDNode tmp = JDD.ITE(ndModel.getNondetMask().copy(), JDD.PLUS_INFINITY.copy(), transRews.copy());
		tmp = JDD.MinAbstract(tmp, ndModel.getAllDDColVars());
		SanityJDD.checkIsDDOverVars(tmp, ndModel.getAllDDNondetVars(), ndModel.getAllDDRowVars());
		JDDNode tmp2 = JDD.MinAbstract(tmp.copy(), ndModel.getAllDDRowVars());
		SanityJDD.checkIsDDOverVars(tmp2, ndModel.getAllDDNondetVars());
		JDD.Deref(tmp);
		
		JDDNode taRews = JDD.ITE(ndModel.getNondetMask().copy(), JDD.Constant(0), tmp2);
		JDDNode tsaRews = JDD.Apply(JDD.MINUS, transRews.copy(), taRews.copy());

		// checks:
		JDDNode tmp3 = JDD.Apply(JDD.PLUS, taRews.copy(), tsaRews.copy());
		SanityJDD.check(tmp3.equals(transRews), "taRews+tsaRews != transRews");
		JDD.Deref(tmp3);

		taRews = JDD.MaxAbstract(taRews, ndModel.getAllDDRowVars());
		taRews = JDD.MaxAbstract(taRews, ndModel.getAllDDColVars());
		tsaRews = JDD.MaxAbstract(tsaRews, ndModel.getAllDDColVars());

		JDDNode base = q.getProbabilitiesForBase();
		JDDNode one = q.getOneStates(false);
		JDDNode zero = q.getZeroStates(false);

		JDDNode sPosRew = qcc.getStatesWithPosRewardTransitions();
		JDDNode sZeroRew = qcc.getStatesWithZeroRewardTransitions();

		JDDNode maxRewForState = qcc.getTransRewards();
		maxRewForState = JDD.MaxAbstract(maxRewForState, ndModel.getAllDDColVars());
		maxRewForState = JDD.MaxAbstract(maxRewForState, ndModel.getAllDDNondetVars());

		qcc.debugVector(getLog(), stateRews, ndModel, "stateRews");
		qcc.debugVector(getLog(), base, ndModel, "base");
		qcc.debugVector(getLog(), zero, ndModel, "zero");
		qcc.debugVector(getLog(), one, ndModel, "one");
		qcc.debugVector(getLog(), infinityStates, ndModel, "infinity");
		qcc.debugVector(getLog(), sPosRew, ndModel, "sPosRew");
		qcc.debugVector(getLog(), sZeroRew, ndModel, "sZeroRew");
		qcc.debugVector(getLog(), maxRewForState, ndModel, "maxRewForState");
		qcc.debugVector(getLog(), todo, ndModel, "todo");

		try {
			DoubleVector soln =
					PrismHybrid.NondetProbQuantile(transPositive,
					                               transZero,
					                               ndModel.getODD(),
					                               ndModel.getAllDDRowVars(),
					                               ndModel.getAllDDColVars(),
					                               ndModel.getAllDDNondetVars(),
					                               stateRews,
					                               taRews,
					                               tsaRews,
					                               base,
					                               one,
					                               zero,
					                               infinityStates,
					                               sPosRew,
					                               sZeroRew,
					                               maxRewForState,
					                               todo,
					                               thresholdOp,
					                               threshold,
					                               q.min(),
					                               q instanceof ReachabilityLowerRewardBound);

			return new StateValuesDV(soln, ndModel);
		} finally {
			JDD.Deref(transPositive);
			JDD.Deref(transZero);
			JDD.Deref(transRews);

			JDD.Deref(taRews);
			JDD.Deref(tsaRews);

			JDD.Deref(stateRews);
			JDD.Deref(base);
			JDD.Deref(zero);
			JDD.Deref(one);
			JDD.Deref(infinityStates);
			JDD.Deref(sPosRew);
			JDD.Deref(sZeroRew);
			JDD.Deref(maxRewForState);
			JDD.Deref(todo);
		}
	}


}
