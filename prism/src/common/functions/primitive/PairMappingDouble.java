package common.functions.primitive;

import java.util.Objects;
import java.util.function.Function;

import common.functions.PairMapping;

@FunctionalInterface
public interface PairMappingDouble<T> extends PairMapping<Double, Double, T>
{
	@Override
	default T apply(Double element1, Double element2)
	{
		return apply(element1.doubleValue(), element2.doubleValue());
	}

	public T apply(double element1, double element2);

	@Override
	default <V> PairMappingDouble<V> andThen(Function<? super T, ? extends V> after)
	{
		Objects.requireNonNull(after);
		return (r, s) -> after.apply(apply(r, s));
	}

	@Override
	default MappingDouble<T> curry(Double element1)
	{
		return curry(element1.doubleValue());
	}

	default MappingDouble<T> curry(double element1)
	{
		return element2 -> apply(element1, element2);
	}

	public static <T> PairMappingDouble<T> constant(T value)
	{
		return (element1, element2) -> value;
	}
}