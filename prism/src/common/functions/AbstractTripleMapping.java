package common.functions;

public abstract class AbstractTripleMapping<Q, R, S, T> implements TripleMapping<Q, R, S, T>
{
	@Override
	public PairMapping<R, S, T> curry(final Q element1)
	{
		return new AbstractPairMapping<R, S, T>()
		{
			@Override
			public T apply(final R element2, final S element3)
			{
				return AbstractTripleMapping.this.apply(element1, element2, element3);
			}
		};
	}

	@Override
	public Mapping<S, T> curry(final Q element1, final R element2)
	{
		return new AbstractMapping<S, T>()
		{
			@Override
			public T apply(final S element3)
			{
				return AbstractTripleMapping.this.apply(element1, element2, element3);
			}
		};
	}
}