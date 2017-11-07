package explicit.modelviews;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.iterable.MappingIterator;
import explicit.DTMCExplicit;
import explicit.MDP;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.Pair;
import prism.PrismException;

public class DTMCFromMDP extends DTMCView
{
	protected MDP model;

	public DTMCFromMDP(MDP model) throws PrismException
	{
		if (model.getMaxNumChoices() > 1) {
			throw new PrismException("Transforming an MDP into a DTMC requires at most one choice each state.");
		}
		this.model = model;
	}

	public DTMCFromMDP(DTMCFromMDP dtmc)
	{
		super(dtmc);
		this.model = dtmc.model;
	}



	//--- Cloneable ---

	@Override
	public DTMCFromMDP clone()
	{
		return new DTMCFromMDP(this);
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
	public boolean isInitialState(int state)
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



	//--- DTMC ---

	@Override
	public int getNumTransitions(int state)
	{
		if (model.getNumChoices() == 0) {
		}
		return model.getNumTransitions(state, 0);
	}

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(int state)
	{
		if (model.getNumChoices() == 0) {
			return Collections.emptyIterator();
		}
		return model.getTransitionsIterator(state, 0);
	}

	@Override
	public Iterator<Entry<Integer, Pair<Double, Object>>> getTransitionsAndActionsIterator(int state)
	{
		if (model.getNumChoices() == 0) {
			return Collections.emptyIterator();			
		}
		Object action = model.getAction(state, 0);
		Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state);
		return new MappingIterator.From<>(transitions, transition -> DTMCExplicit.attachAction(transition, action));
	}



	//--- DTMCView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		try {
			model.findDeadlocks(false);
		} catch (final PrismException e) {
			assert false : "no attempt to fix deadlocks";
		}
		model = MDPAdditionalChoices.fixDeadlocks(model);
	}
}
