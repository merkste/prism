package common.iterable.primitive;

/**
 * @deprecated
 * Use J8: PrimitiveIterator.OfInt
 */
@Deprecated
public interface IteratorInteger extends PrimitiveIterator<Integer>, java.util.PrimitiveIterator.OfInt
{
	public int nextInt();
}