package explicit.modelviews;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import common.BitSetTools;
import common.functions.PairMapping;
import common.functions.primitive.PairPredicateInt;
import common.iterable.FunctionalIterator;
import common.iterable.Interval;
import explicit.DiracDistribution;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import prism.PrismException;
import prism.PrismSettings;

public class ChoicesToStates
{
	private static final class PreserveChoice implements PairPredicateInt
	{
		final int preserve;

		public PreserveChoice(final int choice)
		{
			preserve = choice;
		}

		@Override
		public boolean test(final int state, final int choice)
		{
			return choice != preserve;
		}
	}

	public static MDP choicesToStates(final MDP mdp)
	{
		final int maxNumChoices = mdp.getMaxNumChoices();
		final List<MDP> replica = new ArrayList<>(maxNumChoices);
		replica.add(mdp);
		for (int choice = 0; choice < maxNumChoices; choice++) {
			replica.add(new MDPDroppedChoices(mdp, new PreserveChoice(choice)));
		}
		final MDP union = MDPDisjointUnion.union(replica);

		final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> choiceMapping = new PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>>()
		{
			final int offset = mdp.getNumStates();

			@Override
			public Iterator<Entry<Integer, Double>> apply(final Integer state, final Integer choice)
			{
				if (state < offset) {
					// redirect to choice replica
					final int target = state + (offset * (choice + 1));
					return DiracDistribution.iterator(target);
				}
				// redirect to original model
				final Iterator<Entry<Integer, Double>> transitions = union.getTransitionsIterator(state, choice);
				final Function<Entry<Integer, Double>, Entry<Integer, Double>> redirect = new Function<Entry<Integer, Double>, Entry<Integer, Double>>()
				{
					@Override
					public Entry<Integer, Double> apply(Entry<Integer, Double> transition)
					{
						final int target = transition.getKey() % offset;
						final Double probability = transition.getValue();
						return new AbstractMap.SimpleImmutableEntry<>(target, probability);
					}
				};
				return FunctionalIterator.extend(transitions).map(redirect);
			}
		};
		return new MDPAlteredDistributions(union, choiceMapping);
	}

	public static MDPRestricted selectedChoicesToStates(final MDP mdp, final Map<Integer, Set<Integer>> choices)
	{
		final MDP choicesToStates = choicesToStates(mdp);

		final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> selectedMapping = new PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>>()
		{
			final int offset = mdp.getNumStates();

			@Override
			public Iterator<Entry<Integer, Double>> apply(final Integer state, final Integer choice)
			{
				if (state > offset || (choices.containsKey(state) && choices.get(state).contains(choice)))
					return choicesToStates.getTransitionsIterator(state, choice);
				return mdp.getTransitionsIterator(state, choice);
			}
		};
		final MDP selectedChoicesToStates = new MDPAlteredDistributions(choicesToStates, selectedMapping);

		return new MDPRestricted(selectedChoicesToStates, BitSetTools.asBitSet(new Interval(mdp.getNumStates())));
	}

	public static MDPRestricted selectedChoicesToStates(final MDP mdp, final PairPredicateInt choices)
	{
		final MDP choicesToStates = choicesToStates(mdp);

		final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> selectedMapping = new PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>>()
		{
			final int offset = mdp.getNumStates();

			@Override
			public Iterator<Entry<Integer, Double>> apply(final Integer state, final Integer choice)
			{
				if (state >= offset || choices.test(state, choice))
					return choicesToStates.getTransitionsIterator(state, choice);
				return mdp.getTransitionsIterator(state, choice);
			}
		};
		final MDP selectedChoicesToStates = new MDPAlteredDistributions(choicesToStates, selectedMapping);

		return new MDPRestricted(selectedChoicesToStates, BitSetTools.asBitSet(new Interval(mdp.getNumStates())));
	}

	public static void main(final String[] args) throws PrismException
	{
		final MDPSimple original = new MDPSimple(4);
		original.addInitialState(1);
		Distribution dist = new Distribution();
		dist.add(1, 0.1);
		dist.add(2, 0.9);
		original.addActionLabelledChoice(0, dist, "a");
		dist = new Distribution();
		dist.add(1, 0.2);
		dist.add(2, 0.8);
		original.addActionLabelledChoice(0, dist, "b");
		dist = new Distribution();
		dist.add(3, 1);
		original.addActionLabelledChoice(1, dist, "a");
		dist = new Distribution();
		dist.add(2, 1);
		original.addActionLabelledChoice(1, dist, "b");
		dist = new Distribution();
		dist.add(1, 0.3);
		dist.add(2, 0.7);
		original.addActionLabelledChoice(2, dist, "a");

		System.out.println("Original Model:");
		System.out.print(original.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original.getDeadlockStates()));
		System.out.println(original);

		System.out.println();

		System.out.println("Choice Model:");
		MDP choiceModel = choicesToStates(original);
		System.out.print(choiceModel.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(choiceModel.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(choiceModel.getDeadlockStates()));
		System.out.println(choiceModel);

		original.exportToDotFile("original.dot", PrismSettings.DEFAULT_EXPORT_MODEL_PRECISION);
		choiceModel.exportToDotFile("choiceModel.dot",PrismSettings.DEFAULT_EXPORT_MODEL_PRECISION);
	}
}