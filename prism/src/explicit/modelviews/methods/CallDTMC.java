package explicit.modelviews.methods;

import java.util.Iterator;
import java.util.Map.Entry;

import common.functions.PairMapping;
import common.functions.primitive.MappingFromInteger;
import common.methods.UnaryMethod;
import explicit.DTMC;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
public class CallDTMC
{
	private static final DTMCGetTransitionsIterator GET_TRANSITIONS_ITERATOR = new DTMCGetTransitionsIterator();

	public static DTMCGetTransitionsIterator getTransitionsIterator()
	{
		return GET_TRANSITIONS_ITERATOR;
	}

	public static final class DTMCGetTransitionsIterator
			implements PairMapping<DTMC, Integer, Iterator<Entry<Integer, Double>>>,
			UnaryMethod<DTMC, Integer, Iterator<Entry<Integer, Double>>>
	{
		@Override
		public MappingFromInteger<Iterator<Entry<Integer, Double>>> curry(final DTMC model)
		{
			return new MappingFromInteger<Iterator<Entry<Integer, Double>>>()
			{
				@Override
				public Iterator<Entry<Integer, Double>> apply(final int state)
				{
					return model.getTransitionsIterator(state);
				}
			};
		}

		@Override
		public Iterator<Entry<Integer, Double>> apply(final DTMC model, final Integer state)
		{
			return model.getTransitionsIterator(state);
		}
	}
}