//==============================================================================
//	
//	Copyright (c) 2014-
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

import prism.PrismException;

/**
 * Interface for a model transformation.
 */
public interface ModelTransformation<OriginalModel extends Model, TransformedModel extends Model> {

	/** Get the original model. */
	public OriginalModel getOriginalModel();

	/** Get the transformed model. */
	public TransformedModel getTransformedModel();

	/**
	 * Take a {@code StateValues} object for the transformed model and
	 * project the values to the original model.
	 * @param svTransformedModel a {@code StateValues} object for the transformed model
	 * @return a corresponding {@code StateValues} object for the original model.
	 **/
	public StateValues projectToOriginalModel(StateValues svTransformedModel) throws PrismException;

	/**
	 * Get the transformed set of states of interest.
	 **/
	public BitSet getTransformedStatesOfInterest();

	/**
	 * Get the corresponding index of a {@code state} in the transformed model.
	 * This is the index from which a result may be projected to the original model.
	 * If no such state exists, return {@code null}.
	 *
	 * @param state index in the original model
	 * @return corresponding index in the transformed model or null
	 */
	public Integer mapToTransformedModel(int state);

	/**
	 * Get the indices of a set of {@code states} in the transformed model.
	 *
	 * @param states set of indices in the original model
	 * @return set of indices in the transformed model
	 */
	public BitSet mapToTransformedModel(BitSet states);
}
