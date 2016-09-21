package explicit.modelviews;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import common.BitSetTools;
import common.IteratorTools;
import common.functions.primitive.AbstractPairPredicateIntegerInteger;
import common.functions.primitive.PairPredicateIntegerInteger;
import common.iterable.Interval;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;

public class MDPDroppedChoices extends MDPView
{
	private MDP model;
	private PairPredicateIntegerInteger preserved;



	public MDPDroppedChoices(final MDP model, final PairPredicateIntegerInteger dropped)
	{
		this.model = model;
		// FIXME ALG: consider using preserved instead of dropped
		this.preserved = dropped.not();
	}

	public MDPDroppedChoices(final MDPDroppedChoices dropped)
	{
		super(dropped);
		model = dropped.model;
		preserved = dropped.preserved;
	}



	//--- Clonable ---

	@Override
	public MDPDroppedChoices clone()
	{
		return new MDPDroppedChoices(this);
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
		// FIXME ALG: consider loop instead of Interval for performance
		return IteratorTools.count(new Interval(model.getNumChoices(state)), preserved.curry(state));
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		final int originalChoice = mapChoiceToOriginalModel(state, choice);
		return model.getAction(state, originalChoice);
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return model.areAllChoiceActionsUnique() ? true : super.areAllChoiceActionsUnique();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		final int originalChoice = mapChoiceToOriginalModel(state, choice);
		return model.getSuccessorsIterator(state, originalChoice);
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		final int originalChoice = mapChoiceToOriginalModel(state, choice);
		return model.getTransitionsIterator(state, originalChoice);
	}



	//--- MDPView ---

	@Override
	protected MDPDroppedChoices fixDeadlocks()
	{
		if (fixedDeadlocks) {
			return this;
		}
		model = MDPAdditionalChoices.fixDeadlocks((MDP) this.clone());
		preserved = new AbstractPairPredicateIntegerInteger()
		{
			@Override
			public boolean getBoolean(int element1, int element2)
			{
				return true;
			}
		};
		fixedDeadlocks = true;
		return this;
	}



	//--- static methods

	public static MDPDroppedChoices dropDenormalizedDistributions(final MDP model)
	{
		final PairPredicateIntegerInteger denormalizedChoices = new AbstractPairPredicateIntegerInteger()
		{
			@Override
			public boolean getBoolean(int state, int choice)
			{
				final Distribution distribution = new Distribution(model.getTransitionsIterator(state, choice));
				return distribution.sum() < 1;
			}
		};
		return new MDPDroppedChoices(model, denormalizedChoices);
	}

	// FIXME ALG: similar method in MDPAlteredDistributions
	public int mapChoiceToOriginalModel(final int state, final int choice)
	{
		int countChoices = 0;
		for (int originalChoice = 0, numOriginalChoices = model.getNumChoices(state); originalChoice < numOriginalChoices; originalChoice++) {
			if (preserved.getBoolean(state, originalChoice)) {
				if (countChoices == choice) {
					return originalChoice;
				}
				countChoices++;
			}
		}
		throw new IndexOutOfBoundsException("choice index out of bounds");
	}

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

		final PairPredicateIntegerInteger dropped = new AbstractPairPredicateIntegerInteger()
		{
			@Override
			public boolean getBoolean(final int state, final int choice)
			{
				switch (state) {
				case 0:
					return choice == 1;
				case 1:
					return true;
				default:
					break;
				}
				return false;
			}
		};

		System.out.println("Dropped Choices Model:");
		MDP droppedChoices = new MDPDroppedChoices(original, dropped);
		droppedChoices.findDeadlocks(true);
		System.out.print(droppedChoices.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(droppedChoices.getDeadlockStates()));
		System.out.println(droppedChoices);
	}
}