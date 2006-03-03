//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker, Andrew Hinton
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

package parser;

import java.util.Vector;

import prism.PrismException;
import apmc.*;
import simulator.*;

public class ExpressionOr extends ExpressionNary
{
	// create and return a new expression by renaming

	public Expression rename(RenamedModule rm) throws PrismException
	{
		int i, n;
		ExpressionOr e;
		
		e = new ExpressionOr();		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			e.addOperand(getOperand(i).rename(rm));
		}
		
		return e;
	}
		
	// create and return a new expression by expanding constants

	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		int i, n;
		ExpressionOr e;
		
		e = new ExpressionOr();		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			e.addOperand(getOperand(i).expandConstants(constantList));
		}
		
		return e;
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		int i, n;
		
		// make sure that all operands are booleans
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			if (getOperand(i).getType() != Expression.BOOLEAN) {
				throw new PrismException("Type error in expression \"" + toString() + "\"");
			}
		}
		
		// result is always boolean
		setType(Expression.BOOLEAN);
	}

	// evaluate
	
	public Object evaluate(Values constantValues, Values varValues) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			if (getOperand(i).evaluateBoolean(constantValues, varValues)) {
				return new Boolean(true);
			}
		}
		
		return new Boolean(false);
	}

	// convert to apmc data structures
	
	public int toApmc(Apmc apmc) throws ApmcException
	{
		int r;
		int n = getNumOperands();
		
		if( n < 1 )
			throw new ApmcException("Expression \"" + toString() + "\" has zero operands");
		
		r = getOperand(0).toApmc(apmc);
		for(int i = 1; i < n; i++)
			r = apmc.newBinaryOperand(apmc.OR, r, getOperand(i).toApmc(apmc));
		
		return r;
	}

	/**	
	 *	Convert and build simulator expression data structure
	 */
	public int toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		int n = getNumOperands();
		if( n < 1 )
			throw new SimulatorException("Expression \"" + toString() + "\" has zero operands");
		int[] exprs = new int[n];
		
		//Collect operands pointers in an array
		for (int i = 0; i < n; i++) 
			exprs[i] = getOperand(i).toSimulator(sim);
		
		return SimulatorEngine.createOr(exprs);
	}

	// convert to string
	
	public String toString()
	{
		int i, n;
		String s = "";
		
		n = getNumOperands();
		for (i = 0; i < n-1; i++) {
			s = s + getOperand(i) + "|";
		}
		if (n > 0) {
			s = s + getOperand(n-1);
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
