package common.functions.primitive;

import common.functions.AbstractMapping;

public abstract class AbstractMappingFromInteger<T> extends AbstractMapping<Integer, T>implements MappingFromInteger<T>
{
	@Override
	public T get(final Integer element)
	{
		return get(element.intValue());
	}
}