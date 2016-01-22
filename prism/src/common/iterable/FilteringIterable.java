package common.iterable;

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class FilteringIterable<T> implements Iterable<T>
{
	private final Iterable<? extends T> iterable;
	private final Predicate<T> predicate;

	/**
	 * @deprecated
	 * Use J8 Functions instead.
	 */
	@Deprecated
	public FilteringIterable(final Iterable<? extends T> iterable, final common.functions.Predicate<T> predicate)
	{
		this(iterable, predicate::getBoolean);
	}

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

	public Stream<T> stream()
	{
		return StreamSupport.stream(spliterator(), false);
	}
}