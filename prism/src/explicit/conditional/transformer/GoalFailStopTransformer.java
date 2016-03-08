package explicit.conditional.transformer;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map.Entry;

import prism.PrismException;
import common.BitSetTools;
import common.functions.primitive.MappingInt;
import explicit.DTMCModelChecker;
import explicit.DiracDistribution;
import explicit.Distribution;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import explicit.Model;

public interface GoalFailStopTransformer<M extends Model> extends ConditionalNormalFormTransformer<M>
{
	public static final int FAIL = 1;
	public static final int STOP = 2;

	@Override
	default GoalFailStopTransformation<M> transformModel(final M model, final BitSet objectiveStates, final BitSet conditionStates) throws PrismException
	{
		return new GoalFailStopTransformation<>(ConditionalNormalFormTransformer.super.transformModel(model, objectiveStates, conditionStates));
	}

	@Override
	default public int getNumTrapStates()
	{
		return 3;
	};

	@Override
	default MappingInt<Iterator<Entry<Integer, Double>>> getTransitions(M model, BitSet objectiveStates, BitSet conditionStates) throws PrismException
	{
		// compute Pmax(<> Objective)
		final double[] objectiveProbs = computeReachProbs(model, objectiveStates);
		// compute Pmax(<> Condition)
		final double[] conditionProbs = computeReachProbs(model, conditionStates);

		return new MappingInt<Iterator<Entry<Integer, Double>>>()
		{
			final int offset = model.getNumStates();
			private final BinaryRedistribution conditionRedistribution =
					new BinaryRedistribution(conditionStates, offset + GOAL, offset + STOP, objectiveProbs);
			private final BinaryRedistribution objectiveRedistribution =
					new BinaryRedistribution(objectiveStates, offset + GOAL, offset + FAIL, conditionProbs);

			@Override
			public Iterator<Entry<Integer, Double>> apply(int state)
			{
				Iterator<Entry<Integer, Double>> distribution = conditionRedistribution.apply(state);
				if (distribution != null) {
					// condition state
					return distribution;
				}
				distribution = objectiveRedistribution.apply(state);
				if (distribution != null) {
					// objective\condition state
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




	public static final class DTMC extends ConditionalNormalFormTransformer.DTMC implements GoalFailStopTransformer<explicit.DTMC>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}



	public static final class MDP extends ConditionalNormalFormTransformer.MDP implements GoalFailStopTransformer<explicit.MDP>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		protected BitSet getTerminalStates(BitSet objectiveStates, BitSet conditionStates)
		{
			return BitSetTools.union(objectiveStates, conditionStates);
		}

	}



	public static class GoalFailStopTransformation<M extends Model> extends NormalFormTransformation<M>
	{
		public GoalFailStopTransformation(final M originalModel, final M transformedModel)
		{
			super(originalModel, transformedModel);
		}

		public GoalFailStopTransformation(final NormalFormTransformation<M> transformation)
		{
			super(transformation);
		}

		public int getFailState()
		{
			return numberOfStates + FAIL;
		}

		public int getStopState()
		{
			return numberOfStates + STOP;
		}
	}



	public static void main(final String[] args) throws PrismException
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

		final GoalFailStopTransformer.MDP transformer = new GoalFailStopTransformer.MDP(new MDPModelChecker(null));
		final BitSet objectiveStates = BitSetTools.asBitSet(1);
		final BitSet conditionStates = BitSetTools.asBitSet(2);
		final BitSet statesOfInterest = BitSetTools.asBitSet(1);

		System.out.println();

		System.out.println("Conditional Model, normal form, objectiveStates=" + objectiveStates + ", conditionStates=" + conditionStates + ", statesOfInterest="
				+ statesOfInterest + ":");
		final explicit.MDP transformed = transformer.transformModel(original, objectiveStates, conditionStates).getTransformedModel();
		System.out.print(transformed.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(transformed.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(transformed.getDeadlockStates()));
		System.out.println(transformed);
	}
}