package common.iterable;

public abstract class AbstractIterableArray<T> implements Iterable<T>
{
	protected final int fromIndex;
	protected final int toIndex;

	public AbstractIterableArray(final int fromIndex, final int toIndex)
	{
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
	}
}