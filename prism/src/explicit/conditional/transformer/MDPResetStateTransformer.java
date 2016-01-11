package explicit.conditional.transformer;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.functions.ArrayMapping;
import common.functions.primitive.MappingInt;
import explicit.BasicModelTransformation;
import explicit.MDP;
import explicit.MDPSimple;
import explicit.modelviews.MDPAdditionalChoices;
import explicit.modelviews.MDPDisjointUnion;
import parser.State;
import prism.PrismComponent;
import prism.PrismException;

public class MDPResetStateTransformer extends MDPResetTransformer
{
	public static final int FAIL = 0;

	public MDPResetStateTransformer(final PrismComponent parent)
	{
		super(parent);
	}

	public MDP buildTrapStatesModel(final MDP model, final int trapState)
	{
		final MDPSimple trapStatesModel = new MDPSimple(1);
		final List<State> statesList = buildStatesList(trapStatesModel.getNumStates(), model.getStatesList(), trapState);
		trapStatesModel.setStatesList(statesList);
		return trapStatesModel;
	}

	// FIXME ALG: duplicate in ConditionalNormalFormTransformer
	public List<State> buildStatesList(final int size, final List<State> statesList, final int prototype)
	{
		if (statesList == null) {
			return null;
		}
		final State[] states = new State[size];
		Arrays.fill(states, statesList.get(prototype));
		return Arrays.asList(states);
	}

	public BasicModelTransformation<MDP, MDP> transformModel(final MDP model, final BitSet resetStates, final int targetState) throws PrismException
	{
		final MDP failStateModel = buildTrapStatesModel(model, FAIL);
		final int failState = model.getNumStates();
		final MDPDisjointUnion unionModel = new MDPDisjointUnion(model, failStateModel);

		return transformModel(unionModel, resetStates, targetState, failState);
	}

	public BasicModelTransformation<MDP, MDP> transformModel(final MDP model, final BitSet resetStates, final int targetState, final int failState)
			throws PrismException
	{
		final MappingInt<List<Object>> actions = getActions(resetStates, failState);
		final MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = getChoices(resetStates, failState, targetState);
		final MDPAdditionalChoices resetModel = new MDPAdditionalChoices(model, choices, actions);

		final ArrayMapping<Integer> mapping = ArrayMapping.identity(model.getNumStates());
		return new BasicModelTransformation<MDP, MDP>(model, resetModel, mapping.getElements());
	}

	public MappingInt<List<Object>> getActions(final BitSet resetStates, final int failState)
	{
		List<Object> resetActions = Collections.singletonList((Object) "reset");
		MappingInt<List<Object>> failActions = getResetActions(resetStates, "fail");

		return state -> (state == failState) ? resetActions : failActions.apply(state);
	}

	public MappingInt<List<Iterator<Entry<Integer, Double>>>> getChoices(final BitSet resetStates, final int failState, final int initialState)
	{
		final Entry<Integer, Double> resetTransition = (Entry<Integer, Double>) new AbstractMap.SimpleImmutableEntry<Integer, Double>(initialState, 1.0);
		List<Iterator<Entry<Integer, Double>>> resetTransitions = Collections.singletonList(Collections.singleton(resetTransition).iterator());
		final MappingInt<List<Iterator<Entry<Integer, Double>>>> failChoices = getResetChoices(resetStates, failState);

		return state -> (state == failState) ? resetTransitions : failChoices.apply(state);
	}
}