package common.functions.primitive;

import common.functions.PairPredicate;

public interface PairPredicateIntegerInteger extends PairPredicate<Integer, Integer>
{
	public boolean test(int element1, int element2);

	@Override
	public PairPredicateIntegerInteger negate();
}