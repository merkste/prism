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

import prism.PrismException;

/** Interface for a product operator for a DTMC -> DTMC product */
public interface DTMCProductOperator
{
	/** Get the product state when considering some initial state in the DTMC */
	public ProductState getInitialState(Integer dtmc_state) throws PrismException;

	/** Get the successor state for the ProductState when going to dtmc_to_state in the DTMC */
	public ProductState getSuccessor(ProductState from_state, Integer dtmc_to_state) throws PrismException;

	/**
	 * Notifies the Operator that state is assigned index in the product.
	 * Guaranteed to be called exactly once during the product construction for every
	 * ProductState.
	 */
	public void notify(ProductState state, Integer index) throws PrismException;

	/** Notifies the Operator that the product construction is finished */
	public void finish() throws PrismException;

	/** Get the underlying DTMC */
	public DTMC getGraph();
}
