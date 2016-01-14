package explicit.conditional.transformer.mc;

import java.util.BitSet;

import explicit.BasicModelExpressionTransformation;
import explicit.DTMC;
import explicit.ModelExpressionTransformation;

//FIXME ALG: add comment
public class ConditionalQuotientTransformation extends BasicModelExpressionTransformation<DTMC, DTMC>
{
	private final double[] probabilities;

	public ConditionalQuotientTransformation(final ModelExpressionTransformation<DTMC, DTMC> transformation, final double[] probabilities)
	{
		super(transformation);
		this.probabilities = probabilities;
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
			result[i] = probability > 0 ? values[i] / probability : DEFAULT_DOUBLE ;
		}

		return result;
	}

	@Override
	public int[] projectToOriginalModel(final int[] values)
	{
		throw new UnsupportedOperationException("only probabilites supported");
	}
}