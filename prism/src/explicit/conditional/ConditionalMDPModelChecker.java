package explicit.conditional;

import java.util.BitSet;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionQuant;
import parser.ast.RelOp;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSparse;
import explicit.MinMax;
import explicit.ModelExpressionTransformation;
import explicit.StateValues;
import explicit.conditional.reset.FinallyLtlTransformer;
import explicit.conditional.reset.FinallyUntilTransformer;
import explicit.conditional.reset.LtlLtlTransformer;
import explicit.conditional.reset.LtlUntilTransformer;
import explicit.conditional.transformer.UndefinedTransformationException;

//FIXME ALG: add comment
public class ConditionalMDPModelChecker extends ConditionalModelChecker<MDP, MDPModelChecker>
{
	public ConditionalMDPModelChecker(MDPModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	public StateValues checkExpression(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
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

	@Override
	protected MDPSparse convertToSparseModel(explicit.MDP transformedModel)
	{
		return new MDPSparse(transformedModel);
	}

	protected String getConditionalPatterns()
	{
		return settings.getString(PrismSettings.CONDITIONAL_PATTERNS_MDP);
	}

	protected ConditionalTransformer<MDP, MDPModelChecker> getTransformer(ConditionalTransformerType type)
	{
		switch (type) {
		case FinallyFinally:
			return new FinallyUntilTransformer.MDP(modelChecker);
		case LtlFinally:
			return new LtlUntilTransformer.MDP(modelChecker);
		case FinallyLtl:
			return new FinallyLtlTransformer.MDP(modelChecker);
		case LtlLtl:
			return new LtlLtlTransformer.MDP(modelChecker);
		default:
			return null;
		}
	}

	private StateValues checkExpressionMax(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel       = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		assert oprel.getMinMax(model.getModelType()).isMax() : "Pmax expected: " + expression;

		ConditionalTransformer<MDP, MDPModelChecker> transformer = selectModelTransformer(model, expression);
		if (transformer == null) {
			throw new PrismNotSupportedException("Cannot model check " + expression);
		}

		StateValues result;
		try {
			mainLog.println("\nTransforming model (using " + transformer.getName() + ") for " + expression);
			ModelExpressionTransformation<MDP, ? extends MDP> transformation = transformModel(transformer, model, expression, statesOfInterest);
			StateValues resultTransformedModel = checkExpressionTransformedModel(transformation);
			result = transformation.projectToOriginalModel(resultTransformedModel);
		} catch (UndefinedTransformationException e) {
			mainLog.println("\nTransformation failed: " + e.getMessage());
			result = createUndefinedStateValues(model, expression);
		}

		return result;
	}

	private StateValues checkExpressionMin(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismLangException, PrismException
	{
		// P>x [prop] <=> Pmin=?[prop]    > x
		//                1-Pmax=?[!prop] > x
		//                Pmax=?[!prop]   < 1-x
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel       = objective.getRelopBoundInfo(modelChecker.getConstantValues());
		assert oprel.getMinMax(model.getModelType()).isMin() : "Pmin expected: " + expression;

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
		getLog().println("Transform Pmin conditional expression to "+inverseExpression);
		return modelChecker.checkExpression(model, inverseExpression, statesOfInterest);
	}
}
