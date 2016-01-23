package common.functions.primitive;

import common.functions.AbstractPredicate;
import common.functions.Mapping;
import common.functions.Predicate;

public abstract class AbstractPredicateInteger extends AbstractPredicate<Integer> implements PredicateInteger
{
	public boolean test(final Integer element)
	{
		return test(element.intValue());
	}

	public abstract boolean test(final int element);

	@Override
	public PredicateInteger negate()
	{
		return new AbstractPredicateInteger()
		{
			@Override
			public final boolean test(final int element)
			{
				return !AbstractPredicateInteger.this.test(element);
			}

			@Override
			public PredicateInteger negate()
			{
				return AbstractPredicateInteger.this;
			}
		};
	}

	public PredicateInteger and(final PredicateInteger predicate)
	{
		return new AbstractPredicateInteger()
		{
			@Override
			public final boolean test(final int element)
			{
				return AbstractPredicateInteger.this.test(element) && predicate.test(element);
			}
		};
	}

	public PredicateInteger or(final PredicateInteger predicate)
	{
		return new AbstractPredicateInteger()
		{
			@Override
			public final boolean test(final int element)
			{
				return AbstractPredicateInteger.this.test(element) || predicate.test(element);
			}
		};
	}

	public PredicateInteger implies(final PredicateInteger predicate)
	{
		return new AbstractPredicateInteger()
		{
			@Override
			public final boolean test(final int element)
			{
				return (!AbstractPredicateInteger.this.test(element)) || predicate.test(element);
			}
		};
	}

	public <S> Predicate<S> compose(final Mapping<S, ? extends Integer> mapping)
	{
		return new AbstractPredicate<S>()
		{
			@Override
			public final boolean test(final S element)
			{
				return AbstractPredicateInteger.this.test(mapping.apply(element).intValue());
			}
		};
	}
}