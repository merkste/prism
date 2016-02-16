package common.iterable;

import java.util.PrimitiveIterator.OfInt;
import java.util.function.IntConsumer;

public interface IterableInt extends Iterable<Integer>, PrimitiveIterable<Integer, IntConsumer>
{
	@Override
	public OfInt iterator();
}