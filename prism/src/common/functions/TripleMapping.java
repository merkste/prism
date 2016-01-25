package common.functions;

@FunctionalInterface
public interface TripleMapping<Q, R, S, T>
{
	public T apply(Q element1, R element2, S element3);

	default PairMapping<R, S, T> curry(final Q element1)
	{
		return new PairMapping<R, S, T>()
		{
			@Override
			public T apply(final R element2, final S element3)
			{
				return TripleMapping.this.apply(element1, element2, element3);
			}
		};
	}

	default Mapping<S, T> curry(final Q element1, final R element2)
	{
		return new Mapping<S, T>()
		{
			@Override
			public T apply(final S element3)
			{
				return TripleMapping.this.apply(element1, element2, element3);
			}
		};
	}
}