package common.iterable;

import java.util.PrimitiveIterator.OfLong;
import java.util.function.LongConsumer;

public interface IterableLong extends Iterable<Long>, PrimitiveIterable<Long, LongConsumer>
{
	@Override
	public OfLong iterator();
}