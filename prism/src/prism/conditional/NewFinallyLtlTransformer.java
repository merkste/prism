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
import prism.ECComputer;
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
import prism.conditional.SimplePathProperty.Until;
import prism.conditional.transform.GoalFailStopTransformation;
import prism.conditional.transform.GoalFailStopTransformation.GoalFailStopOperator;
import prism.conditional.transform.GoalFailStopTransformation.ProbabilisticRedistribution;
import prism.conditional.transform.LTLProductTransformer.LabeledDA;



// FIXME ALG: add comment
public interface NewFinallyLtlTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewNormalFormTransformer<M, MC>
{
	// FIXME ALG: Generalize acceptance types: all
	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.RABIN, AcceptanceType.GENERALIZED_RABIN, AcceptanceType.STREETT};
//	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.RABIN, AcceptanceType.GENERALIZED_RABIN, AcceptanceType.STREETT};



	@Override
	default boolean canHandleObjective(M model, ExpressionConditional expression)
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

		// 1) Objective: compute simple path property
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTmp  = objective.getExpression();
		Until objectivePath      = new Until(objectiveTmp, getModelChecker(), true);

		// 2) Condition: build omega automaton
		Expression conditionTmp = expression.getCondition();
		LabeledDA conditionDA   = getLtlTransformer().constructDA(model, conditionTmp, ACCEPTANCE_TYPES);

		// 3) Transform model
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = transformNormalForm(model, objectivePath, conditionDA, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression  = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective        = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded, transformation.getFailLabel(), transformation.getBadLabel());
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(M model, Until objectivePath, LabeledDA conditionDA, JDDNode statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Condition
		LTLProduct<M> product = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
		M productModel        = product.getTransformedModel();
		//    Lift state sets to product model
		Until objectivePathProduct = objectivePath.copy(productModel);
		objectivePath.clear();

		// 2) Normal-Form Transformation
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = transformNormalForm(product, objectivePathProduct);

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.compose(product);
		ExpressionConditional transformedExpression  = result.second;

		return new Pair<>(transformation, transformedExpression);
	}

	Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(LTLProduct<M> product, Until objectivePath)
			throws PrismException, UndefinedTransformationException;

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalFormReach(LTLProduct<M> product, Until objectivePath)
			throws PrismException
	{
		// FIXME ALG: remove redundancy?
		M productModel           = product.getProductModel();
		JDDNode statesOfInterest = product.getTransformedStatesOfInterest();
		JDDNode acceptStates     = getLtlTransformer().findAcceptingStates(product);
		Finally conditionPath    = new Finally(productModel, acceptStates);

		NewFinallyUntilTransformer<M, MC> finallyUntilTransformer = getFinallyUntilTransformer();
		getLog().println("\nDelegating to " + finallyUntilTransformer.getName());
		return finallyUntilTransformer.transformNormalForm(productModel, objectivePath, conditionPath, statesOfInterest);
	}


	default ProbabilisticRedistribution redistributeProb1MaxProbs(M model, Until pathProb1, Until pathMaxProbs)
			throws PrismException
	{
		JDDNode states        = computeProb1(model, pathProb1);
		JDDNode probabilities = computeUntilMaxProbs(model, pathMaxProbs);
		return new ProbabilisticRedistribution(states, probabilities);
	}

	default GoalFailStopOperator<M> configureOperator(M model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution stopFail, JDDNode instantGoalStates, JDDNode instantFailStates, JDDNode statesOfInterest)
			throws PrismException
	{
		return configureOperator(model, goalFail, new ProbabilisticRedistribution(), stopFail, instantGoalStates, instantFailStates, statesOfInterest);
	}

	JDDNode computeUntilMaxProbs(M model, Until until) throws PrismException;

	NewFinallyUntilTransformer<M, MC> getFinallyUntilTransformer();



	public static class DTMC extends NewNormalFormTransformer.DTMC implements NewFinallyLtlTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<ProbModel>, ExpressionConditional> transformNormalForm(LTLProduct<ProbModel> product, Until objectivePath)
				throws PrismException, UndefinedTransformationException
		{
			// Since each BSCC is either accepting or not, it suffices to reach the set of accepting states
			return transformNormalFormReach(product, objectivePath);
		}

		@Override
		public JDDNode computeUntilMaxProbs(ProbModel model, Until until) throws PrismException
		{
			return computeUntilProbs(model, until);
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
		public Pair<GoalFailStopTransformation<NondetModel>, ExpressionConditional> transformNormalForm(LTLProduct<NondetModel> product, Until objectivePath)
				throws PrismException, UndefinedTransformationException
		{
			if (product.getAcceptance().getType() == AcceptanceType.REACH) {
				return transformNormalFormReach(product, objectivePath);
			}

			NondetModel productModel = product.getProductModel();
			JDDNode statesOfInterest = product.getTransformedStatesOfInterest();
			JDDNode acceptStates     = getLtlTransformer().findAcceptingStates(product);
			Finally conditionPath    = new Finally(productModel, acceptStates);

			// FIXME ALG: consider whether this is actually an error in a normal-form transformation
			JDDNode conditionFalsifiedStates = computeProb0(productModel, conditionPath);
			checkSatisfiability(conditionFalsifiedStates, statesOfInterest);

			// compute bad states
			JDDNode badStates = computeBadStates(product, conditionFalsifiedStates);

			// compute redistribution for satisfied objective
			ProbabilisticRedistribution objectiveSatisfied = redistributeProb1MaxProbs(productModel, objectivePath, conditionPath);

			// compute redistribution for falsified objective
			// For efficiency, do not minimizing the probability to satisfy the condition, but
			// maximize the probability to reach badStates | conditionFalsifiedStates, which is equivalent.
			JDDNode nonAcceptingStates                     = JDD.Or(badStates.copy(), conditionFalsifiedStates.copy());
			ProbabilisticRedistribution objectiveFalsified = redistributeProb0MaxProbs(productModel, objectivePath, nonAcceptingStates);

			JDDNode instantGoalStates = computeInstantGoalStates(productModel, objectivePath, objectiveFalsified.getStates(), conditionPath, conditionFalsifiedStates.copy());

			// transform goal-fail-stop
			GoalFailStopOperator<NondetModel> operator             = configureOperator(productModel, objectiveSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, statesOfInterest);
			GoalFailStopTransformation<NondetModel> transformation = new GoalFailStopTransformation<>(productModel, operator, badStates);

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

		public ProbabilisticRedistribution redistributeProb0MaxProbs(NondetModel model, Until pathStates, JDDNode nonAcceptingStates)
				throws PrismException
		{
			JDDNode states           = computeProb0A(model, pathStates);
			Finally nonAcceptingPath = new Finally(model, nonAcceptingStates);
			JDDNode maxProbabilities = computeUntilMaxProbs(model, nonAcceptingPath);
			nonAcceptingPath.clear();
			// inverse probabilities to match redistribution target states
			return new ProbabilisticRedistribution(states, JDD.Apply(JDD.MINUS, model.getReach().copy(), maxProbabilities));
		}

		/**
		 * [ REFS: <i>result</i>, DEREFS: <i> objectiveFalsifiedStates, conditionFalsifiedStates</i> ]
		 */
		public JDDNode computeInstantGoalStates(NondetModel model, Until objectivePath, JDDNode objectiveFalsifiedStates, Until conditionPath, JDDNode conditionFalsifiedStates)
				throws PrismException
		{
			if (objectivePath.isNegated()) {
				JDDNode falsifiedStates   = JDD.Or(objectiveFalsifiedStates, conditionFalsifiedStates);
				JDDNode remain            = JDD.And(model.getReach().copy(), JDD.Not(falsifiedStates));
				// find ECs that may accept condition and objective
				ECComputer ecComputer     = ECComputer.createECComputer(getModelChecker(), model);
				JDDNode restrict = JDD.And(conditionPath.getGoal().copy(), remain.copy());
				ecComputer.computeMECStates(restrict);
				JDD.Deref(restrict);
				JDDNode instantGoalStates = ecComputer.getMECStates().stream().reduce(JDD.Constant(0), JDD::Or);
				// enlarge target set
				JDDNode result = computeProb1E(model, remain, instantGoalStates);
				JDD.Deref(remain, instantGoalStates);
				return result;
			}
			JDD.Deref(objectiveFalsifiedStates, conditionFalsifiedStates);
			return JDD.Constant(0);
		}

		@Override
		public NewFinallyUntilTransformer.MDP getFinallyUntilTransformer()
		{
			return new NewFinallyUntilTransformer.MDP(getModelChecker());
		}
	}
}
