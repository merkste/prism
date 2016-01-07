package explicit.conditional;

import java.util.BitSet;

import common.iterable.Support;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import explicit.BasicModelExpressionTransformation;
import explicit.DTMC;
import explicit.ModelTransformation;

//FIXME ALG: add comment
public class ConditionalQuotientTransformation extends BasicModelExpressionTransformation<DTMC, DTMC>
{
	private final double[] probabilities;

	public ConditionalQuotientTransformation(final ModelTransformation<DTMC, DTMC> transformation, final ExpressionConditional originalExpression,
			final Expression transformedExpression, final BitSet statesOfInterest, final double[] probabilities)
	{
		super(transformation, originalExpression, transformedExpression, statesOfInterest);

		this.probabilities = probabilities;
	}

	@Override
	public BitSet getTransformedStatesOfInterest()
	{
		final BitSet result = new Support(probabilities).asBitSet();
		result.and(super.getTransformedStatesOfInterest());
		return result;
	}

	@Override
	public BitSet projectToOriginalModel(final BitSet values)
	{
		throw new UnsupportedOperationException("only probabilites supported");
	}

	@Override
	public double[] projectToOriginalModel(final double[] values)
	{
		final double[] result = new double[values.length];

		for (int i = 0; i < values.length; i++) {
			final double probability = probabilities[i];
			result[i] = probability == 0 ? DEFAULT_DOUBLE : values[i] / probability;
		}

		return result;
	}

	@Override
	public int[] projectToOriginalModel(final int[] values)
	{
		throw new UnsupportedOperationException("only probabilites supported");
	}
}