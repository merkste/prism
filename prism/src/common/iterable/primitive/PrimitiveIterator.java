package common.iterable.primitive;

import java.util.Iterator;

/**
 * Marker interface for specialized iterators over collections of primitive data types.
 * 
 * @param <T> T supposed to be a container type of a primitive data type. 
 * @deprecated
 * Use J8: PrimitiveIterator
 */
@Deprecated
public interface PrimitiveIterator<T> extends Iterator<T>
{

}