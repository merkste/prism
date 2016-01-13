package explicit.conditional.transformer.mdp;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map.Entry;

import prism.PrismException;
import common.BitSetTools;
import common.functions.primitive.MappingInt;
import explicit.DiracDistribution;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import explicit.conditional.transformer.BinaryRedistribution;

public class GoalStopTransformer extends ConditionalNormalFormTransformer
{
	public static final int STOP = 1;

	public GoalStopTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker, 2);
	}

	@Override
	public GoalStopTransformation transformModel(final MDP model, final BitSet objectiveStates, final BitSet conditionStates) throws PrismException
	{
		return new GoalStopTransformation(super.transformModel(model, objectiveStates, conditionStates));
	}

	@Override
	protected BitSet getTerminalStates(final BitSet objectiveStates, final BitSet conditionStates)
	{
		return conditionStates;
	}

	@Override
	protected MappingInt<Iterator<Entry<Integer, Double>>> getDistributions(final MDP model, final BitSet objectiveStates, final BitSet conditionStates) throws PrismException
	{
		// compute Pmax(<> Objective)
		final double[] objectiveMaxProbs = modelChecker.computeReachProbs(model, objectiveStates, false).soln;

		return new MappingInt<Iterator<Entry<Integer, Double>>>()
		{
			final int offset = model.getNumStates();
			private final BinaryRedistribution conditionRedistribution = new BinaryRedistribution(conditionStates, offset + GOAL, offset + STOP, objectiveMaxProbs);

			@Override
			public Iterator<Entry<Integer, Double>> apply(int state)
			{
				final Iterator<Entry<Integer, Double>> distribution = conditionRedistribution.apply(state);
				if (distribution != null) {
					// condition state
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

		final GoalStopTransformer transformer = new GoalStopTransformer(new MDPModelChecker(null));
		final BitSet objectiveStates = BitSetTools.asBitSet(1);
		final BitSet conditionStates = BitSetTools.asBitSet(2);
		final BitSet statesOfInterest = BitSetTools.asBitSet(1);

		System.out.println();

		System.out.println("Conditional Model, normal form, objectiveStates=" + objectiveStates + ", conditionStates=" + conditionStates + ", statesOfInterest="
				+ statesOfInterest + ":");
		final MDP transformed = transformer.transformModel(original, objectiveStates, conditionStates).getTransformedModel();
		System.out.print(transformed.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(transformed.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(transformed.getDeadlockStates()));
		System.out.println(transformed);
	}



	public static class GoalStopTransformation extends NormalFormTransformation
	{
		public GoalStopTransformation(final MDP originalModel, final MDP transformedModel)
		{
			super(originalModel, transformedModel);
		}

		public GoalStopTransformation(final NormalFormTransformation transformation)
		{
			super(transformation);
		}

		public int getStopState()
		{
			return numberOfStates + STOP;
		}
	}
}