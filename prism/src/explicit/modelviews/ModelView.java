package explicit.modelviews;

import java.io.File;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.IntConsumer;

import common.iterable.FilteringIterable;
import common.iterable.IterableBitSet;
import common.iterable.IterableInt;
import common.iterable.IterableStateSet;
import explicit.Model;
import explicit.PredecessorRelation;
import explicit.StateValues;
import parser.State;
import parser.VarList;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;

public abstract class ModelView implements Model
{
	protected BitSet deadlockStates = new BitSet();
	protected boolean fixedDeadlocks = false;
	protected PredecessorRelation predecessorRelation;



	public ModelView()
	{
	}

	public ModelView(final ModelView model)
	{
		deadlockStates = (BitSet) model.deadlockStates.clone();
		fixedDeadlocks = model.fixedDeadlocks;
	}



	//--- Model ---

	@Override
	public int getNumDeadlockStates()
	{
		return deadlockStates.cardinality();
	}

	@Override
	public IterableInt getDeadlockStates()
	{
		return new IterableBitSet(deadlockStates);
	}

	@Override
	public StateValues getDeadlockStatesList()
	{
		return StateValues.createFromBitSet(deadlockStates, this);
	}

	@Override
	public int getFirstDeadlockState()
	{
		return deadlockStates.nextSetBit(0);
	}

	@Override
	public boolean isDeadlockState(final int state)
	{
		return deadlockStates.get(state);
	}

	@Override
	public boolean isSuccessor(final int s1, final int s2)
	{
		for (Iterator<Integer> successors = getSuccessorsIterator(s1); successors.hasNext();) {
			if (s2 == successors.next()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean allSuccessorsInSet(final int state, final BitSet set)
	{
		for (Iterator<Integer> successors = getSuccessorsIterator(state); successors.hasNext();) {
			if (!set.get(successors.next())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean someSuccessorsInSet(final int state, final BitSet set)
	{
		for (Iterator<Integer> successors = getSuccessorsIterator(state); successors.hasNext();) {
			if (set.get(successors.next())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void findDeadlocks(final boolean fix) throws PrismException
	{
		findDeadlocks(new BitSet()).forEach((IntConsumer) deadlockStates::set);

		if (fix && !fixedDeadlocks) {
			fixDeadlocks();
			fixedDeadlocks = true;
		}
	}

	public IterableInt findDeadlocks(final BitSet except)
	{
		IterableInt states = new IterableStateSet(except, getNumStates(), true);
		return new FilteringIterable.OfInt(states, state -> !getSuccessorsIterator(state).hasNext());
	}

	@Override
	public void checkForDeadlocks() throws PrismException
	{
		checkForDeadlocks(null);
	}

	@Override
	public void checkForDeadlocks(final BitSet except) throws PrismException
	{
		OfInt deadlocks = findDeadlocks(except).iterator();
		if (deadlocks.hasNext()) {
			throw new PrismException(getModelType() + " has a deadlock in state " + deadlocks.nextInt());
		}
	}

	@Override
	public void exportToPrismExplicit(final String baseFilename) throws PrismException
	{
		exportToPrismExplicitTra(baseFilename + ".tra");
	}

	@Override
	public void exportToPrismExplicitTra(final String filename) throws PrismException
	{
		try (PrismFileLog log = PrismFileLog.create(filename)) {
			exportToPrismExplicitTra(log);
		}
	}

	@Override
	public void exportToPrismExplicitTra(final File file) throws PrismException
	{
		exportToPrismExplicitTra(file.getPath());
	}

	@Override
	public void exportStates(final int exportType, final VarList varList, final PrismLog log) throws PrismException
	{
		final List<State> statesList = getStatesList();
		if (statesList == null)
			return;
	
		// Print header: list of model vars
		if (exportType == Prism.EXPORT_MATLAB)
			log.print("% ");
		log.print("(");
		final int numVars = varList.getNumVars();
		for (int i = 0; i < numVars; i++) {
			log.print(varList.getName(i));
			if (i < numVars - 1)
				log.print(",");
		}
		log.println(")");
		if (exportType == Prism.EXPORT_MATLAB)
			log.println("states=[");
	
		// Print states
		for (int state = 0, max = getNumStates(); state < max; state++) {
			final State stateDescription = statesList.get(state);
			if (exportType != Prism.EXPORT_MATLAB)
				log.println(state + ":" + stateDescription.toString());
			else
				log.println(stateDescription.toStringNoParentheses());
		}
	
		// Print footer
		if (exportType == Prism.EXPORT_MATLAB)
			log.println("];");
	}

	@Override
	public boolean hasStoredPredecessorRelation()
	{
		return (predecessorRelation != null);
	}

	@Override
	public PredecessorRelation getPredecessorRelation(PrismComponent parent, boolean storeIfNew)
	{
		if (predecessorRelation != null) {
			return predecessorRelation;
		}
	
		final PredecessorRelation pre = PredecessorRelation.forModel(parent, this);
	
		if (storeIfNew) {
			predecessorRelation = pre;
		}
		return pre;
	}

	@Override
	public void clearPredecessorRelation()
	{
		predecessorRelation = null;
	}



	//--- instance methods ---

	protected abstract void fixDeadlocks();

	/**
	 * Tell whether the receiver is a virtual or explicit model.
	 * Virtual models may impose a significant overhead on any computations.
	 *
	 * @return true iff model is a virtual model
	 */
	public boolean isVirtual()
	{
		return true;
	}
}
