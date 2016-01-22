package common.iterable;

import java.util.Arrays;
import java.util.Iterator;

import common.IteratorTools;

/**
 * @deprecated
 * Use J8: Arrays::stream
 */
@Deprecated
public class ArrayIterator<T> extends AbstractArrayIterator<T> implements Iterator<T>
{
	@SafeVarargs
	public ArrayIterator(final T... elements)
	{
		this(0, elements.length, elements);
	}

	public ArrayIterator(final int fromIndex, final int toIndex, final T[] elements)
	{
		super(Arrays.stream(elements, fromIndex, toIndex).iterator());
	}

	public static void main(final String[] args)
	{
		IteratorTools.printIterator("empty", new ArrayIterator<Integer>());
		IteratorTools.printIterator("one element", new ArrayIterator<Integer>(1));
		IteratorTools.printIterator("three elements", new ArrayIterator<Integer>(1, 2, 3));
		IteratorTools.printIterator("second of three elements", new ArrayIterator<Integer>(1, 2, new Integer[] { 1, 2, 3 }));
	}
}