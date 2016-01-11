package explicit.conditional.transformer;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import parser.State;
import prism.PrismException;
import common.BitSetTools;
import common.functions.primitive.MappingInt;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSimple;

public class GoalFailStopTransformer extends ConditionalNormalFormTransformer
{
	public static final int FAIL = 1;
	public static final int STOP = 2;

	public GoalFailStopTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	protected BitSet getTerminalStates(final BitSet objectiveStates, final BitSet conditionStates)
	{
		final BitSet terminal = (BitSet) objectiveStates.clone();
		terminal.or(conditionStates);
		return terminal;
	}

	@Override
	protected MappingInt<List<Iterator<Entry<Integer, Double>>>> getChoices(final MDP model, final BitSet objectiveStates, final BitSet conditionStates)
			throws PrismException
	{
		final int offset = model.getNumStates();
		// compute Pmax(<> Objective)
		final double[] objectiveMaxProbs = modelChecker.computeReachProbs(model, objectiveStates, false).soln;
		// compute Pmax(<> Condition)
		final double[] conditionMaxProbs = modelChecker.computeReachProbs(model, conditionStates, false).soln;

		return new MappingInt<List<Iterator<Entry<Integer, Double>>>>()
		{
			private final ProbabilisticRedistribution conditionRedistribution = new ProbabilisticRedistribution(conditionStates, offset + GOAL, offset + STOP,
					objectiveMaxProbs);
			private final ProbabilisticRedistribution objectiveRedistribution = new ProbabilisticRedistribution(objectiveStates, offset + GOAL, offset + FAIL,
					conditionMaxProbs);

			@Override
			public List<Iterator<Entry<Integer, Double>>> apply(final int state)
			{
				final List<Iterator<Entry<Integer, Double>>> distribution = conditionRedistribution.apply(state);
				return (distribution == null) ? objectiveRedistribution.apply(state) : distribution;
			}
		};
	}

	@Override
	protected MDPSimple buildTrapStatesModel(final MDP model, final State prototype)
	{
		final MDPSimple trapStatesModel = super.buildTrapStatesModel(model, prototype);
		addTrapState(trapStatesModel, prototype, false, null);
		addTrapState(trapStatesModel, prototype, true, "stop-loop");
		return trapStatesModel;
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

		final GoalFailStopTransformer transformer = new GoalFailStopTransformer(new MDPModelChecker(null));
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
}