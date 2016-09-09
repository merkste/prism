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
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.LTLModelChecker.LTLProduct;
import prism.conditional.SimplePathProperty.Finally;
import prism.conditional.transform.GoalFailStopTransformation;
import prism.conditional.transform.GoalFailStopTransformation.GoalFailStopOperator;
import prism.conditional.transform.GoalFailStopTransformation.ProbabilisticRedistribution;
import prism.conditional.transform.LTLProductTransformer.LabeledDA;



// FIXME ALG: add comment
public interface NewLtlLtlTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewNormalFormTransformer<M, MC>
{
	// FIXME ALG: Generalize acceptance types: DMTC=all, MDP=REACH, STREETT
	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.STREETT};
//	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.STREETT};



	@Override
	default boolean canHandleObjective(M model, ExpressionConditional expression)
			throws PrismLangException
	{
		if (! NewNormalFormTransformer.super.canHandleObjective(model, expression)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		return getLtlTransformer().canHandle(model, objective.getExpression());
	}

	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression)
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
		Finally objectivePathProduct    = new Finally(productModel, acceptStates);
		JDDNode statesOfInterestProduct = product.getTransformedStatesOfInterest();

		NewFinallyLtlTransformer<M, MC> finallyLtlTransformer = getFinallyLtlTransformer();
		getLog().println("\nDelegating to " + finallyLtlTransformer.getName());
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = finallyLtlTransformer.transformNormalForm(productModel, objectivePathProduct, conditionDA.liftToProduct(product), statesOfInterestProduct);
		conditionDA.clear();

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.compose(product);
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
		Finally conditionPathProduct    = new Finally(productModel, acceptStates);
		JDDNode statesOfInterestProduct = product.getTransformedStatesOfInterest();

		// 2) Normal-Form Transformation
		NewLtlUntilTransformer<M,MC> ltlUntilTransformer = getLtlUntilTransformer();
		getLog().println("\nDelegating to " + ltlUntilTransformer.getName());
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = ltlUntilTransformer.transformNormalForm(productModel, objectiveDA.liftToProduct(product), conditionPathProduct, statesOfInterestProduct);
		objectiveDA.clear();

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.compose(product);
		ExpressionConditional transformedExpression  = result.second;

		return new Pair<>(transformation, transformedExpression);
	}

	default GoalFailStopOperator<M> configureOperator(M model, JDDNode instantGoalStates, JDDNode instantFailStates, JDDNode statesOfInterest)
			throws PrismException
	{
		return configureOperator(model, new ProbabilisticRedistribution(), new ProbabilisticRedistribution(), new ProbabilisticRedistribution(), instantGoalStates, instantFailStates, statesOfInterest);
	}

	NewLtlUntilTransformer<M, MC> getLtlUntilTransformer();

	NewFinallyLtlTransformer<M, MC> getFinallyLtlTransformer();



	public static class DTMC extends NewNormalFormTransformer.DTMC implements NewLtlLtlTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<ProbModel>, ExpressionConditional> transformNormalForm(ProbModel model, LabeledDA objectiveDA, LabeledDA conditionDA, JDDNode statesOfInterest)
				throws PrismException
		{
			// Since each BSCC is either accepting or not, it suffices to reach the set of accepting states
			return transformNormalFormReachCondition(model, objectiveDA, conditionDA, statesOfInterest);
		}

		@Override
		public NewLtlUntilTransformer.DTMC getLtlUntilTransformer()
		{
			return new NewLtlUntilTransformer.DTMC(getModelChecker());
		}

		@Override
		public NewFinallyLtlTransformer.DTMC getFinallyLtlTransformer()
		{
			return new NewFinallyLtlTransformer.DTMC(getModelChecker());
		}
	}



	public static class MDP extends NewNormalFormTransformer.MDP implements NewLtlLtlTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<NondetModel>, ExpressionConditional> transformNormalForm(NondetModel model, LabeledDA objectiveDA, LabeledDA conditionDA, JDDNode statesOfInterest)
				throws PrismException
		{
			if (conditionDA.getAutomaton().getAcceptance().getType() == AcceptanceType.REACH) {
				return transformNormalFormReachCondition(model, objectiveDA, conditionDA, statesOfInterest);
			} else if (objectiveDA.getAutomaton().getAcceptance().getType() == AcceptanceType.REACH) {
				return transformNormalFormReachObjective(model, objectiveDA, conditionDA, statesOfInterest);
			}

			// 1) LTL Product Transformation for Condition
			LTLProduct<NondetModel> conditionProduct = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
			NondetModel conditionProductModel        = conditionProduct.getProductModel();
			JDDNode statesOfInterset                 = conditionProduct.getTransformedStatesOfInterest();
			JDDNode acceptStates                     = getLtlTransformer().findAcceptingStates(conditionProduct);
			Finally conditionPath                    = new Finally(conditionProductModel, acceptStates);

			// FIXME ALG: consider whether this is actually an error in a normal-form transformation
			JDDNode conditionFalsifiedStates = computeProb0(conditionProductModel, conditionPath);
			checkSatisfiability(conditionFalsifiedStates, statesOfInterset);
			conditionPath.clear();

			// compute bad states
			JDDNode badStates = computeBadStates(conditionProduct, conditionFalsifiedStates);

			// 2) LTL Product Transformation for Objective
			LTLProduct<NondetModel> objectiveAndConditionProduct = getLtlTransformer().constructProduct(conditionProductModel, objectiveDA.liftToProduct(conditionProduct), statesOfInterset);
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

			// 4) Conjunction of Objective and Condition Acceptance
			AcceptanceStreettDD objectiveAndConditionAcceptance = new AcceptanceStreettDD();
			objectiveAndConditionAcceptance.addAll((AcceptanceStreettDD) objectiveAndConditionProduct.getAcceptance().clone());
			objectiveAndConditionAcceptance.addAll(conditionAcceptanceLifted);
			objectiveAndConditionProduct.getAcceptance().clear();
			objectiveAndConditionProduct.setAcceptance(objectiveAndConditionAcceptance);

			// 5) Objective & Condition Goal States
			// compute states where objective and condition can be satisfied
			JDDNode instantGoalStates = findAcceptingStatesMax(objectiveAndConditionProduct);

			// transform goal-fail-stop
			GoalFailStopOperator<NondetModel> operator             = configureOperator(objectiveAndConditionModel, instantGoalStates, conditionFalsifiedLifted, objectiveAndConditionStatesOfInterest);
			GoalFailStopTransformation<NondetModel> transformation = new GoalFailStopTransformation<>(objectiveAndConditionModel, operator, badStatesLifted);
			// build expression
			ExpressionLabel goal                        = new ExpressionLabel(transformation.getGoalLabel());
			ExpressionTemporal transformedObjectiveTmp  = Expression.Finally(goal);
			ExpressionProb transformedObjective         = new ExpressionProb(transformedObjectiveTmp, MinMax.max(), "=", null);
			// All paths satisfying the condition eventually reach an accepting EC.
			// The label accept_condition is an artificial and stands for the automaton's acceptance condition.
			Expression transformedCondition             = new ExpressionLabel("accept_condition");
			ExpressionConditional transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);


			// 3) Compose Transformations
			transformation = transformation.compose(objectiveAndConditionProduct).compose(conditionProduct);

			return new Pair<>(transformation, transformedExpression);
		}

		public NewLtlUntilTransformer.MDP getLtlUntilTransformer()
		{
			return new NewLtlUntilTransformer.MDP(getModelChecker());
		}

		@Override
		public NewFinallyLtlTransformer.MDP getFinallyLtlTransformer()
		{
			return new NewFinallyLtlTransformer.MDP(getModelChecker());
		}
	}
}
