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
public interface LtlConditionTransformer<M extends Model> extends ResetConditionalTransformer<M>
{
	static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.STREETT};

	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression) throws PrismLangException
	{
		return getLtlTransformer().canHandle(model, expression.getCondition());
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
	default ConditionalReachabilitiyTransformation<M, M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: compute "objective goal states"
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objective.getExpression())).getOperand2();
		BitSet objectiveGoalStates = computeStates(model, objectiveGoal);

		// 2) Condition: build omega automaton
		Expression condition = expression.getCondition();
		LabeledDA conditionDA = getLtlTransformer().constructDA(model, condition, ACCEPTANCE_TYPES);

		// 3) Transformation
		return transform(model, objectiveGoalStates, conditionDA, statesOfInterest);
	}

	default ConditionalReachabilitiyTransformation<M, M> transform(M model, BitSet objectiveGoalStates, LabeledDA conditionDA, BitSet statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Condition
		LTLProduct<M> product = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
		M conditionModel = product.getProductModel();
		BitSet objectiveGoalStatesLifted = product.liftFromModel(objectiveGoalStates);
		BitSet conditionGoalStates = getLtlTransformer().findAcceptingStates(product);
		BitSet transformedStatesOfInterest = product.getTransformedStatesOfInterest();

		// 2) Bad States Transformation
		ConditionalReachabilitiyTransformation<M,M> transformation;
		switch (product.getAcceptance().getType()) {
		case REACH:
			FinallyFinallyTransformer<M> finallyTransformer = getFinallyFinallyTransformer();
			transformation = finallyTransformer.transform(conditionModel, objectiveGoalStatesLifted, conditionGoalStates, transformedStatesOfInterest);
			break;
		case STREETT:
			transformation = transform(product, objectiveGoalStatesLifted, conditionGoalStates, transformedStatesOfInterest);
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

		checkSatisfiability(conditionModel, conditionGoalStates, statesOfInterest);

		// FIXME ALG: restrict to states of interest?

		// 1) Normal-Form Transformation
		GoalFailTransformer<M> normalFormTransformer = getNormalFormTransformer();
		// FIXME ALG: simplify goal-fail signature?
		GoalFailTransformation<M> normalFormTransformation = normalFormTransformer.transformModel(conditionModel, objectiveGoalStates, conditionGoalStates, statesOfInterest);

		// 3) Reset Transformation
		BitSet badStates = computeBadStates(product, objectiveGoalStates);
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

	default BitSet computeBadStates(LTLProduct<M> product, BitSet objectiveGoalStates)
			throws PrismException
	{
		// bad states == {s | Pmin=0[<> Condition]}
		M productModel = product.getProductModel();
		AcceptanceStreett conditionAcceptance = (AcceptanceStreett) product.getAcceptance();
		BitSet badStates = getLtlTransformer().findAcceptingStates(productModel, conditionAcceptance.complementToRabin());
		// reduce number of transitions, i.e.
		// - reset only from r-states of streett acceptance
		// - do not reset from goal states
		BitSet rStates = BitSetTools.union(new MappingIterator.From<>(conditionAcceptance, StreettPair::getR));
		badStates.and(rStates);
		badStates.andNot(objectiveGoalStates);
		return badStates;
	}

	FinallyFinallyTransformer<M> getFinallyFinallyTransformer();

	GoalFailTransformer<M> getNormalFormTransformer();



	public static class DTMC extends ResetConditionalTransformer.DTMC implements LtlConditionTransformer<explicit.DTMC>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
			ltlTransformer = new LTLProductTransformer<>(modelChecker);
		}

		@Override
		public FinallyFinallyTransformer<explicit.DTMC> getFinallyFinallyTransformer()
		{
			return new FinallyFinallyTransformer.DTMC(modelChecker);
		}

		@Override
		public GoalFailTransformer<explicit.DTMC> getNormalFormTransformer()
		{
			return new GoalFailTransformer.DTMC(modelChecker);
		}
	}



	public static class MDP extends ResetConditionalTransformer.MDP implements LtlConditionTransformer<explicit.MDP>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
			ltlTransformer = new LTLProductTransformer<>(modelChecker);
		}

		@Override
		public FinallyFinallyTransformer<explicit.MDP> getFinallyFinallyTransformer()
		{
			return new FinallyFinallyTransformer.MDP(modelChecker);
		}

		@Override
		public GoalFailTransformer<explicit.MDP> getNormalFormTransformer()
		{
			return new GoalFailTransformer.MDP(modelChecker);
		}
	}
}