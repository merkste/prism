package explicit.conditional.reset;

import java.util.BitSet;

import acceptance.AcceptanceStreett;
import acceptance.AcceptanceStreett.StreettPair;
import acceptance.AcceptanceType;
import common.BitSetTools;
import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.MDPModelChecker;
import explicit.MinMax;
import explicit.Model;
import explicit.ProbModelChecker;
import explicit.conditional.checker.SimplePathEvent.Finally;
import explicit.conditional.reset.GoalFailStopTransformer.GoalFailStopTransformation;
import explicit.conditional.reset.GoalFailStopTransformer.ProbabilisticRedistribution;
import explicit.conditional.transformer.LtlProductTransformer.LabeledDA;
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
public interface LtlLtlTransformer<M extends Model, C extends ProbModelChecker> extends NormalFormTransformer<M, C>
{
	// FIXME ALG: Generalize acceptance types: DMTC=all, MDP=REACH, STREETT
	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.STREETT};
//	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.STREETT};



	@Override
	default boolean canHandleObjective(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		if (! NormalFormTransformer.super.canHandleObjective(model, expression)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		return getLtlTransformer().canHandle(model, objective.getExpression());
	}

	@Override
	default boolean canHandleCondition(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		return getLtlTransformer().canHandle(model, expression.getCondition());
	}

	@Override
	default NormalFormTransformation<M> transformNormalForm(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: build omega automaton
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTemp = objective.getExpression();
		LabeledDA objectiveDA    = getLtlTransformer().constructDA(model, objectiveTemp, ACCEPTANCE_TYPES);

		// 2) Condition: build omega automaton
		Expression conditionTmp = expression.getCondition();
		LabeledDA conditionDA   = getLtlTransformer().constructDA(model, conditionTmp, ACCEPTANCE_TYPES);

		// 3) Transform model
		Pair<GoalFailStopTransformation<M>,ExpressionConditional> result = transformNormalForm(model, objectiveDA, conditionDA, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression  = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective        = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded);
	}

	Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(M model, LabeledDA objectiveDA, LabeledDA conditionDA, BitSet statesOfInterest)
			throws PrismException;

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalFormReachObjective(M model, LabeledDA objectiveDA, LabeledDA conditionDA, BitSet statesOfInterest)
			throws PrismException
	{
		LTLProduct<M> product           = getLtlTransformer().constructProduct(model, objectiveDA, statesOfInterest);
		M productModel                  = product.getTransformedModel();
		BitSet acceptStates             = getLtlTransformer().findAcceptingStates(product);
		Finally<M> objectivePathProduct = new Finally<>(productModel, acceptStates);
		BitSet statesOfInterestProduct  = product.getTransformedStatesOfInterest();

		FinallyLtlTransformer<M, C> finallyLtlTransformer             = getFinallyLtlTransformer();
		getLog().println("\nDelegating to " + finallyLtlTransformer.getName());
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = finallyLtlTransformer.transformNormalForm(objectivePathProduct, conditionDA.liftToProduct(product), statesOfInterestProduct);
		finallyLtlTransformer.clear();

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.chain(product);
		ExpressionConditional transformedExpression  = result.second;

		return new Pair<>(transformation, transformedExpression);
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalFormReachCondition(M model, LabeledDA objectiveDA, LabeledDA conditionDA, BitSet statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Condition
		LTLProduct<M> product           = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
		M productModel                  = product.getTransformedModel();
		BitSet acceptStates             = getLtlTransformer().findAcceptingStates(product);
		Finally<M> conditionPathProduct = new Finally<>(productModel, acceptStates);
		BitSet statesOfInterestProduct  = product.getTransformedStatesOfInterest();

		// 2) Normal-Form Transformation
		LtlUntilTransformer<M,C> ltlUntilTransformer                  = getLtlUntilTransformer();
		getLog().println("\nDelegating to " + ltlUntilTransformer.getName());
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = ltlUntilTransformer.transformNormalForm(objectiveDA.liftToProduct(product), conditionPathProduct, statesOfInterestProduct);
		ltlUntilTransformer.clear();

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.chain(product);
		ExpressionConditional transformedExpression  = result.second;

		return new Pair<>(transformation, transformedExpression);
	}

	LtlUntilTransformer<M, C> getLtlUntilTransformer();

	FinallyLtlTransformer<M, C> getFinallyLtlTransformer();



	public static class CTMC extends NormalFormTransformer.CTMC implements LtlLtlTransformer<explicit.CTMC, CTMCModelChecker>
	{
		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<explicit.CTMC>, ExpressionConditional> transformNormalForm(explicit.CTMC model, LabeledDA objectiveDA, LabeledDA conditionDA, BitSet statesOfInterest)
				throws PrismException
		{
			// Since each BSCC is either accepting or not, it suffices to reach the set of accepting states
			return transformNormalFormReachCondition(model, objectiveDA, conditionDA, statesOfInterest);
		}

		@Override
		public LtlUntilTransformer.CTMC getLtlUntilTransformer()
		{
			return new LtlUntilTransformer.CTMC(getModelChecker());
		}

		@Override
		public FinallyLtlTransformer.CTMC getFinallyLtlTransformer()
		{
			return new FinallyLtlTransformer.CTMC(getModelChecker());
		}
	}



	public static class DTMC extends NormalFormTransformer.DTMC implements LtlLtlTransformer<explicit.DTMC, DTMCModelChecker>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<explicit.DTMC>, ExpressionConditional> transformNormalForm(explicit.DTMC model, LabeledDA objectiveDA, LabeledDA conditionDA, BitSet statesOfInterest)
				throws PrismException
		{
			// Since each BSCC is either accepting or not, it suffices to reach the set of accepting states
			return transformNormalFormReachCondition(model, objectiveDA, conditionDA, statesOfInterest);
		}

		@Override
		public LtlUntilTransformer.DTMC getLtlUntilTransformer()
		{
			return new LtlUntilTransformer.DTMC(getModelChecker());
		}

		@Override
		public FinallyLtlTransformer.DTMC getFinallyLtlTransformer()
		{
			return new FinallyLtlTransformer.DTMC(getModelChecker());
		}
	}



	public static class MDP extends NormalFormTransformer.MDP implements LtlLtlTransformer<explicit.MDP, MDPModelChecker>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<explicit.MDP>, ExpressionConditional> transformNormalForm(explicit.MDP model, LabeledDA objectiveDA, LabeledDA conditionDA, BitSet statesOfInterest)
				throws PrismException
		{
			if (conditionDA.getAutomaton().getAcceptance().getType() == AcceptanceType.REACH) {
				return transformNormalFormReachCondition(model, objectiveDA, conditionDA, statesOfInterest);
			}
			if (objectiveDA.getAutomaton().getAcceptance().getType() == AcceptanceType.REACH) {
				return transformNormalFormReachObjective(model, objectiveDA, conditionDA, statesOfInterest);
			}

			// 1) LTL Product Transformation for Condition
			LTLProduct<explicit.MDP> conditionProduct = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
			explicit.MDP conditionProductModel        = conditionProduct.getProductModel();
			BitSet conditionStatesOfInterest          = conditionProduct.getTransformedStatesOfInterest();
			BitSet acceptConditionStates              = getLtlTransformer().findAcceptingStates(conditionProduct);
			Finally<explicit.MDP> conditionPath       = new Finally<>(conditionProductModel, acceptConditionStates);

			// FIXME ALG: consider whether this is actually an error in a normal-form transformation
			BitSet conditionFalsifiedStates = checkSatisfiability(conditionPath, conditionStatesOfInterest);

			// compute bad states
			BitSet badStates = computeBadStates(conditionProduct, conditionFalsifiedStates);

			// 2) LTL Product Transformation for Objective
			LTLProduct<explicit.MDP> objectiveAndConditionProduct = getLtlTransformer().constructProduct(conditionProductModel, objectiveDA.liftToProduct(conditionProduct), conditionStatesOfInterest);
			explicit.MDP objectiveAndConditionModel               = objectiveAndConditionProduct.getProductModel();
			BitSet objectiveAndConditionStatesOfInterest        = objectiveAndConditionProduct.getTransformedStatesOfInterest();

			BitSet conditionFalsifiedLifted = objectiveAndConditionProduct.liftFromModel(conditionFalsifiedStates);
			BitSet badStatesLifted          = objectiveAndConditionProduct.liftFromModel(badStates);

			// 3) Lift Condition Acceptance
			AcceptanceStreett conditionAcceptanceLifted = new AcceptanceStreett();
			for (StreettPair streettPair : (AcceptanceStreett) conditionProduct.getAcceptance()) {
				BitSet R = objectiveAndConditionProduct.liftFromModel(streettPair.getR());
				BitSet G = objectiveAndConditionProduct.liftFromModel(streettPair.getG());
				conditionAcceptanceLifted.add(new StreettPair(R, G));
			}
			// compute redistribution for falsified objective
			ProbabilisticRedistribution objectiveFalsified = redistributeProb0Objective(objectiveAndConditionProduct, conditionFalsifiedLifted, badStatesLifted);

			// 4) Conjunction of Objective and Condition Acceptance
			AcceptanceStreett objectiveAndConditionAcceptance = new AcceptanceStreett();
			objectiveAndConditionAcceptance.addAll((AcceptanceStreett) objectiveAndConditionProduct.getAcceptance().clone());
			objectiveAndConditionAcceptance.addAll(conditionAcceptanceLifted);
			objectiveAndConditionProduct.setAcceptance(objectiveAndConditionAcceptance);

			// 5) Objective & Condition Goal States
			// compute states where objective and condition can be satisfied
			BitSet instantGoalStates = computeInstantGoalStates(objectiveAndConditionProduct);

			// transform goal-fail-stop
			ProbabilisticRedistribution objectiveSatisfied          = new ProbabilisticRedistribution();
			ProbabilisticRedistribution conditionSatisfied          = new ProbabilisticRedistribution();
			GoalFailStopTransformation<explicit.MDP> transformation = transformGoalFailStop(objectiveAndConditionModel, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedLifted, badStatesLifted, objectiveAndConditionStatesOfInterest);

			// build expression
			ExpressionLabel goal                        = new ExpressionLabel(transformation.getGoalLabel());
			ExpressionTemporal transformedObjectiveTmp  = Expression.Finally(goal);
			ExpressionProb transformedObjective         = new ExpressionProb(transformedObjectiveTmp, MinMax.max(), "=", null);
			// All paths satisfying the condition eventually reach an accepting EC.
			// The label accept_condition is an artificial and stands for the automaton's acceptance condition.
			Expression transformedCondition             = new ExpressionLabel("accept_condition");
			ExpressionConditional transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);

			// 3) Compose Transformations
			transformation = transformation.chain(objectiveAndConditionProduct).chain(conditionProduct);

			return new Pair<>(transformation, transformedExpression);
		}

		public BitSet computeInstantGoalStates(LTLProduct<explicit.MDP> objectiveAndConditionProduct)
				throws PrismException
		{
			BitSet acceptingStates = getLtlTransformer().findAcceptingStates(objectiveAndConditionProduct);
			// States in remain from which some scheduler can enforce acceptance to maximize probability
			return getMDPModelChecker().computeProb1E(objectiveAndConditionProduct.getProductModel(), false, ALL_STATES, acceptingStates);
		}

		/**
		 * Compute redistribution for falsified objective.
		 * For efficiency, do not minimizing the probability to satisfy the condition, but
		 * maximize the probability to reach badStates | conditionFalsifiedStates, which is equivalent.
		 */
		public ProbabilisticRedistribution redistributeProb0Objective(LTLProduct<explicit.MDP> product, BitSet conditionFalsified, BitSet conditionMaybeFalsified)
				throws PrismException
		{
			if ( !settings.getBoolean(PrismSettings.CONDITIONAL_RESET_MDP_MINIMIZE)) {
				// Skip costly normalization
				return new ProbabilisticRedistribution();
			}

			// compute accepting states (ECs or REACH states)
			explicit.MDP productModel           = product.getProductModel();
			BitSet acceptObjectiveStates        = getLtlTransformer().findAcceptingStates(product);
			Finally<explicit.MDP> objectivePath = new Finally<>(productModel, acceptObjectiveStates);

			// path to non-accepting states
			BitSet conditionFalsifiedStates              = BitSetTools.union(conditionFalsified, conditionMaybeFalsified);
			Finally<explicit.MDP> conditionFalsifiedPath = new Finally<>(productModel, conditionFalsifiedStates);

			// compute redistribution
			ProbabilisticRedistribution objectiveFalsified = redistributeProb0Complement(objectivePath, conditionFalsifiedPath);
			return objectiveFalsified;
		}

		@Override
		public LtlUntilTransformer.MDP getLtlUntilTransformer()
		{
			return new LtlUntilTransformer.MDP(getModelChecker());
		}

		@Override
		public FinallyLtlTransformer.MDP getFinallyLtlTransformer()
		{
			return new FinallyLtlTransformer.MDP(getModelChecker());
		}
	}
}
