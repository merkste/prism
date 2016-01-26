package common.iterable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.PrimitiveIterator;

import common.IteratorTools;

/**
 * @deprecated
 * Use J8: Arrays.stream(T[], fromIndex, toIndex).iterator()
 */
@Deprecated
public abstract class ArrayIterator<T> implements Iterator<T>
{
	protected final Iterator<T> iterator;

	public ArrayIterator(final Iterator<T> iterator)
	{
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext()
	{
		return iterator.hasNext();
	}

	public static class Of<T> extends ArrayIterator<T>
	{
		@SafeVarargs
		public Of(T... elements)
		{
			this(elements, 0, elements.length);
		}

		public Of(T[] elements, int fromIndex, int toIndex)
		{
			super(Arrays.stream(elements, fromIndex, toIndex).iterator());
		}

		@Override
		public T next()
		{
			return iterator.next();
		}
	}

	public static class OfInt extends ArrayIterator<Integer> implements PrimitiveIterator.OfInt
	{
		@SafeVarargs
		public OfInt(final int... elements)
		{
			this(elements, 0, elements.length);
		}

		public OfInt(final int[] elements, int fromIndex, int toIndex)
		{
			super(Arrays.stream(elements, fromIndex, toIndex).iterator());
		}

		@Override
		public int nextInt()
		{
			return ((PrimitiveIterator.OfInt) iterator).nextInt();
		}
	}

	public static class OfDouble extends ArrayIterator<Double> implements PrimitiveIterator.OfDouble
	{
		@SafeVarargs
		public OfDouble(final double... elements)
		{
			this(elements, 0, elements.length);
		}

		public OfDouble(final double[] elements, final int fromIndex, final int toIndex)
		{
			super(Arrays.stream(elements, fromIndex, toIndex).iterator());
		}

		@Override
		public double nextDouble()
		{
			return ((PrimitiveIterator.OfDouble) iterator).nextDouble();
		}
	}

	public static void main(final String[] args)
	{
		IteratorTools.printIterator("empty", new ArrayIterator.Of<Integer>());
		IteratorTools.printIterator("one element", new ArrayIterator.Of<Integer>(1));
		IteratorTools.printIterator("three elements", new ArrayIterator.Of<Integer>(1, 2, 3));
		IteratorTools.printIterator("second of three elements", new ArrayIterator.Of<Integer>(new Integer[] { 1, 2, 3 }, 1, 2));

		IteratorTools.printIterator("empty", new ArrayIterator.OfInt());
		IteratorTools.printIterator("one element", new ArrayIterator.OfInt(1));
		IteratorTools.printIterator("three elements", new ArrayIterator.OfInt(1, 2, 3));
		IteratorTools.printIterator("second of three elements", new ArrayIterator.OfInt(new int[] { 1, 2, 3 }, 1, 2));

		IteratorTools.printIterator("empty", new ArrayIterator.OfDouble());
		IteratorTools.printIterator("one element", new ArrayIterator.OfDouble(1));
		IteratorTools.printIterator("three elements", new ArrayIterator.OfDouble(1, 2, 3));
		IteratorTools.printIterator("second of three elements", new ArrayIterator.OfDouble(new double[] {1, 2, 3}, 1, 2));
	}
}