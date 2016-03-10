package explicit.conditional.transformer;

import java.util.BitSet;

import explicit.DTMCModelChecker;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.GoalFailStopTransformer;
import explicit.conditional.transformer.GoalFailStopTransformer.GoalFailStopTransformation;
import explicit.conditional.transformer.mdp.ConditionalReachabilitiyTransformation;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;

// FIXME ALG: add comment
public interface FinallyFinallyTransformer<M extends Model> extends ResetConditionalTransformer<M>
{
	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression)
	{
		Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	default boolean canHandleObjective(M model, ExpressionConditional expression) throws PrismLangException
	{
		if (! ResetConditionalTransformer.super.canHandleObjective(model, expression)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression normalized = ExpressionInspector.normalizeExpression(objective.getExpression());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	default ConditionalReachabilitiyTransformation<M, M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest) throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: extract objective
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objective.getExpression())).getOperand2();
		BitSet objectiveGoalStates = computeStates(model, objectiveGoal);

		// 2) Condition: compute "condition goal states"
		Expression conditionGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(expression.getCondition())).getOperand2();
		BitSet conditionGoalStates = computeStates(model, conditionGoal);

		return transform(model, objectiveGoalStates, conditionGoalStates, statesOfInterest);
	}

	default ConditionalReachabilitiyTransformation<M, M> transform(M model, BitSet objectiveGoalStates, BitSet conditionGoalStates, BitSet statesOfInterest)
			throws UndefinedTransformationException, PrismException
	{
		checkSatisfiability(model, conditionGoalStates, statesOfInterest);

		// FIXME ALG: restrict to states of interest?

		// 1) Normal-Form Transformation
		GoalFailStopTransformer<M> normalFormTransformer = getNormalFormTransformer();
		GoalFailStopTransformation<M> normalFormTransformation = normalFormTransformer.transformModel(model, objectiveGoalStates, conditionGoalStates, statesOfInterest);

		// 2) Reset Transformation
		BitSet badStates = computeBadStates(model, objectiveGoalStates, conditionGoalStates);
		// lift bad states from model to normal-form model
		BitSet badStatesLifted = normalFormTransformation.mapToTransformedModel(badStates);
		// reset from fail state as well
		badStatesLifted.set(normalFormTransformation.getFailState());
		// do reset
		M normalForm = normalFormTransformation.getTransformedModel();
		BitSet transformedStatesOfInterest = normalFormTransformation.getTransformedStatesOfInterest();
		ModelTransformation<M, ? extends M> resetTransformation = transformReset(normalForm, badStatesLifted, transformedStatesOfInterest);

		// 3) Compose Transformations
		BitSet goalStatesLifted = resetTransformation.mapToTransformedModel(normalFormTransformation.getGoalStates());
		ModelTransformationNested<M, M, ? extends M> nested = new ModelTransformationNested<>(normalFormTransformation, resetTransformation);
		return new ConditionalReachabilitiyTransformation<>(nested, goalStatesLifted);
	}

	default BitSet computeBadStates(M model, BitSet objectiveGoalStates, BitSet conditionGoalStates)
	{
		// bad states == {s | Pmin=0[<> Condition]}
		BitSet badStates = computeProb0E(model, conditionGoalStates);
		// reduce number of transitions, i.e.
		// - do not reset from goal states
		badStates.andNot(objectiveGoalStates);
		return badStates;
	}

	GoalFailStopTransformer<M> getNormalFormTransformer();



	public static class DTMC extends ResetConditionalTransformer.DTMC implements FinallyFinallyTransformer<explicit.DTMC>
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



	public static class MDP extends ResetConditionalTransformer.MDP implements FinallyFinallyTransformer<explicit.MDP>
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