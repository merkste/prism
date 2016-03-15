package explicit.conditional.transformer;

import java.util.BitSet;

import acceptance.AcceptanceType;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;
import explicit.DTMCModelChecker;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.LTLModelChecker.LTLProduct;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.GoalStopTransformer;
import explicit.conditional.transformer.GoalStopTransformer.GoalStopTransformation;
import explicit.conditional.transformer.LTLProductTransformer.LabeledDA;
import explicit.conditional.transformer.mdp.ConditionalReachabilitiyTransformation;

// FIXME ALG: add comment
public interface LtlUntilTransformer<M extends Model> extends ResetConditionalTransformer<M>
{
	static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.STREETT};

	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression)
	{
		Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		return ExpressionInspector.isSimpleUntilFormula(normalized);
	}

	@Override
	default
	boolean canHandleObjective(M model, ExpressionConditional expression)
			throws PrismLangException
	{
		if (! ResetConditionalTransformer.super.canHandleObjective(model, expression)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		return getLtlTransformer().canHandle(model, objective.getExpression());
	}

	@Override
	default ConditionalReachabilitiyTransformation<M, M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: build omega automaton
		Expression objectiveExpr = ((ExpressionProb) expression.getObjective()).getExpression();
		LabeledDA objectiveDA = getLtlTransformer().constructDA(model, objectiveExpr, ACCEPTANCE_TYPES);

		// 2) Condition: compute "condition goal states"
		ExpressionTemporal conditionExpr = (ExpressionTemporal) ExpressionInspector.normalizeExpression(expression.getCondition());
		Expression conditionRemainExpr = conditionExpr.getOperand1();
		BitSet conditionRemain = computeStates(model, conditionRemainExpr);
		Expression conditionGoalExpr = conditionExpr.getOperand2();
		BitSet conditionGoal = computeStates(model, conditionGoalExpr);

		// 3) Transformation
		return transform(model, objectiveDA, conditionRemain, conditionGoal, statesOfInterest);
	}

	default ConditionalReachabilitiyTransformation<M, M> transform(M model, LabeledDA objectiveDA, BitSet conditionRemain, BitSet conditionGoal, BitSet statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Objective
		LTLProduct<M> product = getLtlTransformer().constructProduct(model, objectiveDA, statesOfInterest);
		M objectiveModel = product.getProductModel();
		BitSet objectiveGoal = getLtlTransformer().findAcceptingStates(product);
		BitSet conditionRemainLifted = conditionRemain == null ? null : product.liftFromModel(conditionRemain);
		BitSet conditionGoalLifted = product.liftFromModel(conditionGoal);
		BitSet transformedStatesOfInterest = product.getTransformedStatesOfInterest();

		// 2) Bad States Transformation
		ConditionalReachabilitiyTransformation<M, M> transformation;
		switch (product.getAcceptance().getType()) {
		case REACH:
			FinallyUntilTransformer<M> finallyTransformer = getFinallyFinallyTransformer();
			transformation = finallyTransformer.transform(objectiveModel, objectiveGoal, conditionRemainLifted, conditionGoalLifted, transformedStatesOfInterest);
			break;
		case STREETT:
			transformation = transform(objectiveModel, objectiveGoal, conditionRemainLifted, conditionGoalLifted, transformedStatesOfInterest);
			break;
		default:
			throw new PrismException("unsupported acceptance type: " + product.getAcceptance().getType());
		}

		// 3) Compose Transformations
		ModelTransformationNested<M, M, M> nested = new ModelTransformationNested<>(product, transformation);
		return new ConditionalReachabilitiyTransformation<>(nested, transformation.getGoalStates());
	}

	default ConditionalReachabilitiyTransformation<M, M> transform(M model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, BitSet statesOfInterest)
			throws PrismException
	{
		BitSet unsatisfiable = checkSatisfiability(model, conditionRemain, conditionGoal, statesOfInterest);

		// 1) Normal-Form Transformation
		GoalStopTransformer<M> normalFormTransformer = getNormalFormTransformer();
		GoalStopTransformation<M> normalFormTransformation = normalFormTransformer.transformModel(model, objectiveGoal, conditionRemain, conditionGoal, statesOfInterest);
		M normalFormModel = normalFormTransformation.getTransformedModel();

		// 2) Deadlock hopeless states
		// do not deadlock goal states
		unsatisfiable.andNot(objectiveGoal);
		BitSet unsatisfiableLifted = normalFormTransformation.mapToTransformedModel(unsatisfiable);
		ModelTransformation<M, M> deadlockTransformation = deadlockStates(normalFormModel, unsatisfiableLifted, normalFormTransformation.getTransformedStatesOfInterest());

		// 3) Reset Transformation
		BitSet bad = computeBadStates(model, objectiveGoal, conditionRemain, conditionGoal);
		// lift bad states from model to normal-form model and to deadlock model
		BitSet badLifted = normalFormTransformation.mapToTransformedModel(bad);
		badLifted = deadlockTransformation.mapToTransformedModel(badLifted);
		// do reset
		M deadlockModel = deadlockTransformation.getTransformedModel();
		BitSet transformedStatesOfInterest = deadlockTransformation.getTransformedStatesOfInterest();
		ModelTransformation<M, ? extends M> resetTransformation = transformReset(deadlockModel, badLifted, transformedStatesOfInterest);

		// 4) Compose Transformations
		ModelTransformationNested<M, M, ? extends M> nested = new ModelTransformationNested<>(deadlockTransformation, resetTransformation);
		BitSet goalLifted = nested.mapToTransformedModel(normalFormTransformation.getGoalStates());
		nested = new ModelTransformationNested<>(normalFormTransformation, nested);
		return new ConditionalReachabilitiyTransformation<>(nested, goalLifted);
	}

	default BitSet computeBadStates(M model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal)
	{
		// bad states == {s | Pmin=0[<> Condition]}
		return computeProb0E(model, conditionRemain, conditionGoal);
	}

	FinallyUntilTransformer<M> getFinallyFinallyTransformer();

	GoalStopTransformer<M> getNormalFormTransformer();



	public static class DTMC extends ResetConditionalTransformer.DTMC implements LtlUntilTransformer<explicit.DTMC>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public FinallyUntilTransformer<explicit.DTMC> getFinallyFinallyTransformer()
		{
			return new FinallyUntilTransformer.DTMC(modelChecker);
		}

		@Override
		public GoalStopTransformer<explicit.DTMC> getNormalFormTransformer()
		{
			return new GoalStopTransformer.DTMC(modelChecker);
		}
	}



	public static class MDP extends ResetConditionalTransformer.MDP implements LtlUntilTransformer<explicit.MDP>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public FinallyUntilTransformer<explicit.MDP> getFinallyFinallyTransformer()
		{
			return new FinallyUntilTransformer.MDP(modelChecker);
		}

		@Override
		public GoalStopTransformer<explicit.MDP> getNormalFormTransformer()
		{
			return new GoalStopTransformer.MDP(modelChecker);
		}
	}
}