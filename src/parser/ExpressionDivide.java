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

public class ExpressionDivide extends ExpressionBinary
{
	// create and return a new expression by renaming

	public Expression rename(RenamedModule rm) throws PrismException
	{
		ExpressionDivide e;
		
		e = new ExpressionDivide();		
		e.setOperand1(operand1.rename(rm));
		e.setOperand2(operand2.rename(rm));
		
		return e;
	}
	
	// create and return a new expression by expanding constants

	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		ExpressionDivide e;
		
		e = new ExpressionDivide();		
		e.setOperand1(operand1.expandConstants(constantList));
		e.setOperand2(operand2.expandConstants(constantList));
		
		return e;
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		int t1, t2;
		
		t1 = operand1.getType();
		t2 = operand2.getType();
		
		// make sure operands are ints or doubles
		if (t1==Expression.BOOLEAN || t2==Expression.BOOLEAN) {
			throw new PrismException("Type error in expression \"" + toString() + "\"");
		}
		
		// type is always double
		setType(Expression.DOUBLE);
	}

	// evaluate
	
	public Object evaluate(Values constantValues, Values varValues) throws PrismException
	{
		Object o1, o2, res;
		double d;
		
		// evaluate operands
		o1 = operand1.evaluate(constantValues, varValues);
		o2 = operand2.evaluate(constantValues, varValues);
		
		// we always do proper (not integer) division
		if (o1 instanceof Double) {
			d = ((Double)o1).doubleValue();
		}
		else {
			d = ((Integer)o1).intValue();
		}
		if (o2 instanceof Double) {
			d /= ((Double)o2).doubleValue();
		}
		else {
			d /= ((Integer)o2).intValue();
		}
		res = new Double(d);
		
		return res;
	}

	// convert to apmc data structures
	
	public int toApmc(Apmc apmc) throws ApmcException
	{  
	    return apmc.newBinaryOperand(apmc.DIV, operand1.toApmc(apmc), operand2.toApmc(apmc));
	}

	/**
	 *	Convert and build simulator expression data structure
	 */
	public int toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		return SimulatorEngine.createDivide(operand1.toSimulator(sim), operand2.toSimulator(sim));		
	}

	// convert to string
	
	public String toString()
	{
		return operand1 + "/" + operand2;
	}
}

//------------------------------------------------------------------------------
