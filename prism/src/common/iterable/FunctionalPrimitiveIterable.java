package common.iterable;

public interface FunctionalPrimitiveIterable<T, T_CONS> extends FunctionalIterable<T>
{
	@Override
	public FunctionalPrimitiveIterator<T, T_CONS> iterator();

	@Override
	default boolean isEmpty()
	{
		return !iterator().hasNext();
	}

	
	default void forEach(T_CONS action)
	{
		iterator().forEachRemaining(action);
	}
}
