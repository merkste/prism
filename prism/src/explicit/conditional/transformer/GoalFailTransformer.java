package explicit.conditional.transformer;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.BitSetTools;
import common.functions.primitive.MappingInt;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import prism.PrismException;

public class GoalFailTransformer extends ConditionalNormalFormTransformer
{
	public static final int FAIL = 1;

	public GoalFailTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker, 2);
	}

	@Override
	public GoalFailTransformation transformModel(final MDP model, final BitSet objectiveStates, final BitSet conditionStates) throws PrismException
	{
		return new GoalFailTransformation(super.transformModel(model, objectiveStates, conditionStates));
	}

	@Override
	protected BitSet getTerminalStates(final BitSet objectiveStates, final BitSet conditionStates)
	{
		return objectiveStates;
	}

	@Override
	protected MappingInt<List<Iterator<Entry<Integer, Double>>>> getChoices(final MDP model, final BitSet objectiveStates, final BitSet conditionStates)
			throws PrismException
	{
		// compute Pmax(<> Condition)
		final double[] conditionMaxProbs = modelChecker.computeReachProbs(model, conditionStates, false).soln;

		return new MappingInt<List<Iterator<Entry<Integer, Double>>>>()
		{
			final int offset = model.getNumStates();
			private final ProbabilisticRedistribution objectiveRedistribution = new ProbabilisticRedistribution(objectiveStates, offset + GOAL, offset + FAIL,
					conditionMaxProbs);

			@Override
			public List<Iterator<Entry<Integer, Double>>> apply(final int state)
			{
				final List<Iterator<Entry<Integer, Double>>> distribution = objectiveRedistribution.apply(state);
				if (distribution != null) {
					// objective state
					return distribution;
				}
				if (state >= offset) {
					// trap state
					final Entry<Integer, Double> loop = (Entry<Integer, Double>) new AbstractMap.SimpleImmutableEntry<Integer, Double>(state, 1.0);
					return Collections.singletonList(Collections.singleton(loop).iterator());
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

		final GoalFailTransformer transformer = new GoalFailTransformer(new MDPModelChecker(null));
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

	public static class GoalFailTransformation extends NormalFormTransformation
	{
		public GoalFailTransformation(final MDP originalModel, final MDP transformedModel)
		{
			super(originalModel, transformedModel);
		}

		public GoalFailTransformation(final NormalFormTransformation transformation)
		{
			super(transformation);
		}

		public int getFailState()
		{
			return numberOfStates + FAIL;
		}
	}
}