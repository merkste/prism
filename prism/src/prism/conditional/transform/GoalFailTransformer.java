package prism.conditional.transform;

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
import parser.ast.ExpressionTemporal;
import prism.LTLModelChecker;
import prism.LTLModelChecker.LTLProduct;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.OpRelOpBound;
import prism.Pair;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.conditional.NewConditionalTransformer;
import prism.conditional.SimplePathProperty.Finally;
import prism.conditional.SimplePathProperty.Until;
import prism.conditional.transform.GoalFailStopTransformation.GoalFailStopOperator;
import prism.conditional.transform.GoalFailStopTransformer.NormalFormTransformation;
import prism.conditional.transform.LTLProductTransformer.LabeledDA;



public interface GoalFailTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewConditionalTransformer<M, MC>
{
	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.RABIN, AcceptanceType.GENERALIZED_RABIN, AcceptanceType.STREETT};

	public static final boolean ROW    = true;
	public static final boolean COLUMN = false;



	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression)
	{
		Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		Expression until = ExpressionInspector.removeNegation(normalized);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
	}

	@Override
	default boolean canHandleObjective(M model, ExpressionConditional expression)
			throws PrismLangException
	{
		// can handle probabilities only
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel = objective.getRelopBoundInfo(getModelChecker().getConstantValues());
		// can handle maximal probabilities only
		if (oprel.getMinMax(model.getModelType()).isMin()) {
			return false;
		}
//		return getLtlTransformer().canHandle(model, objective.getExpression());
		return LTLModelChecker.isSupportedLTLFormula(model.getModelType(), objective.getExpression());
	}

	@Override
	default NormalFormTransformation<M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: build omega automaton
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTemp = objective.getExpression();
		LabeledDA objectiveDA    = getLtlTransformer().constructDA(model, objectiveTemp, ACCEPTANCE_TYPES);

		// 2) Condition: compute simple path property
		Expression conditionTemp = expression.getCondition();
		Until conditionPath      = new Until(conditionTemp, getModelChecker(), true);

		Pair<GoalFailStopTransformation<M>,ExpressionConditional> result = transform(model, objectiveDA, conditionPath, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression  = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective        = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded, transformation.getFailLabel(), transformation.getBadLabel());
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transform(M model, LabeledDA objectiveDA, Until conditionPath, JDDNode statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Objective
		LTLProduct<M> product = getLtlTransformer().constructProduct(model, objectiveDA, statesOfInterest);
		M productModel        = product.getTransformedModel();

		// Lift state sets to product model
		Until conditionPathProduct = conditionPath.copy(productModel);
		conditionPath.clear();

		// 2) Normal-Form Transformation
		Pair<GoalFailStopTransformation<M>,ExpressionConditional> result;
		switch (product.getAcceptance().getType()) {
		case REACH:
			result = transformReach(product, conditionPathProduct);
			break;
		case RABIN:
		case GENERALIZED_RABIN:
		case STREETT:
			result = transformOmega(product, conditionPathProduct);
			break;
		default:
			throw new PrismException("unsupported acceptance type: " + product.getAcceptance().getType());
		}

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.compose(product);
		return new Pair<>(transformation, result.second);
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformReach(LTLProduct<M> product, Until conditionPath)
			throws PrismException
	{
		if (product.getAcceptance().getType() != AcceptanceType.REACH) {
			throw new IllegalArgumentException("Expected acceptance REACH.");
		}
		GoalFailStopTransformer<M, MC> goalFailStopTransformer = getGoalFailStopTransformer();
		getLog().println("\nDetected acceptance REACH for objective, delegating to " + goalFailStopTransformer.getName());

		M productModel = product.getProductModel();

		JDDNode conditionNormalStates = computeNormalFormStates(productModel, conditionPath);
		JDDNode restrict              = computeSuccStar(productModel, conditionNormalStates);
		JDD.Deref(conditionNormalStates);

		if (conditionPath.isNegated()) {
			// states containing only ECs that satisfy the condition
			JDDNode conditionUnsatisfied       = computeUnsatified(productModel, conditionPath);
			JDDNode conditionSatisfiableStates = JDD.Not(conditionUnsatisfied);

			// Although ECs in succ* generally do not satisfy a negated condition,
			// the union is sane, since those states will be probabilistic normal-form states.
			restrict = JDD.Or(restrict, conditionSatisfiableStates);
		}
		JDDNode acceptStates  = getLtlTransformer().findAcceptingStates(product, restrict);
		Finally objectivePath = new Finally(productModel, acceptStates);
		JDD.Deref(restrict);

		// FIXME ALG: reuse computation of conditionNormalStates and conditionUnsatisfied?
		JDDNode statesOfInterest = product.getTransformedStatesOfInterest();
		return goalFailStopTransformer.transform(productModel, objectivePath, conditionPath, statesOfInterest);
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformOmega(LTLProduct<M> product, Until conditionPath)
			throws PrismException
	{
		// FIXME ALG: If the condition is negated, it should be possible
		//            to move from ECs that satisfy objective and condition to goal immediately

		M productModel               = product.getProductModel();
		JDDNode conditionUnsatisfied = computeUnsatified(productModel, conditionPath);

		// FIXME ALG: consider whether this is actually an error in a normal-form transformation
		JDDNode statesOfInterest     = product.getTransformedStatesOfInterest();
		checkSatisfiability(conditionUnsatisfied, statesOfInterest);
		JDD.Deref(statesOfInterest);

		// compute ECs in succ*(terminal)
		JDDNode conditionNormalStates      = computeNormalFormStates(productModel, conditionPath);
		JDDNode succConditionNormalStates  = computeSuccStar(productModel, conditionNormalStates);
		JDDNode acceptStatesPostCondition  = getLtlTransformer().findAcceptingStates(product, succConditionNormalStates);
		Finally objectivePath              = new Finally(productModel, acceptStatesPostCondition);
		JDD.Deref(succConditionNormalStates);

		// compute probabilities for objective
		JDDNode objectiveNormalProbs  = computeNormalFormProbs(productModel, objectivePath);
		objectivePath.clear();

		// transform model
		GoalFailStopOperator<M> operator = configureOperator(productModel, conditionUnsatisfied.copy(), JDD.Constant(0), objectiveNormalProbs, conditionNormalStates, JDD.Constant(0));

		// compute badStates
		JDDNode badStates = computeMaybeUnsatified(productModel, conditionPath, conditionUnsatisfied);

		// transform model
		GoalFailStopTransformation<M> transformation = new GoalFailStopTransformation<>(productModel, operator, badStates);

		// transform expression
		ExpressionLabel goal               = new ExpressionLabel(transformation.getGoalLabel());
		Expression transformedObjectiveTmp = Expression.Finally(goal);
		if (conditionPath.isNegated()) {
			// There might be paths that satisfy objective and condition but do not end in goal
			JDDNode conditionSatisfiableStates = JDD.Not(conditionUnsatisfied);
			JDDNode acceptStatesConditionECs   = getLtlTransformer().findAcceptingStates(product, conditionSatisfiableStates);
			JDD.Deref(conditionSatisfiableStates);

			if (! acceptStatesConditionECs.equals(JDD.ZERO)) {
				// All paths satisfying the objective and condition eventually reach goal or an accepting EC.
				String acceptLabel      = transformation.getTransformedModel().addUniqueLabelDD("accept_objective", acceptStatesConditionECs);
				transformedObjectiveTmp = Expression.Or(transformedObjectiveTmp, Expression.Finally(new ExpressionLabel(acceptLabel)));
			} else {
				JDD.Deref(acceptStatesConditionECs);
			}
		} else {
			JDD.Deref(conditionUnsatisfied);
		}
		ExpressionProb transformedObjective = new ExpressionProb(transformedObjectiveTmp, MinMax.max(), "=", null);
		ExpressionTemporal transformedCondition;
		if (conditionPath.isNegated()) {
			// All paths violating the condition eventually reach the fail state.
			ExpressionLabel fail      = new ExpressionLabel(transformation.getFailLabel());
			transformedCondition      = Expression.Globally(Expression.Not(fail));
		} else {
			// All paths satisfying the condition eventually reach the goal or stop state.
			ExpressionLabel stopLabel = new ExpressionLabel(transformation.getStopLabel());
			transformedCondition      = Expression.Finally(Expression.Parenth(Expression.Or(goal, stopLabel)));
		}
		ExpressionConditional transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);

		conditionPath.clear();
		return new Pair<>(transformation, transformedExpression);
	}

	default JDDNode checkSatisfiability(JDDNode conditionUnsatisfied, JDDNode statesOfInterest)
			throws UndefinedTransformationException
	{
		if (JDD.IsContainedIn(statesOfInterest, conditionUnsatisfied)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		return conditionUnsatisfied;
	}

	JDDNode computeUnsatified(M model, Until until);

	JDDNode computeMaybeUnsatified(M model, Until until, JDDNode unsatisfiedStates);

	JDDNode computeNormalFormStates(M model, Until until);

	JDDNode computeNormalFormProbs(M model, Until until) throws PrismException;

	GoalFailStopOperator<M> configureOperator(M model, JDDNode conditionUnsatisfied, JDDNode objectiveNormalStates, JDDNode objectiveNormalProbs,
			JDDNode conditionNormalStates, JDDNode conditionProbs) throws PrismException;

	GoalFailStopTransformer<M, MC> getGoalFailStopTransformer();



	public class DTMC extends NewConditionalTransformer.DTMC implements GoalFailTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode computeUnsatified(ProbModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0(model, until.getRemain(), until.getGoal());
			}
		}

		@Override
		public JDDNode computeMaybeUnsatified(ProbModel model, Until until, JDDNode unsatisfiedStates)
		{
			// DTMCs are purely probabilistic
			return JDD.Constant(0);
		}

		@Override
		public JDDNode computeNormalFormStates(ProbModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1(model, until.getRemain(), until.getGoal());
			}
// FIXME ALG: fishy: should be all states with Pmin=1 (Condition)
//			JDDNode conditionWeakRemain   = getWeakRemainStates(model, conditionRemain, conditionGoal, conditionNegated);
//			JDDNode conditionWeakGoal     = getWeakGoalStates(model, conditionRemain, conditionGoal, conditionNegated);
//			return computeProb1(model, conditionWeakRemain, conditionWeakGoal);
		}

		@Override
		public JDDNode computeNormalFormProbs(ProbModel model, Until until) throws PrismException
		{
			return computeUntilProbs(model, until);
		}

		@Override
		public GoalFailStopOperator<ProbModel> configureOperator(ProbModel model, JDDNode conditionUnsatisfied, JDDNode objectiveNormalStates, JDDNode objectiveNormalProbs,
				JDDNode conditionNormalStates, JDDNode conditionProbs) throws PrismException
		{
			return new GoalFailStopOperator.DTMC(model, objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, conditionProbs, conditionUnsatisfied, getLog());
		}

		@Override
		public GoalFailStopTransformer<ProbModel, ProbModelChecker> getGoalFailStopTransformer()
		{
			return new GoalFailStopTransformer.DTMC(getModelChecker());
		}
	}



	public class MDP extends NewConditionalTransformer.MDP implements GoalFailTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode computeUnsatified(NondetModel model, Until until)
		{
			return until.isNegated() ? computeProb1A(model, until.getRemain(), until.getGoal()) : computeProb0A(model, until.getRemain(), until.getGoal());
		}

		@Override
		public JDDNode computeMaybeUnsatified(NondetModel model, Until until, JDDNode unsatisfiedStates)
		{
			JDDNode maybeUnsatisfiedStates;
			if (until.isNegated()) {
				maybeUnsatisfiedStates = computeProb1E(model, until.getRemain(), until.getGoal());
			} else {
				maybeUnsatisfiedStates = computeProb0E(model, until.getRemain(), until.getGoal());
			}
			return JDD.And(maybeUnsatisfiedStates, JDD.Not(unsatisfiedStates.copy()));
		}

		@Override
		public JDDNode computeNormalFormStates(NondetModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0A(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1A(model, until.getRemain(), until.getGoal());
			}
// FIXME ALG: fishy: should be all states with Pmin=1 (Condition)
//			JDDNode conditionWeakRemain   = getWeakRemainStates(model, conditionRemain, conditionGoal, conditionNegated);
//			JDDNode conditionWeakGoal     = getWeakGoalStates(model, conditionRemain, conditionGoal, conditionNegated);
//			return computeProb1A(model, conditionWeakRemain, conditionWeakGoal);
		}

		@Override
		public JDDNode computeNormalFormProbs(NondetModel model, Until until) throws PrismException
		{
			return computeUntilMaxProbs(model, until);
		}

		public GoalFailStopOperator<NondetModel> configureOperator(NondetModel model, JDDNode conditionUnsatisfied, JDDNode objectiveNormalStates, JDDNode objectiveNormalProbs,
				JDDNode conditionNormalStates, JDDNode conditionProbs) throws PrismException
		{
			return new GoalFailStopOperator.MDP(model, objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, conditionProbs, conditionUnsatisfied, getLog());
		}

		@Override
		public GoalFailStopTransformer<NondetModel, NondetModelChecker> getGoalFailStopTransformer()
		{
			return new GoalFailStopTransformer.MDP(getModelChecker());
		}
	}
}