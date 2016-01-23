package common.functions.primitive;

import common.functions.AbstractTriplePredicate;

public abstract class AbstractTriplePredicateDoubleDoubleDouble extends AbstractTriplePredicate<Double, Double, Double>
		implements TriplePredicateDoubleDoubleDouble
{
	@Override
	public boolean getBoolean(final Double element1, final Double element2, final Double element3)
	{
		return getBoolean(element1.doubleValue(), element2.doubleValue(), element3.doubleValue());
	}

	public abstract boolean getBoolean(final double element1, final double element2, final double element3);

	@Override
	public PairPredicateDoubleDouble curry(final Double element1)
	{
		return curry(element1.doubleValue());
	}

	@Override
	public PairPredicateDoubleDouble curry(final double element1)
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public boolean test(final double element2, final double element3)
			{
				return AbstractTriplePredicateDoubleDoubleDouble.this.getBoolean(element1, element2, element3);
			}
		};
	}

	@Override
	public PredicateDouble curry(final Double element1, final Double element2)
	{
		return curry(element1.doubleValue(), element2.doubleValue());
	}

	@Override
	public PredicateDouble curry(final double element1, final double element2)
	{
		return new AbstractPredicateDouble()
		{
			@Override
			public boolean test(final double element3)
			{
				return AbstractTriplePredicateDoubleDoubleDouble.this.getBoolean(element1, element2, element3);
			}
		};
	}

	@Override
	public TriplePredicateDoubleDoubleDouble not()
	{
		return new AbstractTriplePredicateDoubleDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double element1, final double element2, final double element3)
			{
				return !AbstractTriplePredicateDoubleDoubleDouble.this.getBoolean(element1, element2, element3);
			}

			@Override
			public TriplePredicateDoubleDoubleDouble not()
			{
				return AbstractTriplePredicateDoubleDoubleDouble.this;
			}
		};
	}

	public TriplePredicateDoubleDoubleDouble and(final TriplePredicateDoubleDoubleDouble predicate)
	{
		return new AbstractTriplePredicateDoubleDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double element1, final double element2, final double element3)
			{
				return AbstractTriplePredicateDoubleDoubleDouble.this.getBoolean(element1, element2, element3)
						&& predicate.getBoolean(element1, element2, element3);
			}
		};
	}

	public TriplePredicateDoubleDoubleDouble or(final TriplePredicateDoubleDoubleDouble predicate)
	{
		return new AbstractTriplePredicateDoubleDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double element1, final double element2, final double element3)
			{
				return AbstractTriplePredicateDoubleDoubleDouble.this.getBoolean(element1, element2, element3)
						|| predicate.getBoolean(element1, element2, element3);
			}
		};
	}

	public TriplePredicateDoubleDoubleDouble implies(final TriplePredicateDoubleDoubleDouble predicate)
	{
		return new AbstractTriplePredicateDoubleDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double element1, final double element2, final double element3)
			{
				return (!AbstractTriplePredicateDoubleDoubleDouble.this.getBoolean(element1, element2, element3))
						|| predicate.getBoolean(element1, element2, element3);
			}
		};
	}
}