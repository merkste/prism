package explicit.quantile;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import explicit.CounterTransformation;
import explicit.MinMax;
import explicit.Model;
import explicit.ModelExpressionTransformation;
import explicit.ModelExpressionTransformationIdentity;
import explicit.ModelExpressionTransformationNested;
import explicit.ProbModelChecker;
import explicit.StateValues;
import parser.ast.Expression;
import parser.ast.ExpressionBoundVariable;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionQuantileProb;
import parser.ast.ExpressionQuantileProbNormalForm;
import parser.ast.ExpressionTemporal;
import parser.ast.RelOp;
import parser.ast.TemporalOperatorBound;
import parser.ast.TemporalOperatorBounds;
import parser.type.TypeInt;
import prism.ModelType;
import prism.OpRelOpBound;
import prism.PrismException;

public class QuantileTransformations
{
	private static ExpressionProb checkForExpressionProb(ExpressionQuantileProb e) throws PrismException
	{
		if (!(e.getInnerFormula() instanceof ExpressionProb))
			throw new PrismException("Can only handle P[...] expressions");
		return (ExpressionProb) e.getInnerFormula();
	}

	private static ExpressionTemporal checkForExpressionTemporal(Expression pathFormula) throws PrismException
	{
		if (!pathFormula.isSimplePathFormula())
			throw new PrismException("Can only handle simple path formulas");
		if (!(pathFormula instanceof ExpressionTemporal))
			throw new PrismException("Can not handle " + pathFormula);
		ExpressionTemporal exprTemp = (ExpressionTemporal) pathFormula;
		if (!(exprTemp.getOperator() == ExpressionTemporal.P_U || exprTemp.getOperator() == ExpressionTemporal.P_F))
			throw new PrismException("Can not handle " + pathFormula);
		return exprTemp;
	}

	private static ExpressionTemporal replaceByDefaultBound(final ExpressionTemporal exprTemp, final boolean hasLowerBound, final boolean boundIsStrict, final String quantileVariable)
	{
		TemporalOperatorBound defaultBound = new TemporalOperatorBound();
		if (hasLowerBound){
			defaultBound.setLowerBound(new ExpressionBoundVariable(quantileVariable, TypeInt.getInstance()), boundIsStrict);
		} else {
			defaultBound.setUpperBound(new ExpressionBoundVariable(quantileVariable, TypeInt.getInstance()), boundIsStrict);
		}
		TemporalOperatorBounds defaultBounds = new TemporalOperatorBounds();
		defaultBounds.setDefaultBound(defaultBound);
		ExpressionTemporal result = (ExpressionTemporal) exprTemp.deepCopy();
		result.setBounds(defaultBounds);
		return result;
	}

	public static <M extends Model> ModelExpressionTransformation<M, M> toNormalForm(final ProbModelChecker pmc, final M model, final ExpressionQuantileProb e,
			final BitSet statesOfInterest) throws PrismException
	{
		ExpressionProb innerFormula = checkForExpressionProb(e);
		Expression pathFormula = innerFormula.getExpression();
		ExpressionTemporal exprTemp = checkForExpressionTemporal(pathFormula);
		// is positive F or U, so until form is again ExpressionTemporal
		exprTemp = (ExpressionTemporal) exprTemp.convertToUntilForm();
		TemporalOperatorBounds bounds = exprTemp.getBounds();

		if (!bounds.hasBounds() || !Expression.hasBoundedVariables(exprTemp)) {
			// TODO XXX do simple computations + warning
			throw new PrismException("No quantile variable in bounds, no computation needed");
		}

		if (bounds.countBounds() > 1) {
			ModelExpressionTransformation<M, M> transformed = transformAdditionalBounds(pmc, model, e, statesOfInterest);
			ModelExpressionTransformation<M, M> normalForm = toNormalForm(pmc, transformed.getTransformedModel(),
					(ExpressionQuantileProb) transformed.getTransformedExpression(), transformed.getTransformedStatesOfInterest());
			return new ModelExpressionTransformationNested<M, M, M>(transformed, normalForm);
		}

		// There is only a single bound
		TemporalOperatorBound quantileBound = bounds.getBounds().get(0);

		if (quantileBound.hasLowerBound() && quantileBound.hasUpperBound())
			throw new PrismException("Can not handle " + exprTemp + ", simultaneous upper and lower bound");

		if (!quantileBound.hasLowerBound() && !quantileBound.hasUpperBound())
			throw new PrismException("Can not handle " + exprTemp + ", no bound (implementation error?)");

		if (quantileBound.hasLowerBound() && !(quantileBound.getLowerBound() instanceof ExpressionBoundVariable)) {
			throw new PrismException("Expected ExpressionBoundVariable in lower bound: " + quantileBound.getLowerBound());
		}

		if (quantileBound.hasUpperBound() && !(quantileBound.getUpperBound() instanceof ExpressionBoundVariable)) {
			throw new PrismException("Expected ExpressionBoundVariable in upper bound: " + quantileBound.getUpperBound());
		}

		boolean formulaLowerBound = quantileBound.hasLowerBound();
		boolean formulaBoundStrict = formulaLowerBound ? quantileBound.lowerBoundIsStrict() : quantileBound.upperBoundIsStrict();

		int adjustment = 0;

		if (formulaBoundStrict && model.getModelType()!=ModelType.CTMC) {
			// computation engine only supports non-strict bounds on temporal operator
			if (formulaLowerBound) {
				// make bound non-strict
				quantileBound.setLowerBound(quantileBound.getLowerBound(), false);
				adjustment--;
			} else {
				// make bound non-strict
				quantileBound.setUpperBound(quantileBound.getUpperBound(), false);
				adjustment++;
			}
		}

		boolean formulaIncreasing = !formulaLowerBound; // until & !lowerBound => increasing

		Object rewardStructIndex = null;
		if (quantileBound.isRewardBound()) {
			rewardStructIndex = quantileBound.getRewardStructureIndex();
		}

		MinMax minMaxQuantile = e.getMinMax();
		RelOp relOpP = innerFormula.getRelOp();

		boolean statePropertyIncreasing = (relOpP.isLowerBound() == formulaIncreasing);
		boolean trivial = (minMaxQuantile.isMin() != statePropertyIncreasing);

		if (trivial) {
			// TODO: support
			throw new PrismException("Quantile is trivial, computation not supported yet");
		}

		MinMax minMaxP = MinMax.min();
		if (model.getModelType() == ModelType.MDP) {
			OpRelOpBound op = innerFormula.getRelopBoundInfo(pmc.getConstantValues());
			minMaxP = op.getMinMax(model.getModelType());
		}

		boolean existentialQ = !(minMaxP.isMin() == relOpP.isLowerBound());

		final ExpressionQuantileProbNormalForm normalForm = new ExpressionQuantileProbNormalForm();
		normalForm.setRewardStructIndex(rewardStructIndex);
		//search for the minimal accumulated reward
		if (minMaxQuantile.isMin()) {
			if (existentialQ) {
				normalForm.setExistential();
			} else {
				normalForm.setUniversal();
			}

			if (formulaIncreasing && relOpP.isUpperBound()) {
				throw new PrismException("Combination of " + relOpP + " and increasing formula not supported");
			}

			if (!formulaIncreasing && relOpP.isLowerBound()) {
				throw new PrismException("Combination of " + relOpP + " and decreasing formula not supported");
			}

			ExpressionProb newInnerFormula = new ExpressionProb();
			newInnerFormula.setExpression(replaceByDefaultBound(exprTemp, formulaLowerBound, formulaBoundStrict, e.getQuantileVariable()));
			newInnerFormula.setRelOp(relOpP);
			newInnerFormula.setProb(innerFormula.getProb().deepCopy());

			normalForm.setInnerFormula(newInnerFormula);
			normalForm.setQuantileVariable(e.getQuantileVariable());
			normalForm.setResultAdjustment(adjustment);
			normalForm.setChooseIntervalUpperBound(true);

			return new ModelExpressionTransformationIdentity<M>(model, e, normalForm, statesOfInterest);
		}
		//search for the maximal accumulated reward
		if (existentialQ) {
			normalForm.setUniversal();
		} else {
			normalForm.setExistential();
		}

		if (!formulaIncreasing && relOpP.isUpperBound()) {
			throw new PrismException("Combination of " + relOpP + " and decreasing formula not supported");
		}

		if (formulaIncreasing && relOpP.isLowerBound()) {
			throw new PrismException("Combination of " + relOpP + " and increasing formula not supported");
		}

		ExpressionProb newInnerFormula = new ExpressionProb();
		newInnerFormula.setExpression(replaceByDefaultBound(exprTemp, formulaLowerBound, formulaBoundStrict, e.getQuantileVariable()));
		newInnerFormula.setRelOp(relOpP.negate());
		newInnerFormula.setProb(innerFormula.getProb().deepCopy());

		normalForm.setInnerFormula(newInnerFormula);
		normalForm.setQuantileVariable(e.getQuantileVariable());
		if (model.getModelType() != ModelType.CTMC){
			adjustment--;
		}
		normalForm.setResultAdjustment(adjustment);
		normalForm.setChooseIntervalUpperBound(false);

		return new ModelExpressionTransformationIdentity<M>(model, e, normalForm, statesOfInterest);
	}

	public static <M extends Model> ModelExpressionTransformation<M, M> transformAdditionalBounds(ProbModelChecker pmc, M model,
			final ExpressionQuantileProb expression, BitSet statesOfInterest) throws PrismException
	{
		final ExpressionQuantileProb expressionTransformed = expression.deepCopy();

		assert (expressionTransformed.getInnerFormula() instanceof ExpressionProb);
		ExpressionProb exprProb = (ExpressionProb) expressionTransformed.getInnerFormula();

		assert (exprProb.getExpression() instanceof ExpressionTemporal);
		ExpressionTemporal exprTemp = (ExpressionTemporal) exprProb.getExpression();

		List<TemporalOperatorBound> boundsToReplace = new ArrayList<TemporalOperatorBound>();
		for (TemporalOperatorBound bound : exprTemp.getBounds().getBounds()) {
			if ((bound.hasUpperBound() && Expression.hasBoundedVariables(bound.getUpperBound()))
					|| (bound.hasLowerBound() && Expression.hasBoundedVariables(bound.getLowerBound()))) {
				// this is the bound with the quantile variable
			} else {
				// this does not have the quantile variable, schedule for replacement
				boundsToReplace.add(bound);
			}
		}

		final ModelExpressionTransformation<M, M> transformed = CounterTransformation.replaceBoundsWithCounters(pmc, model, exprTemp, boundsToReplace,
				statesOfInterest);

		exprProb.setExpression(transformed.getTransformedExpression());
		pmc.getLog().println("\nPerforming actual calculations for\n");
		pmc.getLog().println(model.getModelType() + ":  " + transformed.getTransformedModel().infoString());
		pmc.getLog().println("Formula: " + expression + "\n");

		return new ModelExpressionTransformation<M, M>()
		{

			@Override
			public M getOriginalModel()
			{
				return transformed.getOriginalModel();
			}

			@Override
			public M getTransformedModel()
			{
				return transformed.getTransformedModel();
			}

			@Override
			public StateValues projectToOriginalModel(StateValues svTransformedModel) throws PrismException
			{
				return transformed.projectToOriginalModel(svTransformedModel);
			}

			@Override
			public Expression getTransformedExpression()
			{
				return expressionTransformed;
			}

			@Override
			public Expression getOriginalExpression()
			{
				return expression;
			}

			@Override
			public BitSet getTransformedStatesOfInterest()
			{
				return transformed.getTransformedStatesOfInterest();
			}

			@Override
			public Integer mapToTransformedModel(int state)
			{
				return transformed.mapToTransformedModel(state);
			}

			@Override
			public BitSet mapToTransformedModel(BitSet states)
			{
				return transformed.mapToTransformedModel(states);
			}

		};
	}
}
