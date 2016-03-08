package common.functions.primitive;

import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import common.functions.Mapping;
import common.functions.PairMapping;

@FunctionalInterface
public interface MappingDouble<T> extends Mapping<Double, T>, DoubleFunction<T>
{
	@Override
	default T apply(Double element)
	{
		return apply(element.doubleValue());
	}

	default <P> Mapping<P, T> compose(ToDoubleFunction<? super P> function)
	{
		Objects.requireNonNull(function);
		return element -> apply(function.applyAsDouble(element));
	}

	default <P, Q> PairMapping<P, Q, T> compose(ToDoubleBiFunction<? super P, ? super Q> function)
	{
		Objects.requireNonNull(function);
		return (element1, element2) -> apply(function.applyAsDouble(element1, element2));
	}

	@Override
	default <V> MappingDouble<V> andThen(Function<? super T, ? extends V> after)
	{
		Objects.requireNonNull(after);
		return i -> after.apply(apply(i));
	}

	default DoubleToIntFunction andThen(ToIntFunction<? super T> after)
	{
		return i -> after.applyAsInt(apply(i));
	}

	default DoubleToLongFunction andThen(ToLongFunction<? super T> after)
	{
		return i -> after.applyAsLong(apply(i));
	}

	default DoubleUnaryOperator andThen(ToDoubleFunction<? super T> after)
	{
		return i -> after.applyAsDouble(apply(i));
	}

	public static <T> MappingDouble<T> constant(T value)
	{
		return element -> value;
	}
}