package explicit;

import parser.ast.Expression;

//FIXME ALG: add comment
public class BasicModelExpressionTransformation<OM extends Model, TM extends Model>
		extends BasicModelTransformation<OM, TM>
		implements ModelExpressionTransformation<OM, TM>
{
	protected final Expression originalExpression;
	protected final Expression transformedExpression;

	public BasicModelExpressionTransformation(final ModelTransformation<? extends OM, ? extends TM> transformation, final Expression originalExpression, final Expression transformedExpression)
	{
		super(transformation);
		this.originalExpression    = originalExpression;
		this.transformedExpression = transformedExpression;
	}

	public BasicModelExpressionTransformation(final ModelExpressionTransformation<? extends OM, ? extends TM> transformation)
	{
		this(transformation, transformation.getOriginalExpression(), transformation.getTransformedExpression());
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
}
