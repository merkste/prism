//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Christian von Essen <christian.vonessen@imag.fr> (Verimag, Grenoble)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismSettings;

/**
 * Abstract class for (explicit) classes that compute (B)SCCs,
 * i.e. (bottom) strongly connected components, for a model's transition graph.
 */
public abstract class SCCComputer extends PrismComponent
{
	protected SCCConsumer consumer;

	// Method used for finding (B)SCCs
	public enum SCCMethod {
		TARJAN;
		public String fullName()
		{
			switch (this) {
			case TARJAN:
				return "Tarjan";
			default:
				return this.toString();
			}
		}
	};

	/**
	 * Static method to create a new SCCComputer object, depending on current settings.
	 */
	public static SCCComputer createSCCComputer(PrismComponent parent, Model model, SCCConsumer consumer) throws PrismException
	{
		switch (parent.getSettings().getChoice(PrismSettings.PRISM_SCC_METHOD_EXPLICIT)) {
		case Prism.TARJAN_RECURSIVE:
			return new SCCComputerTarjan(parent, model, consumer);
		case Prism.TARJAN_STACK:
			return new SCCComputerTarjanNonRecursive(parent, model, consumer);
			default:
			throw new PrismException("Unknown SCC decompositon method");
		}
	}

	/**
	 * Base constructor.
	 */
	public SCCComputer(PrismComponent parent, SCCConsumer consumer) throws PrismException
	{
		super(parent);
		this.consumer = consumer;
	}

	/**
	 * Compute strongly connected components (SCCs) and notify the consumer.
	 * This will only report non-trivial SCCs
	 */
	public void computeSCCs() throws PrismException
	{
		computeSCCs(true);
	}

	/**
	 * Compute strongly connected components (SCCs) and notify the consumer.
	 * Ignores trivial SCCS if {@code filterTrivialSCCs} is set to true.
	 */
	public abstract void computeSCCs(boolean filterTrivialSCCs) throws PrismException;

	protected boolean isTrivialSCC(Model model, BitSet scc)
	{
		if (scc.cardinality() != 1) {
			return false;
		}

		int s = scc.nextSetBit(0);
		if (model.someSuccessorsInSet(s, scc)) {
			return false;
		} else {
			return true;
		}
	}
}
