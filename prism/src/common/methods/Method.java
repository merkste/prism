package common.methods;

import common.functions.Mapping;

public interface Method<S, T> extends Mapping<S, T>
{
	public T on(final S instance);

	// FIXME ALG: J8
	//	default T on(final S instance)
	//	{
	//		return get(instance);
	//	}
}