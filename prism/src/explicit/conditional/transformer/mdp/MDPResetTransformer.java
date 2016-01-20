package explicit.conditional.transformer.mdp;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.BitSetTools;
import common.functions.primitive.MappingInt;
import common.iterable.collections.MappingList;
import explicit.BasicModelTransformation;
import explicit.DiracDistribution;
import explicit.MDP;
import explicit.Model;
import explicit.modelviews.MDPAdditionalChoices;
import prism.PrismComponent;
import prism.PrismException;

public class MDPResetTransformer extends PrismComponent
{
	public static final String SINGLE_STATE_OF_INTEREST = "expected a single state of interest";

	public MDPResetTransformer(PrismComponent parent)
	{
		super(parent);
	}

	public ResetTransformation<MDP> transformModel(final MDP model, final BitSet resetStates, final BitSet statesOfInterest) throws PrismException
	{
		checkStatesOfInterest(statesOfInterest);
		return this.transformModel(model, resetStates, statesOfInterest.nextSetBit(0));
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
		DiracDistribution reset = new DiracDistribution(targetState);
		List<DiracDistribution> resetChoices = Collections.singletonList(reset);
		MappingList<DiracDistribution, Iterator<Entry<Integer,Double>>> resetIterators = new MappingList<>(resetChoices, Iterable::iterator);

		return state -> resetStates.get(state) ? resetIterators : null;
	}

	public MappingInt<List<Object>> getResetActions(final BitSet resetStates, final Object action)
	{
		List<Object> resetActions = Collections.singletonList((Object) action);
		List<Object> noActions = Collections.emptyList();

		return state -> resetStates.get(state) ? resetActions : noActions;
	}

	public static void checkStatesOfInterest(final BitSet statesOfInterest) throws PrismException
	{
		if(statesOfInterest.cardinality() != 1) {
			throw new PrismException(SINGLE_STATE_OF_INTEREST);
		}
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

		@Override
		public BitSet getTransformedStatesOfInterest()
		{
			return BitSetTools.asBitSet(targetState);
		}
	}
}