package prism.conditional;

import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.ModelChecker;
import prism.ModelTransformation;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.ProbModel;
import prism.ProbModelChecker;
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
		if (transformer == null) {
			// try quotient as last resort
			Expression quotientExpression = transformForQuotient(expression);
			if (quotientExpression != null) {
				prism.getLog().println("Checking quotient expression: "+quotientExpression);
				return mc.checkExpression(quotientExpression, statesOfInterest);
			} else {
				throw new PrismNotSupportedException("Cannot model check conditional expression " + expression + " (with the current settings)");
			}
		}

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

		return null;
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
	
	private Expression transformForQuotient(ExpressionConditional expr) throws PrismException {
		if (!(expr.getObjective() instanceof ExpressionProb)) {
			throw new PrismNotSupportedException("Can not transform expression with quotient method: "+expr);
		}

		ExpressionProb top = (ExpressionProb) expr.getObjective().deepCopy();
		Expression bound = top.getBound();
		RelOp relOp = top.getRelOp();
		if (top.getBound() != null) {
			// P bowtie bound
			top.setBound(null);
			top.setRelOp(RelOp.EQ);
		}
		ExpressionProb bottom = (ExpressionProb) top.deepCopy();
		bottom.setExpression(expr.getCondition().deepCopy());
		top.setExpression(Expression.And(Expression.Parenth(top.getExpression()),
		                                 Expression.Parenth(bottom.getExpression().deepCopy())));
		Expression result = new ExpressionBinaryOp(ExpressionBinaryOp.DIVIDE,
		                                           top, bottom);
		if (bound != null) {
			int op = -1;
			switch (relOp) {
			case GEQ:
				op = ExpressionBinaryOp.GE;
				break;
			case GT:
				op = ExpressionBinaryOp.GT;
				break;
			case LEQ:
				op = ExpressionBinaryOp.LE;
				break;
			case LT:
				op = ExpressionBinaryOp.LT;
				break;
			default:
				throw new PrismNotSupportedException("Unsupported comparison operator in "+expr);
			}
			result = new ExpressionBinaryOp(op, Expression.Parenth(result), bound.deepCopy());
		}

		return result;
	}
}
