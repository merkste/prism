package common.functions;

import java.util.function.BiFunction;

public interface PairMapping<R, S, T> extends BiFunction<R, S, T>
{
	public T apply(R element1, S element2);

	default Mapping<S, T> curry(final R element1)
	{
		return new Mapping<S, T>()
		{
			@Override
			public T apply(final S element2)
			{
				return PairMapping.this.apply(element1, element2);
			}
		};
	}

	public static <R, S, T> PairMapping<R, S, T> constant(final T value)
	{
		return new PairMapping<R, S, T>() {
			@Override
			public T apply(final R element1, final S element2)
			{
				return value;
			}
		};
	}
}