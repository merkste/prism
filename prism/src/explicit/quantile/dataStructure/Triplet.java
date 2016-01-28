package explicit.quantile.dataStructure;

public class Triplet<T, U, V>
{
	private final T first;
	private final U second;
	private final V third;

	public Triplet(final T t, final U u, final V v)
	{
		first = t;
		second = u;
		third = v;
	}

	public T getFirst(){return first;}
	public U getSecond(){return second;}
	public V getThird(){return third;}
}