package explicit.modelviews;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;

import common.BitSetTools;
import common.iterable.EmptyIterator;
import common.iterable.FunctionalIterable;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.IterableStateSet;
import common.iterable.MappingIterator;
import common.iterable.collections.UnionSet;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;
import explicit.BasicModelTransformation;
import explicit.CTMC;
import explicit.CTMCSimple;
import explicit.ReachabilityComputer;

public class CTMCRestricted extends CTMCView
{
	private static final Restriction STANDARD_RESTRICTION = Restriction.TRANSITIVE_CLOSURE;
	private CTMC model;
	// FIXME ALG: consider not storing state set at all
	private BitSet states;
	protected int numStates;
	private Restriction restriction;
	// FIXME ALG: consider using a mapping function instead
	protected int[] mappingToOriginalModel;
	protected Integer[] mappingToRestrictedModel;
	protected BitSet redirectTransitions;



	public CTMCRestricted(final CTMC model, final Iterable<Integer> states)
	{
		this(model, states, STANDARD_RESTRICTION);
	}

	public CTMCRestricted(final CTMC model, final Iterable<Integer> states, final Restriction restriction)
	{
		this(model, BitSetTools.asBitSet(states), restriction);
	}

	public CTMCRestricted(final CTMC model, final BitSet states)
	{
		this(model, states, STANDARD_RESTRICTION);
	}

	public CTMCRestricted(final CTMC model, final IntPredicate states)
	{
		this(model, states, STANDARD_RESTRICTION);
	}

	public CTMCRestricted(final CTMC model, final IntPredicate states, final Restriction restriction)
	{
		this(model, BitSetTools.asBitSet(new IterableStateSet(states, model.getNumStates())), restriction);
	}

	public CTMCRestricted(final CTMC model, final BitSet include, final Restriction restriction)
	{
		assert include.length() <= model.getNumStates();

		this.model = model;
		this.restriction = restriction;
		this.states = restriction.getStateSet(model, include);
		numStates = states.cardinality();

		mappingToRestrictedModel = new Integer[model.getNumStates()];
		mappingToOriginalModel = new int[numStates];
		int firstModified = 0;
		for (int state = states.nextSetBit(0), index = 0; state >= 0; state = states.nextSetBit(state+1)) {
			mappingToRestrictedModel[state] = index;
			mappingToOriginalModel[index] = state;
			index++;
			if (state < index) {
				firstModified = index;
			}
		}
		redirectTransitions = new BitSet(numStates);
		redirectTransitions.set(firstModified, model.getNumStates());
		redirectTransitions = new ReachabilityComputer(model).computePre(redirectTransitions);
	}

	public CTMCRestricted(final CTMCRestricted restricted)
	{
		super(restricted);
		model = restricted.model;
		states = restricted.states;
		numStates = restricted.numStates;
		restriction = restricted.restriction;
		mappingToOriginalModel = restricted.mappingToOriginalModel;
		mappingToRestrictedModel = restricted.mappingToRestrictedModel;
		redirectTransitions = restricted.redirectTransitions;
	}



	//--- Cloneable ---

	@Override
	public CTMCRestricted clone()
	{
		return new CTMCRestricted(this);
	}



	//--- Model ---

	@Override
	public int getNumStates()
	{
		return numStates;
	}

	@Override
	public int getNumInitialStates()
	{
		return getInitialStates().count();
	}

	@Override
	public IterableInt getInitialStates()
	{
		FunctionalIterable<Integer> initialStates = FunctionalIterable.extend(model.getInitialStates());
		return initialStates.filter(states::get).mapToInt(this::mapStateToRestrictedModel);
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
		if (super.hasLabel(name)) {
			return super.getLabelStates(name);
		}
		if (model.hasLabel(name)) {
			return mapStatesToRestrictedModel(model.getLabelStates(name));
		}
		return null;
	}

	@Override
	public Set<String> getLabels()
	{
		return new UnionSet<>(super.getLabels(), model.getLabels());
	}

	@Override
	public boolean hasLabel(String name)
	{
		return super.hasLabel(name) || model.hasLabel(name);
	}

	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		if (restriction == Restriction.STRICT) {
			return super.getSuccessorsIterator(state);
		}
		int originalState = mapStateToOriginalModel(state);
		Iterator<Integer> successors = model.getSuccessorsIterator(originalState);
		if (redirectTransitions.get(originalState)) {
			successors = new MappingIterator.From<>(successors, this::mapStateToRestrictedModel);
		}
		return successors;
	}



	//--- DTMC ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state)
	{
		int originalState = mapStateToOriginalModel(state);
		final int originalState1 = originalState;
		if (restriction == Restriction.STRICT && ! model.allSuccessorsInSet(originalState1, states)) {
			return EmptyIterator.Of();
		}
		Iterator<Entry<Integer, Double>> transitions = model.getTransitionsIterator(originalState);
		if (redirectTransitions.get(originalState)) {
			transitions = new MappingIterator.From<>(transitions, this::mapTransitionToRestrictedModel);
		}
		return transitions;
	}



	//--- CTMC ---

	@Override
	public void uniformise(double q)
	{
		model = uniformised(this.clone(), q);
		int numStates = model.getNumStates();
		states = new BitSet(numStates);
		states.set(0, numStates);
		restriction = Restriction.TRANSITIVE_CLOSURE_SAFE;
		// FIXME ALG: extract identity array generation
		mappingToOriginalModel = new int[numStates];
		mappingToRestrictedModel = new Integer[mappingToRestrictedModel.length];
		int state = 0;
		for (; state < numStates; state++) {
			mappingToOriginalModel[state] = state;
			mappingToRestrictedModel[state] = state;
		}
		for (; state < mappingToRestrictedModel.length; state++) {
			mappingToRestrictedModel[state] = state;
		}
	}



	//--- ModelView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = CTMCAlteredDistributions.fixDeadlocks(this.clone());
		int numStates = model.getNumStates();
		states = new BitSet(numStates);
		states.set(0, numStates);
		restriction = Restriction.TRANSITIVE_CLOSURE_SAFE;
		// FIXME ALG: extract identity array generation
		mappingToOriginalModel = new int[numStates];
		mappingToRestrictedModel = new Integer[mappingToRestrictedModel.length];
		int state = 0;
		for (; state < numStates; state++) {
			mappingToOriginalModel[state] = state;
			mappingToRestrictedModel[state] = state;
		}
		for (; state < mappingToRestrictedModel.length; state++) {
			mappingToRestrictedModel[state] = state;
		}
	}



	//--- instance methods ---

	public int mapStateToOriginalModel(final int state)
	{
		return mappingToOriginalModel[state];
	}

	public BitSet mapStatesToOriginalModel(final BitSet restrictedStates)
	{
		Objects.requireNonNull(restrictedStates);

		final int length = restrictedStates.length();
		if (length == 0){
			return new BitSet();
		}
		final BitSet originalStates = new BitSet(mappingToOriginalModel[length-1]+1);
		for (int restrictedState : new IterableStateSet(restrictedStates, mappingToOriginalModel.length)) {
			originalStates.set(mappingToOriginalModel[restrictedState]);
		}
		return originalStates;
	}

	public Integer mapStateToRestrictedModel(final int state)
	{
		return mappingToRestrictedModel[state];
	}

	public BitSet mapStatesToRestrictedModel(final BitSet originalStates)
	{
		Objects.requireNonNull(originalStates);

		final int length = originalStates.length();
		if (length == 0){
			return new BitSet();
		}
		//FIXME ALG: consider allocating a BitSet in a suited size
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

	public static BasicModelTransformation<CTMC, CTMCRestricted> transform(final CTMC model, final IntPredicate states)
	{
		return transform(model, BitSetTools.asBitSet(new IterableStateSet(states, model.getNumStates())));
	}

	public static BasicModelTransformation<CTMC, CTMCRestricted> transform(final CTMC model, final BitSet states)
	{
		return transform(model, states, STANDARD_RESTRICTION);
	}

	public static BasicModelTransformation<CTMC, CTMCRestricted> transform(final CTMC model, final BitSet states, final Restriction restriction)
	{
		final CTMCRestricted restricted = new CTMCRestricted(model, states, restriction);
		final BitSet transformedStates = restricted.mapStatesToRestrictedModel(states);
		return new BasicModelTransformation<>(model, restricted, transformedStates, state -> restricted.mappingToRestrictedModel[state]);
	}

	public static void main(final String[] args) throws PrismException
	{
		final CTMCSimple original = new CTMCSimple(4);
		original.addInitialState(1);
		original.setProbability(0, 1, 0.1);
		original.setProbability(0, 2, 0.9);
		original.setProbability(1, 2, 0.2);
		original.setProbability(1, 3, 0.8);
		original.setProbability(2, 1, 0.3);
		original.setProbability(2, 2, 0.7);
		original.findDeadlocks(false);

		CTMCRestricted restricted;

		System.out.println("Original Model:");
		System.out.print(original.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original.getDeadlockStates()));
		System.out.println(original);

		System.out.println();

		final BitSet include = new BitSet();
		include.set(1);

		System.out.println("Restricted Model " + include + " without Reachability:");
		restricted = new CTMCRestricted(original, include, Restriction.STRICT);
		restricted.findDeadlocks(true);
		System.out.print(restricted.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(restricted.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(restricted.getDeadlockStates()));
		System.out.println(restricted);
		BitSet restrictedStates = new BitSet();
		restrictedStates.set(0);
		System.out.println("original states to " + restrictedStates + ": " + restricted.mapStatesToOriginalModel(restrictedStates));
		BitSet originalStates = new BitSet();
		originalStates.set(0);
		originalStates.set(1);
		originalStates.set(3);
		System.out.println("restricted states to " + originalStates + ": " + restricted.mapStatesToRestrictedModel(originalStates));

		System.out.println();

		System.out.println("Restricted Model " + include + " with Reachability:");
		restricted = new CTMCRestricted(original, include);
		restricted.findDeadlocks(true);
		System.out.print(restricted.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(restricted.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(restricted.getDeadlockStates()));
		System.out.println(restricted);
		restrictedStates = new BitSet();
		restrictedStates.set(1);
		System.out.println("original states to " + restrictedStates + ": " + restricted.mapStatesToOriginalModel(restrictedStates));
		originalStates = new BitSet();
		originalStates.set(0);
		originalStates.set(1);
		originalStates.set(3);
		System.out.println("restricted states to " + originalStates + ": " + restricted.mapStatesToRestrictedModel(originalStates));
	}
}