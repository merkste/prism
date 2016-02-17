package common.functions.primitive;

import common.functions.Predicate;

public interface PredicateDouble extends Predicate<Double>
{
	public boolean getBoolean(final double element);

	@Override
	public PredicateDouble not();
}