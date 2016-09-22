package quantile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import common.StopWatch;
import explicit.ExportIterations;
import jdd.JDD;
import jdd.JDDNode;
import jdd.SanityJDD;
import mtbdd.PrismMTBDD;
import parser.ast.RelOp;
import prism.Model;
import prism.ModelType;
import prism.NondetModel;
import prism.NondetModelTransformation;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismNative;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.StateModelChecker;
import prism.StateValues;
import prism.StateValuesDV;
import prism.StateValuesMTBDD;

public class QuantileCalculatorSymbolic extends QuantileCalculatorSymbolicBase
{
	private NondetModel modelZeroRewFragment;

	public QuantileCalculatorSymbolic(PrismComponent parent, StateModelChecker mc, Model model, JDDNode stateRewards, JDDNode transRewards, JDDNode goalStates, JDDNode remainStates)
			throws PrismException
	{
		super(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);

		JDDNode tr_0 = qcc.getTransitionsWithReward(0);
		if (tr_0 != null && !tr_0.equals(JDD.ZERO)) {
			if (model.getModelType() == ModelType.MDP) {
				NondetModel mdp = (NondetModel)model;
				NondetModelTransformation transform =
					new NondetModelTransformation(mdp) {

					@Override
					public int getExtraStateVariableCount()
					{
						return 0;
					}

					@Override
					public int getExtraActionVariableCount()
					{
						return 0;
					}

					@Override
					public JDDNode getTransformedTrans()
					{
						JDDNode trans = qcc.getTransitionsWithReward(0);
						if (qcc.debugDetailed()) qcc.debugDD(trans.copy(), "trans_zero");
						return trans;
					}

					@Override
					public JDDNode getTransformedStart()
					{
						return originalModel.getStart().copy();
					}

					@Override
					public JDDNode getReachableStates() {
						return originalModel.getReach().copy();
					}
					
					@Override
					public boolean deadlocksAreFine() {
						return true;
					}
				};
				modelZeroRewFragment = mdp.getTransformed(transform);
				qcc.getLog().println("Zero reward fragment: ");
				modelZeroRewFragment.printTransInfo(qcc.getLog());

				transform.clear();
			} else {
				throw new PrismNotSupportedException("Model type " + model.getModelType() + " not supported");
			}
		}
		JDD.Deref(tr_0);
	}

	public JDDNode step(CalculatedProbabilities x, int i) throws PrismException {
		Model model = qcc.getModel();
		boolean min = q.min();
		
		StopWatch timer = new StopWatch(mainLog);
		timer.start("handling of positive reward states / transitions");

		getLog().println("Handle positive-reward states/transitions...");

		if (i == 0) {
			JDDNode result = q.getProbabilitiesForBase();
			if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "getProbabilitiesForBase()");
			return result;
		}

		// Tracking the set of states that have a known value (before doing zero-rewards)
		// after the positive-reward actions have been considered:
		//  - zeroStates (always 0)
		//  - oneStates  (always 1)
		//  - states with at least one available outgoing transition with positive reward
		JDDNode statesWithValue = JDD.Constant(0.0);

		JDDNode result = min ? JDD.Constant(1.0) : JDD.Constant(0.0);
		qcc.debugVector(getLog(), result, model, "Result");

		/*
		 * Pre-seeding: for min, set zeroStates, for max, set one states
		 */
		if (min) {
			JDDNode zeroStates = q.getZeroStates(i==0);
			result = JDD.Apply(JDD.TIMES, result, JDD.Not(zeroStates.copy()));
			statesWithValue = JDD.Or(statesWithValue, zeroStates);
		} else {
			// max
			JDDNode oneStates = q.getOneStates(i==0);
			result = JDD.Apply(JDD.MAX, result, oneStates.copy());
			statesWithValue = JDD.Or(statesWithValue, oneStates);
		}
		
		for (Entry<Integer, JDDNode> entry : qcc.getTransitionsWithReward()) {
			int r = entry.getKey();
			
			if (r == 0) continue;
			
			JDDNode tr_r = entry.getValue().copy();
			
			// Compute the target level for this transition by
			// doing the necessary subtraction:
			//  upper bound = i -r
			//  lower bound = max{0, i-r}
			int i_minus_r = q.doSubstraction(i, r);
			if (qcc.debugLevel() >= 2) getLog().println("Iteration "+i+", transitions with reward "+r+" = "+i_minus_r+"...");
			if (i_minus_r < 0) {
				// the transitions with reward r are invalid 
				// for max, we can ignore them
				if (min) {
					// for min, we quantify universally and thus set the value for
					// all states with such outgoing transitions to zero
					JDDNode states_with_tr = getStatesFromTransition(tr_r);
					// set values for those states to 0
					result = JDD.Apply(JDD.TIMES, result, JDD.Not(states_with_tr.copy()));
					statesWithValue = JDD.Or(statesWithValue, states_with_tr);
				} else {
					JDD.Deref(tr_r);
				}
				qcc.debugVector(getLog(), statesWithValue, model, "States with value after "+r+" (negative)");
				qcc.debugVector(getLog(), result, model, "Result after "+r+" (negative)");

				// next
				continue;
			}
			
			JDDNode x_l = x.getProbabilities(i_minus_r);

			JDDNode xPrime_l = JDD.PermuteVariables(x_l, model.getAllDDRowVars(), model.getAllDDColVars());
			JDDNode result_r = JDD.MatrixMultiply(tr_r.copy(), xPrime_l, model.getAllDDColVars(), JDD.BOULDER);

			if (qcc.debugDetailed()) qcc.debugDD(result_r.copy(), "result_"+r);
			
			if (model.getModelType() == ModelType.MDP) {
				NondetModel ndModel = (NondetModel)model;
				
				// (then min or max)
				if (min) {
					// min
					// we are quantifying over all transitions
					// we have to construct a mask of all the choices, that
					// are not relevant here
					JDDNode tr_01_r = JDD.GreaterThan(tr_r.copy(), 0.0);
					if (qcc.debugDetailed()) qcc.debugDD(tr_01_r.copy(), "tr_01_r");
					
					JDDNode nondetMask = JDD.And(JDD.Not(JDD.ThereExists(tr_01_r, model.getAllDDColVars())), model.getReach().copy());
					if (qcc.debugDetailed()) qcc.debugDD(nondetMask.copy(), "nondetMask");
					
  					result_r = JDD.Apply(JDD.MAX, result_r, nondetMask);
  					if (qcc.debugDetailed()) qcc.debugDD(result_r.copy(), "result_"+r+" (2)");
					result_r = JDD.MinAbstract(result_r, ndModel.getAllDDNondetVars());
					if (qcc.debugDetailed()) qcc.debugDD(result_r.copy(), "result_"+r+" (3)");
					result = JDD.Apply(JDD.MIN, result, result_r);
				} else {
					// max
					result_r = JDD.MaxAbstract(result_r, ndModel.getAllDDNondetVars());
					result = JDD.Apply(JDD.MAX, result, result_r);
				}
				
				JDDNode states_with_tr_r = getStatesFromTransition(tr_r.copy());
				statesWithValue = JDD.Or(statesWithValue, states_with_tr_r);
			} else {
				result = JDD.Apply(JDD.MAX, result, result_r);
			}
			
			JDD.Deref(tr_r);

			qcc.debugVector(getLog(), statesWithValue, model, "States with value after "+r);
			qcc.debugVector(getLog(), result, model, "Result after "+r);
		}

		/*
		 * Post-processing: for min, set OneStates, for max, set zero states
		 */
		if (min) {
			JDDNode oneStates = q.getOneStates(i==0);
			result = JDD.Apply(JDD.MAX, result, oneStates.copy());
			statesWithValue = JDD.Or(statesWithValue, oneStates);
		} else {
			JDDNode zeroStates = q.getZeroStates(i==0);
			result = JDD.Apply(JDD.TIMES, result, JDD.Not(zeroStates.copy()));
			statesWithValue = JDD.Or(statesWithValue, zeroStates);
		}

		// set unknown-value-states to zero
		result = JDD.Apply(JDD.TIMES, result, statesWithValue.copy());

		if (qcc.debugLevel() >= 1) {
			StateValuesMTBDD.print(getLog(), statesWithValue.copy(), model, "States with value after positive reward actions");
			StateValuesMTBDD.print(getLog(), result.copy(), model, "x_"+i+" after positive reward actions");
		}

		timer.stop();

		// handle potential zero reward steps
		timer.start("handling of zero reward states / transitions");
		result = stepZeroReward(result, statesWithValue, i, min);

		if (qcc.debugLevel() >= 1) {
			StateValuesMTBDD.print(getLog(), result.copy(), model, "x_"+i+" after zero reward handling");
		}

		timer.stop();

		return result;
	}

	/** [REFS: <i>result<i>, DEREFS: xTau, tauStates]*/
	public JDDNode stepZeroReward(final JDDNode xTau, final JDDNode tauStates, final int i, boolean min) throws PrismException {
		JDDNode result;

		PrismNative.setDefaultExportIterationsFilename("quantile-" + i + ".html");
		
		JDDNode tr_0 = qcc.getTransitionsWithReward(0);
		if (tr_0 == null || tr_0.equals(JDD.ZERO)) {
			// no zero reward transitions -> nothing to do
			
			if (tr_0 != null) JDD.Deref(tr_0);
			JDD.Deref(tauStates);
			
			return xTau;
		}
		JDD.Deref(tr_0);

		getLog().println("Handle zero-reward states/transitions...");

		if (qcc.getModel().getModelType() == ModelType.MDP) {
			JDDNode alternativeValues = xTau.copy();
			if (min) {
				// for min, set reachable states without an alternative to max value, i.e., 1
				alternativeValues = JDD.ITE(tauStates.copy(), alternativeValues, modelZeroRewFragment.getReach().copy());
			}

			JDDNode yes = q.getOneStates(i==0);
			JDDNode maybe = JDD.And(modelZeroRewFragment.getReach().copy(), JDD.Not(q.getZeroStates(i==0)));

			if (SanityJDD.enabled) {
				JDDNode tmp = JDD.GreaterThan(alternativeValues.copy(), 0);
				SanityJDD.checkIsContainedIn(tmp, modelZeroRewFragment.getReach());
				JDD.Deref(tmp);
				SanityJDD.checkIsContainedIn(tauStates, modelZeroRewFragment.getReach());
			}

			result = PrismMTBDD.NondetUntilWithAlternative(modelZeroRewFragment.getTrans(),
			                                               modelZeroRewFragment.getODD(),
			                                               modelZeroRewFragment.getNondetMask(),
			                                               modelZeroRewFragment.getAllDDRowVars(),
			                                               modelZeroRewFragment.getAllDDColVars(),
			                                               modelZeroRewFragment.getAllDDNondetVars(),
			                                               yes,
			                                               maybe,
			                                               tauStates,
			                                               alternativeValues,
			                                               min);
			
			JDD.Deref(alternativeValues, yes, maybe);
		} else {
			// TODO DTMC
			result = null;
		}

		JDD.Deref(xTau);
		JDD.Deref(tauStates);

		return result;
	}

	public StateValues iteration(JDDNode statesOfInterest, RelOp relOp, List<Double> thresholdsP, int result_adjustment) throws PrismException {
		Model model = qcc.getModel();

		StopWatch timer = new StopWatch(mainLog);
		timer.start("precomputation");

		boolean printResultsAsTheyHappen = true;

		TreeMap<Double, JDDNode> results = new TreeMap<Double, JDDNode>();
		List<JDDNode> todos = new ArrayList<JDDNode>();
		for (double threshold : thresholdsP) {
			if (!results.containsKey(threshold)) {
				results.put(threshold, JDD.PLUS_INFINITY.copy());
			}

			JDDNode todo = statesOfInterest.copy();
			todo = JDD.And(todo, model.getReach().copy());
			todos.add(todo);
		}

		getLog().println("\nDetermine states where the quantile equals infinity (precomputation)");
		StateValuesMTBDD infinityStateValues = q.getInfinityStateValues();

		timer.stop();

		JDDNode todoAll = JDD.Constant(0.0);

		for (int t = 0; t < thresholdsP.size(); t++) {
			double threshold = thresholdsP.get(t);
			JDDNode todo = todos.get(t);

			JDDNode infinityStates = q.getInfinityStates(infinityStateValues, relOp, threshold);
			infinityStates = JDD.And(infinityStates, model.getReach().copy());

			JDDNode todoAndInfinityStates = JDD.And(todo.copy(), infinityStates.copy());
		
			getLog().println("States where the quantile equals infinity (threshold "+threshold+"): " 
					+ JDD.GetNumMintermsString(infinityStates, model.getAllDDRowVars().n())
					+ " overall, "
					+ JDD.GetNumMintermsString(todoAndInfinityStates, model.getAllDDRowVars().n())
					+ " of the states of interest.");

			if (qcc.debugDetailed()) qcc.debugDD(infinityStates.copy(), "infinityStates");
			todo = JDD.And(todo, JDD.Not(infinityStates));
			todoAll = JDD.Or(todoAll, todo.copy());
			todos.set(t, todo);
			JDD.Deref(todoAndInfinityStates);

			if (todo.equals(JDD.ZERO) && printResultsAsTheyHappen) {
				// we newly have calculated the values for this threshold

				StateValuesMTBDD sv = new StateValuesMTBDD(results.get(threshold).copy(), model);

				qcc.getLog().println("\nFYI: Results for threshold " + threshold + ":");
				sv.printFiltered(qcc.getLog(), statesOfInterest);
				sv.clear();
			}
		}
		infinityStateValues.clear();

		CalculatedProbabilities x = new CalculatedProbabilities();
		
		int iteration = 0;

		getLog().println("\nStarting iterations...");

		ExportIterations iterationsExport = null;
		if (settings.getBoolean(PrismSettings.PRISM_EXPORT_ITERATIONS)) {
			iterationsExport = new ExportIterations("Quantile (MTBDD)", PrismFileLog.create("quantile.html"));
		}

		while (!todoAll.equals(JDD.ZERO)) {
			getLog().println("\nQuantile iteration "+iteration+", there are "
					+ JDD.GetNumMintermsString(todoAll, model.getNumDDRowVars())
					+ " states that remain undetermined.");

			timer.start("quantile iteration "+iteration);

			JDDNode x_i = step(x, iteration);
			x.storeProbabilities(iteration, x_i.copy());
			x.advanceWindow(iteration, qcc.getMaxReward());
			if (qcc.debugLevel() >= 1) {
				StateValuesMTBDD.print(getLog(), x_i.copy(), model, "x_"+iteration);
			}
			if (iterationsExport != null) {
				StateValuesDV sv = new StateValuesDV(x_i, model);
				iterationsExport.exportVector(sv.getDoubleVector());
				sv.clear();
			}

			// reset todoAll
			JDD.Deref(todoAll);
			todoAll = JDD.Constant(0.0);
			StateValuesMTBDD sv_x_i = new StateValuesMTBDD(x_i, model);
			for (int t = 0; t < thresholdsP.size(); t++) {
				JDDNode todo = todos.get(t);
				if (todo.equals(JDD.ZERO)) {
					continue;  // next;
				}

				double threshold = thresholdsP.get(t);

				JDDNode xThreshold = sv_x_i.getBDDFromInterval(relOp, threshold);
				if (qcc.debugDetailed()) qcc.debugVector(getLog(),  xThreshold, model, "xThreshold");

				if (qcc.debugDetailed()) qcc.debugVector(getLog(),  todo, model, "todo");
				JDDNode xThresholdNew = JDD.And(todo.copy(), xThreshold);
				if (qcc.debugDetailed()) qcc.debugVector(getLog(),  xThresholdNew, model, "xThresholdNew");

				JDDNode result = results.get(threshold);

				// set values for xThresholdNew
				int result_value = iteration + result_adjustment;
				JDDNode newResults = JDD.Times(xThresholdNew.copy(),
				                              JDD.Constant(result_value));
				result = JDD.ITE(xThresholdNew.copy(),
				                 newResults,
				                 result);

				if (qcc.debugDetailed()) qcc.debugVector(getLog(), result, model, "Result after setting xThresholdNew");

				todo = JDD.And(todo, JDD.Not(xThresholdNew));
				if (qcc.debugDetailed()) qcc.debugVector(getLog(), todo, model, "todo after iteration "+iteration);

				todoAll = JDD.Or(todoAll, todo.copy());
				todos.set(t, todo);
				results.put(threshold, result);

				if (todo.equals(JDD.ZERO) && printResultsAsTheyHappen) {
					// we newly have calculated the values for this threshold

					StateValuesMTBDD sv = new StateValuesMTBDD(result.copy(), model);

					qcc.getLog().println("\nFYI: Results for threshold "+thresholdsP.get(t)+":");
					sv.printFiltered(qcc.getLog(), statesOfInterest);
					sv.clear();
				}
			}
			sv_x_i.clear();
			getLog().println();

			timer.stop();

			++iteration;
		}
		
		x.clear();

		boolean finished = todoAll.equals(JDD.ZERO);

		JDD.Deref(todoAll);
		for (JDDNode todo : todos) {
			JDD.Deref(todo);
		}

		if (iterationsExport != null) {
			iterationsExport.close();
		}

		if (!finished) {
			throw new PrismException("Quantile calculations did not terminate!");
		} else {
			getLog().println("Quantile calculations finished for all states of interest in "+iteration+" iterations.");
		}

		if (results.size()==1) {
			return new StateValuesMTBDD(results.firstEntry().getValue(), model);
		} else {
			// multiple threshold case

			// return results for last threshold (extra reference)
			double lastThreshold = thresholdsP.get(thresholdsP.size() - 1);
			StateValuesMTBDD svResult = new StateValuesMTBDD(results.get(lastThreshold).copy(), model);

			// print all results
			for (Entry<Double, JDDNode> entry : results.entrySet()) {
				StateValuesMTBDD sv = new StateValuesMTBDD(entry.getValue(), model);

				qcc.getLog().printSeparator();
				qcc.getLog().println("\nResults for threshold " + entry.getKey() + ":");
				sv.printFiltered(qcc.getLog(), statesOfInterest);
				sv.clear();
			}
			qcc.getLog().printSeparator();

			return svResult;
		}
	}

	@Override
	public void clear()
	{
		if (modelZeroRewFragment != null)
			modelZeroRewFragment.clear();
		super.clear();

		PrismNative.setDefaultExportIterationsFilename("iterations.html");
	}

}
