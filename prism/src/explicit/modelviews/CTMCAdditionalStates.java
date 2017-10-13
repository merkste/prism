package explicit.modelviews;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import common.BitSetTools;
import common.iterable.Interval;
import common.iterable.collections.ChainedList;
import common.iterable.collections.UnionSet;
import explicit.CTMC;
import explicit.CTMCSimple;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;

public class CTMCAdditionalStates extends CTMCView
{
	protected CTMC model;
	protected int numStates;
	protected Interval indices;
	protected List<State> states;

	public CTMCAdditionalStates(final CTMC model, final int numStates)
	{
		this(model, numStates, true);
	}

	public CTMCAdditionalStates(final CTMC model, final int numStates, final boolean fill)
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

	public CTMCAdditionalStates(final CTMC model, final State ... states)
	{
		this(model, Arrays.asList(states));
	}

	public CTMCAdditionalStates(final CTMC model, final List<State> states)
	{
		this.model = model;
		this.numStates = (states == null) ? 0 : states.size();
		final int originalNumStates = model.getNumStates();
		indices = new Interval(originalNumStates, originalNumStates + numStates);
		this.states = states;
	}

	public CTMCAdditionalStates(final CTMCAdditionalStates additional)
	{
		super(additional);
		model = additional.model;
		numStates = additional.numStates;
		indices = additional.indices;
		states = additional.states;
	}



	//--- Cloneable ---

	@Override
	public CTMCAdditionalStates clone()
	{
		return new CTMCAdditionalStates(this);
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
	public BitSet getLabelStates(String name)
	{
		return super.hasLabel(name) ? super.getLabelStates(name) : model.getLabelStates(name);
	}

	@Override
	public Set<String> getLabels()
	{
		return new UnionSet<>(super.getLabels(), model.getLabels());
	}

	@Override
	public boolean hasLabel(String name)
	{
		return super.hasLabel(name) || model.hasLabel(name);
	}

	@Override
	public int getNumTransitions()
	{
		return model.getNumTransitions();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		return (state < model.getNumStates()) ? model.getSuccessorsIterator(state) : Collections.<Integer>emptyIterator();
	}

	@Override
	public boolean isSuccessor(final int s1, final int s2)
	{
		return (s1 < model.getNumStates()) && (s2 < model.getNumStates()) && model.isSuccessor(s1, s2);
	}




	//--- DTMC ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state)
	{
		if (state < model.getNumStates()) {
			return model.getTransitionsIterator(state);
		}
		throw new IndexOutOfBoundsException("state index out of bounds");
	}



	//--- CTMC ---

	@Override
	public void uniformise(double q)
	{
		model = uniformised(clone(), q);
		states = null;
		numStates = 0;
		fixedDeadlocks = true;
	}



	//--- ModelView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = CTMCAlteredDistributions.fixDeadlocks(clone());
		states = null;
		numStates = 0;
		fixedDeadlocks = true;
	}



	//--- instance methods ---

	public Interval getAdditionalStateIndices()
	{
		return indices;
	}



	//--- static methods ---
	public static void main(final String[] args) throws PrismException
	{
		final CTMCSimple original = new CTMCSimple(3);
		original.addInitialState(0);

		original.setProbability(0, 1, 0.1);
		original.setProbability(0, 2, 0.9);

		original.setProbability(1, 1, 1);

		original.setProbability(1, 2, 1);

		System.out.println("Original Model:");
		System.out.print(original.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original.getDeadlockStates()));
		System.out.println(original);

		System.out.println();
		CTMCAdditionalStates union = new CTMCAdditionalStates(original, 2);

		System.out.print("Model with ");
		union.findDeadlocks(true);
		System.out.print(union.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(union.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(union.getDeadlockStates()));
		System.out.println("New States:  " +  union.getAdditionalStateIndices());
		System.out.println(union);
	}
}
