package common.iterable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

public abstract class EmptyIterator<T> implements Iterator<T>
{
	private static final Of<?> OF           = new Of<>();
	private static final OfInt OF_INT       = new OfInt();
	private static final OfLong OF_LONG     = new OfLong();
	private static final OfDouble OF_DOUBLE = new OfDouble();

	@SuppressWarnings("unchecked")
	public static <T> Of<T> Of() {
		return (Of<T>) OF;
	}

	public static OfInt OfInt() {
		return OF_INT;
	}

	public static OfLong OfLong() {
		return OF_LONG;
	}

	public static OfDouble OfDouble() {
		return OF_DOUBLE;
	}

	@Override
	public boolean hasNext()
	{
		return false;
	}

	public static class Of<T> extends EmptyIterator<T>
	{
		private Of() {};

		@Override
		public T next()
		{
			throw new NoSuchElementException();
		}
	}

	public static class OfInt extends EmptyIterator<Integer> implements PrimitiveIterator.OfInt
	{
		private OfInt() {};

		@Override
		public int nextInt()
		{
			throw new NoSuchElementException();
		}
	}

	public static class OfLong extends EmptyIterator<Long> implements PrimitiveIterator.OfLong
	{
		private OfLong() {};

		@Override
		public long nextLong()
		{
			throw new NoSuchElementException();
		}
	}

	public static class OfDouble extends EmptyIterator<Double> implements PrimitiveIterator.OfDouble
	{
		private OfDouble() {};

		@Override
		public double nextDouble()
		{
			throw new NoSuchElementException();
		}
	}
}