package explicit.conditional.transformer;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.functions.primitive.MappingInt;
import explicit.BasicModelTransformation;
import explicit.MDP;
import explicit.modelviews.MDPAdditionalChoices;
import prism.PrismComponent;
import prism.PrismException;

public class MDPResetTransformer extends PrismComponent
{
	public MDPResetTransformer(PrismComponent parent)
	{
		super(parent);
	}

	public BasicModelTransformation<MDP, MDP> transformModel(final MDP model, final BitSet resetStates, final int targetState) throws PrismException
	{
		MappingInt<List<Object>> actions = getResetActions(resetStates, "reset");
		MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = getResetChoices(resetStates, targetState);
		MDPAdditionalChoices resetModel = new MDPAdditionalChoices(model, choices, actions);

		return new BasicModelTransformation<MDP, MDP>(model, resetModel);
	}

	public MappingInt<List<Object>> getResetActions(final BitSet resetStates, final Object action)
	{
		List<Object> resetActions = Collections.singletonList((Object) action);
		List<Object> noActions = Collections.emptyList();

		return state -> resetStates.get(state) ? resetActions : noActions;
	}

	public MappingInt<List<Iterator<Entry<Integer, Double>>>> getResetChoices(final BitSet resetStates, final int initialState)
	{
		Entry<Integer, Double> resetTransition = new AbstractMap.SimpleImmutableEntry<Integer, Double>(initialState, 1.0);
		List<Iterator<Entry<Integer, Double>>> resetTransitions = Collections.singletonList(Collections.singleton(resetTransition).iterator());
		List<Iterator<Entry<Integer, Double>>> noTransitions = Collections.emptyList();

		return state -> resetStates.get(state) ? resetTransitions : noTransitions;
	}
}