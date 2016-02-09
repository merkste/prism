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

import mtbdd.PrismMTBDD;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.NondetModel;
import prism.NondetModelTransformation;
import prism.Prism;
import prism.PrismException;
import prism.StateValuesMTBDD;

/** MDP transformation, switches to goalState once a positive reward transition is encountered. */
public class PositiveRewardReachabilityTransformation extends NondetModelTransformation
{
	/** Context */
	private QuantileCalculatorContext qcc;
	/** 0/1 BDD of transitions with positive reward */
	private JDDNode trPos01;
	/** 0/1 BDD of transitions with zero reward */
	private JDDNode trZero01;

	/** Flag for "produce BDD for the row variables" */
	private final static boolean ROW = true;
	/** Flag for "produce BDD for the column variables" */
	private final static boolean COL = false;

	public PositiveRewardReachabilityTransformation(QuantileCalculatorContext qcc)
	{
		super((NondetModel) qcc.getModel());

		this.qcc = qcc;
		JDDNode trZero = qcc.getTransitionsWithReward(0);
		if (trZero == null)
			trZero = JDD.Constant(0.0);
		trZero01 = JDD.GreaterThan(trZero, 0.0);
		if (qcc.debugDetailed()) qcc.debugDD(trZero01.copy(), "trZero01");

		JDDNode tr01 = qcc.getModel().getTrans01().copy();
		trPos01 = JDD.And(tr01, JDD.Not(trZero01.copy()));
		if (qcc.debugDetailed()) qcc.debugDD(trPos01.copy(), "trPos01");
	}

	@Override
	public void clear()
	{
		super.clear();
		if (trPos01 != null) JDD.Deref(trPos01);
		if (trZero01 != null) JDD.Deref(trZero01);
	}

	@Override
	public int getExtraStateVariableCount()
	{
		// one extra state variable
		// 0 .... original state
		// 1 0000 goal state
		return 1;
	}

	@Override
	public int getExtraActionVariableCount()
	{
		// extra action variable
		// 0 .... original, non-tau action
		// 1 0000 tau action
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
		return JDD.Not(v0.copy());
	}

	/**
	 * Return goal state
	 * <br>[ REFS: <i>result</i> ]
	 * @param row BDD over row vars?
	 */
	public JDDNode goalState(boolean row)
	{
		JDDNode v0 = row ? extraRowVars.getVar(0) : extraColVars.getVar(0);

		JDDNode result = JDD.And(v0.copy(), allZeroOriginalStates(row));
		return result;
	}

	/**
	 * Return tau action.
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode tau()
	{
		JDDNode tau = extraActionVars.getVar(0).copy();
		tau = JDD.And(tau, allZeroOriginalActions());
		return tau;
	}

	/**
	 * Return mask for non-tau actions.
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode notTau()
	{
		JDDNode tau = extraActionVars.getVar(0).copy();

		return JDD.Not(tau);
	}

	/**
	 * Return copy of original model transition matrix
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode tr()
	{
		return originalModel.getTrans().copy();
	}

	/**
	 * Return positive reward transitions (0/1)
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode trPos01()
	{
		return trPos01.copy();
	}

	/**
	 * Return zero reward transitions (0/1)
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode trZero01()
	{
		return trZero01.copy();

	}

	/**
	 * Return a BDD where all variables in vars are set to 0
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode allZero(JDDVars vars)
	{
		JDDNode result = JDD.Constant(1.0);
		for (int i = 0; i < vars.getNumVars(); i++) {
			result = JDD.And(result, JDD.Not(vars.getVar(i).copy()));
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
	public JDDNode getTransformedTrans()
	{
		JDDNode result;

		if (qcc.debugDetailed()) qcc.debugDD(normalState(ROW), "normalState");
		if (qcc.debugDetailed()) qcc.debugDD(trPos01(), "trPos01");
		if (qcc.debugDetailed()) qcc.debugDD(tr(), "tr");
		if (qcc.debugDetailed()) qcc.debugDD(goalState(ROW), "goalState");
		if (qcc.debugDetailed()) qcc.debugDD(tau(), "tau");
		if (qcc.debugDetailed()) qcc.debugDD(notTau(), "notTau");

		// normal & exists(trPos01) & tau -> goal'
		JDDNode existsPosTr = JDD.ThereExists(trPos01(), originalModel.getAllDDColVars());
		existsPosTr = JDD.ThereExists(existsPosTr, originalModel.getAllDDNondetVars());
		if (qcc.debugDetailed()) qcc.debugDD(existsPosTr.copy(), "existsPosTr");

		JDDNode trans = JDD.Times(normalState(ROW),
		                          existsPosTr,
		                          tau(),
		                          goalState(COL));
		result = trans;
		if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (1)");

		// normal & notTau & trZero01 -> normal'
		trans = JDD.Times(normalState(ROW),
		                  trZero01(),
		                  tr(),
		                  normalState(COL),
		                  notTau());
		if (qcc.debugDetailed()) qcc.debugDD(trans.copy(), "trans (2)");
		result = JDD.Apply(JDD.MAX, result, trans);
		if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (2)");

		// goal -tau-> goal'
		trans = JDD.Times(goalState(ROW),
		                  tau(),
		                  goalState(COL));
		if (qcc.debugDetailed()) qcc.debugDD(trans.copy(), "trans (3)");
		result = JDD.Apply(JDD.MAX, result, trans);
		if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (3)");

		return result;
	}

	@Override
	public JDDNode getTransformedStart()
	{
		JDDNode newStart = JDD.And(originalModel.getReach().copy(), normalState(false));
		return newStart;
	}

	/** Project StateValues to original model
	 * <br>[ REFS: <i>result</i> ]
	 */
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
	 * Compute the set of states with 'there exists a scheduler with P=0 [ remain U posReward ]'.
	 */
	public static JDDNode computeProb0EReachPosR(QuantileCalculatorContext qcc) throws PrismException
	{
		qcc.getLog().println("Transform MDP for posR reachability...");

		NondetModel model = (NondetModel) qcc.getModel();

		PositiveRewardReachabilityTransformation transform = new PositiveRewardReachabilityTransformation(qcc);
		NondetModel transformed = model.getTransformed(transform);
		qcc.getLog().println("\nTransformed MDP:");
		transformed.printTransInfo(qcc.getLog());

		try {
			if (qcc.debugLevel() > 1) {
				transformed.exportToFile(Prism.EXPORT_DOT, true, new File("transformed-mdp-posR.dot"));
			}
		} catch (FileNotFoundException e) {
		}

		if (qcc.debugLevel() > 1) transformed.dump(qcc.getLog());

		JDDNode remain = qcc.getRemainStates();
		JDDNode goalState = transform.goalState(false);

		qcc.getLog().println("\nCompute states with Pmin(a U posR) = 0 in transformed MDP...");
		JDDNode prob0e = PrismMTBDD.Prob0E(transformed.getTrans01(),
		                                   transformed.getReach(),
		                                   transformed.getNondetMask(),
		                                   transformed.getAllDDRowVars(),
		                                   transformed.getAllDDColVars(),
		                                   transformed.getAllDDNondetVars(),
		                                   remain,
		                                   goalState);
		JDD.Deref(remain);
		JDD.Deref(goalState);

		if (qcc.debugDetailed()) qcc.debugDD(prob0e.copy(), "prob0e (transformed)");
		prob0e = transform.projectToOriginal(new StateValuesMTBDD(prob0e, transformed));

		if (qcc.debugDetailed()) qcc.debugDD(prob0e.copy(), "prob0e (original)");
		qcc.getLog().println("Done, " + JDD.GetNumMintermsString(prob0e, model.getNumDDRowVars()) + " states can avoid positive reward actions.");

		transform.clear();
		transformed.clear();

		return prob0e;
	}

}
