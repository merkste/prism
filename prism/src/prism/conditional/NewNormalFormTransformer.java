package prism.conditional;

import java.util.HashMap;
import java.util.Map;

import acceptance.AcceptanceOmegaDD;
import acceptance.AcceptanceType;
import explicit.conditional.transformer.UndefinedTransformationException;
import jdd.Clearable;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.ModelExpressionTransformation;
import prism.ModelTransformation;
import prism.ModelTransformationNested;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.PrismException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.LTLModelChecker.LTLProduct;
import prism.conditional.SimplePathProperty.Finally;
import prism.conditional.SimplePathProperty.Reach;
import prism.conditional.SimplePathProperty.Until;
import prism.conditional.transform.BasicModelExpressionTransformation;
import prism.conditional.transform.GoalFailStopTransformation;
import prism.conditional.transform.NewMCResetTransformation;
import prism.conditional.transform.NewMDPResetTransformation;
import prism.conditional.transform.GoalFailStopTransformation.GoalFailStopOperator;
import prism.conditional.transform.GoalFailStopTransformation.ProbabilisticRedistribution;

// FIXME ALG: add comment
public interface NewNormalFormTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewConditionalTransformer<M, MC>, Clearable
{
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

	GoalFailStopTransformation<M> transformGoalFailStop(M model, ProbabilisticRedistribution objectiveSatisfied, ProbabilisticRedistribution conditionSatisfied, ProbabilisticRedistribution objectiveFalsified, JDDNode instantGoalStates, JDDNode conditionFalsifiedStates, JDDNode badStates, JDDNode statesOfInterest)
			throws PrismException;

	// FIXME ALG: Leak. Ensure clear is called after transformation even if an exception occurs
	// FIXME ALG: Check Leakage in explicit implementation
	@Override
	default void clear()
	{
		// No-Op except caching for mc results is implemented
	}



	public static abstract class DTMC extends NewConditionalTransformer.DTMC implements NewNormalFormTransformer<ProbModel, ProbModelChecker>
	{
		protected Map<SimplePathProperty<ProbModel>, JDDNode> cache;

		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
			cache = new HashMap<>();
		}

		/**
		 * Override to enable caching of probabilities.
		 *
		 * @see NewNormalFormTransformer#transform(ProbModel, ExpressionConditional, JDDNode)
		 */
		@Override
		public ModelExpressionTransformation<ProbModel, ProbModel> transform(ProbModel model, ExpressionConditional expression, JDDNode statesOfInterest)
				throws PrismException
		{
			ModelExpressionTransformation<ProbModel,ProbModel> result = NewNormalFormTransformer.super.transform(model, expression, statesOfInterest);
			clear();
			return result;
		}

		@Override
		public JDDNode checkSatisfiability(Reach<ProbModel> conditionPath, JDDNode statesOfInterest)
				throws PrismException
		{
			JDDNode conditionFalsifiedStates = computeProb0(conditionPath);
			checkSatisfiability(conditionFalsifiedStates, statesOfInterest);
			return conditionFalsifiedStates;
		}

		@Override
		public NewMCResetTransformation transformReset(ProbModel model, JDDNode resetStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new NewMCResetTransformation(model, resetStates, statesOfInterest);
		}

		@Override
		public JDDNode computeBadStates(Reach<ProbModel> reach, JDDNode unsatisfiedStates)
		{
			// DTMCs are purely probabilistic
			return noStates();
		}

		@Override
		public JDDNode computeBadStates(LTLProduct<ProbModel> product, JDDNode unsatisfiedStates)
		{
			// DTMCs are purely probabilistic
			return noStates();
		}

		@Override
		public ProbabilisticRedistribution redistributeProb1(Reach<ProbModel> pathProb1, Reach<ProbModel> pathProbs)
				throws PrismException
		{
			pathProb1.requireSameModel(pathProbs);

			JDDNode states = computeProb1(pathProb1);
			JDDNode probabilities;
			if (states.equals(JDD.ZERO)) {
				probabilities = JDD.Constant(0);
			} else {
				probabilities = computeProbs(pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0(Reach<ProbModel> pathProb0, Reach<ProbModel> pathProbs)
				throws PrismException
		{
			pathProb0.requireSameModel(pathProbs);

			JDDNode states = computeProb0(pathProb0);
			JDDNode probabilities;
			if (states.equals(JDD.ZERO)) {
				probabilities = JDD.Constant(0);
			} else {
				probabilities = computeProbs(pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		@Override
		public GoalFailStopTransformation<ProbModel> transformGoalFailStop(ProbModel model, ProbabilisticRedistribution objectiveSatisfied, ProbabilisticRedistribution conditionSatisfied, ProbabilisticRedistribution objectiveFalsified, JDDNode instantGoalStates, JDDNode conditionFalsifiedStates, JDDNode badStates, JDDNode statesOfInterest)
				throws PrismException
		{
			GoalFailStopOperator<ProbModel> operator = new GoalFailStopOperator.DTMC(model, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, statesOfInterest, getLog());
			return new GoalFailStopTransformation<>(model, operator, badStates);
		}

		@Override
		public void clear()
		{
			cache.keySet().forEach(SimplePathProperty::clear);
			cache.values().forEach(JDD::Deref);
			cache.clear();
		}

		@Override
		public JDDNode computeProbs(Finally<ProbModel> eventually)
				throws PrismException
		{
			if (! cache.containsKey(eventually)) {
				JDDNode probabilities = super.computeProbs(eventually);
				cache.put(eventually.clone(), probabilities);
			}
			return cache.get(eventually).copy();
		}

		@Override
		public JDDNode computeProbs(Until<ProbModel> until)
				throws PrismException
		{
			if (! cache.containsKey(until)) {
				JDDNode probabilities = super.computeProbs(until);
				cache.put(until.clone(), probabilities);
			}
			return cache.get(until).copy();
		}
	}



	public static abstract class MDP extends NewConditionalTransformer.MDP implements NewNormalFormTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public JDDNode checkSatisfiability(Reach<NondetModel> conditionPath, JDDNode statesOfInterest)
				throws PrismException
		{
			JDDNode conditionFalsifiedStates = computeProb0A(conditionPath);
			checkSatisfiability(conditionFalsifiedStates, statesOfInterest);
			return conditionFalsifiedStates;
		}

		@Override
		public ModelTransformation<NondetModel, ? extends NondetModel> transformReset(NondetModel model, JDDNode resetStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new NewMDPResetTransformation(model, resetStates, statesOfInterest);
		}

		@Override
		public JDDNode computeBadStates(Reach<NondetModel> reach, JDDNode unsatisfiedStates)
				throws PrismException
		{
			JDDNode maybeFalsified = computeProb0E(reach);
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
			JDDNode maybeUnsatisfiedStatesProb1E = computeProb1E(productModel, false, ALL_STATES, maybeUnsatisfiedStates);
			JDD.Deref(maybeUnsatisfiedStates);
			return JDD.And(maybeUnsatisfiedStatesProb1E, JDD.Not(unsatisfiedStates.copy()));
		}

		@Override
		public ProbabilisticRedistribution redistributeProb1(Reach<NondetModel> pathProb1, Reach<NondetModel> pathProbs)
				throws PrismException
		{
			pathProb1.requireSameModel(pathProbs);

			JDDNode states = computeProb1A(pathProb1);
			JDDNode probabilities;
			if (states.equals(JDD.ZERO)) {
				probabilities = JDD.Constant(0);
			} else {
				probabilities = computeMaxProbs(pathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities);
		}

		@Override
		public ProbabilisticRedistribution redistributeProb0(Reach<NondetModel> pathProb0, Reach<NondetModel> pathProbs)
				throws PrismException
		{
			pathProb0.requireSameModel(pathProbs);

			JDDNode states = computeProb0A(pathProb0);
			JDDNode probabilities;
			if (states.equals(JDD.ZERO)) {
				probabilities = JDD.Constant(0);
			} else {
				probabilities = computeMinProbs(pathProbs);
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

			JDDNode states = computeProb0A(pathProb0);
			JDDNode probabilities;
			if (states.equals(JDD.ZERO)) {
				probabilities = JDD.Constant(0);
			} else {
				probabilities = computeMaxProbs(compPathProbs);
			}
			return new ProbabilisticRedistribution(states, probabilities).swap(pathProb0.getModel());
		}

		@Override
		public GoalFailStopTransformation<NondetModel> transformGoalFailStop(NondetModel model, ProbabilisticRedistribution objectiveSatisfied, ProbabilisticRedistribution conditionSatisfied, ProbabilisticRedistribution objectiveFalsified, JDDNode instantGoalStates, JDDNode conditionFalsifiedStates, JDDNode badStates, JDDNode statesOfInterest)
				throws PrismException
		{
			GoalFailStopOperator<NondetModel> operator = new GoalFailStopOperator.MDP(model, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, statesOfInterest, getLog());
			return new GoalFailStopTransformation<>(model, operator, badStates);
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
