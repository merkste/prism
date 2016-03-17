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
	static final int GOAL = 0;



	default NormalFormTransformation<M> transformModel(M model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, BitSet statesOfInterest)
			throws PrismException
	{
		return transformModel(model, objectiveGoal, conditionRemain, conditionGoal, false, statesOfInterest);
	}

	default NormalFormTransformation<M> transformModel(M model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated, BitSet statesOfInterest)
			throws PrismException
	{
		M trapStatesModel = addTrapStates(model, getNumTrapStates());
		M redirectedModel = normalizeTransitions(model, trapStatesModel, objectiveGoal, conditionRemain, conditionGoal, conditionNegated);

		return new NormalFormTransformation<>(model, redirectedModel, statesOfInterest);
	}

	M addTrapStates(M model, int numTrapStates);

	int getNumTrapStates();

	M normalizeTransitions(M model, M trapStatesModel, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated)
			throws PrismException;

	MappingInt<Iterator<Entry<Integer, Double>>> getTransitions(M model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated)
			throws PrismException;

	double[] computeUntilProbs(M model, BitSet remain, BitSet goal, boolean negated)
			throws PrismException;

	// FIXME ALG: code dupe in ConditionalReachabilityTransformer
	static double[] negateProbabilities(final double[] probabilities)
	{
		for (int state = 0; state < probabilities.length; state++) {
			probabilities[state] = 1 - probabilities[state];
		}
		return probabilities;
	}

	// FIXME ALG: code dupe in ConditionalReachabilityTransformer
	default BitSet getUntilTerminalStates(M model, BitSet remain, BitSet goal, boolean negated)
	{
		// terminal = ! (remain | goal)
		if (! negated) {
			return goal;
		}
		if (remain == null || remain.cardinality() == model.getNumStates()) {
			return new BitSet();
		}
		BitSet terminals = BitSetTools.union(remain, goal);
		terminals.flip(0, model.getNumStates());
		return terminals;
	}


	public abstract static class DTMC extends PrismComponent implements ConditionalNormalFormTransformer<explicit.DTMC>
	{
		protected final DTMCModelChecker modelChecker;

		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
			this.modelChecker = modelChecker;
		}

		@Override
		public explicit.DTMC addTrapStates(explicit.DTMC model, int numTrapStates)
		{
			return new DTMCAdditionalStates(model, numTrapStates);
		}

		@Override
		public explicit.DTMC normalizeTransitions(explicit.DTMC model, explicit.DTMC trapStatesModel, BitSet objectiveGoalStates, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated)
				throws PrismException
		{
			MappingInt<Iterator<Entry<Integer, Double>>> transitions = getTransitions(model, objectiveGoalStates, conditionRemain, conditionGoal, conditionNegated);
			return new DTMCAlteredDistributions(trapStatesModel, transitions);
		}

		@Override
		public double[] computeUntilProbs(explicit.DTMC model, BitSet remain, BitSet goal, boolean negated)
				throws PrismException
		{
			// FIXME ALG: consider precomputation
			double[] probs = modelChecker.computeUntilProbs(model, remain, goal).soln;
			if (negated) {
				return negateProbabilities(probs);
			}
			return probs;
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
		public explicit.MDP normalizeTransitions(explicit.MDP model, explicit.MDP trapStatesModel, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated)
				throws PrismException
		{
			// FIXME ALG: avoid duplicated computation of terminals
			MDPDroppedAllChoices dropped = new MDPDroppedAllChoices(trapStatesModel, getTerminalStates(model, objectiveGoal, conditionRemain, conditionGoal, conditionNegated));

			MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = getChoices(model, objectiveGoal, conditionRemain, conditionGoal, conditionNegated);
			MappingInt<List<Object>> actions = getActions(model, "normalize");
			return new MDPAdditionalChoices(dropped, choices, actions);
		}

		protected abstract BitSet getTerminalStates(explicit.MDP model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, boolean negated);

		protected MappingInt<List<Iterator<Entry<Integer, Double>>>> getChoices(explicit.MDP model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated)
				throws PrismException
		{
			MappingInt<Iterator<Entry<Integer,Double>>> transitions = getTransitions(model, objectiveGoal, conditionRemain, conditionGoal, conditionNegated);

			return transitions.andThen((Iterator<Entry<Integer, Double>> i) -> (i == null) ? null : Collections.singletonList(i));
		}

		protected MappingInt<List<Object>> getActions(explicit.MDP model, Object action)
		{
			int offset = model.getNumStates();
			List<Object> redirectActions = Collections.singletonList(action);

			return state -> (state < offset) ? redirectActions : null;
		}

		@Override
		public double[] computeUntilProbs(explicit.MDP model, BitSet remain, BitSet goal, boolean negated)
				throws PrismException
		{
			double[] probs = modelChecker.computeUntilProbs(model, remain, goal, negated).soln;
			if (negated) {
				probs = negateProbabilities(probs);
			}
			return probs;
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