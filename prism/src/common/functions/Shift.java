package common.functions;

import common.functions.primitive.MappingFromInteger;

/**
 * @deprecated
 * Use J8: x -> x + offset
 */
@Deprecated
public class Shift implements MappingFromInteger<Integer>
{
	private final int offset;

	public Shift(final int offset)
	{
		this.offset = offset;
	}

	@Override
	public final Integer apply(final int i)
	{
		return offset + i;
	}
}