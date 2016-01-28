package explicit.quantile.dataStructure;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.IntPredicate;

import common.BitSetTools;
import common.functions.primitive.PairPredicateInt;
import common.iterable.FilteringIterable;
import common.iterable.Interval;
import prism.PrismComponent;
import prism.PrismException;
import explicit.BasicModelTransformation;
import explicit.ECComputer;
import explicit.ECComputerOnTheFly;
import explicit.ECConsumer;
import explicit.MDP;
import explicit.MDPSparse;
import explicit.Model;
import explicit.Product;
import explicit.modelviews.EquivalenceRelationInteger;
import explicit.modelviews.MDPAdditionalChoices;
import explicit.modelviews.MDPDroppedChoices;
import explicit.modelviews.MDPEquiv;
import explicit.modelviews.MDPRestricted;
import explicit.modelviews.Restriction;
import explicit.modelviews.EquivalenceRelationInteger.KeepSingletons;
import explicit.modelviews.MDPEquiv.StateChoicePair;
import explicit.rewards.MDPRewards;

public class RewardWrapperMDP extends RewardWrapper
{

	private MDP mdp;
	private MDPRewards rewards;

	private BasicModelTransformation<? extends MDP, ? extends MDP> modelTransformation;

	public RewardWrapperMDP(PrismComponent parent, MDP mdp, MDPRewards rewards)
	{
		this(parent, mdp, rewards, null);
	}
	public RewardWrapperMDP(PrismComponent parent, MDP mdp, MDPRewards rewards, BasicModelTransformation<? extends MDP, ? extends MDP> modelTransformation)
	{
		super(parent);
		this.mdp = mdp;
		this.rewards = rewards;
		//if modelTransformation == null, then there is no mapping needed
		this.modelTransformation = modelTransformation;
	}

	@Override
	public MDP getModel()
	{
		return mdp;
	}

	@Override
	public MDPRewards getRewards()
	{
		return rewards;
	}

	public BasicModelTransformation<? extends MDP, ? extends MDP> getModelTransformation()
	{
		return modelTransformation;
	}

	@Override
	public int getNumChoices(int state)
	{
		return mdp.getNumChoices(state);
	}

	@Override
	public int getTransitionReward(int state, int choice)
	{
		return (int) rewards.getTransitionReward(state, choice);
	}

	@Override
	public int getStateReward(int state)
	{
		return (int) rewards.getStateReward(state);
	}

	@Override
	public Iterable<Entry<Integer, Double>> getDistributionIterable(final int state, final int choice)
	{
		return new Iterable<Entry<Integer, Double>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> iterator()
			{
				return mdp.getTransitionsIterator(state, choice);
			}
		};
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		return mdp.getSuccessorsIterator(state, choice);
	}

	public void computeEndComponents(ECConsumer ecConsumer) throws PrismException
	{
		new ECComputerOnTheFly(this, getModel(), ecConsumer).computeMECStates();
	}

	public void computeEndComponents(ECConsumer ecConsumer, BitSet remain) throws PrismException
	{
		new ECComputerOnTheFly(this, getModel(), ecConsumer).computeMECStates(remain);
	}

	/**
	 * Check if the intersection of <code>successors</code> and the successors of <code>state</code> is non-empty for at least one choice of <code>state</code>.
	 * @param state
	 * @param successors
	 * @return
	 */
	private boolean oneChoiceHasSomeSuccessors(int state, BitSet successors)
	{
		for (int choice = 0, numChoices = mdp.getNumChoices(state); choice < numChoices; choice++) {
			if (mdp.someSuccessorsInSet(state, choice, successors))
				return true;
		}
		return false;
	}

	/**
	 * Check if the intersection of <code>successors</code> and the successors of <code>state</code> is non-empty for all choices of <code>state</code>.
	 * @param state
	 * @param successors
	 * @return
	 */
	private boolean allChoicesHaveSomeSuccessors(int state, BitSet successors)
	{
		for (int choice = 0, numChoices = mdp.getNumChoices(state); choice < numChoices; choice++) {
			if (!mdp.someSuccessorsInSet(state, choice, successors))
				return false;
		}
		return true;
	}

	/**
	 * Check if at least one choice of <code>state</code> leads to a superset of <code>successors</code>.
	 * @param state
	 * @param successors
	 * @return
	 */
	private boolean oneChoiceHasAllSuccessors(int state, BitSet successors)
	{
		for (int choice = 0, numChoices = mdp.getNumChoices(state); choice < numChoices; choice++) {
			if (mdp.allSuccessorsInSet(state, choice, successors))
				return true;
		}
		return false;
	}

	/**
	 * Check if every choice of <code>state</code> leads to a superset of <code>successors</code>.
	 * @param state
	 * @param successors
	 * @return
	 */
	private boolean allChoicesHaveAllSuccessors(int state, BitSet successors)
	{
		for (int choice = 0, numChoices = mdp.getNumChoices(state); choice < numChoices; choice++) {
			if (!mdp.allSuccessorsInSet(state, choice, successors))
				return false;
		}
		return true;
	}

	/**
	 * Computes the set of states s such that the intersection of <code>successors</code> and the successors of s are non-empty for at least one choices of s.
	 * @param successors
	 * @return
	 */
	public BitSet statesWhereOneChoiceHasSomeSuccessors(BitSet successors)
	{
		BitSet states = new BitSet();
		for (int state = 0, numStates = mdp.getNumStates(); state < numStates; state++) {
			if (oneChoiceHasSomeSuccessors(state, successors))
				states.set(state);
		}
		return states;
	}

	/**
	 * Computes the set of states s such that the intersection of <code>successors</code> and the successors of s are non-empty for all choices of s.
	 * @param successors
	 * @return
	 */
	public BitSet statesWhereAllChoicesHaveSomeSuccessors(BitSet successors)
	{
		BitSet states = new BitSet();
		for (int state = 0, numStates = mdp.getNumStates(); state < numStates; state++) {
			if (allChoicesHaveSomeSuccessors(state, successors))
				states.set(state);
		}
		return states;
	}

	/**
	 * Computes the set of states s such that the successors of at least one choice of s are contained in <code>successors</code>.
	 * @param successors
	 * @return
	 */
	public BitSet statesWhereOneChoiceHasAllSuccessors(BitSet successors)
	{
		BitSet states = new BitSet();
		for (int state = 0, numStates = mdp.getNumStates(); state < numStates; state++) {
			if (oneChoiceHasAllSuccessors(state, successors))
				states.set(state);
		}
		return states;
	}

	/**
	 * Computes the set of states s such that the successors for all choices of s are contained in <code>successors</code>.
	 * @param successors
	 * @return
	 */
	public BitSet statesWhereAllChoicesHaveAllSuccessors(BitSet successors)
	{
		BitSet states = new BitSet();
		for (int state = 0, numStates = mdp.getNumStates(); state < numStates; state++) {
			if (allChoicesHaveAllSuccessors(state, successors))
				states.set(state);
		}
		return states;
	}

	private boolean allSuccessorsInSetForAtLeastOneChoice(int state, Iterable<Integer> choices, BitSet successors)
	{
		for (int choice : choices){
			if (mdp.allSuccessorsInSet(state, choice, successors)){
				return true;
			}
		}
		return false;
	}

	private boolean allSuccessorsInSetForAllChoices(int state, Iterable<Integer> choices, BitSet successors)
	{
		for (int choice : choices){
			if (! mdp.allSuccessorsInSet(state, choice, successors)){
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean allSuccessorsInSet(final int state, final int choice, final BitSet successors)
	{
		return mdp.allSuccessorsInSet(state, choice, successors);
	}

	public MDPDroppedChoices dropPositiveRewards()
	{
		assert mdp.getNumDeadlockStates() == 0 : "model has to be deadlock free";
		
		return new MDPDroppedChoices(mdp, new PairPredicateInt()
		{
			@Override
			public boolean test(final int state, final int choice)
			{
				return (getStateReward(state) > 0)
						|| (getTransitionReward(state, choice) > 0);
			}
		});
	}

	public BitSet restrictToZeroRewards(final boolean worstCase)
	{
		final BitSet allowedStates = getZeroStateRewardStates();
		if (worstCase){
			allowedStates.and(getZeroRewardForAllChoicesStates());
		} else {
			allowedStates.and(getZeroRewardForAtLeastOneChoiceStates());
		}
		boolean changed = true;
		while (changed) {
			changed = false;
			for (int state = allowedStates.nextSetBit(0); state >= 0; state = allowedStates.nextSetBit(state+1)){
				if (worstCase){
					if (! allSuccessorsInSetForAllChoices(state, getZeroRewardChoices(state), allowedStates)){
						allowedStates.clear(state);
						changed = true;
					}
				} else {
					if (! allSuccessorsInSetForAtLeastOneChoice(state, getZeroRewardChoices(state), allowedStates)){
						allowedStates.clear(state);
						changed = true;
					}
				}
			}
		}
		return allowedStates;
	}

	private Triplet<List<BitSet>, BitSet, BitSet> getZeroRewardMECs(final BitSet invariantStates, final BitSet goalStates) throws PrismException
	{
		final List<BitSet> zeroRewardMecs = new ArrayList<>();
		final BitSet invariantStatesInZeroRewardMECs = new BitSet();
		final BitSet goalStatesInZeroRewardMECs = new BitSet();
		final MDP zeroRewardModel = new MDPSparse(dropPositiveRewards());
		final ECComputer ecComputer = new ECComputerOnTheFly(this, zeroRewardModel, new ECConsumer(this, zeroRewardModel){
			@Override
			public void notifyNextMEC(final BitSet mec)
			{
				zeroRewardMecs.add(mec);
				//the MECs are computed over the restriction of  a or b
				//so, each mec here, fulfills  a or b
				//the adapted invariant states get the a-label
				final int representative = mec.nextSetBit(0);
				invariantStatesInZeroRewardMECs.set(representative);
				if (goalStates.intersects(mec)){
					//if there is a goal-state in this MEC, the representative gets the goal-label as well
					goalStatesInZeroRewardMECs.set(representative);
				}
			}
		});
		ecComputer.computeMECStates(BitSetTools.union(invariantStates, goalStates));
		return new Triplet<>(zeroRewardMecs, invariantStatesInZeroRewardMECs, goalStatesInZeroRewardMECs);
	}

	private MDPRewards getRewards4collapsedZeroRewardMECs(final MDPRestricted reachableStates, final MDPEquiv collapsedZeroRewardMecs, final MDPDroppedChoices droppedMecSelfLoops, final KeepSingletons equivalenceRelation)
	{
		return new MDPRewards()
		{
			@Override
			public MDPRewards liftFromModel(Product<? extends Model> product)
			{
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public boolean hasTransitionRewards()
			{
				return rewards.hasTransitionRewards();
			}
			@Override
			public double getStateReward(final int state)
			{
				final int originalState = reachableStates.mapStateToOriginalModel(state);
				assert (equivalenceRelation.isRepresentative(originalState));
				return rewards.getStateReward(originalState);
			}
			@Override
			public double getTransitionReward(final int state, final int choice)
			{
				final int originalState = reachableStates.mapStateToOriginalModel(state);
				assert (equivalenceRelation.isRepresentative(originalState));
				if (droppedMecSelfLoops.getNumChoices(originalState) == 0){
					//here, the state represents a zero-reward MEC where all choices have been deleted
					//meaning that ALL the previously available choices belong to the MEC (and were therefore deleted)
					//or stated in other words, the state represents a bottom MEC
					return 0;
				}
				//we consider a choice which is not inside a zero-reward MEC
				int mappedState = originalState;
				int mappedChoice = droppedMecSelfLoops.mapChoiceToOriginalModel(originalState, choice);
				final StateChoicePair outsideZeroRewardMEC = collapsedZeroRewardMecs.mapToOriginalModel(mappedState, mappedChoice);
				if (outsideZeroRewardMEC != null){
					//the state represents a zero-reward MEC ==> mapping needed
					mappedState = outsideZeroRewardMEC.getState();
					mappedChoice = outsideZeroRewardMEC.getChoice();
				}
				//the correct choice is received from the model with the dropped self-loops
				return rewards.getTransitionReward(mappedState, mappedChoice);
			}
		};
	}

	private static BitSet markInReachableModel(final BitSet markedStatesInCollapsedModel, final BitSet markedStatesInOriginalModel, final MDPRestricted reachableStates)
	{
		final BitSet result = new BitSet();
		for (int markedState = markedStatesInCollapsedModel.nextSetBit(0); markedState >= 0; markedState = markedStatesInCollapsedModel.nextSetBit(markedState+1)){
			final Integer mappedState = reachableStates.mapStateToRestrictedModel(markedState);
			assert (mappedState != null);
			result.set(mappedState);
		}
		for (int markedState = markedStatesInOriginalModel.nextSetBit(0); markedState >= 0; markedState = markedStatesInOriginalModel.nextSetBit(markedState+1)){
			final Integer mappedState = reachableStates.mapStateToRestrictedModel(markedState);
			if (mappedState != null){
				result.set(mappedState);
			}
		}
		return result;
	}

	private MDPDroppedChoices dropSelfLoopsInZeroRewardMECs(final MDPEquiv collapsedZeroRewardMecs, final KeepSingletons equivalenceRelation)
	{
		return new MDPDroppedChoices(collapsedZeroRewardMecs, new PairPredicateInt()
		{
			@Override
			public boolean test(final int state, final int choice)
			{
				if (! equivalenceRelation.isRepresentative(state)){
					return false;
				}
				int mappedState = state;
				int mappedChoice = choice;
				final StateChoicePair stateChoiceMapping = collapsedZeroRewardMecs.mapToOriginalModel(state, choice);
				if (stateChoiceMapping != null){
					mappedState = stateChoiceMapping.getState();
					mappedChoice = stateChoiceMapping.getChoice();
				}
				if (rewards.getTransitionReward(mappedState, mappedChoice) > 0){
					return false;
				}
				final Iterator<Integer> successors = collapsedZeroRewardMecs.getSuccessorsIterator(state, choice);
				while (successors.hasNext()){
					if (successors.next() != state){
						return false;
					}
				}
				return true;
			}
		});
	}

	public Triplet<RewardWrapperMDP, BitSet, BitSet> collapseZeroRewardMECs(final BitSet invariantStates, final BitSet goalStates, final int debugLevel, final BitSet statesOfInterest) throws PrismException
	{
		//this method is only called whenever we are interested in Interval-Iteration
		//this method does the transformation in order to create the prerequisites for the Interval-Iteration
		final Triplet<List<BitSet>, BitSet, BitSet> mecCalculationResult = getZeroRewardMECs(invariantStates, goalStates);
		final List<BitSet> zeroRewardMecs = mecCalculationResult.getFirst();
		final KeepSingletons equivalenceRelation = new EquivalenceRelationInteger.KeepSingletons(zeroRewardMecs);
		final MDPEquiv collapsedZeroRewardMecs = new MDPEquiv(mdp, equivalenceRelation);
		final MDPDroppedChoices droppedMecSelfLoops = dropSelfLoopsInZeroRewardMECs(collapsedZeroRewardMecs, equivalenceRelation);
		//a deadlock occurs whenever we find a bottom MEC, so no chance of leaving this state (and therefore the MEC it represents)
		//so, it is a zero-probability state (if the MEC does not contain a goal-state)
		//or, it is a prob-1-state (if the MEC contains a goal-state)
		//that is the reason, why the deadlock-states could be reused to settle prob0- or prob1-states
		//BUT: in order to do so, one has to build the set of the deadlock-states and utilize it for the upcoming context-generation
		//     this generation does a reachability-analysis of a U b in either case, regardless if interested in upper- or lower-reward bounded quantiles
		//     both cases do the min- / max-probability computation (upper-reward bounded: used for precomputation / lower-reward bounded: used for weighted sum if reward-budget is not sufficient)
		//     the deadlock-states could be put into the known-values, with the value 0 or 1, depending on the considered MEC
		//     BUT: the reachability-computation does prob0- and prob1-computations each time it is called
		//          the already known 0- and 1-values are overwritten by this analysis, so there is no advantage of reusing the deadlock-states
		//I prefer not to fiddle around with the deadlock-states here, since this will create a new BitSet, which needs mappings and so on
		//this consumes time and memory for results that will be overwritten by prob0- and prob1-computations anyways, regardless if present or not
		//instead I will use predicates here and be much more efficient and smart ;)
		final IntPredicate isDeadlock = state -> equivalenceRelation.isRepresentative(state) && (droppedMecSelfLoops.getNumChoices(state) == 0);
		final MDP preventDeadlocks = MDPAdditionalChoices.addSelfLoops(droppedMecSelfLoops, isDeadlock);
		final BasicModelTransformation<MDP, MDPRestricted> transformModelToReachableStates = MDPRestricted.transform(preventDeadlocks, BitSetTools.complement(getNumStates(), equivalenceRelation.getNonRepresentatives()), statesOfInterest, Restriction.TRANSITIVE_CLOSURE_SAFE);
		final MDPRestricted reachableModel = transformModelToReachableStates.getTransformedModel();

		final MDPRewards rewards4CollapsedModel = getRewards4collapsedZeroRewardMECs(reachableModel, collapsedZeroRewardMecs, droppedMecSelfLoops, equivalenceRelation);

		final BitSet invariantStatesInReachableModel = markInReachableModel(mecCalculationResult.getSecond(), invariantStates, reachableModel);
		final BitSet goalStatesInReachableModel = markInReachableModel(mecCalculationResult.getThird(), goalStates, reachableModel);

		if (debugLevel > 9){
			mdp.exportToDotFile("0-originalModel-markZeroRewardMECs.dot", BitSetTools.union(zeroRewardMecs));
			collapsedZeroRewardMecs.exportToDotFile("1-collapseZeroRewardMECs-markRepresentatives.dot", BitSetTools.complement(getNumStates(), equivalenceRelation.getNonRepresentatives()));
			droppedMecSelfLoops.exportToDotFile("2-dropZeroRewardMECselfLoops-markZeroRewardMECs.dot", BitSetTools.union(zeroRewardMecs));
			preventDeadlocks.exportToDotFile("3-preventDeadlocks-markStatesWithNewLoop.dot", BitSetTools.asBitSet(new FilteringIterable.OfInt(new Interval(getNumStates()), isDeadlock)));
			reachableModel.exportToDotFile("4-reachableStates.dot");

			mainLog.println("\ntransformed model for computation of interval-iteration over maximising properties:");
			mainLog.println("zero-reward MECs in original model: " + zeroRewardMecs);
			for (int state = 0, numStates = reachableModel.getNumStates(); state < numStates; state++){
				mainLog.println("state " + state + " (state-reward = " + rewards4CollapsedModel.getStateReward(state) + ") corresponds to state " + reachableModel.mapStateToOriginalModel(state) + ", has " + reachableModel.getNumChoices(state) + " choices");
				for (int choice = 0, numChoices = reachableModel.getNumChoices(state); choice < numChoices; choice++){
					mainLog.println("\tchoice " + choice + " (" + reachableModel.getAction(state, choice) + ", transition-reward = " + rewards4CollapsedModel.getTransitionReward(state, choice) + ")");
				}
			}
			mainLog.println("\ninvariant states before transformation: " + invariantStates);
			mainLog.println("invariant states after transformation: " + invariantStatesInReachableModel);
			mainLog.println("goal states before transformation: " + goalStates);
			mainLog.println("goal states after transformation: " + goalStatesInReachableModel);
		}
		final MDP modelForCalculation = new MDPSparse(reachableModel);
		if (debugLevel > 0){
			mainLog.println("\ntransformed model because of interval-iteration for maximising quantile:");
			mainLog.println(modelForCalculation.infoString() + "\n");
		}
		final RewardWrapperMDP transformedModel = new RewardWrapperMDP(this, modelForCalculation, rewards4CollapsedModel, transformModelToReachableStates);
		return new Triplet<>(transformedModel, invariantStatesInReachableModel, goalStatesInReachableModel);
	}
}
