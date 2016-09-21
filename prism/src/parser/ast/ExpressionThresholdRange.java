package parser.ast;

import java.util.ArrayList;
import java.util.List;

import common.Helpers;
import param.BigRational;
import parser.EvaluateContext;
import parser.Values;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class ExpressionThresholdRange extends Expression
{
	Expression lower;
	Expression upper;
	Expression step;

	public ExpressionThresholdRange(Expression lower, Expression upper, Expression step)
	{
		this.lower = lower;
		this.upper = upper;
		this.step = step;
	}

	public Expression getLower()
	{
		return lower;
	}

	public Expression getUpper()
	{
		return upper;
	}

	public Expression getStep()
	{
		return step;
	}

	public void setLower(Expression lower)
	{
		this.lower = lower;
	}

	public void setUpper(Expression upper)
	{
		this.upper = upper;
	}

	public void setStep(Expression step)
	{
		this.step = step;
	}

	public List<Double> expandAndEvaluate(Values constantValues) throws PrismLangException
	{
		List<Double> result = new ArrayList<Double>();

		double lowerD = lower.evaluateDouble(constantValues);
		double upperD = upper.evaluateDouble(constantValues);
		double stepD  = step.evaluateDouble(constantValues);

		if (!(stepD > 0)) {
			throw new PrismLangException("Step in threshold is not positive, "+step);
		}
		
		int maxDecimalPower = Math.max(Helpers.getDecimalPowerForDouble2IntegerConversion(stepD), Helpers.getDecimalPowerForDouble2IntegerConversion(lowerD));
		maxDecimalPower = Math.max(maxDecimalPower, Helpers.getDecimalPowerForDouble2IntegerConversion(upperD));
		int stepI = (int) (stepD * maxDecimalPower);
		int lowerI = (int) (lowerD * maxDecimalPower);
		int upperI = (int) (upperD * maxDecimalPower);
		
		for (int threshold = lowerI; threshold <= upperI; threshold += stepI){
			result.add((double) threshold / maxDecimalPower);
		}

		return result;
	}

	@Override
	public boolean isConstant()
	{
		return lower.isConstant() && upper.isConstant() && step.isConstant();
	}

	@Override
	public boolean isProposition()
	{
		return false;
	}

	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Threshold range can not be evaluated to a single value...");
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Threshold range can not be evaluated to a single value...");
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	@Override
	public ExpressionThresholdRange deepCopy()
	{
		return new ExpressionThresholdRange(lower.deepCopy(), upper.deepCopy(), step.deepCopy());
	}

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public String toString()
	{
		return "[" + lower +" : " + upper + " : " + step + "]";
	}

}
