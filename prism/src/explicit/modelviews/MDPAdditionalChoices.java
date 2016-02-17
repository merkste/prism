package explicit.modelviews;

import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import common.BitSetTools;
import common.functions.primitive.AbstractMappingFromInteger;
import common.functions.primitive.MappingFromInteger;
import common.iterable.ArrayIterator;
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
	private MappingFromInteger<List<Iterator<Entry<Integer, Double>>>> choices;
	private MappingFromInteger<List<Object>> actions;



	public MDPAdditionalChoices(final MDP model, final MappingFromInteger<List<Iterator<Entry<Integer, Double>>>> choices,
			MappingFromInteger<List<Object>> actions)
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
		return model.getNumChoices(state) + choices.get(state).size();
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		final int originalNumChoices = model.getNumChoices(state);
		if (choice < originalNumChoices) {
			return model.getAction(state, choice);
		}
		if (actions == null) {
			final int numChoices = originalNumChoices + choices.get(state).size();
			if (choice < numChoices) {
				return null;
			}
			throw new IndexOutOfBoundsException("choice index out of bounds");
		}
		return actions.get(state).get(choice - originalNumChoices);
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
		final int originalNumChoices = model.getNumChoices(state);
		if (choice < originalNumChoices) {
			return model.getTransitionsIterator(state, choice);
		}
		return choices.get(state).get(choice - originalNumChoices);
	}

	//--- MDPView ---

	@Override
	protected MDPView fixDeadlocks()
	{
		if (fixedDeadlocks) {
			return this;
		}
		model = fixDeadlocks((MDP) this.clone());
		choices = new AbstractMappingFromInteger<List<Iterator<Entry<Integer, Double>>>>()
		{
			@Override
			public List<Iterator<Entry<Integer, Double>>> get(int element)
			{
				return Collections.emptyList();
			}
		};
		actions = null;
		fixedDeadlocks = true;
		return this;
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
		final MappingFromInteger<List<Iterator<Entry<Integer, Double>>>> addSelfLoops = new AbstractMappingFromInteger<List<Iterator<Entry<Integer, Double>>>>()
		{
			@Override
			public List<Iterator<Entry<Integer, Double>>> get(final int state)
			{
				if (states.get(state)) {
					final Entry<Integer, Double> loop = new AbstractMap.SimpleImmutableEntry<>(state, 1.0);
					return Collections.<Iterator<Entry<Integer, Double>>> singletonList(new ArrayIterator<>(loop));
				}
				return Collections.emptyList();
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

		final MappingFromInteger<List<Iterator<Entry<Integer, Double>>>> choices = new AbstractMappingFromInteger<List<Iterator<Entry<Integer, Double>>>>()
		{
			@Override
			public List<Iterator<Entry<Integer, Double>>> get(final int state)
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
					return Collections.emptyList();
				}
				return Collections.singletonList(distribution.iterator());
			}
		};

		final MappingFromInteger<List<Object>> actions = new AbstractMappingFromInteger<List<Object>>()
		{
			@Override
			public List<Object> get(final int state)
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