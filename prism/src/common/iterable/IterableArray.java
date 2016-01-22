package common.iterable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

public class IterableArray<T> extends AbstractIterableArray<T>implements Iterable<T>
{
	protected final T[] elements;

	@SafeVarargs
	public IterableArray(final T... elements)
	{
		this(0, elements.length, elements);
	}

	@SafeVarargs
	public IterableArray(final int fromIndex, final int toIndex, final T... elements)
	{
		super(fromIndex, toIndex);
		this.elements = elements;
	}

	@Override
	public Iterator<T> iterator()
	{
		return stream().iterator();
	}

	@Override
	public Stream<T> stream()
	{
		return Arrays.stream(elements, fromIndex, toIndex);
	}
}