package explicit.modelviews.methods;

import java.util.Iterator;
import java.util.Map.Entry;

import common.functions.AbstractTripleMapping;
import common.functions.PairMapping;
import common.methods.BinaryMethod;
import explicit.MDP;

public class CallMDP
{
	private static final MDPGetTransitionsIterator GET_TRANSITIONS_ITERATOR = new MDPGetTransitionsIterator();

	public static MDPGetTransitionsIterator getTransitionsIterator()
	{
		return GET_TRANSITIONS_ITERATOR;
	}

	public static final class MDPGetTransitionsIterator
			extends AbstractTripleMapping<MDP, Integer, Integer, Iterator<Entry<Integer, Double>>>
			implements BinaryMethod<MDP, Integer, Integer, Iterator<Entry<Integer, Double>>>
	{
		// FIXME ALG: consider primitive types
		public PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> on(final MDP model)
		{
			return this.curry(model);
		}

		@Override
		public Iterator<Entry<Integer, Double>> get(final MDP model, final Integer state, final Integer choice)
		{
			return model.getTransitionsIterator(state, choice);
		}
	}
}