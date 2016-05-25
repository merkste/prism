package common.iterable;

import java.util.Iterator;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

public abstract class FilteringIterable<T> implements Iterable<T>
{
	protected final Iterable<T> iterable;

	public static <T> Iterable<T> nonNull(Iterable<T> iterable)
	{
		if (iterable instanceof PrimitiveIterable) {
			return iterable;
		}
		return new FilteringIterable.Of<>(iterable, Objects::nonNull);
	}

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

	public static class OfLong extends FilteringIterable<Long> implements IterableLong
	{
		private LongPredicate predicate;

		public OfLong(IterableLong iterable, LongPredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public PrimitiveIterator.OfLong iterator()
		{
			return new FilteringIterator.OfLong((IterableLong) iterable, predicate);
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

	public static IterableInt dedupe(IterableInt iterable)
	{
		return new IterableInt()
		{
			@Override
			public PrimitiveIterator.OfInt iterator()
			{
				return FilteringIterator.dedupe(iterable.iterator());
			}
		};
	}

	public static IterableLong dedupe(IterableLong iterable)
	{
		return new IterableLong()
		{
			@Override
			public PrimitiveIterator.OfLong iterator()
			{
				return FilteringIterator.dedupe(iterable.iterator());
			}
		};
	}

	public static IterableDouble dedupe(IterableDouble iterable)
	{
		return new IterableDouble()
		{
			@Override
			public PrimitiveIterator.OfDouble iterator()
			{
				return FilteringIterator.dedupe(iterable.iterator());
			}
		};
	}

	public static <T> Iterable<T> dedupe(Iterable<T> iterable)
	{
		return new Iterable<T>()
		{
			@Override
			public Iterator<T> iterator()
			{
				return FilteringIterator.dedupe(iterable.iterator());
			}
		};
	}
}