package explicit.modelviews;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.Map.Entry;

import common.BitSetTools;
import common.functions.Mapping;
import common.iterable.ChainedIterable;
import common.iterable.MappingIterable;
import common.iterable.MappingIterator;
import common.iterable.collections.ChainedList;
import common.iterable.collections.UnionSet;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;

public class MDPDisjointUnion extends MDPView
{
	private MDP model1;
	private MDP model2;
	private final int offset;
	private final Function<Integer, Integer> shiftStateUp;
	private final TransitionShift shiftTransitionUp;



	public MDPDisjointUnion(final MDP model1, final MDP model2)
	{
		this.model1 = model1;
		this.model2 = model2;
		offset = model1.getNumStates();
		shiftStateUp = x -> x + offset;
		shiftTransitionUp = new TransitionShift(offset);
	}

	public MDPDisjointUnion(final MDPDisjointUnion union)
	{
		super(union);
		model1 = union.model1;
		model2 = union.model2;
		offset = union.offset;
		shiftStateUp = union.shiftStateUp;
		shiftTransitionUp = union.shiftTransitionUp;
	}



	//--- Cloneable ---

	@Override
	public MDPDisjointUnion clone()
	{
		return new MDPDisjointUnion(this);
	}



	//--- Model ---

	@Override
	public int getNumStates()
	{
		return model1.getNumStates() + model2.getNumStates();
	}

	@Override
	public int getNumInitialStates()
	{
		return model1.getNumInitialStates() + model2.getNumInitialStates();
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		final Iterable<Integer> initials1 = model1.getInitialStates();
		final Iterable<Integer> initials2 = model2.getInitialStates();
		return new ChainedIterable.Of<>(initials1, new MappingIterable.From<>(initials2, shiftStateUp));
	}

	@Override
	public int getFirstInitialState()
	{
		final int state = model1.getFirstInitialState();
		return state > 0 ? state : model2.getFirstInitialState();
	}

	@Override
	public boolean isInitialState(final int state)
	{
		return (state < offset) ? model1.isInitialState(state) : model2.isInitialState(state - offset);
	}

	@Override
	public List<State> getStatesList()
	{
		final List<State> states1 = model1.getStatesList();
		final List<State> states2 = model2.getStatesList();
		if (states1 == null || states2 == null) {
			return null;
		}
		return new ChainedList<>(states1, states2);
	}

	@Override
	public VarList getVarList()
	{
		// FIXME ALG: Can we be more efficient than potentially recomputing the VarList?
		return null;
	}

	@Override
	public Values getConstantValues()
	{
		final Values constantValues1 = model1.getConstantValues();
		final Values constantValues2 = model2.getConstantValues();
		if (constantValues1 == null || constantValues2 == null) {
			return null;
		}

		final Values constantValues = new Values(constantValues1);
		final int numValues = constantValues2.getNumValues();
		for (int constant = 0; constant < numValues; constant++) {
			final String name = constantValues2.getName(constant);
			final Object value = constantValues2.getValue(constant);
			final int index = constantValues1.getIndexOf(name);
			if (index == -1) {
				constantValues.addValue(name, value);
			} else {
				assert constantValues.getValue(index).equals(value) : "consistent values expeçted";
			}
		}
		return constantValues;
	}

	@Override
	public BitSet getLabelStates(final String name)
	{
		final BitSet labelStates1 = model1.getLabelStates(name);
		final BitSet labelStates2 = model2.getLabelStates(name);
		final BitSet labelStates = (labelStates2 == null) ? new BitSet(0) : BitSetTools.shiftUp(labelStates2, offset);
		labelStates.or((labelStates1 == null) ? new BitSet(0) : labelStates1);
		return labelStates;
	}

	@Override
	public Set<String> getLabels()
	{
		return new UnionSet<>(model1.getLabels(), model2.getLabels());
	}

	@Override
	public boolean hasLabel(String name)
	{
		return model1.hasLabel(name) | model2.hasLabel(name);
	}


	@Override
	public int getNumTransitions()
	{
		return model1.getNumTransitions() + model2.getNumTransitions();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		return (state < offset) ? model1.getSuccessorsIterator(state) : new MappingIterator.From<>(model2.getSuccessorsIterator(state - offset), shiftStateUp);
	}

	@Override
	public boolean isSuccessor(final int s1, final int s2)
	{
		if (s1 < offset && s2 < offset) {
			return model1.isSuccessor(s1, s2);
		}
		if (s1 >= offset && s2 >= offset) {
			return model2.isSuccessor(offset + s1, offset + s2);
		}
		return false;
	}



	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		return (state < offset) ? model1.getNumChoices(state) : model2.getNumChoices(state - offset);
	}

	@Override
	public int getMaxNumChoices()
	{
		return Math.max(model1.getMaxNumChoices(), model2.getMaxNumChoices());
	}

	@Override
	public int getNumChoices()
	{
		return model1.getNumChoices() + model2.getNumChoices();
	}

	@Override
	public Object getAction(int state, int choice)
	{
		return (state < offset) ? model1.getAction(state, choice) : model2.getAction(state - offset, choice);
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return model1.areAllChoiceActionsUnique() && model2.areAllChoiceActionsUnique();
	}

	@Override
	public int getNumTransitions(final int state, final int choice)
	{
		return (state < offset) ? model1.getNumTransitions(state, choice) : model2.getNumTransitions(state - offset, choice);
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(int state, int choice)
	{
		return (state < offset) ? model1.getSuccessorsIterator(state, choice)
				: new MappingIterator.From<>(model2.getSuccessorsIterator(state - offset, choice), shiftStateUp);
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		return (state < offset) ? model1.getTransitionsIterator(state, choice)
				: new MappingIterator.From<>(model2.getTransitionsIterator(state - offset, choice), shiftTransitionUp);
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		try {
			model1.findDeadlocks(false);
			model2.findDeadlocks(false);
		} catch (final PrismException e) {
			assert false : "no attempt to fix deadlocks";
		}
		model1 = MDPAdditionalChoices.fixDeadlocks(model1);
		model2 = MDPAdditionalChoices.fixDeadlocks(model2);
	}



	//--- static methods ---

	public static MDP union(final MDP... models)
	{
		return union(Arrays.asList(models));
	}

	public static MDP union(final Iterable<? extends MDP> models)
	{
		return union(models.iterator());
	}

	public static MDP union(final Iterator<? extends MDP> models)
	{
		if (!models.hasNext()) {
			throw new IllegalArgumentException("at least one model expected");
		}
		MDP union = models.next();
		while (models.hasNext()) {
			union = new MDPDisjointUnion(union, models.next());
		}

		return union;
	}

	// FIXME ALG: reconsider interface types
	public static MDP MDPUnion(final MDP model1, final MDP model2, final Map<Integer, Integer> identify)
	{
		final MDPDisjointUnion union = new MDPDisjointUnion(model1, model2);
		final Mapping<Entry<Integer, Integer>, BitSet> equivalenceClass = new Mapping<Entry<Integer, Integer>, BitSet>()
		{
			@Override
			public final BitSet apply(final Entry<Integer, Integer> id)
			{
				final BitSet equivalentStates = new BitSet();
				equivalentStates.set(id.getKey());
				equivalentStates.set(union.offset + id.getValue());
				return equivalentStates;
			}
		};
		return MDPAlteredDistributions.identifyStates(union, new MappingIterable.From<>(identify.entrySet(), equivalenceClass));
	}

	public static void main(final String[] args) throws PrismException
	{
		final MDPSimple original1 = new MDPSimple(3);
		original1.addInitialState(0);
		Distribution dist = new Distribution();
		dist.add(1, 0.1);
		dist.add(2, 0.9);
		original1.addActionLabelledChoice(0, dist, "a");
		dist = new Distribution();
		dist.add(1, 0.2);
		dist.add(2, 0.8);
		original1.addActionLabelledChoice(0, dist, "b");
		dist = new Distribution();
		dist.add(1, 1);
		original1.addActionLabelledChoice(1, dist, "a");
		dist = new Distribution();
		dist.add(2, 1);
		original1.addActionLabelledChoice(1, dist, "b");

		final MDPSimple original2 = new MDPSimple(2);
		original2.addInitialState(0);
		dist = new Distribution();
		dist.add(0, 0.1);
		dist.add(1, 0.9);
		original2.addActionLabelledChoice(0, dist, "a");
		dist = new Distribution();
		dist.add(0, 0.5);
		dist.add(1, 0.5);
		original2.addActionLabelledChoice(0, dist, "b");
		dist = new Distribution();
		dist.add(0, 1);
		original2.addActionLabelledChoice(1, dist, "a");

		MDP union;

		System.out.println("Original Model 1:");
		System.out.print(original1.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original1.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original1.getDeadlockStates()));
		System.out.println(original1);

		System.out.println();

		System.out.println("Original Model 2:");
		System.out.print(original2.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original2.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original2.getDeadlockStates()));
		System.out.println(original2);

		System.out.println();

		System.out.println("Disjoint Union Model:");
		union = new MDPDisjointUnion(original1, original2);
		union.findDeadlocks(true);
		System.out.print(union.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(union.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(union.getDeadlockStates()));
		System.out.println(union);

		System.out.println();

		final Map<Integer, Integer> identify = new HashMap<>();
		identify.put(1, 0);
		System.out.println("Union Model:");
		union = MDPUnion(original1, original2, identify);
		System.out.print(union.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(union.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(union.getDeadlockStates()));
		System.out.println(union);

		System.out.println();

		System.out.println("Union Model New:");
		union = MDPUnion(original1, original2, identify);
		System.out.print(union.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(union.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(union.getDeadlockStates()));
		System.out.println(union);
	}
}