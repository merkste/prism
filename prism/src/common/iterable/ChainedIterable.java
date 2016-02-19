package common.iterable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator;


public abstract class ChainedIterable<T> implements Iterable<T>
{
	protected final Iterable<? extends Iterable<? extends T>> iterables;

	@SafeVarargs
	public ChainedIterable(Iterable<? extends T>... iterables)
	{
		this(Arrays.asList(iterables));
	}

	public ChainedIterable(Iterable<? extends Iterable<? extends T>> iterables)
	{
		this.iterables = iterables;
	}

	public static class Of<T> extends ChainedIterable<T>
	{
		@SafeVarargs
		public Of(Iterable<? extends T>... iterables)
		{
			super(iterables);
		}

		public Of(Iterable<? extends Iterable<? extends T>> iterables)
		{
			super(iterables);
		}

		@Override
		public Iterator<T> iterator()
		{
			return new ChainedIterator.Of<>(new MappingIterator.From<>(iterables, Iterable::iterator));
		}
	}

	public static class OfInt extends ChainedIterable<Integer> implements IterableInt
	{
		@SafeVarargs
		public OfInt(IterableInt... iterables)
		{
			super(iterables);
		}

		public OfInt(Iterable<IterableInt> iterables)
		{
			super(iterables);
		}

		@SuppressWarnings("unchecked")
		@Override
		public PrimitiveIterator.OfInt iterator()
		{
			return new ChainedIterator.OfInt(new MappingIterator.From<>((Iterable<IterableInt>) iterables, IterableInt::iterator));
		}
	}

	public static class OfLong extends ChainedIterable<Long> implements IterableLong
	{
		@SafeVarargs
		public OfLong(IterableLong... iterables)
		{
			super(iterables);
		}

		public OfLong(Iterable<IterableLong> iterables)
		{
			super(iterables);
		}

		@SuppressWarnings("unchecked")
		@Override
		public PrimitiveIterator.OfLong iterator()
		{
			return new ChainedIterator.OfLong(new MappingIterator.From<>((Iterable<IterableLong>) iterables, IterableLong::iterator));
		}
	}

	public static class OfDouble extends ChainedIterable<Double> implements IterableDouble
	{
		@SafeVarargs
		public OfDouble(IterableDouble... iterables)
		{
			super(iterables);
		}

		public OfDouble(Iterable<IterableDouble> iterables)
		{
			super(iterables);
		}

		@SuppressWarnings("unchecked")
		@Override
		public PrimitiveIterator.OfDouble iterator()
		{
			return new ChainedIterator.OfDouble(new MappingIterator.From<>((Iterable<IterableDouble>) iterables, IterableDouble::iterator));
		}
	}

	public static void main(final String[] args)
	{
		final List<Integer> l1 = Arrays.asList(new Integer[] { 1, 2, 3 });
		final List<Integer> l2 = Arrays.asList(new Integer[] { 4, 5, 6 });
		final Iterable<Integer> chain1 = new ChainedIterable.Of<Integer>(l1, l2);

		System.out.print("[");
		for (Iterator<Integer> integers = chain1.iterator(); integers.hasNext();) {
			System.out.print(integers.next());
			if (integers.hasNext()) {
				System.out.print(", ");
			}
		}
		System.out.println("]");

		final Interval i1 = new Interval(1, 4);
		final Interval i2 = new Interval(5, 10, 2);
		final IterableInt chain2 = new ChainedIterable.OfInt(i1, i2);

		System.out.print("[");
		for (Iterator<Integer> integers = chain2.iterator(); integers.hasNext();) {
			System.out.print(integers.next());
			if (integers.hasNext()) {
				System.out.print(", ");
			}
		}
		System.out.println("]");
	}
}