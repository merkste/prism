package explicit.conditional.reset;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.IntPredicate;

import common.BitSetTools;
import common.functions.primitive.MappingInt;
import explicit.BasicModelTransformation;
import explicit.DiracDistribution;
import explicit.Model;
import explicit.modelviews.CTMCAlteredDistributions;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.MDPAdditionalChoices;
import prism.PrismComponent;
import prism.PrismException;

public interface ResetTransformer<M extends Model>
{
	public static final String RESET = "reset";
	public static final String SINGLE_STATE_OF_INTEREST = "expected a single state of interest";



	default ResetTransformation<M> transformModel(M model, BitSet states, BitSet statesOfInterest)
			throws PrismException
	{
		return transformModel(model, states::get, statesOfInterest);
	}

	default ResetTransformation<M> transformModel(M model, IntPredicate states, BitSet statesOfInterest)
			throws PrismException
	{
		checkStatesOfInterest(model, statesOfInterest);
		return transformModel(model, states, statesOfInterest.nextSetBit(0));
	}

	default ResetTransformation<M> transformModel(M model, IntPredicate states, int target) throws PrismException
	{
		M resetModel = addResetTransitions(model, states, RESET, target);
		return new ResetTransformation<>(model, resetModel, target, RESET);
	}

	public abstract M addResetTransitions(M model, IntPredicate states, Object action, int target);

	default MappingInt<Iterator<Entry<Integer, Double>>> getResetTransitions(IntPredicate states, int target)
	{
		DiracDistribution reset = new DiracDistribution(target);
		return state -> states.test(state) ? reset.iterator() : null;
	}

	public static <M extends Model> void checkStatesOfInterest(M model, BitSet statesOfInterest) throws PrismException
	{
		int numStates = (statesOfInterest == null) ? model.getNumStates() : statesOfInterest.cardinality();
		if(numStates != 1) {
			throw new PrismException(SINGLE_STATE_OF_INTEREST);
		}
	}



	public static class CTMC extends PrismComponent implements ResetTransformer<explicit.CTMC>
	{
		public CTMC(PrismComponent parent) {
			super(parent);
		}

		@Override
		public explicit.CTMC addResetTransitions(explicit.CTMC model, IntPredicate states, Object action, int target)
		{
			MappingInt<Iterator<Entry<Integer, Double>>> transitions = getResetTransitions(states, target);
			return new CTMCAlteredDistributions(model, transitions);
		}
	}



	public static class DTMC extends PrismComponent implements ResetTransformer<explicit.DTMC>
	{
		public DTMC(PrismComponent parent) {
			super(parent);
		}

		@Override
		public explicit.DTMC addResetTransitions(explicit.DTMC model, IntPredicate states, Object action, int target)
		{
			MappingInt<Iterator<Entry<Integer, Double>>> transitions = getResetTransitions(states, target);
			return new DTMCAlteredDistributions(model, transitions);
		}
	}



	public static class MDP extends PrismComponent implements ResetTransformer<explicit.MDP>
	{
		public MDP(PrismComponent parent)
		{
			super(parent);
		}

		@Override
		public explicit.MDP addResetTransitions(explicit.MDP model, IntPredicate states, Object action, int target)
		{
			MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = getResetChoices(states, target);
			MappingInt<List<Object>> actions = getResetActions(states, action);

			return new MDPAdditionalChoices(model, choices, actions);
		}

		public MappingInt<List<Iterator<Entry<Integer, Double>>>> getResetChoices(IntPredicate states, int target)
		{
			MappingInt<Iterator<Entry<Integer,Double>>> transitions = getResetTransitions(states, target);

			return transitions.andThen((Iterator<Entry<Integer, Double>> i) -> (i == null) ? null : Collections.singletonList(i));
		}

		public MappingInt<List<Object>> getResetActions(IntPredicate resetStates, Object action)
		{
			List<Object> resetActions = Collections.singletonList(action);

			return state -> resetStates.test(state) ? resetActions : null;
		}
	}



	public static class ResetTransformation<M extends Model> extends BasicModelTransformation<M, M>
	{
		protected final int targetState;
		protected final Object resetAction;

		public ResetTransformation(M originalModel, M transformedModel, int targetState, Object resetAction)
		{
			super(originalModel, transformedModel);
			this.targetState = targetState;
			this.resetAction = resetAction;
		}

		public ResetTransformation(ResetTransformation<? extends M> transformation)
		{
			super(transformation);
			this.targetState = transformation.targetState;
			this.resetAction = transformation.resetAction;
		}

		public int getTargetState()
		{
			return targetState;
		}

		public Object getResetAction()
		{
			return resetAction;
		}

		@Override
		public BitSet getTransformedStatesOfInterest()
		{
			return BitSetTools.asBitSet(targetState);
		}
	}
}