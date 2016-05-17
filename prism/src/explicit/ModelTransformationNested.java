//==============================================================================
//
//	Copyright (c) 2015-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Steffen Märcker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
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

import prism.PrismException;

/**
 * Combines two (compatible) ModelTransformation.
 * <br>
 * The inner transformation is applied first, with the result
 * serving as the input for the second, outer transformation.
 *
 * @param <OriginalModel> The input type for the inner transformation
 * @param <IntermediateModel> The output type for the inner and the input type for the outer transformation
 * @param <TransformedModel> The output type for the outer transformation
 */
public class ModelTransformationNested<OriginalModel extends Model,
                                       IntermediateModel extends Model,
                                       TransformedModel extends Model>
	implements ModelTransformation<OriginalModel, TransformedModel>
{
	protected final ModelTransformation<? extends OriginalModel, ? extends IntermediateModel> innerTransformation;
	protected final ModelTransformation<? extends IntermediateModel, ? extends TransformedModel> outerTransformation;

	public ModelTransformationNested(final ModelTransformation<? extends OriginalModel, ? extends IntermediateModel> inner,
                                     final ModelTransformation<? extends IntermediateModel, ? extends TransformedModel> outer)
	{
		if (inner.getTransformedModel() != outer.getOriginalModel()) {
			throw new IllegalArgumentException("Trying to nest unrelated ModelTransformations.");
		}
		this.innerTransformation = inner;
		this.outerTransformation = outer;
	}

	@Override
	public OriginalModel getOriginalModel()
	{
		return innerTransformation.getOriginalModel();
	}

	@Override
	public TransformedModel getTransformedModel()
	{
		return outerTransformation.getTransformedModel();
	}

	@Override
	public BitSet getTransformedStatesOfInterest()
	{
		return outerTransformation.getTransformedStatesOfInterest();
	}

	@Override
	public StateValues projectToOriginalModel(final StateValues svTransformedModel) throws PrismException
	{
		// first, get the StateValues in the intermediate model
		StateValues svIntermediate = outerTransformation.projectToOriginalModel(svTransformedModel);
		// then, in the original model
		StateValues svOriginal = innerTransformation.projectToOriginalModel(svIntermediate);
		return svOriginal;
	}

	@Override
	public Integer mapToTransformedModel(final int state)
	{
		final Integer intermediate = innerTransformation.mapToTransformedModel(state);
		return (intermediate == null) ? null : outerTransformation.mapToTransformedModel(intermediate);
	}

	@Override
	public BitSet mapToTransformedModel(final BitSet states)
	{
		return outerTransformation.mapToTransformedModel(innerTransformation.mapToTransformedModel(states));
	}

}
