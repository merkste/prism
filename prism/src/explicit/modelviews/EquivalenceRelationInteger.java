package explicit.modelviews;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import common.functions.primitive.PairPredicateInt;
import common.iterable.IterableBitSet;

public class EquivalenceRelationInteger implements PairPredicateInt
{
	final private Map<Integer, BitSet> classes = new HashMap<Integer, BitSet>();
	final private BitSet nonRepresentatives = new BitSet();

	public EquivalenceRelationInteger(final Iterable<BitSet> equivalenceClasses)
	{
		this(equivalenceClasses, true);
	}

	public EquivalenceRelationInteger(final Iterable<BitSet> equivalenceClasses, final boolean dropSingletonClasses)
	{
		for (BitSet equivalenceClass : equivalenceClasses) {
			switch (equivalenceClass.cardinality()) {
			case 0:
				throw new IllegalArgumentException("expected non-empty classes");
			case 1:
				if (dropSingletonClasses){
					continue;
				}
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
			equivalenceClass = new BitSet(1);
			equivalenceClass.set(i);
		}
		return equivalenceClass;
	}

	/**
	 * Return the equivalence class of {@code i} if it not a singleton otherwise {@code null}. 
	 * @param i a number
	 * @return [i] or null if [i] is a singleton
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
}