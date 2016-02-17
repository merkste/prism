package common.iterable;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SingletonIterator<T> implements Iterator<T>
{
	protected T element;

	public SingletonIterator(final T element)
	{
		this.element = element;
	}

	@Override
	public boolean hasNext()
	{
		return element != null;
	}

	@Override
	public T next()
	{
		if (hasNext()) {
			final T result = element;
			element = null;
			return result;
		}
		throw new NoSuchElementException();
	}
}