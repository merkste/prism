package common.iterable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import common.iterable.primitive.IterableDouble;
import common.iterable.primitive.IterableInt;

public abstract class FilteringIterator<T> implements Iterator<T>
{
	protected final Iterator<T> iterator;
	protected boolean hasNext;

	public FilteringIterator(final Iterable<T> iterable)
	{
		this(iterable.iterator());
	}

	public FilteringIterator(final Iterator<T> iterator)
	{
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext()
	{
		return hasNext;
	}

	protected void requireNext()
	{
		if (!hasNext) {
			throw new NoSuchElementException();
		}
	}

	public static <T> Iterator<T> dedupe(final Iterator<T> iterator)
	{
		final Set<T> elements = new HashSet<>();
		return new FilteringIterator.Of<>(iterator, (Predicate<T>) elements::add);
	}

	public static class Of<T> extends FilteringIterator<T>
	{
		protected final Predicate<? super T> predicate;
		private T next;

		public Of(Iterable<T> iterable, Predicate<? super T> predicate)
		{
			this(iterable.iterator(), predicate);
		}

		public Of(Iterator<T> iterator, Predicate<? super T> predicate)
		{
			super(iterator);
			this.predicate = predicate;
			seekNext();
		}

		@Override
		public T next()
		{
			requireNext();
			T current = next;
			seekNext();
			return current;
		}

		private void seekNext()
		{
			while (iterator.hasNext()) {
				next = iterator.next();
				if (predicate.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
			next = null;
		}
	}

	public static class OfInt extends FilteringIterator<Integer> implements PrimitiveIterator.OfInt
	{
		protected final IntPredicate predicate;
		private int next;

		public OfInt(IterableInt iterable, IntPredicate predicate)
		{
			this(iterable.iterator(), predicate);
		}

		public OfInt(PrimitiveIterator.OfInt iterator, IntPredicate predicate)
		{
			super(iterator);
			this.predicate = predicate;
			seekNext();
		}

		@Override
		public int nextInt()
		{
			requireNext();
			int current = next;
			seekNext();
			return current;
		}

		private void seekNext()
		{
			while (iterator.hasNext()) {
				next = ((PrimitiveIterator.OfInt) iterator).nextInt();
				if (predicate.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}
	}

	public static class OfDouble extends FilteringIterator<Double> implements PrimitiveIterator.OfDouble
	{
		protected final DoublePredicate predicate;
		private double next;

		public OfDouble(IterableDouble iterable, DoublePredicate predicate)
		{
			this(iterable.iterator(), predicate);
		}

		public OfDouble(PrimitiveIterator.OfDouble iterator, DoublePredicate predicate)
		{
			super(iterator);
			this.predicate = predicate;
			seekNext();
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			double current = next;
			seekNext();
			return current;
		}

		private void seekNext()
		{
			while (iterator.hasNext()) {
				next = ((PrimitiveIterator.OfDouble) iterator).nextDouble();
				if (predicate.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}
	}
}