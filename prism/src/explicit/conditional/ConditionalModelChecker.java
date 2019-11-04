package explicit.conditional;

import java.util.BitSet;
import java.util.SortedSet;

import common.StopWatch;
import explicit.BasicModelTransformation;
import explicit.DTMCSimple;
import explicit.Model;
import explicit.ModelExpressionTransformation;
import explicit.StateModelChecker;
import explicit.StateValues;
import explicit.conditional.transformer.BasicModelExpressionTransformation;
import explicit.modelviews.ModelView;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismSettings;

//FIXME ALG: add comment
abstract public class ConditionalModelChecker<M extends Model, C extends StateModelChecker> extends PrismComponent
{
	protected C modelChecker;

	public ConditionalModelChecker(C modelChecker)
	{
		super(modelChecker);
		this.modelChecker = modelChecker;
	}

	abstract public StateValues checkExpression(final M model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException;

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

	public ModelExpressionTransformation<M, ? extends M> transformModel(final ConditionalTransformer<M, C> transformer, final M model,
			final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		long overallTime = System.currentTimeMillis();
		ModelExpressionTransformation<M, ? extends M> transformation = transformer.transform(model, expression, statesOfInterest);
		long transformationTime = System.currentTimeMillis() - overallTime;
		mainLog.println("\nTime for model transformation: " + transformationTime / 1000.0 + " seconds.");
	
		M transformedModel = transformation.getTransformedModel();
		if (isVirtualModel(transformedModel) || transformedModel instanceof DTMCSimple) {
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_VIRTUAL_MODELS)) {
				mainLog.println("Using simple/virtual model");
			} else {
				transformation = convertToSparseModel(transformation);
			}
		}
		overallTime = System.currentTimeMillis() - overallTime;
		mainLog.println("\nOverall time for model transformation: " + overallTime / 1000.0 + " seconds.");
		mainLog.print("Transformed model has ");
		mainLog.println(transformation.getTransformedModel().infoString());
		return transformation;
	}

	public StateValues checkExpressionTransformedModel(final ModelExpressionTransformation<M, ? extends M> transformation) throws PrismException
	{
		M transformedModel                 = transformation.getTransformedModel();
		Expression transformedExpression   = transformation.getTransformedExpression();
		BitSet transformedStatesOfInterest = transformation.getTransformedStatesOfInterest();
	
		mainLog.println("\nChecking transformed property in transformed model: " + transformedExpression);
		long timer     = System.currentTimeMillis();
		StateValues sv = modelChecker.checkExpression(transformedModel, transformedExpression, transformedStatesOfInterest);
		timer          = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking in transformed model: " + timer / 1000.0 + " seconds.");
	
		return sv;
	}

	public StateValues createUndefinedStateValues(M model, ExpressionConditional expression) throws PrismException
	{
		Object value = null;
		Type type = expression.getType();
		if (type instanceof TypeBool) {
			value = BasicModelTransformation.DEFAULT_BOOLEAN;
		} else if (type instanceof TypeDouble) {
			value = BasicModelTransformation.DEFAULT_DOUBLE;
		} else if (type instanceof TypeInt) {
			value = BasicModelTransformation.DEFAULT_INTEGER;
		} else {
			throw new PrismException("Unexpected result type of conditional expression: " + type);
		}
		return new StateValues(type, value, model);
	}

	public boolean isVirtualModel(M model)
	{
		return (model instanceof ModelView) && ((ModelView) model).isVirtual();
	}

	protected ModelExpressionTransformation<M, ? extends M> convertToSparseModel(ModelExpressionTransformation<M, ? extends M> transformation)
	{
		M transformedModel = transformation.getTransformedModel();
		mainLog.println();
		StopWatch watch = new StopWatch(mainLog).start("Converting simple/virtual to sparse model");
		M transformedModelSparse = convertToSparseModel(transformedModel);
		watch.stop();
		// build transformation
		BasicModelTransformation<M, M> sparseTransformation = new BasicModelTransformation<>(transformedModel, transformedModelSparse, transformation.getTransformedStatesOfInterest());
		sparseTransformation = sparseTransformation.compose(transformation);
		// attach transformed expression
		Expression originalExpression    = transformation.getOriginalExpression();
		Expression transformedExpression = transformation.getTransformedExpression();
		return new BasicModelExpressionTransformation<>(sparseTransformation, originalExpression, transformedExpression);
	}

	protected abstract M convertToSparseModel(M model);

	protected abstract String getConditionalPatterns();

	protected abstract ConditionalTransformer<M, C> getTransformer(ConditionalTransformerType type);

}