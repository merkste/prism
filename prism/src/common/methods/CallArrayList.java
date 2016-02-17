package common.methods;

import java.util.ArrayList;
import java.util.Collection;

import common.functions.AbstractMapping;
import common.functions.Mapping;

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
		public ArrayList get(final Collection collection)
		{
			return new ArrayList(collection);
		}
	}
}