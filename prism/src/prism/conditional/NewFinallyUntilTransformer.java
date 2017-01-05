package prism.conditional;

import explicit.MinMax;
import explicit.conditional.ExpressionInspector;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.Model;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.Pair;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.conditional.SimplePathProperty.Globally;
import prism.conditional.SimplePathProperty.Reach;
import prism.conditional.transform.GoalFailStopTransformation;
import prism.conditional.transform.GoalFailStopTransformation.GoalFailStopOperator;
import prism.conditional.transform.GoalFailStopTransformation.ProbabilisticRedistribution;



// FIXME ALG: add comment
public interface NewFinallyUntilTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewNormalFormTransformer<M, MC>
{
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

		// 1) Objective: compute simple path property
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTmp  = objective.getExpression();
		Reach<M> objectivePath   = (Reach<M>) computeSimplePathProperty(model, objectiveTmp);

		// 2) Condition: compute simple path property
		Expression conditionTmp = ExpressionInspector.normalizeExpression(expression.getCondition());
		Reach<M> conditionPath  = (Reach<M>) computeSimplePathProperty(model, conditionTmp);

		// 3) Transform model
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = transformNormalForm(objectivePath, conditionPath, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression  = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective        = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded, transformation.getFailLabel(), transformation.getBadLabel());
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(Reach<M> objectivePath, Reach<M> conditionPath, JDDNode statesOfInterest)
			throws PrismException
	{
		objectivePath.requireSameModel(conditionPath);

		// FIXME ALG: consider whether this is actually an error in a normal-form transformation
		JDDNode conditionFalsifiedStates = checkSatisfiability(conditionPath, statesOfInterest);

		// compute badStates
		JDDNode badStates = computeBadStates(conditionPath, conditionFalsifiedStates);

		// FIXME ALG: reuse precomputation?
		// compute redistribution for satisfied objective
		ProbabilisticRedistribution objectiveSatisfied = redistributeProb1(objectivePath, conditionPath);

		// compute redistribution for satisfied condition
		ProbabilisticRedistribution conditionSatisfied = redistributeProb1(conditionPath, objectivePath);

		// compute redistribution for falsified objective
		ProbabilisticRedistribution objectiveFalsified = redistributeProb0Objective(objectivePath, conditionPath);

		// compute states where objective and condition can be satisfied
		JDDNode instantGoalStates = computeInstantGoalStates(objectivePath, objectiveSatisfied.getStates(), objectiveFalsified.getStates(), conditionPath, conditionSatisfied.getStates(), conditionFalsifiedStates.copy());

		// transform goal-fail-stop
		M model                                      = objectivePath.getModel();
		GoalFailStopOperator<M> operator             = configureOperator(model, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, statesOfInterest);
		GoalFailStopTransformation<M> transformation = new GoalFailStopTransformation<>(model, operator, badStates);

		// build expression 
		ExpressionLabel goal                       = new ExpressionLabel(transformation.getGoalLabel());
		ExpressionTemporal transformedObjectiveTmp = Expression.Finally(goal);
		ExpressionProb transformedObjective        = new ExpressionProb(transformedObjectiveTmp, MinMax.max(), "=", null);
		Expression transformedCondition;
		if (conditionPath.isCoSafe()) {
			// All paths satisfying the condition eventually reach the goal or stop state.
			ExpressionLabel stop = new ExpressionLabel(transformation.getStopLabel());
			transformedCondition = Expression.Finally(Expression.Parenth(Expression.Or(goal, stop)));
		} else {
			// All paths violating the condition eventually reach the fail state.
			ExpressionLabel fail = new ExpressionLabel(transformation.getFailLabel());
			transformedCondition = Expression.Globally(Expression.Not(fail));
		}
		ExpressionConditional transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);

		objectivePath.clear();
		conditionPath.clear();
		return new Pair<>(transformation, transformedExpression);
	}

	ProbabilisticRedistribution redistributeProb0Objective(Reach<M> objectivePath, Reach<M> conditionPath)
			throws PrismException;

	/**
	 * [ REFS: <i>result</i>, DEREFS: <i>objectiveSatisfiedStates, objectiveFalsifiedStates, conditionSatisfiedStates, conditionFalsifiedStates</i> ]
	 */
	JDDNode computeInstantGoalStates(Reach<M> objectivePath, JDDNode objectiveSatisfiedStates, JDDNode objectiveFalsifiedStates, Reach<M> conditionPath, JDDNode conditionSatisfiedStates, JDDNode conditionFalsifiedStates)
			throws PrismException;



	public static class DTMC extends NewNormalFormTransformer.DTMC implements NewFinallyUntilTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode computeInstantGoalStates(Reach<ProbModel> objectivePath, JDDNode objectiveSatisfiedStates, JDDNode objectiveFalsifiedStates, Reach<ProbModel> conditionPath, JDDNode conditionSatisfiedStates, JDDNode conditionFalsifiedStates)
				throws PrismException
		{
			objectivePath.requireSameModel(conditionPath);

			JDD.Deref(objectiveFalsifiedStates, conditionFalsifiedStates);
			return JDD.And(objectiveSatisfiedStates, conditionSatisfiedStates);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0Objective(Reach<ProbModel> objectivePath, Reach<ProbModel> conditionPath)
				throws PrismException
		{
			// Always normalize
			return redistributeProb0(objectivePath, conditionPath);
		}
	}



	public static class MDP extends NewNormalFormTransformer.MDP implements NewFinallyUntilTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode computeInstantGoalStates(Reach<NondetModel> objectivePath, JDDNode objectiveSatisfiedStates, JDDNode objectiveFalsifiedStates, Reach<NondetModel> conditionPath, JDDNode conditionSatisfiedStates, JDDNode conditionFalsifiedStates)
			throws PrismException
		{
			objectivePath.requireSameModel(conditionPath);
			NondetModel model = objectivePath.getModel();

			JDDNode instantGoalStates = JDD.And(objectiveSatisfiedStates, conditionSatisfiedStates);

			// exclude objective/condition falsified states
			JDDNode falsifiedStates    = JDD.Or(objectiveFalsifiedStates, conditionFalsifiedStates);
			JDDNode notFalsifiedStates = JDD.And(model.getReach().copy(), JDD.Not(falsifiedStates));

			// Do both, the objective and the condition, specify behavior in the limit?
			if (!objectivePath.isCoSafe() && !conditionPath.isCoSafe()) {
				// Compute ECs that never falsify the objective/condition
				Globally<NondetModel> neverFalsified = new Globally<>(model, notFalsifiedStates.copy());
				JDDNode neverFalsifiedStates         = computeProb1E(neverFalsified);
				neverFalsified.clear();
				instantGoalStates = JDD.Or(instantGoalStates.copy(), neverFalsifiedStates);
			}
			// enlarge target set
			JDDNode result = computeProb1E(model, false, notFalsifiedStates, instantGoalStates);
			JDD.Deref(notFalsifiedStates, instantGoalStates);
			return result;
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0Objective(Reach<NondetModel> objectivePath, Reach<NondetModel> conditionPath)
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
