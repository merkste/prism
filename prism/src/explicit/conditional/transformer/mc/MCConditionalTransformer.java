package explicit.conditional.transformer.mc;

import java.util.BitSet;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.PrismException;
import explicit.BasicModelExpressionTransformation;
import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.ModelTransformation;
import explicit.ProbModelChecker;
import explicit.conditional.NewConditionalTransformer;

public interface MCConditionalTransformer<M extends explicit.DTMC, C extends ProbModelChecker> extends NewConditionalTransformer.MC<M,C>
{
	@Override
	default BasicModelExpressionTransformation<M, ? extends M> transform(final M model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		final ModelTransformation<M, ? extends M> transformation = transformModel(model, expression, statesOfInterest);
		final Expression transformedExpression = transformExpression(expression);
		return new BasicModelExpressionTransformation<M, M>(transformation, expression, transformedExpression);
	}

	default Expression transformExpression(final ExpressionConditional expression)
		throws PrismException
	{
		return expression.getObjective();
	}

	ModelTransformation<M, ? extends M> transformModel(final M model, final ExpressionConditional expression,
			final BitSet statesOfInterest) throws PrismException;



	public interface CTMC extends MCConditionalTransformer<explicit.CTMC, CTMCModelChecker>, NewConditionalTransformer.CTMC
	{
		// Marker interface to ease inheritance
	}



	public interface DTMC extends MCConditionalTransformer<explicit.DTMC, DTMCModelChecker>, NewConditionalTransformer.DTMC
	{
		// Marker interface to ease inheritance
	}
}
