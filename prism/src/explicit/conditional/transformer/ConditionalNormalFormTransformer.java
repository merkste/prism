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



	default NormalFormTransformation<M> transformModel(M model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, BitSet statesOfInterest)
			throws PrismException
	{
		M trapStatesModel = addTrapStates(model, getNumTrapStates());
		M redirectedModel = normalizeTransitions(model, trapStatesModel, objectiveGoal, conditionRemain, conditionGoal);

		return new NormalFormTransformation<>(model, redirectedModel, statesOfInterest);
	}

	public M addTrapStates(M model, int numTrapStates);

	public int getNumTrapStates();

	public M normalizeTransitions(M model, M trapStatesModel, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal)
			throws PrismException;

	public MappingInt<Iterator<Entry<Integer, Double>>> getTransitions(M model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal)
			throws PrismException;

	public double[] computeUntilProbs(M model, BitSet remain, BitSet goal)
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

		public double[] computeUntilProbs(explicit.DTMC model, BitSet remain, BitSet goal)
				throws PrismException
		{
			return modelChecker.computeUntilProbs(model, remain, goal).soln;
		}

		@Override
		public explicit.DTMC normalizeTransitions(explicit.DTMC model, explicit.DTMC trapStatesModel, BitSet objectiveGoalStates, BitSet conditionRemain, BitSet conditionGoal)
				throws PrismException
		{
			final MappingInt<Iterator<Entry<Integer, Double>>> transitions = getTransitions(model, objectiveGoalStates, conditionRemain, conditionGoal);
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
		public explicit.MDP normalizeTransitions(explicit.MDP model, explicit.MDP trapStatesModel, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal)
				throws PrismException
		{
			MDPDroppedAllChoices dropped = new MDPDroppedAllChoices(trapStatesModel, getTerminalStates(objectiveGoal, conditionGoal));

			MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = getChoices(model, objectiveGoal, conditionRemain, conditionGoal);
			MappingInt<List<Object>> actions = getActions(model, "normalize");
			return new MDPAdditionalChoices(dropped, choices, actions);
		}

		protected abstract BitSet getTerminalStates(BitSet objectiveGoal, BitSet conditionGoal);

		protected MappingInt<List<Iterator<Entry<Integer, Double>>>> getChoices(explicit.MDP model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal)
				throws PrismException
		{
			MappingInt<Iterator<Entry<Integer,Double>>> transitions = getTransitions(model, objectiveGoal, conditionRemain, conditionGoal);

			return transitions.andThen((Iterator<Entry<Integer, Double>> i) -> (i == null) ? null : Collections.singletonList(i));
		}

		protected MappingInt<List<Object>> getActions(explicit.MDP model, Object action)
		{
			int offset = model.getNumStates();
			List<Object> redirectActions = Collections.singletonList(action);

			return state -> (state < offset) ? redirectActions : null;
		}

		@Override
		public double[] computeUntilProbs(explicit.MDP model, BitSet remain, BitSet goal)
				throws PrismException
		{
			return modelChecker.computeUntilProbs(model, remain, goal, false).soln;
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