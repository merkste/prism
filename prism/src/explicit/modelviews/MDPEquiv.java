package explicit.modelviews;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

import common.BitSetTools;
import common.functions.primitive.PairPredicateInt;
import common.iterable.FunctionalIterator;
import common.iterable.Interval;
import common.iterable.IterableArray;
import common.iterable.IterableBitSet;
import common.iterable.collections.UnionSet;
import explicit.BasicModelTransformation;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import explicit.ModelTransformation;
import explicit.ReachabilityComputer;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;

/**
 * An MDPEquiv is the quotient MDP with respect to an equivalence relation.
 * For efficiency, non-representative states are not removed but deadlocked.
 */
public class MDPEquiv extends MDPView
{
	public static final boolean COMPLEMENT = true;

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
		for (OfInt representatives = identify.getRepresentatives(new Interval(numStates)).iterator(); representatives.hasNext();) {
			final int representative = representatives.nextInt();
			BitSet equivalenceClass = this.identify.getEquivalenceClassOrNull(representative);
			if (equivalenceClass == null || equivalenceClass.cardinality() == 1) {
				//the equivalence-class consists only of one state
				// => leave it as it is
				numChoices[representative] = model.getNumChoices(representative);
			} else {
				final IterableBitSet eqStates = new IterableBitSet(equivalenceClass);
				numChoices[representative] = eqStates.map((IntUnaryOperator) model::getNumChoices).sum();
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
		return super.hasLabel(name) ? super.getLabelStates(name) : model.getLabelStates(name);
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



	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		return numChoices[state];
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		final StateChoicePair originals = mapToOriginalModelOrNull(state, choice);
		return (originals == null) ? model.getAction(state, choice) : model.getAction(originals.state, originals.choice);
	}

	@Override
	public int getNumTransitions(final int state, final int choice)
	{
		final StateChoicePair originals = mapToOriginalModelOrNull(state, choice);
		return (originals == null) ? model.getNumTransitions(state, choice) : model.getNumTransitions(originals.state, originals.choice);
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		StateChoicePair originals = mapToOriginalModelOrNull(state, choice);
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
			return FunctionalIterator.extend(successors).map((ToIntFunction<Integer>) identify::getRepresentative).dedupe();
		}
		return successors;
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		StateChoicePair originals = mapToOriginalModelOrNull(state, choice);
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
			return FunctionalIterator.extend(transitions).map(this::mapToRepresentative);
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

	public StateChoicePair mapToOriginalModel(final int state, final int choice)
	{
		StateChoicePair original = mapToOriginalModelOrNull(state, choice);
		return (original == null) ? new StateChoicePair(state, choice) : original;
	}

	public StateChoicePair mapToOriginalModelOrNull(final int state, final int choice)
	{
		if (! identify.isRepresentative(state)) {
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}
		StateChoicePair[] stateChoicePairs = originalChoices[state];
		if (stateChoicePairs == null) {
			return null;
		}
		return stateChoicePairs[choice];
	}



	//--- instance methods ---
	
	public Entry<Integer, Double> mapToRepresentative(final Entry<Integer, Double> transition)
	{
		int state          = transition.getKey();
		int representative = identify.getRepresentative(state);
		if (state == representative) {
			return transition;
		}
		Double probability = transition.getValue();
		return new SimpleImmutableEntry<>(representative, probability);
	}



	//--- static methods ---

	public static ModelTransformation<MDP, ? extends MDP> transform(MDP model, EquivalenceRelationInteger identify)
	{
		return transform(model, identify, false);
	}

	public static ModelTransformation<MDP, ? extends MDP> transform(MDP model, EquivalenceRelationInteger identify, boolean removeNonRepresentatives)
	{
		BasicModelTransformation<MDP,MDPEquiv> quotient = new BasicModelTransformation<>(model, new MDPEquiv(model, identify));
		if (! removeNonRepresentatives) {
			return quotient;
		}
		BitSet representatives = identify.getRepresentatives(model.getNumStates());
		BasicModelTransformation<MDP, MDPRestricted> restriction = MDPRestricted.transform(quotient.getTransformedModel(), representatives, Restriction.TRANSITIVE_CLOSURE_SAFE);
		return restriction.compose(quotient);
	}

	public static BasicModelTransformation<MDP, MDPEquiv> transformDroppingLoops(MDP model, EquivalenceRelationInteger identify)
	{
		final MDPDroppedChoices dropped = new MDPDroppedChoices(model, new PairPredicateInt()
		{
			@Override
			public boolean test(final int state, final int choice)
			{
				Iterator<Integer> successors = model.getSuccessorsIterator(state, choice);
				while (successors.hasNext()){
					if (! identify.test(state, (int) successors.next())){
						return false;
					}
				}
				return true;
			}
		});
		return new BasicModelTransformation<>(model, new MDPEquiv(dropped, identify));
	}

	public static void main(String[] args) throws PrismException
	{
		MDPSimple mdp = new MDPSimple();
		mdp.addStates(3);
		Distribution choice = new Distribution();
		choice.add(1, 0.5);
		choice.add(2, 0.5);
		mdp.addActionLabelledChoice(0, choice, 'a');
		choice = new Distribution();
		choice.add(2, 1.0);
		mdp.addActionLabelledChoice(1, choice, 'b');
		choice = new Distribution();
		choice.add(1, 1.0);
		mdp.addActionLabelledChoice(2, choice, 'c');
		System.out.println("original = " + mdp);

		EquivalenceRelationInteger eq = new EquivalenceRelationInteger(new IterableArray.Of<>(BitSetTools.asBitSet(1,2)));
		MDPEquiv equiv = new MDPEquiv(mdp, eq);
		System.out.println("identify = " + equiv);
		equiv.findDeadlocks(true);
		System.out.println("fixed    = " + equiv);
	}



	public static class StateChoicePair
	{
		final int state;
		final int choice;
	
		protected StateChoicePair(final int state, final int choice)
		{
			this.state = state;
			this.choice = choice;
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
}