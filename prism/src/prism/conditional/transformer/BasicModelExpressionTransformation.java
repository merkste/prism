package prism.conditional.transformer;

import jdd.JDDNode;
import parser.ast.Expression;
import prism.Model;
import prism.ModelExpressionTransformation;
import prism.ModelTransformation;
import prism.PrismException;
import prism.StateValues;

//FIXME ALG: add comment
public class BasicModelExpressionTransformation<OM extends Model, TM extends Model> implements ModelExpressionTransformation<OM, TM>
{
	protected final ModelTransformation<? extends OM, ? extends TM> transformation;
	protected final Expression originalExpression;
	protected final Expression transformedExpression;

	public BasicModelExpressionTransformation(ModelTransformation<? extends OM, ? extends TM> transformation, Expression originalExpression, Expression transformedExpression)
	{
		this.transformation        = transformation;
		this.originalExpression    = originalExpression;
		this.transformedExpression = transformedExpression;
	}

	public BasicModelExpressionTransformation(ModelExpressionTransformation<? extends OM, ? extends TM> transformation)
	{
		this(transformation, transformation.getOriginalExpression(), transformation.getTransformedExpression());
	}

	@Override
	public OM getOriginalModel()
	{
		return transformation.getOriginalModel();
	}

	@Override
	public TM getTransformedModel()
	{
		return transformation.getTransformedModel();
	}

	@Override
	public void clear()
	{
		transformation.clear();
	}

	@Override
	public StateValues projectToOriginalModel(StateValues svTransformedModel) throws PrismException
	{
		return transformation.projectToOriginalModel(svTransformedModel);
	}

	@Override
	public JDDNode getTransformedStatesOfInterest()
	{
		return transformation.getTransformedStatesOfInterest();
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
