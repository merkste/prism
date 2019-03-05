package explicit.conditional;

import java.util.BitSet;
import java.util.SortedSet;

import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLongRun;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import explicit.BasicModelExpressionTransformation;
import explicit.BasicModelTransformation;
import explicit.CTMCModelChecker;
import explicit.CTMCSparse;
import explicit.DTMCModelChecker;
import explicit.DTMCSimple;
import explicit.DTMCSparse;
import explicit.ModelExpressionTransformation;
import explicit.ProbModelChecker;
import explicit.StateValues;
import explicit.conditional.transformer.ConditionalTransformerType;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.conditional.transformer.mc.MCQuotientTransformer;
import explicit.conditional.transformer.mc.MCLtlTransformer;
import explicit.conditional.transformer.mc.MCNextTransformer;
import explicit.conditional.transformer.mc.MCUntilTransformer;
import explicit.modelviews.MCView;

// FIXME ALG: add comment
public abstract class ConditionalMCModelChecker<M extends explicit.DTMC, C extends ProbModelChecker> extends ConditionalModelChecker<M>
{
	protected C modelChecker;

	public ConditionalMCModelChecker(C modelChecker)
	{
		super(modelChecker);
		this.modelChecker = modelChecker;
	}

	@Override
	public StateValues checkExpression(M model, ExpressionConditional expression, BitSet statesOfInterest) throws PrismException
	{
		ConditionalTransformer<M, C> transformer = selectModelTransformer(model, expression);
		if (transformer == null) {
			if (expression.getObjective() instanceof ExpressionLongRun) {
				// try alternative long-run approach
				ExpressionLongRun longrun = (ExpressionLongRun) expression.getObjective();
				return modelChecker.checkConditionalExpressionLongRun(model, longrun, expression.getCondition(), statesOfInterest);
			}
			throw new PrismNotSupportedException("Cannot model check " + expression);
		}

		ModelExpressionTransformation<M, ? extends M> transformation;
		try {
			transformation = transformModel(transformer, model, expression, statesOfInterest);
		} catch (UndefinedTransformationException e) {
			mainLog.println("\nTransformation failed: " + e.getMessage());
			return createUndefinedStateValues(model, expression);
		}
		StateValues result = checkExpressionTransformedModel(transformation);
		return transformation.projectToOriginalModel(result);
	}

	public ModelExpressionTransformation<M, ? extends M> transformModel(final ConditionalTransformer<M, C> transformer, final M model,
			final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		mainLog.println("\nTransforming model (using " + transformer.getName() + ") for condition: " + expression);
		long overallTime = System.currentTimeMillis();
		ModelExpressionTransformation<M, ? extends M> transformation = transformer.transform(model, expression, statesOfInterest);
		long transformationTime = System.currentTimeMillis() - overallTime;
		mainLog.println("\nTime for model transformation: " + transformationTime / 1000.0 + " seconds.");

		M transformedModel = transformation.getTransformedModel();
		if (isVirtualModel(transformedModel) || transformedModel instanceof DTMCSimple) {
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_VIRTUAL_MODELS)) {
				mainLog.println("Using simple/virtual model");
			} else {
				transformation = convertVirtualModel(transformation);
			}
		}
		overallTime = System.currentTimeMillis() - overallTime;
		mainLog.println("\nOverall time for model transformation: " + overallTime / 1000.0 + " seconds.");
		mainLog.print("Transformed model has ");
		mainLog.println(transformation.getTransformedModel().infoString());
		return transformation;
	}

	public boolean isVirtualModel(M model)
	{
		return (model instanceof MCView) && ((MCView) model).isVirtual();
	}

	public ConditionalTransformer<M, C> selectModelTransformer(final M model, final ExpressionConditional expression) throws PrismException
	{
		SortedSet<ConditionalTransformerType> types = getTransformerTypes();
		ConditionalTransformer<M, C> transformer;
		for (ConditionalTransformerType type : types) {
			transformer = getTransformer(type);
			if (transformer != null && transformer.canHandle(model, expression)) {
				return transformer;
			}
		}
		return null;
	}

	public StateValues checkExpressionTransformedModel(final ModelExpressionTransformation<M, ? extends M> transformation) throws PrismException
	{
		M transformedModel                 = transformation.getTransformedModel();
		Expression transformedExpression   = transformation.getTransformedExpression();
		BitSet transformedStatesOfInterest = transformation.getTransformedStatesOfInterest();

		mainLog.println("\nChecking transformed property in transformed model: " + transformedExpression);
		long timer     = System.currentTimeMillis();
		StateValues sv = modelChecker.checkExpression(transformedModel, transformedExpression, transformedStatesOfInterest);
		timer          = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking in transformed model: " + timer / 1000.0 + " seconds.");

		return sv;
	}

	protected abstract SortedSet<ConditionalTransformerType> getTransformerTypes() throws PrismException;

	protected abstract ConditionalTransformer<M, C> getTransformer(ConditionalTransformerType type);

	protected abstract ModelExpressionTransformation<M, ? extends M> convertVirtualModel(ModelExpressionTransformation<M,? extends M> transformation);



	public static class CTMC extends ConditionalMCModelChecker<explicit.CTMC, CTMCModelChecker>
	{
		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public StateValues checkExpression(explicit.CTMC model, ExpressionConditional expression, BitSet statesOfInterest) throws PrismException
		{
			if (! Expression.containsTemporalTimeBounds(expression.getCondition())) {
				return super.checkExpression(model, expression, statesOfInterest);
			}
			if (expression.getObjective() instanceof ExpressionLongRun) {
				// try alternative long-run approach
				ExpressionLongRun longrun = (ExpressionLongRun) expression.getObjective();
				return modelChecker.checkConditionalExpressionLongRun(model, longrun, expression.getCondition(), statesOfInterest);
			}
			if (!(expression.getObjective() instanceof ExpressionProb)) {
				throw new PrismException("Cannot model check " + expression);
			}
			ExpressionProb objective = (ExpressionProb) expression.getObjective();
			if (Expression.containsTemporalTimeBounds(objective.getExpression())) {
				throw new PrismException("Cannot model check " + expression);
			}
			getLog().println();
			getLog().println("Condition contains time bounds, trying Bayes' rule.");
			return checkExpressionBayes(model, expression, statesOfInterest);
		}

		protected StateValues checkExpressionBayes(explicit.CTMC model, ExpressionConditional expression, BitSet statesOfInterest)
				throws PrismException
		{
			ExpressionProb objective = (ExpressionProb) expression.getObjective();
			Expression condition     = expression.getCondition();

			// Bayes' rule: P(A|B) = P(B|A) * P(A) / P(B)
			String computeValues           = RelOp.COMPUTE_VALUES.toString();
			ExpressionProb newObjective    = new ExpressionProb(condition.deepCopy(), computeValues, null);
			Expression newCondition        = objective.getExpression().deepCopy();
			ExpressionConditional pAB      = new ExpressionConditional(newObjective, newCondition);
			ExpressionProb pA              = new ExpressionProb(objective.getExpression().deepCopy(), computeValues, null);
			ExpressionProb pB              = new ExpressionProb(condition.deepCopy(), computeValues, null);
			ExpressionBinaryOp transformed = Expression.Divide(Expression.Times(pAB, pA), pB);

			// translate bounds if necessary
			OpRelOpBound opInfo = expression.getRelopBoundInfo(modelChecker.getConstantValues());
			if (!opInfo.isNumeric()) {
				int binOp = convertToBinaryOp(objective.getRelOp());
				transformed = new ExpressionBinaryOp(binOp, Expression.Parenth(transformed), objective.getBound().deepCopy());
			}

			getLog().println("Checking transformed expression: " + transformed);
			long timer     = System.currentTimeMillis();
			// check numerator
			StateValues result   = modelChecker.checkExpression(model, pAB, statesOfInterest);
			StateValues resultA  = modelChecker.checkExpression(model, pA, statesOfInterest);
			// change NaN to 0 in pAB for each pA==0
			for (int s=0, n=result.getSize(); s<n; n++) {
				if (resultA.getDoubleArray()[s] == 0.0) {
					result.setDoubleValue(s, 0.0);
				}
			}
			result.times(resultA);
			resultA.clear();
			// check denominator
			StateValues resultB  = modelChecker.checkExpression(model, pB, statesOfInterest);
			result.divide(resultB);
			resultB.clear();
			// check bounds if necessary
			if (!opInfo.isNumeric()) {
				BitSet bits = result.getBitSetFromInterval(opInfo.getRelOp(), opInfo.getBound());
				result.clear();
				result = StateValues.createFromBitSet(bits, model);
			}
			timer          = System.currentTimeMillis() - timer;
			mainLog.println("\nTime for model checking transformed expression: " + timer / 1000.0 + " seconds.");
			return result;
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
		protected SortedSet<ConditionalTransformerType> getTransformerTypes() throws PrismException
		{
			String specification = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_CTMC);
			return ConditionalTransformerType.getValuesOf(specification);
		}

		@Override
		protected ConditionalTransformer<explicit.CTMC, CTMCModelChecker> getTransformer(ConditionalTransformerType type)
		{
			switch (type) {
			case Until:
				return new MCUntilTransformer.CTMC(modelChecker);
			case Next:
				return new MCNextTransformer.CTMC(modelChecker);
			case Ltl:
				return new MCLtlTransformer.CTMC(modelChecker);
			case FinallyFinally:
				return new FinallyUntilTransformer.CTMC(modelChecker);
			case LtlFinally:
				return new LtlUntilTransformer.CTMC(modelChecker);
			case FinallyLtl:
				return new FinallyLtlTransformer.CTMC(modelChecker);
			case LtlLtl:
				return  new LtlLtlTransformer.CTMC(modelChecker);
			case Quotient:
				return new MCQuotientTransformer.CTMC(modelChecker);
			default:
				return null;
			}
		}

		@Override
		protected ModelExpressionTransformation<explicit.CTMC, ? extends explicit.CTMC> convertVirtualModel(ModelExpressionTransformation<explicit.CTMC, ? extends explicit.CTMC> transformation)
		{
			mainLog.println("\nConverting simple/virtual model to " + CTMCSparse.class.getSimpleName());
			long buildTime = System.currentTimeMillis();
			explicit.CTMC transformedModel = transformation.getTransformedModel();
			CTMCSparse transformedModelSparse = new CTMCSparse(transformedModel);
			buildTime = System.currentTimeMillis() - buildTime;
			mainLog.println("Time for converting: " + buildTime / 1000.0 + " seconds.");
			// build transformation
			BasicModelTransformation<explicit.CTMC, explicit.CTMC> sparseTransformation = new BasicModelTransformation<>(transformation.getTransformedModel(), transformedModelSparse, transformation.getTransformedStatesOfInterest());
			sparseTransformation = sparseTransformation.compose(transformation);
			// attach transformed expression
			Expression originalExpression    = transformation.getOriginalExpression();
			Expression transformedExpression = transformation.getTransformedExpression();
			return new BasicModelExpressionTransformation<>(sparseTransformation, originalExpression, transformedExpression);
		}
	}



	public static class DTMC extends ConditionalMCModelChecker<explicit.DTMC, DTMCModelChecker>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		protected SortedSet<ConditionalTransformerType> getTransformerTypes() throws PrismException
		{
			String specification = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_DTMC);
			return ConditionalTransformerType.getValuesOf(specification);
		}

		@Override
		protected ConditionalTransformer<explicit.DTMC, DTMCModelChecker> getTransformer(ConditionalTransformerType type)
		{
			switch (type) {
			case Until:
				return new MCUntilTransformer.DTMC(modelChecker);
			case Next:
				return new MCNextTransformer.DTMC(modelChecker);
			case Ltl:
				return new MCLtlTransformer.DTMC(modelChecker);
			case FinallyFinally:
				return new FinallyUntilTransformer.DTMC(modelChecker);
			case LtlFinally:
				return new LtlUntilTransformer.DTMC(modelChecker);
			case FinallyLtl:
				return new FinallyLtlTransformer.DTMC(modelChecker);
			case LtlLtl:
				return  new LtlLtlTransformer.DTMC(modelChecker);
			case Quotient:
				return new MCQuotientTransformer.DTMC(modelChecker);
			default:
				return null;
			}
		}

		@Override
		protected ModelExpressionTransformation<explicit.DTMC, ? extends explicit.DTMC> convertVirtualModel(ModelExpressionTransformation<explicit.DTMC, ? extends explicit.DTMC> transformation)
		{
			mainLog.println("\nConverting simple/virtual model to " + DTMCSparse.class.getSimpleName());
			long buildTime = System.currentTimeMillis();
			explicit.DTMC transformedModel = transformation.getTransformedModel();
			DTMCSparse transformedModelSparse = new DTMCSparse(transformedModel);
			buildTime = System.currentTimeMillis() - buildTime;
			mainLog.println("Time for converting: " + buildTime / 1000.0 + " seconds.");
			// build transformation
			BasicModelTransformation<explicit.DTMC, explicit.DTMC> sparseTransformation = new BasicModelTransformation<>(transformation.getTransformedModel(), transformedModelSparse, transformation.getTransformedStatesOfInterest());
			sparseTransformation = sparseTransformation.compose(transformation);
			// attach transformed expression
			Expression originalExpression    = transformation.getOriginalExpression();
			Expression transformedExpression = transformation.getTransformedExpression();
			return new BasicModelExpressionTransformation<>(sparseTransformation, originalExpression, transformedExpression);
		}
	}
}