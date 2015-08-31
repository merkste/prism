package explicit.conditional.transformer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.functions.primitive.MappingInt;
import explicit.BasicModelTransformation;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import explicit.modelviews.MDPAdditionalChoices;
import explicit.modelviews.MDPDisjointUnion;
import explicit.modelviews.MDPDroppedAllChoices;
import parser.State;
import prism.PrismComponent;
import prism.PrismException;

public abstract class ConditionalNormalFormTransformer extends PrismComponent
{
	public static final int GOAL = 0;

	protected MDPModelChecker modelChecker;

	public ConditionalNormalFormTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker);
		this.modelChecker = modelChecker;
	}

	public BasicModelTransformation<MDP, MDP> transformModel(final MDP model, final BitSet objectiveStates, final BitSet conditionStates) throws PrismException
	{
		final BitSet terminalStates = getTerminalStates(objectiveStates, conditionStates);
		final MDPDroppedAllChoices terminalModel = new MDPDroppedAllChoices(model, terminalStates);
		final MDPDisjointUnion unionModel = addTrapStates(model, terminalModel);

		final MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = getChoices(model, objectiveStates, conditionStates);
		final MappingInt<List<Object>> actions = getActions();
		final MDPAdditionalChoices normalFormModel = new MDPAdditionalChoices(unionModel, choices, actions);

		return new BasicModelTransformation<MDP, MDP>(model, normalFormModel);
	}

	protected abstract MappingInt<List<Iterator<Entry<Integer, Double>>>> getChoices(final MDP model, final BitSet objectiveStates,
			final BitSet conditionStates) throws PrismException;

	protected MappingInt<List<Object>> getActions()
	{
		return MappingInt.constant(Collections.singletonList("normal-form-step"));
	}

	protected MDPDisjointUnion addTrapStates(final MDP model, final MDPDroppedAllChoices terminalModel)
	{
		final MDP trapStatesModel = buildTrapStatesModel(model, model.getFirstInitialState());
		return new MDPDisjointUnion(terminalModel, trapStatesModel);
	}

	protected abstract BitSet getTerminalStates(final BitSet objectiveStates, final BitSet conditionStates);

	protected MDPSimple buildTrapStatesModel(final MDP model, final int prototypeIndex)
	{
		final State prototype;
		if (model.getStatesList() == null) {
			prototype = null;
		} else {
			prototype = model.getStatesList().get(prototypeIndex);
		}
		return buildTrapStatesModel(model, prototype);
	}

	protected MDPSimple buildTrapStatesModel(final MDP model, final State prototype)
	{
		final MDPSimple trapStatesModel = new MDPSimple();
		if (prototype != null) {
			trapStatesModel.setStatesList(new ArrayList<State>());
		}
		addTrapState(trapStatesModel, prototype, true, "goal-loop");
		return trapStatesModel;
	}

	protected int addTrapState(final MDPSimple model, final State state, final boolean addLoop, final String action)
	{
		final int stateIndex = model.addState();
		if (state != null) {
			model.getStatesList().add(state);
		}
		if (addLoop) {
			final Distribution loop = new Distribution();
			loop.add(stateIndex, 1.0);
			if (action == null) {
				model.addChoice(stateIndex, loop);
			} else {
				model.addActionLabelledChoice(stateIndex, loop, action);
			}
		}
		return stateIndex;
	}
}