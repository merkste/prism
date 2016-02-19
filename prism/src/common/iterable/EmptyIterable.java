package common.iterable;

import java.util.Iterator;

public abstract class EmptyIterable<T> implements Iterable<T>
{
	private static final Of<?> OF = new Of<>();
	private static final OfInt OF_INT = new OfInt();
	private static final OfDouble OF_DOUBLE = new OfDouble();

	@SuppressWarnings("unchecked")
	public static <T> Of<T> Of() {
		return (Of<T>) OF;
	}

	public static OfInt OfInt() {
		return OF_INT;
	}

	public static OfDouble OfDouble() {
		return OF_DOUBLE;
	}

	public static class Of<T> extends EmptyIterable<T>
	{
		private Of() {};

		@Override
		public Iterator<T> iterator()
		{
			return EmptyIterator.Of();
		}
	}

	public static class OfInt extends EmptyIterable<Integer> implements IterableInt
	{
		private OfInt() {};

		@Override
		public EmptyIterator.OfInt iterator()
		{
			return EmptyIterator.OfInt();
		}
	}

	public static class OfLong extends EmptyIterable<Long> implements IterableLong
	{
		private OfLong() {};

		@Override
		public EmptyIterator.OfLong iterator()
		{
			return EmptyIterator.OfLong();
		}
	}

	public static class OfDouble extends EmptyIterable<Double> implements IterableDouble
	{
		private OfDouble() {};

		@Override
		public EmptyIterator.OfDouble iterator()
		{
			return EmptyIterator.OfDouble();
		}
	}
}