package parser.ast;

import java.util.ArrayList;
import java.util.List;

import param.BigRational;
import parser.EvaluateContext;
import parser.Values;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class ExpressionMultipleThresholds extends Expression
{
	List<Expression> thresholds = new ArrayList<Expression>();

	@Override
	public boolean isConstant()
	{
		for (Expression threshold : thresholds) {
			if (!threshold.isConstant()) return false;
		}
		return true;
	}

	@Override
	public boolean isProposition()
	{
		// not applicable
		return false;
	}

	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		if (countThresholds() != 1) {
			throw new PrismLangException("Multiple thresholds can not be evaluated to a single value...");
		} else {
			return thresholds.get(0).evaluate(ec);
		}
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		if (countThresholds() != 1) {
			throw new PrismLangException("Multiple thresholds can not be evaluated to a single value...");
		} else {
			return thresholds.get(0).evaluateExact(ec);
		}
	}

	@Override
	public boolean returnsSingleValue()
	{
		// not applicable
		return false;
	}

	@Override
	public ExpressionMultipleThresholds deepCopy()
	{
		ExpressionMultipleThresholds result = new ExpressionMultipleThresholds();

		for (Expression threshold : thresholds) {
			result.addThreshold(threshold.deepCopy());
		}

		return result;
	}

	public List<Double> expandAndEvaluate(Values constantValues) throws PrismLangException
	{
		List<Double> result = new ArrayList<Double>();
		for (Expression threshold : thresholds) {
			if (threshold instanceof ExpressionThresholdRange){
				for (Double t : ((ExpressionThresholdRange) threshold).expandAndEvaluate(constantValues)){
					result.add(t);
				}
			} else {
				double evaluatedThreshold = threshold.evaluateDouble(constantValues);
				if (evaluatedThreshold < 0){
					throw new PrismLangException("Negative thresholds are prohibited: " + evaluatedThreshold);
				}
				result.add(evaluatedThreshold);
			}
		}
		return result;
	}

	public void addThreshold(Expression threshold)
	{
		thresholds.add(threshold);
	}

	public List<Expression> getThresholds()
	{
		return thresholds;
	}

	public Expression getThreshold(int i)
	{
		return thresholds.get(i);
	}

	public void setThreshold(int i, Expression threshold)
	{
		thresholds.set(i, threshold);
	}

	public int countThresholds()
	{
		return thresholds.size();
	}

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public String toString()
	{
		StringBuffer s = new StringBuffer();
		s.append("{");
		boolean first = true;
		for (Expression threshold : thresholds) {
			if (!first) s.append(",");
			first = false;

			s.append(threshold);
		}
		s.append("}");
		return s.toString();
	}

}
