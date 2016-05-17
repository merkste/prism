package explicit.modelviews;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

import common.BitSetTools;
import common.functions.PairMapping;
import common.iterable.IterableBitSet;
import common.iterable.MappingIterable;
import common.iterable.MappingIterator;
import common.iterable.collections.ChainedList;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;

public class MDPAlteredDistributions extends MDPView
{
	private MDP model;
	private PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> choices;
	private PairMapping<Integer, Integer, Object> actions;



	/**
	 * If {@code choices} returns {@code null} for a state and a choice, the original transitions are preserved.
	 * 
	 * @param model
	 * @param choiceMapping
	 */
	public MDPAlteredDistributions(final MDP model, final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> choiceMapping)
	{
		this(model, choiceMapping, model::getAction);
	}

	/**
	 * If {@code choices} returns {@code null} for a state and a choice, the original transitions are preserved.
	 * If {@code actions} is {@code null}, the original actions are preserved.
	 * 
	 * @param model
	 * @param choices
	 * @param actions
	 */
	public MDPAlteredDistributions(final MDP model, final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> choices,
			final PairMapping<Integer, Integer, Object> actions)
	{
		this.model = model;
		this.choices = choices;
		this.actions = actions;
	}

	public MDPAlteredDistributions(final MDPAlteredDistributions altered)
	{
		super(altered);
		model = altered.model;
		choices = altered.choices;
		actions = altered.actions;
	}



	//--- Cloneable ---

	@Override
	public MDPAlteredDistributions clone()
	{
		return new MDPAlteredDistributions(this);
	}



	//--- Model ---

	@Override
	public int getNumStates()
	{
		return model.getNumStates();
	}

	@Override
	public int getNumInitialStates()
	{
		return model.getNumInitialStates();
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		return model.getInitialStates();
	}

	@Override
	public int getFirstInitialState()
	{
		return model.getFirstInitialState();
	}

	@Override
	public boolean isInitialState(final int state)
	{
		return model.isInitialState(state);
	}

	@Override
	public List<State> getStatesList()
	{
		return model.getStatesList();
	}

	@Override
	public VarList getVarList()
	{
		return model.getVarList();
	}

	@Override
	public Values getConstantValues()
	{
		return model.getConstantValues();
	}

	@Override
	public BitSet getLabelStates(final String name)
	{
		return model.getLabelStates(name);
	}

	@Override
	public Set<String> getLabels()
	{
		return model.getLabels();
	}

	@Override
	public boolean hasLabel(String name)
	{
		return model.hasLabel(name);
	}



	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		return model.getNumChoices(state);
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		return actions == null ? model.getAction(state, choice) : actions.apply(state, choice);
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		final Iterator<Entry<Integer, Double>> transitions = choices.apply(state, choice);
		if (transitions == null) {
			return model.getTransitionsIterator(state, choice);
		}
		return transitions;
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = MDPAdditionalChoices.fixDeadlocks(this.clone());
		choices = PairMapping.constant(null);
		actions = null;
	}



	//--- static methods ---

	public static MDPAlteredDistributions normalizeDistributions(final MDP model)
	{
		final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> normalize = new PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final Integer state, final Integer choice)
			{
				final Iterator<Entry<Integer, Double>> transitions = model.getTransitionsIterator(state, choice);
				if (!transitions.hasNext()) {
					return transitions;
				}
				final Distribution distribution = new Distribution(transitions);
				final double sum = distribution.sum();
				if (sum != 1) {
					for (Entry<Integer, Double> trans : distribution) {
						distribution.set(trans.getKey(), trans.getValue() / sum);
					}
				}
				return distribution.iterator();
			}
		};

		return new MDPAlteredDistributions(model, normalize, model::getAction);
	}

	public static MDPRestricted identifyStates(final MDP model, final Iterable<BitSet> equivalenceClasses)
	{
		final EquivalenceRelationInteger identify = new EquivalenceRelationInteger(equivalenceClasses);
		final BitSet representatives = BitSetTools.complement(model.getNumStates(), identify.getNonRepresentatives());

		// 1. drop choices from non-representative states
		final MDPDroppedAllChoices droppedChoices = new MDPDroppedAllChoices(model, identify.getNonRepresentatives());

		// 2. attach all choices of an equivalence class to its representative
		final IntFunction<List<Iterator<Entry<Integer, Double>>>> addChoices = new IntFunction<List<Iterator<Entry<Integer, Double>>>>()
		{
			@Override
			public List<Iterator<Entry<Integer, Double>>> apply(final int state)
			{
				if (! identify.isRepresentative(state)) {
					return null;
				}
				final BitSet equivalenceClass = identify.getEquivalenceClassOrNull(state);
				if (equivalenceClass == null) {
					return null;
				}
				final IntFunction<List<Iterator<Entry<Integer, Double>>>> getOtherChoices = new IntFunction<List<Iterator<Entry<Integer, Double>>>>()
				{
					@Override
					public List<Iterator<Entry<Integer, Double>>> apply(final int state)
					{
						if (identify.isRepresentative(state)) {
							return Collections.emptyList();
						}
						final int numChoices = model.getNumChoices(state);
						final List<Iterator<Entry<Integer, Double>>> choices = new ArrayList<>(numChoices);
						for (int choice = 0; choice < numChoices; choice++) {
							choices.add(model.getTransitionsIterator(state, choice));
						}
						return choices;
					}
				};
				return new ChainedList<Iterator<Entry<Integer, Double>>>(new MappingIterable.FromInt<>(new IterableBitSet(equivalenceClass), getOtherChoices));
			}
		};
		final IntFunction<List<Object>> addActions = new IntFunction<List<Object>>()
		{
			@Override
			public List<Object> apply(final int state)
			{
				if (! identify.isRepresentative(state)) {
					return null;
				}
				final BitSet equivalenceClass = identify.getEquivalenceClassOrNull(state);
				if (equivalenceClass == null) {
					return null;
				}
				final IntFunction<List<Object>> getOtherActions = new IntFunction<List<Object>>()
				{
					@Override
					public List<Object> apply(final int state)
					{
						if (identify.isRepresentative(state)) {
							return null;
						}
						final int numChoices = model.getNumChoices(state);
						final List<Object> actions = new ArrayList<>(numChoices);
						for (int choice = 0; choice < numChoices; choice++) {
							actions.add(model.getAction(state, choice));
						}
						return actions;
					}
				};
				return new ChainedList<Object>(new MappingIterable.FromInt<>(new IterableBitSet(equivalenceClass), getOtherActions));
			}
		};
		final MDPView reattached = new MDPAdditionalChoices(droppedChoices, addChoices, addActions);

		// 3. redirect transitions to representatives
		final Function<Entry<Integer, Double>, Entry<Integer, Double>> redirectTransition = new Function<Entry<Integer, Double>, Entry<Integer, Double>>()
		{
			@Override
			public final Entry<Integer, Double> apply(final Entry<Integer, Double> transition)
			{
				final int target = transition.getKey();
				if (identify.isRepresentative(target)) {
					return transition;
				}
				final int representative = identify.getRepresentative(target);
				final Double probability = transition.getValue();
				return new AbstractMap.SimpleImmutableEntry<>(representative, probability);
			}
		};
		final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> redirectChoice = new PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final Integer state, final Integer choice)
			{
				if (reattached.allSuccessorsInSet(state, representatives)) {
					return null;
				}
				final Iterator<Entry<Integer, Double>> transitions = reattached.getTransitionsIterator(state, choice);
				return new MappingIterator.From<>(transitions, redirectTransition);
			}
		};
		final MDPAlteredDistributions redirected = new MDPAlteredDistributions(reattached, redirectChoice);

		// 4. drop equivalence classes except for the representatives
		return new MDPRestricted(redirected, representatives, Restriction.TRANSITIVE_CLOSURE_SAFE);
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
		original.addActionLabelledChoice(1, dist, "c");
		dist = new Distribution();
		dist.add(2, 1);
		original.addChoice(1, dist);
		dist = new Distribution();
		dist.add(1, 0.3);
		dist.add(2, 0.7);
		original.addChoice(2, dist);
		original.findDeadlocks(false);

		System.out.println("Original Model:");
		System.out.print(original.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original.getDeadlockStates()));
		System.out.println(original);

		System.out.println();

		final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> transitions = new PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final Integer state, final Integer choice)
			{
				final Distribution distribution = new Distribution();
				if (state == 1 && choice == 1) {
					distribution.set(2, 0.4);
					distribution.set(3, 0.6);
				} else if (state == 2) {
					distribution.set(0, 0.25);
					distribution.set(1, 0.25);
					distribution.set(2, 0.25);
					distribution.set(3, 0.25);
				} else {
					return null;
				}
				return distribution.iterator();
			}
		};

		final PairMapping<Integer, Integer, Object> actions = new PairMapping<Integer, Integer, Object>()
		{
			@Override
			public Object apply(final Integer state, final Integer choice)
			{
				Object action = original.getAction(state, choice);
				if (action instanceof String) {
					return action + "'";
				} else {
					return "d";
				}
			}
		};

		System.out.println("Altered Model:");
		MDP alteredDistributions = new MDPAlteredDistributions(original, transitions, actions);
		alteredDistributions.findDeadlocks(true);
		System.out.print(alteredDistributions.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(alteredDistributions.getDeadlockStates()));
		System.out.println(alteredDistributions);
	}
}