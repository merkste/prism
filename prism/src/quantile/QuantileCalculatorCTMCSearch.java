package quantile;

import java.util.List;

import common.StopWatch;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionLiteral;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import parser.ast.RelOp;
import parser.ast.TemporalOperatorBound;
import parser.type.TypeDouble;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.StateModelChecker;
import prism.StateValues;
import prism.StateValuesMTBDD;
import prism.StochModel;
import prism.StochModelChecker;

public class QuantileCalculatorCTMCSearch extends QuantileCalculatorSymbolic
{

	private boolean chooseIntervalUpperBound;

	public QuantileCalculatorCTMCSearch(PrismComponent parent, StateModelChecker mc, StochModel model, JDDNode stateRewards, JDDNode transRewards, JDDNode goalStates, JDDNode remainStates, boolean chooseIntervalUpperBound)
			throws PrismException
	{
		super(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);
		this.chooseIntervalUpperBound = chooseIntervalUpperBound;
	}

	public StateValues iteration(JDDNode statesOfInterest, RelOp relOp, List<Double> thresholdsP, int result_adjustment) throws PrismException {
		StochModel model = (StochModel) qcc.getModel();

		StopWatch timer = new StopWatch(mainLog);
		timer.start("precomputation");

		if (!JDD.isSingleton(statesOfInterest, model.getAllDDRowVars())) {
			throw new PrismNotSupportedException("Search for quantile value only supported for a single state of interest");
		}
		JDDNode stateOfInterest = statesOfInterest;
		
		if (thresholdsP.size() > 1) {
			throw new PrismNotSupportedException("Search for quantile value only supported for a single threshold");
		}

		Double threshold = thresholdsP.get(0);

		getLog().println("\n=== Determine states where the quantile equals infinity (precomputation)");
		StateValuesMTBDD infinityStateValues = q.getInfinityStateValues();

		timer.stop();

		JDDNode infinityStates = q.getInfinityStates(infinityStateValues, relOp, threshold);
		infinityStates = JDD.And(infinityStates, model.getReach().copy());

		boolean stateOfInterestIsInfinity = JDD.AreIntersecting(infinityStates, statesOfInterest);
		
		getLog().println("\n=== States where the quantile equals infinity (threshold "+threshold+"): "
				+ JDD.GetNumMintermsString(infinityStates, model.getAllDDRowVars().n())
				+ " overall,"
				+ (stateOfInterestIsInfinity ? " including " : " excluding ")
				+ "the state of interest.");

		if (qcc.debugDetailed()) qcc.debugDD(infinityStates.copy(), "infinityStates");

		if (stateOfInterestIsInfinity) {
			JDDNode result = JDD.ITE(stateOfInterest.copy(), JDD.PLUS_INFINITY.copy(), JDD.Constant(0));
			return new StateValuesMTBDD(result, model);
		}
		infinityStateValues.clear();
		JDD.Deref(infinityStates);

		StochModelChecker pmc = (StochModelChecker) qcc.getModelChecker();

		timer.start("exponential/binary search");

		getLog().println("\n=== Starting exponential search...");

		// TODO: unique labels
		model.addLabelDD("___goal", qcc.getGoalStates());
		model.addLabelDD("___remain", qcc.getRemainStates());

		ExpressionTemporal exprTemp = new ExpressionTemporal(ExpressionTemporal.P_U,
		                                                     new ExpressionLabel("___remain"),
		                                                     new ExpressionLabel("___goal"));

		TemporalOperatorBound bound = new TemporalOperatorBound();
		bound.setBoundType(TemporalOperatorBound.BoundType.TIME_BOUND);
		exprTemp.getBounds().addBound(bound);
		ExpressionProb e = new ExpressionProb(exprTemp, "=", null);

		long iterations = 0;

		// current lower and upper bounds, initially undefined
		double lower = 0.0;
		double upper = 0.0;

		// the bound that will be currently tests
		double testing = 0.0;

		boolean done = false;
		while (!done) {
			if (q instanceof ReachabilityLowerRewardBound) {
				bound.setLowerBound(new ExpressionLiteral(TypeDouble.getInstance(), testing));
			} else {
				bound.setUpperBound(new ExpressionLiteral(TypeDouble.getInstance(), testing));
			}

			iterations++;
			mainLog.println("=== Exponential search, iteration " + iterations + ", testing bound = " + testing + ", " + e);
			StateValuesMTBDD sv = pmc.checkExpression(e, stateOfInterest.copy()).convertToStateValuesMTBDD();
			sv.filter(stateOfInterest);
			mainLog.println("\n=== Result for bound " + testing + ": " + JDD.FindMax(sv.getJDDNode()));

			// check against the P threshold
			JDDNode res = sv.getBDDFromInterval(relOp, threshold);
			if (JDD.AreIntersecting(res, stateOfInterest)) {
				// successful, P threshold is met, found an upper bound
				upper = testing; 
				done = true;
			} else {
				// unsuccessful, P threshold is not (yet) met, update testing bound

				// we know that testing was not successful, so we remember that as the new lower bound
				lower = testing;

				if (testing == 0.0) {
					// jump from 0 (initial iteration) to 1.0
					testing = 1.0;
				} else {
					// double bound
					testing *= 2.0;
				}
			}
			JDD.Deref(res);
			sv.clear();
		}

		getLog().println("\n=== Found upper bound " + upper +" and lower bound " + lower + ", switching to binary search...");

		double precision = settings.getDouble(PrismSettings.QUANTILE_CTMC_PRECISION);

		done = false;
		while (!done) {
			// select mid-point
			testing = (upper + lower) / 2.0;

			if (q instanceof ReachabilityLowerRewardBound) {
				bound.setLowerBound(new ExpressionLiteral(TypeDouble.getInstance(), testing));
			} else {
				bound.setUpperBound(new ExpressionLiteral(TypeDouble.getInstance(), testing));
			}

			iterations++;
			mainLog.println("=== Binary search, iteration " + iterations + ", testing bound = " + testing + ", " + e);
			StateValuesMTBDD sv = pmc.checkExpression(e, stateOfInterest.copy()).convertToStateValuesMTBDD();
			sv.filter(stateOfInterest);
			mainLog.println("\n=== Result for bound " + testing + ": " + JDD.FindMax(sv.getJDDNode()));

			// check against the P threshold
			JDDNode res = sv.getBDDFromInterval(relOp, threshold);
			if (JDD.AreIntersecting(res, stateOfInterest)) {
				// successful, P threshold is met, found an improved upper bound
				upper = testing;
			} else {
				// unsuccessful, P threshold is not met, found an improved lower bound
				lower = testing;
			}
			JDD.Deref(res);
			sv.clear();

			double diff = upper - lower;
			if (diff <= precision) {  // always true as well if lower < upper (might happen due to numerical rounding) 
				done = true;
			}
			if (done) {
				mainLog.println("=== Final bound on the quantile value: ("+lower + "," + upper +"), distance = " + diff);
			} else {
				mainLog.println("=== Updated bound on the quantile value: ("+lower + "," + upper +"), distance = " + diff);
			}
		}

		timer.stop("(" + iterations + " calls to CTMC model checker during search)");

		double quantileResult = 0;
		if (upper == 0.0) {
			if (!chooseIntervalUpperBound)
				quantileResult = Double.NaN;
			else
				quantileResult = 0;
		} else {
			quantileResult = chooseIntervalUpperBound ? upper : lower;
		}

		JDDNode result = JDD.ITE(stateOfInterest.copy(), JDD.Constant(quantileResult), JDD.Constant(0));
		return new StateValuesMTBDD(result, model);
	}

}
