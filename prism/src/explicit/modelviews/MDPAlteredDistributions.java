package explicit.modelviews;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import common.BitSetTools;
import common.functions.AbstractMapping;
import common.functions.AbstractPairMapping;
import common.functions.Mapping;
import common.functions.PairMapping;
import common.functions.primitive.AbstractMappingFromInteger;
import common.functions.primitive.MappingFromInteger;
import common.iterable.IterableBitSet;
import common.iterable.MappingIterable;
import common.iterable.MappingIterator;
import common.iterable.collections.ChainedList;
import common.methods.CallBitSet;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import explicit.modelviews.methods.CallMDP;
import explicit.modelviews.methods.CallNondetModel;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;

public class MDPAlteredDistributions extends MDPView
{
	private MDP model;
	private PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> choices;
	private PairMapping<Integer, Integer, Object> actions;



	public MDPAlteredDistributions(final MDP model, final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> choiceMapping)
	{
		this(model, choiceMapping, CallNondetModel.getAction().on(model));
	}

	// FIXME ALG: outline invariants of both mappings
	public MDPAlteredDistributions(final MDP model, final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> choices,
			final PairMapping<Integer, Integer, Object> actions)
	{
		this.model = model;
		this.choices = choices;
		this.actions = actions;
	}

	public MDPAlteredDistributions(final MDPAlteredDistributions additional)
	{
		super(additional);
		model = additional.model;
		choices = additional.choices;
		actions = additional.actions;
	}



	//--- Clonable ---

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
		return actions == null ? model.getAction(state, choice) : actions.get(state, choice);
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		final Iterator<Entry<Integer, Double>> transitions = choices.get(state, choice);
		assert transitions.hasNext() : "non-empty transitions iterator expected";
		return transitions;
	}



	//--- MDPView ---

	@Override
	protected MDPAlteredDistributions fixDeadlocks()
	{
		if (fixedDeadlocks) {
			return this;
		}
		model = MDPAdditionalChoices.fixDeadlocks(this.clone());
		choices = CallMDP.getTransitionsIterator().on(model);
		actions = null;
		fixedDeadlocks = true;
		return this;
	}



	//--- static methods ---

	public static MDPAlteredDistributions normalizeDistributions(final MDP model)
	{
		final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> normalize = new AbstractPairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> get(final Integer state, final Integer choice)
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

		return new MDPAlteredDistributions(model, normalize, CallNondetModel.getAction().on(model));
	}

	public static MDP identifyStates(final MDP model, final Iterable<BitSet> equivalenceClasses)
	{
		assert BitSetTools.areDisjoint(equivalenceClasses) : "expected disjoint sets of identified states";
		assert BitSetTools.areNonEmpty(equivalenceClasses) : "expected non-empty sets of identified states";

		final Mapping<BitSet, Integer> getRepresentative = CallBitSet.nextSetBit(0);
		final BitSet representatives = BitSetTools.asBitSet(new MappingIterator<>(equivalenceClasses, getRepresentative));

		final MappingFromInteger<BitSet> equivalenceClass = new AbstractMappingFromInteger<BitSet>()
		{
			@Override
			// FIXME ALG: consider memoizing
			public BitSet get(final int state)
			{
				for (BitSet equivalenceClass : equivalenceClasses) {
					if (equivalenceClass.get(state)) {
						return equivalenceClass;
					}
				}
				return null;
			}
		};
		final BitSet unionOfEquivalenceClasses = BitSetTools.union(equivalenceClasses);

		// 1. drop choices from equivalent states
		final MDPDroppedAllChoices droppedChoices = new MDPDroppedAllChoices(model, unionOfEquivalenceClasses);

		// 2. attach all choices of an equivalence class to its representative
		final MappingFromInteger<List<Iterator<Entry<Integer, Double>>>> addChoices = new AbstractMappingFromInteger<List<Iterator<Entry<Integer, Double>>>>()
		{
			@Override
			public List<Iterator<Entry<Integer, Double>>> get(final int state)
			{
				if (!representatives.get(state)) {
					return Collections.emptyList();
				}
				final MappingFromInteger<List<Iterator<Entry<Integer, Double>>>> getChoices = new AbstractMappingFromInteger<List<Iterator<Entry<Integer, Double>>>>()
				{
					@Override
					public List<Iterator<Entry<Integer, Double>>> get(final int state)
					{
						final int numChoices = model.getNumChoices(state);
						final List<Iterator<Entry<Integer, Double>>> choices = new ArrayList<>(numChoices);
						for (int choice = 0; choice < numChoices; choice++) {
							choices.add(model.getTransitionsIterator(state, choice));
						}
						return choices;
					}
				};
				return new ChainedList<Iterator<Entry<Integer, Double>>>(new MappingIterable<>(new IterableBitSet(equivalenceClass.get(state)), getChoices));
			}
		};
		final MappingFromInteger<List<Object>> addActions = new AbstractMappingFromInteger<List<Object>>()
		{
			@Override
			public List<Object> get(final int state)
			{
				if (!representatives.get(state)) {
					return Collections.emptyList();
				}
				final MappingFromInteger<List<Object>> getActions = new AbstractMappingFromInteger<List<Object>>()
				{
					@Override
					public List<Object> get(final int state)
					{
						final int numChoices = model.getNumChoices(state);
						final List<Object> actions = new ArrayList<>(numChoices);
						for (int choice = 0; choice < numChoices; choice++) {
							actions.add(model.getAction(state, choice));
						}
						return actions;
					}
				};
				return new ChainedList<Object>(new MappingIterable<>(new IterableBitSet(equivalenceClass.get(state)), getActions));
			}
		};
		final MDPView reattached = new MDPAdditionalChoices(droppedChoices, addChoices, addActions);

		// 3. redirect transitions to representatives
		final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> redirect = new AbstractPairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> get(final Integer state, final Integer choice)
			{
				final Iterator<Entry<Integer, Double>> transitions = reattached.getTransitionsIterator(state, choice);
				if (!reattached.someSuccessorsInSet(state, unionOfEquivalenceClasses)) {
					return transitions;
				}
				final Mapping<Entry<Integer, Double>, Entry<Integer, Double>> redirect = new AbstractMapping<Entry<Integer, Double>, Entry<Integer, Double>>()
				{
					@Override
					public final Entry<Integer, Double> get(final Entry<Integer, Double> transition)
					{
						final int target = transition.getKey();
						if (unionOfEquivalenceClasses.get(target) && !representatives.get(target)) {
							final Integer redirectedTarget = getRepresentative.get(equivalenceClass.get(target));
							final Double probability = transition.getValue();
							return new AbstractMap.SimpleImmutableEntry<>(redirectedTarget, probability);
						}
						return transition;
					}
				};
				return new MappingIterator<>(reattached.getTransitionsIterator(state, choice), redirect);
			}
		};
		final MDPAlteredDistributions redirected = new MDPAlteredDistributions(reattached, redirect);

		// 4. drop equivalence classes except for the representatives
		final BitSet drop = BitSetTools.minus(unionOfEquivalenceClasses, representatives);
		final BitSet preserve = BitSetTools.complement(model.getNumStates(), drop);

		return new MDPRestricted(redirected, preserve, Restriction.STRICT);
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

		final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> transitions = new AbstractPairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> get(final Integer state, final Integer choice)
			{
				final Distribution distribution = new Distribution();
				if (state == 1 && choice == 1) {
					distribution.set(2, 0.4);
					distribution.set(3, 0.6);
					return distribution.iterator();
				} else if (state == 2) {
					distribution.set(0, 0.25);
					distribution.set(1, 0.25);
					distribution.set(2, 0.25);
					distribution.set(3, 0.25);
					return distribution.iterator();
				}
				return original.getTransitionsIterator(state, choice);
			}
		};

		final PairMapping<Integer, Integer, Object> actions = new AbstractPairMapping<Integer, Integer, Object>()
		{
			@Override
			public Object get(final Integer state, final Integer choice)
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