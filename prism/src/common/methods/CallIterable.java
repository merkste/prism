package common.methods;

import common.functions.AbstractMapping;
import common.functions.Mapping;

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
	public static final class Iterator extends AbstractMapping<Iterable, java.util.Iterator> implements Method<Iterable, java.util.Iterator>
	{
		@Override
		public java.util.Iterator on(final Iterable iterable)
		{
			return iterable.iterator();
		}

		@Override
		public java.util.Iterator get(final Iterable iterable)
		{
			return iterable.iterator();
		}
	}
}