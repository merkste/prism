package explicit.conditional.transformer.legacy;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.iterable.Support;
import parser.State;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.PrismException;
import explicit.BasicModelTransformation;
import explicit.CTMC;
import explicit.CTMCSimple;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.DTMCSimple;
import explicit.ReachabilityComputer;
import explicit.conditional.ExpressionInspector;

@Deprecated
public abstract class MCTwoModeTransformer extends explicit.conditional.transformer.MCConditionalTransformer
{
	protected Integer[] mappingToModeOne;
	protected Integer[] mappingToModeTwo;
	protected BitSet pivots;
	protected DTMC originalModel;
	protected DTMCSimple transformedModel;

	public MCTwoModeTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	protected boolean canHandleObjective(final DTMC model, final ExpressionConditional expression)
	{
		// cannot handle steady state computation yet
		return !ExpressionInspector.isSteadyStateReward(expression.getObjective());
	}

	@Override
	public BasicModelTransformation<DTMC, DTMC> transformModel(final DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		originalModel = model;
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		final BitSet target = getTargetStates(condition);
		final double[] probabilities = computeProbability(originalModel, condition);

		return transformModel(probabilities, target);
	}

	public BasicModelTransformation<DTMC, DTMC> transformModel(final DTMC model, final BitSet target) throws PrismException
	{
		originalModel = model;
		final double[] probabilities = computeProbabilities(target);

		return transformModel(probabilities, target);
	}

	protected BasicModelTransformation<DTMC, DTMC> transformModel(final double[] probabilities, final BitSet target) throws PrismException
	{
		transformedModel = (originalModel instanceof CTMC) ? new CTMCSimple() : new DTMCSimple();
		transformStates(probabilities, target);
		transformTransitions(probabilities);
		transformedModel.setConstantValues(originalModel.getConstantValues());
		transformedModel.findDeadlocks(false);

		final Integer[] mapping = getMappingToTransformedModel();
		final BasicModelTransformation<DTMC, DTMC> transformation = new BasicModelTransformation<DTMC, DTMC>(originalModel, transformedModel, mapping);
		originalModel = transformedModel = null;
		return transformation;
	}

	protected void transformStates(final double[] probabilities, final BitSet target)
	{
		// S' = (S_1 x {1}) U (S_2 x {2}) where
		// S_1 is "mode 1" and
		// S_2 is "mode 2"

		final BitSet support = new Support(probabilities).asBitSet();
		pivots = getPivotStates(support, target);
		final BitSet modeOne = getStatesAddedToModeOne(support);
		final BitSet modeTwo = getStatesAddedToModeTwo();
		// build state space
		final List<State> stateList = originalModel.getStatesList();
		final List<State> transformedStateList = new ArrayList<>(modeOne.cardinality() + modeTwo.cardinality());
		mappingToModeOne = new Integer[originalModel.getNumStates()];
		mappingToModeTwo = new Integer[originalModel.getNumStates()];
		for (int state = 0; state < originalModel.getNumStates(); state++) {
			if (modeOne.get(state)) {
				// add new state to mode 1
				mappingToModeOne[state] = transformedModel.addState();
				transformedStateList.add(stateList.get(state));
			}
			if (modeTwo.get(state)) {
				// add new state to mode 2
				mappingToModeTwo[state] = transformedModel.addState();
				transformedStateList.add(stateList.get(state));
			}
			if (originalModel.isInitialState(state)) {
				addInitialState(state, modeOne, modeTwo);
			}
		}
		transformedModel.setStatesList(transformedStateList);
	}

	protected void transformTransitions(final double[] probabilities)
	{
		for (int state = 0; state < originalModel.getNumStates(); state++) {
			transformTransitionsModeOne(probabilities, state);
			transformTransitionsModeTwo(state);
		}
	}

	protected BitSet getStatesAddedToModeTwo()
	{
		// "mode 2" S_2 = succ*({s from target | Pr(s) > 0})

		// compute states added to mode two == succ*(pivots)
		return new ReachabilityComputer(originalModel).computeSuccStar(pivots);
	}

	protected void transformTransitionsModeTwo(final int state)
	{
		if (mappingToModeTwo[state] != null) {
			// state is in succ*(pivots)
			final int mappedState = mappingToModeTwo[state];
			for (Iterator<Entry<Integer, Double>> iter = originalModel.getTransitionsIterator(state); iter.hasNext();) {
				// P'(s,v) = P(s,v) for all s in S_2
				final Entry<Integer, Double> transition = iter.next();
				final int mappedSuccessor = mappingToModeTwo[transition.getKey()];
				final double probability = transition.getValue();
				transformedModel.setProbability(mappedState, mappedSuccessor, probability);
			}
		}
	}

	protected Integer[] getMappingToTransformedModel()
	{
		final Integer[] mapping = new Integer[originalModel.getNumStates()];
		for (int state = 0; state < mapping.length; state++) {
			mapping[state] = projectToTransformedModel(state);
		}
		return mapping;
	}

	protected abstract void addInitialState(int state, BitSet modeOne, BitSet modeTwo);

	protected abstract BitSet getPivotStates(final BitSet support, final BitSet target);

	protected abstract BitSet getStatesAddedToModeOne(final BitSet support);

	protected abstract BitSet getTargetStates(final Expression condition) throws PrismException;

	protected abstract void transformTransitionsModeOne(final double[] probabilities, final int state);

	protected abstract double[] computeProbabilities(final BitSet target) throws PrismException;

	protected Integer projectToTransformedModel(final int state)
	{
		return pivots.get(state) ? mappingToModeTwo[state] : mappingToModeOne[state];
	}
}