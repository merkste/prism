package explicit.conditional.transformer;

import java.util.BitSet;

import common.BitSetTools;
import common.iterable.MappingIterator;
import acceptance.AcceptanceStreett;
import acceptance.AcceptanceStreett.StreettPair;
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
import explicit.conditional.transformer.GoalFailTransformer;
import explicit.conditional.transformer.GoalFailTransformer.GoalFailTransformation;
import explicit.conditional.transformer.LTLProductTransformer;
import explicit.conditional.transformer.LTLProductTransformer.LabeledDA;
import explicit.conditional.transformer.mdp.ConditionalReachabilitiyTransformation;

// FIXME ALG: add comment
public interface FinallyLtlTransformer<M extends Model> extends ResetConditionalTransformer<M>
{
	static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.STREETT};

	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression)
			throws PrismLangException
	{
		return getLtlTransformer().canHandle(model, expression.getCondition());
	}

	@Override
	default boolean canHandleObjective(M model, ExpressionConditional expression)
			throws PrismLangException
	{
		if (! ResetConditionalTransformer.super.canHandleObjective(model, expression)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression normalized = ExpressionInspector.normalizeExpression(objective.getExpression());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	default ConditionalReachabilitiyTransformation<M, M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: compute "objective goal states"
		ExpressionProb objectiveExpr = (ExpressionProb) expression.getObjective();
		Expression objectiveGoalExpr = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objectiveExpr.getExpression())).getOperand2();
		BitSet objectiveGoal = computeStates(model, objectiveGoalExpr);

		// 2) Condition: build omega automaton
		Expression condition = expression.getCondition();
		LabeledDA conditionDA = getLtlTransformer().constructDA(model, condition, ACCEPTANCE_TYPES);

		// 3) Transformation
		return transform(model, objectiveGoal, conditionDA, statesOfInterest);
	}

	default ConditionalReachabilitiyTransformation<M, M> transform(M model, BitSet objectiveGoal, LabeledDA conditionDA, BitSet statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Condition
		LTLProduct<M> product = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
		M conditionModel = product.getProductModel();
		BitSet objectiveGoalLifted = product.liftFromModel(objectiveGoal);
		BitSet conditionGoal = getLtlTransformer().findAcceptingStates(product);
		BitSet transformedStatesOfInterest = product.getTransformedStatesOfInterest();

		// 2) Bad States Transformation
		ConditionalReachabilitiyTransformation<M,M> transformation;
		switch (product.getAcceptance().getType()) {
		case REACH:
			FinallyUntilTransformer<M> finallyTransformer = getFinallyFinallyTransformer();
			transformation = finallyTransformer.transform(conditionModel, objectiveGoalLifted, null, conditionGoal, transformedStatesOfInterest);
			break;
		case STREETT:
			transformation = transform(product, objectiveGoalLifted, conditionGoal, transformedStatesOfInterest);
			break;
		default:
			throw new PrismException("unsupported acceptance type: " + product.getAcceptance().getType());
		}

		// 3) Compose Transformations
		ModelTransformationNested<M, M, M> nested = new ModelTransformationNested<>(product, transformation);
		return new ConditionalReachabilitiyTransformation<>(nested, transformation.getGoalStates());
	}

	default ConditionalReachabilitiyTransformation<M, M> transform(LTLProduct<M> product, BitSet objectiveGoalStates, BitSet conditionGoalStates, BitSet statesOfInterest)
			throws PrismException
	{
		M conditionModel = product.getProductModel();

		BitSet unsatisfiable = checkSatisfiability(conditionModel, null, conditionGoalStates, statesOfInterest);

		// 1) Normal-Form Transformation
		GoalFailTransformer<M> normalFormTransformer = getNormalFormTransformer();
		GoalFailTransformation<M> normalFormTransformation = normalFormTransformer.transformModel(conditionModel, objectiveGoalStates, null, conditionGoalStates, statesOfInterest);
		M normalFormModel = normalFormTransformation.getTransformedModel();

		// 2) Deadlock hopeless states
		// do not deadlock goal states
		unsatisfiable.andNot(objectiveGoalStates);
		BitSet unsatisfiableLifted = normalFormTransformation.mapToTransformedModel(unsatisfiable);
		// deadlock fail state as well
		unsatisfiableLifted.set(normalFormTransformation.getFailState());
		ModelTransformation<M, M> deadlockTransformation = deadlockStates(normalFormModel, unsatisfiableLifted, normalFormTransformation.getTransformedStatesOfInterest());

		// 3) Reset Transformation
		BitSet bad = computeBadStates(product, objectiveGoalStates);
		// lift bad states from model to normal-form model
		BitSet badLifted = normalFormTransformation.mapToTransformedModel(bad);
		// reset from fail state as well
		badLifted.set(normalFormTransformation.getFailState());
		// lift bad states from normal-form model to deadlock model
		badLifted = deadlockTransformation.mapToTransformedModel(badLifted);
		// do reset
		badLifted.or(unsatisfiableLifted);
		M deadlockModel = deadlockTransformation.getTransformedModel();
		BitSet transformedStatesOfInterest = deadlockTransformation.getTransformedStatesOfInterest();
		ModelTransformation<M, ? extends M> resetTransformation = transformReset(deadlockModel, badLifted, transformedStatesOfInterest);

		// 3) Compose Transformations
		ModelTransformationNested<M, M, ? extends M> nested = new ModelTransformationNested<>(deadlockTransformation, resetTransformation);
		BitSet goalLifted = nested.mapToTransformedModel(normalFormTransformation.getGoalStates());
		nested = new ModelTransformationNested<>(normalFormTransformation, nested);
		return new ConditionalReachabilitiyTransformation<>(nested, goalLifted);
	}

	default BitSet computeBadStates(LTLProduct<M> product, BitSet objectiveGoal)
			throws PrismException
	{
		// bad states == {s | Pmin=0[<> Condition]}
		M productModel = product.getProductModel();
		AcceptanceStreett conditionAcceptance = (AcceptanceStreett) product.getAcceptance();
		BitSet bad = getLtlTransformer().findAcceptingStates(productModel, conditionAcceptance.complementToRabin());
		// reduce number of transitions, i.e.
		// - reset only from r-states of streett acceptance
		// - do not reset from goal states
		BitSet rStates = BitSetTools.union(new MappingIterator.From<>(conditionAcceptance, StreettPair::getR));
		bad.and(rStates);
		bad.andNot(objectiveGoal);
		return bad;
	}

	FinallyUntilTransformer<M> getFinallyFinallyTransformer();

	GoalFailTransformer<M> getNormalFormTransformer();



	public static class DTMC extends ResetConditionalTransformer.DTMC implements FinallyLtlTransformer<explicit.DTMC>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
			ltlTransformer = new LTLProductTransformer<>(modelChecker);
		}

		@Override
		public FinallyUntilTransformer<explicit.DTMC> getFinallyFinallyTransformer()
		{
			return new FinallyUntilTransformer.DTMC(modelChecker);
		}

		@Override
		public GoalFailTransformer<explicit.DTMC> getNormalFormTransformer()
		{
			return new GoalFailTransformer.DTMC(modelChecker);
		}
	}



	public static class MDP extends ResetConditionalTransformer.MDP implements FinallyLtlTransformer<explicit.MDP>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
			ltlTransformer = new LTLProductTransformer<>(modelChecker);
		}

		@Override
		public FinallyUntilTransformer<explicit.MDP> getFinallyFinallyTransformer()
		{
			return new FinallyUntilTransformer.MDP(modelChecker);
		}

		@Override
		public GoalFailTransformer<explicit.MDP> getNormalFormTransformer()
		{
			return new GoalFailTransformer.MDP(modelChecker);
		}
	}
}