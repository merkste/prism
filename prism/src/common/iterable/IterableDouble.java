package common.iterable;

import java.util.PrimitiveIterator.OfDouble;
import java.util.function.DoubleConsumer;

public interface IterableDouble extends Iterable<Double>, PrimitiveIterable<Double, DoubleConsumer>
{
	@Override
	public OfDouble iterator();
}