package explicit.conditional;

import java.util.BitSet;

import common.BitSetTools;
import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.MDPModelChecker;
import explicit.MinMax;
import explicit.Model;
import explicit.ProbModelChecker;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.NewGoalFailStopTransformer.GoalFailStopTransformation;
import explicit.conditional.NewGoalFailStopTransformer.ProbabilisticRedistribution;
import explicit.conditional.SimplePathProperty.Globally;
import explicit.conditional.SimplePathProperty.Reach;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.Pair;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;



// FIXME ALG: add comment
public interface NewFinallyUntilTransformer<M extends Model, MC extends ProbModelChecker> extends NewNormalFormTransformer<M, MC>
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
	default NormalFormTransformation<M> transformNormalForm(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: compute simple path property
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTmp  = objective.getExpression();
		Reach<M> objectivePath   = (Reach<M>) computeSimplePathProperty(model, objectiveTmp);

		// 2) Condition: compute simple path property
		Expression conditionTmp = expression.getCondition();
		Reach<M> conditionPath  = (Reach<M>) computeSimplePathProperty(model, conditionTmp);

		// 3) Transform model
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = transformNormalForm(objectivePath, conditionPath, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression  = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective        = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded);
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(Reach<M> objectivePath, Reach<M> conditionPath, BitSet statesOfInterest)
			throws PrismException
	{
		objectivePath.requireSameModel(conditionPath);

		// FIXME ALG: consider whether this is actually an error in a normal-form transformation
		BitSet conditionFalsifiedStates = checkSatisfiability(conditionPath, statesOfInterest);

		// compute badStates
		BitSet badStates = computeBadStates(conditionPath, conditionFalsifiedStates);

		// FIXME ALG: reuse precomputation?
		// compute redistribution for satisfied objective
		ProbabilisticRedistribution objectiveSatisfied = redistributeProb1(objectivePath, conditionPath);

		// compute redistribution for satisfied condition
		ProbabilisticRedistribution conditionSatisfied = redistributeProb1(conditionPath, objectivePath);

		// compute redistribution for falsified objective
		ProbabilisticRedistribution objectiveFalsified = redistributeProb0Objective(objectivePath, conditionPath);

		// compute states where objective and condition can be satisfied
		BitSet instantGoalStates = computeInstantGoalStates(objectivePath, objectiveSatisfied.getStates(), objectiveFalsified.getStates(), conditionPath, conditionSatisfied.getStates(), conditionFalsifiedStates);

		// transform goal-fail-stop
		M model                                      = objectivePath.getModel();
		GoalFailStopTransformation<M> transformation = transformGoalFailStop(model, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, badStates, statesOfInterest);

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

		return new Pair<>(transformation, transformedExpression);
	}

	ProbabilisticRedistribution redistributeProb0Objective(Reach<M> objectivePath, Reach<M> conditionPath)
			throws PrismException;

	BitSet computeInstantGoalStates(Reach<M> objectivePath, BitSet objectiveSatisfiedStates, BitSet objectiveFalsifiedStates, Reach<M> conditionPath, BitSet conditionSatisfiedStates, BitSet conditionFalsifiedStates)
			throws PrismException;



	public static class CTMC extends NewNormalFormTransformer.CTMC implements NewFinallyUntilTransformer<explicit.CTMC, CTMCModelChecker>
	{
		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public BitSet computeInstantGoalStates(Reach<explicit.CTMC> objectivePath, BitSet objectiveSatisfiedStates, BitSet objectiveFalsifiedStates, Reach<explicit.CTMC> conditionPath, BitSet conditionSatisfiedStates, BitSet conditionFalsifiedStates)
				throws PrismException
		{
			objectivePath.requireSameModel(conditionPath);

			return BitSetTools.intersect(objectiveSatisfiedStates, conditionSatisfiedStates);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0Objective(Reach<explicit.CTMC> objectivePath, Reach<explicit.CTMC> conditionPath)
				throws PrismException
		{
			// Always normalize
			return redistributeProb0(objectivePath, conditionPath);
		}
	}



	public static class DTMC extends NewNormalFormTransformer.DTMC implements NewFinallyUntilTransformer<explicit.DTMC, DTMCModelChecker>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public BitSet computeInstantGoalStates(Reach<explicit.DTMC> objectivePath, BitSet objectiveSatisfiedStates, BitSet objectiveFalsifiedStates, Reach<explicit.DTMC> conditionPath, BitSet conditionSatisfiedStates, BitSet conditionFalsifiedStates)
				throws PrismException
		{
			objectivePath.requireSameModel(conditionPath);

			return BitSetTools.intersect(objectiveSatisfiedStates, conditionSatisfiedStates);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0Objective(Reach<explicit.DTMC> objectivePath, Reach<explicit.DTMC> conditionPath)
				throws PrismException
		{
			// Always normalize
			return redistributeProb0(objectivePath, conditionPath);
		}
	}



	public static class MDP extends NewNormalFormTransformer.MDP implements NewFinallyUntilTransformer<explicit.MDP, MDPModelChecker>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public BitSet computeInstantGoalStates(Reach<explicit.MDP> objectivePath, BitSet objectiveSatisfiedStates, BitSet objectiveFalsifiedStates, Reach<explicit.MDP> conditionPath, BitSet conditionSatisfiedStates, BitSet conditionFalsifiedStates)
			throws PrismException
		{
			objectivePath.requireSameModel(conditionPath);
			explicit.MDP model = objectivePath.getModel();

			BitSet instantGoalStates = BitSetTools.intersect(objectiveSatisfiedStates, conditionSatisfiedStates);

			// exclude objective/condition falsified states
			BitSet falsifiedStates    = BitSetTools.union(objectiveFalsifiedStates, conditionFalsifiedStates);
			BitSet notFalsifiedStates = BitSetTools.complement(model.getNumStates(), falsifiedStates);

			// Do both, the objective and the condition, specify behavior in the limit?
			if (!objectivePath.isCoSafe() && !conditionPath.isCoSafe()) {
				// Compute ECs that never falsify the objective/condition
				Globally<explicit.MDP> neverFalsified = new Globally<>(model, notFalsifiedStates);
				BitSet neverFalsifiedStates           = computeProb1E(neverFalsified);
				instantGoalStates.or(neverFalsifiedStates);
			}
			// enlarge target set
			return computeProb1E(model, false, notFalsifiedStates, instantGoalStates);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0Objective(Reach<explicit.MDP> objectivePath, Reach<explicit.MDP> conditionPath)
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
