package explicit.conditional.transformer.mdp;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.functions.primitive.MappingInt;
import common.iterable.collections.MappingList;
import explicit.BasicModelTransformation;
import explicit.MDP;
import explicit.Model;
import explicit.modelviews.MDPAdditionalChoices;
import prism.PrismComponent;
import prism.PrismException;

public class MDPResetTransformer extends PrismComponent
{
	public MDPResetTransformer(PrismComponent parent)
	{
		super(parent);
	}

	public ResetTransformation<MDP> transformModel(final MDP model, final BitSet resetStates, final int targetState) throws PrismException
	{
		MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = getResetChoices(resetStates, targetState);
		MappingInt<List<Object>> actions = getResetActions(resetStates, "reset");
		MDPAdditionalChoices resetModel = new MDPAdditionalChoices(model, choices, actions);

		return new ResetTransformation<MDP>(model, resetModel, targetState);
	}

	public MappingInt<List<Iterator<Entry<Integer, Double>>>> getResetChoices(final BitSet resetStates, final int targetState)
	{
		Iterable<Entry<Integer, Double>> resetTransitions = Collections.singleton(new AbstractMap.SimpleImmutableEntry<>(targetState, 1.0));
		List<Iterable<Entry<Integer, Double>>> resetChoices = Collections.singletonList(resetTransitions);
		MappingList<Iterable<Entry<Integer,Double>>,Iterator<Entry<Integer,Double>>> resetChoicesToIterators = new MappingList<>(resetChoices, Iterable::iterator);

		return state -> resetStates.get(state) ? resetChoicesToIterators : null;
	}

	public MappingInt<List<Object>> getResetActions(final BitSet resetStates, final Object action)
	{
		List<Object> resetActions = Collections.singletonList((Object) action);
		List<Object> noActions = Collections.emptyList();

		return state -> resetStates.get(state) ? resetActions : noActions;
	}



	public static class ResetTransformation<M extends Model> extends BasicModelTransformation<M, M>
	{
		private final int targetState;

		public ResetTransformation(final M originalModel, final M transformedModel, final int targetState)
		{
			super(originalModel, transformedModel);
			this.targetState = targetState;
		}

		public ResetTransformation(final ResetTransformation<M> transformation)
		{
			super(transformation);
			this.targetState = transformation.targetState;
		}

		public int getTargetState()
		{
			return targetState;
		}
	}
}