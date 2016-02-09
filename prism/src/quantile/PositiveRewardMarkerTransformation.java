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
import prism.ECComputer;
import prism.NondetModel;
import prism.NondetModelTransformation;
import prism.Prism;
import prism.PrismException;
import prism.StateValuesMTBDD;

/**
 * Transform MDP by switching between two copies: Seeing a (state or transition)
 * reward switches to the marked copy, switching back when there is no reward.
 */
public class PositiveRewardMarkerTransformation extends NondetModelTransformation {
	/** Context */
	private QuantileCalculatorContext qcc;
	/** 0/1 BDD with all transitions that have positive reward */
	private JDDNode tr01PosR;

	/** Flag for "produce BDD for the row variables" */
	private final static boolean ROW = true;
	/** Flag for "produce BDD for the column variables" */
	private final static boolean COL = false;

	/** Constructor */
	public PositiveRewardMarkerTransformation(QuantileCalculatorContext qcc)
	{
		super((NondetModel) qcc.getModel());

		this.qcc = qcc;

		tr01PosR = qcc.getTransitions01WithPosReward();
		if (qcc.debugDetailed()) qcc.debugDD(tr01PosR.copy(), "tr01PosR");
	}

	@Override
	public void clear()
	{
		super.clear();
		if (tr01PosR != null) JDD.Deref(tr01PosR);
	}

	@Override
	public int getExtraStateVariableCount()
	{
		// one extra variable
		// 1 = marked
		// 0 = unmarked
		return 1;
	}

	@Override
	public int getExtraActionVariableCount()
	{
		return 0;
	}

	/**
	 * Returns the markerVariable in a JDDVars.
	 * This is not a copy, do not dereference!
	 */
	private JDDVars markerVariable()
	{
		return extraRowVars;
	}

	/**
	 * Mask for unmarked states
	 * <br>[ REFS: <i>result</i> ]
	 * @param row BDD for row variables?
	 */
	private JDDNode unmarkedState(boolean row)
	{
		JDDNode v0 = row ? extraRowVars.getVar(0) : extraColVars.getVar(0);
		return JDD.Not(v0.copy());
	}

	/**
	 * Mask for marked states
 	 * <br>[ REFS: <i>result</i> ]
	 * @param row BDD for row variables?
	 */
	private JDDNode markedState(boolean row)
	{
		JDDNode v0 = row ? extraRowVars.getVar(0) : extraColVars.getVar(0);
		return v0.copy();
	}

	/**
	 * Get a refed copy of the original transition matrix
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode tr() {
		JDDNode tr = originalModel.getTrans().copy();
		return tr;
	}

	/**
	 * Get a refed copy of the transitions with positive reward
	 * <br>[ REFS: <i>result</i> ]
	 */
	private JDDNode tr01PosR() {
		return tr01PosR.copy();
	}

	@Override
	public JDDNode getTransformedTrans()
	{
		JDDNode result;

		// posR & tr -> marked'
		JDDNode trans = JDD.Times(tr(),
		                          tr01PosR(),
		                          markedState(COL));
		result = trans;
		if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (1)");
		
		// !posR & tr -> marked'
		trans = JDD.Times(tr(),
		                  JDD.Not(tr01PosR()),
		                  unmarkedState(COL));
		if (qcc.debugDetailed()) qcc.debugDD(trans.copy(), "trans (2)");
		result = JDD.Apply(JDD.MAX, result, trans);
		if (qcc.debugDetailed()) qcc.debugDD(result.copy(), "result (2)");
		return result;
	}

	@Override
	public JDDNode getTransformedStart() {
		JDDNode newStart = JDD.And(originalModel.getReach().copy(), unmarkedState(ROW));
		return newStart;
	}

	/** Project StateValues to original model
	 * <br>[ REFS: <i>result</i> ]
	 */
	public JDDNode projectToOriginal(StateValuesMTBDD sv) throws PrismException
	{
		JDDNode filter = unmarkedState(ROW); 
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
	 * Compute the union of end components that have positive reward,
	 * i.e., where there exists a scheduler that can achieve arbitrarily high
	 * reward accumulation.
	 * <br>[ REFS: <i>result</i> ]
	 */
	public static JDDNode computeMECWithPosR(QuantileCalculator qc, QuantileCalculatorContext qcc) throws PrismException
	{
		qc.getLog().println("Transform MDP, marking posR transitions...");

		NondetModel model = (NondetModel) qcc.getModel();

		PositiveRewardMarkerTransformation transform = new PositiveRewardMarkerTransformation(qcc);
		NondetModel transformed = model.getTransformed(transform);
		qc.getLog().println("\nTransformed MDP:");
		transformed.printTransInfo(qc.getLog());

		try {
			if (qcc.debugLevel() > 1) {
				transformed.exportToFile(Prism.EXPORT_DOT, true, new File("transformed-mdp-posR-marked.dot"));
			}
		} catch (FileNotFoundException e) {}

		if (qcc.debugLevel() > 1) transformed.dump(qcc.getLog());

		JDDNode accept = transform.markedState(ROW);
		JDDNode remain = qcc.getRemainStates();

		qc.getLog().println("\nCompute MEC with []a and []<>posR...");
		ECComputer computer = ECComputer.createECComputer(qcc, transformed);
		computer.computeMECStates(remain);
		JDDNode mecStates = JDD.Constant(0.0);
		for (JDDNode mec : computer.getMECStates()) {
			if (JDD.AreIntersecting(mec, accept)) {
				mecStates = JDD.Or(mecStates, mec);
			} else {
				JDD.Deref(mec);
			}
		}
		JDD.Deref(remain);
		JDD.Deref(accept);

		if (qcc.debugDetailed()) qcc.debugDD(mecStates.copy(), "mecStates (transformed)");

		// we don't care whether the marked bit is set or not
		mecStates = JDD.MaxAbstract(mecStates, transform.markerVariable());
		if (qcc.debugDetailed()) qcc.debugDD(mecStates.copy(), "mecStates (original)");
		qc.getLog().println("Done, " + JDD.GetNumMintermsString(mecStates, model.getNumDDRowVars())+" states are in MEC with []a & []<>posR.");

		transform.clear();
		transformed.clear();

		return mecStates;
	}

	/**
	 * Compute the union of end components that do not have a positive reward transition,
	 * i.e., where there exists a scheduler that can ensure that no more reward is
	 * accumulated.
	 * <br>[ REFS: <i>result</i> ]
	 */
	public static JDDNode computeMECWithAlwaysNotPosR(QuantileCalculator qc, QuantileCalculatorContext qcc) throws PrismException
	{
		qc.getLog().println("Transform MDP, marking posR transitions...");

		NondetModel model = (NondetModel) qcc.getModel();

		PositiveRewardMarkerTransformation transform = new PositiveRewardMarkerTransformation(qcc);
		NondetModel transformed = model.getTransformed(transform);
		qc.getLog().println("\nTransformed MDP:");
		transformed.printTransInfo(qc.getLog());

		try {
			if (qcc.debugLevel() > 1) {
				transformed.exportToFile(Prism.EXPORT_DOT, true, new File("transformed-mdp-posR-marked.dot"));
			}
		} catch (FileNotFoundException e) {}

		if (qcc.debugLevel() > 1) transformed.dump(qcc.getLog());

		JDDNode remain = JDD.Not(transform.markedState(ROW));

		qc.getLog().println("\nCompute MEC []!posR...");
		ECComputer computer = ECComputer.createECComputer(qcc, transformed);
		computer.computeMECStates(remain);
		JDDNode mecStates = JDD.Constant(0.0);
		for (JDDNode mec : computer.getMECStates()) {
			mecStates = JDD.Or(mecStates, mec);
		}
		JDD.Deref(remain);

		if (qcc.debugDetailed()) qcc.debugDD(mecStates.copy(), "mecStates (transformed)");

		StateValuesMTBDD sv = new StateValuesMTBDD(mecStates, transformed);
		// filter marked states
		JDDNode filter = transform.unmarkedState(ROW);
		sv.filter(filter);
		JDD.Deref(filter);
		// sum over marker variable
		StateValuesMTBDD svOriginal = sv.sumOverDDVars(transform.markerVariable(), model).convertToStateValuesMTBDD();
		sv.clear();
		// Result
		JDDNode result = svOriginal.getJDDNode().copy();
		svOriginal.clear();

		qc.getLog().println("Done, " + JDD.GetNumMintermsString(result, model.getNumDDRowVars())+" states are in MEC with []!posR.");

		transform.clear();
		transformed.clear();

		return result;
	}

}
