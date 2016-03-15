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
import explicit.ModelCheckerResult;
import explicit.ModelExpressionTransformation;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.StateValues;
import explicit.conditional.transformer.DtmcTransformerType;
import explicit.conditional.transformer.ConditionalTransformer;
import explicit.conditional.transformer.FinallyUntilTransformer;
import explicit.conditional.transformer.FinallyLtlTransformer;
import explicit.conditional.transformer.LtlLtlTransformer;
import explicit.conditional.transformer.LtlUntilTransformer;
import explicit.conditional.transformer.MdpTransformerType;
import explicit.conditional.transformer.mc.MCLTLTransformer;
import explicit.conditional.transformer.mc.MCNextTransformer;
import explicit.conditional.transformer.mc.MCQuotientTransformer;
import explicit.conditional.transformer.mc.MCUntilTransformer;
import explicit.conditional.transformer.mdp.ConditionalReachabilitiyTransformation;
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
		final ConditionalTransformer<DTMC> transformer = selectModelTransformer(model, expression);
		final ModelTransformation<DTMC, DTMC> transformation = transformModel(transformer, model, expression, statesOfInterest);
		final StateValues result = checkExpressionTransformedModel(transformation);
		return transformation.projectToOriginalModel(result);
	}

	public ModelTransformation<DTMC, DTMC> transformModel(final ConditionalTransformer<DTMC> transformer, final DTMC model,
			final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		String typeName = transformer.getClass().getSimpleName();
		String enclosingName = transformer.getClass().getEnclosingClass() == null ? "" : transformer.getClass().getEnclosingClass().getSimpleName() + "."; 
		mainLog.println("\nTransforming model (using " + enclosingName + typeName + ") for " + expression);
		long overallTime = System.currentTimeMillis();
		ModelTransformation<DTMC, DTMC> transformation = transformer.transform(model, expression, statesOfInterest);
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
				ModelTransformation<DTMC, DTMC> sparseTransformation = new BasicModelTransformation<>(transformation.getTransformedModel(), transformedModelSparse);
				sparseTransformation = new ModelTransformationNested<>(transformation, sparseTransformation);
				if (transformation instanceof ModelExpressionTransformation) {
					Expression originalExpression = ((ModelExpressionTransformation<?,?>) transformation).getOriginalExpression();
					Expression transformedExpression = ((ModelExpressionTransformation<?,?>) transformation).getTransformedExpression();
					sparseTransformation = new BasicModelExpressionTransformation<>(sparseTransformation, originalExpression, transformedExpression);
				} else if (transformation instanceof ConditionalReachabilitiyTransformation) {
					sparseTransformation = new ConditionalReachabilitiyTransformation<>(sparseTransformation, ((ConditionalReachabilitiyTransformation<?,?>) transformation).getGoalStates());
				}
				transformation = sparseTransformation;
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

	public ConditionalTransformer<DTMC> selectModelTransformer(final DTMC model, final ExpressionConditional expression) throws PrismException
	{
		ConditionalTransformer<DTMC> transformer;
		if (settings.getBoolean(PrismSettings.CONDITIONAL_DTMC_USE_MDP_TRANSFORMATIONS)) {
			final String specification = settings.getString(PrismSettings.CONDITIONAL_MDP);
			final SortedSet<MdpTransformerType> types = MdpTransformerType.getValuesOf(specification);
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
			final String specification = settings.getString(PrismSettings.CONDITIONAL_MC);
			final SortedSet<DtmcTransformerType> types = DtmcTransformerType.getValuesOf(specification);
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
		}

		throw new PrismException("Cannot model check " + expression);
	}

	public StateValues checkExpressionTransformedModel(final ModelTransformation<DTMC, DTMC> transformation) throws PrismException
	{
		final DTMC transformedModel = transformation.getTransformedModel();
		final BitSet transformedStatesOfInterest = transformation.getTransformedStatesOfInterest();
		mainLog.println("\nChecking property in transformed model ...");
		final StateValues sv;
		long timer = System.currentTimeMillis();
		if (transformation instanceof ModelExpressionTransformation) {
			Expression transformedExpression = ((ModelExpressionTransformation<?,?>) transformation).getTransformedExpression();
			sv = modelChecker.checkExpression(transformedModel, transformedExpression, transformedStatesOfInterest);
		} else if (transformation instanceof ConditionalReachabilitiyTransformation){
			BitSet goalStates = ((ConditionalReachabilitiyTransformation<?,?>) transformation).getGoalStates();
			ModelCheckerResult result = modelChecker.computeReachProbs(transformedModel, goalStates);
			sv = StateValues.createFromDoubleArray(result.soln, transformedModel);
		} else {
			throw new PrismException("Unsupported transformation for model checking");
		}
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking in transformed model: " + timer / 1000.0 + " seconds.");

		return sv;
	}
}