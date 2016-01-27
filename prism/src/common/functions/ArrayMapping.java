package common.functions;

import common.functions.primitive.MappingInt;

public class ArrayMapping<T> implements MappingInt<T>
{
	private final T[] elements;

	@SafeVarargs
	public ArrayMapping(T... elements)
	{
		this.elements = elements;
	}

	@Override
	public T apply(int index)
	{
		return elements[index];
	}

	@Override
	public ArrayMapping<T> memoize()
	{
		// an array mapping is efficiently memoized by its backing array
		return this;
	}

	public T[] getElements()
	{
		return elements;
	}

	public static ArrayMapping<Integer> identity(int length)
	{
		Integer[] elements = new Integer[length];
		for (int index = 0; index < length; index++) {
			elements[index] = index;
		}
		return new ArrayMapping<>(elements);
	}
}