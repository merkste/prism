package common.iterable;

import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

public abstract class EmptyIterable<T> implements FunctionalIterable<T>
{
	private static final Of<?>    OF        = new Of<>();
	private static final OfDouble OF_DOUBLE = new OfDouble();
	private static final OfInt    OF_INT    = new OfInt();
	private static final OfLong   OF_LONG   = new OfLong();

	@SuppressWarnings("unchecked")
	public static <T> Of<T> Of() {
		return (Of<T>) OF;
	}

	public static OfDouble OfDouble() {
		return OF_DOUBLE;
	}

	public static OfInt OfInt() {
		return OF_INT;
	}

	public static OfLong OfLong() {
		return OF_LONG;
	}

	public static class Of<T> extends EmptyIterable<T>
	{
		private Of() {};

		@Override
		public EmptyIterator.Of<T> iterator()
		{
			return EmptyIterator.Of();
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
}