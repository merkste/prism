package common.iterable;

import java.util.PrimitiveIterator;

public interface PrimitiveIterable<T, T_CONS>
{
	public PrimitiveIterator<T, T_CONS> iterator();
}
