package common.iterable;

import java.util.PrimitiveIterator;

public interface PrimitiveIterable<T, T_CONS>
{
	public PrimitiveIterator<T, T_CONS> iterator();

	default boolean isEmpty()
	{
		return !iterator().hasNext();
	}

	default void forEach(T_CONS action)
	{
		iterator().forEachRemaining(action);
	}
}
