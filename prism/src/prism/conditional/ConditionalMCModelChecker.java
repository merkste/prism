package prism.conditional;

import explicit.conditional.ConditionalTransformerType;
import explicit.conditional.transformer.UndefinedTransformationException;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLongRun;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.ModelChecker;
import prism.ModelExpressionTransformation;
import prism.OpRelOpBound;
import prism.Prism;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;
import prism.StateValuesMTBDD;
import prism.StochModel;
import prism.StochModelChecker;
import prism.conditional.quotient.MCQuotientTransformer;
import prism.conditional.reset.FinallyLtlTransformer;
import prism.conditional.reset.FinallyUntilTransformer;
import prism.conditional.reset.LtlLtlTransformer;
import prism.conditional.reset.LtlUntilTransformer;
import prism.conditional.scale.MCLtlTransformer;
import prism.conditional.scale.MCNextTransformer;
import prism.conditional.scale.MCUntilTransformer;

//FIXME ALG: add comment
public abstract class ConditionalMCModelChecker<M extends ProbModel, C extends ProbModelChecker> extends ConditionalModelChecker<M,C>
{
	public ConditionalMCModelChecker(Prism prism, C modelChecker)
	{
		super(modelChecker, prism);
	}

	@Override
	public StateValues checkExpression(M model, ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		ConditionalTransformer<M,C> transformer = selectModelTransformer(model, expression);
		if (transformer == null) {
			if (expression.getObjective() instanceof ExpressionLongRun) {
				// try alternative long-run approach
				ProbModelChecker mc       = (ProbModelChecker) modelChecker.createModelChecker(model);
				ExpressionLongRun longrun = (ExpressionLongRun) expression.getObjective();
				return mc.checkConditionalExpressionLongRun(longrun, expression.getCondition(), statesOfInterest);
			}
			JDD.Deref(statesOfInterest);
			throw new PrismNotSupportedException("Cannot model check " + expression);
		}

		ModelExpressionTransformation<M, ? extends M> transformation;
		try {
			prism.getLog().println("\nTransforming model (using " + transformer.getName() + ") for condition: " + expression.getCondition());
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
			if (expression.getObjective() instanceof ExpressionLongRun) {
				// try alternative long-run approach
				StochModelChecker mc      = (StochModelChecker) modelChecker.createModelChecker(model);
				ExpressionLongRun longrun = (ExpressionLongRun) expression.getObjective();
				return mc.checkConditionalExpressionLongRun(longrun, expression.getCondition(), statesOfInterest);
			}
			if (!(expression.getObjective() instanceof ExpressionProb)) {
				throw new PrismException("Cannot model check " + expression);
			}
			ExpressionProb objective = (ExpressionProb) expression.getObjective();
			if (Expression.containsTemporalTimeBounds(objective.getExpression())) {
				throw new PrismException("Cannot model check " + expression);
			}
			prism.getLog().println();
			prism.getLog().println("Condition contains time bounds, trying Bayes' rule.");
			return checkExpressionBayes(model, expression, statesOfInterest);
		}

		protected StateValues checkExpressionBayes(StochModel model, ExpressionConditional expression, JDDNode statesOfInterest)
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
			ModelChecker mc     = modelChecker.createModelChecker(model);
			OpRelOpBound opInfo = expression.getRelopBoundInfo(mc.getConstantValues());
			if (!opInfo.isNumeric()) {
				int binOp = convertToBinaryOp(objective.getRelOp());
				transformed = new ExpressionBinaryOp(binOp, Expression.Parenth(transformed), objective.getBound().deepCopy());
			}

			prism.getMainLog().println("Checking transformed expression: " + transformed);
			long timer     = System.currentTimeMillis();
			// check numerator
			StateValues result        = mc.checkExpression(pAB, statesOfInterest);
			StateValuesMTBDD resultA  = mc.checkExpression(pA, statesOfInterest).convertToStateValuesMTBDD();
			// change NaN to 0 in pAB for each pA==0
			JDDNode filter = JDD.Not(JDD.Equals(resultA.getJDDNode().copy(), 0.0));
			result.filter(filter, 0.0);
			JDD.Deref(filter);
			result.times(resultA);
			resultA.clear();
			// check denominator
			StateValues resultB  = mc.checkExpression(pB, statesOfInterest);
			result.divide(resultB);
			resultB.clear();
			// check bounds if necessary
			if (!opInfo.isNumeric()) {
				JDDNode bits = result.getBDDFromInterval(opInfo.getRelOp(), opInfo.getBound());
				bits         = JDD.And(bits, model.getReach().copy());
				result.clear();
				result = new StateValuesMTBDD(bits, model);
			}
			timer          = System.currentTimeMillis() - timer;
			prism.getMainLog().println("\nTime for model checking transformed expression: " + timer / 1000.0 + " seconds.");
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
			return prism.getSettings().getString(PrismSettings.CONDITIONAL_PATTERNS_CTMC);
		}

		@Override
		protected ConditionalTransformer.MC<StochModel, StochModelChecker> getTransformer(ConditionalTransformerType type)
		{
			switch (type) {
			case Until:
				return new MCUntilTransformer.CTMC(prism, modelChecker);
			case Next:
				return new MCNextTransformer.CTMC(prism, modelChecker);
			case Ltl:
				return new MCLtlTransformer.CTMC(prism, modelChecker);
			case FinallyFinally:
				return new FinallyUntilTransformer.CTMC(prism, modelChecker);
			case LtlFinally:
				return new LtlUntilTransformer.CTMC(prism, modelChecker);
			case FinallyLtl:
				return new FinallyLtlTransformer.CTMC(prism, modelChecker);
			case LtlLtl:
				return new LtlLtlTransformer.CTMC(prism, modelChecker);
			case Quotient:
				return new MCQuotientTransformer.CTMC(prism, modelChecker);
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
		protected String getConditionalPatterns()
		{
			return prism.getSettings().getString(PrismSettings.CONDITIONAL_PATTERNS_DTMC);
		}

		@Override
		protected ConditionalTransformer.MC<ProbModel, ProbModelChecker> getTransformer(ConditionalTransformerType type)
		{
			switch (type) {
			case Until:
				return new MCUntilTransformer.DTMC(prism, modelChecker);
			case Next:
				return new MCNextTransformer.DTMC(prism, modelChecker);
			case Ltl:
				return new MCLtlTransformer.DTMC(prism, modelChecker);
			case FinallyFinally:
				return new FinallyUntilTransformer.DTMC(prism, modelChecker);
			case LtlFinally:
				return new LtlUntilTransformer.DTMC(prism, modelChecker);
			case FinallyLtl:
				return new FinallyLtlTransformer.DTMC(prism, modelChecker);
			case LtlLtl:
				return new LtlLtlTransformer.DTMC(prism, modelChecker);
			case Quotient:
				return new MCQuotientTransformer.DTMC(prism, modelChecker);
			default:
				return null;
			}
		}
	}
}
