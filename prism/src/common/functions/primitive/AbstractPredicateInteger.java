package common.functions.primitive;

import common.functions.AbstractPredicate;
import common.functions.Mapping;
import common.functions.Predicate;

public abstract class AbstractPredicateInteger extends AbstractPredicate<Integer>implements PredicateInteger
{
	public boolean getBoolean(final Integer element)
	{
		return getBoolean(element.intValue());
	}

	public abstract boolean getBoolean(final int element);

	@Override
	public PredicateInteger not()
	{
		return new AbstractPredicateInteger()
		{
			@Override
			public final boolean getBoolean(final int element)
			{
				return !AbstractPredicateInteger.this.getBoolean(element);
			}

			@Override
			public PredicateInteger not()
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
			public final boolean getBoolean(final int element)
			{
				return AbstractPredicateInteger.this.getBoolean(element) && predicate.getBoolean(element);
			}
		};
	}

	public PredicateInteger or(final PredicateInteger predicate)
	{
		return new AbstractPredicateInteger()
		{
			@Override
			public final boolean getBoolean(final int element)
			{
				return AbstractPredicateInteger.this.getBoolean(element) || predicate.getBoolean(element);
			}
		};
	}

	public PredicateInteger implies(final PredicateInteger predicate)
	{
		return new AbstractPredicateInteger()
		{
			@Override
			public final boolean getBoolean(final int element)
			{
				return (!AbstractPredicateInteger.this.getBoolean(element)) || predicate.getBoolean(element);
			}
		};
	}

	public <S> Predicate<S> compose(final Mapping<S, ? extends Integer> mapping)
	{
		return new AbstractPredicate<S>()
		{
			@Override
			public final boolean getBoolean(final S element)
			{
				return AbstractPredicateInteger.this.getBoolean(mapping.get(element).intValue());
			}
		};
	}
}