package explicit;

import java.util.BitSet;

import parser.ast.Expression;

//FIXME ALG: add comment
public class BasicModelExpressionTransformation<OM extends Model, TM extends Model>
		extends BasicModelTransformation<OM, TM>
		implements ModelExpressionTransformation<OM, TM>
{
	private final Expression originalExpression;
	private final Expression transformedExpression;
	private final BitSet transformedStatesOfInterest;

	public BasicModelExpressionTransformation(final BasicModelTransformation<OM, TM> transformation, final Expression originalExpression,
			final Expression transformedExpression, final BitSet statesOfInterest)
	{
		super(transformation);
		this.originalExpression = originalExpression;
		this.transformedExpression = transformedExpression;
		this.transformedStatesOfInterest = mapToTransformedModel(statesOfInterest);
	}

	public BasicModelExpressionTransformation(final BasicModelExpressionTransformation<OM, TM> transformation)
	{
		super(transformation);
		this.originalExpression = transformation.originalExpression;
		this.transformedExpression = transformation.transformedExpression;
		this.transformedStatesOfInterest = transformation.transformedStatesOfInterest;
	}

	@Override
	public Expression getOriginalExpression()
	{
		return originalExpression;
	}

	@Override
	public Expression getTransformedExpression()
	{
		return transformedExpression;
	}

	@Override
	public BitSet getTransformedStatesOfInterest()
	{
		return transformedStatesOfInterest;
	}
}