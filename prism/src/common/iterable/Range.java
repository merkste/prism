package common.iterable;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;

import common.IteratorTools;
import common.functions.ObjIntFunction;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterator.OfInt;

/**
 * An Iterable that yields all integers between two values, first and last (exclusive).
 * The step width can be customized and defaults to 1.
 * <p>
 * If {@code first <= last} then the sequence is <br>
 * <em>ascending</em> (first, first+1, ... , last-1).<br>
 * otherwise<br>
 * <em>descending</em> (first, first-1, ... , last+1).<br>
 */
public class Range implements IterableInt
{
	public static final Range EMPTY = new Range(0);

	protected final int first;
	protected final int last;
	protected final int step;

	/**
	 * Factory method for a range that includes the last element.
	 *
	 * @param last last {@code int}, inclusive
	 */
	public static Range closed(int last)
	{
		return closed(0, last, 1);
	}

	/**
	 * Factory method for a range that includes the last element.
	 *
	 * @param first first {@code int}, inclusive
	 * @param last last {@code int}, inclusive
	 */
	public static Range closed(int first, int last)
	{
		return closed(first, last, 1);
	}

	/**
	 * Factory method for a range that includes the last element.
	 *
	 * @param first first {@code int}, inclusive
	 * @param last last {@code int}, inclusive
	 * @param step a positive {@code int}
	 */
	public static Range closed(int first, int last, int step)
	{
		return new Range(first, last, step, true);
	}



	/**
	 * Constructor for a range from {@code 0} (inclusive) to {@code last} (exclusive)
	 * with step width 1.
	 *
	 * @param last last {@code int}, exclusive
	 */
	public Range(int last)
	{
		this(0, last);
	}

	/**
	 * Constructor for a range from {@code first} (inclusive) to {@code last} (exclusive)
	 * with step width 1.
	 *
	 * @param first first {@code int}, inclusive
	 * @param last last {@code int}, exclusive
	 */
	public Range(int first, int last)
	{
		this(first, last, 1);
	}

	/**
	 * Constructor for a range from {@code 0} (inclusive) to {@code last} (exclusive)
	 * with step width {@code step}.
	 *
	 * @param first first {@code int}, inclusive
	 * @param last last {@code int}, exclusive
	 * @param step a positive {@code int}
	 */
	public Range(int first, int last, int step)
	{
		this(first, last, step, false);
	}

	/**
	 * Constructor for a range from {@code 0} (inclusive) to {@code last} (inclusive or exclusive)
	 * with step width {@code step}.
	 * If {@step > 0} the range is ascending and otherwise descending.
	 *
	 * @param first first {@code int}, inclusive
	 * @param last last {@code int}, inclusive iff {@code closed == true}
	 * @param step an {@code int != 0}
	 * @param closed flag whether {@code last} is inclusive ({@code true}) or not ({@code false})
	 */
	public Range(int first, int last, int step, boolean closed)
	{
		if (step == 0) {
			throw new IllegalArgumentException("Expected: step != 0");
		}
		this.step = step;
		this.first = first;
		// we store the last int inclusively
		this.last = closed ? last : (isAscending() ? last - 1 : last + 1);
	}

	/**
	 * Is this range in ascending order?
	 */
	public boolean isAscending()
	{
		return step > 0;
	}

	/**
	 * Return a new range with all elements in reverse order.
	 */
	public Range reversed()
	{
		return new Range(last, first, -step, true);
	}

	@Override
	public RangeIterator iterator()
	{
		if (isAscending()) {
			return new AscendingRangeIterator(first, last, step);
		} else {
			return new DescendingRangeIterator(first, last, step);
		}
	}

	@Override
	public boolean isEmpty()
	{
		return isAscending() ? first > last : first < last;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "(" + first + ", " + last + ", " + step + ")";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		if (isEmpty()) {
			return result;
		}
		result = prime * result + first;
		result = prime * result + last;
		result = prime * result + step;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Range)) {
			return false;
		}
		Range other = (Range) obj;
		if (isEmpty() && other.isEmpty()) {
			return true;
		}
		if (first != other.first) {
			return false;
		}
		if (last != other.last) {
			return false;
		}
		if (step != other.step) {
			return false;
		}
		return true;
	}



	/**
	 * A abstract base class for an Iterator from {@code first} (inclusive) and {@code last} (exclusive) with a custom step width.
	 */
	public static abstract class RangeIterator implements OfInt
	{
		protected final int last;
		protected final int step;
		protected int next;

		/**
		 * Constructor for an ascending Iterator.
		 *
		 * @param first first {@code int}, inclusive
		 * @param last last {@code int}, exclusive
		 * @param step a positive {@code int}
		 */
		public RangeIterator(int first, int last, int step)
		{
			this.next = first;
			this.last = last;
			this.step = step;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			int current = next;
			next        = next + step;
			return current;
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			while (hasNext()) {
				action.accept(next);
				next += step;
			}
		}

		@Override
		public long collectAndCount(Collection<? super Integer> collection)
		{
			long count = 0;
			while (hasNext()) {
				count++;
				collection.add(next);
				next += step;
			}
			return count;
		}

		@Override
		public int collectAndCount(Integer[] array, int offset)
		{
			int count = offset;
			while (hasNext()) {
				array[count++] = next;
				next += step;
			}
			return count - offset;
		}

		@Override
		public int collectAndCount(int[] array, int offset)
		{
			int count = offset;
			while (hasNext()) {
				array[count++] = next;
				next += step;
			}
			return count - offset;
		}

		@Override
		public long count()
		{
			if (!hasNext()) {
				return 0;
			}
			return distance() / Math.abs(step) + 1;
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super Integer, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (hasNext()) {
				result = accumulator.apply(result, next);
				next  += step;
			}
			return result;
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (hasNext()) {
				result = accumulator.apply(result, next);
				next  += step;
			}
			return result;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = identity;
			while (hasNext()) {
				result = accumulator.applyAsInt(result, next);
				next  += step;
			}
			return result;
		}

		@Override
		public long sum()
		{
			if (!hasNext()) {
				return 0;
			}
			// Sn = a + (a+d) + (a+2d) + ... + (a+(n-1)d)
			// Sn = n(2a+(n-1)d)/2 
			long count = count();
			return count * (2 * next + (count - 1) * step) / 2;
		}

		/**
		 * Compute the distance between the next and the last element.
		 * Use long as return type to prevent over-/underflows
		 * 
		 * @return abs(last - next)
		 */
		protected long distance()
		{
			return Math.abs((long) last - (long) next);
		}

		/**
		 * Check that there is a next element, throw if not.
		 *
		 * @throws NoSuchElementException if Iterator is exhausted
		 */
		protected void requireNext()
		{
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
		}
	}



	/**
	 * Ascending Iterator from {@code first} (inclusive) and {@code last} (inclusive) with a custom step width.
	 */
	public static class AscendingRangeIterator extends RangeIterator
	{
		/**
		 * Constructor for an asscending Iterator.
		 *
		 * @param first first {@code int}, inclusive
		 * @param last last {@code int}, inclusive
		 * @param step a positive {@code int}
		 */
		public AscendingRangeIterator(int first, int last, int step)
		{
			super(first, last, step);
			if (step <= 0) {
				throw new IllegalArgumentException("Expectd: step > 0");
			}
		}

		@Override
		public boolean hasNext()
		{
			return next <= last;
		}

		@Override
		public boolean contains(int i)
		{
			// Is in interval?
			boolean inInterval = (i >= next) && (i <= last);
			// Is step? Mind potential overflow!
			return inInterval && (Math.abs((long) i - (long) next) % step) == 0;
		}

		@Override
		public OptionalInt max()
		{
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			return OptionalInt.of(next + step * (int)(distance() / step));
		}

		@Override
		public OptionalInt min()
		{
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			return OptionalInt.of(next);
		}
	}



	/**
	 * Descending Iterator from {@code first} (inclusive) and {@code last} (inclusive) with a custom step width.
	 */
	public static class DescendingRangeIterator extends RangeIterator
	{
		/**
		 * Constructor for an descending Iterator.
		 *
		 * @param first first {@code int}, inclusive
		 * @param last last {@code int}, inclusive
		 * @param step a negative {@code int}
		 */
		public DescendingRangeIterator(int first, int last, int step)
		{
			super(first, last, step);
			if (step >= 0) {
				throw new IllegalArgumentException("Expected: step < 0");
			}
		}

		@Override
		public boolean hasNext()
		{
			return next >= last;
		}

		@Override
		public boolean contains(int i)
		{
			// Is in interval?
			boolean inInterval = (i <= next) && (i >= last);
			// Is step? Mind potential overflow!
			return inInterval && (Math.abs((long) i - (long) next) % step) == 0;
		}

		@Override
		public OptionalInt max()
		{
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			return OptionalInt.of(next);
		}

		@Override
		public OptionalInt min()
		{
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			return OptionalInt.of(next - step * (int)(distance() / step));
		}
	}



	/**
	 * Simple test method
	 */
	public static void main(final String[] args)
	{
		Range range = new Range(-3, 5);
		IteratorTools.printIterator("Range(-3, 5, 1)", range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
		System.out.println();

		range = new Range(5, -3, -1);
		IteratorTools.printIterator("Range(5, -3, -1)", range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
		System.out.println();

		range = new Range(-3, 3, 2);
		IteratorTools.printIterator("Range(-3, 3, 2)", range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
		System.out.println();

		range = new Range(3, -3, -2);
		IteratorTools.printIterator("Range(3, -3, -2)", range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
		System.out.println();

		range = new Range(-3, 5, 3);
		IteratorTools.printIterator("Range(-3, 5, 3)", range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
		System.out.println();

		range = new Range(3, -5, -3);
		IteratorTools.printIterator("Range(3, -5, -3)", range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
		System.out.println();

		range = new Range(-3, -3);
		IteratorTools.printIterator("Range(-3, -3)", range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
	}
}
