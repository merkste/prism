package common.functions;

/**
 * @deprecated
 * Use PairMapping or BiFunction interface directly.
 */
@Deprecated
public abstract class AbstractPairMapping<R, S, T> implements PairMapping<R, S, T>
{
	public static <R, S, T> PairMapping<R, S, T> constant(final T value)
	{
		return PairMapping.constant(value);
	}
}