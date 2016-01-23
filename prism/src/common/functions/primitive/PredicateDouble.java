package common.functions.primitive;

import common.functions.Predicate;

// Cannot extend DoublePredicate due to negate() signature clash
public interface PredicateDouble extends Predicate<Double>
{
	public boolean test(double element);

	@Override
	public PredicateDouble negate();
}