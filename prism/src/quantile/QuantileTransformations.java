package quantile;

import java.util.ArrayList;
import java.util.List;

import jdd.JDDNode;
import explicit.MinMax;
import parser.ast.Expression;
import parser.ast.ExpressionBoundVariable;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionQuantileProb;
import parser.ast.ExpressionQuantileProbNormalForm;
import parser.ast.ExpressionTemporal;
import parser.ast.RelOp;
import parser.ast.TemporalOperatorBound;
import parser.ast.TemporalOperatorBounds;
import prism.CounterTransformation;
import prism.Model;
import prism.ModelExpressionTransformation;
import prism.ModelExpressionTransformationIdentity;
import prism.ModelExpressionTransformationNested;
import prism.ModelType;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import prism.StateModelChecker;
import prism.StateValues;

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

	public static <M extends Model> ModelExpressionTransformation<M, M> toNormalForm(final StateModelChecker pmc,
	                                                                                 final Model model,
	                                                                                 final ExpressionQuantileProb e,
	                                                                                 JDDNode statesOfInterest) throws PrismException
	{
		ExpressionProb innerFormula = checkForExpressionProb(e);
		Expression pathFormula = innerFormula.getExpression();
		ExpressionTemporal exprTemp = checkForExpressionTemporal(pathFormula);
		// is positive F or U, so until form is again ExpressionTemporal
		exprTemp = (ExpressionTemporal) exprTemp.convertToUntilForm();
		TemporalOperatorBounds bounds = exprTemp.getBounds();

		if (!bounds.hasBounds() || !Expression.hasBoundedVariables(exprTemp)) {
			// TODO(JK): do simple computations + warning
			throw new PrismException("No quantile variable in bounds, no computation needed");
		}

		if (model.getModelType() == ModelType.CTMC) {
			if (bounds.countBounds() > 1)
				throw new PrismNotSupportedException("For CTMCs, only a single bound is supported");
			if (bounds.hasStepBounds())
				throw new PrismNotSupportedException("For CTMCs, step bounds are not supported");
			if (bounds.hasRewardBounds())
				throw new PrismNotSupportedException("For CTMCs, reward bounds are not supported");
		}

		if (bounds.countBounds() > 1) {
			ModelExpressionTransformation<M, M> transformed =
					(ModelExpressionTransformation<M, M>) transformAdditionalBounds(pmc, model, e, statesOfInterest);
			ModelExpressionTransformation<M, M> normalForm = toNormalForm(pmc, transformed.getTransformedModel(),
					(ExpressionQuantileProb) transformed.getTransformedExpression(), transformed.getTransformedStatesOfInterest());
			return new ModelExpressionTransformationNested<M, M, M>(transformed, normalForm);}

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

		if (formulaBoundStrict && model.getModelType() != ModelType.CTMC) {
			// computation engine only supports non-strict bounds on temporal operator
			// for CTMC, strictness is irrelevant
			if (formulaLowerBound) {
				// make bound non-strict
				quantileBound.setLowerBound(quantileBound.getLowerBound(), false);
				adjustment-=1;
			} else {
				// make bound non-strict
				quantileBound.setUpperBound(quantileBound.getUpperBound(), false);
				adjustment+=1;
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
			throw new PrismNotSupportedException("Quantile is trivial, computation not supported yet");
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
				throw new PrismNotSupportedException("Combination of " + relOpP + " and increasing formula not supported");
			}

			if (!formulaIncreasing && relOpP.isLowerBound()) {
				throw new PrismException("Combination of " + relOpP + " and decreasing formula not supported");
			}

			ExpressionProb newInnerFormula = new ExpressionProb();
			newInnerFormula.setExpression(exprTemp);
			newInnerFormula.setRelOp(relOpP);
			newInnerFormula.setProb(innerFormula.getProb().deepCopy());

			normalForm.setInnerFormula(newInnerFormula);
			normalForm.setQuantileVariable(e.getQuantileVariable());
			if (model.getModelType() != ModelType.CTMC) {
				// set adjustment for DTMC & MDP; for CTMC, don't need any adjustment
				normalForm.setResultAdjustment(adjustment);
			}

			return new ModelExpressionTransformationIdentity<M>((M) model, e, normalForm, statesOfInterest.copy());
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
		newInnerFormula.setExpression(exprTemp);
		newInnerFormula.setRelOp(relOpP.negate());
		newInnerFormula.setProb(innerFormula.getProb().deepCopy());

		normalForm.setInnerFormula(newInnerFormula);
		normalForm.setQuantileVariable(e.getQuantileVariable());
		if (model.getModelType() != ModelType.CTMC){
			// set adjustment for DTMC & MDP; for CTMC, don't need any adjustment
			normalForm.setResultAdjustment(adjustment - 1);
		}

		return new ModelExpressionTransformationIdentity<M>((M) model, e, normalForm, statesOfInterest.copy());
	}

	public static <M extends Model> ModelExpressionTransformation<M, M> transformAdditionalBounds(StateModelChecker pmc, M model,
			final ExpressionQuantileProb expression, JDDNode statesOfInterest) throws PrismException
	{
		final ExpressionQuantileProb expressionTransformed = expression.deepCopy();
		PrismLog log = pmc.getLog();

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
		log.println("\nPerforming actual calculations for\n");
		log.println(model.getModelType() + ":");
		transformed.getTransformedModel().printTransInfo(log);
		log.println("Formula: " + expression + "\n");

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
			public JDDNode getTransformedStatesOfInterest()
			{
				return transformed.getTransformedStatesOfInterest();
			}

			@Override
			public void clear()
			{
				transformed.clear();
			}
		};
	}

}
