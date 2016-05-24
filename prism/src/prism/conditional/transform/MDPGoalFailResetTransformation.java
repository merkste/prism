package prism.conditional.transform;

import common.StopWatch;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.NondetModel;
import prism.NondetModelTransformation;
import prism.PrismException;
import prism.PrismLog;

/**
 * A NondetModelTransformation that redirects some states to new goal and fail states
 * and additionally adds reset-actions.
 *
 * <br>
 * Two additional, special states are added, goal and fail. The goal state has a single
 * self-loop, the fail state deterministically goes to the {@code resetTarget}.
 * For the goalFailStates, all actions are removed and a single tau action goes to
 * the goal state with probGoal and to the fail state with 1-probGoal.
 * <br>
 * Additionally, each resetState gets an additional tau action that leads
 * to the fail state, i.e., providing a choice to use the reset mechanism.
 * <br>
 * If states occur in both the goalFailStates and the resetStates, they will
 * not get the additional reset action, i.e., effectively
 * resetStates = resetStates minus goalFailStates.
 */
public class MDPGoalFailResetTransformation extends NondetModelTransformation {
	/** The log */
	private PrismLog log;

	/** The goalFailStates */
	private JDDNode goalFailStates;
	/** For the goalFailStates, the probability to go to goal */
	private JDDNode probGoal;
	/** States where a reset transition will be added */
	private JDDNode resetStates;
	/** The reset target */
	private JDDNode resetTarget;

	/** Flag for "produce BDD for the row variables" */
	private final boolean ROW = true;
	/** Flag for "produce BDD for the column variables" */
	private final boolean COL = false;

	/** The verbosity flag */
	private boolean verbose = false;

	/** Constructor.
	 *
	 * <br> [ STORES: goalFailStates, probGoal, resetStates, resetTarget, derefed on later call to clear() ]
	 * @param model the model to be transformed
	 * @param goalFailStates the states with a single, probabilistic tau action to either goal or fail
	 * @param probGoal for the goalFailStates, the probability of reaching goal
	 * @param resetStates for these states, an additional tau action to fail/reset is added
	 * @param resetTarget the target state in the model for the reset action from the fail state
	 * @param log PrismLog for status / debug output
	 */
	public MDPGoalFailResetTransformation(NondetModel model,
	                                      JDDNode goalFailStates,
	                                      JDDNode probGoal,
	                                      JDDNode resetStates,
	                                      JDDNode resetTarget,
	                                      PrismLog log) throws PrismException
	{
		super(model);
		this.goalFailStates = goalFailStates;
		this.probGoal = probGoal;
		// ensure resetStates = resetStates \ goalFailStates
		this.resetStates = JDD.And(resetStates, JDD.Not(goalFailStates.copy()));
		this.resetTarget = resetTarget;

		if (!JDD.isSingleton(resetTarget, originalModel.getAllDDRowVars())) {
			throw new PrismException("Reset target has to be a single state!");
		}

		this.log = log;
	}

	/** Set the verbosity flag */
	public void setVerbose(boolean value) {
		this.verbose = value;
	}

	@Override
	public void clear()
	{
		// call underlying clear
		super.clear();
		// clear stored JDDNodes
		JDD.Deref(goalFailStates, probGoal, resetStates, resetTarget);
	}

	/**
	 * Construct a mask over the extra vars signifying a normal state.
	 * A set X of states in the original model corresponds to And(X, normal)
	 * in the transformed model.
	 * <br>[ REFS: <i>result</i> ]
	 * @param row construct for row or col vars?
	 */
	public JDDNode normal(boolean row) {
		// !extra(0) & !extra(1)
		return JDD.And(JDD.Not((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy()),
		               JDD.Not((row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy()));
	}

	public JDDNode trap(boolean row) {
		// !normal & !originalVar(0) & !originalVar(1) & ....
		JDDNode result =  JDD.Not(normal(row));

		JDDVars vars = (row ? originalModel.getAllDDRowVars() : originalModel.getAllDDColVars());
		for (int i = 0; i < vars.getNumVars(); i++) {
			result = JDD.And(result, JDD.Not(vars.getVar(i).copy()));
		}
		return result;
	}

	/** Returns a JDDNode for the goal state in the transformed model.
	 * <br>[ REFS: <i>result</i> ]
	 * @param row construct for row or col vars?
	 */
	public JDDNode goal(boolean row) {
		// extra(0) & !extra(1) & !originalVar(0) & !originalVar(1) & ....
		JDDNode result = JDD.And(        (row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy(),
		                         JDD.Not((row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy()));

		return JDD.And(result, trap(row));
//		JDDVars vars = (row ? originalModel.getAllDDRowVars() : originalModel.getAllDDColVars());
//		for (int i = 0; i < vars.getNumVars(); i++) {
//			result = JDD.And(result, JDD.Not(vars.getVar(i).copy()));
//		}
//		return result;
	}

	/** Returns a JDDNode for the fail state in the transformed model.
	 * <br>[ REFS: <i>result</i> ]
	 * @param row construct for row or col vars?
	 */
	public JDDNode fail(boolean row) {
		// !extra(0) & extra(1) & !originalVar(0) & !originalVar(1) & ....
		JDDNode result = JDD.And(JDD.Not((row ? extraRowVars.getVar(0) : extraColVars.getVar(0)).copy()),
		                                 (row ? extraRowVars.getVar(1) : extraColVars.getVar(1)).copy());

		return JDD.And(result, trap(row));
//		JDDVars vars = (row ? originalModel.getAllDDRowVars() : originalModel.getAllDDColVars());
//		for (int i = 0; i < vars.getNumVars(); i++) {
//			result = JDD.And(result, JDD.Not(vars.getVar(i).copy()));
//		}
//		return result;
	}

	/** Returns a JDDNode for the reset target (column variables)
	 * <br>[ REFS: <i>result</i> ]
	 */
	public JDDNode resetTarget() throws PrismException {
		// switch from row -> col
		JDDNode resetCol =
				JDD.PermuteVariables(resetTarget.copy(),
				                     originalModel.getAllDDRowVars(),
				                     originalModel.getAllDDColVars());
		// and ensure that it is a normal state
		return JDD.And(resetCol, normal(COL));
	}

	/** Returns the tau action.
	 * <br>[ REFS: <i>result</i> ]
	 */
	public JDDNode tau() {
		// extraActionVar(0) & !originalActionVar(0) & !originalActionVar(1) & ...
		JDDNode result = extraActionVars.getVar(0).copy();
		for (int i = 0; i < originalModel.getAllDDNondetVars().getNumVars(); i++) {
			result = JDD.And(result, JDD.Not(originalModel.getAllDDNondetVars().getVar(i).copy()));
		}
		return result;
	}

	/** Returns mask for a non-tau action.
	 * An action A in the original model becomes action And(A, nonTau()) in the
	 * transformed model.
	 * <br>[ REFS: <i>result</i> ]
	 */
	public JDDNode notTau() {
		return JDD.Not(extraActionVars.getVar(0).copy());
	}

	@Override
	public int getExtraStateVariableCount()
	{
		// we need 2 extra state variables:
		// 00 = normal
		// 01 = goal
		// 10 = fail
		return 2;
	}

	@Override
	public int getExtraActionVariableCount()
	{
		// we need 1 extra action variable for the tau action
		return 1;
	}

	@Override
	public JDDNode getTransformedTrans() throws PrismException
	{
		JDDNode newTrans;
		StopWatch sw = new StopWatch(log);

		if (verbose) log.println("Goal/fail/reset transformation:");

		if (verbose) sw.start("normal_to_normal");
		JDDNode normal_to_normal =
			JDD.Times(normal(ROW),
			          notTau(),
			          JDD.Not(goalFailStates.copy()),
			          normal(COL),
			          originalModel.getTrans().copy());
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(normal_to_normal));

		if (verbose) sw.start("normal_to_goal");
		JDDNode normal_to_goal =
			JDD.Times(normal(ROW),
			          tau(),
			          goalFailStates.copy(),
			          goal(COL),
			          probGoal.copy());
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(normal_to_goal));

		if (verbose) sw.start("normal_to_fail");
		JDDNode probFail = JDD.Apply(JDD.MINUS, JDD.Constant(1), probGoal.copy());
		JDDNode normal_to_fail =
			JDD.Times(normal(ROW),
			          tau(),
			          goalFailStates.copy(),
			          fail(COL),
			          probFail);
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(normal_to_fail));

		if (verbose) sw.start("bad_to_fail");
		JDDNode bad_to_fail =
			JDD.Times(normal(ROW),
			            resetStates.copy(),
			            tau(),
			            fail(false));
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(bad_to_fail));


		if (verbose) sw.start("goal_self_loop");
		JDDNode goal_self_loop =
			JDD.Times(goal(ROW),
			          tau(),
			          goal(COL));
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(goal_self_loop));

		if (verbose) sw.start("fail_reset");
		JDDNode fail_reset =
			JDD.Times(fail(COL),
			          tau(),
			          resetTarget());
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(fail_reset));

		// plug new transitions together...
		if (verbose) sw.start("goal_self_loop | fail_reset");
		newTrans = JDD.Apply(JDD.MAX, goal_self_loop, fail_reset);
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(newTrans));

		if (verbose) sw.start(" |= bad_to_fail");
		newTrans = JDD.Apply(JDD.MAX, newTrans, bad_to_fail);
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(newTrans));

		if (verbose) sw.start(" |= normal_to_goal");
		newTrans = JDD.Apply(JDD.MAX, newTrans, normal_to_goal);
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(newTrans));

		if (verbose) sw.start(" |= normal_to_fail");
		newTrans = JDD.Apply(JDD.MAX, newTrans, normal_to_fail);
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(newTrans));

		if (verbose) sw.start(" |= normal_to_normal");
		newTrans = JDD.Apply(JDD.MAX, newTrans, normal_to_normal);
		if (verbose) sw.stop("MTBDD nodes = "+JDD.GetNumNodes(newTrans));

		return newTrans;
	}

	/** Print minterms of the various DDs that are used (for debugging). */
	public void debugDDs()
	{
		JDD.PrintMinterms(log, normal(ROW), "normal(row)");
		JDD.PrintMinterms(log, normal(COL), "normal(col)");
		JDD.PrintMinterms(log, goal(ROW), "goal(row)");
		JDD.PrintMinterms(log, fail(ROW), "fail(row");
		JDD.PrintMinterms(log, tau(), "tau()");
		JDD.PrintMinterms(log, notTau(), "notTau()");
		JDD.PrintMinterms(log, getTransformedStart(), "getTransformedStart()");
	}

	@Override
	public JDDNode getTransformedStart()
	{
		// FIXME ALG: check wether this should be resetTarget instead
		JDDNode start = JDD.And(normal(ROW), originalModel.getStart().copy());
		return start;
	}

	/** Returns a copy of the extra state vars introduced by this transformation
	 * <br>[REFS: <i>result</i> ]
	 */
	public JDDVars getExtraStateVars()
	{
		return extraRowVars.copy();
	}
}
