package prism.conditional.transform;

import java.util.BitSet;

import acceptance.AcceptanceStreett;
import acceptance.AcceptanceType;
import acceptance.AcceptanceStreett.StreettPair;
import acceptance.AcceptanceStreettDD;
import acceptance.AcceptanceStreettDD.StreettPairDD;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.UndefinedTransformationException;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.LTLModelChecker;
import prism.LTLModelChecker.LTLProduct;
import prism.ModelTransformationNested;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.OpRelOpBound;
import prism.Pair;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.conditional.NewConditionalTransformer;
import prism.conditional.SimplePathProperty.Finally;
import prism.conditional.SimplePathProperty.Until;
import prism.conditional.transform.GoalFailStopTransformer.NormalFormTransformation;
import prism.conditional.transform.LTLProductTransformer.LabeledDA;
import prism.conditional.transform.GoalFailStopTransformation.GoalFailStopOperator;



public interface FailStopTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewConditionalTransformer<M, MC>
{
	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.STREETT};

	public static final boolean ROW    = true;
	public static final boolean COLUMN = false;



	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression)
			throws PrismLangException
	{
		return LTLModelChecker.isSupportedLTLFormula(model.getModelType(), expression.getCondition());
	}

	@Override
	default boolean canHandleObjective(M model, ExpressionConditional expression)
			throws PrismLangException
	{
		// can handle probabilities only
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel = objective.getRelopBoundInfo(getModelChecker().getConstantValues());
		// can handle maximal probabilities only
		if (oprel.getMinMax(model.getModelType()).isMin()) {
			return false;
		}
//		return getLtlTransformer().canHandle(model, objective.getExpression());
		return LTLModelChecker.isSupportedLTLFormula(model.getModelType(), objective.getExpression());
	}

	@Override
	default NormalFormTransformation<M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: build omega automaton
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTemp = objective.getExpression();
		LabeledDA objectiveDA    = getLtlTransformer().constructDA(model, objectiveTemp, ACCEPTANCE_TYPES);

		// 2) Condition: build omega automaton
		Expression conditionTmp = expression.getCondition();
		LabeledDA conditionDA = getLtlTransformer().constructDA(model, conditionTmp, ACCEPTANCE_TYPES);

		LTLProduct<M> product;
		Pair<GoalFailStopTransformation<M>,ExpressionConditional> result;
		if (objectiveDA.getAutomaton().getAcceptance().getType() == AcceptanceType.REACH) {
			product                         = getLtlTransformer().constructProduct(model, objectiveDA, statesOfInterest);
			M productModel                  = product.getTransformedModel();
			JDDNode acceptStates            = getLtlTransformer().findAcceptingStates(product);
			Finally objectivePathProduct    = new Finally(productModel, acceptStates);
			JDDNode statesOfInterestProduct = product.getTransformedStatesOfInterest();

			GoalStopTransformer<M, MC> goalStopTransformer = getGoalStopTransformer();
			getLog().println("\nDetected acceptance REACH for objective, delegating to " + goalStopTransformer.getName());
			result = goalStopTransformer.transform(productModel, objectivePathProduct, conditionDA.liftToProduct(product), statesOfInterestProduct);

		} else if (conditionDA.getAutomaton().getAcceptance().getType() == AcceptanceType.REACH) {
			product                         = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
			M productModel                  = product.getTransformedModel();
			JDDNode acceptStates            = getLtlTransformer().findAcceptingStates(product);
			Finally conditionPathProduct    = new Finally(productModel, acceptStates);
			JDDNode statesOfInterestProduct = product.getTransformedStatesOfInterest();

			GoalFailTransformer<M,MC> goalFailTransformer = getGoalFailTransformer();
			getLog().println("\nDetected acceptance REACH for objective, delegating to " + goalFailTransformer.getName());
			result = goalFailTransformer.transform(productModel, objectiveDA.liftToProduct(product), conditionPathProduct, statesOfInterestProduct);
		} else {
			// 1) LTL Product Transformation for Condition
			LTLProduct<M> conditionProduct = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
			M productModel                 = conditionProduct.getTransformedModel();

			// Lift state sets to product model
			JDDNode statesOfInterestProduct = product.getTransformedStatesOfInterest();

			JDDNode acceptStates = getLtlTransformer().findAcceptingStates(product);
			Finally conditionPathProduct = new Finally(productModel, acceptStates);

			// compute unsatisfied states
			JDDNode conditionUnsatisfiedProduct = computeUnsatified(productModel, conditionPathProduct);
			checkSatisfiability(conditionUnsatisfiedProduct, statesOfInterestProduct);

			// 2) LTL Product Transformation for Objective
			LTLProduct<M> objectiveAndConditionProduct = getLtlTransformer().constructProduct(productModel, objectiveDA.liftToProduct(conditionProduct), statesOfInterestProduct);
			M objectiveAndConditionModel               = objectiveAndConditionProduct.getProductModel();

			// 3) Lift Condition Acceptance
			AcceptanceStreettDD conditionAcceptanceLifted = new AcceptanceStreettDD();
			for (StreettPairDD streettPair : (AcceptanceStreettDD) conditionProduct.getAcceptance()) {
				// FIXME ALG: add liftFromModel to LTLProduct
				JDDNode R = JDD.And(streettPair.getR(), objectiveAndConditionModel.getReach().copy());
//				JDDNode R = objectiveAndConditionProduct.liftFromModel(streettPair.getR());
				JDDNode G = JDD.And(streettPair.getG(), objectiveAndConditionModel.getReach().copy());
//				JDDNOde G = objectiveAndConditionProduct.liftFromModel(streettPair.getG());
				conditionAcceptanceLifted.add(new StreettPairDD(R, G));
			}

			// 4) Conjunction of Objective and Condition Acceptance
			AcceptanceStreettDD objectiveAndConditionAcceptance = new AcceptanceStreettDD();
			objectiveAndConditionAcceptance.addAll((AcceptanceStreettDD) objectiveAndConditionProduct.getAcceptance());
			objectiveAndConditionAcceptance.addAll(conditionAcceptanceLifted);

			// 5) Objective & Condition Goal States
			JDDNode objectiveAndConditionGoal = getLtlTransformer().findAcceptingStates(objectiveAndConditionModel, objectiveAndConditionAcceptance);
			// FIXME ALG: enlarge target set and move to goal with probability 1.0???
//			objectiveAndConditionGoal = computeProb1A(objectiveAndConditionModel, null, objectiveAndConditionGoal);

			// 6) Deadlock hopeless states
			JDDNode unsatisfiableLifted = objectiveAndConditionProduct.liftFromModel(unsatisfiable);
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
			transformation = new ConditionalReachabilitiyTransformation<>(nested, objectiveAndConditionGoalStatesLifted);

		}

		
		// compute unsatisfied states
		JDDNode conditionUnsatisfied  = computeUnsatified(model, conditionPath);
		checkSatisfiability(conditionUnsatisfied, statesOfInterest);

		// compute normal-form states for condition
		// FIXME ALG: reuse precomputation?
		JDDNode conditionNormalStates = computeNormalFormStates(model, conditionPath);

		// 1) LTL Product Transformation for Objective
		LTLProduct<M> product = getLtlTransformer().constructProduct(model, objectiveDA, statesOfInterest);
		M productModel        = product.getTransformedModel();

		// Lift state sets to product model
		Until conditionPathProduct = conditionPath.copy(productModel);
		conditionPath.clear();
		JDDNode conditionUnsatisfiedProduct = JDD.And(conditionUnsatisfied, productModel.getReach().copy());
		JDDNode conditionNormalStatesProduct = JDD.And(conditionNormalStates, productModel.getReach().copy());

////>>> Debug: print conditionNormalStates
//getLog().println("conditionNormalStates:");
//JDD.PrintMinterms(getLog(), conditionNormalStates.copy());
//new StateValuesMTBDD(conditionNormalStates.copy(), model).print(getLog());
		// compute ECs in succ*(terminal) ...
		// FIXME ALG: reuse precomputation?
		JDDNode restrict = computeSuccStar(productModel, conditionNormalStatesProduct);
		if (conditionPathProduct.isNegated()) {
			// ... and in S \ unsatisfiable
			restrict = JDD.Or(restrict, JDD.Not(conditionUnsatisfiedProduct.copy()));
		}
		JDDNode acceptStates = getLtlTransformer().findAcceptingStates(product, restrict);
		JDD.Deref(restrict, conditionNormalStatesProduct);
//		acceptStates         = computeProb1A(model, null, acceptStates);
		String acceptLabel   = productModel.addUniqueLabelDD("accept", acceptStates.copy());
		Finally objectivePathProduct = new Finally(productModel, acceptStates);

		JDDNode statesOfInterestProduct = product.getTransformedStatesOfInterest();

		// 2) Normal-Form Transformation
		Pair<GoalFailStopTransformation<M>,ExpressionConditional> result;
		switch (product.getAcceptance().getType()) {
		case REACH:
			GoalFailStopTransformer<M, MC> goalFailStopTransformer = getGoalFailStopTransformer();
			getLog().println("\nDetected acceptance REACH for objective, delegating to " + goalFailStopTransformer.getName());
			result = goalFailStopTransformer.transform(productModel, objectivePathProduct, conditionPathProduct, conditionUnsatisfiedProduct, statesOfInterestProduct);
			break;
		case RABIN:
		case GENERALIZED_RABIN:
		case STREETT:
			result = transform(productModel, objectivePathProduct, conditionPathProduct, conditionUnsatisfiedProduct, statesOfInterestProduct);
			break;
		default:
			throw new PrismException("unsupported acceptance type: " + product.getAcceptance().getType());
		}
		objectivePathProduct.clear();

		// 3) Compose Transformations
		ModelTransformationNested<M, M, M> nested = new ModelTransformationNested<>(product, result);

		// transform expression
		ExpressionLabel goal = new ExpressionLabel(result.getGoalLabel());
		ExpressionLabel fail = new ExpressionLabel(result.getFailLabel());
		Expression transformedObjectiveTemp = Expression.Finally(goal);
		if (product.getAcceptance().getType() != AcceptanceType.REACH && conditionPathProduct.isNegated()) {
			transformedObjectiveTemp        = Expression.Or(transformedObjectiveTemp, Expression.Finally(new ExpressionLabel(acceptLabel)));
		}
		ExpressionProb transformedObjective = new ExpressionProb(transformedObjectiveTemp, objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionTemporal transformedCondition;
		if (conditionPathProduct.isNegated()) {
			// All path violating the condition end in the fail state.
			transformedCondition      = Expression.Globally(Expression.Not(fail));
		} else {
			// All path satisfying the condition end in the goal or stop state.
			ExpressionLabel stopLabel = new ExpressionLabel(result.getStopLabel());
			transformedCondition      = Expression.Finally(Expression.Parenth(Expression.Or(goal, stopLabel)));
		}
		ExpressionConditional transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);

		JDDNode badStates = computeMaybeUnsatified(productModel, conditionPathProduct, conditionUnsatisfiedProduct);
		badStates         = JDD.And(badStates, result.getNonTrapStates());
		// FIXME ALG: do not reset from goal states, go over fail-state
//		bad.andNot(objectiveGoal);
		String badLabel   = result.getTransformedModel().addUniqueLabelDD("bad", badStates);

		conditionPathProduct.clear();
		return new NormalFormTransformation<>(nested, expression, transformedExpression, fail.getName(), badLabel);
	}

	Pair<GoalFailStopTransformation<M>, ExpressionConditional> transform(M model, LabeledDA objectiveDA, Finally conditionPathProduct, JDDNode statesOfInterest);

	GoalFailTransformer<M, MC> getGoalFailTransformer();

	GoalStopTransformer<M, MC> getGoalStopTransformer();

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transform(M model, Finally objectivePath, Until conditionPath, JDDNode conditionUnsatisfied, JDDNode statesOfInterest)
			throws UndefinedTransformationException, PrismException
	{
		checkSatisfiability(conditionUnsatisfied, statesOfInterest);
		JDD.Deref(statesOfInterest);

////>>> Debug: print conditionUnsatisfied
//getLog().println("conditionUnsatisfied:");
//JDD.PrintMinterms(getLog(), conditionUnsatisfied.copy());
//new StateValuesMTBDD(conditionUnsatisfied.copy(), model).print(getLog());
////>>> Debug: print conditionUnsatisfied
//getLog().println("badStates:");
//JDD.PrintMinterms(getLog(), conditionUnsatisfied.copy());
//new StateValuesMTBDD(badStates.copy(), model).print(getLog());

		// compute normal-form states for condition
		JDDNode conditionNormalStates = computeNormalFormStates(model, conditionPath);

		// compute probabilities for objective
		JDDNode objectiveNormalProbs  = computeNormalFormProbs(model, objectivePath);

		// transform model
		GoalFailStopOperator<M> operator = configureOperator(model, conditionUnsatisfied, JDD.Constant(0), objectiveNormalProbs, conditionNormalStates, JDD.Constant(0));
		return new GoalFailStopTransformation<>(model, operator);
	}

	default JDDNode checkSatisfiability(JDDNode conditionUnsatisfied, JDDNode statesOfInterest)
			throws UndefinedTransformationException
	{
		if (JDD.IsContainedIn(statesOfInterest, conditionUnsatisfied)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		return conditionUnsatisfied;
	}

	JDDNode computeUnsatified(M model, Until until);

	JDDNode computeMaybeUnsatified(M model, Until until, JDDNode unsatisfiedStates);

	JDDNode computeNormalFormStates(M model, Until until);

	JDDNode computeNormalFormProbs(M model, Until until) throws PrismException;

	GoalFailStopOperator<M> configureOperator(M model, JDDNode conditionUnsatisfied, JDDNode objectiveNormalStates, JDDNode objectiveNormalProbs,
			JDDNode conditionNormalStates, JDDNode conditionProbs) throws PrismException;

	GoalFailStopTransformer<M, MC> getGoalFailStopTransformer();



	public class DTMC extends NewConditionalTransformer.DTMC implements FailStopTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode computeUnsatified(ProbModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0(model, until.getRemain(), until.getGoal());
			}
		}

		@Override
		public JDDNode computeMaybeUnsatified(ProbModel model, Until until, JDDNode unsatisfiedStates)
		{
			// DTMCs are purely probabilistic
			return JDD.Constant(0);
		}

		@Override
		public JDDNode computeNormalFormStates(ProbModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1(model, until.getRemain(), until.getGoal());
			}
// FIXME ALG: fishy: should be all states with Pmin=1 (Condition)
//			JDDNode conditionWeakRemain   = getWeakRemainStates(model, conditionRemain, conditionGoal, conditionNegated);
//			JDDNode conditionWeakGoal     = getWeakGoalStates(model, conditionRemain, conditionGoal, conditionNegated);
//			return computeProb1(model, conditionWeakRemain, conditionWeakGoal);
		}

		@Override
		public JDDNode computeNormalFormProbs(ProbModel model, Until until) throws PrismException
		{
			return computeUntilProbs(model, until);
		}

		@Override
		public GoalFailStopOperator<ProbModel> configureOperator(ProbModel model, JDDNode conditionUnsatisfied, JDDNode objectiveNormalStates, JDDNode objectiveNormalProbs,
				JDDNode conditionNormalStates, JDDNode conditionProbs) throws PrismException
		{
			return new GoalFailStopOperator.DTMC(model, objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, conditionProbs, conditionUnsatisfied, getLog());
		}

		@Override
		public GoalFailStopTransformer<ProbModel, ProbModelChecker> getGoalFailStopTransformer()
		{
			return new GoalFailStopTransformer.DTMC(getModelChecker());
		}
	}



	public class MDP extends NewConditionalTransformer.MDP implements FailStopTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode computeUnsatified(NondetModel model, Until until)
		{
			return until.isNegated() ? computeProb1A(model, until.getRemain(), until.getGoal()) : computeProb0A(model, until.getRemain(), until.getGoal());
		}

		@Override
		public JDDNode computeMaybeUnsatified(NondetModel model, Until until, JDDNode unsatisfiedStates)
		{
			JDDNode maybeUnsatisfiedStates;
			if (until.isNegated()) {
				maybeUnsatisfiedStates = computeProb1E(model, until.getRemain(), until.getGoal());
			} else {
				maybeUnsatisfiedStates = computeProb0E(model, until.getRemain(), until.getGoal());
			}
			return JDD.And(maybeUnsatisfiedStates, JDD.Not(unsatisfiedStates.copy()));
		}

		@Override
		public JDDNode computeNormalFormStates(NondetModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0A(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1A(model, until.getRemain(), until.getGoal());
			}
// FIXME ALG: fishy: should be all states with Pmin=1 (Condition)
//			JDDNode conditionWeakRemain   = getWeakRemainStates(model, conditionRemain, conditionGoal, conditionNegated);
//			JDDNode conditionWeakGoal     = getWeakGoalStates(model, conditionRemain, conditionGoal, conditionNegated);
//			return computeProb1A(model, conditionWeakRemain, conditionWeakGoal);
		}

		@Override
		public JDDNode computeNormalFormProbs(NondetModel model, Until until) throws PrismException
		{
			return computeUntilMaxProbs(model, until);
		}

		public GoalFailStopOperator<NondetModel> configureOperator(NondetModel model, JDDNode conditionUnsatisfied, JDDNode objectiveNormalStates, JDDNode objectiveNormalProbs,
				JDDNode conditionNormalStates, JDDNode conditionProbs) throws PrismException
		{
			return new GoalFailStopOperator.MDP(model, objectiveNormalStates, objectiveNormalProbs, conditionNormalStates, conditionProbs, conditionUnsatisfied, getLog());
		}

		@Override
		public GoalFailStopTransformer<NondetModel, NondetModelChecker> getGoalFailStopTransformer()
		{
			return new GoalFailStopTransformer.MDP(getModelChecker());
		}
	}
}