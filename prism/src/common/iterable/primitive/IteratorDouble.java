package common.iterable.primitive;

/**
 * @deprecated
 * Use J8: PrimitiveIterator.OfDouble
 */
@Deprecated
public interface IteratorDouble extends PrimitiveIterator<Double>, java.util.PrimitiveIterator.OfDouble
{
	public double nextDouble();
}