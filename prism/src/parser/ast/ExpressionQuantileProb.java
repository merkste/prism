package parser.ast;

import explicit.MinMax;
import param.BigRational;
import parser.EvaluateContext;
import parser.visitor.ASTVisitor;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;

/**
 * provides methods to store Quantiles
 * @author mdaum
 */
public class ExpressionQuantileProb extends Expression
{
	private MinMax minMax = null;
	private Expression innerFormula = null;
	private String quantileVariable = null;
	/**
	 * exclusive for quantiles in discrete-time models (DTMC, MDP)
	 * defines the value which needs to be subtracted or added to the calculated value in order to get the correct value
	 */
	private int resultAdjustment = 0;
	/**
	 * exclusive for quantiles in continuous-time models (CTMC)
	 * the binary search used for the computation restricts the quantile to an epsilon-interval
	 * depending on the demanded quantile either the lower or the upper value is picked as the result
	 */
	private boolean chooseIntervalUpperBound = true;

	public void setInnerFormula(Expression formula)
	{
		this.innerFormula = formula;
		return;
	}

	public void setQuantileVariable(String quantileVariable)
	{
		this.quantileVariable = quantileVariable;
	}

	public void setMinMax(MinMax minMax)
	{
		this.minMax = minMax;
	}

	public void setResultAdjustment(final int resultAdjustment)
	{
		this.resultAdjustment = resultAdjustment;
	}

	public void setChooseIntervalUpperBound(final boolean chooseUpperBound)
	{
		chooseIntervalUpperBound = chooseUpperBound;
	}

	public boolean chooseIntervalUpperBound()
	{
		return chooseIntervalUpperBound;
	}

	// Get methods
	public MinMax getMinMax()
	{
		return minMax;
	}

	public Expression getInnerFormula()
	{
		return innerFormula;
	}

	public String getQuantileVariable()
	{
		return quantileVariable;
	}

	// Methods required for Expression:
	@Override
	public boolean isConstant()
	{
		return false;
	}

	@Override
	public boolean isProposition()
	{
		return false;
	}

	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a Quantile operator without a model");
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a Quantile operator without a model");
	}

	@Override
	public String getResultName()
	{
		// TODO XXX more descriptive
		return "Quantile value";
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	// Methods required for ASTElement:
	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public String toString()
	{
		StringBuffer s = new StringBuffer();
		s.append("quantile(");
		s.append(minMax);
		s.append(" " + quantileVariable);
		s.append(", ");
		s.append(innerFormula);
		s.append(")");
		
		if (resultAdjustment != 0){
			s.append(" ");
			if (resultAdjustment > 0)
				s.append("+");
			s.append(resultAdjustment);
		}
		return s.toString();
	}

	@Override
	public ExpressionQuantileProb deepCopy()
	{
		ExpressionQuantileProb expr = new ExpressionQuantileProb();
		expr.setInnerFormula(innerFormula == null ? null : (Expression) innerFormula.deepCopy());
		expr.setQuantileVariable(new String(quantileVariable));
		expr.setMinMax(minMax.clone());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}

	/**
	 * substitutes the occurences of the quantile variable and return the inner ExpressionProb resulting from this substitution
	 * @param value substitute quantile variable with this value
	 * @return resulting ExpressionProb after quantile parameter was substituted
	 */
	public Expression getInstantiatedExpression(final int quantileValue)
			throws PrismLangException
	{
		Expression innerFormulaCopy = innerFormula.deepCopy();
		innerFormulaCopy.replaceBoundVariable(getQuantileVariable(), quantileValue);
		return innerFormulaCopy;
	}

	public Expression getInstantiatedExpression(final double quantileValue)
			throws PrismLangException
	{
		Expression innerFormulaCopy = innerFormula.deepCopy();
		innerFormulaCopy.replaceBoundVariable(getQuantileVariable(), quantileValue);
		return innerFormulaCopy;
	}

	public Expression getInstantiatedExpressionForDoubleResults(final int quantileValue)
			throws PrismLangException
	{
		ExpressionProb innerFormulaCopy = (ExpressionProb) innerFormula.deepCopy();
		innerFormulaCopy.replaceBoundVariable(getQuantileVariable(), quantileValue);
		innerFormulaCopy.setRelOp(RelOp.COMPUTE_VALUES);
		innerFormulaCopy.setBound(null);
		return innerFormulaCopy;
	}

	public Expression getInstantiatedExpressionForDoubleResults(final double quantileValue)
			throws PrismLangException
	{
		ExpressionProb innerFormulaCopy = (ExpressionProb) innerFormula.deepCopy();
		innerFormulaCopy.replaceBoundVariable(getQuantileVariable(), quantileValue);
		innerFormulaCopy.setRelOp(RelOp.COMPUTE_VALUES);
		innerFormulaCopy.setBound(null);
		return innerFormulaCopy;
	}

	public Expression getInstantiatedExpression(final double value, final ModelType type)
			throws PrismException
	{
		if (type == ModelType.CTMC){
			return getInstantiatedExpression(value);
		}
		assert (type == ModelType.DTMC | type == ModelType.MDP);
		assert (value - ((int) value) == 0.0);
		return getInstantiatedExpression((int) value);
	}
}