package common.functions.primitive;

import common.functions.AbstractMapping;

/**
 * @deprecated
 * Use MappingFromInteger interface directly.
 */
@Deprecated
public abstract class AbstractMappingFromInteger<T> extends AbstractMapping<Integer, T>implements MappingFromInteger<T>
{
	public static <T> MappingFromInteger<T> constantFromInteger(final T value)
	{
		return MappingFromInteger.constantFromInteger(value);
	}
}