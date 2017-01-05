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
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.LTLModelChecker.LTLProduct;
import prism.Model;
import prism.conditional.SimplePathProperty.Finally;
import prism.conditional.SimplePathProperty.Reach;
import prism.conditional.transform.GoalFailStopTransformation;
import prism.conditional.transform.GoalFailStopTransformation.GoalFailStopOperator;
import prism.conditional.transform.GoalFailStopTransformation.ProbabilisticRedistribution;
import prism.conditional.transform.LTLProductTransformer.LabeledDA;



// FIXME ALG: add comment
public interface NewLtlUntilTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewNormalFormTransformer<M, MC>
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
		NewFinallyUntilTransformer<M, MC> finallyUntilTransformer = getFinallyUntilTransformer();
		getLog().println("\nDelegating to " + finallyUntilTransformer.getName());
		return finallyUntilTransformer.transformNormalForm(objectivePath, conditionPath, statesOfInterest);
	}

	NewFinallyUntilTransformer<M, MC> getFinallyUntilTransformer();

	default GoalFailStopOperator<M> configureOperator(M model, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, JDDNode instantGoalStates, JDDNode instantFailStates, JDDNode statesOfInterest)
			throws PrismException
	{
		return configureOperator(model, new ProbabilisticRedistribution(), goalStop, stopFail, instantGoalStates, instantFailStates, statesOfInterest);
	}



	public static class DTMC extends NewNormalFormTransformer.DTMC implements NewLtlUntilTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
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
			return new NewFinallyUntilTransformer.DTMC(getModelChecker());
		}
	}



	public static class MDP extends NewNormalFormTransformer.MDP implements NewLtlUntilTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
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

			GoalFailStopOperator<NondetModel> operator             = transformOmega(product, conditionPath, conditionFalsifiedStates, statesOfInterest);
			GoalFailStopTransformation<NondetModel> transformation = new GoalFailStopTransformation<>(productModel, operator, badStates);

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

			conditionPath.clear();
			return new Pair<>(transformation, transformedExpression);
		}

//		/**
//		 * 1) Search accepting ECs in {@code succ*(Pmin=1(condition))} and<br>
//		 * 2) Search accepting ECs in {@code S \ (Pmax=0(condition) | Pmin=1(condition))}.
//		 */
//		public GoalFailStopOperator<NondetModel> transformOmegaV1(LTLProduct<NondetModel> product, Until conditionPath, JDDNode conditionFalsifiedStates, JDDNode statesOfInterest)
//				throws PrismException
//		{
//getLog().println("tranformOmega");
//			NondetModel productModel = product.getProductModel();
//
//			// compute redistribution for satisfied condition
//			// compute normal-form states for condition (including all satisfying MECs)
//			JDDNode conditionSatisfiedStates = computeProb1A(productModel, conditionPath);
//			JDDNode conditionSatisfiedProbabilities;
//			if (conditionSatisfiedStates.equals(JDD.ZERO)) {
//				conditionSatisfiedProbabilities = JDD.Constant(0);
//			} else {
//				// compute accepting states in succ*(conditionSatisfied) (ECs or REACH states)
//				JDDNode succConditionSatisfied   = computeSuccStar(productModel, conditionSatisfiedStates);
//				JDDNode acceptSuccCondition      = getLtlTransformer().findAcceptingStates(product, succConditionSatisfied);
//				JDD.Deref(succConditionSatisfied);
//				// compute probabilities for objective
//				Finally objectivePath            = new Finally(productModel, acceptSuccCondition);
//				conditionSatisfiedProbabilities  = computeUntilMaxProbs(productModel, objectivePath);
//				objectivePath.clear();
//			}
//			ProbabilisticRedistribution conditionSatisfied = new ProbabilisticRedistribution(conditionSatisfiedStates, conditionSatisfiedProbabilities);
//
//			// compute accepting ECs to be normalized to goal
//			JDDNode instantGoalStates;
//			if (conditionPath.isNegated()) {
//				// ECs might satisfy objective and condition
//				// -> exclude all states that falsify or satisfy the condition for sure
//				JDDNode restrict  = JDD.Not(JDD.Or(conditionFalsifiedStates.copy(), conditionSatisfied.getStates()));
//				restrict          = JDD.And(productModel.getReach().copy(), restrict);
//				instantGoalStates = findAcceptingStatesMax(product, restrict, true);
//			} else {
//				// ECs do not matter, since they are visited after the (non-negated) condition has already been satisfied
//				instantGoalStates = JDD.Constant(0);
//			}
//
//			// transform goal-fail-stop
//			return configureOperator(productModel, conditionSatisfied, instantGoalStates, conditionFalsifiedStates, statesOfInterest);
//		}

		/**
		 * 1) search all accepting ECs and<br>
		 * 2) refine in {@code AccEC \ (Pmax=0(condition) | Pmin=1(condition))}.
		 */
		public GoalFailStopOperator<NondetModel> transformOmega(LTLProduct<NondetModel> product, Reach<NondetModel> conditionPath, JDDNode conditionFalsifiedStates, JDDNode statesOfInterest)
				throws PrismException
		{
			NondetModel productModel = product.getProductModel();

			// compute accepting states  (ECs or REACH states)
			JDDNode acceptStates               = getLtlTransformer().findAcceptingStates(product);
			Finally<NondetModel> objectivePath = new Finally<>(productModel, acceptStates.copy());

			// compute redistribution for satisfied condition
			ProbabilisticRedistribution conditionSatisfied = redistributeProb1(conditionPath, objectivePath);

			// compute redistribution for falsified objective
			ProbabilisticRedistribution objectiveFalsified = redistributeProb0Objective(objectivePath, conditionPath);
			objectivePath.clear();

			// compute accepting ECs to be normalized to goal
			JDDNode instantGoalStates;
			if (conditionPath.isNegated()) {
				// ECs might satisfy objective and condition
				// -> exclude all states that falsify or satisfy the condition for sure
				JDDNode restrict  = JDD.Not(JDD.Or(conditionFalsifiedStates.copy(), conditionSatisfied.getStates()));
				// -> refine only accepting ECs
				restrict          = JDD.And(acceptStates, restrict);
				instantGoalStates = findAcceptingStatesMax(product, restrict);
			} else {
				// ECs do not matter, since they are visited after the (non-negated) condition has already been satisfied
				instantGoalStates = JDD.Constant(0);
				JDD.Deref(acceptStates);
			}

			// transform goal-fail-stop
			return configureOperator(productModel, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, statesOfInterest);
		}

		@Override
		public NewFinallyUntilTransformer.MDP getFinallyUntilTransformer()
		{
			return new NewFinallyUntilTransformer.MDP(getModelChecker());
		}

		public ProbabilisticRedistribution redistributeProb0Objective(Finally<NondetModel> objectivePath, Reach<NondetModel> conditionPath)
				throws PrismException
		{
			objectivePath.requireSameModel(conditionPath);

			// Do we have to reset once a state violates the objective?
			// Is objective path free of invariants?
			if (objectivePath.hasToRemain() || settings.getBoolean(PrismSettings.CONDITIONAL_RESET_MDP_MINIMIZE)) {
				return redistributeProb0(objectivePath, conditionPath);
			}
			// Skip costly normalization
			return new ProbabilisticRedistribution();
		}
	}
}
