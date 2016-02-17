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

import parser.ast.Expression;
import prism.PrismException;


/**
 * Combines two (compatible) ModelExpressionTransformation.
 * <br>
 * The inner transformation is applied first, with the result
 * serving as the input for the second, outer transformation.
 *
 * @param <OriginalModel> The input type for the inner transformation
 * @param <IntermediateModel> The output type for the inner and the input type for the outer transformation
 * @param <TransformedModel> The output type for the outer transformation
 */
public class ModelExpressionTransformationNested<OriginalModel extends Model,
                                                 IntermediateModel extends Model,
                                                 TransformedModel extends Model>
	extends ModelTransformationNested<OriginalModel, IntermediateModel, TransformedModel>
	implements ModelExpressionTransformation<OriginalModel, TransformedModel> {

	/**
	 * Constructor.
	 * <br>
	 * The output (transformed) model of the inner transformation
	 * has to be the input (original) model of the outer transformation.
	 *
	 * @param innerTransformation the inner transformation
	 * @param outerTransformation the outer transformation
	 */
	public ModelExpressionTransformationNested(ModelExpressionTransformation<OriginalModel, ? extends IntermediateModel> innerTransformation,
	                                           ModelExpressionTransformation<? extends IntermediateModel, TransformedModel> outerTransformation)
	{
		super(innerTransformation, outerTransformation);
	}

	@Override
	public Expression getTransformedExpression() {
		// the transformed expression for the nested transformation
		// is the transformed expression for the outer transformation
		return ((ModelExpressionTransformation<?,?>) outerTransformation).getTransformedExpression();
	}

	@Override
	public Expression getOriginalExpression() {
		// the original expression for the nested transformation
		// is the original expression for the inner transformation
		return ((ModelExpressionTransformation<?,?>) innerTransformation).getTransformedExpression();
	}
}
