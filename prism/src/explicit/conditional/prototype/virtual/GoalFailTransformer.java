package explicit.conditional.prototype.virtual;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map.Entry;

import common.BitSetTools;
import common.functions.primitive.MappingInt;
import explicit.DTMCModelChecker;
import explicit.DiracDistribution;
import explicit.Distribution;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import explicit.Model;
import prism.PrismException;

@Deprecated
public interface GoalFailTransformer<M extends Model> extends ConditionalNormalFormTransformer<M>
{
	static final int FAIL = 1;



	@Override
	default GoalFailTransformation<M> transformModel(M model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated, BitSet statesOfInterest)
			throws PrismException
	{
		return new GoalFailTransformation<>(ConditionalNormalFormTransformer.super.transformModel(model, objectiveGoal, conditionRemain, conditionGoal, conditionNegated, statesOfInterest));
	}

	@Override
	default public int getNumTrapStates()
	{
		return 2;
	};

	@Override
	default MappingInt<Iterator<Entry<Integer, Double>>> getTransitions(M model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated)
			throws PrismException
	{
		// compute normal states and enlarge set by prob1a
		BitSet objectiveNormalStates = computeProb1A(model, null, objectiveGoal);
		// compute Pmax(<> Condition)
		double[] conditionMaxProbs = computeUntilProbs(model, conditionRemain, conditionGoal, conditionNegated);

		return new MappingInt<Iterator<Entry<Integer, Double>>>()
		{
			final int offset = model.getNumStates();
			final BinaryRedistribution objectiveRedistribution =
					new BinaryRedistribution(objectiveNormalStates, offset + GOAL, offset + FAIL, conditionMaxProbs);

			@Override
			public Iterator<Entry<Integer, Double>> apply(int state)
			{
				Iterator<Entry<Integer, Double>> distribution = objectiveRedistribution.apply(state);
				if (distribution != null) {
					// objective state
					return distribution;
				}
				if (state >= offset) {
					// trap state
					return DiracDistribution.iterator(state);
				}
				// other model state
				return null;
			}
		};
	}



	public static class DTMC extends ConditionalNormalFormTransformer.DTMC implements GoalFailTransformer<explicit.DTMC>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}



	public static class MDP extends ConditionalNormalFormTransformer.MDP implements GoalFailTransformer<explicit.MDP>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		protected BitSet getTerminalStates(explicit.MDP model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated)
		{
			return objectiveGoal;
		}
	}



	public static class GoalFailTransformation<M extends Model> extends NormalFormTransformation<M>
	{
		public GoalFailTransformation(M originalModel, M transformedModel, BitSet transformedStatesOfInterest)
		{
			super(originalModel, transformedModel, transformedStatesOfInterest);
		}

		public GoalFailTransformation(NormalFormTransformation<? extends M> transformation)
		{
			super(transformation);
		}

		public int getFailState()
		{
			return numberOfStates + FAIL;
		}
	}



	public static void main(final String[] args)
			throws PrismException
	{
		final MDPSimple original = new MDPSimple(4);
		original.addInitialState(1);
		Distribution dist = new Distribution();
		dist.add(1, 0.1);
		dist.add(2, 0.2);
		dist.add(3, 0.7);
		original.addActionLabelledChoice(0, dist, "a");
		dist = new Distribution();
		dist.add(3, 1);
		original.addActionLabelledChoice(1, dist, "a");
		dist = new Distribution();
		dist.add(2, 0.1);
		dist.add(3, 0.9);
		original.addActionLabelledChoice(1, dist, "b");
		dist = new Distribution();
		dist.add(1, 0.1);
		dist.add(2, 0.2);
		dist.add(3, 0.7);
		original.addActionLabelledChoice(2, dist, "a");
		original.findDeadlocks(true);

		System.out.println("Original Model:");
		System.out.print(original.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original.getDeadlockStates()));
		System.out.println(original);
		System.out.println();

		final GoalFailTransformer.MDP transformer = new GoalFailTransformer.MDP(new MDPModelChecker(null));
		final BitSet objectiveGoal = BitSetTools.asBitSet(1);
		final BitSet conditionGoal = BitSetTools.asBitSet(2);
		final BitSet statesOfInterest = BitSetTools.asBitSet(1);

		System.out.println();

		System.out.println("Conditional Model, normal form, objectiveGoal=" + objectiveGoal + ", conditionGoal=" + conditionGoal + ", statesOfInterest="
				+ statesOfInterest + ":");
		final explicit.MDP transformed = transformer.transformModel(original, objectiveGoal, null, conditionGoal, false, statesOfInterest).getTransformedModel();
		System.out.print(transformed.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(transformed.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(transformed.getDeadlockStates()));
		System.out.println(transformed);
	}
}