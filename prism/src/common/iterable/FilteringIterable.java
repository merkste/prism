package common.iterable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public abstract class FilteringIterable<T> implements FunctionalIterable<T>
{
	protected final Iterable<T> iterable;

	public static <T> FunctionalIterable<T> dedupe(Iterable<T> iterable)
	{
		return new DedupedIterable<>(iterable);
	}

	public static IterableDouble dedupe(IterableDouble iterable)
	{
		return new DedupedIterable<>(iterable).map((ToDoubleFunction<Double>) Double::doubleValue);
	}

	public static IterableInt dedupe(IterableInt iterable)
	{
		return new DedupedIterable<>(iterable).map((ToIntFunction<Integer>) Integer::intValue);
	}

	public static IterableLong dedupe(IterableLong iterable)
	{
		return new DedupedIterable<>(iterable).map((ToLongFunction<Long>) Long::longValue);
	}

	public static class DedupedIterable<E> implements FunctionalIterable<E>
	{
		protected FunctionalIterator<E> source;
		protected IterableInt iterable;

		public DedupedIterable(Iterable<E> source)
		{
			this.source   = FunctionalIterator.extend(source.iterator());
			this.iterable = null;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			if (source == null) {
				iterable.iterator();
			}
			Set<E> set = new HashSet<E>();
			FilteringIterator.Of<E> iterator = new FilteringIterator.Of<>(source, set::add);
			source = null;
			return iterator;
		}
	}

	public static <T> Iterable<T> nonNull(Iterable<T> iterable)
	{
		if (iterable instanceof FunctionalPrimitiveIterable) {
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
		public FunctionalIterator<T> iterator()
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
		public FunctionalPrimitiveIterator.OfInt iterator()
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
		public FunctionalPrimitiveIterator.OfLong iterator()
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
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new FilteringIterator.OfDouble((IterableDouble) iterable, predicate);
		}
	}
}
