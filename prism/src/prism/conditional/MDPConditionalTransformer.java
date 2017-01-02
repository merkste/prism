package prism.conditional;

import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.Model;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.OpRelOpBound;
import prism.Prism;
import prism.PrismLangException;

public abstract class MDPConditionalTransformer extends NewConditionalTransformer.MDP
{
	protected Prism prism;

	public MDPConditionalTransformer(NondetModelChecker modelChecker, Prism prism)
	{
		super(modelChecker);
		this.prism = prism;
	}

	@Override
	public boolean canHandle(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!(model instanceof NondetModel)) {
			return false;
		}
		final NondetModel mdp = (NondetModel) model;
		return canHandleCondition(mdp, expression) && canHandleObjective(mdp, expression);
	}

	public abstract boolean canHandleCondition(Model model, ExpressionConditional expression)  throws PrismLangException;

	public boolean canHandleObjective(Model model, ExpressionConditional expression) throws PrismLangException
	{
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final OpRelOpBound oprel = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		// can handle maximal probabilities only
		return oprel.getMinMax(model.getModelType()).isMax();
	}
}
