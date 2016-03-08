package common.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;

import common.functions.primitive.MappingDouble;
import common.functions.primitive.MappingInt;
import common.functions.primitive.MappingLong;

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
		return element -> apply(function.apply(element));
	}

	default <P> MappingInt<T> compose(IntFunction<? extends S> function)
	{
		Objects.requireNonNull(function);
		return element -> apply(function.apply(element));
	}

	default <P> MappingLong<T> compose(LongFunction<? extends S> function)
	{
		Objects.requireNonNull(function);
		return element -> apply(function.apply(element));
	}

	default <P> MappingDouble<T> compose(DoubleFunction<? extends S> function)
	{
		Objects.requireNonNull(function);
		return element -> apply(function.apply(element));
	}

	default <P, Q> PairMapping<P, Q, T> compose(BiFunction<? super P, ? super Q, ? extends S> function)
	{
		Objects.requireNonNull(function);
		return (element1, element2) -> apply(function.apply(element1, element2));
	}

	default <P, Q, R> TripleMapping<P, Q, R, T> compose(TripleMapping<? super P, ? super Q, ? super R, ? extends S> function)
	{
		Objects.requireNonNull(function);
		return (element1, element2, element3) -> apply(function.apply(element1, element2, element3));
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
		return element -> value;
	}
}