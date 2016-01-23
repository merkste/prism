package common.functions;

public abstract class AbstractPredicate<T> extends AbstractMapping<T, Boolean>implements Predicate<T>
{
	@Override
	public Boolean apply(final T element)
	{
		return test(element);
	}

	@SuppressWarnings("unchecked")
	public static final <T> AbstractPredicate<T> True()
	{
		return (AbstractPredicate<T>) True.TRUE;
	}

	@SuppressWarnings("unchecked")
	public static final <T> AbstractPredicate<T> False()
	{
		return (AbstractPredicate<T>) False.FALSE;
	}

	@Override
	public Predicate<T> not()
	{
		return new AbstractPredicate<T>()
		{
			@Override
			public final boolean test(final T element)
			{
				return !AbstractPredicate.this.test(element);
			}

			@Override
			public AbstractPredicate<T> not()
			{
				return AbstractPredicate.this;
			}
		};
	}

	@Override
	public Predicate<T> and(final Predicate<? super T> predicate)
	{
		return new AbstractPredicate<T>()
		{
			@Override
			public final boolean test(final T element)
			{
				return AbstractPredicate.this.test(element) && predicate.test(element);
			}
		};
	}

	@Override
	public Predicate<T> or(final Predicate<? super T> predicate)
	{
		return new AbstractPredicate<T>()
		{
			@Override
			public final boolean test(final T element)
			{
				return AbstractPredicate.this.test(element) || predicate.test(element);
			}
		};
	}

	@Override
	public Predicate<T> implies(final Predicate<? super T> predicate)
	{
		return new AbstractPredicate<T>()
		{
			@Override
			public final boolean test(final T element)
			{
				return (!AbstractPredicate.this.test(element)) || predicate.test(element);
			}
		};
	}

	public <S> Predicate<S> compose(final Mapping<S, ? extends T> mapping)
	{
		return new AbstractPredicate<S>()
		{
			@Override
			public final boolean test(final S element)
			{
				return AbstractPredicate.this.test(mapping.apply(element));
			}
		};
	}

	public static final class True<T> extends AbstractPredicate<T>
	{
		private static final True<Object> TRUE = new True<>();

		@Override
		public final boolean test(final T element)
		{
			return Boolean.TRUE;
		}
	}

	public static final class False<T> extends AbstractPredicate<T>
	{
		private static final False<Object> FALSE = new False<>();

		@Override
		public final boolean test(final T element)
		{
			return Boolean.FALSE;
		}
	}
}