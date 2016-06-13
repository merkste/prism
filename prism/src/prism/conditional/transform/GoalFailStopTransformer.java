package prism.conditional.transform;

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
import prism.ModelTransformation;
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

public interface GoalFailStopTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewConditionalTransformer<M, MC>
{
	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression)
	{
		Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		Expression until = ExpressionInspector.removeNegation(normalized);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
	}

	@Override
	default boolean canHandleObjective(M model, ExpressionConditional expression) throws PrismLangException
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
		// can handle simple finally formulae only
		Expression normalized = ExpressionInspector.normalizeExpression(objective.getExpression());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	default NormalFormTransformation<M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: compute simple path property
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTemp = objective.getExpression();
		Finally objectivePath = new Finally(objectiveTemp, getModelChecker(), true);

		// 2) Condition: compute simple path property
		Expression conditionTemp = ExpressionInspector.normalizeExpression(expression.getCondition());
		Until conditionPath = new Until(conditionTemp, getModelChecker(), true);

		// 3) Transform model
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = transform(model, objectivePath, conditionPath, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(),
				objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded, transformation.getFailLabel(), transformation.getBadLabel());
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transform(M model, Until objectivePath, Until conditionPath, JDDNode statesOfInterest)
			throws PrismException
	{
		// FIXME ALG: consider whether this is actually an error in a normal-form transformation
		JDDNode conditionUnsatisfied = computeUnsatified(model, conditionPath);
		checkSatisfiability(conditionUnsatisfied, statesOfInterest);
		JDD.Deref(statesOfInterest);

		// compute normal-form states and probabilities for objective
		// FIXME ALG: reuse precomputation?
		JDDNode objectiveNormalStates = computeNormalFormStates(model, objectivePath);
		JDDNode objectiveNormalProbs = computeNormalFormProbs(model, objectivePath);

		// compute normal-form states and probabilities for condition
		// FIXME ALG: reuse precomputation?
		JDDNode conditionNormalStates = computeNormalFormStates(model, conditionPath);
		////>>> Debug: print conditionNormalStates
		//getLog().println("conditionNormalStates:");
		//JDD.PrintMinterms(getLog(), conditionNormalStates.copy());
		//new StateValuesMTBDD(conditionNormalStates.copy(), model).print(getLog());
		conditionNormalStates = JDD.And(conditionNormalStates, JDD.Not(objectiveNormalStates.copy()));
		JDDNode conditionProbs = computeNormalFormProbs(model, conditionPath);

		////>>> Debug: print conditionUnsatisfied
		//getLog().println("conditionUnsatisfied:");
		//JDD.PrintMinterms(getLog(), conditionUnsatisfied.copy());
		//new StateValuesMTBDD(conditionUnsatisfied.copy(), model).print(getLog());
		////>>> Debug: print objectiveNormalStates
		//getLog().println("objectiveNormalStates:");
		//JDD.PrintMinterms(getLog(), objectiveNormalStates.copy());
		//new StateValuesMTBDD(objectiveNormalStates.copy(), model).print(getLog());

		// transform model
		GoalFailStopOperator<M> operator = configureOperator(model, conditionUnsatisfied, objectiveNormalStates, objectiveNormalProbs, conditionNormalStates,
				conditionProbs);

		// Compute badStates
		JDDNode badStates = computeMaybeUnsatified(model, conditionPath, conditionUnsatisfied);

		// transform Model
		GoalFailStopTransformation<M> transformation = new GoalFailStopTransformation<>(model, operator, badStates);

		// build expression 
		ExpressionLabel goal = new ExpressionLabel(transformation.getGoalLabel());
		ExpressionLabel fail = new ExpressionLabel(transformation.getFailLabel());
		ExpressionTemporal transformedObjectiveTmp = Expression.Finally(goal);
		ExpressionProb transformedObjective = new ExpressionProb(transformedObjectiveTmp, MinMax.max(), "=", null);
		Expression transformedCondition;
		if (conditionPath.isNegated()) {
			// All paths violating the condition eventually reach the fail state.
			transformedCondition = Expression.Globally(Expression.Not(fail));
		} else {
			// All paths satisfying the condition eventually reach the goal or stop state.
			ExpressionLabel stop = new ExpressionLabel(transformation.getStopLabel());
			transformedCondition = Expression.Finally(Expression.Parenth(Expression.Or(goal, stop)));
		}
		ExpressionConditional transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);

		objectivePath.clear();
		conditionPath.clear();
		return new Pair<>(transformation, transformedExpression);
	}

	default JDDNode checkSatisfiability(JDDNode conditionUnsatisfied, JDDNode statesOfInterest) throws UndefinedTransformationException
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

	public class DTMC extends NewConditionalTransformer.DTMC implements GoalFailStopTransformer<ProbModel, ProbModelChecker>
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
		public GoalFailStopOperator<ProbModel> configureOperator(ProbModel model, JDDNode conditionUnsatisfied, JDDNode objectiveNormalStates,
				JDDNode objectiveNormalProbs, JDDNode conditionNormalStates, JDDNode conditionProbs) throws PrismException
		{
			return new GoalFailStopOperator.DTMC(model, objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, conditionProbs,
					conditionUnsatisfied, getLog());
		}
	}

	public class MDP extends NewConditionalTransformer.MDP implements GoalFailStopTransformer<NondetModel, NondetModelChecker>
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

		public GoalFailStopOperator<NondetModel> configureOperator(NondetModel model, JDDNode conditionUnsatisfied, JDDNode objectiveNormalStates,
				JDDNode objectiveNormalProbs, JDDNode conditionNormalStates, JDDNode conditionProbs) throws PrismException
		{
			return new GoalFailStopOperator.MDP(model, objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, conditionProbs, conditionUnsatisfied,
					getLog());
		}
	}

	public class NormalFormTransformation<M extends ProbModel> extends BasicModelExpressionTransformation<M, M>
	{
		protected String failLabel;
		protected String badLabel;

		public NormalFormTransformation(ModelTransformation<M, M> transformation, ExpressionConditional expression, ExpressionConditional transformedExpression,
				String failLabel, String badLabel)
		{
			super(transformation, expression, transformedExpression);
			this.failLabel = failLabel;
			this.badLabel  = badLabel;
		}

		public String getFailLabel()
		{
			return failLabel;
		}

		public String getBadLabel()
		{
			return badLabel;
		}

		public ExpressionConditional getOriginalExpression()
		{
			return (ExpressionConditional) originalExpression;
		}

		public ExpressionConditional getTransformedExpression()
		{
			return (ExpressionConditional) transformedExpression;
		}
	}
}