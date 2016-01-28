package parser.ast;

import java.util.List;

import explicit.Model;
import explicit.rewards.Rewards;
import parser.EvaluateContext;
import parser.Values;
import parser.visitor.ASTVisitor;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;

/**
 * provides methods to store Quantiles
 * @author mdaum
 */
public class ExpressionQuantileExpNormalForm extends Expression
{
	private boolean existential = false;
	private Object costRewardStructIndex = null;
	private Object valueRewardStructIndex = null;
	private Expression thresholdExpression = null;

	// the empty Constructor is used, therefore I can skip this one
	// Set methods
	public void setExistential()
	{
		existential = true;
	}

	public void setUniversal()
	{
		existential = false;
	}

	public boolean isExistential()
	{
		return existential;
	}

	public boolean isUniversal()
	{
		return !existential;
	}

	public void setCostRewardStructIndex(Object index)
	{
		costRewardStructIndex = index;
	}

	public void setValueRewardStructIndex(Object index)
	{
		valueRewardStructIndex = index;
	}

	public void setThresholdExpression(Expression e)
	{
		thresholdExpression = e;
	}

	// Get methods
	public boolean pickMaximum()
	{
		return existential;
	}

	public boolean pickMinimum()
	{
		return !existential;
	}

	public Object getCostRewardStructIndex()
	{
		return costRewardStructIndex;
	}

	public Object getValueRewardStructIndex()
	{
		return valueRewardStructIndex;
	}

	public Rewards buildCostRewardStructure(Model model, ModulesFile modulesFile, Values constantValues, PrismLog log) throws PrismException
	{
		return ExpressionQuantileHelpers.buildRewardStructure(model, modulesFile, constantValues, log, costRewardStructIndex);
	}

	public Rewards buildValueRewardStructure(Model model, ModulesFile modulesFile, Values constantValues, PrismLog log) throws PrismException
	{
		return ExpressionQuantileHelpers.buildRewardStructure(model, modulesFile, constantValues, log, valueRewardStructIndex);
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
	public String getResultName()
	{
		return "Quantile";
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	public List<Double> getThresholds(Values constantValues) throws PrismException
	{
		return ExpressionQuantileHelpers.initialiseThresholds(thresholdExpression, constantValues, true);
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
		s.append("expQuantile (");
		s.append(existential ? "Ex" : "Un");
		s.append(", ExpUtil ");
		if (valueRewardStructIndex != null) {
			s.append("R{");
			if (valueRewardStructIndex instanceof String)
				s.append("\"");
			s.append(valueRewardStructIndex);
			if (valueRewardStructIndex instanceof String)
				s.append("\"");
			s.append("}");
		}
		s.append(" > ");
		s.append(thresholdExpression);
		s.append(" (");
		if (costRewardStructIndex != null) {
			s.append("R{");
			if (costRewardStructIndex instanceof String)
				s.append("\"");
			s.append(costRewardStructIndex);
			if (costRewardStructIndex instanceof String)
				s.append("\"");
			s.append("}");
		}
		s.append(" <= ?))");
		return s.toString();
	}

	@Override
	public ExpressionQuantileExpNormalForm deepCopy()
	{
		ExpressionQuantileExpNormalForm expr = new ExpressionQuantileExpNormalForm();
		if (existential) {
			expr.setExistential();
		} else {
			expr.setUniversal();
		}
		expr.setType(type);
		expr.setThresholdExpression(thresholdExpression == null ? null : thresholdExpression.deepCopy());
		expr.setPosition(this);
		if (costRewardStructIndex != null && costRewardStructIndex instanceof Expression) {
			expr.setCostRewardStructIndex(((Expression) costRewardStructIndex).deepCopy());
		} else {
			expr.setCostRewardStructIndex(costRewardStructIndex);
		}
		if (valueRewardStructIndex != null && valueRewardStructIndex instanceof Expression) {
			expr.setValueRewardStructIndex(((Expression) valueRewardStructIndex).deepCopy());
		} else {
			expr.setValueRewardStructIndex(valueRewardStructIndex);
		}
		return expr;
	}
}