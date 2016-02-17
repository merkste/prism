package explicit.modelviews;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import common.BitSetTools;
import common.iterable.EmptyIterator;
import common.iterable.Interval;
import common.iterable.collections.ChainedList;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;

public class MDPAdditionalStates extends MDPView
{
	private MDP model;
	private int numStates;
	private Interval indices;
	private List<State> states;

	public MDPAdditionalStates(final MDP model, final int numStates)
	{
		this(model, numStates, true);
	}

	public MDPAdditionalStates(final MDP model, final int numStates, final boolean fill)
	{
		this.model = model;
		this.numStates = numStates;
		final int originalNumStates = model.getNumStates();
		indices = new Interval(originalNumStates, originalNumStates + numStates);

		if (fill) {
			final List<State> statesList = model.getStatesList();
			if (statesList != null && !statesList.isEmpty()) {
				states = Collections.nCopies(numStates, statesList.get(0));
				return;
			}
		}
		this.states = null;
	}

	public MDPAdditionalStates(final MDP model, final State ... states)
	{
		this(model, Arrays.asList(states));
	}

	public MDPAdditionalStates(final MDP model, final List<State> states)
	{
		this.model = model;
		this.numStates = (states == null) ? 0 : states.size();
		final int originalNumStates = model.getNumStates();
		indices = new Interval(originalNumStates, originalNumStates + numStates);
		this.states = states;
	}

	public MDPAdditionalStates(final MDPAdditionalStates additional)
	{
		super(additional);
		model = additional.model;
		numStates = additional.numStates;
		indices = additional.indices;
		states = additional.states;
	}



	//--- Cloneable ---

	@Override
	public MDPAdditionalStates clone()
	{
		return new MDPAdditionalStates(this);
	}



	//--- Model ---

	@Override
	public int getNumStates()
	{
		return model.getNumStates() + numStates;
	}

	@Override
	public int getNumInitialStates()
	{
		return model.getNumInitialStates();
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		return model.getInitialStates();
	}

	@Override
	public int getFirstInitialState()
	{
		return model.getFirstInitialState();
	}

	@Override
	public boolean isInitialState(final int state)
	{
		return (state < model.getNumStates()) && model.isInitialState(state);
	}

	@Override
	public List<State> getStatesList()
	{
		return new ChainedList<>(model.getStatesList(), states);
	}

	@Override
	public VarList getVarList()
	{
		return model.getVarList();
	}

	@Override
	public Values getConstantValues()
	{
		return model.getConstantValues();
	}

	@Override
	public BitSet getLabelStates(final String name)
	{
		return model.getLabelStates(name);
	}

	@Override
	public Set<String> getLabels()
	{
		return model.getLabels();
	}

	@Override
	public boolean hasLabel(String name)
	{
		return model.hasLabel(name);
	}


	@Override
	public int getNumTransitions()
	{
		return model.getNumTransitions();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		return (state < model.getNumStates()) ? model.getSuccessorsIterator(state) : EmptyIterator.Of();
	}

	@Override
	public boolean isSuccessor(final int s1, final int s2)
	{
		return (s1 < model.getNumStates()) && (s2 < model.getNumStates()) && model.isSuccessor(s1, s2);
	}



	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		return (state < model.getNumStates()) ? model.getNumChoices(state) : 0;
	}

	@Override
	public int getMaxNumChoices()
	{
		return model.getMaxNumChoices();
	}

	@Override
	public int getNumChoices()
	{
		return model.getNumChoices();
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		if (state < model.getNumStates()) {
			return model.getAction(state, choice);
		}
		throw new IndexOutOfBoundsException("choice index out of bounds");
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return model.areAllChoiceActionsUnique();
	}

	@Override
	public int getNumTransitions(final int state, final int choice)
	{
		return (state < model.getNumStates()) ? model.getNumTransitions(state, choice) : 0;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(int state, int choice)
	{
		return (state < model.getNumStates()) ? model.getSuccessorsIterator(state, choice) : EmptyIterator.Of();
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		if (state < model.getNumStates()) {
			return model.getTransitionsIterator(state, choice);
		}
		throw new IndexOutOfBoundsException("choice index out of bounds");
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = MDPAdditionalChoices.fixDeadlocks(clone());
		states = null;
		numStates = 0;
	}



	//--- instance methods ---

	public Interval getAdditionalStateIndices()
	{
		return indices;
	}



	//--- static methods ---

	public static void main(final String[] args) throws PrismException
	{
		final MDPSimple original = new MDPSimple(3);
		original.addInitialState(0);
		Distribution dist = new Distribution();
		dist.add(1, 0.1);
		dist.add(2, 0.9);
		original.addActionLabelledChoice(0, dist, "a");
		dist = new Distribution();
		dist.add(1, 0.2);
		dist.add(2, 0.8);
		original.addActionLabelledChoice(0, dist, "b");
		dist = new Distribution();
		dist.add(1, 1);
		original.addActionLabelledChoice(1, dist, "a");
		dist = new Distribution();
		dist.add(2, 1);
		original.addActionLabelledChoice(1, dist, "b");

		System.out.println("Original Model:");
		System.out.print(original.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original.getDeadlockStates()));
		System.out.println(original);

		System.out.println();
		MDPAdditionalStates union = new MDPAdditionalStates(original, 2);

		System.out.print("Model with ");
		union.findDeadlocks(true);
		System.out.print(union.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(union.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(union.getDeadlockStates()));
		System.out.println("New States:  " +  union.getAdditionalStateIndices());
		System.out.println(union);
	}
}