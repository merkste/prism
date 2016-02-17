package prism;

import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;

public class ModelExpressionTransformationIdentity<M extends Model> implements ModelExpressionTransformation<M,M> {
	private M model;
	private Expression original;
	private Expression transformed;
	private JDDNode statesOfInterest;

	public ModelExpressionTransformationIdentity(M model, Expression original, Expression transformed, JDDNode statesOfInterest)
	{
		this.model = model;
		this.original = original;
		this.transformed = transformed;
		this.statesOfInterest = statesOfInterest;
	}
	
	@Override
	public M getOriginalModel() {return model;}

	@Override
	public M getTransformedModel() {return model;}

	@Override
	public StateValues projectToOriginalModel(StateValues svTransformedModel)
	{
		return svTransformedModel; 
	}

	@Override
	public Expression getTransformedExpression() {return transformed;}

	@Override
	public Expression getOriginalExpression() {return original;}

	@Override
	public JDDNode getTransformedStatesOfInterest() {return statesOfInterest.copy();}

	@Override
	public void clear()
	{
		// we don't clear the model
		JDD.Deref(statesOfInterest);
	}

}
