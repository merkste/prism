package parser.ast;

import java.util.List;

import explicit.MinMax;
import explicit.Model;
import explicit.rewards.Rewards;
import param.BigRational;
import parser.EvaluateContext;
import parser.Values;
import parser.type.TypeDouble;
import parser.visitor.ASTVisitor;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;

/**
 * provides methods to store Quantiles
 * @author mdaum
 */
public class ExpressionQuantileProbNormalForm extends Expression
{
	private boolean existential = false;
	private ExpressionProb innerFormula = null;
	private Object rewardStructIndex = null;
	private String quantileVariable = "_q";
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
	/**
	 * does the quantile picks the minimal or maximal probabilities when it comes to resolving nondeterminism?
	 */
	private Boolean pickMinimum = null;
	/**
	 * does the quantile use a lower reward bound or an upper reward bound?
	 */
	private Boolean usesLowerRewardBound = null;

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

	public void setInnerFormula(Expression formula)
	{
		//since the parser only allows ExpressionProb for the inner formula of a quantile there should be no casting failure
		this.innerFormula = (ExpressionProb) formula;
	}

	public void setRewardStructIndex(Object index)
	{
		rewardStructIndex = index;
	}

	public void setQuantileVariable(String name)
	{
		quantileVariable = name;
	}

	// Get methods
	public boolean isExistential()
	{
		return existential;
	}

	public boolean isUniversal()
	{
		return !existential;
	}

	public String getQuantileVariable()
	{
		return quantileVariable;
	}

	public ExpressionProb getInnerFormula()
	{
		return innerFormula;
	}

	public int getResultAdjustment()
	{
		return resultAdjustment;
	}

	public boolean chooseIntervalUpperBound()
	{
		return chooseIntervalUpperBound;
	}

	public boolean usesReachability()
	{
		//XXX: diese Methode sollte benutzt werden, um Rechenaufwand einzusparen!!!
		ExpressionTemporal expressionTemporal = (ExpressionTemporal) innerFormula.getExpression();
		switch (expressionTemporal.op) {
		case ExpressionTemporal.P_F:
			return true;
		default:
			return false;
		}
	}

	public Object getRewardStructIndex()
	{
		return rewardStructIndex;
	}

	public Rewards buildRewardStructure(Model model, ModulesFile modulesFile, Values constantValues, PrismLog log) throws PrismException
	{
		return ExpressionQuantileHelpers.buildRewardStructure(model, modulesFile, constantValues, log, rewardStructIndex);
	}

	public boolean pickMinimum() throws PrismException
	{
		if (pickMinimum == null) {
			//if the correct value has not been determined before, just calculate it and return it
			if (usesUpperRewardBound()) {
				if (!getProbabilityRelation().equals(RelOp.GT) && !getProbabilityRelation().equals(RelOp.GEQ) && !getProbabilityRelation().equals(RelOp.COMPUTE_VALUES)){
					throw new PrismException("QuantileNormalForm only supports upper reward bound in combination with >, >= or = p");
				}
				//existential & upper reward bound  ==>  use maximum
				//universal   & upper reward bound  ==>  use minimum
				pickMinimum = isUniversal();
				return pickMinimum;
			}
			assert (usesLowerRewardBound()) : "There must be an upper or a lower reward bound!";
			if (!getProbabilityRelation().equals(RelOp.LT) && !getProbabilityRelation().equals(RelOp.LEQ) && !getProbabilityRelation().equals(RelOp.COMPUTE_VALUES)){
				throw new PrismException("QuantileNormalForm only supports lower reward bound in combination with <, <= or = p");
			}
			//existential & lower reward bound  ==>  use minimum
			//universal   & lower reward bound  ==>  use maximum
			pickMinimum = isExistential();
			return pickMinimum;
		}
		//if the correct value has been determined before, just return it
		return pickMinimum;
	}

	public boolean pickMaximum() throws PrismException
	{
		return !pickMinimum();
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
		return "Quantile";
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	private boolean isDualQuantileNeccessary()
	{
		//XXX: nochmal ueberarbeiten
		RelOp relation = getProbabilityRelation();
		if (usesLowerRewardBound)
			return (relation.equals(RelOp.GT) || relation.equals(RelOp.GEQ));
		return (relation.equals(RelOp.LT) || relation.equals(RelOp.LEQ));
	}

	public RelOp getProbabilityRelation()
	{
		return innerFormula.getRelOp();
	}

	public List<Double> getProbabilityThresholds(Values constantValues) throws PrismException
	{
		return ExpressionQuantileHelpers.initialiseThresholds(innerFormula.getProb(), constantValues, usesUpperRewardBound());
	}

	public boolean usesLowerRewardBound() throws PrismException
	{
		if (usesLowerRewardBound == null)
			try {
				ExpressionTemporal expressionTemporal = (ExpressionTemporal) innerFormula.getExpression();

				assert (expressionTemporal.getBounds().countBounds() == 1);
				TemporalOperatorBound bound = expressionTemporal.getBounds().getBounds().get(0);

				if (bound.hasLowerBound()) {
					usesLowerRewardBound = true;
					return usesLowerRewardBound;
				}
				if (bound.hasUpperBound()) {
					usesLowerRewardBound = false;
					return usesLowerRewardBound;
				}
				throw new PrismException("There is neither a lower nor an upper reward bound specified for the quantile");
			} catch (ClassCastException classCastException) {
				throw new PrismException("The encapsulated formula of an inner formula inside a quantile should have the type ExpressionTemporal");
			}
		return usesLowerRewardBound;
	}

	public boolean usesUpperRewardBound() throws PrismException
	{
		return !usesLowerRewardBound();
	}

	public boolean isIncreasing() throws PrismException
	{
		return usesUpperRewardBound();
	}

	public boolean isDecreasing() throws PrismException
	{
		return !isIncreasing();
	}

	public static boolean isQuantitativeQuery(List<Double> thresholds)
	{
		//XXX: so far if multiple thresholds are given we want to use the quantitative calculation scheme
		if (thresholds.size() > 1)
			return true;
		double probabilityThreshold = thresholds.get(0);
		if (probabilityThreshold != 0 && probabilityThreshold != 1)
			return true;
		return false;
	}

	public static boolean isQualitativeQuery(List<Double> thresholds)
	{
		return !isQuantitativeQuery(thresholds);
	}

	public ExpressionQuantileProbNormalForm getDualQuantile() throws PrismException
	{
		//XXX: nochmal ueberarbeiten
		ExpressionQuantileProbNormalForm dual = this.deepCopy();
		if (existential) {
			dual.setUniversal();
		} else {
			dual.setExistential();
		}
		dual.innerFormula.setRelOp(getProbabilityRelation().negate());
		return dual;
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
		s.append("Quantile (");
		s.append(existential ? "Ex" : "Un");
		s.append(", ");
		s.append(innerFormula);
		if (rewardStructIndex != null) {
			s.append(", R{");
			if (rewardStructIndex instanceof String)
				s.append("\"");
			s.append(rewardStructIndex);
			if (rewardStructIndex instanceof String)
				s.append("\"");
			s.append("}");
		}
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
	public ExpressionQuantileProbNormalForm deepCopy()
	{
		ExpressionQuantileProbNormalForm expr = new ExpressionQuantileProbNormalForm();
		if (existential) {
			expr.setExistential();
		} else {
			expr.setUniversal();
		}
		expr.setQuantileVariable(new String(getQuantileVariable()));
		expr.setResultAdjustment(getResultAdjustment());
		expr.setChooseIntervalUpperBound(chooseIntervalUpperBound());
		expr.setInnerFormula(innerFormula == null ? null : (ExpressionProb) innerFormula.deepCopy());
		expr.setType(type);
		expr.setPosition(this);
		if (rewardStructIndex != null && rewardStructIndex instanceof Expression) {
			expr.setRewardStructIndex(((Expression) rewardStructIndex).deepCopy());
		} else {
			expr.setRewardStructIndex(rewardStructIndex);
		}
		expr.pickMinimum = (pickMinimum == null) ? null : pickMinimum.booleanValue();
		expr.usesLowerRewardBound = (usesLowerRewardBound == null) ? null : usesLowerRewardBound.booleanValue();
		return expr;
	}

	/** Return an equivalent ExpressionQuantile (with deep-copy of the path expression)
	 * @throws PrismException */
	public ExpressionQuantileProb toExpressionQuantile(ModelType modelType, List<Double> thresholds) throws PrismException
	{
		ExpressionQuantileProb result = new ExpressionQuantileProb();

		ExpressionProb eProb = new ExpressionProb();
		if (modelType == ModelType.MDP) {
			eProb.setMinMax(pickMinimum() ? MinMax.min() : MinMax.max());
		}
		if (thresholds.size() == 1)
			eProb.setProb(new ExpressionLiteral(TypeDouble.getInstance(), thresholds.get(0)));
		else {
			ExpressionMultipleThresholds multipleThresholds = new ExpressionMultipleThresholds();
			for (double threshold : thresholds)
				multipleThresholds.addThreshold(new ExpressionLiteral(TypeDouble.getInstance(), threshold));
			eProb.setProb(new ExpressionMultipleThresholds());
		}
		eProb.setRelOp(getProbabilityRelation());
		eProb.setExpression(getInnerFormula().getExpression().deepCopy());

		ExpressionTemporal exprTemp = getTemporalOperatorForSimplePathFormula(eProb.getExpression());
		TemporalOperatorBound bound = exprTemp.getBounds().getDefaultBound();
		assert (bound != null) : "Can only transform a normal form quantile with a default bound";

		if (getRewardStructIndex() != null) {
			// replace default bound with a correct reward bound
			TemporalOperatorBound rBound = new TemporalOperatorBound();
			rBound.setBoundType(TemporalOperatorBound.BoundType.REWARD_BOUND);
			rBound.setLowerBound(bound.getLowerBound(), bound.lowerBoundIsStrict());
			rBound.setUpperBound(bound.getUpperBound(), bound.upperBoundIsStrict());
			rBound.setRewardStructureIndex(getRewardStructIndex());

			exprTemp.getBounds().getBounds().remove(bound);
			exprTemp.getBounds().addBound(rBound);
		} else {
			// temporal bound = nothing to do
		}

		result.setMinMax(MinMax.min()); // always min
		result.setInnerFormula(eProb);
		result.setQuantileVariable(quantileVariable);
		result.setResultAdjustment(resultAdjustment);
		result.setChooseIntervalUpperBound(chooseIntervalUpperBound);

		return result;
	}

	public ExpressionTemporal getUnboundedExpressionTemporal() throws PrismException
	{
		final ExpressionTemporal temporal = (ExpressionTemporal) ((ExpressionTemporal) innerFormula.getExpression().deepCopy()).convertToUntilForm();
		// TODO: JK refactor
		temporal.getBounds().getBounds().clear();
		return temporal;
	}

	public void setResultAdjustment(final int i)
	{
		resultAdjustment = i;
	}

	public void setChooseIntervalUpperBound(final boolean chooseUpperBound)
	{
		chooseIntervalUpperBound = chooseUpperBound;
	}
}