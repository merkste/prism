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
import explicit.StateValues;
import explicit.conditional.prototype.virtual.FinallyLtlTransformer;
import explicit.conditional.prototype.virtual.FinallyUntilTransformer;
import explicit.conditional.prototype.virtual.LtlLtlTransformer;
import explicit.conditional.prototype.virtual.LtlUntilTransformer;
import explicit.conditional.prototype.virtual.MCLTLTransformer;
import explicit.conditional.prototype.virtual.MCNextTransformer;
import explicit.conditional.prototype.virtual.MCUntilTransformer;
import explicit.conditional.transformer.DtmcTransformerType;
import explicit.conditional.transformer.MdpTransformerType;
import explicit.conditional.transformer.mc.MCQuotientTransformer;
import explicit.conditional.transformer.mc.NewMcLtlTransformer;
import explicit.conditional.transformer.mc.NewMcNextTransformer;
import explicit.conditional.transformer.mc.NewMcUntilTransformer;
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
		final NewConditionalTransformer<DTMC, DTMCModelChecker> transformer = selectModelTransformer(model, expression);
		final ModelExpressionTransformation<DTMC, ? extends DTMC> transformation = transformModel(transformer, model, expression, statesOfInterest);

		final StateValues result = checkExpressionTransformedModel(transformation);
		return transformation.projectToOriginalModel(result);
	}

	public ModelExpressionTransformation<DTMC, ? extends DTMC> transformModel(final NewConditionalTransformer<DTMC, DTMCModelChecker> transformer, final DTMC model,
			final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		mainLog.println("\nTransforming model (using " + transformer.getName() + ") for condition: " + expression);
		long overallTime = System.currentTimeMillis();
		ModelExpressionTransformation<DTMC, ? extends DTMC> transformation = transformer.transform(model, expression, statesOfInterest);
		long transformationTime = System.currentTimeMillis() - overallTime;
		mainLog.println("\nTime for model transformation: " + transformationTime / 1000.0 + " seconds.");

		DTMC transformedModel = transformation.getTransformedModel();
		if (isVirtualModel(transformedModel)) {
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_VIRTUAL_MODELS)) {
				mainLog.println("Using virtual model");
			} else {
				mainLog.println("\nConverting virtual model to " + DTMCSparse.class.getSimpleName());
				long buildTime = System.currentTimeMillis();
				DTMCSparse transformedModelSparse = new DTMCSparse(transformedModel);
				buildTime = System.currentTimeMillis() - buildTime;
				mainLog.println("Time for converting: " + buildTime / 1000.0 + " seconds.");
				// build in transformation
				BasicModelTransformation<DTMC, DTMC> sparseTransformation = new BasicModelTransformation<>(transformation.getTransformedModel(), transformedModelSparse);
				sparseTransformation = sparseTransformation.compose(transformation);
				// attach transformed expression
				Expression originalExpression    = transformation.getOriginalExpression();
				Expression transformedExpression = transformation.getTransformedExpression();
				transformation = new BasicModelExpressionTransformation<>(sparseTransformation, originalExpression, transformedExpression);
			}
		}
		overallTime = System.currentTimeMillis() - overallTime;
		mainLog.println("\nOverall time for model transformation: " + overallTime / 1000.0 + " seconds.");
		mainLog.print("Transformed model has ");
		mainLog.println(transformation.getTransformedModel().infoString());
		return transformation;
	}

	public boolean isVirtualModel(final DTMC model)
	{
		return (model instanceof DTMCView) && ((DTMCView) model).isVirtual();
	}

	public NewConditionalTransformer<DTMC, DTMCModelChecker> selectModelTransformer(final DTMC model, final ExpressionConditional expression) throws PrismException
	{
		NewConditionalTransformer<DTMC, DTMCModelChecker> transformer;
		if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_RESET_FOR_MC)) {
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_TACAS14_PROTOTYPE)) {
				throw new PrismException("There is no TACAS'14 prototype for the reset method in MCs");
			}
			final String specification                = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_RESET);
			final SortedSet<MdpTransformerType> types = MdpTransformerType.getValuesOf(specification);
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_PROTOTYPE)) {
				for (MdpTransformerType type : types) {
					switch (type) {
					case FinallyFinally:
						transformer = new FinallyUntilTransformer.DTMC(modelChecker);
						break;
					case LtlFinally:
						transformer = new LtlUntilTransformer.DTMC(modelChecker);
						break;
					case FinallyLtl:
						transformer = new FinallyLtlTransformer.DTMC(modelChecker);
						break;
					case LtlLtl:
						transformer = new LtlLtlTransformer.DTMC(modelChecker);
						break;
					default:
						continue;
					}
					if (transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			} else {
				for (MdpTransformerType type : types) {
					switch (type) {
					case FinallyFinally:
						transformer = new NewFinallyUntilTransformer.DTMC(modelChecker);
						break;
					case LtlFinally:
						transformer = new NewLtlUntilTransformer.DTMC(modelChecker);
						break;
					case FinallyLtl:
						transformer = new NewFinallyLtlTransformer.DTMC(modelChecker);
						break;
					case LtlLtl:
						transformer = new NewLtlLtlTransformer.DTMC(modelChecker);
						break;
					default:
						continue;
					}
					if (transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			}
		} else {
			final String specification                 = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_SCALE);
			final SortedSet<DtmcTransformerType> types = DtmcTransformerType.getValuesOf(specification);
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_TACAS14_PROTOTYPE)) {
				for (DtmcTransformerType type : types) {
					switch (type) {
					case Finally:
						transformer = new explicit.conditional.prototype.tacas14.MCMatchingFinallyTransformer(modelChecker);
						break;
					case Until:
						transformer = new explicit.conditional.prototype.tacas14.MCUntilTransformer(modelChecker);
						break;
					case Next:
						transformer = new explicit.conditional.prototype.tacas14.MCNextTransformer(modelChecker);
						break;
					case Ltl:
						transformer = new explicit.conditional.prototype.tacas14.MCLTLTransformer(modelChecker);
						break;
					default:
						continue;
					}
					if (transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			} else if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_PROTOTYPE)) {
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
				}
			} else {
				for (DtmcTransformerType type : types) {
					switch (type) {
					case Quotient:
						transformer = new MCQuotientTransformer(modelChecker);
						break;
					case Until:
						transformer = new NewMcUntilTransformer(modelChecker);
						break;
					case Next:
						transformer = new NewMcNextTransformer(modelChecker);
						break;
					case Ltl:
						transformer = new NewMcLtlTransformer(modelChecker);
						break;
					default:
						continue;
					}
					if (transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			}
		}

		throw new PrismException("Cannot model check " + expression);
	}

	public StateValues checkExpressionTransformedModel(final ModelExpressionTransformation<DTMC, ? extends DTMC> transformation) throws PrismException
	{
		DTMC transformedModel              = transformation.getTransformedModel();
		Expression transformedExpression   = transformation.getTransformedExpression();
		BitSet transformedStatesOfInterest = transformation.getTransformedStatesOfInterest();

		mainLog.println("\nChecking property in transformed model ...");
		long timer     = System.currentTimeMillis();
		StateValues sv = modelChecker.checkExpression(transformedModel, transformedExpression, transformedStatesOfInterest);
		timer          = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking in transformed model: " + timer / 1000.0 + " seconds.");

		return sv;
	}
}