package common.functions;

import java.util.BitSet;

import common.functions.primitive.AbstractPredicateInteger;

public class BitSetPredicate extends AbstractPredicateInteger
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
		// an BitSet predicate is efficiently memoized by its backing BitSet
		return this;
	}
}