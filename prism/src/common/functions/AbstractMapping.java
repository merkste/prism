package common.functions;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractMapping<S, T> implements Mapping<S, T>
{
	private static final AbstractMapping<Object, Object> IDENTITY = new AbstractMapping<Object, Object>()
	{
		@Override
		public Object apply(final Object element)
		{
			return element;
		}
	};

	@Override
	public <P> Mapping<P, T> compose(final Mapping<P, ? extends S> mapping)
	{
		return new AbstractMapping<P, T>()
		{
			@Override
			public final T apply(final P element)
			{
				return AbstractMapping.this.apply(mapping.apply(element));
			}
		};
	}

	@Override
	public AbstractMapping<S, T> memoize()
	{
		return new AbstractMappingMemoized<S, T>(this)
		{

			final Map<S, T> lookup = new HashMap<S, T>();

			@Override
			protected T lookup(final S element)
			{
				return lookup.get(element);
			}

			@Override
			protected T store(final S element, final T value)
			{
				lookup.put(element, value);
				return value;
			}
		};
	}

	public static <S, T> Mapping<S, T> constant(final T value)
	{
		return new AbstractMapping<S, T>() {
			@Override
			public T apply(final S element)
			{
				return value;
			}
		};
	}

	@SuppressWarnings("unchecked")
	public static <T> Mapping<T, T> identity()
	{
		return (Mapping<T, T>) IDENTITY;
	}

	public static abstract class AbstractMappingMemoized<S, T> extends AbstractMapping<S, T>
	{
		final Mapping<S, T> mapping;

		public AbstractMappingMemoized(final Mapping<S, T> mapping)
		{
			this.mapping = mapping;
		}

		@Override
		public T apply(final S element)
		{
			T value = lookup(element);
			if (value == null) {
				value = store(element, mapping.apply(element));
			}
			return value;
		}

		@Override
		public AbstractMapping<S, T> memoize()
		{
			return this;
		}

		protected abstract T lookup(final S element);

		protected abstract T store(final S element, T value);
	}
}