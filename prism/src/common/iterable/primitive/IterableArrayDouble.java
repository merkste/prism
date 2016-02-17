package common.iterable.primitive;

import common.iterable.AbstractIterableArray;

public class IterableArrayDouble extends AbstractIterableArray<Double>implements IterableDouble
{
	private final double[] elements;

	public IterableArrayDouble(final int fromIndex, final int toIndex, final double... elements)
	{
		super(fromIndex, toIndex);
		this.elements = elements;
	}

	@Override
	public ArrayIteratorDouble iterator()
	{
		return new ArrayIteratorDouble(fromIndex, toIndex, elements);
	}
}