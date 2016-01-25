package common.functions.primitive;

import common.functions.PairPredicate;

public interface PairPredicateDoubleDouble extends PairPredicate<Double, Double>
{
	public boolean test(double element1, double element2);

	@Override
	default boolean test(final Double element1, final Double element2)
	{
		return test(element1.doubleValue(), element2.doubleValue());
	}

	@Override
	default PredicateDouble curry(final Double element1)
	{
		return curry(element1.doubleValue());
	}

	default PredicateDouble curry(final double element1)
	{
		return new AbstractPredicateDouble()
		{
			@Override
			public boolean test(final double element2)
			{
				return PairPredicateDoubleDouble.this.test(element1, element2);
			}
		};
	}

	@Override
	default PairPredicateDoubleDouble negate()
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public final boolean test(final double element1, final double element2)
			{
				return !PairPredicateDoubleDouble.this.test(element1, element2);
			}

			@Override
			public PairPredicateDoubleDouble negate()
			{
				return PairPredicateDoubleDouble.this;
			}
		};
	}

	default PairPredicateDoubleDouble and(final PairPredicateDoubleDouble predicate)
	{
		return new PairPredicateDoubleDouble()
		{
			@Override
			public final boolean test(final double element1, final double element2)
			{
				return PairPredicateDoubleDouble.this.test(element1, element2) && predicate.test(element1, element2);
			}
		};
	}

	default PairPredicateDoubleDouble or(final PairPredicateDoubleDouble predicate)
	{
		return new PairPredicateDoubleDouble()
		{
			@Override
			public final boolean test(final double element1, final double element2)
			{
				return PairPredicateDoubleDouble.this.test(element1, element2) || predicate.test(element1, element2);
			}
		};
	}

	default PairPredicateDoubleDouble implies(final PairPredicateDoubleDouble predicate)
	{
		return new PairPredicateDoubleDouble()
		{
			@Override
			public final boolean test(final double element1, final double element2)
			{
				return (!PairPredicateDoubleDouble.this.test(element1, element2)) || predicate.test(element1, element2);
			}
		};
	}

	@Override
	default PairPredicateDoubleDouble inverse()
	{
		return new PairPredicateDoubleDouble()
		{
			@Override
			public boolean test(double element1, double element2)
			{
				return PairPredicateDoubleDouble.this.test(element2, element1);
			}
		};
	}
}