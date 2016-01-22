package common.iterable.primitive;

/**
 * @deprecated
 * Use J8: PrimitiveIterator.OfInt
 */
@Deprecated
public interface IterableInteger extends Iterable<Integer>
{
	@Override
	public IteratorInteger iterator();
}