package common.functions;

import java.util.function.BiFunction;

@FunctionalInterface
public interface PairMapping<R, S, T> extends BiFunction<R, S, T>
{
	default Mapping<S, T> curry(R element1)
	{
		return element2 -> apply(element1, element2);
	}

	public static <R, S, T> PairMapping<R, S, T> constant(T value)
	{
		return (element1, element2) -> value;
	}
}