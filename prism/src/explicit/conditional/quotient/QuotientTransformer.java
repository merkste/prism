package explicit.conditional.quotient;

import java.util.BitSet;

import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import explicit.BasicModelTransformation;
import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ProbModelChecker;
import explicit.conditional.ConditionalTransformer;
import explicit.conditional.transformer.BasicModelExpressionTransformation;

// FIXME ALG: add comment
public interface QuotientTransformer<M extends explicit.DTMC, C extends ProbModelChecker> extends ConditionalTransformer.MC<M,C>
{
	@Override
	default BasicModelExpressionTransformation<M, ? extends M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		ModelTransformation<M, ? extends M> transformation = transformModel(model, expression, statesOfInterest);
		Expression transformedExpression = transformExpression(expression);
		return new BasicModelExpressionTransformation<M, M>(transformation, expression, transformedExpression);
	}

	@Override
	default boolean canHandleObjective(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		// only prob formulae
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		return LTLModelChecker.isSupportedLTLFormula(model.getModelType(), objective.getExpression());
	}

	@Override
	default boolean canHandleCondition(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		// FIXME ALG: Should check whether formula can be turned into ExpressionProb
		return LTLModelChecker.isSupportedLTLFormula(model.getModelType(), expression.getCondition());
	}

	default BasicModelTransformation<M, M> transformModel(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		return new BasicModelTransformation<M, M>(model, model, statesOfInterest);
	}

	default Expression transformExpression(ExpressionConditional expression)
			throws PrismNotSupportedException
	{
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression condition     = expression.getCondition();
		
		// apply definition of conditional probability
		String computeValues           = RelOp.COMPUTE_VALUES.toString();
		ExpressionBinaryOp conjunction = Expression.And(Expression.Parenth(objective.getExpression().deepCopy()), Expression.Parenth(condition.deepCopy()));
		Expression numerator           = new ExpressionProb(conjunction, computeValues, null);
		Expression denominator         = new ExpressionProb(condition.deepCopy(), computeValues, null);
		ExpressionBinaryOp fraction    = Expression.Divide(numerator, denominator);

		// translate bounds if necessary
		if (objective.getBound() == null) {
			return fraction;
		}
		int binOp = convertToBinaryOp(objective.getRelOp());
		return new ExpressionBinaryOp(binOp, Expression.Parenth(fraction), objective.getBound().deepCopy());
	}

	default int convertToBinaryOp(RelOp relop) throws PrismNotSupportedException
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



	public static class CTMC extends ConditionalTransformer.Basic<explicit.CTMC, CTMCModelChecker> implements QuotientTransformer<explicit.CTMC, CTMCModelChecker>, ConditionalTransformer.CTMC
	{
		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}



	public static class DTMC extends ConditionalTransformer.Basic<explicit.DTMC, DTMCModelChecker> implements QuotientTransformer<explicit.DTMC, DTMCModelChecker>, ConditionalTransformer.DTMC
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}
}
