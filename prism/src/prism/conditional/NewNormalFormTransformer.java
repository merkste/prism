package prism.conditional;

import acceptance.AcceptanceOmegaDD;
import acceptance.AcceptanceType;
import explicit.conditional.transformer.UndefinedTransformationException;
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
import prism.conditional.SimplePathProperty.Until;
import prism.conditional.transform.BasicModelExpressionTransformation;
import prism.conditional.transform.NewMCResetTransformation;
import prism.conditional.transform.NewMDPResetTransformation;
import prism.conditional.transform.GoalFailStopTransformation.GoalFailStopOperator;
import prism.conditional.transform.GoalFailStopTransformation.ProbabilisticRedistribution;

// FIXME ALG: add comment
public interface NewNormalFormTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewConditionalTransformer<M, MC>
{
	@Override
	default ModelExpressionTransformation<M, M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		// 1) Normal-Form Transformation
		NormalFormTransformation<M> normalFormTransformation = transformNormalForm(model, expression, statesOfInterest);
		M normalFormModel                                    = normalFormTransformation.getTransformedModel();
		getLog().println("Normal-form transformation: " + normalFormTransformation.getTransformedExpression());

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

	default JDDNode computeResetStates(NormalFormTransformation<M> transformation)
	{
		JDDNode badStates = transformation.getTransformedModel().getLabelDD(transformation.getBadLabel());
		JDDNode failState = transformation.getTransformedModel().getLabelDD(transformation.getFailLabel());
		return JDD.Or(badStates.copy(), failState.copy());
	}

	ModelTransformation<M, ? extends M> transformReset(M model, JDDNode resetStates, JDDNode statesOfInterest)
			throws PrismException;

	default JDDNode checkSatisfiability(JDDNode conditionUnsatisfied, JDDNode statesOfInterest)
			throws UndefinedTransformationException
	{
		if (JDD.IsContainedIn(statesOfInterest, conditionUnsatisfied)) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		return conditionUnsatisfied;
	}

//	default JDDNode findAcceptingStatesMax(LTLProduct<M> product)
//			throws PrismException
//	{
//		return findAcceptingStatesMax(product, product.getProductModel().getReach().copy());
//	}
//
//	/**
//	 * Compute all states in remain from which acceptance can be guaranteed
//	 */
//	JDDNode findAcceptingStatesMax(LTLProduct<M> product, JDDNode remain)
//			throws PrismException;

//	JDDNode computeProb0A(M model, Until until);
//
//	JDDNode computeProb0E(M model, Until until);
//
//	JDDNode computeProb1A(M model, Until until);
//
//	JDDNode computeProb1E(M model, Until until);

	JDDNode computeBadStates(M model, Until until, JDDNode unsatisfiedStates);

	JDDNode computeBadStates(LTLProduct<M> product, JDDNode unsatisfiedStates) throws PrismException;

	GoalFailStopOperator<M> configureOperator(M model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, JDDNode instantGoalStates, JDDNode instantFailStates, JDDNode statesOfInterest)
			throws PrismException;



	public static abstract class DTMC extends NewConditionalTransformer.DTMC implements NewNormalFormTransformer<ProbModel, ProbModelChecker>
	{

		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

//		public JDDNode findAcceptingStatesMax(LTLProduct<ProbModel> product, JDDNode remain)
//				throws PrismException
//		{
//			Until accept            = new Until(remain, getLtlTransformer().findAcceptingStates(product, remain));
//			// States in remain that guarantee acceptance
//			JDDNode acceptingStates = computeProb1(product.getProductModel(), accept);
//			accept.clear();
//			return acceptingStates;
//		}

//		public JDDNode computeProb0A(ProbModel model, Until until)
//		{
//			return computeProb0(model, until);
//		}
//
//		public JDDNode computeProb0E(ProbModel model, Until until)
//		{
//			return computeProb0(model, until);
//		}
//
//		public JDDNode computeProb1A(ProbModel model, Until until)
//		{
//			return computeProb1(model, until);
//		}
//
//		public JDDNode computeProb1E(ProbModel model, Until until)
//		{
//			return computeProb1(model, until);
//		}

		@Override
		public NewMCResetTransformation transformReset(ProbModel model, JDDNode resetStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new NewMCResetTransformation(model, resetStates, statesOfInterest);
		}

		@Override
		public JDDNode computeBadStates(ProbModel model, Until until, JDDNode unsatisfiedStates)
		{
			// DTMCs are purely probabilistic
			return JDD.Constant(0);
		}

		@Override
		public JDDNode computeBadStates(LTLProduct<ProbModel> product, JDDNode unsatisfiedStates)
		{
			// DTMCs are purely probabilistic
			return JDD.Constant(0);
		}

		@Override
		public GoalFailStopOperator<ProbModel> configureOperator(ProbModel model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, JDDNode instantGoalStates, JDDNode instantFailStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new GoalFailStopOperator.DTMC(model, goalFail, goalStop, stopFail, instantGoalStates, instantFailStates, statesOfInterest, getLog());
		}
	}



	public static abstract class MDP extends NewConditionalTransformer.MDP implements NewNormalFormTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		public JDDNode findAcceptingStatesMax(LTLProduct<NondetModel> product)
				throws PrismException
		{
			return findAcceptingStatesMax(product, product.getProductModel().getReach().copy(), true);
		}

		/**
		 * [ REFS: <i>result</i>, DEREFS: <i>remain</i> ]
		 */
		public JDDNode findAcceptingStatesMax(LTLProduct<NondetModel> product, JDDNode remain, boolean alwaysRemain)
				throws PrismException
		{
			JDDNode acceptingStates = getLtlTransformer().findAcceptingStates(product, remain, alwaysRemain);
			Until accept            = new Until(remain, acceptingStates);
			// States in remain from which some scheduler can enforce acceptance to maximize probability
			JDDNode acceptingStatesMax = computeProb1E(product.getProductModel(), accept);
			accept.clear();
			return acceptingStatesMax;
		}

		@Override
		public ModelTransformation<NondetModel, ? extends NondetModel> transformReset(NondetModel model, JDDNode resetStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new NewMDPResetTransformation(model, resetStates, statesOfInterest);
		}

		@Override
		public JDDNode computeBadStates(NondetModel model, Until until, JDDNode unsatisfiedStates)
		{
			JDDNode maybeFalsified = computeProb0E(model, until);
			if (maybeFalsified.equals(JDD.ZERO)) {
				return maybeFalsified;
			}
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
			JDDNode maybeUnsatisfiedStatesProb1E = computeProb1E(productModel, null, maybeUnsatisfiedStates);
			JDD.Deref(maybeUnsatisfiedStates);
			return JDD.And(maybeUnsatisfiedStatesProb1E, JDD.Not(unsatisfiedStates.copy()));
		}

		@Override
		public GoalFailStopOperator<NondetModel> configureOperator(NondetModel model, ProbabilisticRedistribution goalFail, ProbabilisticRedistribution goalStop, ProbabilisticRedistribution stopFail, JDDNode instantGoalStates, JDDNode instantFailStates, JDDNode statesOfInterest)
				throws PrismException
		{
			return new GoalFailStopOperator.MDP(model, goalFail, goalStop, stopFail, instantGoalStates, instantFailStates, statesOfInterest, getLog());
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

		public ExpressionConditional getOriginalExpression()
		{
			return (ExpressionConditional) originalExpression;
		}

		public ExpressionConditional getTransformedExpression()
		{
			return (ExpressionConditional) transformedExpression;
		}
	}
}
