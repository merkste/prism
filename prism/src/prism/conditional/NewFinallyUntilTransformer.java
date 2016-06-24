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
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.Pair;
import prism.PrismException;
import prism.PrismLangException;
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
		Until objectivePath    = new Until(objectiveTmp, getModelChecker(), true);

		// 2) Condition: compute simple path property
		Expression conditionTemp = ExpressionInspector.normalizeExpression(expression.getCondition());
		Until conditionPath      = new Until(conditionTemp, getModelChecker(), true);

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
		JDDNode conditionFalsifiedStates = computeProb0(model, conditionPath);
		checkSatisfiability(conditionFalsifiedStates, statesOfInterest);

		// compute badStates
		JDDNode badStates = computeBadStates(model, conditionPath, conditionFalsifiedStates);

		// FIXME ALG: reuse precomputation?
		// compute redistribution for satisfied objective
		ProbabilisticRedistribution objectiveSatisfied = computeRedistribution(model, objectivePath, conditionPath);

		// compute redistribution for satisfied condition
		ProbabilisticRedistribution conditionSatisfied = computeRedistribution(model, conditionPath, objectivePath);

		// compute redistribution for falsified objective
		JDDNode objectiveFalsifiedStates               = computeProb0(model, objectivePath);
		JDDNode objectiveFalsifiedProbabilities        = computeUntilMinProbs(model, conditionPath);
		ProbabilisticRedistribution objectiveFalsified = new ProbabilisticRedistribution(objectiveFalsifiedStates, objectiveFalsifiedProbabilities);
//		ProbabilisticRedistribution objectiveFalsified = new ProbabilisticRedistribution();

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
		if (conditionPath.isNegated()) {
			// All paths violating the condition eventually reach the fail state.
			ExpressionLabel fail = new ExpressionLabel(transformation.getFailLabel());
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

	/**
	 * [ REFS: <i>result</i>, DEREFS: <i>objectiveSatisfiedStates, objectiveFalsifiedStates, conditionSatisfiedStates, conditionFalsifiedStates</i> ]
	 */
	JDDNode computeInstantGoalStates(M model, Until objectivePath, JDDNode objectiveSatisfiedStates, JDDNode objectiveFalsifiedStates, Until conditionPath, JDDNode conditionSatisfiedStates, JDDNode conditionFalsifiedStates)
			throws PrismException;

	default ProbabilisticRedistribution computeRedistribution(M model, Until pathStates, Until pathProbs)
			throws PrismException
	{
		JDDNode states        = computeProb1(model, pathStates);
		JDDNode probabilities = computeNormalFormProbs(model, pathProbs);
		return new ProbabilisticRedistribution(states, probabilities);
	}

	JDDNode computeNormalFormProbs(M model, Until until) throws PrismException;

	JDDNode computeUntilMinProbs(M model, Until until) throws PrismException;




	public static class DTMC extends NewNormalFormTransformer.DTMC implements NewFinallyUntilTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode computeNormalFormProbs(ProbModel model, Until until) throws PrismException
		{
			return computeUntilProbs(model, until);
		}

		public JDDNode computeUntilMinProbs(ProbModel model, Until until) throws PrismException
		{
			return computeUntilProbs(model, until);
		}

		@Override
		public JDDNode computeInstantGoalStates(ProbModel model, Until objectivePath, JDDNode objectiveSatisfiedStates, JDDNode objectiveFalsifiedStates,
				Until conditionPath, JDDNode conditionSatisfiedStates, JDDNode conditionFalsifiedStates) throws PrismException
		{
			JDD.Deref(objectiveFalsifiedStates, conditionFalsifiedStates);
			return JDD.And(objectiveSatisfiedStates, conditionSatisfiedStates);
		}
	}



	public static class MDP extends NewNormalFormTransformer.MDP implements NewFinallyUntilTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode computeNormalFormProbs(NondetModel model, Until until) throws PrismException
		{
			return computeUntilMaxProbs(model, until);
		}

		@Override
		public JDDNode computeInstantGoalStates(NondetModel model, Until objectivePath, JDDNode objectiveSatisfiedStates, JDDNode objectiveFalsifiedStates, Until conditionPath, JDDNode conditionSatisfiedStates, JDDNode conditionFalsifiedStates)
			throws PrismException
		{
			JDDNode instantGoalStates = JDD.And(objectiveSatisfiedStates, conditionSatisfiedStates);
			// exclude objective/condition falsified states
			JDDNode falsifiedStates = JDD.Or(objectiveFalsifiedStates, conditionFalsifiedStates);
			JDDNode remain          = JDD.And(model.getReach().copy(), JDD.Not(falsifiedStates));
			if (objectivePath.isNegated() && conditionPath.isNegated()) {
				// FIXME ALG: create constructor in ECComputerDefault
				ECComputerDefault ecComputer = new ECComputerDefault(this, model.getReach(), model.getTrans(), model.getTrans01(), model.getAllDDRowVars(), model.getAllDDColVars(), ((NondetModel) model).getAllDDNondetVars());
				instantGoalStates = JDD.Or(instantGoalStates, ecComputer.findMaximalStableSet(remain.copy()));
//
//				ECComputer ecComputer = ECComputer.createECComputer(getModelChecker(), model);
//				ecComputer.computeMECStates(remain);
//				List<JDDNode> mecs    = ecComputer.getMECStates();
//				instantGoalStates     = JDD.Or(instantGoalStates, mecs.stream().reduce(JDD.Constant(0), JDD::Or));
			}
			// enlarge target set
			JDDNode result = computeProb1E(model, remain, instantGoalStates);
			JDD.Deref(remain, instantGoalStates);
			return result;
		}
	}
}
