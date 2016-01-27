package common.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface Mapping<S, T> extends Function<S, T>
{
	/**
	 *  Overridden to ensure that the return type is Mapping.
	 */
	@Override
	default <P> Mapping<P, T> compose(Function<? super P, ? extends S> function)
	{
		Objects.requireNonNull(function);
		return each -> apply(function.apply(each));
	}

	default Mapping<S, T> memoize()
	{
		return new MemoizedMapping<S, T>(this)
		{
			final Map<S, T> lookup = new HashMap<S, T>();

			@Override
			protected T lookup(S element)
			{
				return lookup.get(element);
			}

			@Override
			protected T store(S element, T value)
			{
				lookup.put(element, value);
				return value;
			}
		};
	}

	public static <S, T> Mapping<S, T> constant(T value)
	{
		return each -> value;
	}
}