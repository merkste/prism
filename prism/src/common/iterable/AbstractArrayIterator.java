package common.iterable;

import java.util.Iterator;

/**
 * @deprecated
 * Use J8: Arrays::stream
 */
@Deprecated
public abstract class AbstractArrayIterator<T> implements Iterator<T>
{
	protected final Iterator<T> iterator;

	public AbstractArrayIterator(final Iterator<T> iterator)
	{
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext()
	{
		return iterator.hasNext();
	}

	@Override
	public T next()
	{
		return iterator.next();
	}
}