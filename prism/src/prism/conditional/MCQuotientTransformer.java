package prism.conditional;

import explicit.LTLModelChecker;
import explicit.conditional.transformer.UndefinedTransformationException;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.Model;
import prism.ModelExpressionTransformation;
import prism.ModelTransformation;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;
import prism.StochModel;
import prism.StochModelChecker;
import prism.conditional.transform.BasicModelExpressionTransformation;

public abstract class MCQuotientTransformer<M extends ProbModel, C extends ProbModelChecker> extends ConditionalTransformer.MC<M, C>
{
	public MCQuotientTransformer(Prism prism, C modelChecker)
	{
		super(prism, modelChecker);
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
	public ModelExpressionTransformation<M, ? extends M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		ModelTransformation<M,M> modelTransformation = transformModel(model, expression, statesOfInterest);
		Expression transformedExpression             = transformExpression(expression);
		return new BasicModelExpressionTransformation<>(modelTransformation, expression, transformedExpression);
	}

	protected ModelTransformation<M,M> transformModel(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException, UndefinedTransformationException
	{
		return new ModelTransformation<M,M>() {

			@Override
			public M getOriginalModel()
			{
				return model;
			}

			@Override
			public M getTransformedModel()
			{
				return model;
			}

			@Override
			public void clear()
			{
				JDD.Deref(statesOfInterest);
			}

			@Override
			public StateValues projectToOriginalModel(StateValues svTransformedModel) throws PrismException
			{
				return svTransformedModel;
			}

			@Override
			public JDDNode getTransformedStatesOfInterest()
			{
				return statesOfInterest.copy();
			}
		};
	}

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



	public static class CTMC extends MCQuotientTransformer<StochModel, StochModelChecker> implements ConditionalTransformer.CTMC
	{
		public CTMC(Prism prism, StochModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}
	}



	public static class DTMC extends MCQuotientTransformer<ProbModel, ProbModelChecker> implements ConditionalTransformer.DTMC
	{
		public DTMC(Prism prism, ProbModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}
	}
}
