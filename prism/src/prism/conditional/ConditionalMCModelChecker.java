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
import prism.Prism;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;
import prism.StochModel;
import prism.StochModelChecker;
import prism.conditional.NewConditionalTransformer.MC;

public abstract class ConditionalMCModelChecker<M extends ProbModel, C extends ProbModelChecker> extends ConditionalModelChecker<M>
{
	protected C modelChecker;

	public ConditionalMCModelChecker(Prism prism, C modelChecker)
	{
		super(prism);
		this.modelChecker = modelChecker;
	}

	@Override
	public StateValues checkExpression(M model, ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		final NewConditionalTransformer.MC<M,C> transformer = selectModelTransformer(model, expression);
		final ModelExpressionTransformation<M, ? extends M> transformation = transformModel(transformer, model, expression, statesOfInterest);
		final StateValues resultTransformed = checkExpressionTransformedModel(transformation, expression);

		final StateValues resultOriginal = transformation.projectToOriginalModel(resultTransformed);
		transformation.clear();

		return resultOriginal;
	}

	protected ModelExpressionTransformation<M, ? extends M> transformModel(final NewConditionalTransformer.MC<M,C> transformer, final M model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		prism.getLog().println("\nTransforming model (using " + transformer.getName() + ") for condition: " + expression.getCondition());
		long timer = System.currentTimeMillis();
		final ModelExpressionTransformation<M, ? extends M> transformation = transformer.transform(model, expression, statesOfInterest);
		timer = System.currentTimeMillis() - timer;
		prism.getLog().println("\nTime for model transformation: " + timer / 1000.0 + " seconds.");
		prism.getLog().println("\nOverall time for model transformation: " + timer / 1000.0 + " seconds.");
		prism.getLog().print("Transformed model has ");
		prism.getLog().println(transformation.getTransformedModel().infoString());
		prism.getLog().print("Transformed matrix has ");
		prism.getLog().println(transformation.getTransformedModel().matrixInfoString());

		return transformation;
	}

	protected NewConditionalTransformer.MC<M, C> selectModelTransformer(M model, ExpressionConditional expression) throws PrismException
	{
		PrismSettings settings = prism.getSettings();
		if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_TACAS14_PROTOTYPE)) {
			throw new PrismNotSupportedException("There is no symbolic TACAS'14 prototype");
		}
		if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_PROTOTYPE)) {
			throw new PrismNotSupportedException("There is no symbolic prototype for the scale method in MCs");
		}

		NewConditionalTransformer.MC<M, C> transformer;
		if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_RESET_FOR_MC)) {
			String specification                = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_RESET);
			SortedSet<MdpTransformerType> types = MdpTransformerType.getValuesOf(specification);
			for (MdpTransformerType type : types) {
				transformer = getResetTransformer(type);
				if (transformer != null && transformer.canHandle(model, expression)) {
					return transformer;
				}
			}
		} else {
			String specification = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_SCALE);
			SortedSet<DtmcTransformerType> types = DtmcTransformerType.getValuesOf(specification);
			for (DtmcTransformerType type : types) {
				transformer = getScaleTransformer(type);
				if (transformer != null && transformer.canHandle(model, expression)) {
					return transformer;
				}
			}
		}
		throw new PrismException("Cannot model check " + expression);
	}

	protected StateValues checkExpressionTransformedModel(final ModelExpressionTransformation<M, ? extends M> transformation, final ExpressionConditional expression) throws PrismException
	{
		ProbModel transformedModel          = transformation.getTransformedModel();
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

	protected abstract MC<M, C> getScaleTransformer(DtmcTransformerType type);

	protected abstract MC<M, C> getResetTransformer(MdpTransformerType type);



	public static class CTMC extends ConditionalMCModelChecker<StochModel, StochModelChecker>
	{
		public CTMC(Prism prism, StochModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}

		@Override
		public StateValues checkExpression(StochModel model, ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
		{
			if (! Expression.containsTemporalTimeBounds(expression.getCondition())) {
				return super.checkExpression(model, expression, statesOfInterest);
			}
			if (!(expression.getObjective() instanceof ExpressionProb)) {
				throw new PrismException("Cannot model check " + expression);
			}
			ExpressionProb objective = (ExpressionProb) expression.getObjective();
			if (Expression.containsTemporalTimeBounds(objective.getExpression())) {
				throw new PrismException("Cannot model check " + expression);
			}
			Expression transformed = transformExpression(expression);
			prism.getLog().println();
			prism.getLog().println("Condition contains time bounds, trying Bayes' rule.");
			prism.getLog().println("Checking transformed expression: " + transformed);
			return modelChecker.checkExpression(transformed, statesOfInterest);
		}

		protected Expression transformExpression(ExpressionConditional expression)
				throws PrismNotSupportedException
		{
			ExpressionProb objective = (ExpressionProb) expression.getObjective();
			Expression condition     = expression.getCondition();

			// Bayes' rule: P(A|B) = P(B|A) * P(A) / P(B)
			String computeValues        = RelOp.COMPUTE_VALUES.toString();
			ExpressionProb newObjective = new ExpressionProb(condition.deepCopy(), computeValues, null);
			Expression newCondition     = objective.getExpression().deepCopy();
			ExpressionConditional pAB   = new ExpressionConditional(newObjective, newCondition);
			ExpressionProb pA           = new ExpressionProb(objective.getExpression().deepCopy(), computeValues, null);
			ExpressionProb pB           = new ExpressionProb(condition.deepCopy(), computeValues, null);
			ExpressionBinaryOp fraction = Expression.Divide(Expression.Times(pAB, pA), pB);

			// translate bounds if necessary
			if (objective.getBound() == null) {
				return fraction;
			}
			int binOp = convertToBinaryOp(objective.getRelOp());
			return new ExpressionBinaryOp(binOp, Expression.Parenth(fraction), objective.getBound().deepCopy());
		}

		protected int convertToBinaryOp(RelOp relop) throws PrismNotSupportedException
		{
			switch (relop) {
			case GT:
				return ExpressionBinaryOp.GT;
			case GEQ:
				return ExpressionBinaryOp.GE;
			case LT:
				return ExpressionBinaryOp.LT;
			case LEQ:
				return ExpressionBinaryOp.LE;
			default:
				throw new PrismNotSupportedException("Unsupported comparison operator: " + relop);
			}
		}

		@Override
		protected NewConditionalTransformer.MC<StochModel, StochModelChecker> getResetTransformer(MdpTransformerType type)
		{
			switch (type) {
			case FinallyFinally:
				return new NewFinallyUntilTransformer.CTMC(prism, modelChecker);
			case LtlFinally:
				return new NewLtlUntilTransformer.CTMC(prism, modelChecker);
			case FinallyLtl:
				return new NewFinallyLtlTransformer.CTMC(prism, modelChecker);
			case LtlLtl:
				return new NewLtlLtlTransformer.CTMC(prism, modelChecker);
			default:
				return null;
			}
		}

		@Override
		protected MC<StochModel, StochModelChecker> getScaleTransformer(DtmcTransformerType type)
		{
			switch (type) {
			case Quotient:
				return new MCQuotientTransformer.CTMC(prism, modelChecker);
			case Until:
				return new MCUntilTransformer.CTMC(prism, modelChecker);
			case Next:
				return new MCNextTransformer.CTMC(prism, modelChecker);
			case Ltl:
				return new MCLTLTransformer.CTMC(prism, modelChecker);
			default:
				return null;
			}
		}
	}



	public static class DTMC extends ConditionalMCModelChecker<ProbModel, ProbModelChecker>
	{
		public DTMC(Prism prism, ProbModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}

		@Override
		protected MC<ProbModel, ProbModelChecker> getResetTransformer(MdpTransformerType type)
		{
			switch (type) {
			case FinallyFinally:
				return new NewFinallyUntilTransformer.DTMC(prism, modelChecker);
			case LtlFinally:
				return new NewLtlUntilTransformer.DTMC(prism, modelChecker);
			case FinallyLtl:
				return new NewFinallyLtlTransformer.DTMC(prism, modelChecker);
			case LtlLtl:
				return new NewLtlLtlTransformer.DTMC(prism, modelChecker);
			default:
				return null;
			}
		}

		@Override
		protected NewConditionalTransformer.MC<ProbModel, ProbModelChecker> getScaleTransformer(DtmcTransformerType type)
		{
			switch (type) {
			case Quotient:
				return new MCQuotientTransformer.DTMC(prism, modelChecker);
			case Until:
				return new MCUntilTransformer.DTMC(prism, modelChecker);
			case Next:
				return new MCNextTransformer.DTMC(prism, modelChecker);
			case Ltl:
				return new MCLTLTransformer.DTMC(prism, modelChecker);
			default:
				return null;
			}
		}
	}
}
