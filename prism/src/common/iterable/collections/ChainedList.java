package common.iterable.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import common.iterable.FunctionalIterator;
import common.iterable.IterableArray;

public class ChainedList<T> extends AbstractList<T>
{
	protected final ArrayList<List<? extends T>> lists;
	protected int size = -1;

	@SafeVarargs
	public ChainedList(final List<? extends T>... lists)
	{
		this(new IterableArray.Of<>(lists));
	}

	public ChainedList(final Iterable<? extends List<? extends T>> lists)
	{
		this.lists = new ArrayList<>();
		for (List<? extends T> list : lists) {
			if (list != null && ! list.isEmpty()) {
				this.lists.add(list);
			}
		}
		this.lists.trimToSize();
	}

	@Override
	public T get(final int index)
	{
		int localIndex = index;
		for (List<? extends T> list : lists) {
			int localSize = list.size();
			if (localIndex < localSize) {
				return list.get(localIndex);
			}
			localIndex -= localSize;
		}
		throw new IndexOutOfBoundsException();
	}

	@Override
	public Iterator<T> iterator()
	{
		return FunctionalIterator.extend(lists).flatMap(List::iterator);
	}

	@Override
	public Stream<T> stream()
	{
		return lists.stream().flatMap(List::stream);
	}

	@Override
	public int size()
	{
		if (size < 0) {
			size = Math.toIntExact(lists.stream().mapToInt(List::size).sum());
		}
		return size;
	}

	public static void main(final String[] args)
	{
		final List<Integer> l1 = Arrays.asList(new Integer[] {0, 1, 2});
		final List<Integer> l2 = Arrays.asList(new Integer[] {3, 4, 5});
		final List<Integer> l3 = Arrays.asList(new Integer[] {6, 7, 8});
		final List<Integer> chain = new ChainedList<>(null, new ArrayList<Integer>(), l1, l2, l3);

		System.out.println(chain);
		System.out.println("chain.size() = " + chain.size());
		System.out.println("chain.get(6) = " + chain.get(6));
	}
}