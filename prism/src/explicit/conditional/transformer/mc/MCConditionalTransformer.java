package explicit.conditional.transformer.mc;

import java.util.BitSet;
import java.util.PrimitiveIterator.OfInt;

import common.BitSetTools;
import common.iterable.IterableBitSet;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.PrismException;
import explicit.BasicModelExpressionTransformation;
import explicit.DTMCModelChecker;
import explicit.ModelTransformation;
import explicit.conditional.NewConditionalTransformer;

public abstract class MCConditionalTransformer extends NewConditionalTransformer.DTMC
{
	public MCConditionalTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
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

	// FIXME ALG: Here we actually reuse prob0/1 precomputation
	public double[] computeUntilProbs(final explicit.DTMC model, final boolean negated, final BitSet remain, final BitSet goal, final BitSet prob0, final BitSet prob1)
			throws PrismException
	{
		double[] init = new double[model.getNumStates()]; // initialized with 0.0's
		BitSet setToOne = negated ? prob0 : prob1;
		for (OfInt iter = new IterableBitSet(setToOne).iterator(); iter.hasNext();) {
			init[iter.nextInt()] = 1.0;
		}
		BitSet known = BitSetTools.union(prob0, prob1);
		double[] probabilities = getModelChecker(model).computeReachProbs(model, remain, goal, init, known).soln;
		return negated ? subtractFromOne(probabilities) : probabilities;
	}
}
