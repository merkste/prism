package common.iterable;

import java.util.Iterator;

import common.functions.Mapping;

public class MappingIterator<S, T> implements Iterator<T>
{
	private final Iterator<? extends S> iter;
	private final Mapping<S, ? extends T> mapping;

	public MappingIterator(final Iterable<? extends S> iterable, final Mapping<S, ? extends T> mapping)
	{
		this(iterable.iterator(), mapping);
	}

	public MappingIterator(final Iterator<? extends S> iter, final Mapping<S, ? extends T> mapping)
	{
		this.iter = iter;
		this.mapping = mapping;
	}

	@Override
	public boolean hasNext()
	{
		return iter.hasNext();
	}

	@Override
	public T next()
	{
		return mapping.get(iter.next());
	}

	@Override
	public void remove()
	{
		iter.remove();
	}
}