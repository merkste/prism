package explicit.conditional;

import java.util.BitSet;

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
import explicit.CTMCModelChecker;
import explicit.CTMCSparse;
import explicit.DTMCModelChecker;
import explicit.DTMCSparse;
import explicit.ModelExpressionTransformation;
import explicit.ProbModelChecker;
import explicit.StateValues;
import explicit.conditional.quotient.MCQuotientTransformer;
import explicit.conditional.reset.FinallyLtlTransformer;
import explicit.conditional.reset.FinallyUntilTransformer;
import explicit.conditional.reset.LtlLtlTransformer;
import explicit.conditional.reset.LtlUntilTransformer;
import explicit.conditional.scale.MCLtlTransformer;
import explicit.conditional.scale.MCNextTransformer;
import explicit.conditional.scale.MCUntilTransformer;
import explicit.conditional.transformer.UndefinedTransformationException;

// FIXME ALG: add comment
public abstract class ConditionalMCModelChecker<M extends explicit.DTMC, C extends ProbModelChecker> extends ConditionalModelChecker<M, C>
{
	public ConditionalMCModelChecker(C modelChecker)
	{
		super(modelChecker);
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
			mainLog.println("\nTransforming model (using " + transformer.getName() + ") for condition: " + expression);
			transformation = transformModel(transformer, model, expression, statesOfInterest);
		} catch (UndefinedTransformationException e) {
			mainLog.println("\nTransformation failed: " + e.getMessage());
			return createUndefinedStateValues(model, expression);
		}
		StateValues result = checkExpressionTransformedModel(transformation);
		return transformation.projectToOriginalModel(result);
	}



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
		protected String getConditionalPatterns()
		{
			return settings.getString(PrismSettings.CONDITIONAL_PATTERNS_CTMC);
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
		protected CTMCSparse convertToSparseModel(explicit.CTMC model)
		{
			return new CTMCSparse(model);
		}
	}



	public static class DTMC extends ConditionalMCModelChecker<explicit.DTMC, DTMCModelChecker>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		protected String getConditionalPatterns()
		{
			return settings.getString(PrismSettings.CONDITIONAL_PATTERNS_DTMC);
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
		protected DTMCSparse convertToSparseModel(explicit.DTMC model)
		{
			return new DTMCSparse(model);
		}
	}
}