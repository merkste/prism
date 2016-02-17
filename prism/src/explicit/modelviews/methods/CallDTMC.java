package explicit.modelviews.methods;

import java.util.Iterator;
import java.util.Map.Entry;

import common.functions.AbstractPairMapping;
import common.functions.primitive.AbstractMappingFromInteger;
import common.functions.primitive.MappingFromInteger;
import common.methods.UnaryMethod;
import explicit.DTMC;

public class CallDTMC
{
	private static final DTMCGetTransitionsIterator GET_TRANSITIONS_ITERATOR = new DTMCGetTransitionsIterator();

	public static DTMCGetTransitionsIterator getTransitionsIterator()
	{
		return GET_TRANSITIONS_ITERATOR;
	}

	public static final class DTMCGetTransitionsIterator
			extends AbstractPairMapping<DTMC, Integer, Iterator<Entry<Integer, Double>>>
			implements UnaryMethod<DTMC, Integer, Iterator<Entry<Integer, Double>>>
	{
		@Override
		public MappingFromInteger<Iterator<Entry<Integer, Double>>> on(final DTMC model)
		{
			return curry(model);
		}

		@Override
		public MappingFromInteger<Iterator<Entry<Integer, Double>>> curry(final DTMC model)
		{
			return new AbstractMappingFromInteger<Iterator<Entry<Integer, Double>>>()
			{
				@Override
				public Iterator<Entry<Integer, Double>> get(final int state)
				{
					return model.getTransitionsIterator(state);
				}
			};
		}

		@Override
		public Iterator<Entry<Integer, Double>> get(final DTMC model, final Integer state)
		{
			return model.getTransitionsIterator(state);
		}
	}
}