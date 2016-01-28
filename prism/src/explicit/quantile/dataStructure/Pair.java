package explicit.quantile.dataStructure;

public class Pair<T, U>
{
	private final T first;
	private final U second;

	public Pair(final T t, final U u)
	{
		first = t;
		second = u;
	}

	public T getFirst(){return first;}
	public U getSecond(){return second;}
}