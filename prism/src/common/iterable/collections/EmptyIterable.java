package common.iterable.collections;

import java.util.Iterator;

import common.iterable.IterableDouble;
import common.iterable.IterableInt;

public abstract class EmptyIterable<T> implements Iterable<T>
{
	public static class Of<T> extends EmptyIterable<T>
	{
		@Override
		public Iterator<T> iterator()
		{
			return new EmptyIterator.Of<>();
		}
	}

	public static class OfInt extends EmptyIterable<Integer> implements IterableInt
	{
		@Override
		public EmptyIterator.OfInt iterator()
		{
			return new EmptyIterator.OfInt();
		}
	}

	public static class OfDouble extends EmptyIterable<Double> implements IterableDouble
	{
		@Override
		public EmptyIterator.OfDouble iterator()
		{
			return new EmptyIterator.OfDouble();
		}
	}
}