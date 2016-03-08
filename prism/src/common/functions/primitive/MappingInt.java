package common.functions.primitive;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

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

	@Override
	default <V> MappingInt<V> andThen(Function<? super T, ? extends V> after)
	{
		Objects.requireNonNull(after);
		return i -> after.apply(apply(i));
	}

	default IntUnaryOperator andThen(ToIntFunction<? super T> after)
	{
		return i -> after.applyAsInt(apply(i));
	}

	default IntToLongFunction andThen(ToLongFunction<? super T> after)
	{
		return i -> after.applyAsLong(apply(i));
	}

	default IntToDoubleFunction andThen(ToDoubleFunction<? super T> after)
	{
		return i -> after.applyAsDouble(apply(i));
	}

	public static <T> MappingInt<T> constant(T value)
	{
		return element -> value;
	}
}