package explicit.modelviews;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;

import common.IteratorTools;
import common.iterable.IterableBitSet;
import common.iterable.IterableStateSet;
import common.iterable.MappingIterator;
import common.iterable.SingletonIterator;
import explicit.DiracDistribution;
import explicit.MDP;
import explicit.ReachabilityComputer;

public class MDPEquivSelfLoopsForNonRepresentatives extends MDPEquiv
{
	public MDPEquivSelfLoopsForNonRepresentatives(final MDP model, final EquivalenceRelationInteger identify)
	{
		//XXX: refactor to avoid code-duplications with MDPEquiv
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
					//state is not the representative of its equivalence-class
					// => it receives a self-loop
					numChoices[eqState] = 1;
				}
			}
		}
		// Expensive if predecessor relation hasn't been computed yet
		hasTransitionToNonRepresentative = new ReachabilityComputer(model).computePre(identify.getNonRepresentatives());
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		final StateChoicePair originals = mapToOriginalModel(state, choice);
		if (originals != null){
			return model.getAction(originals.state, originals.choice);
		}
		if (identify.isRepresentative(state)){
			return model.getAction(state, choice);
		}
		return null;
	}

	@Override
	public int getNumTransitions(final int state, final int choice)
	{
		final StateChoicePair originals = mapToOriginalModel(state, choice);
		if (originals != null){
			return model.getNumTransitions(originals.state, originals.choice);
		}
		if (identify.isRepresentative(state)){
			return model.getNumTransitions(state, choice);
		}
		//non-representatives have self-loops
		return 1;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		if (! identify.isRepresentative(state)){
			return new SingletonIterator.OfInt(state);
		}
		return super.getSuccessorsIterator(state, choice);
	}

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		if (! identify.isRepresentative(state)){
			return new DiracDistribution(state).iterator();
		}
		return super.getTransitionsIterator(state, choice);
	}
}