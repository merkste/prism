package common.iterable;

import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import common.iterable.primitive.IterableDouble;
import common.iterable.primitive.IterableInt;

public abstract class FilteringIterable<T> implements Iterable<T>
{
	protected final Iterable<T> iterable;

	public FilteringIterable(final Iterable<T> iterable)
	{
		this.iterable = iterable;
	}

	public static class Of<T> extends FilteringIterable<T>
	{
		private Predicate<? super T> predicate;

		public Of(Iterable<T> iterable, Predicate<? super T> predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public Iterator<T> iterator()
		{
			return new FilteringIterator.Of<>(iterable, predicate);
		}
	}

	public static class OfInt extends FilteringIterable<Integer> implements IterableInt
	{
		private IntPredicate predicate;

		public OfInt(IterableInt iterable, IntPredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public PrimitiveIterator.OfInt iterator()
		{
			return new FilteringIterator.OfInt((IterableInt) iterable, predicate);
		}
	}

	public static class OfDouble extends FilteringIterable<Double> implements IterableDouble
	{
		private DoublePredicate predicate;

		public OfDouble(IterableDouble iterable, DoublePredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public PrimitiveIterator.OfDouble iterator()
		{
			return new FilteringIterator.OfDouble((IterableDouble) iterable, predicate);
		}
	}
}