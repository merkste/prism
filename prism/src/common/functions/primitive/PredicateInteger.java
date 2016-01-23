package common.functions.primitive;

import common.functions.Predicate;

public interface PredicateInteger extends Predicate<Integer>
{
	public boolean test(int element);

	@Override
	public PredicateInteger not();
}