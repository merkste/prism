package common.functions;

public abstract class AbstractPairMapping<R, S, T> implements PairMapping<R, S, T>
{
	@Override
	public Mapping<S, T> curry(final R element1)
	{
		return new AbstractMapping<S, T>()
		{
			@Override
			public T get(final S element2)
			{
				return AbstractPairMapping.this.get(element1, element2);
			}
		};
	}
}