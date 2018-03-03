package common.iterable;

import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

public abstract class IterableArray<E> implements FunctionalIterable<E>
{
	protected final int fromIndex;
	protected final int toIndex;

	public IterableArray(int fromIndex, int toIndex)
	{
		if (fromIndex < 0) {
			throw new IllegalArgumentException("non-negative fromIndex expected, got: " + fromIndex);
		}
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
	}

	public int size()
	{
		return Math.max(0, toIndex - fromIndex);
	}



	public static class Of<E> extends IterableArray<E>
	{
		protected final E[] elements;

		@SafeVarargs
		public Of(E... elements)
		{
			super(0, elements.length);
			this.elements = elements;
		}

		public Of(E[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			this.elements = elements;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new ArrayIterator.Of<>(elements, fromIndex, toIndex);
		}
	}



	public static class OfInt extends IterableArray<Integer> implements IterableInt
	{
		protected final int[] elements;

		@SafeVarargs
		public OfInt(int... elements)
		{
			super(0, elements.length);
			this.elements = elements;
		}

		public OfInt(int[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			this.elements = elements;
		}

		@Override
		public ArrayIterator.OfInt iterator()
		{
			return new ArrayIterator.OfInt(elements, fromIndex, toIndex);
		}
	}



	public static class OfDouble extends IterableArray<Double> implements IterableDouble
	{
		protected final double[] elements;

		@SafeVarargs
		public OfDouble(double... elements)
		{
			super(0, elements.length);
			this.elements = elements;
		}

		public OfDouble(double[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			this.elements = elements;
		}

		@Override
		public ArrayIterator.OfDouble iterator()
		{
			return new ArrayIterator.OfDouble(elements, fromIndex, toIndex);
		}
	}



	public static class OfLong extends IterableArray<Long> implements IterableLong
	{
		protected final long[] elements;

		@SafeVarargs
		public OfLong(long... elements)
		{
			super(0, elements.length);
			this.elements = elements;
		}

		public OfLong(long[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			this.elements = elements;
		}

		@Override
		public ArrayIterator.OfLong iterator()
		{
			return new ArrayIterator.OfLong(elements, fromIndex, toIndex);
		}
	}
}
