package explicit.conditional.transformer.mdp;

import java.util.BitSet;

import common.BitSetTools;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import explicit.BasicModelTransformation;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.conditional.transformer.ConditionalTransformer;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.conditional.transformer.mdp.MDPFinallyTransformer.BadStatesTransformation;
import explicit.conditional.transformer.mdp.MDPResetTransformer.ResetTransformation;
import explicit.modelviews.MDPRestricted;

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
	public abstract ConditionalReachabilitiyTransformation<MDP, MDP> transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException;

	public ConditionalReachabilitiyTransformation<MDP, MDP> transformReset(final BadStatesTransformation badStatesTransformation, final BitSet statesOfInterest) throws PrismException
	{
		// 1) Restriction to States Reachable form States of Interest
		final BasicModelTransformation<MDP, MDPRestricted> restriction = MDPRestricted.transform(badStatesTransformation.getTransformedModel(), badStatesTransformation.mapToTransformedModel(statesOfInterest));

		// 2) Reset Transformation
		final MDPRestricted restrictedModel = restriction.getTransformedModel();
		final BitSet restrictedBadStates = restriction.mapToTransformedModel(badStatesTransformation.getBadStates());
		final BitSet restrictedStatesOfInterest = restriction.mapToTransformedModel(badStatesTransformation.mapToTransformedModel(statesOfInterest));
		final MDPResetTransformer reset = new MDPResetTransformer(modelChecker);
		final ResetTransformation<MDP> resetTransformation = reset.transformModel(restrictedModel, restrictedBadStates, restrictedStatesOfInterest);

		// 3) Flatten Nested Transformation
		final BitSet restrictedGoalStates = restriction.mapToTransformedModel(badStatesTransformation.getGoalStates());
		final BasicModelTransformation<MDP, MDP> composed = resetTransformation.compose(restriction.compose(badStatesTransformation));
		return new ConditionalReachabilitiyTransformation<>(composed, resetTransformation.mapToTransformedModel(restrictedGoalStates));
	}

	protected void checkSatisfiability(final MDP model, final BitSet goalStates, final BitSet statesOfInterest) throws UndefinedTransformationException
	{
		final BitSet unsatisfiable = modelChecker.prob0(model, null, goalStates, false, null);
		if (!BitSetTools.areDisjoint(unsatisfiable, statesOfInterest)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
	}
}