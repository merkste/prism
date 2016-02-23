package common.functions.primitive;

import java.util.Objects;
import java.util.function.Function;

import common.functions.PairMapping;

@FunctionalInterface
public interface PairMappingInt<T> extends PairMapping<Integer, Integer, T>
{
	@Override
	default T apply(Integer element1, Integer element2)
	{
		return apply(element1.intValue(), element2.intValue());
	}

	public T apply(int element1, int element2);

	@Override
	default <V> PairMappingInt<V> andThen(Function<? super T, ? extends V> after)
	{
		Objects.requireNonNull(after);
		return (r, s) -> after.apply(apply(r, s));
	}

	@Override
	default MappingInt<T> curry(Integer element1)
	{
		return curry(element1.intValue());
	}

	default MappingInt<T> curry(int element1)
	{
		return element2 -> apply(element1, element2);
	}

	public static <T> PairMappingInt<T> constant(T value)
	{
		return (element1, element2) -> value;
	}
}