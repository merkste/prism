package explicit.conditional;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.function.IntFunction;

import common.BitSetTools;
import common.functions.primitive.MappingInt;
import common.iterable.IterableArray;
import explicit.BasicModelTransformation;
import explicit.DiracDistribution;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.modelviews.CTMCAdditionalStates;
import explicit.modelviews.CTMCAlteredDistributions;
import explicit.modelviews.DTMCAdditionalStates;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.MDPAdditionalChoices;
import explicit.modelviews.MDPAdditionalStates;
import explicit.modelviews.MDPDroppedAllChoices;
import prism.PrismException;



public interface NewGoalFailStopTransformer<M extends Model>
{
	public static final int GOAL = 0;
	public static final int FAIL = 1;
	public static final int STOP = 2;



	default GoalFailStopTransformation<M> transformModel(M model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, BitSet instantGoalStates, BitSet instantFailStates, BitSet badStates, BitSet statesOfInterest)
			throws PrismException
	{
		int numStates = model.getNumStates();
		M trapStatesModel = addTrapStates(model, 3);
//		FIXME ALG: should normalizeStates return a ModelTransformation ???
		M normalizedModel = normalizeStates(trapStatesModel, goalFail, goalStop, stopFail, instantGoalStates, instantFailStates);

		String goalLabel = normalizedModel.addUniqueLabel("goal", BitSetTools.asBitSet(numStates + GOAL));
		String failLabel = normalizedModel.addUniqueLabel("fail", BitSetTools.asBitSet(numStates + FAIL));
		String stopLabel = normalizedModel.addUniqueLabel("stop", BitSetTools.asBitSet(numStates + STOP));

		badStates        = BitSetTools.minus(badStates, goalFail.getStates(), goalStop.getStates(), stopFail.getStates(), instantGoalStates, instantFailStates);
		String badLabel  = normalizedModel.addUniqueLabel("bad", badStates);

		return new GoalFailStopTransformation<>(model, normalizedModel, statesOfInterest, goalLabel, failLabel, stopLabel, badLabel);
	}

	default IntFunction<Iterator<Entry<Integer, Double>>> getTransitions(M model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, BitSet instantGoalStates, BitSet instantFailStates)
	{
		int numStates = model.getNumStates();
		int goalState = numStates - 3 + GOAL;
		int failState = numStates - 3 + FAIL;
		int stopState = numStates - 3 + STOP;
		DiracDistribution transToGoal = new DiracDistribution(goalState);
		DiracDistribution transToFail = new DiracDistribution(failState);
		DiracDistribution transToStop = new DiracDistribution(stopState);

		return new IntFunction<Iterator<Entry<Integer,Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(int state)
			{
				// FIXME ALG: ensure instantGoal/Fail do not intersect
				Iterable<Entry<Integer, Double>> transitions = null;
				if (instantGoalStates.get(state) || state == goalState) {
					return transToGoal.iterator();
				}
				if (instantFailStates.get(state) || state == failState) {
					return transToFail.iterator();
				}
				if (state == stopState) {
					return transToStop.iterator();
				}
				transitions = goalFail.getDistribution(state, goalState, failState);
				if (transitions != null) {
					return transitions.iterator();
				}
				transitions = goalStop.getDistribution(state, goalState, stopState);
				if (transitions != null) {
					return transitions.iterator();
				}
				transitions = stopFail.getDistribution(state, stopState, failState);
				if (transitions != null) {
					return transitions.iterator();
				}
				return null;
			}
		};
	}

	M addTrapStates(M model, int numTrapStates);

	M normalizeStates(M model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, BitSet instantGoalStates, BitSet instantFailStates);



	public static class CTMC implements NewGoalFailStopTransformer<explicit.CTMC>
	{

		@Override
		public CTMCAdditionalStates addTrapStates(explicit.CTMC model, int numTrapStates)
		{
			return new CTMCAdditionalStates(model, numTrapStates);
		}

		@Override
		public CTMCAlteredDistributions normalizeStates(explicit.CTMC model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, BitSet instantGoalStates, BitSet instantFailStates)
		{
			IntFunction<Iterator<Entry<Integer, Double>>> getTransitions = getTransitions(model, goalFail, goalStop, stopFail, instantGoalStates, instantFailStates);
			return new CTMCAlteredDistributions(model, getTransitions);
		}
	}



	public static class DTMC implements NewGoalFailStopTransformer<explicit.DTMC>
	{

		@Override
		public DTMCAdditionalStates addTrapStates(explicit.DTMC model, int numTrapStates)
		{
			return new DTMCAdditionalStates(model, numTrapStates);
		}

		@Override
		public DTMCAlteredDistributions normalizeStates(explicit.DTMC model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, BitSet instantGoalStates, BitSet instantFailStates)
		{
			IntFunction<Iterator<Entry<Integer, Double>>> getTransitions = getTransitions(model, goalFail, goalStop, stopFail, instantGoalStates, instantFailStates);
			return new DTMCAlteredDistributions(model, getTransitions);
		}
	}



	public static class MDP implements NewGoalFailStopTransformer<explicit.MDP>
	{
		@Override
		public MDPAdditionalStates addTrapStates(explicit.MDP model, int numTrapStates)
		{
			return new MDPAdditionalStates(model, numTrapStates);
		}

		@Override
		public MDPAdditionalChoices normalizeStates(explicit.MDP model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, BitSet instantGoalStates, BitSet instantFailStates)
		{
			BitSet normalize     = BitSetTools.union(goalFail.getStates(), goalStop.getStates(), stopFail.getStates(), instantGoalStates, instantFailStates);
			explicit.MDP dropped = new MDPDroppedAllChoices(model, normalize);

			IntFunction<List<Iterator<Entry<Integer, Double>>>> getChoices = getChoices(model, goalFail, goalStop, stopFail, instantGoalStates, instantFailStates);
			IntFunction<List<Object>> getActions                           = getActions(model, "normalize");
			return new MDPAdditionalChoices(dropped, getChoices, getActions);	
		}

		protected IntFunction<List<Iterator<Entry<Integer, Double>>>> getChoices(explicit.MDP model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, BitSet instantGoalStates, BitSet instantFailStates)
		{
			IntFunction<Iterator<Entry<Integer, Double>>> getTransitions = getTransitions(model, goalFail, goalStop, stopFail, instantGoalStates, instantFailStates);
			return new IntFunction<List<Iterator<Entry<Integer,Double>>>>()
			{
				@Override
				public List<Iterator<Entry<Integer, Double>>> apply(int state)
				{
					Iterator<Entry<Integer, Double>> transitions = getTransitions.apply(state); 
					return transitions == null ? null : Collections.singletonList(transitions);
				}
			};
		}

		protected MappingInt<List<Object>> getActions(explicit.MDP model, Object action)
		{
			int offset = model.getNumStates();
			List<Object> redirectActions = Collections.singletonList(action);

			return state -> (state < offset) ? redirectActions : null;
		}
	}



	public static class ProbabilisticRedistribution
	{
		protected BitSet states;
		protected double[] probabilities; 

		public ProbabilisticRedistribution()
		{
			// FIXME ALG: check whether we use new double[0], new double[model.getNumState()] or null
			this(new BitSet(0), new double[0]);
		}

		public ProbabilisticRedistribution(BitSet states, double[] probabilities)
		{
			Objects.requireNonNull(states);
			Objects.requireNonNull(probabilities);
			this.states        = states;
			this.probabilities = probabilities;
		}

		public BitSet getStates()
		{
			return states;
		}

		public double[] getProbabilities()
		{
			return probabilities;
		}

		public Iterable<Entry<Integer,Double>> getDistribution(int from, int toA, int toB)
		{
			if (! states.get(from)) {
				return null;
			}
			double probabilityToA = probabilities[from];
			if (probabilityToA == 1.0) {
				return new DiracDistribution(toA);
			}
			if (probabilityToA == 0.0) {
				return new DiracDistribution(toB);
			}
			Entry<Integer, Double> transitionA = new SimpleImmutableEntry<>(toA, probabilityToA);
			Entry<Integer, Double> transitionB = new SimpleImmutableEntry<>(toB, 1.0 - probabilityToA);
			return new IterableArray.Of<>(transitionA, transitionB);
		}

		public ProbabilisticRedistribution swap()
		{
			// inverse probabilities to swap target states
			probabilities = NewConditionalTransformer.Basic.subtractFromOne(probabilities);
			return this;
		}
	}

	public static class GoalFailStopTransformation<M extends Model> extends BasicModelTransformation<M, M>
	{
		protected final String goalLabel;
		protected final String failLabel;
		protected final String stopLabel;
		protected final String badLabel;

		public GoalFailStopTransformation(M originalModel, M transformedModel, BitSet transformedStatesOfInterest, String goalLabel, String failLabel, String stopLabel, String badLabel)
		{
			super(originalModel, transformedModel,transformedStatesOfInterest);
			this.goalLabel      = goalLabel;
			this.failLabel      = failLabel;
			this.stopLabel      = stopLabel;
			this.badLabel       = badLabel;
			checkLabels();
		}

		public GoalFailStopTransformation(ModelTransformation<? extends M, ? extends M> transformation, String goalLabel, String failLabel, String stopLabel, String badLabel)
		{
			super(transformation);
			this.goalLabel      = goalLabel;
			this.failLabel      = failLabel;
			this.stopLabel      = stopLabel;
			this.badLabel       = badLabel;
		}

		public GoalFailStopTransformation(GoalFailStopTransformation<? extends M> transformation)
		{
			super(transformation);
			this.goalLabel       = transformation.goalLabel;
			this.failLabel       = transformation.failLabel;
			this.stopLabel       = transformation.stopLabel;
			this.badLabel        = transformation.badLabel;
		}

		public void checkLabels() {
			Objects.requireNonNull(this.transformedModel.getLabelStates(this.goalLabel));
			Objects.requireNonNull(this.transformedModel.getLabelStates(this.failLabel));
			Objects.requireNonNull(this.transformedModel.getLabelStates(this.stopLabel));
			Objects.requireNonNull(this.transformedModel.getLabelStates(this.badLabel));
		}

		public String getGoalLabel()
		{
			return goalLabel;
		}

		public String getFailLabel()
		{
			return failLabel;
		}

		public String getStopLabel()
		{
			return stopLabel;
		}

		public String getBadLabel()
		{
			return badLabel;
		}

		public GoalFailStopTransformation<M> chain(ModelTransformation<? extends M, ? extends M> inner)
		{
			return new GoalFailStopTransformation<M>(nest(inner), goalLabel, failLabel, stopLabel, badLabel);
		}
	}
}
