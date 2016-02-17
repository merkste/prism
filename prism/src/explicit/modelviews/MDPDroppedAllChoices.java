package explicit.modelviews;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import common.BitSetTools;
import common.iterable.EmptyIterator;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;

public class MDPDroppedAllChoices extends MDPView
{
	private MDP model;
	private BitSet states;



	public MDPDroppedAllChoices(final MDP model, final BitSet dropped)
	{
		this.model = model;
		this.states = dropped;
	}

	public MDPDroppedAllChoices(final MDPDroppedAllChoices dropped)
	{
		super(dropped);
		model = dropped.model;
		states = dropped.states;
	}



	//--- Cloneable ---

	@Override
	public MDPDroppedAllChoices clone()
	{
		return new MDPDroppedAllChoices(this);
	}



	//--- Model ---

	@Override
	public int getNumStates()
	{
		return model.getNumStates();
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
		return model.isInitialState(state);
	}

	@Override
	public List<State> getStatesList()
	{
		return model.getStatesList();
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
	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		return states.get(state) ? EmptyIterator.Of() : model.getSuccessorsIterator(state);
	}



	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		return states.get(state) ? 0 : model.getNumChoices(state);
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		if (states.get(state)) {
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}
		return model.getAction(state, choice);
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return model.areAllChoiceActionsUnique() ? true : super.areAllChoiceActionsUnique();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		if (states.get(state)) {
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}
		return model.getSuccessorsIterator(state, choice);
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		if (states.get(state)) {
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}
		return model.getTransitionsIterator(state, choice);
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = MDPAdditionalChoices.fixDeadlocks((MDP) this.clone());
		states = new BitSet();
	}



	//--- static methods ---

	public static void main(final String[] args) throws PrismException
	{
		final MDPSimple original = new MDPSimple(4);
		original.addInitialState(1);
		Distribution dist = new Distribution();
		dist.add(1, 0.1);
		dist.add(2, 0.9);
		original.addActionLabelledChoice(0, dist, "a");
		dist = new Distribution();
		dist.add(1, 0.2);
		dist.add(2, 0.8);
		original.addActionLabelledChoice(0, dist, "b");
		dist = new Distribution();
		dist.add(3, 1);
		original.addActionLabelledChoice(1, dist, "c");
		dist = new Distribution();
		dist.add(2, 1);
		original.addChoice(1, dist);
		dist = new Distribution();
		dist.add(1, 0.3);
		dist.add(2, 0.7);
		original.addChoice(2, dist);
		original.findDeadlocks(false);

		System.out.println("Original Model:");
		System.out.print(original.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original.getDeadlockStates()));
		System.out.println(original);

		System.out.println();

		final BitSet dropped = BitSetTools.asBitSet(0, 2);

		System.out.println("Dropped Choices " + dropped + " Model:");
		MDP droppedChoices = new MDPDroppedAllChoices(original, dropped);
		droppedChoices.findDeadlocks(true);
		System.out.print(droppedChoices.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(droppedChoices.getDeadlockStates()));
		System.out.println(droppedChoices);
	}
}