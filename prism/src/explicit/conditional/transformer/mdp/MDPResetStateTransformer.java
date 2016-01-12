package explicit.conditional.transformer.mdp;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.functions.primitive.MappingInt;
import common.iterable.collections.MappingList;
import explicit.MDP;
import explicit.Model;
import explicit.modelviews.MDPAdditionalChoices;
import explicit.modelviews.MDPAdditionalStates;
import prism.PrismComponent;
import prism.PrismException;

public class MDPResetStateTransformer extends MDPResetTransformer
{
	public MDPResetStateTransformer(final PrismComponent parent)
	{
		super(parent);
	}

	@Override
	public ResetStateTransformation<MDP> transformModel(final MDP model, final BitSet resetStates, final int targetState) throws PrismException
	{
		final MDP resetStateModel = new MDPAdditionalStates(model, 1);
		final int resetState = model.getNumStates();

		final MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = getChoices(resetStates, resetState, targetState);
		final MappingInt<List<Object>> actions = getActions(resetStates, resetState);
		final MDPAdditionalChoices resetModel = new MDPAdditionalChoices(resetStateModel, choices, actions);

		return new ResetStateTransformation<MDP>(model, resetModel, resetState, targetState);
	}

	public MappingInt<List<Iterator<Entry<Integer, Double>>>> getChoices(final BitSet resetStates, final int resetState, final int targetState)
	{
		Iterable<Entry<Integer, Double>> resetTransitions = Collections.singleton(new AbstractMap.SimpleImmutableEntry<>(targetState, 1.0));
		List<Iterable<Entry<Integer, Double>>> resetChoices = Collections.singletonList(resetTransitions);
		MappingList<Iterable<Entry<Integer,Double>>,Iterator<Entry<Integer,Double>>> resetChoicesToIterators = new MappingList<>(resetChoices, Iterable::iterator);
		MappingInt<List<Iterator<Entry<Integer, Double>>>> redirectChoices = getResetChoices(resetStates, resetState);

		return state -> (state == resetState)
				? resetChoicesToIterators
				: redirectChoices.apply(state);
	}

	public MappingInt<List<Object>> getActions(final BitSet resetStates, final int resetState)
	{
		List<Object> resetActions = Collections.singletonList((Object) "reset");
		MappingInt<List<Object>> redirectActions = getResetActions(resetStates, "redirect");
	
		return state -> (state == resetState) ? resetActions : redirectActions.apply(state);
	}



	public static class ResetStateTransformation<M extends Model> extends ResetTransformation<M>
	{
		private final int resetState;

		public ResetStateTransformation(final M originalModel, final M transformedModel, final int resetState, final int targetState)
		{
			super(originalModel, transformedModel, targetState);
			this.resetState = resetState;
		}

		public ResetStateTransformation(final ResetStateTransformation<M> transformation)
		{
			super(transformation);
			this.resetState = transformation.resetState;
		}

		public int getResetState()
		{
			return resetState;
		}
	}
}