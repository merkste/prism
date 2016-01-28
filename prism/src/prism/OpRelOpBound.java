package prism;

import java.util.List;

import parser.ast.RelOp;
import explicit.MinMax;

/**
 * Class to represent info (operator, relational operator, bound, etc.) found in a P/R/S operator.
 */
public class OpRelOpBound
{
	protected String op;
	protected RelOp relOp;
	protected MinMax minMax;
	protected boolean numeric;
	protected double bound;
	protected List<Double> multipleBounds;

	/** Constructor, no bound */
	public OpRelOpBound(String op, MinMax minMax)
	{
		this.op = op;
		this.minMax = minMax;
		this.relOp = RelOp.COMPUTE_VALUES;
		numeric = true;
	}

	/** Constructor, with relOp and bound */
	public OpRelOpBound(String op, MinMax minMax, RelOp relOp, Double boundObject)
	{
		this.op = op;
		this.minMax = minMax;
		this.relOp = relOp;
		numeric = (boundObject == null);
		if (boundObject != null)
			bound = boundObject.doubleValue();
	}

	/** Constructor, with relOp and multiple bounds */
	public OpRelOpBound(String op, MinMax minMax, RelOp relOp, List<Double> bounds)
	{
		this.op = op;
		this.minMax = minMax;
		this.relOp = relOp;
		numeric = (bounds == null);
		multipleBounds = bounds;
	}

	public boolean isProbabilistic()
	{
		return "P".equals(op);
	}

	public boolean isReward()
	{
		return "R".equals(op);
	}

	public RelOp getRelOp()
	{
		return relOp;
	}

	public boolean isNumeric()
	{
		return numeric;
	}

	public double getBound()
	{
		if (multipleBounds != null) {
			if (multipleBounds.size() == 1) {
				return multipleBounds.get(0);
			}
			throw new UnsupportedOperationException("Multiple bounds, can not pick just one");
		}
		return bound;
	}
	
	public boolean hasMultipleBounds()
	{
		return multipleBounds != null && multipleBounds.size() > 1;
	}

	public List<Double> getBounds() {
		return multipleBounds;
	}
	
	public boolean hasExplicitMinMax()
	{
		return minMax != null;
	}

	public boolean isQualitative()
	{
		return !isNumeric() && op.equals("P") && (bound == 0 || bound == 1);
	}

	public boolean isTriviallyTrue()
	{
		if (!isNumeric() && op.equals("P")) {
			// >=0
			if (bound == 0 && relOp == RelOp.GEQ)
				return true;
			// <=1
			if (bound == 1 && relOp == RelOp.LEQ)
				return true;
		}
		return false;
	}

	public boolean isTriviallyFalse()
	{
		if (!isNumeric() && op.equals("P")) {
			// <0
			if (bound == 0 && relOp == RelOp.LT)
				return true;
			// >1
			if (bound == 1 && relOp == RelOp.GT)
				return true;
		}
		return false;
	}

	public MinMax getMinMax(ModelType modelType) throws PrismLangException
	{
		return getMinMax(modelType, true);
	}

	public MinMax getMinMax(ModelType modelType, boolean forAll) throws PrismLangException
	{
		if (this.minMax != null) {
			return this.minMax;
		}

		MinMax minMax = MinMax.blank();

		if (modelType.nondeterministic()) {
			if (!(modelType == ModelType.MDP || modelType == ModelType.CTMDP)) {
				throw new PrismLangException("Don't know how to model check " + getTypeOfOperator() + " properties for " + modelType + "s");
			}
			if (isNumeric()) {
				if (relOp == RelOp.COMPUTE_VALUES) {
					throw new PrismLangException("Can't use \"" + op + "=?\" for nondeterministic models; use e.g. \"" + op + "min=?\" or \"" + op + "max=?\"");
				}
			} else {
				if (forAll) {
					minMax = (relOp.isLowerBound() ) ? MinMax.min() : MinMax.max();
				} else {
					minMax = (relOp.isLowerBound() ) ? MinMax.max() : MinMax.min();
				}
			}
		}
		return minMax;
	}

	public String getTypeOfOperator()
	{
		String s = "";
		s += op + relOp;
		s += isNumeric() ? "?" : "p"; // TODO: always "p"?
		return s;
	}

	public String relOpBoundString()
	{
		return relOp.toString() + bound;
	}

	@Override
	public String toString()
	{
		return op + relOp.toString() + (isNumeric() ? "?" : bound);
	}
}
