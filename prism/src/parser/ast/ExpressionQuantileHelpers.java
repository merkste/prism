package parser.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import parser.Values;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import explicit.Model;
import explicit.rewards.ConstructRewards;
import explicit.rewards.Rewards;
import explicit.rewards.StateRewardsConstant;

public abstract class ExpressionQuantileHelpers{
	public static Rewards buildRewardStructure(Model model, ModulesFile modulesFile, Values constantValues, PrismLog log, Object rewardStructIndex) throws PrismException{
		if (rewardStructIndex == null)
			//no reward was specified --> just return a new reward structure giving reward 1 to each state
			return new StateRewardsConstant(1);
		if (modulesFile == null){
			throw new PrismException("No model file to obtain reward structures");
		}
		if (modulesFile.getNumRewardStructs() == 0){
			throw new PrismException("Model has no rewards specified");
		}
		RewardStruct rewStruct = null;
		if (rewardStructIndex instanceof Expression){
			int i = ((Expression) rewardStructIndex).evaluateInt(constantValues);
			rewardStructIndex = new Integer(i); // for better error reporting below
			rewStruct = modulesFile.getRewardStruct(i - 1);
		} else if (rewardStructIndex instanceof String){
			rewStruct = modulesFile.getRewardStructByName((String) rewardStructIndex);
		}
		if (rewStruct == null){
			throw new PrismException("Invalid reward structure index \"" + rewardStructIndex + "\"");
		}
		ConstructRewards constructRewards = new ConstructRewards(log);
		return constructRewards.buildRewardStructure(model, rewStruct, constantValues);
	}
	private static void sortThresholdsProperly(List<Double> thresholds, boolean sortAscending){
		if (sortAscending)
			Collections.sort(thresholds);
		else
			Collections.reverse(thresholds);
	}
	protected static List<Double> initialiseThresholds(Expression thresholdExpression, Values constantValues, boolean sortAscending) throws PrismException{
		if (thresholdExpression instanceof ExpressionMultipleThresholds){
			final List<Double> thresholds = ((ExpressionMultipleThresholds) thresholdExpression).expandAndEvaluate(constantValues);
			sortThresholdsProperly(thresholds, sortAscending);
			return thresholds;
		}
		double threshold = thresholdExpression.evaluateDouble(constantValues);
		if (threshold < 0)
			throw new PrismLangException("Invalid threshold bound: " + threshold);
		final List<Double> thresholds = new ArrayList<>(1);
		thresholds.add(threshold);
		return thresholds;
	}
}