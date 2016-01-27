package common.functions;

@FunctionalInterface
public interface TripleMapping<Q, R, S, T>
{
	public T apply(Q element1, R element2, S element3);

	default PairMapping<R, S, T> curry(Q element1)
	{
		return (element2, element3) -> apply(element1, element2, element3);
	}

	default Mapping<S, T> curry(Q element1, R element2)
	{
		return element3 -> apply(element1, element2, element3);
	}

	public static <Q, R, S, T> TripleMapping<Q, R, S, T> constant(T value)
	{
		return (element1, element2, element3) -> value;
	}
}