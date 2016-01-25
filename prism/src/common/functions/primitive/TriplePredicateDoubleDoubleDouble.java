package common.functions.primitive;

import common.functions.TriplePredicate;

@FunctionalInterface
public interface TriplePredicateDoubleDoubleDouble extends TriplePredicate<Double, Double, Double>
{
	public boolean getBoolean(double element1, double element2, double element3);

	@Override
	default boolean getBoolean(final Double element1, final Double element2, final Double element3)
	{
		return getBoolean(element1.doubleValue(), element2.doubleValue(), element3.doubleValue());
	}

	@Override
	default PairPredicateDoubleDouble curry(final Double element1)
	{
		return curry(element1.doubleValue());
	}

	default PairPredicateDoubleDouble curry(final double element1)
	{
		return new PairPredicateDoubleDouble()
		{
			@Override
			public boolean test(final double element2, final double element3)
			{
				return TriplePredicateDoubleDoubleDouble.this.getBoolean(element1, element2, element3);
			}
		};
	}

	@Override
	default PredicateDouble curry(final Double element1, final Double element2)
	{
		return curry(element1.doubleValue(), element2.doubleValue());
	}

	default PredicateDouble curry(final double element1, final double element2)
	{
		return new PredicateDouble()
		{
			@Override
			public boolean test(final double element3)
			{
				return TriplePredicateDoubleDoubleDouble.this.getBoolean(element1, element2, element3);
			}
		};
	}

	@Override
	default TriplePredicateDoubleDoubleDouble negate()
	{
		return new TriplePredicateDoubleDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double element1, final double element2, final double element3)
			{
				return !TriplePredicateDoubleDoubleDouble.this.getBoolean(element1, element2, element3);
			}

			@Override
			public TriplePredicateDoubleDoubleDouble negate()
			{
				return TriplePredicateDoubleDoubleDouble.this;
			}
		};
	}

	default TriplePredicateDoubleDoubleDouble and(final TriplePredicateDoubleDoubleDouble predicate)
	{
		return new TriplePredicateDoubleDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double element1, final double element2, final double element3)
			{
				return TriplePredicateDoubleDoubleDouble.this.getBoolean(element1, element2, element3)
						&& predicate.getBoolean(element1, element2, element3);
			}
		};
	}

	default TriplePredicateDoubleDoubleDouble or(final TriplePredicateDoubleDoubleDouble predicate)
	{
		return new TriplePredicateDoubleDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double element1, final double element2, final double element3)
			{
				return TriplePredicateDoubleDoubleDouble.this.getBoolean(element1, element2, element3)
						|| predicate.getBoolean(element1, element2, element3);
			}
		};
	}

	default TriplePredicateDoubleDoubleDouble implies(final TriplePredicateDoubleDoubleDouble predicate)
	{
		return new TriplePredicateDoubleDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double element1, final double element2, final double element3)
			{
				return (!TriplePredicateDoubleDoubleDouble.this.getBoolean(element1, element2, element3))
						|| predicate.getBoolean(element1, element2, element3);
			}
		};
	}
}