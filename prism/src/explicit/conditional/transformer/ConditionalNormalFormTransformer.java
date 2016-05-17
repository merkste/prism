package explicit.conditional.transformer;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.BitSetTools;
import common.functions.primitive.MappingInt;
import explicit.BasicModelTransformation;
import explicit.DTMCModelChecker;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.modelviews.DTMCAdditionalStates;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.MDPAdditionalChoices;
import explicit.modelviews.MDPAdditionalStates;
import explicit.modelviews.MDPDroppedAllChoices;
import prism.PrismComponent;
import prism.PrismException;

public interface ConditionalNormalFormTransformer<M extends Model>
{
	public static final int GOAL = 0;



	default NormalFormTransformation<M> transformModel(M model, BitSet objectiveStates, BitSet conditionStates, BitSet statesOfInterest)
			throws PrismException
	{
		M trapStatesModel = addTrapStates(model, getNumTrapStates());
		M redirectedModel = normalizeTransitions(model, trapStatesModel, objectiveStates, conditionStates);

		return new NormalFormTransformation<>(model, redirectedModel, statesOfInterest);
	}

	public M addTrapStates(M model, int numTrapStates);

	public int getNumTrapStates();

	public M normalizeTransitions(M model, M trapStatesModel, BitSet objectiveStates, BitSet conditionStates)
			throws PrismException;

	public MappingInt<Iterator<Entry<Integer, Double>>> getTransitions(M model, BitSet objectiveStates, BitSet conditionStates)
			throws PrismException;

	public double[] computeReachProbs(M model, BitSet goal)
			throws PrismException;



	public abstract static class DTMC extends PrismComponent implements ConditionalNormalFormTransformer<explicit.DTMC>
	{
		protected final DTMCModelChecker modelChecker;

		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
			this.modelChecker = modelChecker;
		}

		public explicit.DTMC addTrapStates(explicit.DTMC model, int numTrapStates)
		{
			return new DTMCAdditionalStates(model, numTrapStates);
		}

		public double[] computeReachProbs(explicit.DTMC model, BitSet goal)
				throws PrismException
		{
			return modelChecker.computeReachProbs(model, goal).soln;
		}

		@Override
		public explicit.DTMC normalizeTransitions(explicit.DTMC model, explicit.DTMC trapStatesModel, BitSet objectiveStates, BitSet conditionStates)
				throws PrismException
		{
			final MappingInt<Iterator<Entry<Integer, Double>>> transitions = getTransitions(model, objectiveStates, conditionStates);
			return new DTMCAlteredDistributions(trapStatesModel, transitions);
		}
	}



	public static abstract class MDP extends PrismComponent implements ConditionalNormalFormTransformer<explicit.MDP>
	{
		protected final MDPModelChecker modelChecker;

		public MDP(MDPModelChecker modelChecker)
		{
			this.modelChecker = modelChecker;
		}

		@Override
		public explicit.MDP addTrapStates(explicit.MDP model, int numTrapStates)
		{
			return new MDPAdditionalStates(model, numTrapStates);
		}

		@Override
		public explicit.MDP normalizeTransitions(explicit.MDP model, explicit.MDP trapStatesModel, BitSet objectiveStates, BitSet conditionStates)
				throws PrismException
		{
			MDPDroppedAllChoices dropped = new MDPDroppedAllChoices(trapStatesModel, getTerminalStates(objectiveStates, conditionStates));

			MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = getChoices(model, objectiveStates, conditionStates);
			MappingInt<List<Object>> actions = getActions(model, "normalize");
			return new MDPAdditionalChoices(dropped, choices, actions);
		}

		protected abstract BitSet getTerminalStates(BitSet objectiveStates, BitSet conditionStates);

		protected MappingInt<List<Iterator<Entry<Integer, Double>>>> getChoices(explicit.MDP model, BitSet objectiveStates, BitSet conditionStates)
				throws PrismException
		{
			MappingInt<Iterator<Entry<Integer,Double>>> transitions = getTransitions(model, objectiveStates, conditionStates);

			return transitions.andThen((Iterator<Entry<Integer, Double>> i) -> (i == null) ? null : Collections.singletonList(i));
		}

		protected MappingInt<List<Object>> getActions(explicit.MDP model, Object action)
		{
			int offset = model.getNumStates();
			List<Object> redirectActions = Collections.singletonList(action);

			return state -> (state < offset) ? redirectActions : null;
		}

		@Override
		public double[] computeReachProbs(explicit.MDP model, BitSet goal)
				throws PrismException
		{
			return modelChecker.computeReachProbs(model, goal, false).soln;
		}
	}



	public static class NormalFormTransformation<M extends Model> extends BasicModelTransformation<M, M> implements ReachabilityTransformation<M, M>
	{
		public NormalFormTransformation(M originalModel, M transformedModel, BitSet transformedStatesOfInterest)
		{
			super(originalModel, transformedModel, transformedStatesOfInterest);
		}

		public NormalFormTransformation(NormalFormTransformation<? extends M> transformation)
		{
			super(transformation);
		}

		@Override
		public BitSet getGoalStates()
		{
			return BitSetTools.asBitSet(getGoalState());
		}

		public int getGoalState()
		{
			return numberOfStates + GOAL;
		}
	}
}