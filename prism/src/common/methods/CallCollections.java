package common.methods;

import java.util.Collections;
import java.util.List;

import common.functions.AbstractMapping;
import common.functions.Mapping;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
@Deprecated
public class CallCollections
{
	public static final class Static
	{
		@SuppressWarnings("rawtypes")
		private static final Mapping SINGLETON_LIST = new SingletonList();

		@SuppressWarnings("unchecked")
		public static <T> Mapping<T, List<T>> singletonList()
		{
			return SINGLETON_LIST;
		}

		public static final class SingletonList extends AbstractMapping<Object, List<Object>>
		{
			@Override
			public List<Object> apply(final Object object)
			{
				return Collections.singletonList(object);
			}
		}
	}
}