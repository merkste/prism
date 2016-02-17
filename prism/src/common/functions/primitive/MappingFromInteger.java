package common.functions.primitive;

import common.functions.Mapping;

public interface MappingFromInteger<T> extends Mapping<Integer, T>
{
	public T get(final int element);
}