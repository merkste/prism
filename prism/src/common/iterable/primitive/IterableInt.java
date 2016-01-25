package common.iterable.primitive;

import java.util.PrimitiveIterator.OfInt;

public interface IterableInt extends Iterable<Integer>
{
	@Override
	public OfInt iterator();
}