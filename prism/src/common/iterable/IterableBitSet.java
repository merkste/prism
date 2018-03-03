//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
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

package common.iterable;

import java.util.BitSet;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;

import common.IteratorTools;
import common.functions.ObjIntFunction;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;

/**
 * Convenience class to loop easily over the set/clear bits of a BitSet.
 *
 * For example:<br/><br/>
 * <code>for (Integer index : getSetBits(set)) { ... }</code><br/>
 */
public class IterableBitSet implements IterableInt
{
	protected final BitSet set;
	protected final boolean clearBits;
	protected final int fromIndex;
	protected final int maxIndex;

	/**
	 * Constructor for an Iterable that iterates over the set bits of {@code set}
	 */
	public IterableBitSet(BitSet set)
	{
		this(set, set.length()- 1, false);
	}

	/**
	 * Constructor for an Iterable that iterates over the set bits of {@code set}
	 */
	public IterableBitSet(BitSet set, int fromIndex, int maxIndex)
	{
		this(set, fromIndex, maxIndex, false);
	}

	/**
	 * Constructor for an Iterable that iterates over bits of the given set {@code set},
	 * up to the maximal index given by {@code maxIndex}. If {@code clearBits} is {@code true},
	 * iterate over the cleared bits instead of the set bits.
	 * @param set the underlying BitSet
	 * @param maxIndex the maximal index for iteration (negative = iterate over the empty set)
	 * @param clearBits if true, iterate over the cleared bits in the BitSet
	 */
	public IterableBitSet(BitSet set, int maxIndex, boolean clearBits)
	{
		this(set, 0, maxIndex, clearBits);
	}

	public IterableBitSet(BitSet set, int fromIndex, int maxIndex, boolean clearBits)
	{
		this.set       = set;
		this.fromIndex = fromIndex;
		this.maxIndex  = maxIndex;
		this.clearBits = clearBits;
	}

	/** Implementation of the iterator over the set bits */
	private class SetBitsIterator implements FunctionalPrimitiveIterator.OfInt
	{
		protected int current = -1;
		protected int next    = set.nextSetBit(fromIndex);

		@Override
		public boolean hasNext()
		{
			// limit to 0 ... maxIndex
			return next >= 0 && next <= maxIndex;
		}

		@Override
		public int nextInt()
		{
			if (hasNext()) {
				current = next;
				next    = set.nextSetBit(current + 1);
				return current;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove()
		{
			set.clear(current);
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			while (next >= 0 && next <= maxIndex) {
				current = next;
				next    = set.nextSetBit(current + 1);
				action.accept(current);
			}
		}

		@Override
		public int collectAndCount(Collection<? super Integer> collection)
		{
			int count = 0;
			while (next >= 0 && next <= maxIndex) {
				current = next;
				next    = set.nextSetBit(current + 1);
				collection.add(current);
				count++;
			}
			return count;
		}

		@Override
		public int collectAndCount(Integer[] array, int offset)
		{
			int count = offset;
			while (next >= 0 && next <= maxIndex) {
				current        = next;
				next           = set.nextSetBit(current + 1);
				array[count++] = current;
			}
			return count - offset;
		}

		@Override
		public int collectAndCount(int[] array, int offset)
		{
			int count = offset;
			while (next >= 0 && next <= maxIndex) {
				current        = next;
				next           = set.nextSetBit(current + 1);
				array[count++] = current;
			}
			return count - offset;
		}

		@Override
		public boolean contains(int i)
		{
			return (next >= 0 && next <= maxIndex) && (i >= next && i <= maxIndex) && set.get(i);
		}

		@Override
		public int count()
		{
			int count = 0;
			while (next >= 0 && next <= maxIndex) {
				current = next;
				next    = set.nextSetBit(current + 1);
				count++;
			}
			return count;
		}

		@Override
		public OptionalInt max()
		{
			if (next <= maxIndex) {
				int max = set.previousSetBit(maxIndex);
				if (max >= next) {
					return OptionalInt.of(max);
				}
			}
			return OptionalInt.empty();
		}

		@Override
		public OptionalInt min()
		{
			if (next <= maxIndex) {
				int min = set.nextSetBit(next);
				if (min <= maxIndex) {
					return OptionalInt.of(min);
				}
			}
			return OptionalInt.empty();
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super Integer, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (next >= 0 && next <= maxIndex) {
				current = next;
				next    = set.nextSetBit(current + 1);
				accumulator.apply(result, current);
			}
			return result;
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (next >= 0 && next <= maxIndex) {
				current = next;
				next    = set.nextSetBit(current + 1);
				accumulator.apply(result, current);
			}
			return result;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = identity;
			while (next >= 0 && next <= maxIndex) {
				current = next;
				next    = set.nextSetBit(current + 1);
				accumulator.applyAsInt(result, current);
			}
			return result;
		}

		@Override
		public int sum()
		{
			int sum = 0;
			while (next >= 0 && next <= maxIndex) {
				current = next;
				next    = set.nextSetBit(current + 1);
				sum    += current;
			}
			return sum;
		}
	}

	/** Implementation of the iterator over the cleared bits, requires that {@code maxIndex != null} */
	private class ClearBitsIterator implements FunctionalPrimitiveIterator.OfInt
	{
		protected int current = -1;
		protected int next    = set.nextClearBit(fromIndex);

		@Override
		public boolean hasNext()
		{
			// limit to 0 ... maxIndex
			return next <= maxIndex;
		}

		@Override
		public int nextInt()
		{
			if (hasNext()) {
				current = next;
				next    = set.nextClearBit(current + 1);
				return current;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove()
		{
			set.set(current);
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			BitSet bits = set;
			for(int i = next, max = maxIndex; i <= max; i = bits.nextClearBit(i + 1)) {
				action.accept(i);
				current = next;
			}
		}

		@Override
		public int collectAndCount(Collection<? super Integer> collection)
		{
			int count = 0;
			while (next <= maxIndex) {
				current = next;
				next    = set.nextClearBit(current + 1);
				collection.add(current);
				count++;
			}
			return count;
		}

		@Override
		public int collectAndCount(Integer[] array, int offset)
		{
			int count = offset;
			while (next <= maxIndex) {
				current        = next;
				next           = set.nextClearBit(current + 1);
				array[count++] = current;
			}
			return count - offset;
		}

		@Override
		public int collectAndCount(int[] array, int offset)
		{
			int count = offset;
			while (next <= maxIndex) {
				current        = next;
				next           = set.nextClearBit(current + 1);
				array[count++] = current;
			}
			return count - offset;
		}

		@Override
		public boolean contains(int i)
		{
			return (next <= maxIndex) && (i >= next && i <= maxIndex) && !set.get(i);
		}

		@Override
		public int count()
		{
			int count = 0;
			while (next <= maxIndex) {
				current = next;
				next    = set.nextClearBit(current + 1);
				count++;
			}
			return count;
		}

		@Override
		public OptionalInt max()
		{
			if (next <= maxIndex) {
				int max = set.previousClearBit(maxIndex);
				if (max >= next) {
					return OptionalInt.of(max);
				}
			}
			return OptionalInt.empty();
		}

		@Override
		public OptionalInt min()
		{
			if (next <= maxIndex) {
				int min = set.nextClearBit(next);
				if (min <= maxIndex) {
					return OptionalInt.of(min);
				}
			}
			return OptionalInt.empty();
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super Integer, T> accumulator)
		{
			T result = identity;
			while (next <= maxIndex) {
				current = next;
				next    = set.nextClearBit(current + 1);
				accumulator.apply(result, current);
			}
			return result;
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (next <= maxIndex) {
				current = next;
				next    = set.nextClearBit(current + 1);
				accumulator.apply(result, current);
			}
			return result;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = identity;
			while (next <= maxIndex) {
				current = next;
				next    = set.nextClearBit(current + 1);
				accumulator.applyAsInt(result, current);
			}
			return result;
		}

		@Override
		public int sum()
		{
			int sum = 0;
			while (next <= maxIndex) {
				current = next;
				next    = set.nextClearBit(current + 1);
				sum    += current;
			}
			return sum;
		}
	}

	@Override
	public FunctionalPrimitiveIterator.OfInt iterator()
	{
		if (clearBits == false) {
			return new SetBitsIterator();
		} else {
			return new ClearBitsIterator();
		}
	}

	/**
	 * Get an IterableBitSet that iterates over the bits of {@code set} that are set.
	 * @param set a BitSet
	 * @return an IterableBitSet over the set bits
	 */
	public static IterableBitSet getSetBits(BitSet set)
	{
		return new IterableBitSet(set);
	}

	/**
	 * Get an IterableBitSet that iterates over the bits of {@code set} that are set.
	 * @param set a BitSet
	 * @return an IterableBitSet over the set bits
	 */
	public static IterableBitSet getSetBits(BitSet set, int maxIndex)
	{
		return new IterableBitSet(set, 0, maxIndex, false);
	}

	/**
	 * Get an IterableBitSet that iterates over the bits of {@code set} that are set.
	 * @param set a BitSet
	 * @return an IterableBitSet over the set bits
	 */
	public static IterableBitSet getSetBits(BitSet set, int fromIndex, int maxIndex)
	{
		return new IterableBitSet(set, fromIndex, maxIndex, false);
	}

	/**
	 * Get an IterableBitSet that iterates over the cleared bits of {@code set}, up to {@code maxIndex}
	 * @param set a BitSet
	 * @param maxIndex the maximal index
	 * @return an IterableBitSet over the cleared bits
	 */
	public static IterableBitSet getClearBits(BitSet set, int maxIndex)
	{
		return new IterableBitSet(set, maxIndex, true);
	}

	/**
	 * Get an IterableBitSet that iterates over the cleared bits of {@code set}, up to {@code maxIndex}
	 * @param set a BitSet
	 * @param maxIndex the maximal index
	 * @return an IterableBitSet over the cleared bits
	 */
	public static IterableBitSet getClearBits(BitSet set, int fromIndex, int maxIndex)
	{
		return new IterableBitSet(set, fromIndex, maxIndex, true);
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
		test.set(2);
		test.set(3);
		test.set(5);
		test.set(8);
		test.set(13);
		test.set(21);

		IteratorTools.printIterator("set bits", getSetBits(test).iterator());
		IteratorTools.printIterator("clear bits", getClearBits(test, test.length()).iterator());


		System.out.println("\n" + test + " - set bits:");
		for (Integer index : getSetBits(test)) {
			System.out.println(index);
		}

		test.clear();
		for (@SuppressWarnings("unused")
		Integer index : getSetBits(test)) {
			throw new RuntimeException("BitSet should be empty!");
		}
	}
}