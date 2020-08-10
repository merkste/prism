package prism.conditional.reset;

import acceptance.AcceptanceOmegaDD;
import acceptance.AcceptanceType;
import explicit.conditional.transformer.UndefinedTransformationException;
import jdd.Clearable;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.Model;
import prism.ModelExpressionTransformation;
import prism.ModelTransformation;
import prism.ModelTransformationNested;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.StochModel;
import prism.StochModelChecker;
import prism.LTLModelChecker.LTLProduct;
import prism.conditional.ConditionalTransformer;
import prism.conditional.reset.GoalFailStopTransformation.GoalFailStopOperator;
import prism.conditional.reset.GoalFailStopTransformation.ProbabilisticRedistribution;
import prism.conditional.transformer.BasicModelExpressionTransformation;
import prism.statebased.CachedMCModelChecker;
import prism.statebased.SimplePathEvent.Reach;

// FIXME ALG: add comment
public interface NormalFormTransformer<M extends ProbModel, C extends StateModelChecker> extends ConditionalTransformer<M, C>, Clearable
{
	@Override
	default boolean canHandleObjective(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		// can handle probabilities only
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel       = objective.getRelopBoundInfo(getModelChecker().getConstantValues());
		// can handle maximal probabilities only
		return oprel.getMinMax(model.getModelType()).isMax();
	}

	@Override
	default ModelExpressionTransformation<M, M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		// 1) Normal-Form Transformation
		NormalFormTransformation<M> normalFormTransformation = transformNormalForm(model, expression, statesOfInterest);
		M normalFormModel                                    = normalFormTransformation.getTransformedModel();
		getLog().println("\nNormal-form transformation: " + normalFormTransformation.getTransformedExpression());

		// 2) Reset Transformation
		JDDNode badStates                                       = computeResetStates(normalFormTransformation);
		JDDNode transformedStatesOfInterest                     = normalFormTransformation.getTransformedStatesOfInterest();
		ModelTransformation<M, ? extends M> resetTransformation = transformReset(normalFormModel, badStates, transformedStatesOfInterest);

		// 3) Transform expression
		Expression originalExpression    = normalFormTransformation.getOriginalExpression();
		Expression transformedExpression = normalFormTransformation.getTransformedExpression().getObjective();

		// 4) Compose Transformations
		ModelTransformationNested<M, M, ? extends M> nested = new ModelTransformationNested<>(normalFormTransformation, resetTransformation);

		return new BasicModelExpressionTransformation<>(nested, originalExpression, transformedExpression);
	}

	NormalFormTransformation<M> transformNormalForm(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException;

	ModelTransformation<M, ? extends M> transformReset(M model, JDDNode resetStates, JDDNode statesOfInterest)
			throws PrismException;

	JDDNode checkSatisfiability(Reach<M> conditionPath, JDDNode statesOfInterest)
			throws PrismException;

	default JDDNode checkSatisfiability(JDDNode conditionUnsatisfied, JDDNode statesOfInterest)
			throws UndefinedTransformationException
	{
		if (JDD.IsContainedIn(statesOfInterest, conditionUnsatisfied)) {
			// FIXME ALG: Deref JDDNodes!
			throw new UndefinedTransformationException("Condition is not satisfiable");
		}
		return conditionUnsatisfied;
	}

	default JDDNode computeResetStates(NormalFormTransformation<M> transformation)
	{
		JDDNode badStates = transformation.getTransformedModel().getLabelDD(transformation.getBadLabel());
		JDDNode failState = transformation.getTransformedModel().getLabelDD(transformation.getFailLabel());
		return JDD.Or(badStates.copy(), failState.copy());
	}

	JDDNode computeBadStates(Reach<M> reach, JDDNode unsatisfiedStates) throws PrismException;

	JDDNode computeBadStates(LTLProduct<M> product, JDDNode unsatisfiedStates)
			throws PrismException;

	ProbabilisticRedistribution redistributeProb1(Reach<M> pathProb1, Reach<M> pathProbs)
			throws PrismException;

	ProbabilisticRedistribution redistributeProb0(Reach<M> pathProb0, Reach<M> pathProbs)
			throws PrismException;

	default GoalFailStopTransformation<M> transformGoalFailStop(M model, ProbabilisticRedistribution objectiveSatisfied, ProbabilisticRedistribution conditionSatisfied, ProbabilisticRedistribution objectiveFalsified, JDDNode instantGoalStates, JDDNode conditionFalsifiedStates, JDDNode badStates, JDDNode statesOfInterest)
			throws PrismException
	{
		GoalFailStopOperator<M> operator = getGoalFailStopOperator(model, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, statesOfInterest);
		return new GoalFailStopTransformation<M>(model, operator, badStates);
	}

	public abstract GoalFailStopOperator<M> getGoalFailStopOperator(M model, ProbabilisticRedistribution objectiveSatisfied, ProbabilisticRedistribution conditionSatisfied, ProbabilisticRedistribution objectiveFalsified, JDDNode instantGoalStates, JDDNode conditionFalsifiedStates, JDDNode statesOfInterest)
			throws PrismException;

	// FIXME ALG: Leak. Ensure clear is called after transformation even if an exception occurs
	// FIXME ALG: Check Leakage in explicit implementation
	@Override
	default void clear()
	{
		// No-Op except caching for mc results is implemented
	}



	public static abstract class MC<M extends ProbModel, C extends ProbModelChecker> extends ConditionalTransformer.Basic<M, C> implements ConditionalTransformer.MC<M,C>, NormalFormTransformer<M, C>
	{
		protected CachedMCModelChecker<M,C> mcModelChecker;

		public MC(C modelChecker)
		{
			super(modelChecker);
			mcModelChecker = createCachedMcModelChecker();
		}

		protected abstract CachedMCModelChecker<M, C> createCachedMcModelChecker();

		@Override
		public CachedMCModelChecker<M, C> getMcModelChecker()
		{
			return mcModelChecker;
		}

		/**
		 * Override to enable caching of probabilities.
		 *
		 * @see NormalFormTransformer#transform(ProbModel, ExpressionConditional, JDDNode)
		 */
		@Override
		public ModelExpressionTransformation<M, M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
				throws PrismException
		{
			ModelExpressionTransformation<M,M> result = NormalFormTransformer.super.transform(model, expression, statesOfInterest);
			clear();
			return result;
		}

		@Override
		public JDDNode checkSatisfiability(Reach<M> conditionPath, JDDNode statesOfInterest)
				throws PrismException
		{
			JDDNode conditionFalsifiedStates = getMcModelChecker().computeProb0(conditionPath);
			checkSatisfiability(conditionFalsifiedStates, statesOfInterest);
			return conditionFalsifiedStates;
		}

		@Override
		public MCResetTransformation<M> transformReset(M model, JDDNode resetStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new MCResetTransformation<>(model, resetStates, statesOfInterest);
		}

		@Override
		public JDDNode computeBadStates(Reach<M> reach, JDDNode unsatisfiedStates)
		{
			// DTMCs are purely probabilistic
			return noStates();
		}

		@Override
		public JDDNode computeBadStates(LTLProduct<M> product, JDDNode unsatisfiedStates)
		{
			// DTMCs are purely probabilistic
			return noStates();
		}

		@Override
		public ProbabilisticRedistribution redistributeProb1(Reach<M> pathProb1, Reach<M> pathProbs)
				throws PrismException
		{
			pathProb1.requireSameModel(pathProbs);

			JDDNode states = getMcModelChecker().computeProb1(pathProb1);
			JDDNode probabilities;
			if (states.equals(JDD.ZERO)) {
				probabilities = JDD.Constant(0);
			} else {
				probabilities = getMcModelChecker().computeProbs(pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0(Reach<M> pathProb0, Reach<M> pathProbs)
				throws PrismException
		{
			pathProb0.requireSameModel(pathProbs);

			JDDNode states = getMcModelChecker().computeProb0(pathProb0);
			JDDNode probabilities;
			if (states.equals(JDD.ZERO)) {
				probabilities = JDD.Constant(0);
			} else {
				probabilities = getMcModelChecker().computeProbs(pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		@Override
		public void clear()
		{
			mcModelChecker.clear();
		}
	}



	public static abstract class CTMC extends MC<StochModel, StochModelChecker> implements ConditionalTransformer.CTMC
	{
		public CTMC(StochModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public CachedMCModelChecker.CTMC createCachedMcModelChecker()
		{
			return new CachedMCModelChecker.CTMC(getModelChecker());
		}

		@Override
		public GoalFailStopOperator.CTMC getGoalFailStopOperator(StochModel model, ProbabilisticRedistribution objectiveSatisfied, ProbabilisticRedistribution conditionSatisfied, ProbabilisticRedistribution objectiveFalsified, JDDNode instantGoalStates, JDDNode conditionFalsifiedStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new GoalFailStopOperator.CTMC(model, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, statesOfInterest, getLog());
		}
	}



	public static abstract class DTMC extends MC<ProbModel, ProbModelChecker> implements ConditionalTransformer.DTMC
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public CachedMCModelChecker.DTMC createCachedMcModelChecker()
		{
			return new CachedMCModelChecker.DTMC(getModelChecker());
		}

		@Override
		public GoalFailStopOperator.DTMC getGoalFailStopOperator(ProbModel model, ProbabilisticRedistribution objectiveSatisfied, ProbabilisticRedistribution conditionSatisfied, ProbabilisticRedistribution objectiveFalsified, JDDNode instantGoalStates, JDDNode conditionFalsifiedStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new GoalFailStopOperator.DTMC(model, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, statesOfInterest, getLog());
		}
	}



	public static abstract class MDP extends ConditionalTransformer.Basic<NondetModel, NondetModelChecker> implements ConditionalTransformer.MDP, NormalFormTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode checkSatisfiability(Reach<NondetModel> conditionPath, JDDNode statesOfInterest)
				throws PrismException
		{
			JDDNode conditionFalsifiedStates = getMDPModelChecker().computeProb0A(conditionPath);
			checkSatisfiability(conditionFalsifiedStates, statesOfInterest);
			return conditionFalsifiedStates;
		}

		@Override
		public ModelTransformation<NondetModel, ? extends NondetModel> transformReset(NondetModel model, JDDNode resetStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new MDPResetTransformation(model, resetStates, statesOfInterest);
		}

		@Override
		public JDDNode computeBadStates(Reach<NondetModel> reach, JDDNode unsatisfiedStates)
				throws PrismException
		{
			JDDNode maybeFalsified = getMDPModelChecker().computeProb0E(reach);
			if (maybeFalsified.equals(JDD.ZERO)) {
				return maybeFalsified;
			}
			// FIXME ALG: check if we may alter the set if we use caching
			return JDD.And(maybeFalsified, JDD.Not(unsatisfiedStates.copy()));
		}

		@Override
		public JDDNode computeBadStates(LTLProduct<NondetModel> product, JDDNode unsatisfiedStates)
				throws PrismException
		{
			// bad states == {s | Pmin=0[<> Condition]}
			NondetModel productModel                        = product.getProductModel();
			AcceptanceOmegaDD conditionAcceptance           = product.getAcceptance();
			AcceptanceOmegaDD conditionAcceptanceComplement = conditionAcceptance.complement(AcceptanceType.allTypes());
			JDDNode maybeUnsatisfiedStates                  = getLtlTransformer().findAcceptingStates(productModel, conditionAcceptanceComplement);
			conditionAcceptanceComplement.clear();
//			// reduce number of choices, i.e.
//			// - reset only from r-states of streett acceptance
//			if (conditionAcceptance instanceof AcceptanceStreett) {
//				BitSet rStates = BitSetTools.union(new MappingIterator.From<>((AcceptanceStreett) conditionAcceptance, StreettPair::getR));
//				bad.and(rStates);
//			}
			JDDNode maybeUnsatisfiedStatesProb1E = getMDPModelChecker().computeProb1E(productModel, false, ALL_STATES, maybeUnsatisfiedStates);
			JDD.Deref(maybeUnsatisfiedStates);
			return JDD.And(maybeUnsatisfiedStatesProb1E, JDD.Not(unsatisfiedStates.copy()));
		}

		@Override
		public ProbabilisticRedistribution redistributeProb1(Reach<NondetModel> pathProb1, Reach<NondetModel> pathProbs)
				throws PrismException
		{
			pathProb1.requireSameModel(pathProbs);

			JDDNode states = getMDPModelChecker().computeProb1A(pathProb1);
			JDDNode probabilities;
			if (states.equals(JDD.ZERO)) {
				probabilities = JDD.Constant(0);
			} else {
				probabilities = getMDPModelChecker().computeMaxProbs(pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0(Reach<NondetModel> pathProb0, Reach<NondetModel> pathProbs)
				throws PrismException
		{
			pathProb0.requireSameModel(pathProbs);

			JDDNode states = getMDPModelChecker().computeProb0A(pathProb0);
			JDDNode probabilities;
			if (states.equals(JDD.ZERO)) {
				probabilities = JDD.Constant(0);
			} else {
				probabilities = getMDPModelChecker().computeMinProbs(pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		/**
		 * Compute redistribution but use complementary path event for efficiency.
		 * Instead of Pmin(path) use 1-Pmax(not path).
		 */
		public ProbabilisticRedistribution redistributeProb0Complement(Reach<NondetModel> pathProb0, Reach<NondetModel> compPathProbs)
				throws PrismException
		{
			pathProb0.requireSameModel(compPathProbs);

			JDDNode states = getMDPModelChecker().computeProb0A(pathProb0);
			JDDNode probabilities;
			if (states.equals(JDD.ZERO)) {
				probabilities = JDD.Constant(0);
			} else {
				probabilities = getMDPModelChecker().computeMaxProbs(compPathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities).swap(pathProb0.getModel());
		}

		@Override
		public GoalFailStopOperator.MDP getGoalFailStopOperator(NondetModel model, ProbabilisticRedistribution objectiveSatisfied, ProbabilisticRedistribution conditionSatisfied, ProbabilisticRedistribution objectiveFalsified, JDDNode instantGoalStates, JDDNode conditionFalsifiedStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new GoalFailStopOperator.MDP(model, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, statesOfInterest, getLog());
		}
	}



	public class NormalFormTransformation<M extends ProbModel> extends BasicModelExpressionTransformation<M, M>
	{
		protected String failLabel;
		protected String badLabel;

		public NormalFormTransformation(ModelTransformation<M, M> transformation, ExpressionConditional expression, ExpressionConditional transformedExpression,
				String failLabel, String badLabel)
		{
			super(transformation, expression, transformedExpression);
			this.failLabel = failLabel;
			this.badLabel  = badLabel;
		}

		public String getFailLabel()
		{
			return failLabel;
		}

		public String getBadLabel()
		{
			return badLabel;
		}

		@Override
		public ExpressionConditional getOriginalExpression()
		{
			return (ExpressionConditional) originalExpression;
		}

		@Override
		public ExpressionConditional getTransformedExpression()
		{
			return (ExpressionConditional) transformedExpression;
		}
	}
}
