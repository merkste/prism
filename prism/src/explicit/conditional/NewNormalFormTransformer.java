package explicit.conditional;

import java.util.BitSet;

import acceptance.AcceptanceOmega;
import acceptance.AcceptanceOmegaDD;
import acceptance.AcceptanceType;
import common.BitSetTools;
import explicit.BasicModelExpressionTransformation;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ModelExpressionTransformation;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.StateModelChecker;
import explicit.conditional.GoalFailStopTransformation.ProbabilisticRedistribution;
import explicit.conditional.SimplePathProperty.Until;
import explicit.conditional.transformer.ResetTransformer;
import explicit.conditional.transformer.ResetTransformer.ResetTransformation;
import explicit.conditional.transformer.UndefinedTransformationException;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.PrismException;

// FIXME ALG: add comment
public interface NewNormalFormTransformer<M extends Model, MC extends StateModelChecker> extends NewConditionalTransformer<M, MC>
{
	@Override
	default ModelExpressionTransformation<M, M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		// 1) Normal-Form Transformation
		NormalFormTransformation<M> normalFormTransformation = transformNormalForm(model, expression, statesOfInterest);
		M normalFormModel                                    = normalFormTransformation.getTransformedModel();
		getLog().println("Normal-form transformation: " + normalFormTransformation.getTransformedExpression());

		// 2) Reset Transformation
		BitSet badStates                                       = computeResetStates(normalFormTransformation);
		BitSet transformedStatesOfInterest                     = normalFormTransformation.getTransformedStatesOfInterest();
		ModelTransformation<M, ? extends M> resetTransformation = transformReset(normalFormModel, badStates, transformedStatesOfInterest);

		// 3) Transform expression
		Expression originalExpression    = normalFormTransformation.getOriginalExpression();
		Expression transformedExpression = normalFormTransformation.getTransformedExpression().getObjective();

		// 4) Compose Transformations
		ModelTransformationNested<M, M, ? extends M> nested = new ModelTransformationNested<>(normalFormTransformation, resetTransformation);

		return new BasicModelExpressionTransformation<>(nested, originalExpression, transformedExpression);
	}

	NormalFormTransformation<M> transformNormalForm(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException;

	default BitSet computeResetStates(NormalFormTransformation<M> transformation)
	{
		BitSet badStates = transformation.getTransformedModel().getLabelStates(transformation.getBadLabel());
		BitSet failState = transformation.getTransformedModel().getLabelStates(transformation.getFailLabel());
		// FIXME ALG: handle bad/fail states == null
		// FIXME ALG: check whether we may alter the returned sets
		return BitSetTools.union(badStates, failState);
	}

	ModelTransformation<M, ? extends M> transformReset(M model, BitSet resetStates, BitSet statesOfInterest)
			throws PrismException;

	BitSet checkSatisfiability(M model, Until conditionPath, BitSet statesOfInterest)
			throws UndefinedTransformationException;

	default BitSet checkSatisfiability(BitSet conditionUnsatisfied, BitSet statesOfInterest)
			throws UndefinedTransformationException
	{
		if (BitSetTools.isSubset(statesOfInterest, conditionUnsatisfied)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		return conditionUnsatisfied;
	}

	BitSet computeBadStates(M model, Until until, BitSet unsatisfiedStates);

	BitSet computeBadStates(LTLProduct<M> product, BitSet unsatisfiedStates)
			throws PrismException;

	ProbabilisticRedistribution redistributeProb1(M model, Until pathProb1, Until pathProbs)
			throws PrismException;

	ProbabilisticRedistribution redistributeProb0(M model, Until pathProb0, Until pathProbs)
			throws PrismException;

	GoalFailStopOperator<M> configureOperator(M model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, BitSet instantGoalStates, BitSet instantFailStates, BitSet statesOfInterest)
			throws PrismException;



	public static abstract class DTMC extends NewConditionalTransformer.DTMC implements NewNormalFormTransformer<explicit.DTMC, DTMCModelChecker>
	{

		private static final BitSet NO_STATES = new BitSet(0);

		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public BitSet checkSatisfiability(explicit.DTMC model, Until conditionPath, BitSet statesOfInterest)
				throws UndefinedTransformationException
		{
			BitSet conditionFalsifiedStates = computeProb0(model, conditionPath);
			checkSatisfiability(conditionFalsifiedStates, statesOfInterest);
			return conditionFalsifiedStates;
		}

		@Override
		public ResetTransformation<explicit.DTMC> transformReset(explicit.DTMC model, BitSet resetStates, BitSet statesOfInterest)
				throws PrismException
		{
			return new ResetTransformer.DTMC(this).transformModel(model, resetStates, statesOfInterest);
		}

		@Override
		public BitSet computeBadStates(explicit.DTMC model, Until until, BitSet unsatisfiedStates)
		{
			// DTMCs are purely probabilistic
			return NO_STATES;
		}

		@Override
		public BitSet computeBadStates(LTLProduct<explicit.DTMC> product, BitSet unsatisfiedStates)
		{
			// DTMCs are purely probabilistic
			return NO_STATES;
		}

		@Override
		public ProbabilisticRedistribution redistributeProb1(explicit.DTMC model, Until pathProb1, Until pathProbs)
				throws PrismException
		{
			BitSet states = computeProb1(model, pathProb1);
			double[] probabilities;
			if (states.isEmpty()) {
				// FIXME ALG: check whether we use new double[0], new double[model.getNumState()] or null
				probabilities = new double[0];
			} else {
				probabilities = computeUntilProbs(model, pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0(explicit.DTMC model, Until pathProb0, Until pathProbs)
				throws PrismException
		{
			BitSet states = computeProb0(model, pathProb0);
			double[] probabilities;
			if (states.isEmpty()) {
				// FIXME ALG: check whether we use new double[0], new double[model.getNumState()] or null
				probabilities = new double[0];
			} else {
				probabilities = computeUntilProbs(model, pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		@Override
		public GoalFailStopOperator<explicit.DTMC> configureOperator(explicit.DTMC model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, BitSet instantGoalStates, BitSet instantFailStates, BitSet statesOfInterest)
				throws PrismException
		{
			return new GoalFailStopOperator.DTMC(model, goalFail, goalStop, stopFail, instantGoalStates, instantFailStates, statesOfInterest, getLog());
		}
	}



	public static abstract class MDP extends NewConditionalTransformer.MDP implements NewNormalFormTransformer<explicit.MDP, MDPModelChecker>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public BitSet checkSatisfiability(explicit.MDP model, Until conditionPath, BitSet statesOfInterest)
				throws UndefinedTransformationException
		{
			BitSet conditionFalsifiedStates = computeProb0A(model, conditionPath);
			checkSatisfiability(conditionFalsifiedStates, statesOfInterest);
			return conditionFalsifiedStates;
		}

		public BitSet findAcceptingStatesMax(LTLProduct<explicit.MDP> product)
				throws PrismException
		{
			return findAcceptingStatesMax(product, product.getProductModel().getReach().copy(), true);
		}

		/**
		 * [ REFS: <i>result</i>, DEREFS: <i>remain</i> ]
		 */
		public BitSet findAcceptingStatesMax(LTLProduct<explicit.MDP> product, BitSet remain, boolean alwaysRemain)
				throws PrismException
		{
			BitSet acceptingStates = getLtlTransformer().findAcceptingStates(product, remain, alwaysRemain);
			Until accept            = new Until(remain, acceptingStates, product.getProductModel());
			// States in remain from which some scheduler can enforce acceptance to maximize probability
			return computeProb1E(product.getProductModel(), accept);
		}

		@Override
		public ResetTransformation<explicit.MDP> transformReset(explicit.MDP model, BitSet resetStates, BitSet statesOfInterest)
				throws PrismException
		{
			return new ResetTransformer.MDP(this).transformModel(model, resetStates, statesOfInterest);
		}

		@Override
		public BitSet computeBadStates(explicit.MDP model, Until until, BitSet unsatisfiedStates)
		{
			BitSet maybeFalsified = computeProb0E(model, until);
			if (maybeFalsified.isEmpty()) {
				return maybeFalsified;
			}
			// FIXME ALG: check if we may alter the set if we use caching
			maybeFalsified.andNot(unsatisfiedStates);
			return maybeFalsified;
		}

		@Override
		public BitSet computeBadStates(LTLProduct<explicit.MDP> product, BitSet unsatisfiedStates)
				throws PrismException
		{
			// bad states == {s | Pmin=0[<> Condition]}
			explicit.MDP productModel                     = product.getProductModel();
			AcceptanceOmega conditionAcceptance           = product.getAcceptance();
			AcceptanceOmega conditionAcceptanceComplement = conditionAcceptance.complement(AcceptanceType.allTypes());
			BitSet maybeUnsatisfiedStates                 = getLtlTransformer().findAcceptingStates(productModel, conditionAcceptanceComplement);
//			// reduce number of choices, i.e.
//			// - reset only from r-states of streett acceptance
//			if (conditionAcceptance instanceof AcceptanceStreett) {
//				BitSet rStates = BitSetTools.union(new MappingIterator.From<>((AcceptanceStreett) conditionAcceptance, StreettPair::getR));
//				bad.and(rStates);
//			}
			BitSet maybeUnsatisfiedStatesProb1E = computeProb1E(productModel, null, maybeUnsatisfiedStates);
			maybeUnsatisfiedStatesProb1E.andNot(unsatisfiedStates);
			return maybeUnsatisfiedStatesProb1E;
		}

		@Override
		public ProbabilisticRedistribution redistributeProb1(explicit.MDP model, Until pathProb1, Until pathProbs)
				throws PrismException
		{
			BitSet states = computeProb1A(model, pathProb1);
			double[] probabilities;
			if (states.isEmpty()) {
				// FIXME ALG: check whether we use new double[0], new double[model.getNumState()] or null
				probabilities = new double[0];
			} else {
				probabilities = computeUntilMaxProbs(model, pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0(explicit.MDP model, Until pathProb0, Until pathProbs)
				throws PrismException
		{
			BitSet states = computeProb0A(model, pathProb0);
			double[] probabilities;
			if (states.isEmpty()) {
				// FIXME ALG: check whether we use new double[0], new double[model.getNumState()] or null
				probabilities = new double[0];
			} else {
				probabilities = computeUntilMinProbs(model, pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		/**
		 * Compute redistribution but use complementary path event for efficiency.
		 * Instead of Pmin(path) use 1-Pmax(not path).
		 */
		public ProbabilisticRedistribution redistributeProb0Complement(explicit.MDP model, Until pathProb0, Until compPathProbs)
				throws PrismException
		{
			BitSet states = computeProb0A(model, pathProb0);
			double[] probabilities;
			if (states.isEmpty()) {
				// FIXME ALG: check whether we use new double[0], new double[model.getNumState()] or null
				probabilities = new double[0];
			} else {
				probabilities = computeUntilMaxProbs(model, compPathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities).swap(model);
		}

		@Override
		public GoalFailStopOperator<explicit.MDP> configureOperator(explicit.MDP model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, BitSet instantGoalStates, BitSet instantFailStates, BitSet statesOfInterest)
				throws PrismException
		{
			return new GoalFailStopOperator.MDP(model, goalFail, goalStop, stopFail, instantGoalStates, instantFailStates, statesOfInterest, getLog());
		}
	}



	public class NormalFormTransformation<M extends Model> extends BasicModelExpressionTransformation<M, M>
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
