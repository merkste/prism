package common.functions;

public interface TripleMapping<Q, R, S, T>
{
	public T get(final Q element1, final R element2, final S element3);

	public PairMapping<R, S, T> curry(final Q element1);

	public Mapping<S, T> curry(final Q element1, final R element2);
}