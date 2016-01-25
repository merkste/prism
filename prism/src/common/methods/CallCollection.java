package common.methods;

import java.util.Collection;

import common.functions.Mapping;
import common.functions.PairPredicate;
import common.functions.Predicate;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
@Deprecated
public class CallCollection
{
	@SuppressWarnings("rawtypes")
	private static final Contains CONTAINS = new Contains();
	private static final Size SIZE = new Size();

	@SuppressWarnings("unchecked")
	public static <T> Contains<T> contains()
	{
		return CONTAINS;
	}

	public static Size size()
	{
		return SIZE;
	}

	public static final class Contains<T> implements PairPredicate<Collection<T>, T>, UnaryMethod<Collection<T>, T, Boolean>
	{
		@Override
		public Predicate<T> curry(final Collection<T> collection)
		{
			return new Predicate<T>()
			{
				@Override
				public boolean test(final T element)
				{
					return collection.contains(element);
				}
			};
		}

		@Override
		public boolean test(final Collection<T> collection, final T element)
		{
			return collection.contains(element);
		}
	}

	public static final class Size implements Mapping<Collection<?>, Integer>, Method<Collection<?>, Integer>
	{
		@Override
		public Integer apply(final Collection<?> collection)
		{
			return collection.size();
		}
	}
}