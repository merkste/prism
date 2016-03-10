package explicit.conditional.transformer.mc;

import java.util.BitSet;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.PrismException;
import prism.PrismLangException;
import explicit.BasicModelExpressionTransformation;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.conditional.transformer.ConditionalTransformer;

public abstract class MCConditionalTransformer extends ConditionalTransformer.Basic<DTMC, DTMCModelChecker>
{
	public MCConditionalTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	public boolean canHandle(final Model model, ExpressionConditional expression) throws PrismLangException
	{
		if (!(model instanceof DTMC)) {
			return false;
		}
		final DTMC dtmc = (DTMC) model;
		return canHandleCondition(dtmc, expression) && canHandleObjective(dtmc, expression);
	}

	protected abstract boolean canHandleCondition(final DTMC model, final ExpressionConditional expression) throws PrismLangException;

	protected abstract boolean canHandleObjective(final DTMC model, final ExpressionConditional expression) throws PrismLangException;

	// FIXME ALG: add test canHandle for objective independent transformations

	@Override
	public BasicModelExpressionTransformation<DTMC, DTMC> transform(final DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		final ModelTransformation<DTMC, DTMC> transformation = transformModel(model, expression, statesOfInterest);
		final Expression transformedExpression = transformExpression(expression);

		return new BasicModelExpressionTransformation<DTMC, DTMC>(transformation, expression, transformedExpression, statesOfInterest);
	}

	protected Expression transformExpression(final ExpressionConditional expression)
	{
		return expression.getObjective();
	}

	protected abstract ModelTransformation<DTMC, DTMC> transformModel(final DTMC model, final ExpressionConditional expression,
			final BitSet statesOfInterest) throws PrismException;
}