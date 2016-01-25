package common.iterable.primitive;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import common.iterable.AbstractIterableArray;

/**
 * @deprecated
 * Use J8: Arrays::stream
 */
@Deprecated
public class IterableArrayDouble extends AbstractIterableArray<Double> implements IterableDouble
{
	protected final double[] elements;

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

	@Override
	public Stream<Double> stream()
	{
		return primitiveStream().boxed();
	}

	protected DoubleStream primitiveStream()
	{
		return Arrays.stream(elements, fromIndex, toIndex);
	}
}