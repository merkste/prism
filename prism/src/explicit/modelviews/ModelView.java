package explicit.modelviews;

import java.io.File;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;
import java.util.function.IntConsumer;

import common.IterableBitSet;
import common.IterableStateSet;
import common.iterable.FilteringIterable;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import explicit.Model;
import explicit.PredecessorRelation;
import explicit.PredecessorRelationSparse;
import explicit.StateValues;
import parser.State;
import parser.VarList;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;
import prism.PrismSettings;

public abstract class ModelView implements Model
{
	protected boolean fixedDeadlocks    = false;
	protected BitSet deadlockStates     = new BitSet();
	protected Map<String,BitSet> labels = new HashMap<>();
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
			exportToPrismExplicitTra(log, PrismSettings.DEFAULT_EXPORT_MODEL_PRECISION);
		}
	}

	@Override
	public void exportToPrismExplicitTra(final File file) throws PrismException
	{
		exportToPrismExplicitTra(file.getPath());
	}

	@Override
	public void exportToDotFile(final String filename, int precision) throws PrismException
	{
		exportToDotFile(filename, null, precision);
	}

	@Override
	public void exportToDotFile(final String filename, final BitSet mark, int precision) throws PrismException
	{
		try (PrismFileLog log = PrismFileLog.create(filename)) {
			exportToDotFile(log, mark,precision);
		}
	}

	@Override
	public void exportToDotFile(final PrismLog out, int precision)
	{
		exportToDotFile(out, null, false, precision);
	}

	@Override
	public void exportToDotFile(final PrismLog out, final BitSet mark, int precision)
	{
		exportToDotFile(out, mark, false, precision);
	}

	@Override
	public void exportToDotFile(final PrismLog out, final BitSet mark, final boolean showStates, int precision)
	{
		// Header
		out.print("digraph " + getModelType() + " {\nsize=\"8,5\"\nnode [shape=box];\n");
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			out.print(state);
			// Style for each state
			if (mark != null && mark.get(state)) {
				out.print(" [style=filled  fillcolor=\"#cccccc\"]");
			} else {
				out.println(" []");
			}
			// Transitions for state
			exportTransitionsToDotFile(state, out);
		}
		// Append state info (if required)
		if (showStates) {
			final List<State> states = getStatesList();
			if (states != null) {
				for (int state = 0, max = getNumStates(); state < max; state++) {
					out.print(state + " [label=\"" + state + "\\n" + states.get(state) + "\"]\n");
				}
			}
		}
		// Footer
		out.print("}\n");
	}

	@Override
	public void exportStates(final int exportType, final VarList varList, final PrismLog log, int precision) throws PrismException
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
	
		final PredecessorRelation pre = PredecessorRelationSparse.forModel(parent, this);
	
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

	@Override
	public BitSet getLabelStates(String name)
	{
		return labels.containsKey(name) ? labels.get(name) : null;
	}

	@Override
	public Set<String> getLabels()
	{
		return labels.keySet();
	}

	@Override
	public boolean hasLabel(String name)
	{
		return labels.containsKey(name);
	}

	@Override
	public void addLabel(String name, BitSet states)
	{
		labels.put(name, states);
	}

	@Override
	public String addUniqueLabel(String prefix, BitSet labelStates)
	{
		String label = prefix;
		for (BigInteger i=BigInteger.ZERO; hasLabel(label); i=i.add(BigInteger.ONE)) {
			label = prefix + "_" + i;
		}

		addLabel(label, labelStates);
		return label;
	}



	//--- instance methods ---

	protected abstract void exportTransitionsToDotFile(final int state, final PrismLog out);

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