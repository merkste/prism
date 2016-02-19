//==============================================================================
//	
//	Copyright (c) 2014-
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

package common.iterable;

import java.util.BitSet;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.IntPredicate;

/**
 * A convenience wrapper around IterableBitSet that handles the three cases of
 * iterating over the set or cleared bits of a BitSet representing a set of states
 * as well as iterating over all states if the BitSet is {@code null}.
 */
public class IterableStateSet implements IterableInt
{
	private final IterableInt setOfStates;

	/**
	 * Constructor (iterate over all states 0..numStates-1)
	 * 
	 * @param numStates the number of states in the model, i.e., with indices 0..numStates-1
	 */
	public IterableStateSet(int numStates)
	{
		this(null, numStates, false);
	}

	/**
	 * Constructor (iterate over the sets given by setOfStates or over all states)
	 * 
	 * @param setOfStates the BitSet representing state indices in the model.
	 *                    {@code null} signifies "all states in the model"
	 * @param numStates the number of states in the model, i.e., with indices 0..numStates-1
	 */
	public IterableStateSet(BitSet setOfStates, int numStates)
	{
		this(setOfStates, numStates, false);
	}

	/**
	 * Constructor (iterate over the all states filtered by predicate or over all states)
	 * 
	 * @param setOfStates the AbstractPredicateInteger filtering the state indices in the model.
	 *                    {@code null} signifies "all states in the model"
	 * @param numStates the number of states in the model, i.e., with indices 0..numStates-1
	 */
	public IterableStateSet(IntPredicate predicate, int numStates)
	{
		if (predicate == null) {
			this.setOfStates = new Interval(numStates);
		} else {
			this.setOfStates = new FilteringIterable.OfInt(new Interval(numStates), predicate);
		}
	}

	/**
	 * Constructor (most general form)
	 *
	 * @param setOfStates the BitSet representing state indices in the model.
	 *                    {@code null} signifies "all states in the model"
	 * @param numStates the number of states in the model, i.e., with indices 0..numStates-1
	 * @param complement if {@code true}, iterate over all the states not included in setOfStates
	 */
	public IterableStateSet(BitSet setOfStates, int numStates, boolean complement)
	{
		if (setOfStates == null || (setOfStates.length() == numStates && setOfStates.cardinality() == numStates)) {
			// all states
			this.setOfStates = complement ? EmptyIterable.OfInt() : new Interval(numStates);
		} else if (setOfStates.isEmpty()) {
			// no states
			this.setOfStates = complement ? new Interval(numStates) : EmptyIterable.OfInt();
		} else {
			// build appropriate IterableBitSet with maxIndex = numStates-1
			this.setOfStates = new IterableBitSet(setOfStates, numStates - 1, complement);
		}
	}

	@Override
	public OfInt iterator()
	{
		return setOfStates.iterator();
	}

	/**
	 * Simple test method.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args)
	{
		BitSet test = new BitSet();
		test.set(1);
		test.set(3);
		final IntPredicate odd = n -> n % 2 == 1;

		int numStates = 5;

		System.out.println("\n" + test + " - included states:");
		for (Integer index : new IterableStateSet(test, numStates)) {
			System.out.println(index);
		}

		System.out.println("\nodd(n) - included states:");
		for (Integer index : new IterableStateSet(odd, numStates)) {
			System.out.println(index);
		}

		System.out.println("\n" + test + " - excluded states:");
		for (Integer index : new IterableStateSet(test, numStates, true)) {
			System.out.println(index);
		}

		System.out.println("\nAll " + numStates + " states:");
		for (Integer index : new IterableStateSet((BitSet) null, numStates)) {
			System.out.println(index);
		}

		System.out.println("\nNo " + numStates + " states:");
		for (Integer index : new IterableStateSet(null, numStates, true)) {
			System.out.println(index);
		}
	}
}