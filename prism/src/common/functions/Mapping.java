package common.functions;

public interface Mapping<S, T>
{
	public T get(S element);

	public <P> Mapping<P, T> compose(final Mapping<P, ? extends S> mapping);

	public Mapping<S, T> memoize();
}