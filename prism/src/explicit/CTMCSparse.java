//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import java.util.Map;
import java.util.Map.Entry;

import common.IterableBitSet;
import common.iterable.ArrayIterator;
import common.iterable.FunctionalIterable;
import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.Interval;

import java.util.BitSet;

import prism.ModelType;
import prism.PrismException;

/**
 * Simple explicit-state representation of a CTMC.
 */
public class CTMCSparse extends DTMCSparse implements CTMC
{
	/**
	 * The cached embedded DTMC.
	 * <p>
	 * Will become invalid if the CTMC is changed. In this case
	 * construct a new one by calling buildImplicitEmbeddedDTMC()
	 * <p>
	 * We cache this so that the PredecessorRelation of the
	 * embedded DTMC is cached.
	 */
	private DTMC cachedEmbeddedDTMC = null;

	// Constructors

	/**
	 * Copy constructor.
	 */
	public CTMCSparse(CTMC ctmc)
	{
		super(ctmc);
	}
	
	/**
	 * Construct a CTMC from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Note: have to build new Distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public CTMCSparse(CTMC ctmc, int permut[])
	{
		super(ctmc, permut);
	}

	// Accessors (for ModelSimple, overrides DTMCSimple)
	
	@Override
	public ModelType getModelType()
	{
		return ModelType.CTMC;
	}

	// Accessors (for CTMC)
	
	@Override
	public double getExitRate(int state)
	{
		return new ArrayIterator.OfDouble(probabilities, rows[state], rows[state+1]).sum();
	}
	
	@Override
	public double getMaxExitRate()
	{
		IterableDouble exitRates = new Interval(getNumStates()).mapToDouble((int s) -> getExitRate(s));
		return exitRates.max().orElse(Double.NEGATIVE_INFINITY);
	}
	
	@Override
	public double getMaxExitRate(BitSet subset)
	{
		IterableDouble exitRates = new IterableBitSet(subset).mapToDouble((int s) -> getExitRate(s));
		return exitRates.max().orElse(Double.NEGATIVE_INFINITY);
	}
	
	@Override
	public double getDefaultUniformisationRate()
	{
		return 1.02 * getMaxExitRate(); 
	}
	
	@Override
	public double getDefaultUniformisationRate(BitSet nonAbs)
	{
		return 1.02 * getMaxExitRate(nonAbs); 
	}
	
	@Override
	public DTMC buildImplicitEmbeddedDTMC()
	{
		DTMC dtmc = new DTMCSparse(new DTMCEmbeddedSimple(this));
		if (cachedEmbeddedDTMC != null) {
			// replace cached DTMC
			cachedEmbeddedDTMC = dtmc;
		}
		return dtmc;
	}
	
	@Override
	public DTMC getImplicitEmbeddedDTMC()
	{
		if (cachedEmbeddedDTMC == null) {
			cachedEmbeddedDTMC = new DTMCEmbeddedSimple(this);
		}
		return cachedEmbeddedDTMC;
	}

	
	@Override
	public DTMC buildEmbeddedDTMC()
	{
		int numStates = getNumStates();
		DTMCSimple dtmc = new DTMCSimple(numStates);
		for (int in : getInitialStates()) {
			dtmc.addInitialState(in);
		}
		for (int i = 0; i < numStates; i++) {
			final int s = i;
			FunctionalIterable<Entry<Integer,Double>> transitions = FunctionalIterable.extend(() -> getTransitionsIterator(s));
			double d = transitions.mapToDouble(Entry::getValue).sum();
			if (d == 0) {
				dtmc.setProbability(i, i, 1.0);
			} else {
				for (Map.Entry<Integer, Double> e : transitions) {
					dtmc.setProbability(i, e.getKey(), e.getValue() / d);
				}
			}
		}
		return new DTMCSparse(dtmc);
	}

	@Override
	public void uniformise(double q) throws PrismException
	{
		throw new PrismException("Can't uniformise a CTMCSparse since it cannot be modified after construction");
	}

	@Override
	public DTMC buildImplicitUniformisedDTMC(double q)
	{
		return new DTMCUniformisedSimple(this, q);
	}
	
	@Override
	public DTMCSimple buildUniformisedDTMC(double q)
	{
		int numStates = getNumStates();
		DTMCSimple dtmc = new DTMCSimple(numStates);
		for (int in : getInitialStates()) {
			dtmc.addInitialState(in);
		}
		for (int i = 0; i < numStates; i++) {
			final int s = i;
			FunctionalIterable<Entry<Integer,Double>> transitions = FunctionalIterable.extend(() -> getTransitionsIterator(s));
			double d = transitions.mapToDouble(Entry::getValue).sum();
			if (d == 0) {
				dtmc.setProbability(i, i, 1.0);
			} else {
				for (Map.Entry<Integer, Double> e : transitions) {
					dtmc.setProbability(i, e.getKey(), e.getValue() / d);
				}
			}
		}
		return dtmc;
	}
}
