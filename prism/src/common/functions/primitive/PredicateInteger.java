package common.functions.primitive;

import java.util.function.Function;

import common.functions.Predicate;

//Cannot extend IntPredicate due to negate() signature clash
@FunctionalInterface
public interface PredicateInteger extends Predicate<Integer>
{
	public boolean test(int element);

	default boolean test(final Integer element)
	{
		return test(element.intValue());
	}

	@Override
	default <S> Predicate<S> compose(final Function<? super S, ? extends Integer> mapping)
	{
		return new Predicate<S>()
		{
			@Override
			public final boolean test(final S element)
			{
				return PredicateInteger.this.test(mapping.apply(element).intValue());
			}
		};
	}

	@Override
	default PredicateInteger negate()
	{
		return new PredicateInteger()
		{
			@Override
			public final boolean test(final int element)
			{
				return !PredicateInteger.this.test(element);
			}

			@Override
			public PredicateInteger negate()
			{
				return PredicateInteger.this;
			}
		};
	}

	default PredicateInteger and(final PredicateInteger predicate)
	{
		return new PredicateInteger()
		{
			@Override
			public final boolean test(final int element)
			{
				return PredicateInteger.this.test(element) && predicate.test(element);
			}
		};
	}

	default PredicateInteger or(final PredicateInteger predicate)
	{
		return new PredicateInteger()
		{
			@Override
			public final boolean test(final int element)
			{
				return PredicateInteger.this.test(element) || predicate.test(element);
			}
		};
	}

	default PredicateInteger implies(final PredicateInteger predicate)
	{
		return new PredicateInteger()
		{
			@Override
			public final boolean test(final int element)
			{
				return (!PredicateInteger.this.test(element)) || predicate.test(element);
			}
		};
	}
}