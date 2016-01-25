package common.methods;

import common.functions.Mapping;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
@Deprecated
public class CallIterable
{
	@SuppressWarnings("rawtypes")
	private static final Mapping ITERATOR = new Iterator();

	@SuppressWarnings("unchecked")
	public static <T> Mapping<Iterable<? extends T>, java.util.Iterator<T>> iterator()
	{
		return (Mapping<Iterable<? extends T>, java.util.Iterator<T>>) ITERATOR;
	}

	@SuppressWarnings("rawtypes")
	public static final class Iterator implements Mapping<Iterable, java.util.Iterator>, Method<Iterable, java.util.Iterator>
	{
		@Override
		public java.util.Iterator apply(final Iterable iterable)
		{
			return iterable.iterator();
		}
	}
}