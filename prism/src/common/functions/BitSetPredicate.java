package common.functions;

import java.util.BitSet;

import common.functions.primitive.PredicateInt;

public class BitSetPredicate implements PredicateInt
{
	private final BitSet indices;

	public BitSetPredicate(BitSet indices)
	{
		this.indices = indices;
	}

	@Override
	public boolean test(int index)
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