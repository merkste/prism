package prism.conditional.scale;

import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.ModelExpressionTransformation;
import prism.ModelTransformation;
import prism.PrismException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StochModel;
import prism.StochModelChecker;
import prism.conditional.ConditionalTransformer;
import prism.conditional.transformer.BasicModelExpressionTransformation;

//FIXME ALG: add comment
public interface ScaleTransformer<M extends ProbModel, C extends ProbModelChecker> extends ConditionalTransformer.MC<M, C>
{
	@Override
	default ModelExpressionTransformation<M, ? extends M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		ModelTransformation<M, ? extends M> transformation = transformModel(model, expression, statesOfInterest);
		Expression transformedExpression = transformExpression(expression);
		return new BasicModelExpressionTransformation<>(transformation, expression, transformedExpression);
	}

	default Expression transformExpression(final ExpressionConditional expression)
		throws PrismException
	{
		return expression.getObjective();
	}

	public ModelTransformation<M, ? extends M> transformModel(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException;



	public interface CTMC extends ScaleTransformer<StochModel, StochModelChecker>, ConditionalTransformer.CTMC
	{
		
	}



	public interface DTMC extends ScaleTransformer<ProbModel, ProbModelChecker>, ConditionalTransformer.DTMC
	{
		
	}
}
