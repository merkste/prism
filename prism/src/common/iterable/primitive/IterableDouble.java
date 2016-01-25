package common.iterable.primitive;

import java.util.PrimitiveIterator.OfDouble;

public interface IterableDouble extends Iterable<Double>
{
	@Override
	public OfDouble iterator();
}