package common.iterable;

import java.util.Collection;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;

import common.IteratorTools;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;

public class Interval implements IterableInt
{
	public static class IntervalIterator implements FunctionalPrimitiveIterator.OfInt
	{
		final int upperBound;
		final int step;
		int next;

		public IntervalIterator(int first, int upperBound, int step)
		{
			assert step > 0 : "positive step width expected";
			this.upperBound = upperBound;
			this.step       = step;
			this.next       = first;
		}

		@Override
		public boolean hasNext()
		{
			return next < upperBound;
		}

		@Override
		public int nextInt()
		{
			int current = next;
			next        = next + step;
			return current;
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			while (next < upperBound) {
				action.accept(next);
				next += step;
			}
		}

		@Override
		public int collectAndCount(Collection<? super Integer> collection)
		{
			int count = 0;
			while (next < upperBound) {
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
			while (next < upperBound) {
				array[count++] = next;
				next += step;
			}
			return count - offset;
		}

		@Override
		public int collectAndCount(int[] array, int offset)
		{
			int count = offset;
			while (next < upperBound) {
				array[count++] = next;
				next += step;
			}
			return count - offset;
		}

		@Override
		public boolean contains(int i)
		{
			return (i >= next) && (i < upperBound) && ((i - next) % step) == 0;
		}

		@Override
		public int count()
		{
			if (next >= upperBound) {
				return 0;
			}
			return (upperBound - next - 1) / step + 1;
		}

		@Override
		public OptionalInt max()
		{
			return (next >= upperBound) ? OptionalInt.empty() : OptionalInt.of(upperBound);
		}

		@Override
		public OptionalInt min()
		{
			return (next >= upperBound) ? OptionalInt.empty() : OptionalInt.of(next);
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super Integer, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (next < upperBound) {
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
			while (next < upperBound) {
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
			while (next < upperBound) {
				result = accumulator.applyAsInt(result, next);
				next  += step;
			}
			return result;
		}

		@Override
		public int sum()
		{
			if (next >= upperBound) {
				return 0;
			}
			// Sn = a + (a+d) + (a+2d) + ... + (a+(n-1)d)
			// Sn = n(2a+(n-1)d)/2 
			int count = count();
			return count * (2*next + (count-1)*step)/2;
		}
	}

	final int lowerBound;
	final int upperBound;
	final int step;

	public Interval(int upperBound)
	{
		this(0, upperBound);
	}

	public Interval(final int lowerBound, final int upperBound)
	{
		this(lowerBound, upperBound, 1);
	}

	public Interval(final int lowerBound, final int upperBound, final int step)
	{
		assert step > 0 : "positive step width expected";
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.step = step;
	}

	@Override
	public IntervalIterator iterator()
	{
		return new IntervalIterator(lowerBound, upperBound, step);
	}



	public String toString()
	{
		return getClass().getSimpleName() + "(" + lowerBound + ", " + upperBound + ", " + step + ")";
	}

	public static void main(final String[] args)
	{
		Interval interval = new Interval(-3, 5);
		IteratorTools.printIterator("Interval(-3, 5, 1)", interval.iterator());
		System.out.println("count = "+interval.count());
		System.out.println("sum   = "+interval.sum());

		interval = new Interval(-3, 3, 2);
		IteratorTools.printIterator("Interval(-3, 3, 2)", interval.iterator());
		System.out.println("count = "+interval.count());
		System.out.println("sum   = "+interval.sum());

		interval = new Interval(-3, 5, 3);
		IteratorTools.printIterator("Interval(-3, 5, 3)", interval.iterator());
		System.out.println("count = "+interval.count());
		System.out.println("sum   = "+interval.sum());

		interval = new Interval(-3, -3);
		IteratorTools.printIterator("Interval(-3, -3)", interval.iterator());
		System.out.println("count = "+interval.count());
		System.out.println("sum   = "+interval.sum());

	}
}