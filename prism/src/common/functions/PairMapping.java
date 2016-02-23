package common.functions;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface PairMapping<R, S, T> extends BiFunction<R, S, T>
{
	@Override
	default <V> PairMapping<R, S, V> andThen(Function<? super T, ? extends V> after)
	{
		Objects.requireNonNull(after);
		return (R r, S s) -> after.apply(apply(r, s));
	}

	default Mapping<S, T> curry(R element1)
	{
		return element2 -> apply(element1, element2);
	}

	public static <R, S, T> PairMapping<R, S, T> constant(T value)
	{
		return (element1, element2) -> value;
	}
}