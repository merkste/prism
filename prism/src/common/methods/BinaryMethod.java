package common.methods;

import common.functions.PairMapping;
import common.functions.TripleMapping;

public interface BinaryMethod<Q, R, S, T> extends TripleMapping<Q, R, S, T>
{
	default PairMapping<R, S, T> on(final Q instance)
	{
		return this.curry(instance);
	}
}