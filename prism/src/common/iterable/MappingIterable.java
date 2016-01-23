package common.iterable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import common.functions.AbstractMapping;
import common.functions.Mapping;

public class MappingIterable<S, T> implements Iterable<T>
{
	private final Iterable<S> iterable;
	private final Function<? super S, T> function;

	/**
	 * @deprecated
	 * Use J8 Functions instead.
	 */
	@Deprecated
	public MappingIterable(final Iterable<S> iterable, final Mapping<? super S, T> mapping)
	{
		this(iterable, mapping::get);
	}

	public MappingIterable(final Iterable<S> iterable, final Function<? super S, T> function)
	{
		this.iterable = iterable;
		this.function = function;
	}

	@Override
	public Iterator<T> iterator()
	{
		return new common.iterable.MappingIterator.From<>(iterable.iterator(), function);
	}

	public Stream<T> stream()
	{
		return StreamSupport.stream(spliterator(), false);
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