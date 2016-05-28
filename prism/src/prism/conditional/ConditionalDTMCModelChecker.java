package prism.conditional;

import java.util.SortedSet;

import explicit.conditional.transformer.DtmcTransformerType;
import explicit.conditional.transformer.MdpTransformerType;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.ModelChecker;
import prism.ModelExpressionTransformation;
import prism.ModelTransformation;
import prism.Prism;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;

public class ConditionalDTMCModelChecker extends ConditionalModelChecker<ProbModel> {
	
	protected ProbModelChecker modelChecker;

	public ConditionalDTMCModelChecker(ProbModelChecker mc, Prism prism)
	{
		super(prism);
		this.modelChecker = mc;
	}

	@Override
	public StateValues checkExpression(final ProbModel model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		final NewConditionalTransformer.DTMC transformer = selectModelTransformer(model, expression);
		if (transformer == null) {
			// try quotient as last resort
			try {
				Expression quotientExpression = transformForQuotient(expression);
				if (quotientExpression != null) {
					prism.getLog().println("Checking quotient expression: "+quotientExpression);
					return modelChecker.checkExpression(quotientExpression, statesOfInterest.copy());
				} else {
					throw new PrismNotSupportedException("Cannot model check conditional expression " + expression + " (with the current settings)");
				}
			} finally {
				JDD.Deref(statesOfInterest);
			}
		}

		final ModelTransformation<ProbModel, ProbModel> transformation = transformModel(transformer, model, expression, statesOfInterest);
		final StateValues resultTransformed = checkExpressionTransformedModel(transformation, expression);

		final StateValues resultOriginal = transformation.projectToOriginalModel(resultTransformed);
		transformation.clear();

		return resultOriginal;
	}

	private ModelTransformation<ProbModel, ProbModel> transformModel(final NewConditionalTransformer.DTMC transformer, final ProbModel model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		prism.getLog().println("\nTransforming model (using " + transformer.getClass().getSimpleName() + ") for condition: " + expression.getCondition());
		long timer = System.currentTimeMillis();
		final ModelTransformation<ProbModel, ProbModel> transformation = transformer.transform(model, expression, statesOfInterest);
		timer = System.currentTimeMillis() - timer;
		prism.getLog().println("\nTime for model transformation: " + timer / 1000.0 + " seconds.");
		prism.getLog().println("\nOverall time for model transformation: " + timer / 1000.0 + " seconds.");
		prism.getLog().print("Transformed model has ");
		prism.getLog().println(transformation.getTransformedModel().infoString());
		prism.getLog().print("Transformed matrix has ");
		prism.getLog().println(transformation.getTransformedModel().matrixInfoString());
		return transformation;
	}

	private NewConditionalTransformer.DTMC selectModelTransformer(final ProbModel model, final ExpressionConditional expression) throws PrismException
	{
		PrismSettings settings = prism.getSettings();
		NewConditionalTransformer.DTMC transformer;
		if (settings.getBoolean(PrismSettings.CONDITIONAL_DTMC_USE_MDP_TRANSFORMATIONS)) {
			final String specification = settings.getString(PrismSettings.CONDITIONAL_MDP);
			final SortedSet<MdpTransformerType> types = MdpTransformerType.getValuesOf(specification);
			for (MdpTransformerType type : types) {
				switch (type) {
				case FinallyFinally:
					transformer = new NewFinallyUntilTransformer.DTMC(modelChecker);
					break;
//				case LtlFinally:
//					transformer = new NewLtlUntilTransformer.DTMC(modelChecker);
//					break;
//				case FinallyLtl:
//					transformer = new NewFinallyLtlTransformer.DTMC(modelChecker);
//					break;
//				case LtlLtl:
//					transformer = new NewLtlLtlTransformer.DTMC(modelChecker);
//					break;
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
			for (DtmcTransformerType type : types) {
				switch (type) {
//				case Quotient:
//					transformer = new MCQuotientTransformer(modelChecker);
//					break;
				case Until:
					transformer = new MCUntilTransformer(modelChecker, prism);
					break;
				case Next:
					transformer = new MCNextTransformer(modelChecker, prism);
					break;
				case Ltl:
					transformer = new MCLTLTransformer(modelChecker, prism);
					break;
				default:
					continue;
				}
				if (transformer.canHandle(model, expression)) {
					return transformer;
				}
			}
		}

//		throw new PrismException("Cannot model check " + expression);
		return null;
	}

	private StateValues checkExpressionTransformedModel(final ModelTransformation<ProbModel, ProbModel> transformation, final ExpressionConditional expression) throws PrismException
	{
		final ProbModel transformedModel = transformation.getTransformedModel();
		final Expression transformedExpression;
		if (transformation instanceof ModelExpressionTransformation) {
			transformedExpression = ((ModelExpressionTransformation<?,?>) transformation).getTransformedExpression();
		} else {
			transformedExpression = expression.getObjective();
		}

		prism.getLog().println("\nChecking property in transformed model ...");
		long timer = System.currentTimeMillis();

		ModelChecker mcTransformed = modelChecker.createModelChecker(transformedModel);

		if (transformation instanceof ModelExpressionTransformation) {
			
		} else {
		
		}
		final StateValues result = mcTransformed.checkExpression(transformedExpression, JDD.Constant(1));
		timer = System.currentTimeMillis() - timer;
		prism.getLog().println("\nTime for model checking in transformed model: " + timer / 1000.0 + " seconds.");

		return result;
	}

	private Expression transformForQuotient(ExpressionConditional expr) throws PrismException
	{
		if (!(expr.getObjective() instanceof ExpressionProb)) {
			throw new PrismNotSupportedException("Can not transform expression with quotient method: "+expr);
		}

		final String specification = prism.getSettings().getString(PrismSettings.CONDITIONAL_MC);
		final SortedSet<DtmcTransformerType> types = DtmcTransformerType.getValuesOf(specification);

		if (!types.contains(DtmcTransformerType.Quotient)) {
			return null;
		}

		ExpressionProb top = (ExpressionProb) expr.getObjective().deepCopy();
		Expression bound = top.getBound();
		RelOp relOp = top.getRelOp();
		if (top.getBound() != null) {
			// P bowtie bound
			top.setBound(null);
			top.setRelOp(RelOp.COMPUTE_VALUES);
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
