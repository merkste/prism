package common.functions.primitive;

import java.util.function.IntFunction;

import common.functions.Mapping;

public interface MappingFromInteger<T> extends Mapping<Integer, T>, IntFunction<T>
{
	public T apply(int element);

	@Override
	default T apply(final Integer element)
	{
		return apply(element.intValue());
	}

	public static <T> MappingFromInteger<T> constantFromInteger(final T value)
	{
		return new MappingFromInteger<T>() {
			@Override
			public T apply(final int element)
			{
				return value;
			}
		};
	}
}