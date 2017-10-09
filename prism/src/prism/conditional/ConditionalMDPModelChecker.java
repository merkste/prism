package prism.conditional;

import java.util.SortedSet;

import explicit.MinMax;
import explicit.conditional.transformer.MdpTransformerType;
import explicit.conditional.transformer.UndefinedTransformationException;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.ModelChecker;
import prism.ModelExpressionTransformation;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.OpRelOpBound;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.ProbModel;
import prism.StateValues;
import prism.StateValuesMTBDD;
import prism.conditional.prototype.MDPFinallyTransformer;
import prism.conditional.prototype.MDPLTLTransformer;

public class ConditionalMDPModelChecker extends ConditionalModelChecker<NondetModel> {
	
	protected NondetModelChecker modelChecker;

	public ConditionalMDPModelChecker(NondetModelChecker modelChecker, Prism prism)
	{
		super(prism);
		this.modelChecker = modelChecker;
	}

	@Override
	public StateValues checkExpression(final NondetModel model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		assert expression.getObjective() instanceof ExpressionProb;

		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		if (oprel.getMinMax(model.getModelType()).isMin()) {
			return checkExpressionMin(model, expression, statesOfInterest);
		}
		StateValues result = checkExpressionMax(model, expression, statesOfInterest);

		return result;
	}

	public StateValues checkExpressionMax(final NondetModel model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		double n = JDD.GetNumMinterms(statesOfInterest, model.getNumDDRowVars());
		if (n != 1) {
			JDD.Deref(statesOfInterest);
			throw new PrismException("Currently, only a single state of interest is supported for MDP conditionals, got "+n);
		}

		try {
			final NewConditionalTransformer.MDP transformer = selectModelTransformer(model, expression);
			if (transformer == null) {
				JDD.Deref(statesOfInterest);
				throw new PrismNotSupportedException("Cannot model check conditional expression " + expression + " (with the current settings)");
			}
			final ModelExpressionTransformation<NondetModel, ? extends NondetModel> transformation = transformModel(transformer, model, expression, statesOfInterest);
			final StateValues resultTransformed = checkExpressionTransformedModel(transformation);

			final StateValues resultOriginal = transformation.projectToOriginalModel(resultTransformed);
			transformation.clear();

			return resultOriginal;
		} catch (UndefinedTransformationException e) {
			// the condition is unsatisfiable for the state of interest
			ExpressionProb expr = (ExpressionProb) expression.getObjective();
			if (expr.getProb() != null) {
				// P with bound -> false
				return new StateValuesMTBDD(JDD.Constant(0.0), model);	
			} else {
				// =? -> not a number
				return new StateValuesMTBDD(JDD.Constant(Double.NaN), model);
			}
		}
	}

	protected StateValues checkExpressionMin(final NondetModel model, final ExpressionConditional expression, final JDDNode statesOfInterest) throws PrismLangException, PrismException
	{
		// P>x [prop] <=>	Pmin=?[prop]	> x
		//					1-Pmax=?[!prop]	> x
		//					Pmax=?[!prop]	< 1-x
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel       = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		assert oprel.getMinMax(model.getModelType()).isMin(): "Pmin expected: " + expression;

		final Expression inverseExpression;
		if (oprel.isNumeric()) {
			ExpressionProb inverseObjective = new ExpressionProb(Expression.Not(objective.getExpression()), MinMax.max(), RelOp.COMPUTE_VALUES.toString(), null);
			inverseExpression               = Expression.Minus(Expression.Literal(1), new ExpressionConditional(inverseObjective, expression.getCondition()));
		} else {
			RelOp inverseRelop              = oprel.getRelOp().negate(true); // negate but keep strictness
			Expression inverseProb          = Expression.Minus(Expression.Literal(1), objective.getProb());
			ExpressionProb inverseObjective = new ExpressionProb(Expression.Not(objective.getExpression()), inverseRelop.toString(), inverseProb);
			inverseExpression = new ExpressionConditional(inverseObjective, expression.getCondition());
		}
		// Debug output
		prism.getLog().println("Transform Pmin conditional expression to "+inverseExpression);
		return modelChecker.checkExpression(inverseExpression, statesOfInterest);
	}

	protected ModelExpressionTransformation<NondetModel, ? extends NondetModel> transformModel(final NewConditionalTransformer.MDP transformer, final NondetModel model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		prism.getLog().println("\nTransforming model (using " + transformer.getName() + ") for " + expression);
		long timer = System.currentTimeMillis();
		ModelExpressionTransformation<NondetModel, ? extends NondetModel> transformation = transformer.transform(model, expression, statesOfInterest);
		timer = System.currentTimeMillis() - timer;
		prism.getLog().println("\nTime for model transformation: " + timer / 1000.0 + " seconds.");
		prism.getLog().println("\nOverall time for model transformation: " + timer / 1000.0 + " seconds.");
		prism.getLog().print("Transformed model has ");
		prism.getLog().println(transformation.getTransformedModel().infoString());
		prism.getLog().print("Transformed matrix has ");
		prism.getLog().println(transformation.getTransformedModel().matrixInfoString());
		return transformation;
	}

	protected NewConditionalTransformer.MDP selectModelTransformer(final ProbModel model, final ExpressionConditional expression) throws PrismException
	{
		PrismSettings settings = prism.getSettings();
		if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_TACAS14_PROTOTYPE)) {
			throw new PrismException("There is no symbolic TACAS'14 prototype");
		}

		final String specification = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_RESET);
		final SortedSet<MdpTransformerType> types = MdpTransformerType.getValuesOf(specification);
		if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_PROTOTYPE)) {
			for (MdpTransformerType type : types) {
				NewConditionalTransformer.MDP transformer;
				switch (type) {
				case FinallyFinally:
					transformer = new MDPFinallyTransformer(prism, modelChecker);
					break;
				case LtlLtl:
					transformer = new MDPLTLTransformer(prism, modelChecker);
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
				NewConditionalTransformer.MDP transformer;
				switch (type) {
				case FinallyFinally:
					transformer = new NewFinallyUntilTransformer.MDP(prism, modelChecker);
					break;
				case LtlFinally:
					transformer = new NewLtlUntilTransformer.MDP(prism, modelChecker);
					break;
				case FinallyLtl:
					transformer = new NewFinallyLtlTransformer.MDP(prism, modelChecker);
					break;
				case LtlLtl:
					transformer = new NewLtlLtlTransformer.MDP(prism, modelChecker);
					break;
				default:
					continue;
				}
				if (transformer.canHandle(model, expression)) {
					return transformer;
				}
			}
		}

		return null;
	}

	protected StateValues checkExpressionTransformedModel(final ModelExpressionTransformation<NondetModel, ? extends NondetModel> transformation) throws PrismException
	{
		final NondetModel transformedModel = transformation.getTransformedModel();
		final Expression transformedExpression = transformation.getTransformedExpression();

		prism.getLog().println("\nChecking transformed property in transformed model: " + transformedExpression);
		long timer = System.currentTimeMillis();

		ModelChecker mcTransformed = modelChecker.createModelChecker(transformedModel);
		
		final StateValues resultProduct = mcTransformed.checkExpression(transformedExpression, transformation.getTransformedStatesOfInterest());
		timer = System.currentTimeMillis() - timer;
		prism.getLog().println("\nTime for model checking in transformed model: " + timer / 1000.0 + " seconds.");

		return resultProduct;
	}
}
