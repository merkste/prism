package common.iterable.primitive;

/**
 * @deprecated
 * Use J8: PrimitiveIterator.OfDouble
 */
@Deprecated
public interface IterableDouble extends Iterable<Double>
{
	@Override
	public IteratorDouble iterator();
}