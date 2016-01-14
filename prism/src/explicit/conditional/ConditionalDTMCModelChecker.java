package explicit.conditional;

import java.util.BitSet;
import java.util.SortedSet;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.PrismException;
import prism.PrismSettings;
import explicit.BasicModelExpressionTransformation;
import explicit.BasicModelTransformation;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.DTMCSparse;
import explicit.ModelExpressionTransformation;
import explicit.ModelTransformation;
import explicit.StateValues;
import explicit.conditional.transformer.DtmcTransformerType;
import explicit.conditional.transformer.mc.MCConditionalTransformer;
import explicit.conditional.transformer.mc.MCLTLTransformer;
import explicit.conditional.transformer.mc.MCNextTransformer;
import explicit.conditional.transformer.mc.MCQuotientTransformer;
import explicit.conditional.transformer.mc.MCUntilTransformer;
import explicit.modelviews.DTMCView;

// FIXME ALG: add comment
public class ConditionalDTMCModelChecker extends ConditionalModelChecker<DTMC>
{
	protected DTMCModelChecker modelChecker;

	public ConditionalDTMCModelChecker(DTMCModelChecker mc)
	{
		super(mc);
		this.modelChecker = mc;
	}

	@Override
	public StateValues checkExpression(final DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		final MCConditionalTransformer transformer = selectModelTransformer(model, expression);
		final ModelExpressionTransformation<DTMC, DTMC> transformation = transformModel(transformer, model, expression, statesOfInterest);
		final StateValues result = checkExpressionTransformedModel(transformation);
		return transformation.projectToOriginalModel(result);
	}

	public BasicModelExpressionTransformation<DTMC, DTMC> transformModel(final MCConditionalTransformer transformer, final DTMC model,
			final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		mainLog.println("\nTransforming model (using " + transformer.getClass().getSimpleName() + ") for condition: " + expression.getCondition());
		long overallTime = System.currentTimeMillis();
		BasicModelExpressionTransformation<DTMC, DTMC> transformation = transformer.transform(model, expression, statesOfInterest);
		long transformationTime = System.currentTimeMillis() - overallTime;
		mainLog.println("Time for model transformation: " + transformationTime / 1000.0 + " seconds.");

		if (isVirtualModel(transformation.getTransformedModel())) {
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_VIRTUAL_MODELS)) {
				mainLog.println("Using virtual model");
			} else {
				mainLog.println("Converting virtual model to " + DTMCSparse.class.getSimpleName());
				long buildTime = System.currentTimeMillis();
				final DTMC transformedModel = new DTMCSparse(transformation.getTransformedModel());
				buildTime = System.currentTimeMillis() - buildTime;
				final ModelTransformation<DTMC, DTMC> simpleTransformation = new BasicModelTransformation<>(transformation.getOriginalModel(),
						transformedModel, transformation.getMapping());
				transformation = new BasicModelExpressionTransformation<>(simpleTransformation, transformation.getOriginalExpression(),
						transformation.getTransformedExpression(), statesOfInterest);
				mainLog.println("Time for converting: " + buildTime / 1000.0 + " seconds.");
			}
		}

		overallTime = System.currentTimeMillis() - overallTime;
		mainLog.println("Overall time for model transformation: " + overallTime / 1000.0 + " seconds.");
		mainLog.print("Transformed model has ");
		mainLog.println(transformation.getTransformedModel().infoString());
		return transformation;
	}

	public boolean isVirtualModel(final DTMC model)
	{
		return (model instanceof DTMCView) && ((DTMCView) model).isVirtual();
	}

	public MCConditionalTransformer selectModelTransformer(final DTMC model, final ExpressionConditional expression) throws PrismException
	{
		final String specification = settings.getString(PrismSettings.CONDITIONAL_MC);
		final SortedSet<DtmcTransformerType> types = DtmcTransformerType.getValuesOf(specification);

		MCConditionalTransformer transformer;
		if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_LEGACY_TRANSFORMATIONS)) {
			for (DtmcTransformerType type : types) {
				switch (type) {
				case Finally:
					transformer = new explicit.conditional.transformer.legacy.MCMatchingFinallyTransformer(modelChecker);
					break;
				case Until:
					transformer = new explicit.conditional.transformer.legacy.MCUntilTransformer(modelChecker);
					break;
				case Next:
					transformer = new explicit.conditional.transformer.legacy.MCNextTransformer(modelChecker);
					break;
				case Ltl:
					transformer = new explicit.conditional.transformer.legacy.MCLTLTransformer(modelChecker);
					break;
				default:
					continue;
				}
				if (transformer.canHandle(model, expression)) {
					return transformer;
				}
				;
			}
		} else {
			for (DtmcTransformerType type : types) {
				switch (type) {
				case Quotient:
					transformer = new MCQuotientTransformer(modelChecker);
					break;
				case Until:
					transformer = new MCUntilTransformer(modelChecker);
					break;
				case Next:
					transformer = new MCNextTransformer(modelChecker);
					break;
				case Ltl:
					transformer = new MCLTLTransformer(modelChecker);
					break;
				default:
					continue;
				}
				if (transformer.canHandle(model, expression)) {
					return transformer;
				}
				;
			}
		}

		throw new PrismException("Cannot model check " + expression);
	}

	public StateValues checkExpressionTransformedModel(final ModelExpressionTransformation<DTMC, DTMC> transformation) throws PrismException
	{
		final DTMC transformedModel = transformation.getTransformedModel();
		final Expression transformedExpression = transformation.getTransformedExpression();
		final BitSet transformedStatesOfInterest = transformation.getTransformedStatesOfInterest();

		mainLog.println("\nChecking property in transformed model ...");
		long timer = System.currentTimeMillis();
		final StateValues result = modelChecker.checkExpression(transformedModel, transformedExpression, transformedStatesOfInterest);
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for property checking in transformed model: " + timer / 1000.0 + " seconds.");

		return result;
	}
}