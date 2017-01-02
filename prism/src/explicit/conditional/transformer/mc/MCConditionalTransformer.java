package explicit.conditional.transformer.mc;

import java.util.BitSet;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.PrismException;
import explicit.BasicModelExpressionTransformation;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.conditional.NewConditionalTransformer;

public abstract class MCConditionalTransformer extends NewConditionalTransformer.Basic<DTMC, DTMCModelChecker>
{
	public MCConditionalTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	public boolean canHandleModelType(Model model)
	{
		return model instanceof explicit.DTMC;
	}

	@Override
	public BasicModelExpressionTransformation<explicit.DTMC, ? extends explicit.DTMC> transform(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		final ModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformation = transformModel(model, expression, statesOfInterest);
		final Expression transformedExpression = transformExpression(expression);
		return new BasicModelExpressionTransformation<explicit.DTMC, explicit.DTMC>(transformation, expression, transformedExpression);
	}

	protected Expression transformExpression(final ExpressionConditional expression)
	{
		return expression.getObjective();
	}

	protected abstract ModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformModel(final explicit.DTMC model, final ExpressionConditional expression,
			final BitSet statesOfInterest) throws PrismException;
}
