package common.iterable;

import java.util.Iterator;

public class IterableArray<T> extends AbstractIterableArray<T>implements Iterable<T>
{
	private final T[] elements;

	@SafeVarargs
	public IterableArray(final T... elements)
	{
		this(0, elements.length, elements);
	}

	@SafeVarargs
	public IterableArray(final int fromIndex, final int toIndex, final T... elements)
	{
		super(fromIndex, toIndex);
		this.elements = elements;
	}

	@Override
	public Iterator<T> iterator()
	{
		return new ArrayIterator<T>(fromIndex, toIndex, elements);
	}
}