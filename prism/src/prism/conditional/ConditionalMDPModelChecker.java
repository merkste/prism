package prism.conditional;

import java.util.SortedSet;

import explicit.MinMax;
import explicit.conditional.transformer.ConditionalTransformerType;
import explicit.conditional.transformer.UndefinedTransformationException;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionQuant;
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
import prism.conditional.prototype.MDPFinallyTransformer;
import prism.conditional.prototype.MDPLTLTransformer;

//FIXME ALG: add comment
public class ConditionalMDPModelChecker extends ConditionalModelChecker<NondetModel>
{	
	protected NondetModelChecker modelChecker;

	public ConditionalMDPModelChecker(NondetModelChecker modelChecker, Prism prism)
	{
		super(prism);
		this.modelChecker = modelChecker;
	}

	@Override
	public StateValues checkExpression(final NondetModel model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		ExpressionQuant<?> objective = expression.getObjective();
		if (! (objective instanceof ExpressionProb)) {
			throw new PrismNotSupportedException("Can only model check conditional probabilities in MDPs, not " + objective.getClass().getSimpleName());
		}

		OpRelOpBound oprel = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		if (oprel.getMinMax(model.getModelType()).isMin()) {
			return checkExpressionMin(model, expression, statesOfInterest);
		}
		return checkExpressionMax(model, expression, statesOfInterest);
	}

	public StateValues checkExpressionMax(final NondetModel model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel       = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		assert oprel.getMinMax(model.getModelType()).isMax() : "Pmax expected: " + expression;

		// FIXME ALG: code dupes, e.g., in reset transformer types
		double n = JDD.GetNumMinterms(statesOfInterest, model.getNumDDRowVars());
		if (n != 1) {
			JDD.Deref(statesOfInterest);
			throw new PrismException("Currently, only a single state of interest is supported for MDP conditionals, got "+n);
		}

		NewConditionalTransformer.MDP transformer = selectModelTransformer(model, expression);
		if (transformer == null) {
			JDD.Deref(statesOfInterest);
			throw new PrismNotSupportedException("Cannot model check " + expression);
		}

		ModelExpressionTransformation<NondetModel, ? extends NondetModel> transformation;
		try {
			transformation = transformModel(transformer, model, expression, statesOfInterest);
		} catch (UndefinedTransformationException e) {
			// the condition is unsatisfiable for the state of interest
			prism.getLog().println("\nTransformation failed: " + e.getMessage());
			return createUndefinedStateValues(model, expression);
		}
		StateValues resultTransformed = checkExpressionTransformedModel(transformation);
		StateValues resultOriginal = transformation.projectToOriginalModel(resultTransformed);
		transformation.clear();
		return resultOriginal;
	}

	protected StateValues checkExpressionMin(final NondetModel model, final ExpressionConditional expression, final JDDNode statesOfInterest)
			throws PrismLangException, PrismException
	{
		// P>x [prop] <=> Pmin=?[prop]    > x
		//                1-Pmax=?[!prop] > x
		//                Pmax=?[!prop]   < 1-x
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
			ExpressionProb inverseObjective = new ExpressionProb(Expression.Not(objective.getExpression()), MinMax.max(), inverseRelop.toString(), inverseProb);
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

		final String specification = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_MDP);
		final SortedSet<ConditionalTransformerType> types = ConditionalTransformerType.getValuesOf(specification);
		if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_PROTOTYPE)) {
			for (ConditionalTransformerType type : types) {
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
			for (ConditionalTransformerType type : types) {
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
		NondetModel transformedModel        = transformation.getTransformedModel();
		Expression transformedExpression    = transformation.getTransformedExpression();
		JDDNode transformedStatesOfInterest = transformation.getTransformedStatesOfInterest();
		prism.getLog().println("\nChecking transformed property in transformed model: " + transformedExpression);

		long timer = System.currentTimeMillis();
		ModelChecker mcTransformed = modelChecker.createModelChecker(transformedModel);
		StateValues result         = mcTransformed.checkExpression(transformedExpression, transformedStatesOfInterest);
		timer = System.currentTimeMillis() - timer;
		prism.getLog().println("\nTime for model checking in transformed model: " + timer / 1000.0 + " seconds.");

		return result;
	}
}
