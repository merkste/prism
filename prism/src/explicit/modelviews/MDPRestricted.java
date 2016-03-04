package explicit.modelviews;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

import common.BitSetTools;
import common.IteratorTools;
import common.iterable.FilteringIterable;
import common.iterable.IterableInt;
import common.iterable.IterableStateSet;
import common.iterable.MappingIterable;
import common.iterable.MappingIterator;
import explicit.BasicModelTransformation;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;

public class MDPRestricted extends MDPView
{
	private MDP model;
	// FIXME ALG: consider using a predicate instead
	private BitSet states;
	private Restriction restriction;
	// FIXME ALG: consider using a mapping function instead
	private int[] mappingToOriginalModel;
	private Integer[] mappingToRestrictedModel;



	public MDPRestricted(final MDP model, final BitSet states)
	{
		this(model, states, Restriction.TRANSITIVE_CLOSURE);
	}

	public MDPRestricted(final MDP model, final BitSet include, final Restriction restriction)
	{
		assert include.length() <= model.getNumStates();

		this.model = model;
		this.restriction = restriction;
		this.states = restriction.getStateSet(model, include);

		mappingToRestrictedModel = new Integer[model.getNumStates()];
		mappingToOriginalModel = new int[states.cardinality()];
		for (int state = states.nextSetBit(0), index = 0; state >= 0; state = states.nextSetBit(state+1)) {
			mappingToRestrictedModel[state] = index;
			mappingToOriginalModel[index] = state;
			index++;
		}
	}

	public MDPRestricted(final MDPRestricted restricted)
	{
		super(restricted);
		model = restricted.model;
		states = restricted.states;
		restriction = restricted.restriction;
		mappingToOriginalModel = restricted.mappingToOriginalModel;
		mappingToRestrictedModel = restricted.mappingToRestrictedModel;
		//		mapTargetState = restricted.mapTargetState;
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
		return states.cardinality();
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
		return model.getSuccessorsIterator(mapStateToOriginalModel(state));
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
		final int originalState = mapStateToOriginalModel(state);
		final Iterator<Integer> successors = model.getSuccessorsIterator(originalState, choice);
		return new MappingIterator.From<>(successors, this::mapStateToRestrictedModel);
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		final int originalState = mapStateToOriginalModel(state);
		final int originalChoice = mapChoiceToOriginalModel(state, choice);
		final Iterator<Entry<Integer, Double>> transitions = model.getTransitionsIterator(originalState, originalChoice);
		return new MappingIterator.From<>(transitions, this::mapTransitionToRestrictedModel);
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = MDPAdditionalChoices.fixDeadlocks(this.clone());
		states = new BitSet();
		states.flip(0, model.getNumStates());
		restriction = Restriction.TRANSITIVE_CLOSURE;
		// FIXME ALG: extract identity array generation
		mappingToOriginalModel = new int[model.getNumStates()];
		mappingToRestrictedModel = new Integer[model.getNumStates()];
		for (int state = 0; state < mappingToOriginalModel.length; state++) {
			mappingToOriginalModel[state] = state;
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
		//XXX: consider allocating a BitSet in a suited size
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

	public static BasicModelTransformation<MDP, MDPRestricted> transform(final MDP model, final BitSet states)
	{
		final MDPRestricted restricted = new MDPRestricted(model, states);
		return new BasicModelTransformation<>(model, restricted, restricted.mappingToRestrictedModel);
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

		System.out.println("Restricted Model " + include + " " + Restriction.TRANSITIVE_CLOSURE);
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