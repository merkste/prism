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

package explicit;

import java.util.BitSet;

import parser.ast.Expression;

/**
 * A ModelExpressionTransformation where only the expression changes, not the model.
 * @param <M> the model type
 */
public class ModelExpressionTransformationIdentity<M extends Model> implements ModelExpressionTransformation<M,M> {
	/** The model */
	private M model;

	/** The original expression */
	private Expression originalExpression;

	/** The transformed expression */
	private Expression transformedExpression;

	/** The original (and transformed) states of interest */
	private BitSet statesOfInterest;

	/**
	 * Constructor.
	 * @param model the model
	 * @param originalExpression the original expression
	 * @param transformedExpression the transformed expression
	 * @param statesOfInterest the states of interest
	 */
	public ModelExpressionTransformationIdentity(M model, Expression originalExpression, Expression transformedExpression, BitSet statesOfInterest)
	{
		this.model = model;
		this.originalExpression = originalExpression;
		this.transformedExpression = transformedExpression;
		this.statesOfInterest = statesOfInterest;
	}

	@Override
	public M getOriginalModel()
	{
		return model;
	}

	@Override
	public M getTransformedModel()
	{
		return model;
	}

	@Override
	public StateValues projectToOriginalModel(StateValues svTransformedModel)
	{
		return svTransformedModel;
	}

	@Override
	public Expression getTransformedExpression()
	{
		return transformedExpression;
	}

	@Override
	public Expression getOriginalExpression()
	{
		return originalExpression;
	}

	@Override
	public BitSet getTransformedStatesOfInterest()
	{
		return statesOfInterest;
	}

	@Override
	public Integer mapToTransformedModel(final int state)
	{
		return state;
	}

	@Override
	public BitSet mapToTransformedModel(final BitSet states)
	{
		return (BitSet) states.clone();
	}

}
