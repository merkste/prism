package explicit.conditional.transformer.mdp;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.functions.primitive.MappingInt;
import explicit.BasicModelTransformation;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.modelviews.MDPAdditionalChoices;
import explicit.modelviews.MDPAdditionalStates;
import explicit.modelviews.MDPDroppedAllChoices;
import prism.PrismComponent;
import prism.PrismException;

public abstract class ConditionalNormalFormTransformer extends PrismComponent
{
	public static final int GOAL = 0;

	protected MDPModelChecker modelChecker;
	protected final int numTrapStates;

	public ConditionalNormalFormTransformer(final MDPModelChecker modelChecker, int numTrapStates)
	{
		super(modelChecker);
		this.modelChecker = modelChecker;
		this.numTrapStates = numTrapStates;
	}

	public NormalFormTransformation transformModel(final MDP model, final BitSet objectiveStates, final BitSet conditionStates) throws PrismException
	{
		final BitSet terminalStates = getTerminalStates(objectiveStates, conditionStates);
		final MDPDroppedAllChoices terminalModel = new MDPDroppedAllChoices(model, terminalStates);

		final MDPAdditionalStates trapModel = new MDPAdditionalStates(terminalModel, numTrapStates);
		final MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = getChoices(model, objectiveStates, conditionStates);
		final MappingInt<List<Object>> actions = getActions(model);
		final MDPAdditionalChoices normalFormModel = new MDPAdditionalChoices(trapModel, choices, actions);

		return new NormalFormTransformation(model, normalFormModel);
	}

	protected abstract MappingInt<List<Iterator<Entry<Integer, Double>>>> getChoices(final MDP model, final BitSet objectiveStates,
			final BitSet conditionStates) throws PrismException;

	protected MappingInt<List<Object>> getActions(final MDP model)
	{
		final int offset = model.getNumStates();
		final List<Object> actions = Collections.<Object>singletonList("normal-form-step");

		return state -> (state < offset) ? actions : null;
	}

	protected abstract BitSet getTerminalStates(final BitSet objectiveStates, final BitSet conditionStates);



	public static class NormalFormTransformation extends BasicModelTransformation<MDP, MDP>
	{
		public NormalFormTransformation(final MDP originalModel, final MDP transformedModel)
		{
			super(originalModel, transformedModel);
		}

		public NormalFormTransformation(final NormalFormTransformation transformation)
		{
			super(transformation);
		}

		public int getGoalState()
		{
			return numberOfStates + GOAL;
		}
	}
}