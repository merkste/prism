package common.functions;

public interface TripleMapping<Q, R, S, T>
{
	public T apply(Q element1, R element2, S element3);

	public PairMapping<R, S, T> curry(Q element1);

	public Mapping<S, T> curry(Q element1, R element2);
}