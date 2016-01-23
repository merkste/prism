package common.functions;

public interface PairMapping<R, S, T>
{
	public T apply(R element1, S element2);

	public Mapping<S, T> curry(R element1);
}