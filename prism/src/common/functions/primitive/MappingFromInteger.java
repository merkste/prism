package common.functions.primitive;

import java.util.function.IntFunction;

import common.functions.Mapping;

public interface MappingFromInteger<T> extends Mapping<Integer, T>, IntFunction<T>
{
	public T apply(int element);
}