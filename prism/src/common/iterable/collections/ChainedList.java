package common.iterable.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class ChainedList<T> extends AbstractList<T>
{
	private final List<List<? extends T>> lists;
	private int size = -1;

	@SafeVarargs
	public ChainedList(final List<? extends T>... lists)
	{
		this(Arrays.asList(lists));
	}

	public ChainedList(final Iterable<? extends List<? extends T>> lists)
	{
		final Stream<? extends List<? extends T>> streamOfLists = StreamSupport.stream(lists.spliterator(), false);
		this.lists = streamOfLists.filter(l -> !(l == null || l.isEmpty())).collect(Collectors.toList());
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

	@Override
	public Iterator<T> iterator()
	{
		return stream().iterator();
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
		final List<Integer> l1 = Arrays.asList(new Integer[] { 1, 2, 3 });
		final List<Integer> l2 = Arrays.asList(new Integer[] { 4, 5, 6 });
		final List<Integer> l3 = Arrays.asList(new Integer[] { 7, 8, 9 });
		final List<Integer> chain = new ChainedList<>(null, new ArrayList<Integer>(), l1, l2, l3);

		System.out.println(chain);
		System.out.println("chain.size() = " + chain.size());
	}
}