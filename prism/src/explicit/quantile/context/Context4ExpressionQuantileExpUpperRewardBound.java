package explicit.quantile.context;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import common.BitSetTools;
import common.functions.primitive.PairPredicateInt;
import parser.ast.ExpressionQuantileExpNormalForm;
import prism.PrismException;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSparse;
import explicit.modelviews.EquivalenceRelationInteger;
import explicit.modelviews.EquivalenceRelationInteger.KeepSingletons;
import explicit.modelviews.MDPAdditionalStates;
import explicit.modelviews.MDPEquivSelfLoopsForNonRepresentatives;
import explicit.modelviews.MDPDroppedChoices;
import explicit.quantile.QuantileUtilities;
import explicit.quantile.context.helpers.QuantitativeCalculationHelper;
import explicit.quantile.context.helpers.BsccStates;
import explicit.quantile.context.helpers.EcStates;
import explicit.quantile.context.helpers.MecStates;
import explicit.quantile.context.helpers.PrecomputationHelper;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.dataStructure.RewardWrapper;
import explicit.quantile.dataStructure.RewardWrapperMDP;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;

public class Context4ExpressionQuantileExpUpperRewardBound extends Context4ExpressionQuantileExp
{

	public Context4ExpressionQuantileExpUpperRewardBound(RewardWrapper theCostModel, RewardWrapper theValueModel,
			BitSet theZeroStateRewardStatesWithZeroRewardTransition, BitSet theZeroStateRewardStatesWithMixedTransitionRewards, BitSet theZeroValueStates,
			EcStates theEcStates, BitSet theStatesOfInterest, QuantileUtilities theQuantileUtilities, ExpressionQuantileExpNormalForm theExpressionQuantile)
	{
		super(theCostModel, theValueModel,
				theZeroStateRewardStatesWithZeroRewardTransition, theZeroStateRewardStatesWithMixedTransitionRewards, theZeroValueStates,
				theStatesOfInterest, theQuantileUtilities, theExpressionQuantile);
		isExistential = theExpressionQuantile.isExistential();
		ecStates = theEcStates;
		//when interested in maximising the value, one can compute all states that try to reach infiniteValue-mecs with a positive probability
		final BitSet infinityStates = (BitSet) ecStates.getInfinityValueStates().clone();
		switch (model.getModelType()){
		case DTMC:
			final BitSet notReachingInfinityStates = ((DTMCModelChecker) quantileUtilities.getProbModelChecker()).prob0((DTMC) model.getModel(), model.getZeroStateRewardStates(), infinityStates);
			infinityStates.or(BitSetTools.complement(model.getNumStates(), notReachingInfinityStates));
			break;
		case MDP:
			if (isExistential){
				final MDPSparse zeroCostSubMDP = new MDPSparse(((RewardWrapperMDP) model).dropPositiveRewards());
				final BitSet notReachingInfinityStatesinSubModel = ((MDPModelChecker) quantileUtilities.getProbModelChecker()).prob0(zeroCostSubMDP, null, infinityStates, false, null);
				final BitSet reachingInfinityStatesInSubModel = BitSetTools.complement(zeroCostSubMDP.getNumStates(), notReachingInfinityStatesinSubModel);
				infinityStates.or(reachingInfinityStatesInSubModel);
			}
			break;
		default:
			throw new UnsupportedOperationException();
		}
		infinityValueStates = quantileUtilities.getSetFactory().getSet(infinityStates);
	}

	private final boolean isExistential;

	@Override
	public boolean pickMaximum()
	{
		return isExistential;
	}

	protected final Set<Integer> infinityValueStates;

	public Set<Integer> getInfinityValueStates()
	{
		return infinityValueStates;
	}

	private EcStates ecStates;

	@Override
	public Map<Double, BitSet> determineFiniteQuantileStates() throws PrismException
	{
		final BitSet finiteQuantileStates;
		final double[] expectedRewards;
		switch (model.getModelType()) {
		case DTMC:
			final DTMC dtmc = (DTMC) model.getModel();
			final DTMCModelChecker dtmcModelChecker = (DTMCModelChecker) quantileUtilities.getProbModelChecker();
			final BsccStates bsccStates = (BsccStates) ecStates;
			
			final BitSet reachingZeroValueStates = dtmcModelChecker.prob0(dtmc, null, bsccStates.getValueDivergentStates());
			//states with Pr(<> valueDivergentStates) > 0 can accumulate arbitrary value in the respective BSCCs they will end in
			//each path leading to a BSCC can only accumulate finite cost
			//so the quantile-value is finite as the desired value-threshold will be exceeded after paying some finite cost
			finiteQuantileStates = BitSetTools.complement(dtmc.getNumStates(), reachingZeroValueStates);
			
			//states with Pr(<> valueDivergentStates) = 0 will end up in BSCCs where there is no possibility of accumulating any value at all
			//they can only accumulate value on their way to the BSCCs
			//so the quantile-value is finite if the expected value accumulated on the way will exceed the desired threshold
			expectedRewards = dtmcModelChecker.computeReachRewards(dtmc, (MCRewards) valueModel.getRewards(), bsccStates.getBsccStates()).soln;
			break;
		case MDP:
			final MDP mdp = (MDP) model.getModel();
			final MDPModelChecker mdpModelChecker = (MDPModelChecker) quantileUtilities.getProbModelChecker();
			final MecStates mecStates = (MecStates) ecStates;
			final List<BitSet> zeroValueMecs = mecStates.getZeroValueMecStates();
			if (quantileUtilities.getDebugLevel() > 9){
				mdp.exportToDotFile("0-originalModel-markZeroValueMECs.dot", BitSetTools.union(zeroValueMecs));
			}
			if (zeroValueMecs.isEmpty()){
				quantileUtilities.getLog().println("\nfyi: there are no zero-value MECs");
				expectedRewards = new double[mdp.getNumStates()];
				Arrays.fill(expectedRewards, Double.POSITIVE_INFINITY);
				//no zero-value MECs  ->  each state has a finite quantile-value
				return PrecomputationHelper.finiteQuantileStatesMap(getRelationOperator(), expectedRewards, quantileUtilities.getThresholds());
			}
			
			if (isExistential) {
				//states with Pr(<> valueDivergentStates) = 1 can accumulate arbitrary value in the respective MEC
				//so the quantile-value is finite as the desired value-threshold will be exceeded after some time
				finiteQuantileStates = mdpModelChecker.prob1(mdp, null, mecStates.getValueDivergentStates(), false, null);
				
				//states with Pr(<> valueDivergentStates) < 1 will need to be considered concerning their expected value in a modified model
				//they can accumulate value on their way to an EC
				//as soon as an end-component is reached, there is the possibility of staying there forever or of leaving the EC and return to it later
				//in either case one has to check the expected value
				
				final KeepSingletons identify = new EquivalenceRelationInteger.KeepSingletons(zeroValueMecs);
				//XXX: ??? evtl in MDPSparse cachen ???
				final MDPDroppedChoices droppedMecSelfLoops = new MDPDroppedChoices(mdp, new PairPredicateInt()
				{
					@Override
					public boolean test(final int state, final int choice)
					{
						if (model.getTransitionReward(state, choice) > 0){
							return false;
						}
						return PrecomputationHelper.isSelfLoopInMec(state, mdp.getSuccessorsIterator(state, choice), identify);
					}
				});
				if (quantileUtilities.getDebugLevel() > 9){
					droppedMecSelfLoops.exportToDotFile("1-Ex-dropZeroValueMECselfLoops-markZeroValueMECs.dot", BitSetTools.union(zeroValueMecs));
				}
				//XXX:
				//XXX:
				//XXX:
				quantileUtilities.getLog().print("\nbuilding model with collapsed zero-utility MECs (using MDPEquivSelfLoopsForNonRepresentatives) ... ");
				long timer = System.currentTimeMillis();
				//XXX:
				final MDP mdpEquiv = new MDPEquivSelfLoopsForNonRepresentatives(droppedMecSelfLoops, identify);
				//XXX:
				//XXX:
				//XXX:
				quantileUtilities.getLog().println("done in " + (System.currentTimeMillis() - timer) / 1000.0 + " seconds");
				//XXX:
				if (quantileUtilities.getDebugLevel() > 9){
					mdpEquiv.exportToDotFile("2-Ex-collapseZeroValueMECs-markRepresentatives.dot", BitSetTools.complement(mdp.getNumStates(), identify.getNonRepresentatives()));
				}
				
				//add fresh goal-state as trap
				final MDP modelWithGoalState = new MDPAdditionalStates(mdpEquiv, 1);
				final int goalState = mdpEquiv.getNumStates();
				if (quantileUtilities.getDebugLevel() > 9){
					modelWithGoalState.exportToDotFile("3-Ex-addFreshGoalState-markFreshState.dot", BitSetTools.asBitSet(goalState));
				}
				
				//MDPSparse is used to achieve better performances
				//XXX:
				//XXX:
				//XXX:
				quantileUtilities.getLog().print("building MDPSparse out of MDPEquivSelfLoopsForNonRepresentatives ... ");
				timer = System.currentTimeMillis();
				//XXX:
				final MDP model4expectedRewardsComputation = new MDPSparse(PrecomputationHelper.redirectStatesToGoal(modelWithGoalState, BitSetTools.complement(mdp.getNumStates(), identify.getNonRepresentatives()), goalState));
				if (quantileUtilities.getDebugLevel() > 9){
					model4expectedRewardsComputation.exportToDotFile("4-Ex-finalModel-introduceTauTransition-markRepresentatives.dot", BitSetTools.complement(mdp.getNumStates(), identify.getNonRepresentatives()));
				}
				//XXX:
				//XXX:
				//XXX:
				quantileUtilities.getLog().println("done in " + (System.currentTimeMillis() - timer) / 1000.0 + " seconds");
				//XXX:
				assert (! PrecomputationHelper.hasDeadlocks(model4expectedRewardsComputation));
				
				if (quantileUtilities.getDebugLevel() > 0) {
					quantileUtilities.getLog().println("\nmodel with collapsed zero-value MECs and fresh trap-state:");
					quantileUtilities.getLog().println(model4expectedRewardsComputation.infoString() + "\n");
				}
				
				final double[] expectedRewards4collapsedModel = mdpModelChecker.computeReachRewards(model4expectedRewardsComputation, (MDPRewards) valueModel.getRewards(), BitSetTools.asBitSet(goalState), false).soln;
				expectedRewards = PrecomputationHelper.expectedRewards4originalModel(mdp.getNumStates(), expectedRewards4collapsedModel, zeroValueMecs);
			} else {
				//universal quantile
				
				// assumptions (A1) and (A2) must be ensured by adequate preprocessing
				//(A1) t \in ZU  and  P(t, \alpha, t') > 0   ==>  t' \in ZU  and  urew(t, \alpha) = 0
				//(A2) Pr_M,t^\max (<> ZU) = 1  for all states t   (or: t \models (\exists <> ZU) for all states t)
				
				//since universal quantiles ask for worst-case behaviour, schedulers that try to accumulate as little utility as possible are sufficient
				//so, each zero-utility EC will not be left
				//this ensures (A1)
				
				//XXX:
				//XXX:
				//XXX:
				quantileUtilities.getLog().print("\nbuilding model with trapping zero-utility MECs (using MDPDroppedChoices) ... ");
				long timer = System.currentTimeMillis();
				//XXX:
				final MDP stayInZeroValueMecsVirtual = PrecomputationHelper.stayInComponents(mdp, zeroValueMecs);
				//XXX:
				//XXX:
				//XXX:
				quantileUtilities.getLog().println("done in " + (System.currentTimeMillis() - timer) / 1000.0 + " seconds");
				//XXX:
				//XXX:
				//XXX:
				//XXX:
				quantileUtilities.getLog().print("building MDPSparse out of MDPDroppedChoices ... ");
				timer = System.currentTimeMillis();
				//XXX:
				//MDPSparse is used to achieve better performances
				final MDP stayInZeroValueMecs = new MDPSparse(stayInZeroValueMecsVirtual);
				if (quantileUtilities.getDebugLevel() > 9){
					stayInZeroValueMecs.exportToDotFile("1-Un-dropChoicesLeavingZeroValueMECs-markZeroValueMECs.dot", BitSetTools.union(zeroValueMecs));
				}
				//XXX:
				//XXX:
				//XXX:
				quantileUtilities.getLog().println("done in " + (System.currentTimeMillis() - timer) / 1000.0 + " seconds");
				//XXX:
				assert (! PrecomputationHelper.hasDeadlocks(stayInZeroValueMecs));
				
				if (quantileUtilities.getDebugLevel() > 0) {
					quantileUtilities.getLog().println("\nmodel with trapping zero-value MECs:");
					quantileUtilities.getLog().println(stayInZeroValueMecs.infoString() + "\n");
				}
				
				final BitSet zeroValueMecStates = BitSetTools.union(zeroValueMecs);
				final BitSet reachingZeroValueMecStates = mdpModelChecker.prob1(stayInZeroValueMecs, null, zeroValueMecStates, false, null);
				//states with Pr(<> ZU) < 1 can avoid zero-utility ECs
				finiteQuantileStates = BitSetTools.complement(mdp.getNumStates(), reachingZeroValueMecStates);
				
				//states with Pr(<> ZU) = 1 will enter a zero-utility EC, and can not accumulate arbitrary utility
				//compute the expected value accumulated on their way to zero-utility ECs
				expectedRewards = mdpModelChecker.computeReachRewards(stayInZeroValueMecs, (MDPRewards) valueModel.getRewards(), zeroValueMecStates, true).soln;
			}
			break;
		default:
			throw new RuntimeException("The model type " + model.getModelType() + " is not supported!");
		}
		//clear
		ecStates = null;
		//finiteQuantileStates can accumulate arbitrary value
		for (int state = finiteQuantileStates.nextSetBit(0); state >= 0; state = finiteQuantileStates.nextSetBit(state+1)){
			expectedRewards[state] = Double.POSITIVE_INFINITY;
		}
		//filter the states with finite quantile-values
		final Map<Double, BitSet> finiteQuantileStatesMap = PrecomputationHelper.finiteQuantileStatesMap(getRelationOperator(), expectedRewards, quantileUtilities.getThresholds());
		PrecomputationHelper.logPrecomputedValues("expected values", statesOfInterest, expectedRewards, quantileUtilities.getLog());
		return finiteQuantileStatesMap;
	}

	@Override
	public void calculateDerivableStates(CalculatedValues values, int rewardStep)
	{
		//states having value 0  ==>  0
		values.setCurrentValues(zeroValueStates, 0);
		//states having value infty ==> infty
		values.setCurrentValues(infinityValueStates, Double.POSITIVE_INFINITY);
		//states having a positive reward
		QuantitativeCalculationHelper.calculatePositiveRewardStates4UpperRewardBoundedQuantile(positiveRewardStates, model, valueModel, values, rewardStep, pickMaximum(), quantileUtilities.getWorkerPoolForPositiveRewardStates());
	}

	@Override
	public double calculatePositiveRewardTransitionForZeroRewardState(final int state, final int choice, final CalculatedValues values, final int rewardStep)
	{
		return QuantitativeCalculationHelper.valueForPositiveRewardTransition4UpperRewardBoundedQuantile(model, valueModel.getStateReward(state)+valueModel.getTransitionReward(state, choice), values, state, choice, 0, rewardStep);
	}

	@Override
	public int getNumberOfDerivableStates()
	{
		return infinityValueStates.size() + zeroValueStates.size() + positiveRewardStates.size();
	}
}