package common.functions;

public interface PairMapping<R, S, T>
{
	public T get(final R element1, final S element2);

	public Mapping<S, T> curry(final R element1);
}