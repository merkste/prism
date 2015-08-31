package explicit.conditional.transformer;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.IntPredicate;

import common.BitSetTools;
import common.functions.Mapping;
import common.functions.primitive.MappingInt;
import common.functions.primitive.PredicateInt;
import common.iterable.IterableBitSet;
import common.iterable.MappingIterator;
import common.iterable.Support;
import prism.PrismComponent;
import prism.PrismException;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.DTMCSimple;
import explicit.ModelTransformation;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.DTMCDisjointUnion;
import explicit.modelviews.DTMCRestricted;

public class ConditionalNextTransformer extends PrismComponent
{
	final DTMCModelChecker modelChecker;

	public ConditionalNextTransformer(final DTMCModelChecker modelChecker)
	{
		this.modelChecker = modelChecker;
	}

	public ModelTransformation<DTMC, DTMC> transformModel(final DTMC model, final BitSet goal, final BitSet statesOfInterest) throws PrismException
	{
		return transformModel(model, goal, false, statesOfInterest);
	}

	public ConditionalTerminalTransformation<DTMC, DTMC> transformModel(final DTMC model, final BitSet goal, final boolean negated,
			final BitSet statesOfInterest) throws PrismException
	{
		final double[] probabilities = computeProbabilities(model, goal, negated);
		final BitSet terminal = getTerminal(model, goal, negated);

		// 1. union model
		final DTMCDisjointUnion unionModel = new DTMCDisjointUnion(model, model);

		// 1. make probabilities conditional, terminal states absorbing
		final MappingInt<Iterator<Entry<Integer, Double>>> conditionalProbability = new ConditionalNext(unionModel, probabilities, terminal);
		final DTMCAlteredDistributions conditionalModel = new DTMCAlteredDistributions(unionModel, conditionalProbability);

		// 2. restrict to states reachable from support and statesOfInterest
		final Support support = new Support(probabilities);
		final PredicateInt conditionalStatesOfInterest = statesOfInterest == null ? support : ((PredicateInt) statesOfInterest::get).and((IntPredicate) support);
		final DTMCRestricted restrictedModel = new DTMCRestricted(conditionalModel, conditionalStatesOfInterest);

		// 4. create mapping from original to restricted model
		final Integer[] mapping = buildMapping(model, restrictedModel);

		// 5. create mapping of terminals from restricted model to original model
		final Map<Integer, Integer> terminalLookup = buildTerminalLookup(unionModel, terminal, restrictedModel);

		return new ConditionalTerminalTransformation<DTMC, DTMC>(model, restrictedModel, mapping, terminalLookup);
	}

	private double[] computeProbabilities(final DTMC model, final BitSet target, final boolean negated) throws PrismException
	{
		final double[] probabilities = modelChecker.computeNextProbs(model, target).soln;
		return negated ? ConditionalReachabilityTransformer.negateProbabilities(probabilities) : probabilities;
	}

	public BitSet getTerminal(final DTMC model, final BitSet goal, boolean negated)
	{
		if (!negated) {
			return goal;
		}
		final BitSet terminal = (BitSet) goal.clone();
		terminal.flip(0, model.getNumStates());
		return terminal;
	}

	private Integer[] buildMapping(final DTMC model, final DTMCRestricted restrictedModel)
	{
		// FIXME ALG: consider mapping impl as fn instead of array, move to DMTCRestricted ?
		final Integer[] mapping = new Integer[model.getNumStates()];
		for (int state = 0; state < mapping.length; state++) {
			mapping[state] = restrictedModel.mapStateToRestrictedModel(state);
		}
		return mapping;
	}

	public Map<Integer, Integer> buildTerminalLookup(final DTMCDisjointUnion model, final BitSet terminal, final DTMCRestricted restrictedModel)
	{
		final int offset = model.offset;
		final Map<Integer, Integer> terminalLookup = new HashMap<>();
		for (Integer state : new IterableBitSet(terminal)) {
			final Integer restrictedState = restrictedModel.mapStateToRestrictedModel(state + offset);
			if (restrictedState != null) {
				terminalLookup.put(restrictedState, state);
			}
		}
		return terminalLookup;
	}

	private final class ConditionalNext implements MappingInt<Iterator<Entry<Integer, Double>>>
	{
		private final DTMC model;
		private final double[] probabilities;
		private final Support support;
		private final BitSet terminal;
		private final int offset;

		private ConditionalNext(final DTMC model, final double[] probabilities, final BitSet terminal)
		{
			this.model = model;
			this.probabilities = probabilities;
			this.support = new Support(probabilities);
			this.terminal = terminal;
			offset = probabilities.length;
		}

		@Override
		public Iterator<Entry<Integer, Double>> apply(final int state)
		{
			if (!support.test(state)) {
				// deadlock irrelevant and terminal states
				return Collections.emptyIterator();
			}
			// apply conditional probability & redirect to second part of the model
			final double stateProbability = probabilities[state];
			assert stateProbability > 0 : "expected non-zero probability";

			final Mapping<Entry<Integer, Double>, Entry<Integer, Double>> conditionalProbability = new Mapping<Entry<Integer, Double>, Entry<Integer, Double>>()
			{
				@Override
				public final Entry<Integer, Double> apply(Entry<Integer, Double> transition)
				{
					final int target = transition.getKey();
					final int mappedTarget = target + offset;
					final double probability = transition.getValue();
					final double conditionalProbability = terminal.get(target) ? probability / stateProbability : 0.0;
					return new AbstractMap.SimpleImmutableEntry<>(mappedTarget, conditionalProbability);
				}
			};

			return new MappingIterator.From<>(model.getTransitionsIterator(state), conditionalProbability);
		}
	}

	public static void main(final String[] args) throws PrismException
	{
		final DTMCSimple model = new DTMCSimple(4);
		model.addInitialState(0);
		model.setProbability(0, 1, 0.1);
		model.setProbability(0, 2, 0.9);
		model.setProbability(1, 2, 0.8);
		model.setProbability(1, 3, 0.2);
		model.setProbability(2, 1, 0.3);
		model.setProbability(2, 2, 0.7);
		model.setProbability(3, 3, 1.0);

		System.out.println("Original Model:");
		System.out.print(model.infoStringTable());
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(model.getDeadlockStates()));
		System.out.println(model);

		System.out.println();

		final ConditionalNextTransformer transformer = new ConditionalNextTransformer(new DTMCModelChecker(null));
		final BitSet target = BitSetTools.asBitSet(2);
		final BitSet statesOfInterest = BitSetTools.asBitSet(0, 3);
		DTMC transformedModel;

		System.out.println();

		System.out.println("Conditional Model, next step, target=" + target + ", statesOfInterest=" + statesOfInterest + ":");
		transformedModel = transformer.transformModel(model, target, statesOfInterest).getTransformedModel();
		System.out.print(transformedModel.infoStringTable());
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(transformedModel.getDeadlockStates()));
		System.out.println(transformedModel);

		System.out.println();

		System.out.println("Conditional Model, negated next step, target=" + target + ", statesOfInterest=" + statesOfInterest + ":");
		transformedModel = transformer.transformModel(model, target, true, statesOfInterest).getTransformedModel();
		System.out.print(transformedModel.infoStringTable());
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(transformedModel.getDeadlockStates()));
		System.out.println(transformedModel);
	}
}