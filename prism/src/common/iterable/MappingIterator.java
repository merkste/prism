package common.iterable;

import java.util.Iterator;
import java.util.function.Function;

import common.functions.Mapping;

public class MappingIterator<S, T> implements Iterator<T>
{
	private final Iterator<? extends S> iterator;
	private final Function<S, ? extends T> function;

	/**
	 * @deprecated
	 * Use J8 Functions instead.
	 */
	@Deprecated()
	public MappingIterator(final Iterable<? extends S> iterable, final Mapping<S, ? extends T> mapping)
	{
		this(iterable.iterator(), mapping);
	}

	/**
	 * @deprecated
	 * Use J8 Functions instead.
	 */
	@Deprecated()
	public MappingIterator(final Iterator<? extends S> iter, final Mapping<S, ? extends T> mapping)
	{
		this(iter, mapping::get);
	}

	public MappingIterator(final Iterable<? extends S> iterable, final Function<S, ? extends T> function)
	{
		this(iterable.iterator(), function);
	}

	public MappingIterator(final Iterator<? extends S> iter, final Function<S, ? extends T> function)
	{
		this.iterator = iter;
		this.function = function;
	}

	@Override
	public boolean hasNext()
	{
		return iterator.hasNext();
	}

	@Override
	public T next()
	{
		return function.apply(iterator.next());
	}

	@Override
	public void remove()
	{
		iterator.remove();
	}
}