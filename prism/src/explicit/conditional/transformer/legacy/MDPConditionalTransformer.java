package explicit.conditional.transformer.legacy;

import java.util.BitSet;

import common.iterable.IterableBitSet;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import explicit.Model;
import explicit.conditional.transformer.ConditionalTransformer;
import explicit.conditional.transformer.mdp.ConditionalReachabilitiyTransformation;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;

@Deprecated
public abstract class MDPConditionalTransformer extends ConditionalTransformer.Basic<MDP, MDPModelChecker>
{
	public MDPConditionalTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker);
	}

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

	@Override
	public boolean canHandle(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!(model instanceof MDP)) {
			return false;
		}
		final MDP mdp = (MDP) model;
		return canHandleCondition(mdp, expression) && canHandleObjective(mdp, expression);
	}

	protected boolean canHandleObjective(final MDP model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final OpRelOpBound oprel = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		// can handle maximal probabilities only
		return oprel.getMinMax(model.getModelType()).isMax();
	}

	protected abstract boolean canHandleCondition(final MDP model, final ExpressionConditional expression) throws PrismLangException;

	/**
	 * Override to specify return type.
	 * 
	 * @see explicit.conditional.transformer.ConditionalTransformer#transform ConditionalTransformer
	 **/
	@Override
	public abstract ConditionalReachabilitiyTransformation<MDP, MDP> transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException;
}