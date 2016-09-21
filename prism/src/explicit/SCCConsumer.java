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

import prism.PrismComponent;
import prism.PrismException;

/**
 * Abstract base class for a consumer of SCC information, for use with an {@code SCCComputer}.
 * When a new SCC is discovered, {@code notifyNextSCC} will be called with a {@code BitSet} of the
 * states in the SCC. When the SCC computation is finished, {@code notifyDone()} will be
 * called once.
 */
public abstract class SCCConsumer extends PrismComponent {
	protected Model model;

	public SCCConsumer(PrismComponent parent, Model model)
	{
		super(parent);
		this.model = model;
	}

	/**
	 * Call-back function. Will be called upon discovery of a new SCC.
	 * @param scc the set of states in the SCC.
	 **/
	public abstract void notifyNextSCC(BitSet scc) throws PrismException;

	/**
	 * Call-back function. Will be called after SCC computation is complete.
	 */
	public void notifyDone() {}
}
