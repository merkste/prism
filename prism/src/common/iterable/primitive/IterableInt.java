package common.iterable.primitive;

/**
 * @deprecated
 * Use J8: PrimitiveIterator.OfInt
 */
@Deprecated
public interface IterableInt extends Iterable<Integer>
{
	@Override
	public IteratorInt iterator();
}