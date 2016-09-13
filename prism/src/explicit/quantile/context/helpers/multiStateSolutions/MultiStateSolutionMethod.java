package explicit.quantile.context.helpers.multiStateSolutions;

import java.util.Map;
import java.util.Set;

import explicit.quantile.QuantileUtilities;
import explicit.quantile.context.Context;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.Pair;
import prism.PrismException;

public enum MultiStateSolutionMethod
{
	VALUE_ITERATION {
		@Override
		public Pair<double[], Map<Integer, Integer>> solveMultipleStates(final Context context, final CalculatedValues values, final Set<Integer> states, final Set<Integer> statesWithPositiveRewardTransition, final int rewardStep, final QuantileUtilities quantileUtilities) throws PrismException
		{
			return ValueIterationComputer.valueIteration(context, values, states, rewardStep, statesWithPositiveRewardTransition, quantileUtilities.getMaxIters(), quantileUtilities.getTermCritParam(), quantileUtilities.getTermCritAbsolute(), quantileUtilities.getLog(), quantileUtilities.getDebugLevel());
		}
	},
	LP_SOLVER {
		@Override
		public Pair<double[], Map<Integer, Integer>> solveMultipleStates(final Context context, final CalculatedValues values, final Set<Integer> states, final Set<Integer> statesWithPositiveRewardTransition, final int rewardStep, final QuantileUtilities quantileUtilities) throws PrismException
		{
			return LinearProgramComputer.lpSolver(context, values, states, rewardStep, quantileUtilities.getDebugLevel());
		}
	};

	public abstract Pair<double[], Map<Integer, Integer>> solveMultipleStates(final Context context, final CalculatedValues values, final Set<Integer> states, final Set<Integer> statesWithPositiveRewardTransition, final int rewardStep, final QuantileUtilities quantileUtilities) throws PrismException;
}