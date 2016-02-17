package common.iterable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import common.functions.AbstractPredicate;
import common.functions.Predicate;

public class FilteringIterator<T> implements Iterator<T>
{
	private final Iterator<? extends T> iter;
	private final Predicate<T> predicate;
	private boolean hasNext;
	private T next;

	public FilteringIterator(final Iterable<? extends T> iterable, final Predicate<T> predicate)
	{
		this(iterable.iterator(), predicate);
	}

	public FilteringIterator(final Iterator<? extends T> iter, final Predicate<T> predicate)
	{
		this.iter = iter;
		this.predicate = predicate;
		seekNext();
	}

	@Override
	public boolean hasNext()
	{
		return hasNext;
	}

	@Override
	public T next()
	{
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		final T current = next;
		seekNext();
		return current;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("removing not supported");
	}

	private void seekNext()
	{
		while (iter.hasNext()) {
			next = iter.next();
			if (predicate.getBoolean(next)) {
				hasNext = true;
				return;
			}
		}
		hasNext = false;
		next = null;
	}

	public static <T> FilteringIterator<T> dedupe(final Iterator<T> iter)
	{
		return new FilteringIterator<>(iter, new NoDupes<T>());
	}

	public static class NoDupes<T> extends AbstractPredicate<T>
	{
		final Set<T> elements = new HashSet<>();

		@Override
		public boolean getBoolean(T element)
		{
			return elements.add(element);
		}
	}
}