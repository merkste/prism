package common.methods;

import common.functions.Mapping;
import common.functions.PairMapping;

public interface UnaryMethod<R, S, T> extends PairMapping<R, S, T>
{
	public Mapping<S, T> on(final R instance);

	//	FIXME ALG: J8
	//	default Mapping<S, T> on(final R instance)
	//	{
	//		return curry(instance);
	//	}
}