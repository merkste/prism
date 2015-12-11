package prism.conditional;

import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.ModelChecker;
import prism.ModelTransformation;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismSettings;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.StateValues;

public class ConditionalDTMCModelChecker extends ConditionalModelChecker<ProbModel> {
	
	protected ProbModelChecker mc;

	public ConditionalDTMCModelChecker(ProbModelChecker mc, Prism prism) {
		super(prism);
		this.mc = mc;
	}

	@Override
	public StateValues checkExpression(final ProbModel model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException {
		final ConditionalTransformer<ProbModelChecker, ProbModel> transformer = selectModelTransformer(model, expression);
		final ModelTransformation<ProbModel, ProbModel> transformation = transformModel(transformer, model, expression, statesOfInterest);
		final StateValues resultTransformed = checkExpressionTransformedModel(transformation, expression);

		final StateValues resultOriginal = transformation.projectToOriginalModel(resultTransformed);
		transformation.clear();

		return resultOriginal;
	}

	private ModelTransformation<ProbModel, ProbModel> transformModel(final ConditionalTransformer<ProbModelChecker, ProbModel> transformer, final ProbModel model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException {
		// Debug output
		// ModelPrinter.exportToDotFile(model, "../conditional/conditional_mc_original.dot", target);
		prism.getLog().println("\nTransforming model (using " + transformer.getClass().getSimpleName() + ") for condition: " + expression.getCondition());
		long timer = System.currentTimeMillis();
		final ModelTransformation<ProbModel, ProbModel> transformation = transformer.transform(model, expression, statesOfInterest);
		timer = System.currentTimeMillis() - timer;
		prism.getLog().println("Time for model transformation: " + timer / 1000.0 + " seconds.");
		prism.getLog().print("Transformed model has\n");
		transformation.getTransformedModel().printTransInfo(prism.getLog());
		// Debug output
		// ModelPrinter.exportToDotFile(this, "../conditional/conditional_mc_transformed.dot", untilTransforamtion.mapStates(target));
		return transformation;
	}

	private ConditionalTransformer<ProbModelChecker, ProbModel> selectModelTransformer(final ProbModel model, final ExpressionConditional expression) throws PrismException {
		boolean forceLtlCondition = prism.getSettings().getBoolean(PrismSettings.PRISM_ALL_CONDITIONS_VIA_LTL);
		/*
 			if(!forceLtlCondition) {

			if (new MCFinallyTransformer(mc).canHandle(model, expression)) {
				return new MCFinallyTransformer(mc);
			}
		}

*/
		
		if(new MCLTLTransformer(mc, prism).canHandle(model, expression)) {
			return new MCLTLTransformer(mc, prism);
		}

		throw new PrismException("Cannot model check " + expression);
	}

	private StateValues checkExpressionTransformedModel(final ModelTransformation<ProbModel, ProbModel> transformation, final ExpressionConditional expression) throws PrismException {
		final ProbModel transformedModel = transformation.getTransformedModel();
		final Expression transformedExpression = expression.getObjective();

		prism.getLog().println("\nChecking property in transformed model ...");
		long timer = System.currentTimeMillis();

		ModelChecker mcTransformed = mc.createModelChecker(transformedModel);
		
		final StateValues result = mcTransformed.checkExpression(transformedExpression, JDD.Constant(1));
		timer = System.currentTimeMillis() - timer;
		prism.getLog().println("Time for property checking in transformed model: " + timer / 1000.0 + " seconds.");

		return result;
	}
}
