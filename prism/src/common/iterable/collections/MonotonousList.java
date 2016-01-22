package common.iterable.collections;

import java.util.AbstractList;

/**
 * A monotonous list is an immutable list which elements are identical.
 *
 * @author Steffen
 * @param <T>
 * @deprecated Use: Collections::nCopies
 */
@Deprecated
public class MonotonousList<T> extends AbstractList<T>
{
	private final T element;
	private final int size;

	public MonotonousList(final T element, final int size)
	{
		if (size < 0) {
			throw new IllegalArgumentException("positive list size expected");
		}
		this.element = element;
		this.size = size;
	}

	@Override
	public T get(final int index)
	{
		if (0 <= index && index < size) {
			return element;
		}
		throw new IndexOutOfBoundsException();
	}

	@Override
	public int size()
	{
		return size;
	}
}