package common.methods;

import common.functions.Mapping;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
public interface Method<S, T> extends Mapping<S, T>
{
	default T on(final S instance)
	{
		return apply(instance);
	}
}