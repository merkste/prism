package common.iterable.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import common.IteratorTools;
import common.iterable.MappingIterator;
import common.methods.CallCollection;

public class ChainedList<T> extends AbstractList<T>
{
	private final List<List<? extends T>> lists;

	@SafeVarargs
	public ChainedList(final List<? extends T>... lists)
	{
		this(Arrays.asList(lists));
	}

	public ChainedList(final Iterable<? extends List<? extends T>> lists)
	{
		this.lists = new ArrayList<>();
		for (List<? extends T> list : lists) {
			if (list == null || list.isEmpty()) {
				continue;
			}
			this.lists.add(list);
		}
	}

	@Override
	public int size()
	{
		return IteratorTools.sumIntegers(new MappingIterator<>(lists, CallCollection.size()));
	}

	@Override
	public T get(final int index)
	{
		int localIndex = index;
		for (List<? extends T> list : lists) {
			if (localIndex < list.size()) {
				return list.get(localIndex);
			}
			localIndex -= list.size();
		}
		throw new IndexOutOfBoundsException();
	}

	public static void main(final String[] args)
	{
		final List<Integer> l1 = Arrays.asList(new Integer[] { 1, 2, 3 });
		final List<Integer> l2 = Arrays.asList(new Integer[] { 4, 5, 6 });
		final List<Integer> l3 = Arrays.asList(new Integer[] { 7, 8, 9 });
		final List<Integer> chain = new ChainedList<>(null, new ArrayList<Integer>(), l1, l2, l3);

		System.out.println(chain);
		System.out.println("chain.size() = " + chain.size());
	}
}