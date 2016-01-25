package common.functions;

import java.util.BitSet;

import common.functions.primitive.PredicateInteger;

public class BitSetPredicate implements PredicateInteger
{
	private final BitSet indices;

	public BitSetPredicate(final BitSet indices)
	{
		this.indices = indices;
	}

	@Override
	public boolean test(final int index)
	{
		return indices.get(index);
	}

	@Override
	public BitSetPredicate memoize()
	{
		// a BitSet predicate is efficiently memoized by its backing BitSet
		return this;
	}
}