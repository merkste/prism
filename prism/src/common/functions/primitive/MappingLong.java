package common.functions.primitive;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;

import common.functions.Mapping;
import common.functions.PairMapping;

@FunctionalInterface
public interface MappingLong<T> extends Mapping<Long, T>, LongFunction<T>
{
	@Override
	default T apply(Long element)
	{
		return apply(element.longValue());
	}

	default <P> Mapping<P, T> compose(ToLongFunction<? super P> function)
	{
		Objects.requireNonNull(function);
		return element -> apply(function.applyAsLong(element));
	}

	default <P, Q> PairMapping<P, Q, T> compose(ToLongBiFunction<? super P, ? super Q> function)
	{
		Objects.requireNonNull(function);
		return (element1, element2) -> apply(function.applyAsLong(element1, element2));
	}

	@Override
	default <V> MappingLong<V> andThen(Function<? super T, ? extends V> after)
	{
		Objects.requireNonNull(after);
		return i -> after.apply(apply(i));
	}

	default LongToIntFunction andThen(ToIntFunction<? super T> after)
	{
		return i -> after.applyAsInt(apply(i));
	}

	default LongUnaryOperator andThen(ToLongFunction<? super T> after)
	{
		return i -> after.applyAsLong(apply(i));
	}

	default LongToDoubleFunction andThen(ToDoubleFunction<? super T> after)
	{
		return i -> after.applyAsDouble(apply(i));
	}

	public static <T> MappingLong<T> constant(T value)
	{
		return element -> value;
	}
}