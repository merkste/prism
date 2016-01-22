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
public class IterableArrayInteger extends AbstractIterableArray<Integer>implements IterableInteger
{
	private final int[] elements;

	public IterableArrayInteger(final int[] elements)
	{
		this(0, elements.length, elements);
	}

	public IterableArrayInteger(final int fromIndex, final int toIndex, final int... elements)
	{
		super(fromIndex, toIndex);
		this.elements = elements;
	}

	@Override
	public ArrayIteratorInteger iterator()
	{
		return new ArrayIteratorInteger(fromIndex, toIndex, elements);
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