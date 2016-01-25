package common.iterable.collections;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import common.functions.Mapping;

public class MappingList<S, T> extends AbstractList<T>
{
	private final List<S> list;
	private final Function<? super S, T> function;

	/**
	 * @deprecated
	 * Use J8 Functions instead.
	 */
	@Deprecated
	public MappingList(final List<S> list, final Mapping<? super S, T> mapping)
	{
		this(list, (Function<? super S, T>) mapping::apply);
	}

	public MappingList(final List<S> list, final Function<? super S, T> function)
	{
		this.list = list;
		this.function = function;
	}

	@Override
	public T get(int index)
	{
		return function.apply(list.get(index));
	}

	@Override
	public Iterator<T> iterator()
	{
		return stream().iterator();
	}

	@Override
	public Stream<T> stream()
	{
		return list.stream().map(function);
	}

	@Override
	public int size()
	{
		return list.size();
	}

	public static void main(String[] args)
	{
		final List<Integer> list = Arrays.asList(new Integer[] {1, 2, 3});
		final Mapping<Integer, Integer> successor = new Mapping<Integer, Integer>()
		{
			@Override
			public final Integer apply(final Integer i)
			{
				return i + 1;
			}
		};
		final Iterable<Integer> successors = new MappingList<>(list, successor);

		System.out.println(successors);
	}
}
