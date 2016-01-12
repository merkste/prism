package explicit.conditional.transformer.mc;

import java.util.BitSet;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;
import explicit.BasicModelTransformation;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker;
import explicit.ModelTransformation;

// FIXME ALG: add comment
public class MCQuotientTransformer extends MCConditionalTransformer
{
	public MCQuotientTransformer(final DTMCModelChecker mc)
	{
		super(mc);
	}

	@Override
	protected boolean canHandleCondition(final DTMC model, final ExpressionConditional expression) throws PrismLangException
	{
		// arbitrary path formulae
		return LTLModelChecker.isSupportedLTLFormula(ModelType.DTMC, expression.getCondition());
	}

	@Override
	protected boolean canHandleObjective(final DTMC model, final ExpressionConditional expression)
	{
		// only prob formulae without bounds
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		return ((ExpressionProb) expression.getObjective()).getProb() == null;
	}

	@Override
	public ConditionalQuotientTransformation transform(final DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		// FIXME ALG: cleanup
		final double[] probabilities = computeProbability(model, expression.getCondition());

		final Integer[] mapping = new Integer[model.getNumStates()];
		for (int state = 0; state < mapping.length; state++) {
			mapping[state] = probabilities[state] > 0 ? state : null;
		}

		final Expression transformedExpression = transformExpression(expression);
		final BasicModelTransformation<DTMC, DTMC> transformation = new BasicModelTransformation<DTMC, DTMC>(model, model, mapping);
		return new ConditionalQuotientTransformation(transformation, expression, transformedExpression, statesOfInterest, probabilities);
	}

	@Override
	protected ModelTransformation<DTMC, DTMC> transformModel(final DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		// FIXME ALG: cleanup
		return null;
	}

	protected ExpressionProb transformExpression(final ExpressionConditional expression)
	{
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final Expression condition = expression.getCondition();

		return new ExpressionProb(Expression.And(objective.getExpression(), condition), "=", null);
	}

	@Override
	protected double[] computeProbability(final DTMC model, final Expression pathFormula) throws PrismException
	{
		final ExpressionProb expression = new ExpressionProb(pathFormula, "=", null);

		return modelChecker.checkExpression(model, expression, null).getDoubleArray();
	}
}