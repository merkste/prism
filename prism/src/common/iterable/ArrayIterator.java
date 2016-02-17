package common.iterable;

import java.util.Iterator;
import java.util.NoSuchElementException;

import common.IteratorTools;

public class ArrayIterator<T> extends AbstractArrayIterator<T>implements Iterator<T>
{
	private final T[] elements;

	@SafeVarargs
	public ArrayIterator(final T... elements)
	{
		this(0, elements.length, elements);
	}

	public ArrayIterator(final int fromIndex, final int toIndex, final T[] elements)
	{
		super(fromIndex, toIndex);
		this.elements = elements;
	}

	@Override
	public T next()
	{
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return elements[next++];
	}

	public static void main(final String[] args)
	{
		IteratorTools.printIterator("empty", new ArrayIterator<Integer>());
		IteratorTools.printIterator("one element", new ArrayIterator<Integer>(1));
		IteratorTools.printIterator("three elements", new ArrayIterator<Integer>(1, 2, 3));
		IteratorTools.printIterator("second of three elements", new ArrayIterator<Integer>(2, 3, new Integer[] { 1, 2, 3 }));
	}
}