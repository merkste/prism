package quantile;

import java.util.List;

import jdd.JDD;
import jdd.JDDNode;
import parser.ast.RelOp;
import prism.Model;
import prism.ModelType;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.PrismComponent;
import prism.PrismException;
import prism.StateModelChecker;
import prism.StateValues;
import prism.StateValuesMTBDD;

public class QuantileCalculatorSymbolicBigStep extends QuantileCalculatorSymbolicTACAS16
{
	
	public QuantileCalculatorSymbolicBigStep(PrismComponent parent, StateModelChecker mc, Model model, JDDNode stateRewards, JDDNode transRewards, JDDNode goalStates,
			JDDNode remainStates) throws PrismException
	{
		super(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);
	}

	public StateValues iteration(JDDNode statesOfInterest, RelOp relOp, List<Double> thresholdsP, int result_adjustment) throws PrismException
	{
		if (qcc.getModel().getModelType() != ModelType.MDP) {
			throw new IllegalArgumentException("Big step iteration only supported for MDP");
		}
		
		if (thresholdsP.size() != 1) {
			throw new PrismException("Big step iteration only supported for single threshold");
		}
		
		double threshold = thresholdsP.get(0);

		NondetModel model = (NondetModel) qcc.getModel();

		JDDNode result = JDD.PLUS_INFINITY.copy();
		JDDNode todo = statesOfInterest.copy();

		
		getLog().println("\nDetermine states where the quantile equals infinity (precomputation)");
		StateValuesMTBDD infinityStateValues = q.getInfinityStateValues();
		JDDNode infinityStates = q.getInfinityStates(infinityStateValues, relOp, threshold);
		infinityStateValues.clear();

		JDDNode todoAndInfinityStates = JDD.And(todo.copy(), infinityStates.copy());

		getLog().println("\nPrecomputation finished, states where the quantile equals infinity: " 
				+ JDD.GetNumMintermsString(infinityStates, model.getAllDDRowVars().n())
				+ " overall, "
				+ JDD.GetNumMintermsString(todoAndInfinityStates, model.getAllDDRowVars().n())
				+ " of the states of interest.");

//		qcc.debugDD(infinityStates.copy(), "infinityStates");
		todo = JDD.And(todo, JDD.Not(infinityStates));
		JDD.Deref(todoAndInfinityStates);

		int bits = 0;
		while (!todo.equals(JDD.ZERO)) {
			bits++;
			int limit = (1 << bits) -1;
			
			getLog().println("\nConsidering counter range [0.."+limit+"], "+
					JDD.GetNumMintermsString(todo, model.getAllDDRowVars().n())+
					" states remain to be determined...");
			
			getLog().println("Building product with counter ("+bits+" bits)...");
			RewardCounterTransformationSubtract trans = new RewardCounterTransformationSubtract(model, qcc, limit, todo.copy());
			NondetModel transformed = model.getTransformed(trans);
			transformed.printTransInfo(mainLog);

			/*
			PrismFileLog stat = new PrismFileLog("dd-stat-"+bits+".csv");
			JDD.statisticsForDD(stat, transformed.getTrans(), transformed.getDDVarNames());
			stat.close();
			*/

			/*
			try {
				qcc.getLog().println("Bits = "+bits+", limit="+limit);
				transformed.exportToFile(Prism.EXPORT_DOT, true, new File("transformed-"+bits+"-bits.dot"));
				transformed.exportStates(Prism.EXPORT_PLAIN, qcc.getLog());
			} catch (FileNotFoundException e) {}
			*/

			JDDNode remain = null;
			JDDNode goal = null;
			if (q instanceof ReachabilityUpperRewardBound) {
				// a U<=r b  ->  (a & !negative) U (b & !negative) 
				remain = JDD.And(qcc.getRemainStates(), JDD.Not(trans.negative(false)));
				goal = JDD.And(qcc.getGoalStates(), JDD.Not(trans.negative(false)));
			} else if (q instanceof ReachabilityLowerRewardBound) {
				// a U>=r b  ->  a U (b & zeroOrNegative)
				remain = qcc.getRemainStates();
				goal = JDD.And(qcc.getGoalStates(), trans.zeroOrNegative(false));
			} else {
				throw new PrismException("Unsupported quantile variant");
			}

			getLog().println("Computing probabilties in product with counter...");
			NondetModelChecker mc = (NondetModelChecker) qcc.getModelChecker().createModelChecker(transformed);

			remain = JDD.And(remain, transformed.getReach().copy());
			goal = JDD.And(goal, transformed.getReach().copy());

			StateValuesMTBDD probs = mc.checkProbUntil(remain,
			                                           goal,
			                                           false, // quantitative
			                                           q.min()).convertToStateValuesMTBDD();

			JDD.Deref(remain);
			JDD.Deref(goal);

			JDDNode xThreshold = probs.getBDDFromInterval(relOp, threshold);
			probs.clear();
			xThreshold = JDD.And(xThreshold, todo.copy());
			xThreshold = JDD.And(xThreshold, JDD.Not(trans.negative(false)));
			//JDD.PrintMinterms(mainLog, model.getTrans().copy(), "original.getTrans()");
			//JDD.PrintMinterms(mainLog, transformed.getTrans().copy(), "transformed.getTrans()");
			//JDD.PrintMinterms(mainLog, xThreshold.copy(), "xThreshold");

			getLog().println("Determine new states passing the threshold...");
			while (true) {
				JDDNode counterValues = JDD.ThereExists(xThreshold.copy(), model.getAllDDRowVars());
				//qcc.debugDD(counterValues.copy(), "counterValues");
				if (counterValues.equals(JDD.ZERO)) {
					JDD.Deref(counterValues);
					break;
				}

				int smallestValue = trans.getSmallestCounter(counterValues.copy());
				JDDNode smallestValueDD = trans.encodeInt(smallestValue, false);
	//			qcc.debugDD(smallestValueDD.copy(), "smallestValueDD");

				JDDNode statesWithValue = JDD.And(xThreshold.copy(), smallestValueDD);
				statesWithValue = JDD.ThereExists(statesWithValue, trans.getExtraRowVars());

				int resultValue = smallestValue + result_adjustment;
				JDDNode values = JDD.ITE(statesWithValue.copy(), JDD.Constant(resultValue), JDD.PlusInfinity());
				result = JDD.Apply(JDD.MIN, result, values);

				todo = JDD.And(todo, JDD.Not(statesWithValue.copy()));
				xThreshold = JDD.And(xThreshold, JDD.Not(statesWithValue));
				
				JDD.Deref(counterValues);
			}
			
			JDD.Deref(xThreshold);
			transformed.clear();
			trans.clear();
		}

		JDD.Deref(todo);
		return new StateValuesMTBDD(result, model);
	}


}
