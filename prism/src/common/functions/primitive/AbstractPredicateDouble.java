package common.functions.primitive;

import common.functions.AbstractPredicate;
import common.functions.Mapping;
import common.functions.Predicate;

public abstract class AbstractPredicateDouble extends AbstractPredicate<Double>implements PredicateDouble
{
	@Override
	public boolean test(final Double element)
	{
		return test(element.doubleValue());
	}

	public abstract boolean test(final double element);

	@Override
	public PredicateDouble not()
	{
		return new AbstractPredicateDouble()
		{
			@Override
			public final boolean test(final double element)
			{
				return !AbstractPredicateDouble.this.test(element);
			}

			@Override
			public AbstractPredicateDouble not()
			{
				return AbstractPredicateDouble.this;
			}
		};
	}

	public PredicateDouble and(final PredicateDouble predicate)
	{
		return new AbstractPredicateDouble()
		{
			@Override
			public final boolean test(final double element)
			{
				return AbstractPredicateDouble.this.test(element) && predicate.test(element);
			}
		};
	}

	public PredicateDouble or(final PredicateDouble predicate)
	{
		return new AbstractPredicateDouble()
		{
			@Override
			public final boolean test(final double element)
			{
				return AbstractPredicateDouble.this.test(element) || predicate.test(element);
			}
		};
	}

	public PredicateDouble implies(final PredicateDouble predicate)
	{
		return new AbstractPredicateDouble()
		{
			@Override
			public final boolean test(final double element)
			{
				return (!AbstractPredicateDouble.this.test(element)) || predicate.test(element);
			}
		};
	}

	public <S> Predicate<S> compose(final Mapping<S, ? extends Double> mapping)
	{
		return new AbstractPredicate<S>()
		{
			@Override
			public final boolean test(final S element)
			{
				return AbstractPredicateDouble.this.test(mapping.apply(element).doubleValue());
			}
		};
	}
}