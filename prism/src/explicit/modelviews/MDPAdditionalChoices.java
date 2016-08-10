package explicit.modelviews;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import common.BitSetTools;
import common.functions.primitive.MappingInt;
import explicit.DiracDistribution;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;

public class MDPAdditionalChoices extends MDPView
{
	private MDP model;
	private IntFunction<List<Iterator<Entry<Integer, Double>>>> choices;
	private IntFunction<List<Object>> actions;



	/**
	 * If {@code choices} returns {@code null} for a state and a choice, no additional choice is added.
	 * If {@code actions} is {@code null} or returns {@code null} for a state, no additional action is attached.
	 *
	 * @param model
	 * @param choices
	 * @param actions
	 */
	public MDPAdditionalChoices(final MDP model, final IntFunction<List<Iterator<Entry<Integer, Double>>>> choices,
			IntFunction<List<Object>> actions)
	{
		this.model = model;
		this.choices = choices;
		this.actions = actions;
	}

	public MDPAdditionalChoices(final MDPAdditionalChoices additional)
	{
		super(additional);
		model = additional.model;
		choices = additional.choices;
		actions = additional.actions;
	}



	//--- Cloneable ---

	@Override
	public MDPView clone()
	{
		return new MDPAdditionalChoices(this);
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


	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		return model.getNumChoices(state) + getNumAdditionalChoices(state);
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		final int numOriginalChoices = model.getNumChoices(state);
		if (choice < numOriginalChoices) {
			return model.getAction(state, choice);
		}
		if (actions == null) {
			final int numChoices = numOriginalChoices + getNumAdditionalChoices(state);
			if (choice < numChoices) {
				return null;
			}
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}
		
		final List<Object> additional = actions.apply(state);
		return (additional == null) ?  null : additional.get(choice - numOriginalChoices);
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return model.areAllChoiceActionsUnique() && super.areAllChoiceActionsUnique();
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		final int numOriginalChoices = model.getNumChoices(state);
		if (choice < numOriginalChoices) {
			return model.getTransitionsIterator(state, choice);
		}
		try {
			return choices.apply(state).get(choice - numOriginalChoices);
		} catch (NullPointerException | IndexOutOfBoundsException e)
		{
			// alter message of exception
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}
	}

	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = fixDeadlocks((MDP) this.clone());
		choices = new MappingInt<List<Iterator<Entry<Integer, Double>>>>()
		{
			@Override
			public List<Iterator<Entry<Integer, Double>>> apply(int element)
			{
				return null;
			}
		};
		actions = null;
	}



	//--- instance methods ---

	private int getNumAdditionalChoices(final int state)
	{
		final List<Iterator<Entry<Integer, Double>>> additional = choices.apply(state);
		return (additional == null) ? 0 : additional.size();
	}



	//--- static methods ---

	public static MDPView fixDeadlocks(final MDP model)
	{
		final BitSet deadlockStates = BitSetTools.asBitSet(model.getDeadlockStates());
		final MDPView fixed = addSelfLoops(model, deadlockStates);
		fixed.deadlockStates = deadlockStates;
		fixed.fixedDeadlocks = true;
		return fixed;
	}

	public static MDPView addSelfLoops(final MDP model, final BitSet states)
	{
		return addSelfLoops(model, states::get);
	}

	public static MDPView addSelfLoops(final MDP model, final IntPredicate states)
	{
		final IntFunction<List<Iterator<Entry<Integer, Double>>>> addSelfLoops = new IntFunction<List<Iterator<Entry<Integer, Double>>>>()
		{
			@Override
			public List<Iterator<Entry<Integer, Double>>> apply(final int state)
			{
				if (states.test(state)) {
					return Collections.singletonList(DiracDistribution.iterator(state));
				}
				return null;
			}
		};
		return new MDPAdditionalChoices(model, addSelfLoops, null);
	}

	public static void main(final String[] args) throws PrismException
	{
		final MDPSimple original = new MDPSimple(5);
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

		final MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = new MappingInt<List<Iterator<Entry<Integer, Double>>>>()
		{
			@Override
			public List<Iterator<Entry<Integer, Double>>> apply(final int state)
			{
				final Distribution distribution = new Distribution();
				switch (state) {
				case 1:
					distribution.set(2, 0.4);
					distribution.set(3, 0.6);
					break;
				case 3:
					distribution.set(2, 0.5);
					distribution.set(3, 0.5);
					break;
				default:
					return null;
				}
				return Collections.singletonList(distribution.iterator());
			}
		};

		final MappingInt<List<Object>> actions = new MappingInt<List<Object>>()
		{
			@Override
			public List<Object> apply(final int state)
			{
				final Object action;
				switch (state) {
				case 1:
					action = "d";
					break;
				case 3:
					action = "e";
					break;
				default:
					action = null;
				}
				return Collections.singletonList(action);
			}
		};

		System.out.println("Altered Model:");
		MDP alteredChoices = new MDPAdditionalChoices(original, choices, actions);
		alteredChoices.findDeadlocks(true);
		System.out.print(alteredChoices.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(alteredChoices.getDeadlockStates()));
		System.out.println(alteredChoices);
	}
}