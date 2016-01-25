package common.functions;

@FunctionalInterface
public interface Predicate<T> extends Mapping<T, Boolean>, java.util.function.Predicate<T>
{
	public boolean test(T element);

	@Override
	default Boolean apply(final T element)
	{
		return test(element);
	}

	@Override
	default <S> Predicate<S> compose(final Mapping<S, ? extends T> mapping)
	{
		return new Predicate<S>()
		{
			@Override
			public final boolean test(final S element)
			{
				return Predicate.this.test(mapping.apply(element));
			}
		};
	}

	@Override
	default Predicate<T> negate()
	{
		return new Predicate<T>()
		{
			@Override
			public final boolean test(final T element)
			{
				return !Predicate.this.test(element);
			}

			@Override
			public Predicate<T> negate()
			{
				return Predicate.this;
			}
		};
	}

	@Override
	default Predicate<T> and(java.util.function.Predicate<? super T> predicate)
	{
		return new Predicate<T>()
		{
			@Override
			public final boolean test(final T element)
			{
				return Predicate.this.test(element) && predicate.test(element);
			}
		};
	}

	@Override
	default Predicate<T> or(java.util.function.Predicate<? super T> predicate)
	{
		return new Predicate<T>()
		{
			@Override
			public final boolean test(final T element)
			{
				return Predicate.this.test(element) || predicate.test(element);
			}
		};
	}

	default Predicate<T> implies(java.util.function.Predicate<? super T> predicate)
	{
		return new Predicate<T>()
		{
			@Override
			public final boolean test(final T element)
			{
				return (!Predicate.this.test(element)) || predicate.test(element);
			}
		};
	}

	@SuppressWarnings("unchecked")
	public static <T> Predicate<T> True()
	{
		return (Predicate<T>) True.TRUE;
	}

	@SuppressWarnings("unchecked")
	public static <T> Predicate<T> False()
	{
		return (Predicate<T>) False.FALSE;
	}

	public static final class True<T> implements Predicate<T>
	{
		private static final True<Object> TRUE = new True<>();

		@Override
		public final boolean test(final T element)
		{
			return Boolean.TRUE;
		}
	}

	public static final class False<T> implements Predicate<T>
	{
		private static final False<Object> FALSE = new False<>();

		@Override
		public final boolean test(final T element)
		{
			return Boolean.FALSE;
		}
	}
}