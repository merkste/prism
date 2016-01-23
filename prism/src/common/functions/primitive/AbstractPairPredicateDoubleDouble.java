package common.functions.primitive;

import common.functions.AbstractPairPredicate;

public abstract class AbstractPairPredicateDoubleDouble extends AbstractPairPredicate<Double, Double>implements PairPredicateDoubleDouble
{
	@Override
	public boolean test(final Double element1, final Double element2)
	{
		return test(element1.doubleValue(), element2.doubleValue());
	}

	public abstract boolean test(final double element1, final double element2);

	@Override
	public PredicateDouble curry(final Double element1)
	{
		return curry(element1.doubleValue());
	}

	@Override
	public PredicateDouble curry(final double element1)
	{
		return new AbstractPredicateDouble()
		{
			@Override
			public boolean test(final double element2)
			{
				return AbstractPairPredicateDoubleDouble.this.test(element1, element2);
			}
		};
	}

	@Override
	public PairPredicateDoubleDouble negate()
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public final boolean test(final double element1, final double element2)
			{
				return !AbstractPairPredicateDoubleDouble.this.test(element1, element2);
			}

			@Override
			public AbstractPairPredicateDoubleDouble negate()
			{
				return AbstractPairPredicateDoubleDouble.this;
			}
		};
	}

	@Override
	public PairPredicateDoubleDouble and(final PairPredicateDoubleDouble predicate)
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public final boolean test(final double element1, final double element2)
			{
				return AbstractPairPredicateDoubleDouble.this.test(element1, element2) && predicate.test(element1, element2);
			}
		};
	}

	@Override
	public PairPredicateDoubleDouble or(final PairPredicateDoubleDouble predicate)
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public final boolean test(final double element1, final double element2)
			{
				return AbstractPairPredicateDoubleDouble.this.test(element1, element2) || predicate.test(element1, element2);
			}
		};
	}

	@Override
	public PairPredicateDoubleDouble implies(final PairPredicateDoubleDouble predicate)
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public final boolean test(final double element1, final double element2)
			{
				return (!AbstractPairPredicateDoubleDouble.this.test(element1, element2)) || predicate.test(element1, element2);
			}
		};
	}

	@Override
	public PairPredicateDoubleDouble inverse()
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public boolean test(double element1, double element2)
			{
				return AbstractPairPredicateDoubleDouble.this.test(element2, element1);
			}
		};
	}
}