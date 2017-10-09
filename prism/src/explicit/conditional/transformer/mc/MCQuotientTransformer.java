package explicit.conditional.transformer.mc;

import java.util.BitSet;

import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import explicit.BasicModelExpressionTransformation;
import explicit.BasicModelTransformation;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker;
import explicit.Model;
import explicit.ModelTransformation;

// FIXME ALG: add comment
public class MCQuotientTransformer extends MCConditionalTransformer
{
	public MCQuotientTransformer(DTMCModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	public boolean canHandleObjective(Model model, ExpressionConditional expression)
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
	public boolean canHandleCondition(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		// FIXME ALG: Should check whether formula can be turned into ExpressionProb
		return LTLModelChecker.isSupportedLTLFormula(model.getModelType(), expression.getCondition());
	}

	@Override
	public BasicModelExpressionTransformation<explicit.DTMC, explicit.DTMC> transform(explicit.DTMC model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		ModelTransformation<explicit.DTMC,explicit.DTMC> modelTransformation = transformModel(model, expression, statesOfInterest);
		Expression transformedExpression             = transformExpression(expression);
		return new BasicModelExpressionTransformation<explicit.DTMC, explicit.DTMC>(modelTransformation, expression, transformedExpression);
	}

	@Override
	protected BasicModelTransformation<explicit.DTMC, explicit.DTMC> transformModel(explicit.DTMC model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		return new BasicModelTransformation<explicit.DTMC, explicit.DTMC>(model, model, statesOfInterest);
	}

	@Override
	protected Expression transformExpression(ExpressionConditional expression)
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
}