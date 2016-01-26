package common.iterable.collections;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import common.iterable.MappingIterator;

public class MappingList<S, T> extends AbstractList<T>
{
	protected final List<S> list;
	protected final Function<? super S, T> function;

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
		return new MappingIterator.From<>(list, function);
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
		final Function<Integer, Integer> successor = x -> x + 1;
		final Iterable<Integer> successors = new MappingList<>(list, successor);

		System.out.println(successors);
	}
}
