package prism.conditional;

import acceptance.AcceptanceStreettDD;
import acceptance.AcceptanceType;
import acceptance.AcceptanceStreettDD.StreettPairDD;
import explicit.MinMax;
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
import prism.conditional.transform.GoalFailStopTransformation;
import prism.conditional.transform.GoalFailStopTransformation.ProbabilisticRedistribution;
import prism.conditional.transform.LTLProductTransformer.LabeledDA;



// FIXME ALG: add comment
public interface LtlLtlTransformer<M extends ProbModel, C extends StateModelChecker> extends NormalFormTransformer<M, C>
{
	// FIXME ALG: Generalize acceptance types: DMTC=all, MDP=REACH, STREETT
	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.STREETT};
//	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.STREETT};



	@Override
	default boolean canHandleObjective(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		if (! NormalFormTransformer.super.canHandleObjective(model, expression)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		return getLtlTransformer().canHandle(model, objective.getExpression());
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

		// 1) Objective: build omega automaton
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTemp = objective.getExpression();
		LabeledDA objectiveDA    = getLtlTransformer().constructDA(model, objectiveTemp, ACCEPTANCE_TYPES);

		// 2) Condition: build omega automaton
		Expression conditionTmp = expression.getCondition();
		LabeledDA conditionDA   = getLtlTransformer().constructDA(model, conditionTmp, ACCEPTANCE_TYPES);

		// 3) Transform model
		Pair<GoalFailStopTransformation<M>,ExpressionConditional> result = transformNormalForm(model, objectiveDA, conditionDA, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression  = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective        = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded, transformation.getFailLabel(), transformation.getBadLabel());
	}

	Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(M model, LabeledDA objectiveDA, LabeledDA conditionDA, JDDNode statesOfInterest)
			throws PrismException;

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalFormReachObjective(M model, LabeledDA objectiveDA, LabeledDA conditionDA, JDDNode statesOfInterest)
			throws PrismException
	{
		LTLProduct<M> product           = getLtlTransformer().constructProduct(model, objectiveDA, statesOfInterest);
		M productModel                  = product.getTransformedModel();
		JDDNode acceptStates            = getLtlTransformer().findAcceptingStates(product);
		Finally<M> objectivePathProduct = new Finally<>(productModel, acceptStates);
		JDDNode statesOfInterestProduct = product.getTransformedStatesOfInterest();

		FinallyLtlTransformer<M, C> finallyLtlTransformer = getFinallyLtlTransformer();
		getLog().println("\nDelegating to " + finallyLtlTransformer.getName());
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = finallyLtlTransformer.transformNormalForm(objectivePathProduct, conditionDA.liftToProduct(product), statesOfInterestProduct);
		conditionDA.clear();

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.chain(product);
		ExpressionConditional transformedExpression  = result.second;

		return new Pair<>(transformation, transformedExpression);
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalFormReachCondition(M model, LabeledDA objectiveDA, LabeledDA conditionDA, JDDNode statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Condition
		LTLProduct<M> product           = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
		M productModel                  = product.getTransformedModel();
		JDDNode acceptStates            = getLtlTransformer().findAcceptingStates(product);
		Finally<M> conditionPathProduct = new Finally<>(productModel, acceptStates);
		JDDNode statesOfInterestProduct = product.getTransformedStatesOfInterest();

		// 2) Normal-Form Transformation
		LtlUntilTransformer<M,C> ltlUntilTransformer = getLtlUntilTransformer();
		getLog().println("\nDelegating to " + ltlUntilTransformer.getName());
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = ltlUntilTransformer.transformNormalForm(objectiveDA.liftToProduct(product), conditionPathProduct, statesOfInterestProduct);
		objectiveDA.clear();

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.chain(product);
		ExpressionConditional transformedExpression  = result.second;

		return new Pair<>(transformation, transformedExpression);
	}

	LtlUntilTransformer<M, C> getLtlUntilTransformer();

	FinallyLtlTransformer<M, C> getFinallyLtlTransformer();



	public static class CTMC extends NormalFormTransformer.CTMC implements LtlLtlTransformer<StochModel, StochModelChecker>
	{
		public CTMC(Prism prism, StochModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<StochModel>, ExpressionConditional> transformNormalForm(StochModel model, LabeledDA objectiveDA, LabeledDA conditionDA, JDDNode statesOfInterest)
				throws PrismException
		{
			// Since each BSCC is either accepting or not, it suffices to reach the set of accepting states
			return transformNormalFormReachCondition(model, objectiveDA, conditionDA, statesOfInterest);
		}

		@Override
		public LtlUntilTransformer.CTMC getLtlUntilTransformer()
		{
			return new LtlUntilTransformer.CTMC(prism, getModelChecker());
		}

		@Override
		public FinallyLtlTransformer.CTMC getFinallyLtlTransformer()
		{
			return new FinallyLtlTransformer.CTMC(prism, getModelChecker());
		}
	}



	public static class DTMC extends NormalFormTransformer.DTMC implements LtlLtlTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(Prism prism, ProbModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<ProbModel>, ExpressionConditional> transformNormalForm(ProbModel model, LabeledDA objectiveDA, LabeledDA conditionDA, JDDNode statesOfInterest)
				throws PrismException
		{
			// Since each BSCC is either accepting or not, it suffices to reach the set of accepting states
			return transformNormalFormReachCondition(model, objectiveDA, conditionDA, statesOfInterest);
		}

		@Override
		public LtlUntilTransformer.DTMC getLtlUntilTransformer()
		{
			return new LtlUntilTransformer.DTMC(prism, getModelChecker());
		}

		@Override
		public FinallyLtlTransformer.DTMC getFinallyLtlTransformer()
		{
			return new FinallyLtlTransformer.DTMC(prism, getModelChecker());
		}
	}



	public static class MDP extends NormalFormTransformer.MDP implements LtlLtlTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(Prism prism, NondetModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<NondetModel>, ExpressionConditional> transformNormalForm(NondetModel model, LabeledDA objectiveDA, LabeledDA conditionDA, JDDNode statesOfInterest)
				throws PrismException
		{
			if (conditionDA.getAutomaton().getAcceptance().getType() == AcceptanceType.REACH) {
				return transformNormalFormReachCondition(model, objectiveDA, conditionDA, statesOfInterest);
			}
			if (objectiveDA.getAutomaton().getAcceptance().getType() == AcceptanceType.REACH) {
				return transformNormalFormReachObjective(model, objectiveDA, conditionDA, statesOfInterest);
			}

			// 1) LTL Product Transformation for Condition
			LTLProduct<NondetModel> conditionProduct = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
			NondetModel conditionProductModel        = conditionProduct.getProductModel();
			JDDNode conditionStatesOfInterest        = conditionProduct.getTransformedStatesOfInterest();
			JDDNode acceptConditionStates            = getLtlTransformer().findAcceptingStates(conditionProduct);
			Finally<NondetModel> conditionPath       = new Finally<>(conditionProductModel, acceptConditionStates);

			// FIXME ALG: consider whether this is actually an error in a normal-form transformation
			JDDNode conditionFalsifiedStates = checkSatisfiability(conditionPath, conditionStatesOfInterest);
			conditionPath.clear();

			// compute bad states
			JDDNode badStates = computeBadStates(conditionProduct, conditionFalsifiedStates);

			// 2) LTL Product Transformation for Objective
			LTLProduct<NondetModel> objectiveAndConditionProduct = getLtlTransformer().constructProduct(conditionProductModel, objectiveDA.liftToProduct(conditionProduct), conditionStatesOfInterest);
			objectiveDA.clear();
			NondetModel objectiveAndConditionModel               = objectiveAndConditionProduct.getProductModel();
			JDDNode objectiveAndConditionStatesOfInterest        = objectiveAndConditionProduct.getTransformedStatesOfInterest();

			// FIXME ALG: add liftFromModel to LTLProduct
//			JDDNode conditionFalsifiedLifted = objectiveAndConditionProduct.liftFromModel(instantFailStates);
			JDDNode conditionFalsifiedLifted = JDD.And(conditionFalsifiedStates, objectiveAndConditionModel.getReach().copy());
			JDDNode badStatesLifted          = JDD.And(badStates, objectiveAndConditionModel.getReach().copy());

			// 3) Lift Condition Acceptance
			AcceptanceStreettDD conditionAcceptanceLifted = new AcceptanceStreettDD();
			for (StreettPairDD streettPair : (AcceptanceStreettDD) conditionProduct.getAcceptance()) {
				// FIXME ALG: add liftFromModel to LTLProduct
				JDDNode R = JDD.And(streettPair.getR(), objectiveAndConditionModel.getReach().copy());
//				JDDNode R = objectiveAndConditionProduct.liftFromModel(streettPair.getR());
				JDDNode G = JDD.And(streettPair.getG(), objectiveAndConditionModel.getReach().copy());
//				JDDNOde G = objectiveAndConditionProduct.liftFromModel(streettPair.getG());
				conditionAcceptanceLifted.add(new StreettPairDD(R, G));
			}
			// compute redistribution for falsified objective
			ProbabilisticRedistribution objectiveFalsified = redistributeProb0Objective(objectiveAndConditionProduct, conditionFalsifiedLifted, badStatesLifted);

			// 4) Conjunction of Objective and Condition Acceptance
			AcceptanceStreettDD objectiveAndConditionAcceptance = new AcceptanceStreettDD();
			objectiveAndConditionAcceptance.addAll((AcceptanceStreettDD) objectiveAndConditionProduct.getAcceptance().clone());
			objectiveAndConditionAcceptance.addAll(conditionAcceptanceLifted);
			objectiveAndConditionProduct.getAcceptance().clear();
			objectiveAndConditionProduct.setAcceptance(objectiveAndConditionAcceptance);

			// 5) Objective & Condition Goal States
			// compute states where objective and condition can be satisfied
			JDDNode instantGoalStates = computeInstantGoalStates(objectiveAndConditionProduct);

			// transform goal-fail-stop
			ProbabilisticRedistribution objectiveSatisfied         = new ProbabilisticRedistribution();
			ProbabilisticRedistribution conditionSatisfied         = new ProbabilisticRedistribution();
			GoalFailStopTransformation<NondetModel> transformation = transformGoalFailStop(objectiveAndConditionModel, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedLifted, badStatesLifted, objectiveAndConditionStatesOfInterest);

			// build expression
			ExpressionLabel goal                        = new ExpressionLabel(transformation.getGoalLabel());
			ExpressionTemporal transformedObjectiveTmp  = Expression.Finally(goal);
			ExpressionProb transformedObjective         = new ExpressionProb(transformedObjectiveTmp, MinMax.max(), "=", null);
			// All paths satisfying the condition eventually reach an accepting EC.
			// The label accept_condition is an artificial and stands for the automaton's acceptance condition.
			Expression transformedCondition             = new ExpressionLabel("accept_condition");
			ExpressionConditional transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);

			// 3) Compose Transformations
			transformation = transformation.chain(objectiveAndConditionProduct).chain(conditionProduct);

			return new Pair<>(transformation, transformedExpression);
		}

		public JDDNode computeInstantGoalStates(LTLProduct<NondetModel> objectiveAndConditionProduct)
				throws PrismException
		{
			JDDNode acceptingStates = getLtlTransformer().findAcceptingStates(objectiveAndConditionProduct);
			// States in remain from which some scheduler can enforce acceptance to maximize probability
			JDDNode result = computeProb1E(objectiveAndConditionProduct.getProductModel(), false, ALL_STATES, acceptingStates);
			JDD.Deref(acceptingStates);
			return result;
		}

		/**
		 * Compute redistribution for falsified objective.
		 * For efficiency, do not minimizing the probability to satisfy the condition, but
		 * maximize the probability to reach badStates | conditionFalsifiedStates, which is equivalent.
		 */
		public ProbabilisticRedistribution redistributeProb0Objective(LTLProduct<NondetModel> product, JDDNode conditionFalsified, JDDNode conditionMaybeFalsified)
				throws PrismException
		{
			if ( !settings.getBoolean(PrismSettings.CONDITIONAL_RESET_MDP_MINIMIZE)) {
				// Skip costly normalization
				return new ProbabilisticRedistribution();
			}

			// compute accepting states  (ECs or REACH states)
			NondetModel productModel           = product.getProductModel();
			JDDNode acceptObjectiveStates      = getLtlTransformer().findAcceptingStates(product);
			Finally<NondetModel> objectivePath = new Finally<>(productModel, acceptObjectiveStates);

			// path to non-accepting states
			JDDNode conditionFalsifiedStates            = JDD.Or(conditionFalsified.copy(), conditionMaybeFalsified.copy());
			Finally<NondetModel> conditionFalsifiedPath = new Finally<>(productModel, conditionFalsifiedStates);

			// compute redistribution
			ProbabilisticRedistribution objectiveFalsified = redistributeProb0Complement(objectivePath, conditionFalsifiedPath);
			objectivePath.clear();
			conditionFalsifiedPath.clear();
			return objectiveFalsified;
		}

		@Override
		public LtlUntilTransformer.MDP getLtlUntilTransformer()
		{
			return new LtlUntilTransformer.MDP(prism, getModelChecker());
		}

		@Override
		public FinallyLtlTransformer.MDP getFinallyLtlTransformer()
		{
			return new FinallyLtlTransformer.MDP(prism, getModelChecker());
		}
	}
}
