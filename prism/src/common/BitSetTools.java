//==============================================================================
//
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
//	* Marcus Daum <marcus.daum@ivi.fraunhofer.de> (Frauenhofer Institut)
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

package common;

import java.util.BitSet;
import java.util.Iterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.stream.IntStream;

import common.iterable.ArrayIterator;
import common.iterable.IterableArray;
import common.iterable.Reducible;

import javax.print.attribute.standard.ReferenceUriSchemesSupported;

public class BitSetTools
{
	/**
	 * Create a BitSet from an array of integers.
	 *
	 * @param indices the {@code int} array of set bits
	 * @return a {@code BitSet} with all the bits from the argument being set
	 */
	public static BitSet asBitSet(int... indices)
	{
		BitSet result = new BitSet();
		for (int i : indices) {
			result.set(i);
		}
		return result;
	}

	/**
	 * Create a BitSet from an {@link Iterator} of integers.
	 *
	 * @param indices the {@code Iterator} of set bits
	 * @return a {@code BitSet} with all the bits from the argument being set
	 */
	public static BitSet asBitSet(Iterator<Integer> indices)
	{
		if (indices instanceof OfInt) {
			return asBitSet((OfInt) indices);
		}
		return asBitSet(Reducible.extend(indices));
	}

	/**
	 * Create a BitSet from a {@link java.util.PrimitiveIterator.OfInt}.
	 *
	 * @param indices the {@code PrimitiveIterator.OfInt} of set bits
	 * @return a {@code BitSet} with all the bits from the argument being set
	 */
	public static BitSet asBitSet(OfInt indices)
	{
		return Reducible.extend(indices).collect(new BitSet());
	}

	/**
	 * Create a BitSet from an {@link IntStream}.
	 *
	 * @param indices the {@code IntStream} of set bits
	 * @return a {@code BitSet} with all the bits from the argument being set
	 */
	public static BitSet asBitSet(IntStream indices)
	{
		return asBitSet(indices.iterator());
	}

	/**
	 * Shift all indices down (towards zero) by an offset.
	 *
	 * @param indices the {@code BitSet} to be shifted
	 * @param offset the offset to shift
	 * @return a {@code BitSet} with all indices from the argument shifted by the offset
	 */
	public static BitSet shiftDown(BitSet indices, int offset)
	{
		assert offset >= 0 : "positive offset expected";
		return indices.get(offset, indices.length());
	}

	/**
	 * Shift all indices up (towards infinity) by an offset.
	 *
	 * @param indices the {@code BitSet} to be shifted
	 * @param offset the offset to shift
	 * @return a BitSet with all indices from the argument shifted by the offset
	 */
	public static BitSet shiftUp(BitSet indices, int offset)
	{
		assert offset >= 0 : "positive offset expected";
		return new IterableBitSet(indices).mapToInt((int i) -> i + offset).collect(new BitSet());
	}

	/**
	 * Test whether some {@link BitSet}s disjoint?
	 * Answer true if none or a single BitSet is given.
	 *
	 * @param sets the {@code BitSet}s to be tested
	 * @return true iff all arguments are disjoint
	 */
	public static boolean areDisjoint(BitSet... sets)
	{
		return areDisjoint(new ArrayIterator.Of<>(sets));
	}

	/**
	 * Test whether some {@link BitSet}s are disjoint?
	 * Answer true if none or a single BitSet is given.
	 *
	 * @param sets the Iterator of {@code BitSet}s to be tested
	 * @return {@code true} iff all arguments are disjoint
	 */
	public static boolean areDisjoint(Iterator<BitSet> sets)
	{
		BitSet union = new BitSet();
		while(sets.hasNext()) {
			int cardinality = union.cardinality();
			BitSet set = sets.next();
			union.or(set);
			if (union.cardinality() < cardinality + set.cardinality()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Test whether all {@link BitSet}s empty?
	 *
	 * @param sets the {@code Iterator} of {@code BitSet}s to be tested
	 * @return {@code true} iff all arguments are empty
	 */
	public static boolean areNonEmpty(Iterator<BitSet> sets)
	{
		while (sets.hasNext()) {
			if (sets.next().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Complement a {@link BitSet} up to an index.
	 *
	 * @param indices the {@code BitSet} to be complemented
	 * @param toIndex the index (exclusive) up to which the argument is complemented
	 * @return a copy of the argument with all bits flipped up to the index
	 */
	public static BitSet complement(BitSet indices, int toIndex)
	{
		return complement(indices, 0, toIndex);
	}

	/**
	 * Complement a {@link BitSet} between two indices.
	 *
	 * @param indices the {@code BitSet} to be complemented
	 * @param fromIndex the index (inclusive) of the first bit to be flipped
	 * @param toIndex the index (exclusive) up to which all bits are flipped
	 * @return a copy of the argument with all bits flipped between the given indices
	 */
	public static BitSet complement(BitSet indices, int fromIndex, int toIndex)
	{
		BitSet complement = (BitSet) indices.clone();
		complement.flip(fromIndex, toIndex);
		return complement;
	}

	/**
	 * Compute the set difference between a {@link BitSet} and multiple other BitSets.
	 *
	 * @param set the {@code BitSet} the other sets are substracted from
	 * @param sets the {@code BitSet}s to substract
	 * @return a {@code BitSet} with all indeces set that are in non but the first argument
	 */
	public static BitSet minus(BitSet set, BitSet... sets)
	{
		BitSet difference = (BitSet) set.clone();
		for (BitSet each : sets) {
			difference.andNot(each);
		}
		return difference;
	}

	/**
	 * Compute the union of multiple {@link BitSet}s.
	 *
	 * @param sets the {@code BitSet}s to be joined
	 * @return the union of the arguments
	 */
	public static BitSet union(BitSet... sets)
	{
		BitSet union = new BitSet();
		for (BitSet set : sets) {
			union.or(set);
		}
		return union;
	}

	/**
	 * Compute the union of multiple {@link BitSet}s.
	 *
	 * @param sets the {@code Iterator} of {@code BitSet}s to be joined
	 * @return the union of the arguments
	 */
	public static BitSet union(Iterator<BitSet> sets)
	{
		BitSet union = new BitSet();
		while (sets.hasNext()) {
			union.or(sets.next());
		}
		return union;
	}

	/**
	 * Compute the intersection of multiple {@link BitSet}s.
	 *
	 * @param sets the {@code BitSet}s to be cut
	 * @return the intersection of the arguments
	 */
	public static BitSet intersect(BitSet set, BitSet... sets)
	{
		BitSet intersection = (BitSet) set.clone();
		for (BitSet each : sets) {
			intersection.and(each);
		}
		return intersection;
	}

	/**
	 * Compute the intersection of multiple {@link BitSet}s.
	 *
	 * @param sets the {@code Iterator} of {@code BitSet}s to be cut
	 * @return the intersection of the arguments
	 */
	public static BitSet intersect(Iterator<BitSet> sets)
	{
		if (! sets.hasNext()) {
			throw new IllegalArgumentException("Expected at least one set.");
		}
		BitSet intersection = sets.next();
		while (sets.hasNext()) {
			intersection.and(sets.next());
		}
		return intersection;
	}

	/**
	 * Tests whether a {@link BitSet} is a subset of another {@link BitSet}.
	 *
	 * @param subset the subset to be tested
	 * @param superset the superset to be tested
	 * @return {@code true} iff the first argument is a subset of the second
	 */
	public static boolean isSubset(BitSet subset, BitSet superset)
	{
		return minus(subset, superset).isEmpty();
	}

	/**
	 * Count the number of set bits up to the specified {@code toIndex} (exclusive).
	 * 
	 * @param indices the {@code BitSet} to be be counted
	 * @param toIndex the index (exclusive) up to which is counted
	 * @return the number of set bits in the interval [0, toIndex)
	 * @see common.BitSetTools#countSetBits(BitSet, int, int)
	 */
	public static int countSetBits(BitSet indices, int toIndex)
	{
		return countSetBits(indices, 0, toIndex);
	}

	/**
	 * Count the number of set bits from the specified {@code fromIndex}
	 * to the specified {@code toIndex}.
	 *
	 * @param indices the {@code BitSet} to be be counted
	 * @param fromIndex the index (inclusive) of the first bit to count
	 * @param toIndex the index (exclusive) up to which is counted
	 * @return the number of set bits in the interval [fromIndex, toIndex)
	 */
	public static int countSetBits(BitSet indices, int fromIndex, int toIndex)
	{
		int count = 0;
		for (int i = indices.nextSetBit(fromIndex); i >= 0 && i < toIndex; i = indices.nextSetBit(i + 1)) {
			count++;
		}
		return count;
	}

	/**
	 * Return the {@code n}-th set bit from the specified {@code fromIndex}.
	 * If no such bit exists then {@code -1} is returned.
	 *
	 * @param indices the {@code BitSet} to be searched in
	 * @param n the number of bits to be counted
	 * @return the index of the {@code n}-th set bit
	 */
	public static int getIndexOfNthSetBit(BitSet indices, int n)
	{
		return getIndexOfNthSetBit(indices, 0, n);
	}

	/**
	 * Return the {@code n}-th set bit from the specified {@code fromIndex}.
	 * If no such bit exists then {@code -1} is returned.
	 *
	 * @param indices the {@code BitSet} to be searched in
	 * @param fromIndex the index (inclusive) of the first bit to be counted
	 * @param n the number of bits to be counted
	 * @return the index of the {@code n}-th set bit
	 */
	public static int getIndexOfNthSetBit(BitSet indices, int fromIndex, int n)
	{
		if (n < 1) {
			throw new IllegalArgumentException("n < 1: " + n);
		}
		int count = 1;
		for (int i = indices.nextSetBit(fromIndex); i >= 0; i = indices.nextSetBit(i + 1)) {
			if (count == n) {
				return i;
			}
			count++;
		}
		//there are not enough bits set in indices
		//the -1 is an artifact of BitSet.nextSetBit()
		return -1;
	}

	public static void main(String[] args)
	{
		BitSet bs = new BitSet();
		bs.set(0);
		bs.set(3);

		System.out.println("countSetBits(" + bs + ", " + 0 + ") = " + countSetBits(bs, 0));
		System.out.println("countSetBits(" + bs + ", " + 1 + ") = " + countSetBits(bs, 1));
		System.out.println("countSetBits(" + bs + ", " + 2 + ") = " + countSetBits(bs, 2));
		System.out.println("countSetBits(" + bs + ", " + 3 + ") = " + countSetBits(bs, 3));
		System.out.println("countSetBits(" + bs + ", " + 4 + ") = " + countSetBits(bs, 4));
		System.out.println("countSetBits(" + bs + ", " + 0 + ", " + 4 + ") = " + countSetBits(bs, 0, 4));
		System.out.println("countSetBits(" + bs + ", " + 1 + ", " + 4 + ") = " + countSetBits(bs, 1, 4));
		System.out.println("countSetBits(" + bs + ", " + 2 + ", " + 4 + ") = " + countSetBits(bs, 2, 4));
		System.out.println("countSetBits(" + bs + ", " + 3 + ", " + 4 + ") = " + countSetBits(bs, 3, 4));
		System.out.println("countSetBits(" + bs + ", " + 4 + ", " + 4 + ") = " + countSetBits(bs, 4, 4));
		System.out.println();

		int n = 10000000;

		BitSet superset = new BitSet(n);
		superset.set(0, n);

		BitSet subset = new BitSet(n);
		for (int i=0; i<n; i++) {
			subset.set((int)(n * Math.random()));
		}
		System.out.println("|subset| = " + subset.cardinality());

		System.out.print("isSubset ... ");
		StopWatch watch = new StopWatch();
		boolean result = watch.run(() -> isSubset(subset, superset));
		System.out.println(result + " in " + watch.elapsedSeconds() + "s");
	}
}