package common.iterable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

public abstract class EmptyIterator<T> implements Iterator<T>
{
	@Override
	public boolean hasNext()
	{
		return false;
	}

	public static class Of<T> extends EmptyIterator<T>
	{
		@Override
		public T next()
		{
			throw new NoSuchElementException();
		}
	}

	public static class OfInt extends EmptyIterator<Integer> implements PrimitiveIterator.OfInt
	{
		@Override
		public int nextInt()
		{
			throw new NoSuchElementException();
		}
	}

	public static class OfDouble extends EmptyIterator<Double> implements PrimitiveIterator.OfDouble
	{
		@Override
		public double nextDouble()
		{
			throw new NoSuchElementException();
		}
	}
}