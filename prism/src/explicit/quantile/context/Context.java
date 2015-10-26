package explicit.quantile.context;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import common.BitSetTools;
import common.iterable.collections.SetFactory;
import common.iterable.collections.SortedSingletonSet;
import parser.ast.RelOp;
import prism.PrismException;
import explicit.quantile.QuantileUtilities;
import explicit.quantile.context.helpers.QuantitativeCalculationHelper;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.Pair;
import explicit.quantile.dataStructure.RewardWrapper;

/**
 * This class encapsulates the information necessary for the calculation of quantile queries.
 * @author Marcus Daum (mdaum@tcs.inf.tu-dresden.de)
 */
public abstract class Context
{
	protected final QuantileUtilities quantileUtilities;
	protected final BitSet statesOfInterest;

	public BitSet getStatesOfInterest()
	{
		return statesOfInterest;
	}

	public Context(RewardWrapper theModel,
			BitSet theZeroStateRewardStatesWithZeroRewardTransition, BitSet theZeroStateRewardStatesWithMixedTransitionRewards, BitSet theZeroValueStates,
			BitSet theStatesOfInterest, QuantileUtilities theQuantileUtilities)
	{
		statesOfInterest = theStatesOfInterest;
		quantileUtilities = theQuantileUtilities;
		model = theModel;
		final SetFactory setFactory = quantileUtilities.getSetFactory();
		zeroStateRewardStatesWithZeroRewardTransition = setFactory.getSet(theZeroStateRewardStatesWithZeroRewardTransition);
		zeroStateRewardStatesWithMixedTransitionRewards = setFactory.getSet(theZeroStateRewardStatesWithMixedTransitionRewards);
		zeroValueStates = setFactory.getSet(theZeroValueStates);
		//XXX: see how you can do this with the help of
		//model.getPositiveStateRewardStates() in an appropriate fashion
		final BitSet thePositiveRewardStates = BitSetTools.complement(model.getNumStates(), theZeroStateRewardStatesWithZeroRewardTransition);
		thePositiveRewardStates.andNot(theZeroValueStates);
		positiveRewardStates = setFactory.getSet(thePositiveRewardStates);
	}

	protected final RewardWrapper model;

	public RewardWrapper getModel()
	{
		return model;
	}

	protected final Set<Integer> zeroValueStates;

	public Set<Integer> getZeroValueStates()
	{
		return zeroValueStates;
	}

	protected final Set<Integer> positiveRewardStates;

	public Set<Integer> getPositiveRewardStates()
	{
		return positiveRewardStates;
	}

	private final Set<Integer> zeroStateRewardStatesWithZeroRewardTransition;

	public Set<Integer> getZeroStateRewardStatesWithZeroRewardTransition()
	{
		return zeroStateRewardStatesWithZeroRewardTransition;
	}

	private final Set<Integer> zeroStateRewardStatesWithMixedTransitionRewards;

	public abstract boolean pickMaximum();

	public boolean pickMinimum()
	{
		return !pickMaximum();
	}

	public abstract int getResultAdjustment();

	public abstract RelOp getRelationOperator();

	public abstract Map<Double, BitSet> determineFiniteQuantileStates() throws PrismException;

	public abstract void calculateDerivableStates(CalculatedValues values, int rewardStep);

	public void calculateZeroRewardStatesSequential(final CalculatedValues values, final List<Set<Integer>> zeroRewardOrder, final int rewardStep)
			throws PrismException
	{
		for (Set<Integer> scc : zeroRewardOrder){
			if (scc.size() == 1) {
				for (int state : scc){
					values.setCurrentValue(state, calculateZeroRewardState(state, values, rewardStep));
				}
			} else {
				final Pair<double[], Map<Integer, Integer>> result = calculateSet(scc, values, rewardStep);
				final double[] calculatedValues = result.getFirst();
				final Map<Integer, Integer> stateToIndex = result.getSecond();
				for (int state : scc){
					values.setCurrentValue(state, calculatedValues[stateToIndex.get(state)]);
				}
			}
			assert (values.allStatesAreDefined(scc)) : "At least one SCC was not computed properly!";
		}
	}

	public void calculateZeroRewardStatesParallel(final CalculatedValues values, final List<Set<Set<Integer>>> zeroRewardOrder, final int rewardStep, final ExecutorService workerPool)
	{
		for (Set<Set<Integer>> parallelTier : zeroRewardOrder){
			try {
				workerPool.submit(
					() -> parallelTier.parallelStream().forEach(
						scc -> {
							if (scc.size() == 1) {
								for (int state : scc){
									values.setCurrentValue(state, calculateZeroRewardState(state, values, rewardStep));
								}
							} else {
								try {
									final Pair<double[], Map<Integer, Integer>> result = calculateSet(scc, values, rewardStep);
									final double[] calculatedValues = result.getFirst();
									final Map<Integer, Integer> stateToIndex = result.getSecond();
									for (int state : scc){
										values.setCurrentValue(state, calculatedValues[stateToIndex.get(state)]);
									}
								} catch (PrismException e){
									throw new RuntimeException(e.getMessage());
								}
							}
							assert (values.allStatesAreDefined(scc)) : "At least one SCC was not computed properly!";
						}
					)
				).get();
			} catch (InterruptedException | ExecutionException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private double calculateZeroRewardState(final int state, final CalculatedValues values, final int rewardStep)
	{
		double result = CalculatedValues.UNDEFINED;
		for (int choice = 0, numChoices = model.getNumChoices(state); choice < numChoices; choice++){
			if (model.getTransitionReward(state, choice) == 0){
				result = QuantitativeCalculationHelper.pickReasonableValue(result, calculateZeroRewardTransitionForZeroRewardState(state, choice, values), pickMaximum());
			} else {
				result = QuantitativeCalculationHelper.pickReasonableValue(result, calculatePositiveRewardTransitionForZeroRewardState(state, choice, values, rewardStep), pickMaximum());
			}
		}
		return result;
	}
	protected abstract double calculateZeroRewardTransitionForZeroRewardState(final int state, final int choice, final CalculatedValues values);
	public abstract double calculatePositiveRewardTransitionForZeroRewardState(final int state, final int choice, final CalculatedValues values, final int rewardStep);

	public Pair<double[], Map<Integer, Integer>> calculateSet(Set<Integer> set, CalculatedValues values, int rewardStep) throws PrismException
	{
		return quantileUtilities.getMultiStateSolutionMethod().solveMultipleStates(this, values, set, zeroStateRewardStatesWithMixedTransitionRewards, rewardStep, quantileUtilities);
	}

	public void calculateZeroRewardTransitionsForZeroStateRewardStatesTryAndSet(CalculatedValues values, List<Set<Integer>> zeroRewardOrder, int rewardStep)
	{
		//the best order for calculating the Z-values will be generated in the very first iteration
		//but be aware, the very first iteration when using lower reward bounds is 1, and 0 for upper reward bounds
		int veryFirstIteration = 0;
		if (this instanceof Context4ExpressionQuantileProbLowerRewardBound)
			veryFirstIteration = 1;
		if (rewardStep == veryFirstIteration) {
			Set<Integer> statesWaitingForCalculation = values.getUndefinedStates();
			boolean doRepetition = true;
			while (doRepetition) {
				doRepetition = false;
				selectionLoop: for (Iterator<Integer> it = statesWaitingForCalculation.iterator(); it.hasNext();) {
					int state = it.next();
					for (int choice : model.getZeroRewardChoices(state)) {
						for (Iterator<Integer> successorsIterator = model.getSuccessorsIterator(state, choice); successorsIterator.hasNext();) {
							int successor = successorsIterator.next();
							if (values.getCurrentValue(successor) == CalculatedValues.UNDEFINED && successor != state)
								//since the calculation will not be successful, try another state
								continue selectionLoop;
						}
					}
					values.setCurrentValue(state, calculateZeroRewardState(state, values, rewardStep));
					doRepetition = true;
					Set<Integer> singletonSet = new SortedSingletonSet<Integer>(state);
					zeroRewardOrder.add(singletonSet);
					it.remove();
				}
			}
		} else {
			//rewardStep > 0    ==>    the best order to go through the Z-states was already determined
			for (Set<Integer> singletonSet : zeroRewardOrder) {
				for (int state : singletonSet)
					values.setCurrentValue(state, calculateZeroRewardState(state, values, rewardStep));
			}
		}
	}

	public double getHighestThreshold()
	{
		List<Double> thresholds = quantileUtilities.getThresholds();
		return thresholds.get(thresholds.size() - 1);
	}

	public abstract int getNumberOfDerivableStates();
}