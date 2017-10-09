package prism.conditional;

import acceptance.AcceptanceType;
import explicit.MinMax;
import explicit.conditional.ExpressionInspector;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.Pair;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.StochModel;
import prism.StochModelChecker;
import prism.LTLModelChecker.LTLProduct;
import prism.Model;
import prism.conditional.SimplePathProperty.Finally;
import prism.conditional.SimplePathProperty.Reach;
import prism.conditional.transform.GoalFailStopTransformation;
import prism.conditional.transform.GoalFailStopTransformation.ProbabilisticRedistribution;
import prism.conditional.transform.LTLProductTransformer.LabeledDA;



// FIXME ALG: add comment
public interface NewLtlUntilTransformer<M extends ProbModel, C extends StateModelChecker> extends NewNormalFormTransformer<M, C>
{
	// FIXME ALG: Generalize acceptance types: all
	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.RABIN, AcceptanceType.GENERALIZED_RABIN, AcceptanceType.STREETT};
//	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.RABIN, AcceptanceType.GENERALIZED_RABIN, AcceptanceType.STREETT};



	@Override
	default boolean canHandleObjective(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		if (! NewNormalFormTransformer.super.canHandleObjective(model, expression)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		return getLtlTransformer().canHandle(model, objective.getExpression());
	}

	@Override
	default boolean canHandleCondition(Model model, ExpressionConditional expression)
	{
		Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		Expression until = ExpressionInspector.removeNegation(normalized);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
	}

	@Override
	default NormalFormTransformation<M> transformNormalForm(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: build omega automaton
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTemp = objective.getExpression();
		LabeledDA objectiveDA    = getLtlTransformer().constructDA(model, objectiveTemp, ACCEPTANCE_TYPES);

		// 2) Condition: compute simple path property
		Expression conditionTemp = expression.getCondition();
		Reach<M> conditionPath   = (Reach<M>) computeSimplePathProperty(model, conditionTemp);

		// 3) Transform model
		Pair<GoalFailStopTransformation<M>,ExpressionConditional> result = transformNormalForm(objectiveDA, conditionPath, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression  = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective        = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded, transformation.getFailLabel(), transformation.getBadLabel());
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(LabeledDA objectiveDA, Reach<M> conditionPath, JDDNode statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Objective
		LTLProduct<M> product = getLtlTransformer().constructProduct(conditionPath.getModel(), objectiveDA, statesOfInterest);
		//    Lift state sets to product model
		Reach<M> conditionPathProduct = conditionPath.lift(product);
		conditionPath.clear();

		// 2) Normal-Form Transformation
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = transformNormalForm(product, conditionPathProduct);

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.chain(product);
		ExpressionConditional transformedExpression  = result.second;

		return new Pair<>(transformation, transformedExpression);
	}

	Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(LTLProduct<M> product, Reach<M> conditionPath)
			throws PrismException;

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalFormReach(LTLProduct<M> product, Reach<M> conditionPath)
			throws PrismException
	{
		M productModel           = product.getProductModel();
		JDDNode statesOfInterest = product.getTransformedStatesOfInterest();
		JDDNode acceptStates     = getLtlTransformer().findAcceptingStates(product);
		Finally<M> objectivePath = new Finally<M>(productModel, acceptStates);

		// FIXME ALG: reuse computation of conditionSatisfied?
		NewFinallyUntilTransformer<M, C> finallyUntilTransformer         = getFinallyUntilTransformer();
		getLog().println("\nDelegating to " + finallyUntilTransformer.getName());
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = finallyUntilTransformer.transformNormalForm(objectivePath, conditionPath, statesOfInterest);
		finallyUntilTransformer.clear();
		return result;
	}

	NewFinallyUntilTransformer<M, C> getFinallyUntilTransformer();



	public static class CTMC extends NewNormalFormTransformer.CTMC implements NewLtlUntilTransformer<StochModel, StochModelChecker>
	{
		public CTMC(Prism prism, StochModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<StochModel>, ExpressionConditional> transformNormalForm(LTLProduct<StochModel> product, Reach<StochModel> conditionPath)
				throws PrismException
		{
			// Since each BSCC is either accepting or not, it suffices to reach the set of accepting states
			return transformNormalFormReach(product, conditionPath);
		}

		@Override
		public NewFinallyUntilTransformer.CTMC getFinallyUntilTransformer()
		{
			return new NewFinallyUntilTransformer.CTMC(prism, getModelChecker());
		}
	}



	public static class DTMC extends NewNormalFormTransformer.DTMC implements NewLtlUntilTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(Prism prism, ProbModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<ProbModel>, ExpressionConditional> transformNormalForm(LTLProduct<ProbModel> product, Reach<ProbModel> conditionPath)
				throws PrismException
		{
			// Since each BSCC is either accepting or not, it suffices to reach the set of accepting states
			return transformNormalFormReach(product, conditionPath);
		}

		@Override
		public NewFinallyUntilTransformer.DTMC getFinallyUntilTransformer()
		{
			return new NewFinallyUntilTransformer.DTMC(prism, getModelChecker());
		}
	}



	public static class MDP extends NewNormalFormTransformer.MDP implements NewLtlUntilTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(Prism prism, NondetModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<NondetModel>, ExpressionConditional> transformNormalForm(LTLProduct<NondetModel> product, Reach<NondetModel> conditionPath)
				throws PrismException
		{
			conditionPath.requireSameModel(product.getProductModel());

			if (product.getAcceptance().getType() == AcceptanceType.REACH) {
				return transformNormalFormReach(product, conditionPath);
			}

			NondetModel productModel = product.getProductModel();
			JDDNode statesOfInterest = product.getTransformedStatesOfInterest();

			// FIXME ALG: consider whether this is actually an error in a normal-form transformation
			JDDNode conditionFalsifiedStates = checkSatisfiability(conditionPath, statesOfInterest);

			// compute badStates
			JDDNode badStates = computeBadStates(conditionPath, conditionFalsifiedStates);

			// compute accepting states  (ECs or REACH states)
			JDDNode acceptStates               = getLtlTransformer().findAcceptingStates(product);
			Finally<NondetModel> objectivePath = new Finally<>(productModel, acceptStates.copy());

			// compute redistribution for satisfied condition
			ProbabilisticRedistribution conditionSatisfied = redistributeProb1(conditionPath, objectivePath);

			// compute redistribution for falsified objective
			ProbabilisticRedistribution objectiveFalsified = redistributeProb0Objective(objectivePath, conditionPath);

			// compute states where objective and condition can be satisfied
			JDDNode instantGoalStates = computeInstantGoalStates(product, acceptStates, objectiveFalsified.getStates(), conditionPath, conditionSatisfied.getStates(), conditionFalsifiedStates);
			JDD.Deref(acceptStates);

			// transform goal-fail-stop
			ProbabilisticRedistribution objectiveSatisfied         = new ProbabilisticRedistribution();
			GoalFailStopTransformation<NondetModel> transformation = transformGoalFailStop(productModel, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, badStates, statesOfInterest);

			// transform expression
			ExpressionLabel goal                        = new ExpressionLabel(transformation.getGoalLabel());
			ExpressionTemporal transformedObjectiveTmp  = Expression.Finally(goal);
			ExpressionProb transformedObjective         = new ExpressionProb(transformedObjectiveTmp, MinMax.max(), "=", null);
			ExpressionTemporal transformedCondition;
			if (conditionPath.isCoSafe()) {
				// All paths satisfying the condition eventually reach the goal or stop state.
				ExpressionLabel stopLabel = new ExpressionLabel(transformation.getStopLabel());
				transformedCondition      = Expression.Finally(Expression.Parenth(Expression.Or(goal, stopLabel)));
			} else {
				// All paths violating the condition eventually reach the fail state.
				ExpressionLabel fail      = new ExpressionLabel(transformation.getFailLabel());
				transformedCondition      = Expression.Globally(Expression.Not(fail));
			}
			ExpressionConditional transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);

			objectivePath.clear();
			conditionPath.clear();
			return new Pair<>(transformation, transformedExpression);
		}

		public JDDNode computeInstantGoalStates(LTLProduct<NondetModel> product, JDDNode objectiveAcceptStates, JDDNode objectiveFalsifiedStates, Reach<NondetModel> conditionPath, JDDNode conditionSatisfiedStates, JDDNode conditionFalsifiedStates)
				throws PrismException
		{
			NondetModel productModel = product.getProductModel();
			conditionPath.requireSameModel(productModel);

			JDDNode instantGoalStates = JDD.And(objectiveAcceptStates.copy(), conditionSatisfiedStates.copy());

			// exclude objective/condition falsified states
			JDDNode falsifiedStates    = JDD.Or(objectiveFalsifiedStates.copy(), conditionFalsifiedStates.copy());
			JDDNode notFalsifiedStates = JDD.And(allStates(productModel), JDD.Not(falsifiedStates.copy()));

			// Does the condition specify behavior in the limit?
			if (!conditionPath.isCoSafe()) {
				// Find accepting ECs that do not already include a normalize state
				JDDNode remain       = JDD.And(objectiveAcceptStates.copy(), JDD.Not(JDD.Or(conditionSatisfiedStates.copy(), falsifiedStates.copy())));
				JDDNode acceptStates = getLtlTransformer().findAcceptingStates(product, remain);
				instantGoalStates    = JDD.Or(instantGoalStates, acceptStates);
				JDD.Deref(remain);
			}
			JDDNode result = computeProb1E(productModel, false, notFalsifiedStates, instantGoalStates);
			JDD.Deref(falsifiedStates, notFalsifiedStates, instantGoalStates);
			return result;
		}

		@Override
		public NewFinallyUntilTransformer.MDP getFinallyUntilTransformer()
		{
			return new NewFinallyUntilTransformer.MDP(prism, getModelChecker());
		}

		public ProbabilisticRedistribution redistributeProb0Objective(Finally<NondetModel> objectivePath, Reach<NondetModel> conditionPath)
				throws PrismException
		{
			objectivePath.requireSameModel(conditionPath);

			// Do we have to reset once a state violates the objective?
			if (objectivePath.hasToRemain() || settings.getBoolean(PrismSettings.CONDITIONAL_RESET_MDP_MINIMIZE)) {
				return redistributeProb0(objectivePath, conditionPath);
			}
			// Skip costly normalization
			return new ProbabilisticRedistribution();
		}
	}
}
