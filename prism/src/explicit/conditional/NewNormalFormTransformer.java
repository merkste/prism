package explicit.conditional;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import acceptance.AcceptanceOmega;
import acceptance.AcceptanceType;
import common.BitSetTools;
import explicit.BasicModelExpressionTransformation;
import explicit.BasicModelTransformation;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ModelExpressionTransformation;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.StateModelChecker;
import explicit.conditional.NewGoalFailStopTransformer.ProbabilisticRedistribution;
import explicit.conditional.SimplePathProperty.Finally;
import explicit.conditional.SimplePathProperty.Reach;
import explicit.conditional.SimplePathProperty.Until;
import explicit.conditional.transformer.ResetTransformer;
import explicit.conditional.transformer.ResetTransformer.ResetTransformation;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.MDPRestricted;
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
		getLog().println("\nNormal-form transformation: " + normalFormTransformation.getTransformedExpression());

		// 2) Reset Transformation
		BitSet badStates                                        = computeResetStates(normalFormTransformation);
		BitSet transformedStatesOfInterest                      = normalFormTransformation.getTransformedStatesOfInterest();
		ModelTransformation<M, ? extends M> resetTransformation = transformReset(normalFormModel, badStates, transformedStatesOfInterest);

		// 3) Restrict to Reachable States
		ModelTransformation<M, ? extends M> restrictTransformation = transformRestrict(resetTransformation);

		// 4) Transform expression
		Expression originalExpression    = normalFormTransformation.getOriginalExpression();
		Expression transformedExpression = normalFormTransformation.getTransformedExpression().getObjective();

		// 5) Compose Transformations and Expressions
		ModelTransformationNested<M, M, ? extends M> nested = restrictTransformation.nest(resetTransformation).nest(normalFormTransformation);
		return new BasicModelExpressionTransformation<>(nested, originalExpression, transformedExpression);
	}

	NormalFormTransformation<M> transformNormalForm(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException;

	ModelTransformation<M, ? extends M> transformReset(M model, BitSet resetStates, BitSet statesOfInterest)
			throws PrismException;

	ModelTransformation<M, ? extends M> transformRestrict(ModelTransformation<M, ? extends M> resetTransformation);

	BitSet checkSatisfiability(Reach<M> conditionPath, BitSet statesOfInterest)
			throws UndefinedTransformationException;

	default BitSet computeResetStates(NormalFormTransformation<M> transformation)
	{
		BitSet badStates = transformation.getTransformedModel().getLabelStates(transformation.getBadLabel());
		BitSet failState = transformation.getTransformedModel().getLabelStates(transformation.getFailLabel());
		return BitSetTools.union(badStates, failState);
	}

	default BitSet checkSatisfiability(BitSet conditionUnsatisfied, BitSet statesOfInterest)
			throws UndefinedTransformationException
	{
		if (BitSetTools.isSubset(statesOfInterest, conditionUnsatisfied)) {
			throw new UndefinedTransformationException("Condition is not satisfiable");
		}
		return conditionUnsatisfied;
	}

	BitSet computeBadStates(Reach<M> reach, BitSet unsatisfiedStates);

	BitSet computeBadStates(LTLProduct<M> product, BitSet unsatisfiedStates)
			throws PrismException;

	ProbabilisticRedistribution redistributeProb1(Reach<M> pathProb1, Reach<M> pathProbs)
			throws PrismException;

	ProbabilisticRedistribution redistributeProb0(Reach<M> pathProb0, Reach<M> pathProbs)
			throws PrismException;

	NewGoalFailStopTransformer<M> getGoalFailStopTransformer();




	public static abstract class DTMC extends NewConditionalTransformer.DTMC implements NewNormalFormTransformer<explicit.DTMC, DTMCModelChecker>
	{

		public static final BitSet NO_STATES = new BitSet(0);

		protected Map<SimplePathProperty<explicit.DTMC>, double[]> cache;

		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
			cache = new HashMap<>();
		}

		/**
		 * Override to enable caching of probabilities.
		 *
		 * @see NewNormalFormTransformer#transform(Model, ExpressionConditional, BitSet)
		 */
		@Override
		public ModelExpressionTransformation<explicit.DTMC, explicit.DTMC> transform(explicit.DTMC model, ExpressionConditional expression, BitSet statesOfInterest)
				throws PrismException
		{
			ModelExpressionTransformation<explicit.DTMC, explicit.DTMC> result = NewNormalFormTransformer.super.transform(model, expression, statesOfInterest);
			clear();
			return result;
		}

		@Override
		public BitSet checkSatisfiability(Reach<explicit.DTMC> conditionPath, BitSet statesOfInterest)
				throws UndefinedTransformationException
		{
			BitSet conditionFalsifiedStates = computeProb0(conditionPath);
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
		public BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformRestrict(ModelTransformation<explicit.DTMC, ? extends explicit.DTMC> resetTransformation)
		{
			return DTMCRestricted.transform(resetTransformation.getTransformedModel(), resetTransformation.getTransformedStatesOfInterest());
		}

		@Override
		public BitSet computeBadStates(Reach<explicit.DTMC> reach, BitSet unsatisfiedStates)
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
		public ProbabilisticRedistribution redistributeProb1(Reach<explicit.DTMC> pathProb1, Reach<explicit.DTMC> pathProbs)
				throws PrismException
		{
			pathProb1.requireSameModel(pathProbs);

			BitSet states = computeProb1(pathProb1);
			double[] probabilities;
			if (states.isEmpty()) {
				// FIXME ALG: check whether we use new double[0], new double[model.getNumState()] or null
				probabilities = new double[0];
			} else {
				probabilities = computeProbs(pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0(Reach<explicit.DTMC> pathProb0, Reach<explicit.DTMC> pathProbs)
				throws PrismException
		{
			pathProb0.requireSameModel(pathProbs);

			BitSet states = computeProb0(pathProb0);
			double[] probabilities;
			if (states.isEmpty()) {
				// FIXME ALG: check whether we use new double[0], new double[model.getNumState()] or null
				probabilities = new double[0];
			} else {
				probabilities = computeProbs(pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		@Override
		public NewGoalFailStopTransformer<explicit.DTMC> getGoalFailStopTransformer()
		{
			return new NewGoalFailStopTransformer.DTMC();
		}

		public void clear()
		{
			cache.clear();
		}

		@Override
		public double[] computeProbs(Finally<explicit.DTMC> eventually)
				throws PrismException
		{
			if (! cache.containsKey(eventually)) {
				double[] probabilities = super.computeProbs(eventually);
				cache.put(eventually, probabilities);
			}
			return cache.get(eventually);
		}

		@Override
		public double[] computeProbs(Until<explicit.DTMC> until)
				throws PrismException
		{
			if (! cache.containsKey(until)) {
				double[] probabilities = super.computeProbs(until);
				cache.put(until, probabilities);
			}
			return cache.get(until);
		}
	}



	public static abstract class MDP extends NewConditionalTransformer.MDP implements NewNormalFormTransformer<explicit.MDP, MDPModelChecker>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public BitSet checkSatisfiability(Reach<explicit.MDP> conditionPath, BitSet statesOfInterest)
				throws UndefinedTransformationException
		{
			BitSet conditionFalsifiedStates = computeProb0A(conditionPath);
			checkSatisfiability(conditionFalsifiedStates, statesOfInterest);
			return conditionFalsifiedStates;
		}

		@Override
		public ResetTransformation<explicit.MDP> transformReset(explicit.MDP model, BitSet resetStates, BitSet statesOfInterest)
				throws PrismException
		{
			return new ResetTransformer.MDP(this).transformModel(model, resetStates, statesOfInterest);
		}

		@Override
		public BasicModelTransformation<explicit.MDP, ? extends explicit.MDP> transformRestrict(ModelTransformation<explicit.MDP, ? extends explicit.MDP> resetTransformation)
		{
			return MDPRestricted.transform(resetTransformation.getTransformedModel(), resetTransformation.getTransformedStatesOfInterest());
		}

		@Override
		public BitSet computeBadStates(Reach<explicit.MDP> reach, BitSet unsatisfiedStates)
		{
			BitSet maybeFalsified = computeProb0E(reach);
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
			AcceptanceOmega conditionAcceptanceComplement = conditionAcceptance.complement(productModel.getNumStates(), AcceptanceType.allTypes());
			BitSet maybeUnsatisfiedStates                 = getLtlTransformer().findAcceptingStates(productModel, conditionAcceptanceComplement);
//			// reduce number of choices, i.e.
//			// - reset only from r-states of streett acceptance
//			if (conditionAcceptance instanceof AcceptanceStreett) {
//				BitSet rStates = BitSetTools.union(new MappingIterator.From<>((AcceptanceStreett) conditionAcceptance, StreettPair::getR));
//				bad.and(rStates);
//			}
			BitSet maybeUnsatisfiedStatesProb1E = computeProb1E(new Finally<>(productModel, maybeUnsatisfiedStates));
			maybeUnsatisfiedStatesProb1E.andNot(unsatisfiedStates);
			return maybeUnsatisfiedStatesProb1E;
		}

		@Override
		public ProbabilisticRedistribution redistributeProb1(Reach<explicit.MDP> pathProb1, Reach<explicit.MDP> pathProbs)
				throws PrismException
		{
			pathProb1.requireSameModel(pathProbs);

			BitSet states = computeProb1A(pathProb1);
			double[] probabilities;
			if (states.isEmpty()) {
				// FIXME ALG: check whether we use new double[0], new double[model.getNumState()] or null
				probabilities = new double[0];
			} else {
				probabilities = computeMaxProbs(pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0(Reach<explicit.MDP> pathProb0, Reach<explicit.MDP> pathProbs)
				throws PrismException
		{
			pathProb0.requireSameModel(pathProbs);

			BitSet states = computeProb0A(pathProb0);
			double[] probabilities;
			if (states.isEmpty()) {
				// FIXME ALG: check whether we use new double[0], new double[model.getNumState()] or null
				probabilities = new double[0];
			} else {
				probabilities = computeMinProbs(pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		/**
		 * Compute redistribution but use complementary path event for efficiency.
		 * Instead of Pmin(path) use 1-Pmax(not path).
		 */
		public ProbabilisticRedistribution redistributeProb0Complement(Reach<explicit.MDP> pathProb0, Reach<explicit.MDP> compPathProbs)
				throws PrismException
		{
			pathProb0.requireSameModel(compPathProbs);

			BitSet states = computeProb0A(pathProb0);
			double[] probabilities;
			if (states.isEmpty()) {
				// FIXME ALG: check whether we use new double[0], new double[model.getNumState()] or null
				probabilities = new double[0];
			} else {
				probabilities = computeMaxProbs(compPathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities).swap();
		}

		@Override
		public NewGoalFailStopTransformer<explicit.MDP> getGoalFailStopTransformer()
		{
			return new NewGoalFailStopTransformer.MDP();
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
