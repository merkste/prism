package prism.conditional;

import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.Model;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.OpRelOpBound;
import prism.Prism;
import prism.PrismLangException;

public abstract class MDPConditionalTransformer extends ConditionalTransformer<NondetModelChecker, NondetModel>
{
	public MDPConditionalTransformer(NondetModelChecker modelChecker, Prism prism)
	{
		super(modelChecker, prism);
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

	protected abstract boolean canHandleCondition(NondetModel model, ExpressionConditional expression)  throws PrismLangException;

	protected boolean canHandleObjective(NondetModel model, ExpressionConditional expression) throws PrismLangException
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
