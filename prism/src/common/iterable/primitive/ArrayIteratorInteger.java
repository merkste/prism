package common.iterable.primitive;

import java.util.NoSuchElementException;

import common.iterable.AbstractArrayIterator;

public class ArrayIteratorInteger extends AbstractArrayIterator<Integer>implements IteratorInteger
{
	private final int[] elements;

	public ArrayIteratorInteger(final int... elements)
	{
		this(0, elements.length, elements);
	}

	public ArrayIteratorInteger(final int fromIndex, final int toIndex, final int... elements)
	{
		super(fromIndex, toIndex);
		this.elements = elements;
	}

	@Override
	public Integer next()
	{
		return nextInteger();
	}

	@Override
	public int nextInteger()
	{
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return elements[next++];
	}
}