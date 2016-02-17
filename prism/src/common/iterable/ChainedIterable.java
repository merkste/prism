package common.iterable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import common.methods.CallIterable;

public class ChainedIterable<T> implements Iterable<T>
{
	private final Iterable<? extends Iterable<? extends T>> iterables;

	@SafeVarargs
	public ChainedIterable(final Iterable<? extends T>... iterables)
	{
		this(Arrays.asList(iterables));
	}

	public ChainedIterable(final Iterable<? extends Iterable<? extends T>> iterables)
	{
		this.iterables = iterables;
	}

	@Override
	public Iterator<T> iterator()
	{
		return new ChainedIterator<>(new MappingIterator<>(iterables, CallIterable.<T> iterator()));
	}

	public static void main(final String[] args)
	{
		final List<Integer> l1 = Arrays.asList(new Integer[] { 1, 2, 3 });
		final List<Integer> l2 = Arrays.asList(new Integer[] { 4, 5, 6 });
		final Iterable<Integer> chain = new ChainedIterable<Integer>(l1, l2);

		System.out.print("[");
		for (Iterator<Integer> integers = chain.iterator(); integers.hasNext();) {
			System.out.print(integers.next());
			if (integers.hasNext()) {
				System.out.print(", ");
			}
		}
		System.out.println("]");
	}
}