package explicit.conditional.transformer.mdp;

import java.util.BitSet;

import common.BitSetTools;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ModelTransformationNested;
import explicit.conditional.transformer.ConditionalTransformer;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.conditional.transformer.mdp.MDPFinallyTransformer.BadStatesTransformation;
import explicit.conditional.transformer.mdp.MDPResetTransformer.ResetTransformation;

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

	protected void checkSatisfiability(final MDP model, final BitSet goalStates, final BitSet statesOfInterest) throws UndefinedTransformationException
	{
		final BitSet unsatisfiable = modelChecker.prob0(model, null, goalStates, false, null);
		if (!BitSetTools.areDisjoint(unsatisfiable, statesOfInterest)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
	}

	public ConditionalMDPTransformation transformReset(final BadStatesTransformation badStatesTransformation, final BitSet statesOfInterest) throws PrismException
	{
		// FIXME ALG: consider restriction to part reachable from states of interest
		final BitSet transformedStatesOfInterest = badStatesTransformation.mapToTransformedModel(statesOfInterest);
		final MDPResetTransformer resetTransformer = new MDPResetStateTransformer(modelChecker);
		final ResetTransformation<MDP> resetTransformation = resetTransformer.transformModel(badStatesTransformation.getTransformedModel(),
				badStatesTransformation.getBadStates(), transformedStatesOfInterest);

		// flatten nested transformation
		final ModelTransformationNested<MDP, MDP, MDP> transformation = new ModelTransformationNested<>(badStatesTransformation, resetTransformation);
		return new ConditionalMDPTransformation(transformation, badStatesTransformation.getGoalStates());
	}
}