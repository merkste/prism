package quantile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import common.StopWatch;
import jdd.Clearable;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import jdd.TemporaryJDDRefs;
import parser.ast.Expression;
import parser.ast.ExpressionQuantileProbNormalForm;
import parser.ast.ExpressionTemporal;
import parser.ast.RelOp;
import parser.ast.TemporalOperatorBound;
import prism.Model;
import prism.ModelType;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.NondetModelTransformation;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.StateModelChecker;
import prism.StateValues;
import prism.StateValuesMTBDD;

public class QuantileCalculator extends PrismComponent implements Clearable {
	private QuantileCalculatorContext qcc;
	private ReachabilityQuantile q;

	public QuantileCalculator(PrismComponent parent, StateModelChecker mc, Model model, JDDNode transRewards, JDDNode goalStates, JDDNode remainStates) throws PrismException
	{
		super(parent);
		qcc = new QuantileCalculatorContext(this, model, mc, transRewards, goalStates, remainStates);
	}
	
	private void setReachabilityQuantile(ReachabilityQuantile q) {
		this.q = q;
	}

	public void clear()
	{
		qcc.clear();
	}

	private JDDNode getStatesFromTransition(JDDNode tr) {
		JDDNode tr01_r = JDD.GreaterThan(tr, 0.0);
		if (qcc.getModel().getModelType() == ModelType.MDP) {
			tr01_r = JDD.ThereExists(tr01_r, ((NondetModel)qcc.getModel()).getAllDDNondetVars());
		}
		JDDNode states_with_tr_r = JDD.ThereExists(tr01_r, qcc.getModel().getAllDDColVars());
		states_with_tr_r = JDD.And(states_with_tr_r, qcc.getModel().getReach().copy());

		return states_with_tr_r;
	}

	/**
	 * <br>[DEREFS: tr, REFS: <i>result</i>]
	 */
	private JDDNode thereExistsChoiceStates(JDDNode tr, NondetModel ndModel) {
		JDDNode tr01 = JDD.GreaterThan(tr, 0.0);
		JDDNode states_with_tr = JDD.ThereExists(tr01, ndModel.getAllDDNondetVars());
		states_with_tr = JDD.ThereExists(states_with_tr, ndModel.getAllDDColVars());
		states_with_tr = JDD.And(states_with_tr, ndModel.getReach().copy());
		
		return states_with_tr;
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
			JDDNode zeroStates = q.getZeroStates(i);
			result = JDD.Apply(JDD.TIMES, result, JDD.Not(zeroStates.copy()));
			statesWithValue = JDD.Or(statesWithValue, zeroStates);
		} else {
			// max
			JDDNode oneStates = q.getOneStates(i);
			result = JDD.Apply(JDD.MAX, result, oneStates.copy());
			statesWithValue = JDD.Or(statesWithValue, oneStates);
		}
		
		for (Entry<Integer, JDDNode> entry : qcc.getTransitionsWithReward()) {
			int r = entry.getKey();
			
			if (r == 0) continue;
			
			JDDNode tr_r = entry.getValue().copy();
			
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
			JDDNode oneStates = q.getOneStates(i);
			result = JDD.Apply(JDD.MAX, result, oneStates.copy());
			statesWithValue = JDD.Or(statesWithValue, oneStates);
		} else {
			JDDNode zeroStates = q.getZeroStates(i);
			result = JDD.Apply(JDD.TIMES, result, JDD.Not(zeroStates.copy()));
			statesWithValue = JDD.Or(statesWithValue, zeroStates);
		}

		// set unknown-value-states to zero
		result = JDD.Apply(JDD.TIMES, result, statesWithValue.copy());

		if (qcc.debugLevel() >= 1) {
			StateValuesMTBDD.print(getLog(), statesWithValue, model, "States with value after positive reward actions");
			StateValuesMTBDD.print(getLog(), result, model, "x_"+i+" after positive reward actions");
		}

		timer.stop();

		// handle potential zero reward steps
		timer.start("handling of zero reward states / transitions");
		result = stepZeroReward(result, statesWithValue, i, min);
		timer.stop();

		return result;
	}

	/** [REFS: <i>result<i>, DEREFS: xTau, tauStates]*/
	public JDDNode stepZeroReward(final JDDNode xTau, final JDDNode tauStates, final int i, boolean min) throws PrismException {
		JDDNode result;
		final boolean compact = true;
		
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
			final NondetModel ndModel = (NondetModel)qcc.getModel();

			class Transformation extends NondetModelTransformation {
				public Transformation(NondetModel model) {
					super(model);
				}

				@Override
				public int getExtraStateVariableCount() {
					return 2;
				}

				@Override
				public int getExtraActionVariableCount() {
					return 1;
				}

				private JDDNode normalState(boolean col) {
					JDDNode v0 = col ? extraColVars.getVar(0) : extraRowVars.getVar(0);
					JDDNode v1 = col ? extraColVars.getVar(1) : extraRowVars.getVar(1);
					
					return JDD.And(JDD.Not(v0.copy()), JDD.Not(v1.copy()));
				}

				public JDDNode goalState(boolean col) {
					JDDNode v0 = col ? extraColVars.getVar(0) : extraRowVars.getVar(0);
					JDDNode v1 = col ? extraColVars.getVar(1) : extraRowVars.getVar(1);
					
					JDDNode result = JDD.And(v0.copy(), v1.copy());
					if (compact) {
						result = JDD.And(result, allZeroOriginalStates(col));
					}
					return result;
				}
				
				private JDDNode failState(boolean col) {
					JDDNode v0 = col ? extraColVars.getVar(0) : extraRowVars.getVar(0);
					JDDNode v1 = col ? extraColVars.getVar(1) : extraRowVars.getVar(1);
					
					JDDNode result = JDD.And(v0.copy(), JDD.Not(v1.copy()));
					if (compact) {
						result = JDD.And(result, allZeroOriginalStates(col));
					}
					return result;
				}

				private JDDNode tau() {
					JDDNode tau = extraActionVars.getVar(0).copy();
					
					if (compact) {
						tau = JDD.And(tau, allZeroOriginalActions());
					}
					return tau;
				}
				
				private JDDNode notTau() {
					JDDNode tau = extraActionVars.getVar(0).copy();
					
					return JDD.Not(tau);
				}
				
				private JDDNode tr0() {
					JDDNode tr0 = qcc.getTransitionsWithReward(0);

					return tr0;
				}

				private JDDNode allZero(JDDVars vars) {
					JDDNode result = JDD.Constant(1.0);
					for (int i = 0; i< vars.getNumVars(); i++) {
						JDD.Ref(vars.getVar(i));
						result = JDD.And(result, JDD.Not(vars.getVar(i)));
					}
					return result;
				}
				
				private JDDNode allZeroOriginalActions() {
					return allZero(ndModel.getAllDDNondetVars());
				}
				
				private JDDNode allZeroOriginalStates(boolean col) {
					if (col) {
						return allZero(ndModel.getAllDDColVars());
					} else {
						return allZero(ndModel.getAllDDRowVars());
					}
				}

				
				@Override
				public JDDNode getTransformedTrans() {
					JDDNode result;

					if (qcc.debugDetailed()) qcc.debugDD(ndModel.getTrans().copy(), "tr");

					if (qcc.debugDetailed()) qcc.debugDD(tr0(), "tr0");
					if (qcc.debugDetailed()) qcc.debugDD(normalState(false), "normalState");
					if (qcc.debugDetailed()) qcc.debugDD(goalState(false), "goalState");
					if (qcc.debugDetailed()) qcc.debugDD(failState(false), "failState");
					if (qcc.debugDetailed()) qcc.debugDD(tau(), "tau");
					if (qcc.debugDetailed()) qcc.debugDD(xTau.copy(), "xTau");
					
					JDDNode zeroStates = q.getZeroStates(i);
					JDDNode oneStates = q.getOneStates(i);
					
					JDDNode maybeStates = ndModel.getReach().copy();
					maybeStates = JDD.And(maybeStates, JDD.Not(zeroStates.copy()));
					maybeStates = JDD.And(maybeStates, JDD.Not(oneStates.copy()));

					JDDNode trans = JDD.Times(normalState(false),
					                          tr0(),
					                          maybeStates.copy(),
					                          normalState(true),
					                          notTau());
					result = trans;
					if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (1)");
					
					trans = JDD.Times(normalState(false),
					                  tau(),
					                  maybeStates.copy(),
					                  tauStates.copy(),
					                  goalState(true),
					                  xTau.copy());
					if (qcc.debugDetailed()) qcc.debugDD(trans.copy(), "trans (2)");
					result = JDD.Apply(JDD.MAX, result, trans);
					if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (2)");

					
					JDDNode oneMinusXTau = JDD.Apply(JDD.MINUS,
					                                 JDD.Constant(1.0),
					                                 xTau.copy());
					if (qcc.debugDetailed()) qcc.debugDD(oneMinusXTau.copy(), "1- xTau");

					trans = JDD.Times(normalState(false),
					                  tau(),
					                  maybeStates.copy(),
					                  tauStates.copy(),
					                  failState(true),
					                  oneMinusXTau);
					if (qcc.debugDetailed()) qcc.debugDD(trans.copy(), "trans (3)");
					result = JDD.Apply(JDD.MAX, result, trans);
					if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (3)");

					
					trans = JDD.Times(normalState(false),
					                  tau(),
					                  zeroStates.copy(),
					                  failState(true));
					if (qcc.debugDetailed()) qcc.debugDD(trans.copy(), "trans (4)");
					result = JDD.Apply(JDD.MAX, result, trans);
					if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (4)");


					trans = JDD.Times(normalState(false),
					                 tau(),
					                 oneStates.copy(),
					                 goalState(true));
					result = JDD.Apply(JDD.MAX, result, trans);
					if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (5)");

					
					trans = JDD.Times(goalState(false),
					                 tau(),
					                 goalState(true));
					result = JDD.Apply(JDD.MAX, result, trans);
					if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (6)");

					
					trans = JDD.Times(failState(false),
					                  tau(),
					                  failState(true));
					result = JDD.Apply(JDD.MAX, result, trans);
					if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (7)");

					
					JDDNode tr0States = thereExistsChoiceStates(tr0(), ndModel);
					JDDNode deadlockStates = JDD.Or(tr0States, tauStates.copy());
					deadlockStates = JDD.And(ndModel.getReach().copy(), JDD.Not(deadlockStates));

					trans = JDD.Times(normalState(false),
					                  deadlockStates,
					                  tau(),
					                  failState(true));
					result = JDD.Apply(JDD.MAX, result, trans);
					if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (8)");

					JDD.Deref(oneStates);
					JDD.Deref(zeroStates);
					JDD.Deref(maybeStates);

					return result;
				}

				@Override
				public JDDNode getTransformedStart() {
					JDDNode reach = ndModel.getReach();
					
					JDD.Ref(reach);
					JDDNode newStart = JDD.Apply(JDD.TIMES, reach, normalState(false));
					return newStart;
				}
				
				public StateValuesMTBDD projectToOriginal(StateValuesMTBDD sv) throws PrismException
				{
					JDDNode filter = normalState(false);
					sv.filter(filter);
					JDD.Deref(filter);
					if (qcc.debugDetailed()) qcc.debugDD(sv.getJDDNode().copy(), "sv after filter");
					
					StateValuesMTBDD svOriginal = sv.sumOverDDVars(extraRowVars, qcc.getModel()).convertToStateValuesMTBDD();
					sv.clear();
					if (qcc.debugDetailed()) qcc.debugDD(svOriginal.getJDDNode().copy(), "sv after sum");
					return svOriginal;
				}
			};

			Transformation transform = new Transformation(ndModel);
			getLog().println("Transform MDP to zero-reward fragment...");
			NondetModel transformed = ndModel.getTransformed(transform);
			getLog().println("\nTransformed MDP:");
			transformed.printTransInfo(getLog());

			JDDNode target = q.getOneStates(i);
			target = JDD.Or(target, transform.goalState(false));
			if (qcc.debugDetailed()) qcc.debugDD(target.copy(), "target");
			
			JDDNode remain = qcc.getRemainStates();

			try {
				if (qcc.debugLevel() > 1) {
					transformed.exportToFile(Prism.EXPORT_DOT, true, new File("transformed-zero-rew-mdp"+i+".dot"));
					StateValuesMTBDD.print(getLog(), target, transformed, "Target states");
				}
			} catch (FileNotFoundException e) {}
			
			if (qcc.debugLevel() > 1) transformed.dump(qcc.getLog());
			
			
			getLog().println("\nDo reachability analysis in transformed MDP...");
			NondetModelChecker mcTransformed = (NondetModelChecker)qcc.getModelChecker().createModelChecker(transformed);

			remain = JDD.And(remain, transformed.getReach().copy());
			target = JDD.And(target, transformed.getReach().copy());

			StateValuesMTBDD sv = 
				mcTransformed.checkProbUntil(remain,
				                             target,
				                             false,  // quantitative
				                             min).convertToStateValuesMTBDD();
			
			JDD.Deref(remain);
			JDD.Deref(target);
			
			getLog().println("Reachability analysis done.");
			if (qcc.debugDetailed()) qcc.debugDD(sv.getJDDNode().copy(), "sv");
			sv = transform.projectToOriginal(sv);
			if (qcc.debugDetailed()) qcc.debugDD(sv.getJDDNode().copy(), "sv");
			
			result = sv.getJDDNode().copy();
			sv.clear();
			
			transformed.clear();
			transform.clear();
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

		List<JDDNode> results = new ArrayList<JDDNode>();
		List<JDDNode> todos = new ArrayList<JDDNode>();
		for (@SuppressWarnings("unused") Double threshold : thresholdsP) {
			results.add(JDD.PLUS_INFINITY.copy());

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

				StateValuesMTBDD sv = new StateValuesMTBDD(results.get(t).copy(), model);

				qcc.getLog().println("\nFYI: Results for threshold "+thresholdsP.get(t)+":");
				sv.printFiltered(qcc.getLog(), statesOfInterest);
				sv.clear();
			}
		}
		infinityStateValues.clear();

		CalculatedProbabilities x = new CalculatedProbabilities();
		
		int iteration = 0;
		int maxIterations = 1000;

		getLog().println("\nStarting iterations...");

		int maxIters = qcc.getSettings().getInteger(PrismSettings.PRISM_MAX_ITERS);
		while (iteration < maxIters && !todoAll.equals(JDD.ZERO)) {
			getLog().println("\nQuantile iteration "+iteration+", there are "
					+ JDD.GetNumMintermsString(todoAll, model.getNumDDRowVars())
					+ " states that remain undetermined.");

			timer.start("quantile iteration "+iteration);

			JDDNode x_i = step(x, iteration);
			x.storeProbabilities(iteration, x_i.copy());
			x.advanceWindow(iteration, qcc.getMaxReward());
			if (qcc.debugLevel() >= 1) {
				StateValuesMTBDD.print(getLog(), x_i, model, "x_"+iteration);
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

				JDDNode result = results.get(t);

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
				results.set(t, result);
				
				if (todo.equals(JDD.ZERO) && printResultsAsTheyHappen) {
					// we newly have calculated the values for this threshold

					StateValuesMTBDD sv = new StateValuesMTBDD(results.get(t).copy(), model);

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
		
		if (iteration == maxIterations && !finished) {
			throw new PrismException("Quantile calculations did not terminate in "+maxIterations+"!");
		} else {
			getLog().println("Quantile calculations finished for all states of interest in "+iteration+" iterations.");
			
		}

		if (results.size()==1) {
			return new StateValuesMTBDD(results.get(0), model);
		} else {
			// multiple threshold case
			
			// return results for last threshold (extra reference)
			StateValuesMTBDD svResult = new StateValuesMTBDD(results.get(results.size()-1).copy(), model);
			
			// print all results
			for (int t = 0; t < results.size(); t++) {
				StateValuesMTBDD sv = new StateValuesMTBDD(results.get(t), model);

				qcc.getLog().printSeparator();
				qcc.getLog().println("\nResults for threshold "+thresholdsP.get(t)+":");
				sv.printFiltered(qcc.getLog(), statesOfInterest);
				sv.clear();
			}
			qcc.getLog().printSeparator();

			return svResult;
		}
	}
	
	
	public StateValues iterationBigStep(JDDNode statesOfInterest, RelOp relOp, List<Double> thresholdsP, int result_adjustment) throws PrismException
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

	public static StateValues checkExpressionQuantile(PrismComponent parent, StateModelChecker mc, Model model, ExpressionQuantileProbNormalForm expr, JDDNode statesOfInterest) throws PrismException {
		// restrict statesOfInterest to the reachable part of the model
		statesOfInterest = JDD.And(statesOfInterest, model.getReach().copy());

		// try-with-resource: activeRefs, hold the currenty active JDDNodes
		// on exception or on leaving the try block, they will be automaticall derefed
		try (TemporaryJDDRefs activeRefs = new TemporaryJDDRefs()) {
			activeRefs.register(statesOfInterest);

			expr = expr.deepCopy();
			expr.findAllBoundQuantileVariables();

			if (model.getModelType() == ModelType.DTMC) {
				throw new PrismException("Symbolic quantiles currently only for MDP.");
			}

			Object rs = expr.getRewardStructIndex();

			JDDNode stateRewards = null;
			JDDNode transRewards = null;

			int i;
			// get reward info
			if (rs == null) {
				// step 
				stateRewards = JDD.Constant(1.0);
				transRewards = JDD.Constant(0.0);
			} else {
				if (model.getNumRewardStructs() == 0) {
					throw new PrismException("Model has no rewards specified");
				} else if (rs instanceof Expression) {
					i = ((Expression) rs).evaluateInt(mc.getConstantValues());
					rs = new Integer(i); // for better error reporting below
					stateRewards = model.getStateRewards(i - 1).copy();
					transRewards = model.getTransRewards(i - 1).copy();
				} else if (rs instanceof String) {
					stateRewards = model.getStateRewards((String) rs).copy();
					transRewards = model.getTransRewards((String) rs).copy();
				} else {
					throw new PrismException("Unknown reward info "+rs.getClass());
				}
			}

			activeRefs.register(stateRewards);
			activeRefs.register(transRewards);

			if (stateRewards == null || transRewards == null) {
				throw new PrismException("Invalid reward structure index \"" + rs + "\"");
			}

			RelOp relP = expr.getProbabilityRelation();
			List<Double> thresholdsP = expr.getProbabilityThresholds(mc.getConstantValues());

			if (thresholdsP.size()==0) {
				throw new PrismException("At least one threshold is needed for quantile: "+expr);
			}

			if (!relP.isStrict()) {
				throw new PrismNotSupportedException("Non-strict probability bounds not yet supported for symbolic quantile");
			}

			ExpressionTemporal pathFormula = Expression.getTemporalOperatorForSimplePathFormula(expr.getInnerFormula().getExpression());
			if (pathFormula.getOperator() != ExpressionTemporal.P_F &&
			    pathFormula.getOperator() != ExpressionTemporal.P_U) {
				throw new PrismNotSupportedException("Only support F and U in quantile");
			}

			TemporalOperatorBound rewardBound = pathFormula.getBounds().getBounds().get(0);
			if (rewardBound.hasLowerBound() && rewardBound.hasUpperBound()) {
				throw new PrismLangException("Can not have upper and lower bound in quantile");
			}

			if (!rewardBound.hasLowerBound() && !rewardBound.hasUpperBound()) {
				// TODO: handle
				throw new PrismLangException("Trivial case: no reward bound");
			}

			boolean rewardBoundLower = rewardBound.hasLowerBound();
			boolean rewardBoundStrict = rewardBoundLower? rewardBound.lowerBoundIsStrict() : rewardBound.upperBoundIsStrict();
			// TODO: complain about strictness

			JDDNode goalStates = mc.checkExpressionDD(pathFormula.getOperand2(), JDD.Constant(1));
			activeRefs.register(goalStates);

			JDDNode remainStates;
			if (pathFormula.getOperator() == ExpressionTemporal.P_U) {
				remainStates = mc.checkExpressionDD(pathFormula.getOperand1(), JDD.Constant(1));
			} else {
				remainStates = JDD.Constant(1.0);
			}
			activeRefs.register(remainStates);

/*		if (!remainStates.equals(JDD.ONE)) {
			throw new PrismException("Currently, restricted reachability is not yet supported.");
		}*/


			// --- Reward normalization
			if (model.getModelType() == ModelType.DTMC) {
				if (!transRewards.equals(JDD.ZERO)) {
					throw new PrismException("Quantiles for DTMC are not supported for transition rewards");
				}
			}

			// TODO: ensure integer rewards...

			// incorporate state rewards to transition rewards
			activeRefs.release(stateRewards);
			activeRefs.release(transRewards);
			transRewards = JDD.Apply(JDD.PLUS, stateRewards, transRewards);
			activeRefs.register(transRewards);

			// --- Calculator generation

			activeRefs.release(transRewards, goalStates, remainStates);
			QuantileCalculator qc = new QuantileCalculator(parent, mc, model, transRewards, goalStates, remainStates);
			activeRefs.register(qc);

			ReachabilityQuantile q;
			boolean universal = expr.isUniversal();
			if (universal) {
				if (rewardBoundLower) {
					q = new ReachabilityLowerRewardBoundUniversal(qc, qc.qcc);
				} else {
					q = new ReachabilityUpperRewardBoundUniversal(qc, qc.qcc);
				}
			} else {
				if (rewardBoundLower) {
					q = new ReachabilityLowerRewardBoundExistential(qc, qc.qcc);
				} else {
					q = new ReachabilityUpperRewardBoundExistential(qc, qc.qcc);
				}
			}
			activeRefs.register(q);
			qc.setReachabilityQuantile(q);

			int result_adjustment = expr.getResultAdjustment(); 

			StateValues result;
			if (parent.getSettings().getBoolean(PrismSettings.QUANTILE_USE_BIGSTEP)) {
				result = qc.iterationBigStep(statesOfInterest, relP, thresholdsP, result_adjustment);
			} else {
				result = qc.iteration(statesOfInterest, relP, thresholdsP, result_adjustment);
			}

			return result;
		}
	}
}