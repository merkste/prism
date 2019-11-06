package prism.conditional;

import java.util.SortedSet;

import explicit.BasicModelTransformation;
import explicit.conditional.ConditionalTransformerType;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.ModelChecker;
import prism.ModelExpressionTransformation;
import prism.PrismComponent;
import prism.PrismException;
import prism.ProbModel;
import prism.StateModelChecker;
import prism.StateValues;
import prism.StateValuesMTBDD;

//FIXME ALG: add comment
abstract public class ConditionalModelChecker<M extends ProbModel, C extends StateModelChecker> extends PrismComponent
{
	protected C modelChecker;

	public ConditionalModelChecker(C modelChecker) {
		this.modelChecker = modelChecker;
	}

	abstract public StateValues checkExpression(final M model, final ExpressionConditional expression, final JDDNode statesOfInterest) throws PrismException;

	public ConditionalTransformer<M, C> selectModelTransformer(final M model, final ExpressionConditional expression) throws PrismException
	{
		SortedSet<ConditionalTransformerType> types =  ConditionalTransformerType.getValuesOf(getConditionalPatterns());
		ConditionalTransformer<M, C> transformer;
		for (ConditionalTransformerType type : types) {
			transformer = getTransformer(type);
			if (transformer != null && transformer.canHandle(model, expression)) {
				return transformer;
			}
		}
		return null;
	}

	public ModelExpressionTransformation<M, ? extends M> transformModel(final ConditionalTransformer<M, C> transformer, final M model, final ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		long timer = System.currentTimeMillis();
		ModelExpressionTransformation<M, ? extends M> transformation = transformer.transform(model, expression, statesOfInterest);
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model transformation: " + timer / 1000.0 + " seconds.");
		mainLog.println("\nOverall time for model transformation: " + timer / 1000.0 + " seconds.");
		return transformation;
	}

	protected StateValues checkExpressionTransformedModel(final ModelExpressionTransformation<M, ? extends M> transformation) throws PrismException
	{
		M transformedModel                  = transformation.getTransformedModel();
		Expression transformedExpression    = transformation.getTransformedExpression();
		JDDNode transformedStatesOfInterest = transformation.getTransformedStatesOfInterest();
		mainLog.println("\nChecking transformed property in transformed model: " + transformedExpression);

		long timer = System.currentTimeMillis();
		ModelChecker mc    = modelChecker.createModelChecker(transformedModel);
		StateValues result = mc.checkExpression(transformedExpression, transformedStatesOfInterest);
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking in transformed model: " + timer / 1000.0 + " seconds.");

		return result;
	}

	public StateValues createUndefinedStateValues(M model, ExpressionConditional expression) throws PrismException
	{
		JDDNode value = null;
		Type type = expression.getType();
		if (type instanceof TypeBool) {
			value = JDD.Constant(BasicModelTransformation.DEFAULT_BOOLEAN ? 1 : 0);
		} else if (type instanceof TypeDouble) {
			value = JDD.Constant(BasicModelTransformation.DEFAULT_DOUBLE);
		} else if (type instanceof TypeInt) {
			value = JDD.Constant(BasicModelTransformation.DEFAULT_INTEGER);
		} else {
			throw new PrismException("Unexpected result type of conditional expression: " + type);
		}
		value = JDD.Times(value, model.getReach().copy());
		return new StateValuesMTBDD(value, model);
	}

	protected abstract String getConditionalPatterns();

	protected abstract ConditionalTransformer<M, C> getTransformer(ConditionalTransformerType type);

}
