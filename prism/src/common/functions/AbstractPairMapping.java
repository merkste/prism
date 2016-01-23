package common.functions;

public abstract class AbstractPairMapping<R, S, T> implements PairMapping<R, S, T>
{
	@Override
	public Mapping<S, T> curry(final R element1)
	{
		return new AbstractMapping<S, T>()
		{
			@Override
			public T apply(final S element2)
			{
				return AbstractPairMapping.this.get(element1, element2);
			}
		};
	}

	public static <R, S, T> PairMapping<R, S, T> constant(final T value)
	{
		return new AbstractPairMapping<R, S, T>() {
			@Override
			public T get(final R element1, final S element2)
			{
				return value;
			}
		};
	}
}