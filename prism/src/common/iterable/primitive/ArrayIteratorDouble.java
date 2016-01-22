package common.iterable.primitive;

import java.util.Arrays;
import java.util.PrimitiveIterator;

import common.IteratorTools;
import common.iterable.AbstractArrayIterator;

/**
 * @deprecated
 * Use J8: Arrays::stream
 */
@Deprecated
public class ArrayIteratorDouble extends AbstractArrayIterator<Double> implements IteratorDouble
{
	@SafeVarargs
	public ArrayIteratorDouble(final double... elements)
	{
		this(0, elements.length, elements);
	}

	public ArrayIteratorDouble(final int fromIndex, final int toIndex, final double[] elements)
	{
		super(Arrays.stream(elements, fromIndex, toIndex).iterator());
	}

	@Override
	public double nextDouble()
	{
		return ((PrimitiveIterator.OfDouble) iterator).nextDouble();
	}

	public static void main(final String[] args)
	{
		IteratorTools.printIterator("empty", new ArrayIteratorDouble());
		IteratorTools.printIterator("one element", new ArrayIteratorDouble(1));
		IteratorTools.printIterator("three elements", new ArrayIteratorDouble(1, 2, 3));
		IteratorTools.printIterator("second of three elements", new ArrayIteratorDouble(1, 2, new double[] {1, 2, 3}));
	}
}