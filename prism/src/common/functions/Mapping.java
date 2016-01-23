package common.functions;

import java.util.function.Function;

public interface Mapping<S, T> extends Function<S, T>
{
	public T apply(S element);

	public <P> Mapping<P, T> compose(Mapping<P, ? extends S> mapping);

	public Mapping<S, T> memoize();
}