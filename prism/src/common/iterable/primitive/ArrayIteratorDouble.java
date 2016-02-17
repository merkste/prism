package common.iterable.primitive;

import java.util.NoSuchElementException;

import common.iterable.AbstractArrayIterator;

public class ArrayIteratorDouble extends AbstractArrayIterator<Double>implements IteratorDouble
{
	private final double[] elements;

	public ArrayIteratorDouble(final int fromIndex, final int toIndex, final double... elements)
	{
		super(fromIndex, toIndex);
		this.elements = elements;
	}

	public Double next()
	{
		return nextDouble();
	}

	public double nextDouble()
	{
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return elements[next++];
	}
}