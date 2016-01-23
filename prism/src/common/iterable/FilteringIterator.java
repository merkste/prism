package common.iterable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

public class FilteringIterator<T> implements Iterator<T>
{
	private final Iterator<? extends T> iter;
	private final Predicate<T> predicate;
	private boolean hasNext;
	private T next;

	/**
	 * @deprecated
	 * Use J8 Functions instead.
	 */
	@Deprecated
	public FilteringIterator(final Iterable<? extends T> iterable, final common.functions.Predicate<T> predicate)
	{
		this(iterable.iterator(), predicate);
	}

	/**
	 * @deprecated
	 * Use J8 Functions instead.
	 */
	@Deprecated
	public FilteringIterator(final Iterator<? extends T> iter, final common.functions.Predicate<T> predicate)
	{
		this(iter, predicate::test);
	}

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

	private void seekNext()
	{
		while (iter.hasNext()) {
			next = iter.next();
			if (predicate.test(next)) {
				hasNext = true;
				return;
			}
		}
		hasNext = false;
		next = null;
	}

	public static <T> Iterator<T> dedupe(final Iterator<T> iter)
	{
		final Set<T> elements = new HashSet<>();
		return new FilteringIterator<>(iter, elements::add);
	}
}