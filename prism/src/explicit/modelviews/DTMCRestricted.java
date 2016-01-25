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
import common.IteratorTools;
import common.functions.BitSetPredicate;
import common.functions.primitive.PredicateInteger;
import common.iterable.FilteringIterable;
import common.iterable.IterableStateSet;
import common.iterable.MappingIterable;
import common.iterable.MappingIterator;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;
import explicit.DTMC;
import explicit.DTMCSimple;

public class DTMCRestricted extends DTMCView
{
	private DTMC model;
	// FIXME ALG: consider using a predicate instead
	private BitSet states;
	private Restriction restriction;
	private PredicateInteger isStateIncluded;
	// FIXME ALG: consider using a mapping function instead
	private int[] mappingToOriginalModel;
	private Integer[] mappingToRestrictedModel;



	public DTMCRestricted(final DTMC model, final Iterable<Integer> states)
	{
		this(model, states, Restriction.TRANSITIVE_CLOSURE);
	}

	public DTMCRestricted(final DTMC model, final Iterable<Integer> states, final Restriction restriction)
	{
		this(model, BitSetTools.asBitSet(states), restriction);
	}

	public DTMCRestricted(final DTMC model, final BitSet states)
	{
		this(model, states, Restriction.TRANSITIVE_CLOSURE);
	}

	public DTMCRestricted(final DTMC model, final PredicateInteger states)
	{
		this(model, states, Restriction.TRANSITIVE_CLOSURE);
	}

	public DTMCRestricted(final DTMC model, final PredicateInteger states, final Restriction restriction)
	{
		this(model, BitSetTools.asBitSet(new IterableStateSet(states, model.getNumStates())), restriction);
	}

	public DTMCRestricted(final DTMC model, final BitSet include, final Restriction restriction)
	{
		assert include.length() <= model.getNumStates();

		this.model = model;
		this.restriction = restriction;
		this.states = restriction.getStateSet(model, include);

		isStateIncluded = new BitSetPredicate(states);
		mappingToRestrictedModel = new Integer[model.getNumStates()];
		mappingToOriginalModel = new int[states.cardinality()];
		for (int state = 0, index = 0, numStates = model.getNumStates(); state < numStates; state++) {
			if (states.get(state)) {
				mappingToRestrictedModel[state] = index;
				mappingToOriginalModel[index] = state;
				index++;
			}
		}
	}

	public DTMCRestricted(final DTMCRestricted restricted)
	{
		super(restricted);
		model = restricted.model;
		states = restricted.states;
		restriction = restricted.restriction;
		isStateIncluded = restricted.isStateIncluded;
		mappingToOriginalModel = restricted.mappingToOriginalModel;
		mappingToRestrictedModel = restricted.mappingToRestrictedModel;
	}



	//--- Cloneable ---

	@Override
	public DTMCRestricted clone()
	{
		return new DTMCRestricted(this);
	}



	//--- Model ---

	@Override
	public int getNumStates()
	{
		return states.cardinality();
	}

	@Override
	public int getNumInitialStates()
	{
		return IteratorTools.count(getInitialStates());
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		return new MappingIterable<>(new FilteringIterable<>(model.getInitialStates(), isStateIncluded), this::mapStateToRestrictedModel);
	}

	@Override
	public int getFirstInitialState()
	{
		final Iterator<Integer> initials = getInitialStates().iterator();
		return initials.hasNext() ? initials.next() : -1;
	}

	@Override
	public boolean isInitialState(final int state)
	{
		return model.isInitialState(mapStateToOriginalModel(state));
	}

	@Override
	public List<State> getStatesList()
	{
		final List<State> originalStates = model.getStatesList();
		if (originalStates == null) {
			return null;
		}
		final List<State> states = new ArrayList<State>(getNumStates());
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			states.add(originalStates.get(mappingToOriginalModel[state]));
		}
		return states;
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
	public BitSet getLabelStates(String name)
	{
		final BitSet labelStates = model.getLabelStates(name);
		return (labelStates == null) ? null : mapStatesToRestrictedModel(labelStates);
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


	//--- DTMC ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state)
	{
		final int originalState = mapStateToOriginalModel(state);
		if (restriction == Restriction.STRICT && ! allSuccessorsIncluded(originalState)) {
			return Collections.emptyIterator();
		}
		return new MappingIterator.From<>(model.getTransitionsIterator(originalState), this::mapTransitionToRestrictedModel);
	}

	private boolean allSuccessorsIncluded(final int originalState)
	{
		// FIXME ALG: consider memoizing
		return model.allSuccessorsInSet(originalState, states);
	}



	//--- DTMCView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = DTMCAlteredDistributions.fixDeadlocks(this.clone());
		states = new BitSet();
		states.flip(0, model.getNumStates());
		restriction = Restriction.TRANSITIVE_CLOSURE;
		isStateIncluded = new PredicateInteger()
		{
			final int numStates = model.getNumStates();

			@Override
			public boolean test(final int state)
			{
				return (0 <= state) && (state < numStates);
			}
		};
		// FIXME ALG: extract identity array generation
		mappingToOriginalModel = new int[model.getNumStates()];
		mappingToRestrictedModel = new Integer[model.getNumStates()];
		for (int state = 0; state < mappingToOriginalModel.length; state++) {
			mappingToOriginalModel[state] = state;
			mappingToRestrictedModel[state] = state;
		}
	}



	//--- instance methods ---

	public int mapStateToOriginalModel(final int state)
	{
		return mappingToOriginalModel[state];
	}

	public Integer mapStateToRestrictedModel(final int state)
	{
		return mappingToRestrictedModel[state];
	}

	public BitSet mapStatesToRestrictedModel(final BitSet originalStates)
	{
		if (originalStates == null) {
			throw new NullPointerException();
		}
		final BitSet mappedStates = new BitSet();
		for (int originalState : new IterableStateSet(originalStates, model.getNumStates())) {
			final Integer state = mappingToRestrictedModel[originalState];
			if (state != null) {
				mappedStates.set(state);
			}
		}
		return mappedStates;
	}

	public Entry<Integer, Double> mapTransitionToRestrictedModel(final Entry<Integer, Double> transition)
	{
		final Integer state = mapStateToRestrictedModel(transition.getKey());
		final Double probability = transition.getValue();
		return new AbstractMap.SimpleImmutableEntry<>(state, probability);
	}



	//--- static methods ---

	public static void main(final String[] args) throws PrismException
	{
		final DTMCSimple original = new DTMCSimple(4);
		original.addInitialState(1);
		original.setProbability(0, 1, 0.1);
		original.setProbability(0, 2, 0.9);
		original.setProbability(1, 2, 0.2);
		original.setProbability(1, 3, 0.8);
		original.setProbability(2, 1, 0.3);
		original.setProbability(2, 2, 0.7);
		original.findDeadlocks(false);

		DTMC restricted;

		System.out.println("Original Model:");
		System.out.print(original.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original.getDeadlockStates()));
		System.out.println(original);

		System.out.println();

		final BitSet include = new BitSet();
		include.set(1);

		System.out.println("Restricted Model " + include + " without Reachability:");
		restricted = new DTMCRestricted(original, include, Restriction.STRICT);
		restricted.findDeadlocks(true);
		System.out.print(restricted.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(restricted.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(restricted.getDeadlockStates()));
		System.out.println(restricted);

		System.out.println();

		System.out.println("Restricted Model " + include + " with Reachability:");
		restricted = new DTMCRestricted(original, include);
		restricted.findDeadlocks(true);
		System.out.print(restricted.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(restricted.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(restricted.getDeadlockStates()));
		System.out.println(restricted);
	}
}