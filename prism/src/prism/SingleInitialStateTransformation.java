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


package prism;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

/**
 * NondetModelTransformation that takes a set of states S and
 * adds a new, single initial state start with choices
 * act_s for s in S, P(start, act_s, s) = 1.0
 *
 * I.e., from the start state there is an initial choice which
 * of the S states is selected, then the model behaves as before.
 */
public class SingleInitialStateTransformation extends NondetModelTransformation
{
	/** The set of states that serve as the initial choices */
	private JDDNode S;

	/** Flag for "produce BDD for the row variables" */
	private final static boolean ROW = true;
	/** Flag for "produce BDD for the column variables" */
	private final static boolean COL = false;

	public SingleInitialStateTransformation(NondetModel model, JDDNode S)
	{
		super(model);
		this.S = S;
	}

	@Override
	public void clear()
	{
		super.clear();
		JDD.Deref(S);
	}
	
	@Override
	public int getExtraStateVariableCount()
	{
		return 1;
	}

	@Override
	public int getExtraActionVariableCount()
	{
		return 1 + originalModel.getNumDDRowVars();
	}

	private JDDNode start(boolean row)
	{
		JDDNode startBit = row ? extraRowVars.getVar(0).copy() : extraColVars.getVar(0).copy();
		return JDD.And(startBit, allZeroOriginalStates(row));
	}

	private JDDNode normalState(boolean row)
	{
		JDDNode startBit = row ? extraRowVars.getVar(0).copy() : extraColVars.getVar(0).copy();
		return JDD.Not(startBit);
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
		// !tau = !extraAction[0]
		// all other extra action vars = 0 as well
		// so, we can just zero all extra action vars
		JDDNode notTau = allZero(extraActionVars);

		return notTau;
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
	public JDDNode getTransformedTrans() throws PrismException
	{
		// the set S of states that can be target of the initial choice
		JDDNode Scol = JDD.PermuteVariables(S.copy(), originalModel.getAllDDRowVars(), originalModel.getAllDDColVars());

		// JDD.PrintMinterms(prism.getLog(), Scol.copy(), "Scol");

		// map the extra action vars to the corresponding successor state
		JDDVars stateActionVars = new JDDVars();
		for (int i=1; i < extraActionVars.getNumVars(); i++) {
			stateActionVars.addVar(extraActionVars.getVar(i).copy());
		}
		JDDNode initialChoice = JDD.Identity(stateActionVars, originalModel.getAllDDColVars());
		// JDD.PrintMinterms(prism.getLog(), initialChoice.copy(), "initialChoice(1)");
		stateActionVars.derefAll();

		// restrict successor states (and the actions) to those that are in S
		initialChoice = JDD.And(initialChoice, Scol);
		// JDD.PrintMinterms(prism.getLog(), initialChoice.copy(), "initialChoice(2)");
		// and add tau symbol
		initialChoice = JDD.And(tau(), initialChoice);
		// JDD.PrintMinterms(prism.getLog(), initialChoice.copy(), "initialChoice(3)");

		// the initial choice:
		// from the start state we can go to a normal successor,
		// constrained by the possible initial choices
		JDDNode transInitial = JDD.Times(start(ROW),
		                                 normalState(COL),
		                                 initialChoice);

		// JDD.PrintMinterms(prism.getLog(), transInitial.copy(), "transInitial");

		JDDNode transNormal = JDD.Times(normalState(ROW),  // a normal from state
		                                normalState(COL),  // a normal to state
		                                notTau(),  // not a special action
		                                originalModel.getTrans().copy());

		// JDD.PrintMinterms(prism.getLog(), transNormal.copy(), "transNormal");

		// JDD.PrintMinterms(prism.getLog(), originalModel.getTrans().copy(), "trans");
		// new trans = union of transInitial and transNormal
		JDDNode newTrans = JDD.Apply(JDD.MAX, transInitial, transNormal);
		// JDD.PrintMinterms(prism.getLog(), newTrans.copy(), "newTrans");
		return newTrans;
	}

	@Override
	public JDDNode getTransformedStart() throws PrismException
	{
		return start(ROW);
	}
	
	@Override
	public JDDNode getReachableStates()
	{
		// reachable: the new start state and all the originially reachable states
		return JDD.Or(JDD.And(normalState(ROW), originalModel.getReach().copy()),
		              start(ROW));
	}

}
