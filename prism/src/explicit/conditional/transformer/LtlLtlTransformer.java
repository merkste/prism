package explicit.conditional.transformer;

import java.util.BitSet;

import common.BitSetTools;
import common.iterable.MappingIterator;
import acceptance.AcceptanceStreett;
import acceptance.AcceptanceType;
import acceptance.AcceptanceStreett.StreettPair;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.PrismException;
import prism.PrismLangException;
import explicit.DTMCModelChecker;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.LTLModelChecker.LTLProduct;
import explicit.conditional.transformer.LTLProductTransformer;
import explicit.conditional.transformer.LTLProductTransformer.LabeledDA;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.conditional.transformer.mdp.ConditionalReachabilitiyTransformation;

// FIXME ALG: add comment
public interface LtlLtlTransformer<M extends Model> extends ResetConditionalTransformer<M>
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
		return getLtlTransformer().canHandle(model, objective.getExpression());
	}

	@Override
	default ConditionalReachabilitiyTransformation<M, M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: build omega automaton
		Expression objective = ((ExpressionProb) expression.getObjective()).getExpression();
		LabeledDA objectiveDA = getLtlTransformer().constructDA(model, objective, ACCEPTANCE_TYPES);

		// 2) Condition: build omega automaton
		Expression condition = expression.getCondition();
		LabeledDA conditionDA = getLtlTransformer().constructDA(model, condition, ACCEPTANCE_TYPES);

		// 3) Transformation
		return transform(model, objectiveDA, conditionDA, statesOfInterest);
	}

	default ConditionalReachabilitiyTransformation<M, M> transform(M model, LabeledDA objectiveDA, LabeledDA conditionDA, BitSet statesOfInterest)
			throws PrismException, UndefinedTransformationException
	{
		ConditionalReachabilitiyTransformation<M, M> transformation;
		AcceptanceType objectiveAcceptanceType = objectiveDA.getAutomaton().getAcceptance().getType();
		AcceptanceType conditionAcceptanceType = conditionDA.getAutomaton().getAcceptance().getType();
		ModelTransformation<M, M> product;
		if (objectiveAcceptanceType == AcceptanceType.REACH) {
			// 1) LTL Product Transformation for Objective
			LTLProduct<M> objectiveProduct = getLtlTransformer().constructProduct(model, objectiveDA, statesOfInterest);
			product = objectiveProduct;
			M objectiveModel = objectiveProduct.getTransformedModel();
			BitSet objectiveGoal = getLtlTransformer().findAcceptingStates(objectiveProduct);
			BitSet transformedStatesOfInterest = objectiveProduct.getTransformedStatesOfInterest();

			// 2) Bad States Transformation
			FinallyLtlTransformer<M> ltlConditionTransformer = getLtlConditionTransformer();
			transformation = ltlConditionTransformer.transform(objectiveModel, objectiveGoal, conditionDA.liftToProduct(objectiveProduct), transformedStatesOfInterest);
		} else if (conditionAcceptanceType  == AcceptanceType.REACH) {
			// 1) LTL Product Transformation for Condition
			LTLProduct<M> conditionProduct = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
			product = conditionProduct;
			M conditionModel = conditionProduct.getTransformedModel();
			BitSet conditionGoal = getLtlTransformer().findAcceptingStates(conditionProduct);
			BitSet transformedStatesOfInterest = conditionProduct.getTransformedStatesOfInterest();

			// 2) Bad States Transformation
			LtlUntilTransformer<M> ltlObjectiveTransformer = getLtlObjectiveTransformer();
			transformation = ltlObjectiveTransformer.transform(conditionModel, objectiveDA.liftToProduct(conditionProduct), null, conditionGoal, false, transformedStatesOfInterest);
		} else {
			checkAcceptanceType(objectiveAcceptanceType);
			checkAcceptanceType(conditionAcceptanceType);

			// 1) LTL Product Transformation for Condition
			LTLProduct<M> conditionProduct = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
			M conditionModel = conditionProduct.getProductModel();
			BitSet conditionGoal = getLtlTransformer().findAcceptingStates(conditionProduct);
			BitSet conditionStatesOfInterest = conditionProduct.getTransformedStatesOfInterest();

			BitSet unsatisfiable = checkSatisfiability(conditionModel, null, conditionGoal, false, conditionStatesOfInterest);

			// 2) LTL Product Transformation for Objective
			LTLProduct<M> objectiveAndConditionProduct = getLtlTransformer().constructProduct(conditionModel, objectiveDA.liftToProduct(conditionProduct), conditionStatesOfInterest);
			M objectiveAndConditionModel = objectiveAndConditionProduct.getProductModel();
			product = new ModelTransformationNested<>(conditionProduct, objectiveAndConditionProduct);

			// 3) Lift Condition Acceptance
			AcceptanceStreett conditionAcceptanceLifted = new AcceptanceStreett();
			for (StreettPair streettPair : (AcceptanceStreett) conditionProduct.getAcceptance()) {
				BitSet R = objectiveAndConditionProduct.liftFromModel(streettPair.getR());
				BitSet G = objectiveAndConditionProduct.liftFromModel(streettPair.getG());
				conditionAcceptanceLifted.add(new StreettPair(R, G));
			}

			// 4) Conjunction of Objective and Condition Acceptance
			AcceptanceStreett objectiveAndConditionAcceptance = new AcceptanceStreett();
			objectiveAndConditionAcceptance.addAll((AcceptanceStreett) objectiveAndConditionProduct.getAcceptance());
			objectiveAndConditionAcceptance.addAll(conditionAcceptanceLifted);

			// 5) Objective & Condition Goal States
			BitSet objectiveAndConditionGoal = getLtlTransformer().findAcceptingStates(objectiveAndConditionModel, objectiveAndConditionAcceptance);

			// 6) Deadlock hopeless states
			BitSet unsatisfiableLifted = objectiveAndConditionProduct.liftFromModel(unsatisfiable);
			ModelTransformation<M, M> deadlockTransformation = deadlockStates(objectiveAndConditionModel, unsatisfiableLifted, objectiveAndConditionProduct.getTransformedStatesOfInterest());

			// 7) Reset Transformation
			BitSet bad = computeBadStates(objectiveAndConditionModel, objectiveAndConditionGoal, conditionAcceptanceLifted);
			// lift bad states from normal-form model to deadlock model
			BitSet badLifted = deadlockTransformation.mapToTransformedModel(bad);
			// do reset
			badLifted.or(unsatisfiableLifted);
			M deadlockModel = deadlockTransformation.getTransformedModel();
			BitSet transformedStatesOfInterest = deadlockTransformation.getTransformedStatesOfInterest();
			ModelTransformation<M, ? extends M> resetTransformation = transformReset(deadlockModel, badLifted, transformedStatesOfInterest);

			// 8) Compose Transformations
			ModelTransformationNested<M, M, ? extends M> nested = new ModelTransformationNested<>(deadlockTransformation, resetTransformation);
			BitSet objectiveAndConditionGoalStatesLifted = nested.mapToTransformedModel(objectiveAndConditionGoal);
			return new ConditionalReachabilitiyTransformation<>(nested, objectiveAndConditionGoalStatesLifted);
		}

		// 3) Compose Transformations
		ModelTransformationNested<M, M, M> nested = new ModelTransformationNested<>(product, transformation);
		return new ConditionalReachabilitiyTransformation<>(nested, transformation.getGoalStates());
	}

	default BitSet computeBadStates(M productModel, BitSet objectiveAndConditionGoal, AcceptanceStreett conditionAcceptance)
			throws PrismException
	{
		// bad states == {s | Pmin=0[<> Condition]}
		BitSet bad = getLtlTransformer().findAcceptingStates(productModel, conditionAcceptance.complementToRabin());
		// reduce number of choices, i.e.
		// - reset only from r-states of streett acceptance
		BitSet rStates = BitSetTools.union(new MappingIterator.From<>(conditionAcceptance, StreettPair::getR));
		bad.and(rStates);
		// - do not reset from goal states
		bad.andNot(objectiveAndConditionGoal);
		return bad;
	}

	default void checkAcceptanceType(AcceptanceType objectiveAcceptanceType)
			throws PrismException
	{
		if (objectiveAcceptanceType != AcceptanceType.STREETT) {
			throw new PrismException("unsupported acceptance type: " + objectiveAcceptanceType);
		}
	}

	LtlUntilTransformer<M> getLtlObjectiveTransformer();

	FinallyLtlTransformer<M> getLtlConditionTransformer();



	public static class DTMC extends ResetConditionalTransformer.DTMC implements LtlLtlTransformer<explicit.DTMC>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public LtlUntilTransformer<explicit.DTMC> getLtlObjectiveTransformer()
		{
			return new LtlUntilTransformer.DTMC(modelChecker);
		}

		@Override
		public FinallyLtlTransformer<explicit.DTMC> getLtlConditionTransformer()
		{
			return new FinallyLtlTransformer.DTMC(modelChecker);
		}
	}



	public static class MDP extends ResetConditionalTransformer.MDP implements LtlLtlTransformer<explicit.MDP>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
			ltlTransformer = new LTLProductTransformer<>(modelChecker);
		}

		@Override
		public LtlUntilTransformer<explicit.MDP> getLtlObjectiveTransformer()
		{
			return new LtlUntilTransformer.MDP(modelChecker);
		}

		@Override
		public FinallyLtlTransformer<explicit.MDP> getLtlConditionTransformer()
		{
			return new FinallyLtlTransformer.MDP(modelChecker);
		}
	}
}