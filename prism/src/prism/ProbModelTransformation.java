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
 * Description of a transformation for a ProbModel (DTMC/CTMC).
 * The transformation can request the allocation of extra state variables,
 * and specify new transition matrix, initial states and reward information.
 * <br>
 * After use, the transformation should be cleared by calling {@code clear()}.
 */
public abstract class ProbModelTransformation
{
	protected ProbModel originalModel;
	protected JDDVars extraRowVars;
	protected JDDVars extraColVars;

	/**
	 * Constructor with the original model that is to be transformed.
	 * The original model is not changed and will not be cleared when
	 * {@code clear()} is called.
 	 */
	public ProbModelTransformation(ProbModel model)
	{
		originalModel = model;
	}

	/** Clear the transformation */
	public void clear()
	{
		if (extraRowVars != null) {
			extraRowVars.derefAll();
		}
		if (extraColVars != null) {
			extraColVars.derefAll();
		}
	}

	/** Return the name for any new extra state variables */
	public String getExtraStateVariableName()
	{
		return "extra";
	}

	/** Get the number of needed extra state variables for the transformation.
	 *  This is the number n of state variables, will lead to the allocation of
	 *  2*n variables, n row and n column variables */
	public abstract int getExtraStateVariableCount();

	/**
	 * This method is called to notify the transformation about the state variables
	 * that were allocated.
	 * <br>
	 * The extraRowVars and extraColVars are copies, i.e., this method is responsible
	 * to ensure that they will be derefed eventually.
	 * <br>[ STORES: extraRowVars, extraColVars ]
	 */
	public void hookExtraStateVariableAllocation(JDDVars extraRowVars, JDDVars extraColVars)
	{
		this.extraRowVars = extraRowVars;
		this.extraColVars = extraColVars;
	}

	/**
	 * Get the transformed transition function.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public abstract	JDDNode getTransformedTrans();

	/**
	 * Get the transformed start function.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public abstract JDDNode getTransformedStart();

	/**
	 * Get the transformed state reward relation, given the old reward relation.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getTransformedStateReward(JDDNode oldReward)
	{
		JDD.Ref(oldReward);
		return oldReward;
	}

	/**
	 * Get the transformed transition reward relation, given the old reward relation.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getTransformedTransReward(JDDNode oldReward)
	{
		JDD.Ref(oldReward);
		return oldReward;
	}
}
