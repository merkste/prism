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
import explicit.PredecessorRelation;
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

	default BitSet computeNormalStates(M model, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated)
	{
		if (conditionNegated) {
			return computeProb0A(model, conditionRemain, conditionGoal);
		} else {
			return computeProb1A(model, conditionRemain, conditionGoal);
		}
// FIXME ALG: fishy: should be all states with Pmin=1 (Condition)
//		BitSet conditionWeakRemain   = getWeakRemainStates(model, conditionRemain, conditionGoal, conditionNegated);
//		BitSet conditionWeakGoal     = getWeakGoalStates(model, conditionRemain, conditionGoal, conditionNegated);
//		BitSet conditionNormalStates = computeProb1A(model, conditionWeakRemain, conditionWeakGoal);
//		return conditionNormalStates;
	}

	double[] computeUntilProbs(M model, BitSet remain, BitSet goal, boolean negated)
			throws PrismException;

	// FIXME ALG: code dupe in ResetConditionTransformer
	BitSet computeProb0A(M model, BitSet remain, BitSet goal);

	// FIXME ALG: code dupe in ResetConditionTransformer
	BitSet computeProb1A(M model, BitSet remain, BitSet goal);

	// FIXME ALG: code dupe in ConditionalReachabilityTransformer
	static double[] negateProbabilities(final double[] probabilities)
	{
		for (int state = 0; state < probabilities.length; state++) {
			probabilities[state] = 1 - probabilities[state];
		}
		return probabilities;
	}

//	// FIXME ALG: code dupe in ConditionalReachabilityTransformer
//	default BitSet getWeakGoalStates(M model, BitSet remain, BitSet goal, boolean negated)
//	{
//		if (! negated) {
//			return goal;
//		}
//		// terminal = ! (remain | goal)
//		int numStates = model.getNumStates();
//		if (goal == null || goal.cardinality() == numStates
//			|| remain == null || remain.cardinality() == numStates) {
//			return new BitSet();
//		}
//		BitSet terminals = BitSetTools.union(remain, goal);
//		terminals.flip(0, numStates);
//		return terminals;
//	}
//
//	default BitSet getWeakRemainStates(M model, BitSet remain, BitSet goal, boolean negated)
//	{
//		if (! negated) {
//			return remain;
//		}
//		// remain = ! goal
//		final int numStates = model.getNumStates();
//		if (goal == null || goal.cardinality() == numStates) {
//			return new BitSet();
//		}
//		return BitSetTools.complement(numStates, goal);
//	}



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

		// FIXME ALG: code dupe in ResetConditionTransformer
		@Override
		public BitSet computeProb0A(explicit.DTMC model, BitSet remain, BitSet goal)
		{
			PredecessorRelation pre = model.getPredecessorRelation(modelChecker, true);
			return modelChecker.prob0(model, remain, goal, pre);
		}

		// FIXME ALG: code dupe in ResetConditionTransformer
		@Override
		public BitSet computeProb1A(explicit.DTMC model, BitSet remain, BitSet goal)
		{
			PredecessorRelation pre = model.getPredecessorRelation(modelChecker, true);
			return modelChecker.prob1(model, remain, goal, pre);
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

		// FIXME ALG: code dupe in ResetConditionTransformer
		@Override
		public BitSet computeProb0A(explicit.MDP model, BitSet remain, BitSet goal)
		{
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return modelChecker.prob0(model, remain, goal, false, null, pre);
		}

		// FIXME ALG: code dupe in ResetConditionTransformer
		@Override
		public BitSet computeProb1A(explicit.MDP model, BitSet remain, BitSet goal)
		{
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return modelChecker.prob1(model, remain, goal, true, null, pre);
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