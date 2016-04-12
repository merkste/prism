package explicit.conditional.transformer;

import java.util.BitSet;

import acceptance.AcceptanceType;
import common.BitSetTools;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import prism.PrismException;
import prism.PrismLangException;
import explicit.DTMCModelChecker;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.ReachabilityComputer;
import explicit.LTLModelChecker.LTLProduct;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.GoalStopTransformer;
import explicit.conditional.transformer.GoalStopTransformer.GoalStopTransformation;
import explicit.conditional.transformer.LTLProductTransformer.LabeledDA;
import explicit.conditional.transformer.mdp.ConditionalReachabilitiyTransformation;

// FIXME ALG: add comment
public interface LtlUntilTransformer<M extends Model> extends ResetConditionalTransformer<M>
{
	static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.RABIN, AcceptanceType.GENERALIZED_RABIN, AcceptanceType.STREETT};

	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression)
	{
		Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		Expression until = ExpressionInspector.removeNegation(normalized);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
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
		LabeledDA objectiveDA    = getLtlTransformer().constructDA(model, objectiveExpr, ACCEPTANCE_TYPES);

		// 2) Condition: compute "condition remain and goal states"
		Expression conditionExpr       = ExpressionInspector.normalizeExpression(expression.getCondition());
		ExpressionTemporal untilExpr   = (ExpressionTemporal)ExpressionInspector.removeNegation(conditionExpr);
		Expression conditionRemainExpr = untilExpr.getOperand1();
		BitSet conditionRemain         = computeStates(model, conditionRemainExpr);
		Expression conditionGoalExpr   = untilExpr.getOperand2();
		BitSet conditionGoal           = computeStates(model, conditionGoalExpr);
		boolean conditionNegated       = conditionExpr instanceof ExpressionUnaryOp;

		// 3) Transformation
		return transform(model, objectiveDA, conditionRemain, conditionGoal, conditionNegated, statesOfInterest);
	}

	default ConditionalReachabilitiyTransformation<M, M> transform(M model, LabeledDA objectiveDA, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated, BitSet statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Objective
		LTLProduct<M> product = getLtlTransformer().constructProduct(model, objectiveDA, statesOfInterest);
		BitSet conditionRemainLifted       = conditionRemain == null ? null : product.liftFromModel(conditionRemain);
		BitSet conditionGoalLifted         = product.liftFromModel(conditionGoal);
		BitSet transformedStatesOfInterest = product.getTransformedStatesOfInterest();

		// 2) Bad States Transformation
		BitSet objectiveGoal;
		ConditionalReachabilitiyTransformation<M, M> transformation;
		switch (product.getAcceptance().getType()) {
		case REACH:
			M objectiveModel = product.getTransformedModel();
			objectiveGoal = getLtlTransformer().findAcceptingStates(product);
			FinallyUntilTransformer<M> finallyTransformer = getFinallyFinallyTransformer();
			getLog().println("\nDetected acceptance REACH for objective, delegating to " + finallyTransformer.getName());
			transformation = finallyTransformer.transform(objectiveModel, objectiveGoal, conditionRemainLifted, conditionGoalLifted, conditionNegated, transformedStatesOfInterest);
			break;
		case RABIN:
		case GENERALIZED_RABIN:
		case STREETT:
			transformation = transform(product, conditionRemainLifted, conditionGoalLifted, conditionNegated, transformedStatesOfInterest);
			break;
		default:
			throw new PrismException("unsupported acceptance type: " + product.getAcceptance().getType());
		}
		// 3) Compose Transformations
		ModelTransformationNested<M, M, M> nested = new ModelTransformationNested<>(product, transformation);
		return new ConditionalReachabilitiyTransformation<>(nested, transformation.getGoalStates());
	}

	default ConditionalReachabilitiyTransformation<M, M> transform(LTLProduct<M> product, BitSet conditionRemain, BitSet conditionGoal, boolean conditionNegated, BitSet statesOfInterest)
			throws UndefinedTransformationException, PrismException
	{
		M objectiveModel     = product.getTransformedModel();
		BitSet unsatisfiable = checkSatisfiability(objectiveModel, conditionRemain, conditionGoal, conditionNegated, statesOfInterest);

		// 1) Normal-Form Transformation
		GoalStopTransformer<M> normalFormTransformer = getNormalFormTransformer();
		// FIXME ALG: consider moving objective-goal-computation to normal-form transformer
		BitSet terminal = normalFormTransformer.getWeakGoalStates(objectiveModel, conditionRemain, conditionGoal, conditionNegated);
		// compute ECs in succ*(terminal)
		BitSet restrict = new ReachabilityComputer(objectiveModel).computeSuccStar(terminal);
		if (conditionNegated) {
			// and in  S \ unsatisfiable
			restrict.or(BitSetTools.complement(objectiveModel.getNumStates(), unsatisfiable));
		}
		BitSet objectiveGoal = getLtlTransformer().findAcceptingStates(product, restrict);
		// enlarge target set
		objectiveGoal = computeProb1A(objectiveModel, null, objectiveGoal);
		GoalStopTransformation<M> normalFormTransformation = normalFormTransformer.transformModel(objectiveModel, objectiveGoal, conditionRemain, conditionGoal, conditionNegated, statesOfInterest);
		M normalFormModel = normalFormTransformation.getTransformedModel();

		// 2) Deadlock hopeless states
		BitSet unsatisfiableLifted = normalFormTransformation.mapToTransformedModel(unsatisfiable);
		ModelTransformation<M, M> deadlockTransformation = deadlockStates(normalFormModel, unsatisfiableLifted, normalFormTransformation.getTransformedStatesOfInterest());

		// 3) Reset Transformation
		BitSet bad = computeBadStates(objectiveModel, objectiveGoal, conditionRemain, conditionGoal, conditionNegated);
		// lift bad states from model to normal-form model and to deadlock model
		BitSet badLifted = normalFormTransformation.mapToTransformedModel(bad);
		badLifted = deadlockTransformation.mapToTransformedModel(badLifted);
		// do reset
		M deadlockModel = deadlockTransformation.getTransformedModel();
		BitSet transformedStatesOfInterest = deadlockTransformation.getTransformedStatesOfInterest();
		ModelTransformation<M, ? extends M> resetTransformation = transformReset(deadlockModel, badLifted, transformedStatesOfInterest);

		// 4) Compose Transformations
		ModelTransformationNested<M, M, ? extends M> nested = new ModelTransformationNested<>(deadlockTransformation, resetTransformation);
		// FIXME ALG: consider moving goal-set computation to normal-form transformer
		BitSet goalLifted = normalFormTransformation.mapToTransformedModel(objectiveGoal);
		goalLifted.set(normalFormTransformation.getGoalState());
		goalLifted = nested.mapToTransformedModel(goalLifted);
		nested = new ModelTransformationNested<>(normalFormTransformation, nested);
		return new ConditionalReachabilitiyTransformation<>(nested, goalLifted);
	}

	default BitSet computeBadStates(M model, BitSet objectiveGoal, BitSet conditionRemain, BitSet conditionGoal, boolean negated)
	{
		if (negated) {
			// bad states == {s | Pmax=1[<> Condition]}
			final BitSet badStates = computeProb1E(model, conditionRemain, conditionGoal);
			badStates.andNot(objectiveGoal); // optionally, reduce number of choices
			return badStates;
		} else {
			// bad states == {s | Pmin=0[<> Condition]}
			return computeProb0E(model, conditionRemain, conditionGoal);
		}
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