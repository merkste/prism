package common.functions.primitive;

import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import common.functions.Mapping;
import common.functions.PairMapping;

@FunctionalInterface
public interface MappingInt<T> extends Mapping<Integer, T>, IntFunction<T>
{
	@Override
	default T apply(Integer element)
	{
		return apply(element.intValue());
	}

	default <P> Mapping<P, T> compose(ToIntFunction<? super P> function)
	{
		Objects.requireNonNull(function);
		return element -> apply(function.applyAsInt(element));
	}

	default <P, Q> PairMapping<P, Q, T> compose(ToIntBiFunction<? super P, ? super Q> function)
	{
		Objects.requireNonNull(function);
		return (element1, element2) -> apply(function.applyAsInt(element1, element2));
	}

	public static <T> MappingInt<T> constant(T value)
	{
		return element -> value;
	}
}