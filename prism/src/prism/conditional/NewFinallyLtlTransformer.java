package prism.conditional;

import acceptance.AcceptanceType;
import explicit.MinMax;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.UndefinedTransformationException;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
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
import prism.conditional.transform.GoalFailStopTransformation.ProbabilisticRedistribution;
import prism.conditional.transform.LTLProductTransformer.LabeledDA;



// FIXME ALG: add comment
public interface NewFinallyLtlTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewNormalFormTransformer<M, MC>
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
		Expression normalized    = ExpressionInspector.normalizeExpression(objective.getExpression());
		Expression until         = ExpressionInspector.removeNegation(normalized);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
	}

	@Override
	default boolean canHandleCondition(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		return getLtlTransformer().canHandle(model, expression.getCondition());
	}

	@Override
	default NormalFormTransformation<M> transformNormalForm(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: compute simple path property
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTmp  = objective.getExpression();
		Reach<M> objectivePath   = (Reach<M>) computeSimplePathProperty(model, objectiveTmp);

		// 2) Condition: build omega automaton
		Expression conditionTmp = expression.getCondition();
		LabeledDA conditionDA   = getLtlTransformer().constructDA(model, conditionTmp, ACCEPTANCE_TYPES);

		// 3) Transform model
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = transformNormalForm(objectivePath, conditionDA, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression  = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective        = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded, transformation.getFailLabel(), transformation.getBadLabel());
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(Reach<M> objectivePath, LabeledDA conditionDA, JDDNode statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Condition
		LTLProduct<M> product = getLtlTransformer().constructProduct(objectivePath.getModel(), conditionDA, statesOfInterest);
		//    Lift state sets to product model
		Reach<M> objectivePathProduct = objectivePath.lift(product);
		objectivePath.clear();

		// 2) Normal-Form Transformation
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = transformNormalForm(product, objectivePathProduct);

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.chain(product);
		ExpressionConditional transformedExpression  = result.second;

		return new Pair<>(transformation, transformedExpression);
	}

	Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(LTLProduct<M> product, Reach<M> objectivePath)
			throws PrismException;

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalFormReach(LTLProduct<M> product, Reach<M> objectivePath)
			throws PrismException
	{
		// FIXME ALG: remove redundancy?
		M productModel           = product.getProductModel();
		JDDNode statesOfInterest = product.getTransformedStatesOfInterest();
		JDDNode acceptStates     = getLtlTransformer().findAcceptingStates(product);
		Finally<M> conditionPath = new Finally<>(productModel, acceptStates);

		NewFinallyUntilTransformer<M, MC> finallyUntilTransformer         = getFinallyUntilTransformer();
		getLog().println("\nDelegating to " + finallyUntilTransformer.getName());
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = finallyUntilTransformer.transformNormalForm(objectivePath, conditionPath, statesOfInterest);
		finallyUntilTransformer.clear();
		return result;
	}

	NewFinallyUntilTransformer<M, MC> getFinallyUntilTransformer();



	public static class DTMC extends NewNormalFormTransformer.DTMC implements NewFinallyLtlTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<ProbModel>, ExpressionConditional> transformNormalForm(LTLProduct<ProbModel> product, Reach<ProbModel> objectivePath)
				throws PrismException, UndefinedTransformationException
		{
			// Since each BSCC is either accepting or not, it suffices to reach the set of accepting states
			return transformNormalFormReach(product, objectivePath);
		}

		@Override
		public NewFinallyUntilTransformer.DTMC getFinallyUntilTransformer()
		{
			return new NewFinallyUntilTransformer.DTMC(getModelChecker());
		}
	}



	public static class MDP extends NewNormalFormTransformer.MDP implements NewFinallyLtlTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<NondetModel>, ExpressionConditional> transformNormalForm(LTLProduct<NondetModel> product, Reach<NondetModel> objectivePath)
				throws PrismException
		{
			if (product.getAcceptance().getType() == AcceptanceType.REACH) {
				return transformNormalFormReach(product, objectivePath);
			}

			NondetModel productModel = product.getProductModel();
			JDDNode statesOfInterest = product.getTransformedStatesOfInterest();

			// compute accepting states  (ECs or REACH states)
			JDDNode acceptStates               = getLtlTransformer().findAcceptingStates(product);
			Finally<NondetModel> conditionPath = new Finally<>(productModel, acceptStates.copy());

			// FIXME ALG: consider whether this is actually an error in a normal-form transformation
			JDDNode conditionFalsifiedStates = checkSatisfiability(conditionPath, statesOfInterest);

			// compute bad states
			JDDNode badStates = computeBadStates(product, conditionFalsifiedStates);

			// compute redistribution for satisfied objective
			ProbabilisticRedistribution objectiveSatisfied = redistributeProb1(objectivePath, conditionPath);

			// compute redistribution for falsified objective
			ProbabilisticRedistribution objectiveFalsified = redistributeProb0Objective(objectivePath, conditionFalsifiedStates, badStates);

			// compute states where objective and condition can be satisfied
			JDDNode instantGoalStates = computeInstantGoalStates(product, objectivePath, objectiveSatisfied.getStates(), objectiveFalsified.getStates(), acceptStates, conditionFalsifiedStates);
			JDD.Deref(acceptStates);

			// transform goal-fail-stop
			ProbabilisticRedistribution conditionSatisfied         = new ProbabilisticRedistribution();
			GoalFailStopTransformation<NondetModel> transformation = transformGoalFailStop(productModel, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, badStates, statesOfInterest);

			// build expression
			ExpressionLabel goal                = new ExpressionLabel(transformation.getGoalLabel());
			Expression transformedObjectiveTmp  = Expression.Finally(goal);
			ExpressionProb transformedObjective = new ExpressionProb(transformedObjectiveTmp, MinMax.max(), "=", null);
			// All paths satisfying the condition eventually reach goal or an accepting EC.
			// The label accept_condition is artificial and stands for the automaton's acceptance condition.
			Expression transformedCondition     = Expression.Or(Expression.Finally(goal), new ExpressionLabel("accept_condition"));
			ExpressionConditional transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);

			objectivePath.clear();
			conditionPath.clear();
			return new Pair<>(transformation, transformedExpression);
		}

		/**
		 * Compute redistribution for falsified objective.
		 * For efficiency, do not minimizing the probability to satisfy the condition, but
		 * maximize the probability to reach badStates | conditionFalsifiedStates, which is equivalent.
		 */
		public ProbabilisticRedistribution redistributeProb0Objective(Reach<NondetModel> objectivePath, JDDNode conditionFalsified, JDDNode conditionMaybeFalsified)
				throws PrismException
		{
			// Do we have to reset once a state violates the objective?
			if (objectivePath.hasToRemain() || settings.getBoolean(PrismSettings.CONDITIONAL_RESET_MDP_MINIMIZE)) {
				// path to non-accepting states
				JDDNode conditionFalsifiedStates            = JDD.Or(conditionFalsified.copy(), conditionMaybeFalsified.copy());
				Finally<NondetModel> conditionFalsifiedPath = new Finally<>(objectivePath.getModel(), conditionFalsifiedStates);
				// compute redistribution
				ProbabilisticRedistribution objectiveFalsified = redistributeProb0Complement(objectivePath, conditionFalsifiedPath);
				conditionFalsifiedPath.clear();
				return objectiveFalsified;
			}
			// Skip costly normalization
			return new ProbabilisticRedistribution();
		}

		public JDDNode computeInstantGoalStates(LTLProduct<NondetModel> product, Reach<NondetModel> objectivePath, JDDNode objectiveSatisfiedStates, JDDNode objectiveFalsifiedStates, JDDNode conditionAcceptStates, JDDNode conditionFalsifiedStates)
				throws PrismException
		{
			NondetModel productModel = product.getProductModel();
			objectivePath.requireSameModel(productModel);

			JDDNode instantGoalStates = JDD.And(objectiveSatisfiedStates.copy(), conditionAcceptStates.copy());

			// exclude objective/condition falsified states
			JDDNode falsifiedStates    = JDD.Or(objectiveFalsifiedStates.copy(), conditionFalsifiedStates.copy());
			JDDNode notFalsifiedStates = JDD.And(allStates(productModel), JDD.Not(falsifiedStates.copy()));

			// Does the objective specify behavior in the limit?
			if (!objectivePath.isCoSafe()) {
				// Find accepting ECs that do not already include a normalize state
				JDDNode remain       = JDD.And(conditionAcceptStates.copy(), JDD.Not(JDD.Or(objectiveSatisfiedStates.copy(), falsifiedStates.copy())));
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
			return new NewFinallyUntilTransformer.MDP(getModelChecker());
		}
	}
}
