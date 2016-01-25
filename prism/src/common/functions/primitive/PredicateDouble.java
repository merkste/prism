package common.functions.primitive;

import common.functions.AbstractPredicate;
import common.functions.Mapping;
import common.functions.Predicate;

// Cannot extend DoublePredicate due to negate() signature clash
@FunctionalInterface
public interface PredicateDouble extends Predicate<Double>
{
	public boolean test(double element);

	@Override
	default boolean test(final Double element)
	{
		return test(element.doubleValue());
	}

	@Override
	default <S> Predicate<S> compose(final Mapping<S, ? extends Double> mapping)
	{
		return new AbstractPredicate<S>()
		{
			@Override
			public final boolean test(final S element)
			{
				return PredicateDouble.this.test(mapping.apply(element).doubleValue());
			}
		};
	}

	@Override
	default PredicateDouble negate()
	{
		return new PredicateDouble()
		{
			@Override
			public final boolean test(final double element)
			{
				return !PredicateDouble.this.test(element);
			}

			@Override
			public PredicateDouble negate()
			{
				return PredicateDouble.this;
			}
		};
	}

	default PredicateDouble and(final PredicateDouble predicate)
	{
		return new PredicateDouble()
		{
			@Override
			public final boolean test(final double element)
			{
				return PredicateDouble.this.test(element) && predicate.test(element);
			}
		};
	}

	default PredicateDouble or(final PredicateDouble predicate)
	{
		return new PredicateDouble()
		{
			@Override
			public final boolean test(final double element)
			{
				return PredicateDouble.this.test(element) || predicate.test(element);
			}
		};
	}

	default PredicateDouble implies(final PredicateDouble predicate)
	{
		return new PredicateDouble()
		{
			@Override
			public final boolean test(final double element)
			{
				return (!PredicateDouble.this.test(element)) || predicate.test(element);
			}
		};
	}
}