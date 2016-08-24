package explicit.modelviews;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.IntPredicate;

import common.BitSetTools;
import common.IteratorTools;
import common.iterable.FilteringIterable;
import common.iterable.IterableBitSet;
import common.iterable.IterableInt;
import common.iterable.IterableStateSet;
import common.iterable.MappingIterable;
import common.iterable.MappingIterator;
import explicit.BasicModelTransformation;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import explicit.ReachabilityComputer;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;

public class MDPRestricted extends MDPView
{
	private static final Restriction STANDARD_RESTRICTION = Restriction.TRANSITIVE_CLOSURE;
	protected MDP model;
	// FIXME ALG: consider not storing state set at all
	protected BitSet states;
	protected int numStates;
	protected Restriction restriction;
	// FIXME ALG: consider using a mapping function instead
	protected int[] mappingToOriginalModel;
	protected Integer[] mappingToRestrictedModel;
	protected BitSet redirectTransitions;



	public MDPRestricted(final MDP model, final BitSet states)
	{
		this(model, states, STANDARD_RESTRICTION);
	}

	public MDPRestricted(final MDP model, final BitSet include, final Restriction restriction)
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

	public MDPRestricted(final MDPRestricted restricted)
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
	public MDPRestricted clone()
	{
		return new MDPRestricted(this);
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
		return IteratorTools.count(getInitialStates());
	}

	@Override
	public IterableInt getInitialStates()
	{
		final FilteringIterable<Integer> initialStates = new FilteringIterable.Of<>(model.getInitialStates(), states::get);
		return new MappingIterable.ToInt<>(initialStates, this::mapStateToRestrictedModel);
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
			states.add(originalStates.get(mapStateToOriginalModel(state)));
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
	public BitSet getLabelStates(final String name)
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

	@Override
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



	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		final int originalState = mapStateToOriginalModel(state);
		if (restriction != Restriction.STRICT) {
			return model.getNumChoices(originalState);
		}
		// FIXME ALG: consider caching
		int countChoices = 0;
		for (int originalChoice = 0, numOriginalChoices = model.getNumChoices(originalState); originalChoice < numOriginalChoices; originalChoice++) {
			if (model.allSuccessorsInSet(originalState, originalChoice, states)) {
				countChoices++;
			}
		}
		return countChoices;
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		return model.getAction(mapStateToOriginalModel(state), mapChoiceToOriginalModel(state, choice));
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return model.areAllChoiceActionsUnique() ? true : super.areAllChoiceActionsUnique();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		if (restriction == Restriction.STRICT) {
			return super.getSuccessorsIterator(state, choice);
		}
		int originalState = mapStateToOriginalModel(state);
		Iterator<Integer> successors = model.getSuccessorsIterator(originalState, choice);
		if (redirectTransitions.get(originalState)) {
			successors = new MappingIterator.From<>(successors, this::mapStateToRestrictedModel);
		}
		return successors;
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		int originalState = mapStateToOriginalModel(state);
		int originalChoice = mapChoiceToOriginalModel(state, choice);
		Iterator<Entry<Integer, Double>> transitions = model.getTransitionsIterator(originalState, originalChoice);
		if (redirectTransitions.get(originalState)) {
			transitions = new MappingIterator.From<>(transitions, this::mapTransitionToRestrictedModel);
		}
		return transitions;
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = MDPAdditionalChoices.fixDeadlocks(this.clone());
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

	// FIXME ALG: similar method in MDPAlteredDistributions
	public int mapChoiceToOriginalModel(final int state, final int choice)
	{
		if (restriction != Restriction.STRICT) {
			return choice;
		}
		// FIXME ALG: consider caching
		final int originalState = mapStateToOriginalModel(state);
		int countChoices = 0;
		for (int originalChoice = 0, numOriginalChoices = model.getNumChoices(originalState); originalChoice < numOriginalChoices; originalChoice++) {
			if (model.allSuccessorsInSet(originalState, originalChoice, states)) {
				if (countChoices == choice) {
					return originalChoice;
				}
				countChoices++;
			}
		}
		throw new IndexOutOfBoundsException("choice index out of bounds");
	}

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
		for (int originalState : new IterableBitSet(originalStates)) {
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

	public static BasicModelTransformation<MDP, MDPRestricted> transform(final MDP model, final IntPredicate states)
	{
		return transform(model, BitSetTools.asBitSet(new IterableStateSet(states, model.getNumStates())));
	}

	public static BasicModelTransformation<MDP, MDPRestricted> transform(final MDP model, final BitSet states)
	{
		return transform(model, states, STANDARD_RESTRICTION);
	}

	public static BasicModelTransformation<MDP, MDPRestricted> transform(final MDP model, final BitSet states, final Restriction restriction)
	{
		return transform(model, states, states, restriction);
	}

	public static BasicModelTransformation<MDP, MDPRestricted> transform(final MDP model, final BitSet states, final BitSet statesOfInterest, final Restriction restriction)
	{
		final MDPRestricted restricted = new MDPRestricted(model, states, restriction);
		final BitSet transformedStates = restricted.mapStatesToRestrictedModel(statesOfInterest);
		return new BasicModelTransformation<>(model, restricted, transformedStates, restricted.mappingToRestrictedModel);
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

		MDPRestricted restricted;

		System.out.println("Original Model:");
		System.out.print(original.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original.getDeadlockStates()));
		System.out.println(original);

		System.out.println();

		final BitSet include = BitSetTools.asBitSet(1, 2);

		System.out.println("Restricted Model " + include + " " + Restriction.STRICT);
		restricted = new MDPRestricted(original, include, Restriction.STRICT);
		restricted.findDeadlocks(true);
		System.out.print(restricted.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(restricted.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(restricted.getDeadlockStates()));
		System.out.println(restricted);
		BitSet restrictedStates = new BitSet();
		restrictedStates.set(0);
		restrictedStates.set(1);
		System.out.println("original states to " + restrictedStates + ": " + restricted.mapStatesToOriginalModel(restrictedStates));
		BitSet originalStates = new BitSet();
		originalStates.set(0);
		originalStates.set(1);
		originalStates.set(3);
		System.out.println("restricted states to " + originalStates + ": " + restricted.mapStatesToRestrictedModel(originalStates));

		System.out.println();

		System.out.println("Restricted Model " + include + " " + STANDARD_RESTRICTION);
		restricted = new MDPRestricted(original, include);
		//		restricted.findDeadlocks(true);
		System.out.print(restricted.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(restricted.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(restricted.getDeadlockStates()));
		System.out.println(restricted);
		restrictedStates = new BitSet();
		restrictedStates.set(0);
		restrictedStates.set(1);
		restrictedStates.set(2);
		System.out.println("original states to " + restrictedStates + ": " + restricted.mapStatesToOriginalModel(restrictedStates));
		originalStates = new BitSet();
		originalStates.set(0);
		originalStates.set(1);
		originalStates.set(2);
		originalStates.set(3);
		System.out.println("restricted states to " + originalStates + ": " + restricted.mapStatesToRestrictedModel(originalStates));
	}
}