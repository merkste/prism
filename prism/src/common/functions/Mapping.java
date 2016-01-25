package common.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public interface Mapping<S, T> extends Function<S, T>
{
	public T apply(S element);

	default <P> Mapping<P, T> compose(final Mapping<P, ? extends S> mapping)
	{
		return new Mapping<P, T>()
		{
			@Override
			public final T apply(final P element)
			{
				return Mapping.this.apply(mapping.apply(element));
			}
		};
	}

	default Mapping<S, T> memoize()
	{
		return new MemoizedMapping<S, T>(this)
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
		return new Mapping<S, T>() {
			@Override
			public T apply(final S element)
			{
				return value;
			}
		};
	}

	public static final Mapping<Object, Object> IDENTITY = new Mapping<Object, Object>()
	{
		@Override
		public Object apply(final Object element)
		{
			return element;
		}
	};

	@SuppressWarnings("unchecked")
	public static <T> Mapping<T, T> identity()
	{
		return (Mapping<T, T>) IDENTITY;
	}
}