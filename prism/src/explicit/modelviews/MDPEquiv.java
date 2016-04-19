package explicit.modelviews;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;

import common.BitSetTools;
import common.IteratorTools;
import common.iterable.FilteringIterator;
import common.iterable.IterableArray;
import common.iterable.IterableBitSet;
import common.iterable.IterableStateSet;
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

public class MDPEquiv extends MDPView
{
	protected MDP model;
	protected EquivalenceRelationInteger identify;
	protected int[] numChoices;
	protected StateChoicePair[][] originalChoices;
	protected BitSet hasTransitionToNonRepresentative;

	protected MDPEquiv(){/* only here to satisfy the compiler */}

	public MDPEquiv(final MDP model, final EquivalenceRelationInteger identify)
	{
		this.model = model;
		this.identify = identify;
		final int numStates = model.getNumStates();
		this.numChoices = new int[numStates];
		this.originalChoices = new StateChoicePair[numStates][];
		for (OfInt representativeIterator = new IterableStateSet(identify.nonRepresentatives, numStates, true).iterator(); representativeIterator.hasNext();){
			final int representative = representativeIterator.nextInt();
			BitSet equivalenceClass = this.identify.getEquivalenceClassOrNull(representative);
			if (equivalenceClass == null || equivalenceClass.cardinality() == 1) {
				//the equivalence-class consists only of one state
				// => leave it as it is
				numChoices[representative] = model.getNumChoices(representative);
			} else {
				final IterableBitSet eqStates = new IterableBitSet(equivalenceClass);
				numChoices[representative] = IteratorTools.sum(new MappingIterator.FromIntToInt(eqStates, model::getNumChoices));
				StateChoicePair[] choices = originalChoices[representative] = new StateChoicePair[numChoices[representative]];
				assert representative == equivalenceClass.nextSetBit(0);
				int choice = model.getNumChoices(representative);
				OfInt others = eqStates.iterator();
				// skip representative
				others.nextInt();
				while (others.hasNext()) {
					final int eqState = others.nextInt();
					for (int eqChoice=0, numChoices=model.getNumChoices(eqState); eqChoice < numChoices; eqChoice++) {
						choices[choice++] = new StateChoicePair(eqState, eqChoice);
					}
				}
			}
		}
		// Expensive if predecessor relation hasn't been computed yet
		hasTransitionToNonRepresentative = new ReachabilityComputer(model).computePre(identify.getNonRepresentatives());
	}

	public MDPEquiv(MDPEquiv mdpEquiv)
	{
		super(mdpEquiv);
		model = mdpEquiv.model;
		identify = mdpEquiv.identify;
		numChoices = mdpEquiv.numChoices;
		originalChoices = mdpEquiv.originalChoices;
		hasTransitionToNonRepresentative = mdpEquiv.hasTransitionToNonRepresentative;
	}



	//--- Cloneable ---

	@Override
	public MDPEquiv clone()
	{
		return new MDPEquiv(this);
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
	public boolean isInitialState(int state)
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
	public BitSet getLabelStates(String name)
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
		return numChoices[state];
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		final StateChoicePair originals = mapToOriginalModel(state, choice);
		return (originals == null) ? model.getAction(state, choice) : model.getAction(originals.state, originals.choice);
	}

	@Override
	public int getNumTransitions(final int state, final int choice)
	{
		final StateChoicePair originals = mapToOriginalModel(state, choice);
		return (originals == null) ? model.getNumTransitions(state, choice) : model.getNumTransitions(originals.state, originals.choice);
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		StateChoicePair originals = mapToOriginalModel(state, choice);
		final int originalState, originalChoice;
		if (originals == null) {
			originalState = state;
			originalChoice = choice;
		} else {
			originalState = originals.state;
			originalChoice = originals.choice;
		}
		Iterator<Integer> successors = model.getSuccessorsIterator(originalState, originalChoice);
		if (hasTransitionToNonRepresentative.get(originalState)) {
			return FilteringIterator.dedupe(new MappingIterator.From<>(successors, this::mapStateToRestrictedModel));
		}
		return successors;
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		StateChoicePair originals = mapToOriginalModel(state, choice);
		final int originalState, originalChoice;
		if (originals == null) {
			originalState = state;
			originalChoice = choice;
		} else {
			originalState = originals.state;
			originalChoice = originals.choice;
		}
		Iterator<Entry<Integer, Double>> transitions = model.getTransitionsIterator(originalState, originalChoice);
		if (hasTransitionToNonRepresentative.get(originalState)) {
			return new MappingIterator.From<>(transitions, this::mapTransitionToRestrictedModel);
		}
		return transitions;
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";
		model = MDPAdditionalChoices.fixDeadlocks(this.clone());
		identify = new EquivalenceRelationInteger();
		final int numStates = model.getNumStates();
		numChoices = new int[numStates];
		for (int state=0; state<numStates; state++) {
			numChoices[state] = model.getNumChoices(state);
		}
		originalChoices = new StateChoicePair[numStates][];
		hasTransitionToNonRepresentative = new BitSet();
	}



	//--- instance methods ---

	public Integer mapStateToRestrictedModel(final int state)
	{
		return identify.getRepresentative(state);
	}

	public SimpleImmutableEntry<Integer, Double> mapTransitionToRestrictedModel(final Entry<Integer, Double> transition)
	{
		final Integer target = identify.getRepresentative(transition.getKey());
		final Double probability = transition.getValue();
		return new SimpleImmutableEntry<>(target, probability);
	}

	public class StateChoicePair
	{
		final int state;
		final int choice;

		protected StateChoicePair(final int theState, final int theChoice)
		{
			state = theState;
			choice = theChoice;
		}

		public int getState()
		{
			return state;
		}

		public int getChoice()
		{
			return choice;
		}
	}

	public StateChoicePair mapToOriginalModel(final int state, final int choice)
	{
		StateChoicePair[] stateChoicePairs = originalChoices[state];
		if (stateChoicePairs == null) {
			return null;
		}
		return stateChoicePairs[choice];
	}



	//--- static methods ---

	public static BasicModelTransformation<MDP, MDPEquiv> transform(MDP model, EquivalenceRelationInteger identify)
	{
		return new BasicModelTransformation<>(model, new MDPEquiv(model, identify));
	}

	public static void main(String[] args) throws PrismException
	{
		MDPSimple mdpSimple = new MDPSimple();
		mdpSimple.addStates(3);
		Distribution choice = new Distribution();
		choice.add(1, 0.5);
		choice.add(2, 0.5);
		mdpSimple.addChoice(0, choice);
		choice = new Distribution();
		choice.add(2, 1.0);
		mdpSimple.addChoice(1, choice);
		choice = new Distribution();
		choice.add(1, 1.0);
		mdpSimple.addChoice(2, choice);
		System.out.println("original = " + mdpSimple);

		EquivalenceRelationInteger eq = new EquivalenceRelationInteger(new IterableArray.Of<>(BitSetTools.asBitSet(1,2)));
		MDPEquiv equiv = new MDPEquiv(mdpSimple, eq);
		System.out.println("identify = " + equiv);

		equiv.findDeadlocks(true);
		System.out.println("fixed    = " + equiv);
	}
}