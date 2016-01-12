package explicit.conditional.transformer.mc;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

import common.BitSetTools;
import common.functions.Mapping;
import common.functions.Predicate;
import common.functions.primitive.MappingInt;
import common.functions.primitive.PredicateInt;
import common.iterable.FilteringIterator;
import common.iterable.IterableBitSet;
import common.iterable.MappingIterator;
import common.iterable.Support;
import prism.PrismComponent;
import prism.PrismException;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.DTMCSimple;
import explicit.PredecessorRelation;
import explicit.conditional.transformer.ConditionalTerminalTransformation;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.DTMCRestricted;

public class ConditionalReachabilityTransformer extends PrismComponent
{
	private DTMCModelChecker modelChecker;

	public ConditionalReachabilityTransformer(final DTMCModelChecker modelChecker)
	{
		this.modelChecker = modelChecker;
	}

	public ConditionalTerminalTransformation<DTMC, DTMC> transformModel(final DTMC model, final BitSet remain, final BitSet goal, final BitSet statesOfInterest)
			throws PrismException
	{
		return transformModel(model, remain, goal, false, statesOfInterest);
	}

	public ConditionalTerminalTransformation<DTMC, DTMC> transformModel(final DTMC model, final BitSet remain, final BitSet goal, final BitSet statesOfInterest,
			final boolean collapse) throws PrismException
	{
		return this.transformModel(model, remain, goal, false, statesOfInterest, collapse);
	}

	public ConditionalTerminalTransformation<DTMC, DTMC> transformModel(final DTMC model, final BitSet remain, final BitSet goal, final boolean negated,
			final BitSet statesOfInterest) throws PrismException
	{
		return transformModel(model, remain, goal, negated, statesOfInterest, true);
	}

	public ConditionalTerminalTransformation<DTMC, DTMC> transformModel(final DTMC model, final BitSet remain, final BitSet goal, final boolean negated,
			final BitSet statesOfInterest, final boolean collapse) throws PrismException
	{
		// 1. compute probabilities of constraint reachability
		// FIXME ALG: move exact results to ModelCheckerResults
		final PredecessorRelation pre = model.getPredecessorRelation(modelChecker, true);
		final BitSet prob0 = modelChecker.prob0(model, remain, goal, pre);
		final BitSet prob1 = modelChecker.prob1(model, remain, goal, pre);
		final double[] probabilities = computeProbabilities(model, remain, goal, prob0, prob1, negated);

		// 2. identify terminal states
		final BitSet terminal;
		if (collapse) {
			terminal = negated ? prob0 : prob1;
		} else {
			terminal = getTerminal(model, remain, goal, negated);
		}

		// 1. make probabilities conditional, deadlock terminal states and states where prob = 0
		final ConditionalReachability conditionalProbability = new ConditionalReachability(model, probabilities, terminal);
		final DTMCAlteredDistributions conditionalModel = new DTMCAlteredDistributions(model, conditionalProbability);

		// 2. restrict to states reachable from statesOfInterest where prob > 0
		final Support support = new Support(probabilities);
		final PredicateInt conditionalStatesOfInterest = statesOfInterest == null ? support : ((PredicateInt) statesOfInterest::get).and((IntPredicate) support);
		final DTMCRestricted restrictedModel = new DTMCRestricted(conditionalModel, conditionalStatesOfInterest);

		// 3. create mapping from original to restricted model
		final Integer[] mapping = buildMapping(model, restrictedModel);

		// 4. create mapping of terminals from restricted model to original model
		final Map<Integer, Integer> terminalLookup = buildTerminalLookup(terminal, restrictedModel);

		return new ConditionalTerminalTransformation<DTMC, DTMC>(model, restrictedModel, mapping, terminalLookup);
	}

	private double[] computeProbabilities(final DTMC model, final BitSet remain, final BitSet target, final BitSet prob0, final BitSet prob1, final boolean negated) throws PrismException
	{
		final double[] init = new double[model.getNumStates()]; // initialized with 0.0's
		for (OfInt iter = new IterableBitSet(prob1).iterator(); iter.hasNext();) {
			init[iter.nextInt()] = 1.0;
		}
		final BitSet known = BitSetTools.union(prob0, prob1);
		final double[] probabilities = modelChecker.computeReachProbs(model, remain, target, init, known).soln;
		return negated ? negateProbabilities(probabilities) : probabilities;
	}

	public static double[] negateProbabilities(final double[] probabilities)
	{
		for (int state = 0; state < probabilities.length; state++) {
			probabilities[state] = 1 - probabilities[state];
		}
		return probabilities;
	}

	public BitSet getTerminal(final DTMC model, final BitSet remain, final BitSet goal, final boolean negated)
	{
		// (!strict) let terminal states be all states with prob = 1;

		if (!negated) {
			return goal;
		}
		// terminal = ! (remain | goal)
		if (remain == null || remain.cardinality() == model.getNumStates()) {
			return new BitSet();
		}
		final BitSet terminal = BitSetTools.union(remain, goal);
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

	public Map<Integer, Integer> buildTerminalLookup(final BitSet terminal, final DTMCRestricted restrictedModel)
	{
		final Map<Integer, Integer> terminalLookup = new HashMap<>();
		for (Integer state : new IterableBitSet(terminal)) {
			final Integer restrictedState = restrictedModel.mapStateToRestrictedModel(state);
			if (restrictedState != null) {
				terminalLookup.put(restrictedState, state);
			}
		}
		return terminalLookup;
	}

	private class ConditionalReachability implements MappingInt<Iterator<Entry<Integer, Double>>>
	{
		private final DTMC model;
		private final double[] probabilities;
		private final Support support;
		private final BitSet terminal;

		private ConditionalReachability(final DTMC model, final double[] probabilities, final BitSet terminal)
		{
			this.model = model;
			this.probabilities = probabilities;
			this.support = new Support(probabilities);
			this.terminal = terminal;
		}

		@Override
		public Iterator<Entry<Integer, Double>> apply(final int state)
		{
			if (!support.test(state) || terminal.get(state)) {
				// deadlock irrelevant and terminal states
				return Collections.emptyIterator();
			}
			// apply conditional probability
			final double stateProbability = probabilities[state];
			assert stateProbability > 0 : "expected non-zero probability";
			if (stateProbability == 1.0) {
				return null;
			}

			final Predicate<Entry<Integer, Double>> inSupport = support.compose((ToIntFunction<Entry<Integer, Double>>) Entry::getKey);

			final Mapping<Entry<Integer, Double>, Entry<Integer, Double>> conditionalProbability = new Mapping<Entry<Integer, Double>, Entry<Integer, Double>>()
			{
				@Override
				public final Entry<Integer, Double> apply(final Entry<Integer, Double> transition)
				{
					final int target = transition.getKey();
					final double probability = transition.getValue();
					final double conditionalProbability = (probability * probabilities[target]) / stateProbability;
					return new AbstractMap.SimpleImmutableEntry<>(target, conditionalProbability);
				}
			};

			return new MappingIterator.From<>(new FilteringIterator.Of<>(model.getTransitionsIterator(state), inSupport), conditionalProbability);
		}
	}

	public static void main(final String[] args) throws PrismException
	{
		final DTMCSimple model = new DTMCSimple(7);
		model.addInitialState(0);
		model.setProbability(0, 1, 0.5);
		model.setProbability(0, 2, 0.3);
		model.setProbability(0, 3, 0.2);

		model.setProbability(1, 5, 1.0);

		model.setProbability(2, 1, 0.9);
		model.setProbability(2, 6, 0.1);

		model.setProbability(3, 3, 0.5);
		model.setProbability(3, 4, 0.3);
		model.setProbability(3, 6, 0.2);

		model.setProbability(4, 6, 1.0);

		model.setProbability(5, 5, 1.0);

		model.setProbability(6, 1, 0.6);
		model.setProbability(6, 6, 0.4);

		System.out.println("Original Model:");
		System.out.print(model.infoStringTable());
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(model.getDeadlockStates()));
		System.out.println(model);

		System.out.println();

		final BitSet target = BitSetTools.asBitSet(6);
		final BitSet statesOfInterest = BitSetTools.asBitSet(0, 1);

		final ConditionalReachabilityTransformer transformer = new ConditionalReachabilityTransformer(new DTMCModelChecker(null));
		ConditionalTerminalTransformation<DTMC, DTMC> transformation;
		DTMC transformedModel;

		System.out.println();

		System.out.println("Conditional Model, reachabilitiy, target=" + target + ", statesOfInterest=" + statesOfInterest + ":");
		transformation = transformer.transformModel(model, null, target, statesOfInterest);
		transformedModel = transformation.getTransformedModel();
		System.out.print(transformedModel.infoStringTable());
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(transformedModel.getDeadlockStates()));
		System.out.println(transformedModel);

		System.out.println();

		final BitSet remain = BitSetTools.asBitSet(0, 1, 2);
		System.out.println(
				"Conditional Model, constrained reachabilitiy, remain=" + remain + " target=" + target + ", statesOfInterest=" + statesOfInterest + ":");
		transformation = transformer.transformModel(model, remain, target, statesOfInterest);
		transformedModel = transformation.getTransformedModel();
		System.out.print(transformedModel.infoStringTable());
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(transformedModel.getDeadlockStates()));
		System.out.println(transformedModel);

		System.out.println();

		System.out.println("Conditional Model, negated constrained reachabilitiy, remain=" + remain + " target=" + target + ", statesOfInterest="
				+ statesOfInterest + ":");
		transformation = transformer.transformModel(model, remain, target, true, statesOfInterest);
		transformedModel = transformation.getTransformedModel();
		System.out.print(transformedModel.infoStringTable());
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(transformedModel.getDeadlockStates()));
		System.out.println(transformedModel);
	}
}