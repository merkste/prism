package common.iterable.collections;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import common.functions.AbstractMapping;
import common.functions.Mapping;
import common.iterable.MappingIterator;

public class MappingList<S, T> extends AbstractList<T>
{
	private final List<? extends S> list;
	private final Mapping<S, ? extends T> mapping;

	public MappingList(final List<? extends S> list, final Mapping<S, ? extends T> mapping)
	{
		this.list = list;
		this.mapping = mapping;
	}

	@Override
	public T get(int index)
	{
		return mapping.get(list.get(index));
	}

	@Override
	public Iterator<T> iterator()
	{
		return new MappingIterator<>(list.iterator(), mapping);
	}

	@Override
	public int size()
	{
		return list.size();
	}

	public static void main(String[] args)
	{
		final List<Integer> list = Arrays.asList(new Integer[] {1, 2, 3});
		final Mapping<Integer, Integer> successor = new AbstractMapping<Integer, Integer>()
		{
			@Override
			public final Integer get(final Integer i)
			{
				return i + 1;
			}
		};
		final Iterable<Integer> successors = new MappingList<>(list, successor);

		System.out.println(successors);
	}
}
