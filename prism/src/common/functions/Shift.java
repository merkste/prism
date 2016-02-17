package common.functions;

import common.functions.primitive.AbstractMappingFromInteger;

// FIXME ALG: consider sum operator abstraction
public class Shift extends AbstractMappingFromInteger<Integer>
{
	private final int offset;

	public Shift(final int offset)
	{
		this.offset = offset;
	}

	@Override
	public final Integer get(final int i)
	{
		return offset + i;
	}
}