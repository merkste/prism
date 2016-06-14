package explicit.modelviews;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import common.functions.primitive.PairPredicateInt;
import common.iterable.IterableBitSet;

public class EquivalenceRelationInteger implements PairPredicateInt
{
	final protected Map<Integer, BitSet> classes = new HashMap<Integer, BitSet>();
	final protected BitSet nonRepresentatives    = new BitSet();

	public EquivalenceRelationInteger() {}

	public EquivalenceRelationInteger(final Iterable<BitSet> equivalenceClasses)
	{
		for (BitSet equivalenceClass : equivalenceClasses) {
			switch (equivalenceClass.cardinality()) {
			case 0:
				throw new IllegalArgumentException("expected non-empty classes");
			case 1:
				continue;
			default:
				for (Integer i : new IterableBitSet(equivalenceClass)) {
					if (classes.put(i, equivalenceClass) != null) {
						throw new IllegalArgumentException("expected disjoint classes");
					}
				}
				nonRepresentatives.or(equivalenceClass);
				nonRepresentatives.clear(equivalenceClass.nextSetBit(0));
			}
		}
	}

	@Override
	public boolean test(final int i, final int j)
	{
		if (i == j) {
			return true;
		}
		final BitSet equivalenceClass = classes.get(i);
		return equivalenceClass != null && equivalenceClass.get(j);
	}

	public int getRepresentative(final int i)
	{
		final BitSet equivalenceClass = classes.get(i);
		return equivalenceClass == null ? i : equivalenceClass.nextSetBit(0);
	}

	public BitSet getEquivalenceClass(final int i)
	{
		BitSet equivalenceClass = getEquivalenceClassOrNull(i);
		if (equivalenceClass == null) {
			equivalenceClass = new BitSet(i + 1);
			equivalenceClass.set(i);
		}
		return equivalenceClass;
	}

	/**
	 * Return the equivalence class of {@code i} if {@code i} is no singleton, otherwise {@code null}. 
	 * @param i a number
	 * @return {@code null} if [i] is a singleton, otherwise [i]
	 */
	public BitSet getEquivalenceClassOrNull(final int i)
	{
		return classes.get(i);
	}

	public BitSet getNonRepresentatives()
	{
		return nonRepresentatives;
	}

	public boolean isRepresentative(final int i)
	{
		return ! nonRepresentatives.get(i);
	}



	public static class KeepSingletons extends EquivalenceRelationInteger
	{
		public KeepSingletons(Iterable<BitSet> equivalenceClasses)
		{
			for (BitSet equivalenceClass : equivalenceClasses) {
				switch (equivalenceClass.cardinality()) {
				case 0:
					throw new IllegalArgumentException("expected non-empty classes");
				default:
					for (Integer i : new IterableBitSet(equivalenceClass)) {
						if (classes.put(i, equivalenceClass) != null) {
							throw new IllegalArgumentException("expected disjoint classes");
						}
					}
					nonRepresentatives.or(equivalenceClass);
					nonRepresentatives.clear(equivalenceClass.nextSetBit(0));
				}
			}
		}

		@Override
		public BitSet getEquivalenceClassOrNull(int i)
		{
			BitSet equivalenceClass = super.getEquivalenceClassOrNull(i);
			if (equivalenceClass == null) {
				return null;
			}
			return (equivalenceClass.cardinality() == 1) ? null : equivalenceClass;
		}

		public BitSet getOriginalEquivalenceClass(int i)
		{
			return classes.get(i);
		}
	}
}