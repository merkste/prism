package common.iterable;

import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

public abstract class SingletonIterable<E> implements FunctionalIterable<E>
{
	public static class Of<E> extends SingletonIterable<E>
	{
		final E element;

		public Of(E theElement)
		{
			element = theElement;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new SingletonIterator.Of<>(element);
		}
	}



	public static class OfDouble extends SingletonIterable<Double> implements IterableDouble
	{
		final double element;
	
		public OfDouble(double theElement)
		{
			element = theElement;
		}
	
		@Override
		public SingletonIterator.OfDouble iterator()
		{
			return new SingletonIterator.OfDouble(element);
		}
	}



	public static class OfInt extends SingletonIterable<Integer> implements IterableInt
	{
		final int element;

		public OfInt(int theElement)
		{
			element = theElement;
		}

		@Override
		public SingletonIterator.OfInt iterator()
		{
			return new SingletonIterator.OfInt(element);
		}
	}



	public static class OfLong extends SingletonIterable<Long> implements IterableLong
	{
		final long element;

		public OfLong(long theElement)
		{
			element = theElement;
		}

		@Override
		public SingletonIterator.OfLong iterator()
		{
			return new SingletonIterator.OfLong(element);
		}
	}
}
