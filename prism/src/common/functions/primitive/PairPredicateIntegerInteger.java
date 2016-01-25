package common.functions.primitive;

import common.functions.PairPredicate;

public interface PairPredicateIntegerInteger extends PairPredicate<Integer, Integer>
{
	public abstract boolean test(final int element1, final int element2);

	@Override
	default boolean test(final Integer element1, final Integer element2)
	{
		return test(element1.intValue(), element2.intValue());
	}

	@Override
	default PredicateInteger curry(final Integer element1)
	{
		return curry(element1.intValue());
	}

	default PredicateInteger curry(final int element1)
	{
		return new PredicateInteger()
		{
			@Override
			public boolean test(final int element2)
			{
				return PairPredicateIntegerInteger.this.test(element1, element2);
			}
		};
	}

	@Override
	default PairPredicateIntegerInteger negate()
	{
		return new PairPredicateIntegerInteger()
		{
			@Override
			public final boolean test(final int element1, final int element2)
			{
				return !PairPredicateIntegerInteger.this.test(element1, element2);
			}

			@Override
			public PairPredicateIntegerInteger negate()
			{
				return PairPredicateIntegerInteger.this;
			}
		};
	}

	default PairPredicateIntegerInteger and(final PairPredicateIntegerInteger predicate)
	{
		return new PairPredicateIntegerInteger()
		{
			@Override
			public final boolean test(final int element1, final int element2)
			{
				return PairPredicateIntegerInteger.this.test(element1, element2) && predicate.test(element1, element2);
			}
		};
	}

	default PairPredicateIntegerInteger or(final PairPredicateIntegerInteger predicate)
	{
		return new PairPredicateIntegerInteger()
		{
			@Override
			public final boolean test(final int element1, final int element2)
			{
				return PairPredicateIntegerInteger.this.test(element1, element2) || predicate.test(element1, element2);
			}
		};
	}

	default PairPredicateIntegerInteger implies(final PairPredicateIntegerInteger predicate)
	{
		return new PairPredicateIntegerInteger()
		{
			@Override
			public final boolean test(final int element1, final int element2)
			{
				return (!PairPredicateIntegerInteger.this.test(element1, element2)) || predicate.test(element1, element2);
			}
		};
	}
}