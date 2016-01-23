package common.methods;

import java.util.ArrayList;
import java.util.Collection;

import common.functions.AbstractMapping;
import common.functions.Mapping;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
@Deprecated
public class CallArrayList
{
	@SuppressWarnings("rawtypes")
	private static final Mapping CONSTRUCTOR = new Constructor();

	@SuppressWarnings("unchecked")
	public static final <E> Mapping<Collection<? extends E>, ArrayList<E>> constructor()
	{
		return (Mapping<Collection<? extends E>, ArrayList<E>>) CONSTRUCTOR;
	}

	@SuppressWarnings("rawtypes")
	public static final class Constructor extends AbstractMapping<Collection, ArrayList>
	{
		@SuppressWarnings("unchecked")
		@Override
		public ArrayList apply(final Collection collection)
		{
			return new ArrayList(collection);
		}
	}
}