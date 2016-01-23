package common.functions.primitive;

import common.functions.Predicate;

//Cannot extend IntPredicate due to negate() signature clash
public interface PredicateInteger extends Predicate<Integer>
{
	public boolean test(int element);

	@Override
	public PredicateInteger not();
}