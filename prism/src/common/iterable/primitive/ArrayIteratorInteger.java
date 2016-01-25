package common.iterable.primitive;

import java.util.Arrays;
import java.util.PrimitiveIterator;

import common.IteratorTools;
import common.iterable.AbstractArrayIterator;

/**
 * @deprecated
 * Use J8: Arrays::stream
 */
@Deprecated
public class ArrayIteratorInteger extends AbstractArrayIterator<Integer>implements IteratorInteger
{

	@SafeVarargs
	public ArrayIteratorInteger(final int... elements)
	{
		this(0, elements.length, elements);
	}

	public ArrayIteratorInteger(final int fromIndex, final int toIndex, final int[] elements)
	{
		super(Arrays.stream(elements, fromIndex, toIndex).iterator());
	}

	@Override
	public int nextInt()
	{
		return ((PrimitiveIterator.OfInt) iterator).nextInt();
	}

	public static void main(final String[] args)
	{
		IteratorTools.printIterator("empty", new ArrayIteratorInteger());
		IteratorTools.printIterator("one element", new ArrayIteratorInteger(1));
		IteratorTools.printIterator("three elements", new ArrayIteratorInteger(1, 2, 3));
		IteratorTools.printIterator("second of three elements", new ArrayIteratorInteger(1, 2, new int[] { 1, 2, 3 }));
	}
}