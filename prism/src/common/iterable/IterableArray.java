package common.iterable;

import java.util.Arrays;

public abstract class IterableArray<T> implements FunctionalIterable<T>
{
	protected final int fromIndex;
	protected final int toIndex;

	public IterableArray(int fromIndex, int toIndex)
	{
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
	}

	public int size()
	{
		return Math.max(0, toIndex - fromIndex);
	}

	public static class Of<T> extends IterableArray<T>
	{
		protected final T[] elements;

		@SafeVarargs
		public Of(T... elements)
		{
			super(0, elements.length);
			this.elements = elements;
		}

		public Of(T[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			this.elements = elements;
		}

		@Override
		public FunctionalIterator<T> iterator()
		{
			return FunctionalIterator.extend(Arrays.stream(elements, fromIndex, toIndex).iterator());
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
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return (FunctionalPrimitiveIterator.OfInt) FunctionalIterator.extend(Arrays.stream(elements, fromIndex, toIndex).iterator());
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
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return (FunctionalPrimitiveIterator.OfLong) FunctionalIterator.extend(Arrays.stream(elements, fromIndex, toIndex).iterator());
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
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return (FunctionalPrimitiveIterator.OfDouble) FunctionalIterator.extend(Arrays.stream(elements, fromIndex, toIndex).iterator());
		}
	}
}