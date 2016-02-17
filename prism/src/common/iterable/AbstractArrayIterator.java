package common.iterable;

import java.util.Iterator;

public abstract class AbstractArrayIterator<T> implements Iterator<T>
{
	protected int toIndex;
	protected int next;

	public AbstractArrayIterator(final int fromIndex, final int toIndex)
	{
		this.toIndex = toIndex;
		this.next = fromIndex;
	}

	@Override
	public boolean hasNext()
	{
		return next < toIndex;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("remove");
	}
}