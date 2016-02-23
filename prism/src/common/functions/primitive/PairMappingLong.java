package common.functions.primitive;

import java.util.Objects;
import java.util.function.Function;

import common.functions.PairMapping;

@FunctionalInterface
public interface PairMappingLong<T> extends PairMapping<Long, Long, T>
{
	@Override
	default T apply(Long element1, Long element2)
	{
		return apply(element1.longValue(), element2.longValue());
	}

	public T apply(long element1, long element2);

	@Override
	default <V> PairMappingLong<V> andThen(Function<? super T, ? extends V> after)
	{
		Objects.requireNonNull(after);
		return (r, s) -> after.apply(apply(r, s));
	}

	@Override
	default MappingLong<T> curry(Long element1)
	{
		return curry(element1.longValue());
	}

	default MappingLong<T> curry(long element1)
	{
		return element2 -> apply(element1, element2);
	}

	public static <T> PairMappingLong<T> constant(T value)
	{
		return (element1, element2) -> value;
	}
}