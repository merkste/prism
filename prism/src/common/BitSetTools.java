package common;

import java.util.BitSet;
import java.util.Iterator;

public class BitSetTools
{
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
}