package prism.conditional;

import explicit.MinMax;
import explicit.conditional.ConditionalTransformerType;
import explicit.conditional.transformer.UndefinedTransformationException;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionQuant;
import parser.ast.RelOp;
import prism.ModelExpressionTransformation;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.OpRelOpBound;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.StateValues;
import prism.conditional.reset.FinallyLtlTransformer;
import prism.conditional.reset.FinallyUntilTransformer;
import prism.conditional.reset.LtlLtlTransformer;
import prism.conditional.reset.LtlUntilTransformer;

//FIXME ALG: add comment
public class ConditionalMDPModelChecker extends ConditionalModelChecker<NondetModel, NondetModelChecker>
{	
	public ConditionalMDPModelChecker(NondetModelChecker modelChecker, Prism prism)
	{
		super(modelChecker, prism);
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

		ConditionalTransformer<NondetModel,NondetModelChecker> transformer = selectModelTransformer(model, expression);
		if (transformer == null) {
			JDD.Deref(statesOfInterest);
			throw new PrismNotSupportedException("Cannot model check " + expression);
		}

		ModelExpressionTransformation<NondetModel, ? extends NondetModel> transformation;
		try {
			prism.getLog().println("\nTransforming model (using " + transformer.getName() + ") for " + expression);
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

	@Override
	protected String getConditionalPatterns()
	{
		return prism.getSettings().getString(PrismSettings.CONDITIONAL_PATTERNS_MDP);
	}

	@Override
	protected ConditionalTransformer<NondetModel, NondetModelChecker> getTransformer(ConditionalTransformerType type)
	{
		switch (type) {
		case FinallyFinally:
			return new FinallyUntilTransformer.MDP(prism, modelChecker);
		case LtlFinally:
			return new LtlUntilTransformer.MDP(prism, modelChecker);
		case FinallyLtl:
			return new FinallyLtlTransformer.MDP(prism, modelChecker);
		case LtlLtl:
			return new LtlLtlTransformer.MDP(prism, modelChecker);
		default:
			return null;
		}
	}
}
