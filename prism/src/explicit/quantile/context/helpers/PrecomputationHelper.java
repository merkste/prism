package explicit.quantile.context.helpers;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import common.BitSetTools;
import common.functions.CloseDoubleRelation;
import common.functions.primitive.MappingInt;
import common.functions.primitive.PairPredicateInt;
import common.iterable.IterableStateSet;
import common.iterable.Support;
import prism.PrismException;
import prism.PrismLog;
import explicit.CTMC;
import explicit.CTMCModelChecker;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.DiracDistribution;
import explicit.ECConsumer;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ProbModelChecker;
import explicit.SCCComputer;
import explicit.SCCConsumerBSCCs;
import explicit.modelviews.EquivalenceRelationInteger;
import explicit.modelviews.EquivalenceRelationInteger.KeepSingletons;
import explicit.modelviews.MDPAdditionalChoices;
import explicit.modelviews.MDPDroppedChoices;
import explicit.quantile.context.Context4ExpressionQuantileProbLowerRewardBound;
import explicit.quantile.dataStructure.RewardWrapperDTMC;
import explicit.quantile.dataStructure.RewardWrapperMDP;
import parser.ast.RelOp;

public class PrecomputationHelper
{
	public static Map<Double, BitSet> finiteQuantileStatesMap(RelOp relationOperator, double[] values, List<Double> thresholds)
	{
		Map<Double, BitSet> finiteQuantileStatesMap = new HashMap<Double, BitSet>();
		for (double threshold : thresholds)
			finiteQuantileStatesMap.put(threshold, finiteQuantileStates(relationOperator, threshold, values));
		return finiteQuantileStatesMap;
	}

	private static BitSet finiteQuantileStates(RelOp relationOperator, double threshold, double[] values)
	{
		switch (relationOperator) {
		case LT:
			return new Support(values, CloseDoubleRelation.C_LT(threshold)).asBitSet();
		case LEQ:
			return new Support(values, CloseDoubleRelation.C_LEQ(threshold)).asBitSet();
		case GT:
			return new Support(values, CloseDoubleRelation.C_GT(threshold)).asBitSet();
		case GEQ:
			return new Support(values, CloseDoubleRelation.C_GEQ(threshold)).asBitSet();
		default:
			throw new RuntimeException(relationOperator + " is not supported");
		}
	}

	public static double[] reachabilityProbabilitiesLowerRewardBound(final CTMC model, final BitSet invariantStates, final BitSet goalStates, final CTMCModelChecker modelChecker)
			throws PrismException
	{
		final BitSet targetStates = new BitSet();
		final DTMC embeddedDTMC = model.getImplicitEmbeddedDTMC();
		final SCCConsumerBSCCs sccConsumer = new SCCConsumerBSCCs(modelChecker, embeddedDTMC)
		{
			@Override
			public void notifyNextBSCC(final BitSet bscc){
				if (goalStates.intersects(bscc) && EndComponentUtilities.containsOnlyExpectedStates(bscc, invariantStates, goalStates)){
					targetStates.or(bscc);
				}
			}
		};
		final SCCComputer sccComputer = SCCComputer.createSCCComputer(modelChecker, embeddedDTMC, sccConsumer);
		sccComputer.computeSCCs();
		return new DTMCModelChecker(modelChecker).computeReachProbs(embeddedDTMC, invariantStates, targetStates, null, null).soln;
	}

	public static double[] universalReachabilityProbabilitiesLowerRewardBound(final Context4ExpressionQuantileProbLowerRewardBound context,
			ProbModelChecker probModelChecker) throws PrismException
	{
		// probReachGoal = Pmax( a U b )
		double[] probReachGoal = context.getExtremalProbabilities().clone();
		final BitSet invariantStates = BitSetTools.asBitSet(context.getInvariantStates());
		// Compute MECs that allow staying in the remaining states
		final BitSet C = new BitSet();
		switch (context.getModel().getModelType()) {
		case DTMC:
			RewardWrapperDTMC dtmcWrapper = (RewardWrapperDTMC) context.getModel();
			SCCConsumerBSCCs sccConsumer = new SCCConsumerBSCCs(dtmcWrapper, dtmcWrapper.getModel())
			{
				@Override
				public void notifyNextBSCC(BitSet bscc)
				{
					if (EndComponentUtilities.isProperPositiveRewardEndComponent(dtmcWrapper, bscc, context.getInvariantStates(), context.getGoalStates()))
						C.or(bscc);
				}
			};
			dtmcWrapper.computeStronglyConnectedComponents(sccConsumer);
			break;
		case MDP:
			RewardWrapperMDP mdpWrapper = (RewardWrapperMDP) context.getModel();
			ECConsumer ecConsumer = new ECConsumer(mdpWrapper, mdpWrapper.getModel())
			{
				@Override
				public void notifyNextMEC(BitSet mec)
				{
					if (EndComponentUtilities.isProperPositiveRewardEndComponent(mdpWrapper, mec, context.getInvariantStates(), context.getGoalStates()))
						C.or(mec);
				}
			};
			mdpWrapper.computeEndComponents(ecConsumer, invariantStates);
			break;
		default:
			throw new PrismException(context.getModel().getModelType() + " is not yet supported");
		}

		// C = there exists a scheduler that guarantees []a & []<>posR
		for (int state : new IterableStateSet(C, context.getModel().getNumStates(), true)) {
			// not in C -> set probReachGoal = 0.0
			probReachGoal[state] = 0.0;
		}

		// calculate Pmax(a U C), where C is weighed by probReachGoal
		// this is achieved by passing probReachGoal as the init vector and setting known = C
		return computeReachabilityProbabilities(context.getModel().getModel(), probModelChecker, BitSetTools.asBitSet(context.getInvariantStates()), //remain
				C, //target
				false, probReachGoal, //init
				C); //known
	}

	public static double[] existentialReachabilityProbabilitiesLowerRewardBound(Context4ExpressionQuantileProbLowerRewardBound context,
			ProbModelChecker probModelChecker) throws PrismException
	{
		BitSet invariantStates = BitSetTools.asBitSet(context.getInvariantStates());
		BitSet notInvariantStates = BitSetTools.complement(context.getModel().getNumStates(), invariantStates);
		final BitSet goalStates = BitSetTools.asBitSet(context.getGoalStates());
		final BitSet D = notInvariantStates;

		switch (context.getModel().getModelType()) {
		case DTMC:
			RewardWrapperDTMC dtmcWrapper = (RewardWrapperDTMC) context.getModel();
			// -> only one choice per state (in the actual setting there is no transition reward for DTMCs)
			// -> positive reward states correspond to states with positive reward for the states
			final BitSet posRewardStates = dtmcWrapper.getPositiveStateRewardStates();
			SCCConsumerBSCCs sccConsumer = new SCCConsumerBSCCs(dtmcWrapper, dtmcWrapper.getModel())
			{
				@Override
				public void notifyNextBSCC(BitSet bscc)
				{
					if (!(bscc.intersects(goalStates) && bscc.intersects(posRewardStates)))
						D.or(bscc);
				}
			};
			dtmcWrapper.computeStronglyConnectedComponents(sccConsumer);
			break;
		case MDP:
			RewardWrapperMDP mdpWrapper = (RewardWrapperMDP) context.getModel();
			// Calculate MECs with [](!goal)
			ECConsumer ecConsumer = new ECConsumer(mdpWrapper, mdpWrapper.getModel())
			{
				@Override
				public void notifyNextMEC(BitSet mec)
				{
					D.or(mec);
				}
			};
			BitSet restrict = BitSetTools.complement(mdpWrapper.getNumStates(), goalStates);
			mdpWrapper.computeEndComponents(ecConsumer, restrict);

			// Calculate states with Pmax([](!posR)) = 1
			D.or(mdpWrapper.restrictToZeroRewards(false));
			break;
		default:
			throw new PrismException(context.getModel().getModelType() + " is not yet supported");
		}

		double[] probabilities2reachD = computeReachabilityProbabilities(context.getModel().getModel(), probModelChecker,
				BitSetTools.asBitSet(context.getInvariantStates()), //remain
				D, //target
				false);
		// Subtract from 1 for negation
		for (int state = 0; state < probabilities2reachD.length; state++)
			probabilities2reachD[state] = 1.0 - probabilities2reachD[state];
		return probabilities2reachD;
	}

	public static void logPrecomputedValues(String explanation, BitSet states, double[] values, PrismLog log)
	{
		log.println("\nPrecomputation: " + explanation + " in the states of interest:");
		for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state+1)){
			log.println("state " + state + ": " + values[state]);
		}
	}

	public static double[] computeReachabilityProbabilities(Model model, ProbModelChecker probModelChecker, BitSet remain, BitSet target, boolean minimum)
			throws PrismException
	{
		return computeReachabilityProbabilities(model, probModelChecker, remain, target, minimum, null, null);
	}

	private static double[] computeReachabilityProbabilities(Model model, ProbModelChecker probModelChecker, BitSet remain, BitSet target, boolean minimum,
			double[] init, BitSet known) throws PrismException
	{
		switch (model.getModelType()) {
		case CTMC:
			return ((CTMCModelChecker) probModelChecker).computeReachProbs((CTMC) model, remain, target, init, known).soln;
		case DTMC:
			return ((DTMCModelChecker) probModelChecker).computeReachProbs((DTMC) model, remain, target, init, known).soln;
		case MDP:
			return ((MDPModelChecker) probModelChecker).computeReachProbs((MDP) model, remain, target, minimum, init, known).soln;
		default:
			throw new PrismException("The model type " + model.getModelType() + " is not supported!");
		}
	}

	public static boolean isSelfLoopInMec(final int state, final Iterator<Integer> successors, final KeepSingletons mecs)
	{
		if (mecs.getOriginalEquivalenceClass(state) == null){
			return false;
		}
		while (successors.hasNext()){
			if (! mecs.test(state, (int) successors.next())){
				return false;
			}
		}
		return true;
	}

	public static MDPAdditionalChoices redirectStatesToGoal(final MDP model, final BitSet states, final int goal)
	{
		final MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = new MappingInt<List<Iterator<Entry<Integer, Double>>>>()
		{
			@Override
			public List<Iterator<Entry<Integer, Double>>> apply(final int state)
			{
				if (state==goal || states.get(state)){
					return Collections.<Iterator<Entry<Integer, Double>>> singletonList(new DiracDistribution(goal).iterator());
				}
				return Collections.emptyList();
			}
		};
		final MappingInt<List<Object>> actions = new MappingInt<List<Object>>()
		{
			@Override
			public List<Object> apply(final int state)
			{
				if (state==goal || states.get(state)){
					return Collections.<Object> singletonList("tau");
				}
				return Collections.emptyList();
			}
		};
		return new MDPAdditionalChoices(model, choices, actions);
	}

	public static double[] expectedRewards4originalModel(final int numStates, final double[] expectedRewards4collapsedModel, final List<BitSet> zeroValueMecs)
	{
		assert (expectedRewards4collapsedModel.length == numStates+1);
		
		final double[] expectedRewards = new double[numStates];
		System.arraycopy(expectedRewards4collapsedModel, 0, expectedRewards, 0, numStates);
		for (BitSet mec : zeroValueMecs){
			final int representative = mec.nextSetBit(0);
			final double valueOfRepresentative = expectedRewards4collapsedModel[representative];
			for (int state = representative; state >= 0; state = mec.nextSetBit(state+1)){
				expectedRewards[state] = valueOfRepresentative;
			}
		}
		return expectedRewards;
	}

	public static MDPDroppedChoices stayInComponents(final MDP model, final List<BitSet> components)
	{
		return new MDPDroppedChoices(model, new PairPredicateInt()
		{
			final EquivalenceRelationInteger componentLUT = new EquivalenceRelationInteger.KeepSingletons(components);
			@Override
			public boolean test(final int state, final int choice)
			{
				final BitSet component = componentLUT.getEquivalenceClassOrNull(state);
				if (component == null){
					//state does not belong to a given component
					return false;
				}
				return ! model.allSuccessorsInSet(state, choice, component);
			}
		});
	}

	public static boolean hasDeadlocks(final Model model)
	{
		try {
			model.checkForDeadlocks();
		} catch (PrismException e){
			return true;
		}
		return false;
	}
}