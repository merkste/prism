package common.functions.primitive;

import common.functions.AbstractMapping;

public abstract class AbstractMappingFromInteger<T> extends AbstractMapping<Integer, T>implements MappingFromInteger<T>
{
	@Override
	public T apply(final Integer element)
	{
		return apply(element.intValue());
	}

	public static <T> MappingFromInteger<T> constantFromInteger(final T value)
	{
		return new AbstractMappingFromInteger<T>() {
			@Override
			public T apply(final int element)
			{
				return value;
			}
		};
	}
}