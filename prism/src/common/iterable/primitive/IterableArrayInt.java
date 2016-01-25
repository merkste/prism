package common.iterable.primitive;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import common.iterable.AbstractIterableArray;

/**
 * @deprecated
 * Use J8: Arrays::stream
 */
@Deprecated
public class IterableArrayInt extends AbstractIterableArray<Integer> implements IterableInt
{
	private final int[] elements;

	public IterableArrayInt(final int[] elements)
	{
		this(0, elements.length, elements);
	}

	public IterableArrayInt(final int fromIndex, final int toIndex, final int... elements)
	{
		super(fromIndex, toIndex);
		this.elements = elements;
	}

	@Override
	public ArrayIteratorInt iterator()
	{
		return new ArrayIteratorInt(fromIndex, toIndex, elements);
	}

	@Override
	public Stream<Integer> stream()
	{
		return primitiveStream().boxed();
	}

	protected IntStream primitiveStream()
	{
		return Arrays.stream(elements, fromIndex, toIndex);
	}
}