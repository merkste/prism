package explicit.conditional.scale;

import java.util.BitSet;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.PrismException;
import explicit.BasicModelTransformation;
import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.ModelTransformation;
import explicit.ProbModelChecker;
import explicit.conditional.ConditionalTransformer;
import explicit.conditional.transformer.BasicModelExpressionTransformation;
import explicit.modelviews.CTMCAlteredDistributions;
import explicit.modelviews.CTMCRestricted;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.Restriction;

//FIXME ALG: add comment
public interface ScaleTransformer<M extends explicit.DTMC, C extends ProbModelChecker> extends ConditionalTransformer.MC<M, C>
{
	@Override
	default BasicModelExpressionTransformation<M, ? extends M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		ModelTransformation<M, ? extends M> transformation = transformModel(model, expression, statesOfInterest);
		Expression transformedExpression = transformExpression(expression);
		return new BasicModelExpressionTransformation<M, M>(transformation, expression, transformedExpression);
	}

	default Expression transformExpression(final ExpressionConditional expression)
		throws PrismException
	{
		return expression.getObjective();
	}

	ModelTransformation<M, ? extends M> transformModel(final M model, final ExpressionConditional expression,
			final BitSet statesOfInterest) throws PrismException;

	BasicModelTransformation<M, ? extends M> pivot(M model, BitSet pivotStates);

	BasicModelTransformation<M, ? extends M> restrict(BasicModelTransformation<M, ? extends M> scaled, BitSet restrict);

	BasicModelTransformation<M, ? extends M> scale(M model, double[] probs);

	BasicModelTransformation<M, ? extends M> scale(M model, double[] originProbs, double[] targetProbs);



	public interface CTMC extends ScaleTransformer<explicit.CTMC, CTMCModelChecker>, ConditionalTransformer.CTMC
	{
		@Override
		default BasicModelTransformation<explicit.CTMC, CTMCAlteredDistributions> pivot(explicit.CTMC model, BitSet pivotStates)
		{
			return MCPivotTransformation.transform(model, pivotStates);
		}

		@Override
		default BasicModelTransformation<explicit.CTMC, CTMCRestricted> restrict(BasicModelTransformation<explicit.CTMC, ? extends explicit.CTMC> scaled, BitSet restrict)
		{
			return CTMCRestricted.transform(scaled.getTransformedModel(), restrict, Restriction.TRANSITIVE_CLOSURE_SAFE);
		}

		@Override
		default BasicModelTransformation<explicit.CTMC, CTMCAlteredDistributions> scale(explicit.CTMC model, double[] probs)
		{
			return MCScaledTransformation.transform(model, probs);
		}

		@Override
		default BasicModelTransformation<explicit.CTMC, CTMCAlteredDistributions> scale(explicit.CTMC model, double[] originProbs, double[] targetProbs)
		{
			return MCScaledTransformation.transform(model, originProbs, targetProbs);
		}
	}



	public interface DTMC extends ScaleTransformer<explicit.DTMC, DTMCModelChecker>, ConditionalTransformer.DTMC
	{
		@Override
		default BasicModelTransformation<explicit.DTMC, DTMCAlteredDistributions> pivot(explicit.DTMC model, BitSet pivotStates)
		{
			return MCPivotTransformation.transform(model, pivotStates);
		}

		@Override
		default BasicModelTransformation<explicit.DTMC, DTMCRestricted> restrict(BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> scaled, BitSet restrict)
		{
			return DTMCRestricted.transform(scaled.getTransformedModel(), restrict, Restriction.TRANSITIVE_CLOSURE_SAFE);
		}

		@Override
		default BasicModelTransformation<explicit.DTMC, DTMCAlteredDistributions> scale(explicit.DTMC model, double[] probs)
		{
			return MCScaledTransformation.transform(model, probs);
		}

		@Override
		default BasicModelTransformation<explicit.DTMC, DTMCAlteredDistributions> scale(explicit.DTMC model, double[] originProbs, double[] targetProbs)
		{
			return MCScaledTransformation.transform(model, originProbs, targetProbs);
		}
	}
}
