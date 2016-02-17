package common.iterable;

import java.util.Iterator;

import common.functions.Predicate;

public class FilteringIterable<T> implements Iterable<T>
{
	private final Iterable<? extends T> iterable;
	private final Predicate<T> predicate;

	public FilteringIterable(final Iterable<? extends T> iterable, final Predicate<T> predicate)
	{
		this.iterable = iterable;
		this.predicate = predicate;
	}

	@Override
	public Iterator<T> iterator()
	{
		return new FilteringIterator<>(iterable, predicate);
	}
}