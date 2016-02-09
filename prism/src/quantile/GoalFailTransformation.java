//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package quantile;

import java.io.File;
import java.io.FileNotFoundException;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.NondetModelTransformation;
import prism.Prism;
import prism.PrismException;
import prism.StateValuesMTBDD;

/**
 * GoalFailTransformation:
 * Given a set of states and the probabilities for reaching goal from these states,
 * transform the MDP as follows:
 *  1) remove all outgoing transitions from the states in 'states'
 *  2) add a new tau-transition to each state in 'states'...
 *  3) ... which goes with probGoal to a goal state and with 1-probGoal to a fail state
 *  4) goal and fail states have a single, deterministic tau-self-loop 
 */
public class GoalFailTransformation extends NondetModelTransformation {
	/** The context */
	private QuantileCalculatorContext qcc;
	/** The set of states which are modified */
	private JDDNode states;
	/** The probabilities for reaching goal from states in 'states' */
	private JDDNode probGoal;

	/** Flag for "produce BDD for the row variables" */
	private final static boolean ROW = true;
	/** Flag for "produce BDD for the column variables" */
	private final static boolean COL = false;

	/**
	 * Constructor
	 * <br>[ STORES: states, probGoal; cleared upon call to clear() ]
	 */
	public GoalFailTransformation(QuantileCalculatorContext qcc, JDDNode states, JDDNode probGoal)
	{
		super((NondetModel) qcc.getModel());

		this.qcc = qcc;
		this.states = states;
		this.probGoal = probGoal;
	}

	@Override
	public void clear()
	{
		super.clear();
		if (states != null) JDD.Deref(states);
		if (probGoal != null) JDD.Deref(probGoal);
	}

	@Override
	public int getExtraStateVariableCount()
	{
		// two new state variables:
		// 00 = normal state
		// 11 = goal state
		// 10 = fail state
		return 2;
	}

	@Override
	public int getExtraActionVariableCount()
	{
		// one extra action variable:
		// 1 0.....0 = tau action
		// 0 ....... = original (non-tau) action
		return 1;
	}

	/**
	 * Return mask for normal states
	 * <br>[ REFS: <i>result</i> ]
	 * @param row BDD over row vars?
	 */
	private JDDNode normalState(boolean row)
	{
		JDDNode v0 = row ? extraRowVars.getVar(0) : extraColVars.getVar(0);
		JDDNode v1 = row ? extraRowVars.getVar(1) : extraColVars.getVar(1);

		return JDD.And(JDD.Not(v0.copy()), JDD.Not(v1.copy()));
	}

	/**
	 * Return goal state
	 * <br>[ REFS: <i>result</i> ]
	 * @param row BDD over row vars?
	 */
	public JDDNode goalState(boolean row)
	{
		JDDNode v0 = row ? extraRowVars.getVar(0) : extraColVars.getVar(0);
		JDDNode v1 = row ? extraRowVars.getVar(1) : extraColVars.getVar(1);

		JDDNode result = JDD.And(v0.copy(), v1.copy());
		result = JDD.And(result, allZeroOriginalStates(row));
		return result;
	}

	/**
	 * Return fail state
	 * <br>[ REFS: <i>result</i> ]
	 * @param row BDD over row vars?
	 */
	private JDDNode failState(boolean row)
	{
		JDDNode v0 = row ? extraRowVars.getVar(0) : extraColVars.getVar(0);
		JDDNode v1 = row ? extraRowVars.getVar(1) : extraColVars.getVar(1);

		JDDNode result = JDD.And(v0.copy(), JDD.Not(v1.copy()));
		result = JDD.And(result, allZeroOriginalStates(row));
		return result;
	}

	/**
	 * Return tau action
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode tau()
	{
		JDDNode tau = extraActionVars.getVar(0).copy();

		tau = JDD.And(tau, allZeroOriginalActions());
		return tau;
	}

	/**
	 * Return mask for non-tau actions
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode notTau()
	{
		JDDNode tau = extraActionVars.getVar(0).copy();
		
		return JDD.Not(tau);
	}

	/**
	 * Return a refed copy of the original transition matrix
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode tr()
	{
		JDDNode tr = originalModel.getTrans().copy();
		return tr;
	}

	/**
	 * Return a BDD where all variables in vars are set to 0
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode allZero(JDDVars vars)
	{
		JDDNode result = JDD.Constant(1.0);
		for (int i = 0; i< vars.getNumVars(); i++) {
			JDD.Ref(vars.getVar(i));
			result = JDD.And(result, JDD.Not(vars.getVar(i)));
		}
		return result;
	}

	/**
	 * Return a BDD where all original nondet variables are set to 0
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode allZeroOriginalActions()
	{
		return allZero(originalModel.getAllDDNondetVars());
	}

	/**
	 * Return a BDD where all original state variables are set to 0
	 * <br>[ REFS: <i>result</i> ]
	 * @param row BDD over row vars?
	 */
	private JDDNode allZeroOriginalStates(boolean row)
	{
		if (row) {
			return allZero(originalModel.getAllDDRowVars());
		} else {
			return allZero(originalModel.getAllDDColVars());
		}
	}

	@Override
	public JDDNode getTransformedTrans() {
		JDDNode result;

		// normal & !states & notTau & tr -> normal'
		JDDNode trans = JDD.Times(normalState(ROW),
		                          JDD.Not(states.copy()),
		                          notTau(),
		                          tr(),
		                          normalState(COL));
		result = trans;
		if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (1)");

		// normal & states & tau & probGoal -> goal'
		trans = JDD.Times(normalState(ROW),
		                  states.copy(),
		                  tau(),
		                  probGoal.copy(),
		                  goalState(COL));
		if (qcc.debugDetailed()) qcc.debugDD(trans.copy(), "trans (2)");
		result = JDD.Apply(JDD.MAX, result, trans);
		if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (2)");

		// normal & states & tau & (1-probGoal) -> fail'
		trans = JDD.Times(normalState(ROW),
		                  states.copy(),
		                  tau(),
		                  JDD.Apply(JDD.MINUS,
		                            JDD.Constant(1.0),
		                            probGoal.copy()),
		                  failState(COL));
		if (qcc.debugDetailed()) qcc.debugDD(trans.copy(), "trans (3)");
		result = JDD.Apply(JDD.MAX, result, trans);
		if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (3)");

		// goal -tau-> goal'
		trans = JDD.Times(goalState(ROW),
		                  tau(),
		                  goalState(COL));
		if (qcc.debugDetailed()) qcc.debugDD(trans.copy(), "trans (4)");
		result = JDD.Apply(JDD.MAX, result, trans);
		if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (4)");

		// fail -tau-> fail'
		trans = JDD.Times(failState(ROW),
		                  tau(),
		                  failState(COL));
		if (qcc.debugDetailed()) qcc.debugDD(trans.copy(), "trans (5)");
		result = JDD.Apply(JDD.MAX, result, trans);
		if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (5)");

		return result;
	}

	@Override
	public JDDNode getTransformedStart()
	{
		JDDNode newStart = JDD.And(originalModel.getReach().copy(), normalState(ROW));
		return newStart;
	}

	/** Project StateValues to original state space */
	public JDDNode projectToOriginal(StateValuesMTBDD sv) throws PrismException
	{
		JDDNode filter = normalState(ROW); 
		sv.filter(filter);
		JDD.Deref(filter);
		if (qcc.debugDetailed()) qcc.debugDD(sv.getJDDNode().copy(), "sv after filter");

		StateValuesMTBDD svOriginal = sv.sumOverDDVars(extraRowVars, qcc.getModel()).convertToStateValuesMTBDD();
		sv.clear();
		if (qcc.debugDetailed()) qcc.debugDD(svOriginal.getJDDNode().copy(), "sv after sum");

		JDDNode result = svOriginal.getJDDNode().copy();
		svOriginal.clear();
		return result;
	}

	/**
	 * Compute Pmin/max [ F goal ] for the MDP obtained when transforming using the goal / fail transformation.
	 *
	 * @param qcc quantile calculator context
	 * @param states the states that are modified
	 * @param probGoal the probabilities for going to goal state from 'states'
	 * @param min compute Pmin? 
	 */
	public static JDDNode computeGoalReachability(QuantileCalculatorContext qcc, JDDNode states, JDDNode probGoal, boolean min) throws PrismException
	{
		qcc.getLog().println("Transform MDP for goal reachability...");

		NondetModel model = (NondetModel) qcc.getModel();

		GoalFailTransformation transform = new GoalFailTransformation(qcc, states, probGoal);
		NondetModel transformed = model.getTransformed(transform);
		qcc.getLog().println("\nTransformed MDP:");
		transformed.printTransInfo(qcc.getLog());

		try {
			if (qcc.debugLevel() > 1) {
				transformed.exportToFile(Prism.EXPORT_DOT, true, new File("transformed-mdp-goal.dot"));
			}
		} catch (FileNotFoundException e) {}

		if (qcc.debugLevel() > 1) transformed.dump(qcc.getLog());

		JDDNode remain = qcc.getRemainStates();
		JDDNode goalState = transform.goalState(ROW);

		qcc.getLog().println("\nDo reachability analysis in transformed MDP...");
		NondetModelChecker mcTransformed = (NondetModelChecker)qcc.getModelChecker().createModelChecker(transformed);

		remain = JDD.And(remain, transformed.getReach().copy());
		goalState = JDD.And(goalState, transformed.getReach().copy());

		StateValuesMTBDD sv = 
			mcTransformed.checkProbUntil(remain,
			                             goalState,
			                             false,  // quantitative
			                             min).convertToStateValuesMTBDD();

		JDD.Deref(remain);
		JDD.Deref(goalState);

		qcc.getLog().println("Reachability analysis done.");
		if (qcc.debugDetailed()) qcc.debugDD(sv.getJDDNode().copy(), "sv");
		JDDNode result = transform.projectToOriginal(sv);  // clears sv
		if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result");

		transformed.clear();
		transform.clear();

		return result;
	}
	
}
