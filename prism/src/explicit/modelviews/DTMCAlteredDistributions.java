package explicit.modelviews;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import common.BitSetTools;
import common.functions.Relation;
import common.iterable.ChainedIterator;
import common.iterable.EmptyIterator;
import common.iterable.FilteringIterator;
import common.iterable.IterableBitSet;
import common.iterable.MappingIterator;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;
import explicit.DTMC;
import explicit.DTMCSimple;
import explicit.DiracDistribution;
import explicit.Distribution;

public class DTMCAlteredDistributions extends DTMCView
{
	private static final Predicate<Entry<Integer, Double>> nonZero = Relation.GT(0.0).compose((Function<Entry<Integer, Double>, Double>) Entry::getValue);

	private DTMC model;
	private IntFunction<Iterator<Entry<Integer, Double>>> mapping;



	/**
	 * If {@code mapping} returns {@code null} for a state, the original transitions are preserved.
	 *
	 * @param model a DTMC
	 * @param mapping from states to (new) distributions or null
	 */
	public DTMCAlteredDistributions(final DTMC model, final IntFunction<Iterator<Entry<Integer, Double>>> mapping)
	{
		this.model = model;
		this.mapping = mapping;
	}

	public DTMCAlteredDistributions(final DTMCAlteredDistributions altered)
	{
		super(altered);
		model = altered.model;
		mapping = altered.mapping;
	}



	//--- Cloneable ---

	@Override
	public DTMCAlteredDistributions clone()
	{
		return new DTMCAlteredDistributions(this);
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



	//--- DTMC ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state)
	{
		final Iterator<Entry<Integer, Double>> transitions = mapping.apply(state);
		if (transitions == null) {
			return model.getTransitionsIterator(state);
		}
		return new FilteringIterator.Of<>(transitions, nonZero);
	}



	//--- DTMCView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = fixDeadlocks(this.clone());
		mapping = state -> null;
	}



	//--- static methods ---

	public static DTMCAlteredDistributions fixDeadlocks(final DTMC model)
	{
		final BitSet deadlockStates = BitSetTools.asBitSet(model.getDeadlockStates());
		final DTMCAlteredDistributions fixed = addSelfLoops(model, deadlockStates);
		fixed.deadlockStates = deadlockStates;
		fixed.fixedDeadlocks = true;
		return fixed;
	}

	public static DTMCAlteredDistributions addSelfLoops(final DTMC model, final BitSet states)
	{
		final IntFunction<Iterator<Entry<Integer, Double>>> addLoops = new IntFunction<Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final int state)
			{
				if (states.get(state)) {
					return DiracDistribution.iterator(state);
				}
				return null;
			}
		};
		return new DTMCAlteredDistributions(model, addLoops);
	}

	public static DTMCRestricted identifyStates(final DTMC model, final Iterable<BitSet> equivalenceClasses)
	{
		final EquivalenceRelationInteger identify = new EquivalenceRelationInteger(equivalenceClasses);
		final BitSet representatives = BitSetTools.complement(model.getNumStates(), identify.getNonRepresentatives());

		// 1. attach all transitions of an equivalence class to its representative
		final IntFunction<Iterator<Entry<Integer, Double>>> reattach = new IntFunction<Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final int state)
			{
				if (! identify.isRepresentative(state)) {
					return EmptyIterator.Of();
				}
				final BitSet equivalenceClass = identify.getEquivalenceClassOrNull(state);
				if (equivalenceClass == null) {
					return null;
				}
				final MappingIterator<Integer, Iterator<Entry<Integer, Double>>> transitionIterators =
						new MappingIterator.FromInt<>(new IterableBitSet(equivalenceClass), model::getTransitionsIterator);
				return FilteringIterator.dedupe(new ChainedIterator.Of<>(transitionIterators));
			}
		};
		final DTMC reattached = new DTMCAlteredDistributions(model, reattach);

		// 2. redirect transitions to representatives
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
		final IntFunction<Iterator<Entry<Integer, Double>>> redirectDistribution = new IntFunction<Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final int state)
			{
				if (reattached.allSuccessorsInSet(state, representatives)) {
					return null;
				}
				final Iterator<Entry<Integer, Double>> transitions = reattached.getTransitionsIterator(state);
				final Iterator<Entry<Integer, Double>> redirected = new MappingIterator.From<>(transitions, redirectTransition);
				// use Distribution to dedupe successors
				return new Distribution(redirected).iterator();
			}
		};
		final DTMC redirected = new DTMCAlteredDistributions(reattached, redirectDistribution);

		// 3. drop equivalence classes except for the representatives
		return new DTMCRestricted(redirected, representatives, Restriction.STRICT);
	}

	public static void main(final String[] args) throws PrismException
	{
		final DTMCSimple original = new DTMCSimple(4);
		original.addInitialState(0);
		original.setProbability(0, 1, 0.1);
		original.setProbability(0, 3, 0.9);
		original.setProbability(1, 2, 0.2);
		original.setProbability(1, 3, 0.8);
		original.setProbability(2, 1, 0.3);
		original.setProbability(2, 3, 0.7);
		original.findDeadlocks(false);

		DTMC alteredDistributions;

		System.out.println("Original Model:");
		System.out.print(original.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original.getDeadlockStates()));
		System.out.println(original);

		System.out.println();

		final IntFunction<Iterator<Entry<Integer, Double>>> transitions = new IntFunction<Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final int state)
			{
				final Distribution distribution = new Distribution();
				if (state == 0) {
					return EmptyIterator.Of();
				}
				if (state == 1) {
					distribution.set(1, 0.4);
					distribution.set(3, 0.6);
				} else if (state == 3) {
					distribution.set(2, 1.0);
					distribution.set(3, 0.0);
				} else {
					return null;
				}
				return distribution.iterator();
			}
		};

		System.out.println("Altered Model:");
		alteredDistributions = new DTMCAlteredDistributions(original, transitions);
		alteredDistributions.findDeadlocks(true);
		System.out.print(alteredDistributions.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(alteredDistributions.getDeadlockStates()));
		System.out.println(alteredDistributions);
	}
}