package common.methods;

import java.util.Arrays;

import common.functions.Mapping;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
@Deprecated
public class CallArrays
{
	public static final class Static
	{
		@SuppressWarnings("rawtypes")
		private static final Mapping COPY_OF = new CopyOf();

		@SuppressWarnings("unchecked")
		public static <T> Mapping<T[], T[]> copyOf()
		{
			return COPY_OF;
		}

		public static final class CopyOf implements Mapping<Object[], Object[]>
		{
			@Override
			public Object[] apply(final Object[] array)
			{
				return Arrays.copyOf(array, array.length);
			}
		}
	}
}