package prism.conditional;

import parser.ast.ExpressionConditional;
import prism.Model;
import prism.Prism;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;

public abstract class MCConditionalTransformer extends NewConditionalTransformer.DTMC
{
	protected Prism prism;

	public MCConditionalTransformer(ProbModelChecker modelChecker, Prism prism)
	{
		super(modelChecker);
		this.prism = prism;
	}

	@Override
	public boolean canHandle(final Model model, ExpressionConditional expression) throws PrismLangException
	{
		if (!(model instanceof ProbModel)) {
			return false;
		}
		final ProbModel mc = (ProbModel) model;
		return canHandleCondition(mc, expression) && canHandleObjective(mc, expression);
	}
}
