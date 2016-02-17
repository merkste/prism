package common.iterable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import common.functions.AbstractMapping;
import common.functions.Mapping;

public class MappingIterable<S, T> implements Iterable<T>
{
	private final Iterable<? extends S> iterable;
	private final Mapping<S, ? extends T> mapping;

	public MappingIterable(final Iterable<? extends S> iterable, final Mapping<S, ? extends T> mapping)
	{
		this.iterable = iterable;
		this.mapping = mapping;
	}

	@Override
	public Iterator<T> iterator()
	{
		return new MappingIterator<>(iterable.iterator(), mapping);
	}

	public static void main(String[] args)
	{
		final List<Integer> list = Arrays.asList(new Integer[] { 1, 2, 3 });
		final Mapping<Integer, Integer> successor = new AbstractMapping<Integer, Integer>()
		{
			@Override
			public final Integer get(final Integer i)
			{
				return i + 1;
			}
		};
		final Iterable<Integer> successors = new MappingIterable<>(list, successor);

		System.out.print("[");
		for (Iterator<Integer> integers = successors.iterator(); integers.hasNext();) {
			System.out.print(integers.next());
			if (integers.hasNext()) {
				System.out.print(", ");
			}
		}
		System.out.println("]");
	}
}