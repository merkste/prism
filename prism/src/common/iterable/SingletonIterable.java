package common.iterable;

public abstract class SingletonIterable<T> implements FunctionalIterable<T>
{
	public static class Of<T> extends SingletonIterable<T>
	{
		final T element;

		public Of(T theElement)
		{
			element = theElement;
		}

		@Override
		public FunctionalIterator<T> iterator()
		{
			return new SingletonIterator.Of<>(element);
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
}