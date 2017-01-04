package explicit.conditional.prototype.virtual;

import java.util.BitSet;

import explicit.DTMCModelChecker;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.ProbModelChecker;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.prototype.ConditionalReachabilitiyTransformation;
import explicit.conditional.prototype.virtual.GoalFailStopTransformer.GoalFailStopTransformation;
import explicit.conditional.transformer.UndefinedTransformationException;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import prism.PrismException;
import prism.PrismLangException;

@Deprecated
public interface FinallyUntilTransformer<M extends Model, MC extends ProbModelChecker> extends ResetConditionalTransformer<M, MC>
{
	@Override
	default boolean canHandleCondition(Model model, ExpressionConditional expression)
	{
		Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		Expression until = ExpressionInspector.removeNegation(normalized);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
	}

	@Override
	default boolean canHandleObjective(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		if (! ResetConditionalTransformer.super.canHandleObjective(model, expression)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		// can handle simple finally formulae only
		Expression normalized = ExpressionInspector.normalizeExpression(objective.getExpression());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	default ConditionalReachabilitiyTransformation<M, M> transformReachability(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: extract objective
		ExpressionProb objectiveExpr = (ExpressionProb) expression.getObjective();
		Expression objectiveGoalExpr = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objectiveExpr.getExpression())).getOperand2();
		BitSet objectiveGoal         = computeStates(model, objectiveGoalExpr);

		// 2) Condition: compute "condition remain and goal states"
		Expression conditionExpr       = ExpressionInspector.normalizeExpression(expression.getCondition());
		ExpressionTemporal untilExpr   = (ExpressionTemporal)ExpressionInspector.removeNegation(conditionExpr);
		Expression conditionRemainExpr = untilExpr.getOperand1();
		BitSet conditionRemain         = computeStates(model, conditionRemainExpr);
		Expression conditionGoalExpr   = untilExpr.getOperand2();
		BitSet conditionGoal           = computeStates(model, conditionGoalExpr);
		boolean conditionNegated       = conditionExpr instanceof ExpressionUnaryOp;

		return transform(model, objectiveGoal, conditionRemain, conditionGoal, conditionNegated, statesOfInterest);
	}

	default ConditionalReachabilitiyTransformation<M, M> transform(M model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated, BitSet statesOfInterest)
			throws UndefinedTransformationException, PrismException
	{
		BitSet unsatisfiable = checkSatisfiability(model, conditionRemain, conditionGoal, conditionNegated, statesOfInterest);

		// 1) Normal-Form Transformation
		GoalFailStopTransformer<M> normalFormTransformer = getNormalFormTransformer();
		GoalFailStopTransformation<M> normalFormTransformation = normalFormTransformer.transformModel(model, objectiveGoal, conditionRemain, conditionGoal, conditionNegated, statesOfInterest);
		M normalFormModel = normalFormTransformation.getTransformedModel();

		// 2) Deadlock hopeless states
		// do not deadlock goal states, go over fail-state
		unsatisfiable.andNot(objectiveGoal);
		BitSet unsatisfiableLifted = normalFormTransformation.mapToTransformedModel(unsatisfiable);
		// deadlock fail state as well
		unsatisfiableLifted.set(normalFormTransformation.getFailState());
		ModelTransformation<M, M> deadlockTransformation = deadlockStates(normalFormModel, unsatisfiableLifted, normalFormTransformation.getTransformedStatesOfInterest());

		// 3) Reset Transformation
		BitSet bad = computeBadStates(model, objectiveGoal, conditionRemain, conditionGoal, conditionNegated);
		// lift bad states from model to normal-form model
		BitSet badLifted = normalFormTransformation.mapToTransformedModel(bad);
		// reset from fail state as well
		badLifted.set(normalFormTransformation.getFailState());
		// lift bad states from normal-form model to deadlock model
		badLifted = deadlockTransformation.mapToTransformedModel(badLifted);
		// do reset
		M deadlockModel = deadlockTransformation.getTransformedModel();
		BitSet transformedStatesOfInterest = deadlockTransformation.getTransformedStatesOfInterest();
		ModelTransformation<M, ? extends M> resetTransformation = transformReset(deadlockModel, badLifted, transformedStatesOfInterest);

		// 4) Compose Transformations
		ModelTransformationNested<M, M, ? extends M> nested = new ModelTransformationNested<>(deadlockTransformation, resetTransformation);
		BitSet goalStatesLifted = nested.mapToTransformedModel(normalFormTransformation.getGoalStates());
		nested = new ModelTransformationNested<>(normalFormTransformation, nested);
		return new ConditionalReachabilitiyTransformation<>(nested, goalStatesLifted);
	}

	default BitSet computeBadStates(M model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, boolean negated)
			throws PrismException
	{
		BitSet badStates;
		if (negated) {
			// bad states == {s | Pmax=1[<> Condition]}
			badStates = computeProb1E(model, conditionRemain, conditionGoal);
		} else {
			// bad states == {s | Pmin=0[<> Condition]}
			badStates = computeProb0E(model, conditionRemain, conditionGoal);
		}
		// reduce number of choices, i.e.
		// - do not reset from goal states
		badStates.andNot(objectiveGoal);
		return badStates;
	}

	GoalFailStopTransformer<M> getNormalFormTransformer();



	public static class DTMC extends ResetConditionalTransformer.DTMC implements FinallyUntilTransformer<explicit.DTMC, DTMCModelChecker>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public GoalFailStopTransformer<explicit.DTMC> getNormalFormTransformer()
		{
			return new GoalFailStopTransformer.DTMC(modelChecker);
		}
	}



	public static class MDP extends ResetConditionalTransformer.MDP implements FinallyUntilTransformer<explicit.MDP, MDPModelChecker>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public GoalFailStopTransformer<explicit.MDP> getNormalFormTransformer()
		{
			return new GoalFailStopTransformer.MDP(modelChecker);
		}
	}
}
