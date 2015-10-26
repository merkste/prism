package explicit.quantile.context.helpers.multiStateSolutions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import common.PrimitiveArrayConverter;
import explicit.ModelCheckerResult;
import explicit.quantile.QuantileUtilities;
import explicit.quantile.context.Context;
import explicit.quantile.context.Context4ExpressionQuantileProb;
import explicit.quantile.context.Context4ExpressionQuantileProbLowerRewardBound;
import explicit.quantile.context.Context4ExpressionQuantileProbUpperRewardBound;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.Pair;
import explicit.quantile.dataStructure.RewardWrapper;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import prism.ModelType;
import prism.PrismException;

public class LinearProgramComputer
{
	private static int determineLpOperator(ModelType modelType, boolean greaterEquals) throws PrismException
	{
		switch (modelType) {
		case DTMC:
			return LpSolve.EQ;
		case MDP:
			if (greaterEquals)
				return LpSolve.GE;
			else
				return LpSolve.LE;
		default:
			throw new PrismException(modelType + " is not yet supported");
		}
	}

	private static void determineVariableBounds(LpSolve solver, Context context, int variables) throws LpSolveException
	{
		//the variable indizes start with 1 instead of 0 for lpSolve
		variables++;
		for (int variable = 1; variable < variables; variable++) {
			//obsolete, since every variable is bounded by 0 from below (default case for LpSolve)
			//but since it is not wrong I leave this line
			solver.setLowbo(variable, 0);
			if (context instanceof Context4ExpressionQuantileProb){
				//probability quantiles operate with probabilities -> 1 is the highest value
				solver.setUpbo(variable, 1);
			}
			//expectation quantiles operate with unbounded reals -> infinity is the highest value
			//this is the default case for LpSolve
		}
	}

	public static Pair<double[], Map<Integer, Integer>> lpSolver(Context context, CalculatedValues values, Set<Integer> set, int rewardStep, int debugLevel) throws PrismException
	{
		assert (set != null);
		LpSolve solver = null;
		double[] result;
		Map<Integer, Integer> stateToLpIndex;
		try {
			final int states = set.size();
			//BEWARE: the indizes within LpSolve begin with value 1 instead of 0
			solver = LpSolve.makeLp(0, states);
			solver.setVerbose(LpSolve.CRITICAL);
			solver.setAddRowmode(true);
			stateToLpIndex = new HashMap<>(states);
			determineVariableBounds(solver, context, states);
			int[] indizes = new int[states];
			double[] coefficients = new double[states];
			int index = 0;
			for (int state : set){
				coefficients[index] = 1;
				indizes[index] = index+1;
				index++;
				stateToLpIndex.put(state, index);
				solver.setColName(index, "X_"+state);
			}
			for (int state : set){
				assert (context.getModel().getStateReward(state) == 0);
				for (int choice = 0, numChoices = context.getModel().getNumChoices(state); choice < numChoices; choice++){
					if (context.getModel().getTransitionReward(state, choice) > 0){
						double valueForPositiveRewardTransition = context.calculatePositiveRewardTransitionForZeroRewardState(state, choice, values, rewardStep);
						int operator = determineLpOperator(context.getModel().getModelType(), context.pickMaximum());
						solver.addConstraintex(1, new double[] { 1.0 }, new int[] { stateToLpIndex.get(state) }, operator, valueForPositiveRewardTransition);
					} else {
						assert (context.getModel().getTransitionReward(state, choice) == 0): "negative transition rewards are NOT supported!";
						addZeroRewardLpConstraint(solver, context, values, state, choice, stateToLpIndex);
					}
				}
			}
			solver.setAddRowmode(false);
			solver.setObjFnex(indizes.length, coefficients, indizes);
			//has no influence on DTMCs
			if (context.pickMaximum())
				solver.setMinim();
			else
				solver.setMaxim();
			if (debugLevel >= 10)
				solver.writeLp("outputLpSolve.lp");
			solver.solve();
			result = solver.getPtrVariables();
		} catch (LpSolveException e) {
			throw new PrismException(e.getMessage());
		} finally {
			solver.deleteLp();
		}
		for (Integer key : stateToLpIndex.keySet()){
			//LpSolve begins to count from 1 and Java counts from 0
			// --> for mapping to correct indizes one has to convert to Java-counting
			stateToLpIndex.put(key, stateToLpIndex.get(key)-1);
		}
		return new Pair<>(result, stateToLpIndex);
	}

	private static void addZeroRewardLpConstraint(LpSolve solver, Context context, CalculatedValues values, int state, int choice, Map<Integer, Integer> stateToLpIndex) throws PrismException, LpSolveException
	{
		List<Integer> successors = new ArrayList<Integer>();
		List<Double> probabilities = new ArrayList<Double>();
		double currentValue = calculateSuccessorProbabilities(successors, probabilities, context.getModel(), values, state, choice, stateToLpIndex);
		int[] indizes = PrimitiveArrayConverter.convertToIntegerArray(successors);
		double[] probs = PrimitiveArrayConverter.convertToDoubleArray(probabilities);
		final int operator = determineLpOperator(context.getModel().getModelType(), context.pickMinimum());
		solver.addConstraintex(indizes.length, probs, indizes, operator, currentValue);
	}

	private static void addLpConstraint(LpSolve solver, Context context, int state, int choice, int currentReward, int lookupReward) throws PrismException, LpSolveException
	{
		List<Integer> successors = new ArrayList<Integer>();
		List<Double> probabilities = new ArrayList<Double>();
		calculateSuccessorProbabilities(successors, probabilities, context.getModel(), state, choice, currentReward, lookupReward);
		int[] indizes = PrimitiveArrayConverter.convertToIntegerArray(successors);
		double[] probs = PrimitiveArrayConverter.convertToDoubleArray(probabilities);
		int operator = determineLpOperator(context.getModel().getModelType(), context.pickMinimum());
		solver.addConstraintex(indizes.length, probs, indizes, operator, 0);
	}

	public static ModelCheckerResult lpSolverExclusively(Context4ExpressionQuantileProb context, int rewardBound,
			QuantileUtilities quantileUtilities) throws PrismException
	{
		final long timer = System.currentTimeMillis();
		quantileUtilities.getLog().println("Using external LP-solver for the reward bounds [0.." + (rewardBound - 1) + "]:");
		double[] probabilities;
		int states = context.getModel().getNumStates();
		if (context instanceof Context4ExpressionQuantileProbLowerRewardBound){
			probabilities = lpSolverExclusively((Context4ExpressionQuantileProbLowerRewardBound) context, states, rewardBound, quantileUtilities);
		} else {
			assert (context instanceof Context4ExpressionQuantileProbUpperRewardBound);
			probabilities = lpSolverExclusively((Context4ExpressionQuantileProbUpperRewardBound) context, states, rewardBound, quantileUtilities);
		}
		for (int rew = 0; rew < rewardBound; rew++) {
			double[] currentProbabilities = new double[states];
			System.arraycopy(probabilities, rew*states, currentProbabilities, 0, states);
			quantileUtilities.setQuantileForReward(rew, context, currentProbabilities, timer);
			if (quantileUtilities.finiteQuantileStatesAreDetermined()) {
				if (quantileUtilities.getDebugLevel() >= 4)
					quantileUtilities.getLog().println("result = " + Arrays.toString(currentProbabilities));
				break;
			}
		}
		return quantileUtilities.prepareResults(states, timer, rewardBound);
	}

	private static double[] lpSolverExclusively(Context4ExpressionQuantileProbLowerRewardBound context, int states, int rewardBound,
			QuantileUtilities quantileUtilities) throws PrismException
	{
		//XXX: REFACTORING !!!!
		//XXX: hier fehlt noch die Bedingung fuer 0 probability wenn es einen scheduler gibt, der keinen Reward aufsammelt (ist nur kritisch bei lower reward und minimise)
		LpSolve solver = null;
		try {
			//BEWARE: the indizes within LpSolve begin with value 1 instead of 0
			//we have rewardBound * states different variables
			solver = LpSolve.makeLp(0, rewardBound * states);
			solver.setVerbose(LpSolve.CRITICAL);
			solver.setAddRowmode(true);
			determineVariableBounds(solver, context, rewardBound * states);
			for (int state = 0; state < states; state++) {
				solver.setColName(state+1, "X_"+state+"_0");
				//reward 0 -> extremal probabilities
				solver.addConstraintex(1, new double[] { 1.0 }, new int[] { state + 1 }, LpSolve.EQ, context.getExtremalProbabilities()[state]);
				//reward 1 and above
				for (int reward = 1; reward < rewardBound; reward++) {
					solver.setColName(state+1, "X_"+state+"_"+reward);
					if (context.getZeroValueStates().contains(state) || !context.getInvariantStates().contains(state)) {
						//states not fulfilling the invariants -> value 0
						int stateOffsetInsideIteration = state + reward * states + 1;
						solver.addConstraintex(1, new double[] { 1.0 }, new int[] { stateOffsetInsideIteration }, LpSolve.EQ, 0);
					} else {
						//all states from where reaching the goal is possible
						int stateReward = (int) context.getModel().getStateReward(state);
						if (reward <= stateReward) {
							//states having a state-reward that is too high -> value depends on the extremal probabilities
							for (int choice = 0, numberOfChoices = context.getModel().getNumChoices(state); choice < numberOfChoices; choice++)
								addLpConstraint(solver, context, state, choice, reward, reward);
						} else {
							//states having a state-reward that is allowed
							for (int choice = 0, numberOfChoices = context.getModel().getNumChoices(state); choice < numberOfChoices; choice++) {
								int transitionReward = (int) context.getModel().getTransitionReward(state, choice);
								if (reward <= stateReward + transitionReward)
									//state-reward + transition-reward is too high -> value depends on the extremal probabilities
									addLpConstraint(solver, context, state, choice, reward, reward);
								else
									//state-reward + transition-reward is allowed
									addLpConstraint(solver, context, state, choice, reward, stateReward + transitionReward);
							}
						}
					}
				}
			}
			solver.setAddRowmode(false);
			//I want to know the results for all states
			int[] indizes = new int[rewardBound * states];
			double[] coefficients = new double[rewardBound * states];
			for (int i = 0; i < indizes.length; i++) {
				indizes[i] = i + 1;
				coefficients[i] = 1;
			}
			solver.setObjFnex(indizes.length, coefficients, indizes);
			//has no influence on DTMCs
			if (context.pickMaximum())
				solver.setMinim();
			else
				solver.setMaxim();
			if (quantileUtilities.getDebugLevel() >= 10)
				solver.writeLp("LPlowerRewardBounds.lp");
			solver.solve();
			return solver.getPtrVariables();
		} catch (LpSolveException e) {
			throw new PrismException(e.getMessage());
		} finally {
			solver.deleteLp();
		}
	}

	private static double[] lpSolverExclusively(Context4ExpressionQuantileProbUpperRewardBound context, int states, int rewardBound,
			QuantileUtilities quantileUtilities) throws PrismException
	{
		//XXX: REFACTORING !!!!
		LpSolve solver = null;
		try {
			//BEWARE: the indizes within LpSolve begin with value 1 instead of 0
			//we have rewardBound * states different variables
			solver = LpSolve.makeLp(0, rewardBound * states);
			solver.setVerbose(LpSolve.CRITICAL);
			solver.setAddRowmode(true);
			determineVariableBounds(solver, context, rewardBound * states);
			for (int state = 0; state < states; state++) {
				for (int reward = 0; reward < rewardBound; reward++) {
					int stateOffsetInsideIteration = state + reward * states + 1;
					if (context.getGoalStates().contains(state)) {
						//goal-states -> value 1
						solver.addConstraintex(1, new double[] { 1.0 }, new int[] { stateOffsetInsideIteration }, LpSolve.EQ, 1);
					} else if (context.getZeroValueStates().contains(state) || !context.getInvariantStates().contains(state)) {
						//states that never reach the goal -> value 0
						solver.addConstraintex(1, new double[] { 1.0 }, new int[] { stateOffsetInsideIteration }, LpSolve.EQ, 0);
					} else {
						//all states from where reaching the goal is possible
						int stateReward = (int) context.getModel().getStateReward(state);
						if (reward < stateReward) {
							//states having a state-reward that is too high -> value 0
							solver.addConstraintex(1, new double[] { 1.0 }, new int[] { stateOffsetInsideIteration }, LpSolve.EQ, 0);
						} else {
							//states having a state-reward that is allowed
							for (int choice = 0, numberOfChoices = context.getModel().getNumChoices(state); choice < numberOfChoices; choice++) {
								int transitionReward = (int) context.getModel().getTransitionReward(state, choice);
								if (reward < stateReward + transitionReward) {
									//state-reward + transition-reward is too high -> value 0 for this transition
									int operator = determineLpOperator(context.getModel().getModelType(), context.pickMaximum());
									solver.addConstraintex(1, new double[] { 1.0 }, new int[] { stateOffsetInsideIteration }, operator, 0);
								} else
									//state-reward + transition-reward is allowed
									addLpConstraint(solver, context, state, choice, reward, stateReward + transitionReward);
							}
						}
					}
				}
			}
			solver.setAddRowmode(false);
			//I want to know the results for all states
			int[] indizes = new int[rewardBound * states];
			double[] coefficients = new double[rewardBound * states];
			for (int i = 0; i < indizes.length; i++) {
				indizes[i] = i + 1;
				coefficients[i] = 1;
			}
			solver.setObjFnex(indizes.length, coefficients, indizes);
			//has no influence on DTMCs
			if (context.pickMaximum())
				solver.setMinim();
			else
				solver.setMaxim();
			if (quantileUtilities.getDebugLevel() >= 10)
				solver.writeLp("LPupperRewardBounds.lp");
			solver.solve();
			return solver.getPtrVariables();
		} catch (LpSolveException e) {
			throw new PrismException(e.getMessage());
		} finally {
			solver.deleteLp();
		}
	}

	private static double calculateSuccessorProbabilities(List<Integer> successors, List<Double> probabilities, RewardWrapper model, CalculatedValues values, int state, int choice, Map<Integer, Integer> stateToLpIndex)
	{
		successors.add(stateToLpIndex.get(state));
		probabilities.add(-1.0);
		double currentValue = 0;
		for (Map.Entry<Integer, Double> entry : model.getDistributionIterable(state, choice)){
			final int successor = entry.getKey();
			final double probability = entry.getValue();
			final double successorValue = values.getCurrentValue(successor);
			if (successorValue != CalculatedValues.UNDEFINED){
				if (successorValue == Double.POSITIVE_INFINITY){
					successors.clear();
					successors.add(stateToLpIndex.get(state));
					probabilities.clear();
					probabilities.add(1.0);
					return Double.POSITIVE_INFINITY;
				}
				currentValue -= probability * successorValue;
			} else {
				if (successor == state){
					probabilities.set(0, probabilities.get(0) + probability);
				} else {
					successors.add(stateToLpIndex.get(successor));
					probabilities.add(probability);
				}
			}
		}
		return currentValue;
	}

	private static void calculateSuccessorProbabilities(List<Integer> successors, List<Double> probabilities, RewardWrapper model, int state, int choice,
			int rewardStep, int reward)
	{
		int states = model.getNumStates();
		//the first element considered is the actual state itself
		successors.add(rewardStep * states + state + 1);
		probabilities.add(-1.0);
		for (Map.Entry<Integer, Double> entry : model.getDistributionIterable(state, choice)) {
			int index = entry.getKey();
			double probability = entry.getValue();
			//if the state has a self loop, and its reward is zero ...
			if ((index == state) && (reward == 0)) {
				//... then add its probability to the initialised -1 for the actual state
				probabilities.set(0, probabilities.get(0) + probability);
				//if the successor is different from the actual state ...
			} else {
				//... determine its index with respect to the actual reward round
				int successorIndex = rewardStep * states + index + 1;
				//subtract the actual state reward to get the final index
				successorIndex -= reward * states;
				successors.add(successorIndex);
				probabilities.add(probability);
			}
		}
	}
}