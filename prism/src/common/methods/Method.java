package common.methods;

import common.functions.Mapping;

public interface Method<S, T> extends Mapping<S, T>
{
	default T on(final S instance)
	{
		return get(instance);
	}
}