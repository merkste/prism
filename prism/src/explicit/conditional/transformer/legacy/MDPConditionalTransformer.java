package explicit.conditional.transformer.legacy;

import java.util.BitSet;

import common.iterable.IterableBitSet;
import explicit.Distribution;
import explicit.MDPModelChecker;
import explicit.MDPSimple;

public abstract class MDPConditionalTransformer extends explicit.conditional.transformer.mdp.MDPConditionalTransformer
{
	public MDPConditionalTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Deprecated
	protected void redirectChoices(final MDPSimple model, final BitSet states, final int target1, final int target2, final double[] probabilities)
	{
		for (Integer state : new IterableBitSet(states)) {
			final double probability = probabilities[state];
			final Distribution distribution = new Distribution();
			if (probability != 0.0) {
				distribution.add(target1, probability);
			}
			if (1.0 - probability != 0.0) {
				distribution.add(target2, 1.0 - probability);
			}
			model.clearState(state);
			model.addChoice(state, distribution);
		}
	}

	@Deprecated
	protected void addDiracChoice(final MDPSimple transformedModel, final Integer from, final int to, final String action)
	{
		final Distribution distribution = new Distribution();
		distribution.add(to, 1.0);
		if (action == null) {
			transformedModel.addChoice(from, distribution);
		} else {
			transformedModel.addActionLabelledChoice(from, distribution, action);
		}
	}
}