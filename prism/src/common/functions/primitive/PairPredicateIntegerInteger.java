package common.functions.primitive;

import common.functions.PairPredicate;

public interface PairPredicateIntegerInteger extends PairPredicate<Integer, Integer>
{
	public boolean getBoolean(final int element1, final int element2);

	@Override
	public PairPredicateIntegerInteger not();
}