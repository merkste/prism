package explicit.conditional.transformer;

import java.util.BitSet;

import common.iterable.IterableBitSet;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import explicit.Model;
import explicit.conditional.ConditionalTransformer;

public abstract class MDPConditionalTransformer extends ConditionalTransformer<MDPModelChecker, MDP>
{
	public MDPConditionalTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker);
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

	protected abstract boolean canHandleCondition(final MDP model, final ExpressionConditional expression) throws PrismLangException;

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

	@Override
	public abstract ConditionalMDPTransformation transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException;

	public void checkStatesOfInterest(final BitSet statesOfInterest) throws PrismException
	{
		if (statesOfInterest.cardinality() != 1) {
			// FIXME ALG: remove after implementing successive model checking for multiple states
			throw new PrismException("statesOfInterest exptected to be a singleton set");
		}
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