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
import prism.ECComputerDefault;
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
import prism.conditional.SimplePathProperty.Until;
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
		Until objectivePath      = new Until(objectiveTmp, getModelChecker(), true);

		// 2) Condition: compute simple path property
		Expression conditionTmp = ExpressionInspector.normalizeExpression(expression.getCondition());
		Until conditionPath     = new Until(conditionTmp, getModelChecker(), true);

		// 3) Transform model
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = transformNormalForm(model, objectivePath, conditionPath, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression  = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective        = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded, transformation.getFailLabel(), transformation.getBadLabel());
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(M model, Until objectivePath, Until conditionPath, JDDNode statesOfInterest)
			throws PrismException
	{
		// FIXME ALG: consider whether this is actually an error in a normal-form transformation
		JDDNode conditionFalsifiedStates = checkSatisfiability(model, conditionPath, statesOfInterest);

		// compute badStates
		JDDNode badStates = computeBadStates(model, conditionPath, conditionFalsifiedStates);

		// FIXME ALG: reuse precomputation?
		// compute redistribution for satisfied objective
		ProbabilisticRedistribution objectiveSatisfied = redistributeProb1(model, objectivePath, conditionPath);

		// compute redistribution for satisfied condition
		ProbabilisticRedistribution conditionSatisfied = redistributeProb1(model, conditionPath, objectivePath);

		// compute redistribution for falsified objective
		ProbabilisticRedistribution objectiveFalsified = redistributeProb0Objective(model, objectivePath, conditionPath);

		// compute states where objective and condition can be satisfied
		JDDNode instantGoalStates = computeInstantGoalStates(model, objectivePath, objectiveSatisfied.getStates(), objectiveFalsified.getStates(), conditionPath, conditionSatisfied.getStates(), conditionFalsifiedStates.copy());

		// transform goal-fail-stop
		GoalFailStopOperator<M> operator             = configureOperator(model, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, statesOfInterest);
		GoalFailStopTransformation<M> transformation = new GoalFailStopTransformation<>(model, operator, badStates);

		// build expression 
		ExpressionLabel goal                       = new ExpressionLabel(transformation.getGoalLabel());
		ExpressionTemporal transformedObjectiveTmp = Expression.Finally(goal);
		ExpressionProb transformedObjective        = new ExpressionProb(transformedObjectiveTmp, MinMax.max(), "=", null);
		Expression transformedCondition;
		if (! conditionPath.isNegated()) {
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

	ProbabilisticRedistribution redistributeProb0Objective(M model, Until objectivePath, Until conditionPath)
			throws PrismException;

	/**
	 * [ REFS: <i>result</i>, DEREFS: <i>objectiveSatisfiedStates, objectiveFalsifiedStates, conditionSatisfiedStates, conditionFalsifiedStates</i> ]
	 */
	JDDNode computeInstantGoalStates(M model, Until objectivePath, JDDNode objectiveSatisfiedStates, JDDNode objectiveFalsifiedStates, Until conditionPath, JDDNode conditionSatisfiedStates, JDDNode conditionFalsifiedStates)
			throws PrismException;



	public static class DTMC extends NewNormalFormTransformer.DTMC implements NewFinallyUntilTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode computeInstantGoalStates(ProbModel model, Until objectivePath, JDDNode objectiveSatisfiedStates, JDDNode objectiveFalsifiedStates,
				Until conditionPath, JDDNode conditionSatisfiedStates, JDDNode conditionFalsifiedStates) throws PrismException
		{
			JDD.Deref(objectiveFalsifiedStates, conditionFalsifiedStates);
			return JDD.And(objectiveSatisfiedStates, conditionSatisfiedStates);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0Objective(ProbModel model, Until objectivePath, Until conditionPath)
				throws PrismException
		{
			// Always normalize
			return redistributeProb0(model, objectivePath, conditionPath);
		}
	}



	public static class MDP extends NewNormalFormTransformer.MDP implements NewFinallyUntilTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode computeInstantGoalStates(NondetModel model, Until objectivePath, JDDNode objectiveSatisfiedStates, JDDNode objectiveFalsifiedStates, Until conditionPath, JDDNode conditionSatisfiedStates, JDDNode conditionFalsifiedStates)
			throws PrismException
		{
			JDDNode instantGoalStates = JDD.And(objectiveSatisfiedStates, conditionSatisfiedStates);

			// exclude objective/condition falsified states
			JDDNode falsifiedStates    = JDD.Or(objectiveFalsifiedStates, conditionFalsifiedStates);
			JDDNode notFalsifiedStates = JDD.And(model.getReach().copy(), JDD.Not(falsifiedStates));

			// Do both, the objective and the condition, specify behavior in the limit?
			if (objectivePath.isNegated() && conditionPath.isNegated()) {
				// Compute ECs that never falsify the objective/condition
				// FIXME ALG: Use algorithm from explicit implementation
				// FIXME ALG: create constructor in ECComputerDefault
				ECComputerDefault ecComputer = new ECComputerDefault(this, model.getReach(), model.getTrans(), model.getTrans01(), model.getAllDDRowVars(), model.getAllDDColVars(), ((NondetModel) model).getAllDDNondetVars());
				instantGoalStates            = JDD.Or(instantGoalStates, ecComputer.findMaximalStableSet(notFalsifiedStates.copy()));
			}
			// enlarge target set
			JDDNode result = computeProb1E(model, notFalsifiedStates, instantGoalStates);
			JDD.Deref(notFalsifiedStates, instantGoalStates);
			return result;
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0Objective(NondetModel model, Until objectivePath, Until conditionPath)
				throws PrismException
		{
			// Do we have to reset once a state violates the objective?
			// Is objective path free of invariants?
			boolean isOptional = !objectivePath.isNegated() && JDD.IsContainedIn(model.getReach(), objectivePath.getRemain());
			if (!isOptional || settings.getBoolean(PrismSettings.CONDITIONAL_RESET_MDP_MINIMIZE)) {
				return redistributeProb0(model, objectivePath, conditionPath);
			}
			// Skip costly normalization
			return new ProbabilisticRedistribution();
		}
	}
}
