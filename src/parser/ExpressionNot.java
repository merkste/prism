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

public class ExpressionNot extends ExpressionUnary
{
	// constructors
	
	public ExpressionNot()
	{
		super();
	}
	
	public ExpressionNot(Expression e)
	{
		super(e);
	}
	
	// create and return a new expression by renaming

	public Expression rename(RenamedModule rm) throws PrismException
	{
		return new ExpressionNot(operand.rename(rm));		
	}
	
	// create and return a new expression by expanding constants

	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		return new ExpressionNot(operand.expandConstants(constantList));
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		// check operand is boolean
		if (operand.getType() != Expression.BOOLEAN) {
			throw new PrismException("Type error in expression \"" + toString() + "\"");
		}
		
		// result is always boolean
		setType(Expression.BOOLEAN);
	}

	// evaluate
	
	public Object evaluate(Values constantValues, Values varValues) throws PrismException
	{
		return new Boolean(!operand.evaluateBoolean(constantValues, varValues));
	}

	// convert to apmc data structures
	
	public int toApmc(Apmc apmc) throws ApmcException
	{
		return apmc.newUnaryOperand(apmc.NOT, operand.toApmc(apmc));
	}

	/**
	 *	Convert and build simulator expression data structure
	 */
	public int toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		return SimulatorEngine.createNot(operand.toSimulator(sim));
	}

	// convert to string
	
	public String toString()
	{
		return "!" + operand;
	}
}

//------------------------------------------------------------------------------
