package common;

import java.util.BitSet;
import java.util.Iterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.stream.IntStream;

import common.iterable.IterableArray;

public class BitSetTools
{
	public static BitSet asBitSet(final int... indices)
	{
		final BitSet result = new BitSet();
		for (int i : indices) {
			result.set(i);
		}
		return result;
	}

	public static BitSet asBitSet(final Iterable<Integer> indices)
	{
		Iterator<Integer> iter = indices.iterator();
		return (iter instanceof OfInt) ? asBitSet((OfInt) iter) : asBitSet(iter);
	}

	public static BitSet asBitSet(final IntStream indices)
	{
		return asBitSet(indices.iterator());
	}

	public static BitSet asBitSet(final Iterator<Integer> indices)
	{
		final BitSet result = new BitSet();
		while (indices.hasNext()) {
			result.set(indices.next());
		}
		return result;
	}

	public static BitSet asBitSet(final OfInt indices)
	{
		final BitSet result = new BitSet();
		while (indices.hasNext()) {
			result.set(indices.nextInt());
		}
		return result;
	}

	public static BitSet shiftDown(final BitSet indices, final int offset)
	{
		assert offset >= 0 : "positive offset expected";
		return indices.get(offset, indices.length());
	}

	public static BitSet shiftUp(final BitSet indices, final int offset)
	{
		assert offset >= 0 : "positive offset expected";
		return asBitSet(indices.stream().map(i -> i + offset));
	}

	public static boolean areDisjoint(final BitSet... sets)
	{
		return areDisjoint(new IterableArray.Of<>(sets));
	}

	public static boolean areDisjoint(final Iterable<BitSet> sets)
	{
		final BitSet union = new BitSet();
		for (BitSet set : sets) {
			final int cardinality = union.cardinality();
			union.or(set);
			if (union.cardinality() < cardinality + set.cardinality()) {
				return false;
			}
		}
		return true;
	}

	public static boolean areNonEmpty(final Iterable<BitSet> sets)
	{
		for (BitSet set : sets) {
			if (set.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public static BitSet complement(final int toIndex, final BitSet indices)
	{
		final BitSet complement = (BitSet) indices.clone();
		complement.flip(0, toIndex);
		return complement;
	}

	public static BitSet minus(final BitSet set, final BitSet... sets)
	{
		final BitSet difference = (BitSet) set.clone();
		for (BitSet each : sets) {
			difference.andNot(each);
		}
		return difference;
	}

	public static BitSet union(final BitSet... sets)
	{
		BitSet union = new BitSet();
		for (BitSet set : sets) {
			union.or(set);
		}
		return union;
	}

	public static BitSet union(final Iterable<BitSet> sets)
	{
		return union(sets.iterator());
	}

	public static BitSet union(final Iterator<BitSet> sets)
	{
		BitSet union = new BitSet();
		while (sets.hasNext()) {
			union.or(sets.next());
		}
		return union;
	}

	public static BitSet intersect(BitSet set, BitSet... sets)
	{
		BitSet intersection = (BitSet) set.clone();
		for (BitSet each : sets) {
			intersection.and(each);
		}
		return intersection;
	}

	public static BitSet intersect(Iterable<BitSet> sets)
	{
		return intersect(sets.iterator());
	}

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

	public static boolean isSubset(BitSet subset, BitSet superset)
	{
		return minus(subset, superset).isEmpty();
	}

	/**
	 * Count the number of set bits up to the specified <code>toIndex</code> (exclusive).
	 * 
	 * @param indices
	 * @param toIndex index after the last bit to count
	 * @return number of set bits in the interval [0, toIndex)
	 * @see common.BitSetTools#countSetBits(BitSet, int, int)
	 */
	public static int countSetBits(final BitSet indices, final int toIndex)
	{
		return countSetBits(indices, 0, toIndex);
	}

	/**
	 * Count the number of set bits from the specified <code>fromIndex</code> (inclusive)
	 * to the specified <code>toIndex</code> (exclusive).
	 * 
	 * @param indices
	 * @param fromIndex index of the first bit to count
	 * @param toIndex index after the last bit to count
	 * @return number of set bits in the interval [fromIndex, toIndex)
	 */
	public static int countSetBits(final BitSet indices, final int fromIndex, final int toIndex)
	{
		int count = 0;
		for (int i = indices.nextSetBit(fromIndex); i >= 0 && i < toIndex; i = indices.nextSetBit(i + 1)) {
			count++;
		}
		return count;
	}

	public static int getIndexOfNthSetBit(final BitSet indices, final int n)
	{
		return getIndexOfNthSetBit(indices, 0, n);
	}

	/**
	 * Return the {@code n}-th set bit from the specified <code>fromIndex</code>.
	 * If no such bit exists then {@code -1} is returned.
	 * @param indices
	 * @param fromIndex
	 * @param n
	 * @return
	 */
	public static int getIndexOfNthSetBit(final BitSet indices, final int fromIndex, final int n)
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

	public static void main(final String[] args)
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