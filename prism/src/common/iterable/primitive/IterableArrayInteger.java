package common.iterable.primitive;

import common.iterable.AbstractIterableArray;

public class IterableArrayInteger extends AbstractIterableArray<Integer>implements IterableInteger
{
	private final int[] elements;

	public IterableArrayInteger(final int[] elements)
	{
		this(0, elements.length, elements);
	}

	public IterableArrayInteger(final int fromIndex, final int toIndex, final int... elements)
	{
		super(fromIndex, toIndex);
		this.elements = elements;
	}

	@Override
	public ArrayIteratorInteger iterator()
	{
		return new ArrayIteratorInteger(fromIndex, toIndex, elements);
	}
}