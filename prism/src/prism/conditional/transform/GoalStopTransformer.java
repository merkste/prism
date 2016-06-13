package prism.conditional.transform;


import acceptance.AcceptanceOmegaDD;
import acceptance.AcceptanceType;
import explicit.MinMax;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.UndefinedTransformationException;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import prism.LTLModelChecker;
import prism.LTLModelChecker.LTLProduct;
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



public interface GoalStopTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewConditionalTransformer<M, MC>
{
	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.RABIN, AcceptanceType.GENERALIZED_RABIN, AcceptanceType.STREETT};

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
		Expression normalized = ExpressionInspector.normalizeExpression(objective.getExpression());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	default NormalFormTransformation<M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: compute simple path property
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTmp = objective.getExpression();
		Finally objectivePath      = new Finally(objectiveTmp, getModelChecker(), true);

		// 2) Condition: build omega automaton
		Expression conditionTmp = expression.getCondition();
		LabeledDA conditionDA = getLtlTransformer().constructDA(model, conditionTmp, ACCEPTANCE_TYPES);

		// 3) Transform Model
		Pair<GoalFailStopTransformation<M>,ExpressionConditional> result = transform(model, objectivePath, conditionDA, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression  = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective        = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded, transformation.getFailLabel(), transformation.getBadLabel());
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transform(M model, Finally objectivePath, LabeledDA conditionDA, JDDNode statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Condition
		LTLProduct<M> product = getLtlTransformer().constructProduct(model, conditionDA, statesOfInterest);
		M productModel        = product.getTransformedModel();

		// Lift state sets to product model
		Finally objectivePathProduct = objectivePath.copy(productModel);
		objectivePath.clear();

		// 2) Normal-Form Transformation
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result;
		switch (product.getAcceptance().getType()) {
		case REACH:
			result = transformReach(product, objectivePathProduct);
			break;
		case RABIN:
		case GENERALIZED_RABIN:
		case STREETT:
			result = transform(product, objectivePathProduct);
			break;
		default:
			throw new PrismException("unsupported acceptance type: " + product.getAcceptance().getType());
		}

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.compose(product);
		return new Pair<>(transformation, result.second);
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformReach(LTLProduct<M> product, Finally objectivePathProduct) throws PrismException
	{
		if (product.getAcceptance().getType() != AcceptanceType.REACH) {
			throw new IllegalArgumentException("Expected acceptance REACH.");
		}
		GoalFailStopTransformer<M, MC> goalFailStopTransformer = getGoalFailStopTransformer();
		getLog().println("\nDetected acceptance REACH for objective, delegating to " + goalFailStopTransformer.getName());

		M productModel           = product.getProductModel();

		JDDNode acceptStates         = getLtlTransformer().findAcceptingStates(product);
		Finally conditionPathProduct = new Finally(productModel, acceptStates);

		JDDNode statesOfInterestProduct = product.getTransformedStatesOfInterest();
		return goalFailStopTransformer.transform(productModel, objectivePathProduct, conditionPathProduct, statesOfInterestProduct);
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transform(LTLProduct<M> product, Finally objectivePath)
			throws UndefinedTransformationException, PrismException
	{
		M productModel               = product.getProductModel();
		JDDNode acceptStates         = getLtlTransformer().findAcceptingStates(product);
		Finally conditionPath        = new Finally(productModel, acceptStates);
		JDDNode conditionUnsatisfied = computeUnsatified(productModel, conditionPath);

		// FIXME ALG: consider whether this is actually an error in a normal-form transformation
		JDDNode statesOfInterest = product.getTransformedStatesOfInterest();
		checkSatisfiability(conditionUnsatisfied, statesOfInterest);
		JDD.Deref(statesOfInterest);

		// compute normal-form states for objective
		JDDNode objectiveNormalStates = computeNormalFormStates(productModel, objectivePath);

		// compute probabilities for condition
		JDDNode conditionNormalProbs  = computeNormalFormProbs(productModel, conditionPath);

		// configure transformation operator
		GoalFailStopOperator<M> operator = configureOperator(productModel, conditionUnsatisfied, objectiveNormalStates, JDD.Constant(0), JDD.Constant(0), conditionNormalProbs);

		// compute bad states
		JDDNode badStates = computeMaybeUnsatified(product, conditionUnsatisfied);

		// transform model
		GoalFailStopTransformation<M> transformation = new GoalFailStopTransformation<>(productModel, operator, badStates);

		// build expression
		ExpressionLabel goal                = new ExpressionLabel(transformation.getGoalLabel());
		Expression transformedObjectiveTmp  = Expression.Finally(goal);
		ExpressionProb transformedObjective = new ExpressionProb(transformedObjectiveTmp, MinMax.max(), "=", null);
		// All paths satisfying the condition eventually reach goal or an accepting EC.
		// The label accept_condition is an artificial and stands for the automaton's acceptance condition.
		Expression transformedCondition     = Expression.Or(Expression.Finally(goal), new ExpressionLabel("accept_condition"));
		ExpressionConditional transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);

		objectivePath.clear();
		conditionPath.clear();
		return new Pair<>(transformation, transformedExpression);
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

	JDDNode computeMaybeUnsatified(LTLProduct<M> product, JDDNode unsatisfiedStates) throws PrismException;

	JDDNode computeNormalFormStates(M model, Until until);

	JDDNode computeNormalFormProbs(M model, Until until) throws PrismException;

	GoalFailStopOperator<M> configureOperator(M model, JDDNode conditionUnsatisfied, JDDNode objectiveNormalStates, JDDNode objectiveNormalProbs,
			JDDNode conditionNormalStates, JDDNode conditionProbs) throws PrismException;

	GoalFailStopTransformer<M, MC> getGoalFailStopTransformer();



	public class DTMC extends NewConditionalTransformer.DTMC implements GoalStopTransformer<ProbModel, ProbModelChecker>
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
		public JDDNode computeMaybeUnsatified(LTLProduct<ProbModel> product, JDDNode unsatisfiedStates)
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



	public class MDP extends NewConditionalTransformer.MDP implements GoalStopTransformer<NondetModel, NondetModelChecker>
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
		public JDDNode computeMaybeUnsatified(LTLProduct<NondetModel> product, JDDNode unsatisfiedStates) throws PrismException
		{
			// bad states == {s | Pmin=0[<> Condition]}
			NondetModel productModel                        = product.getProductModel();
			AcceptanceOmegaDD conditionAcceptance           = product.getAcceptance();
			AcceptanceOmegaDD conditionAcceptanceComplement = conditionAcceptance.complement(ACCEPTANCE_TYPES);
			JDDNode maybeUnsatisfiedStates                  = getLtlTransformer().findAcceptingStates(productModel, conditionAcceptanceComplement);
			conditionAcceptanceComplement.clear();
//			// reduce number of choices, i.e.
//			// - reset only from r-states of streett acceptance
//			if (conditionAcceptance instanceof AcceptanceStreett) {
//				BitSet rStates = BitSetTools.union(new MappingIterator.From<>((AcceptanceStreett) conditionAcceptance, StreettPair::getR));
//				bad.and(rStates);
//			}
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