package explicit.conditional.scale;

import java.util.BitSet;
import java.util.PrimitiveIterator.OfInt;

import acceptance.AcceptanceType;
import common.BitSetTools;
import common.IterableBitSet;
import common.iterable.IterableArray;
import explicit.BasicModelTransformation;
import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ProbModelChecker;
import explicit.conditional.ConditionalTransformer;
import explicit.conditional.transformer.LtlProductTransformer;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.modelviews.CTMCEquiv;
import explicit.modelviews.DTMCEquiv;
import explicit.modelviews.EquivalenceRelationInteger;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;

//FIXME ALG: add comment
public interface MCLtlTransformer<M extends explicit.DTMC,C extends ProbModelChecker> extends ScaleTransformer<M,C>
{
	public static final AcceptanceType[] ACCEPTANCE_TYPES = AcceptanceType.allTypes();

	default LtlProductTransformer<M> getLtlTransformer()
	{
		return new LtlProductTransformer<M>(getModelChecker());
	}

	@Override
	default boolean canHandleCondition(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		return getLtlTransformer().canHandle(model, expression.getCondition());
	}

	@Override
	default boolean canHandleObjective(final Model model, final ExpressionConditional expression)
	{
		// Can handle all ExpressionQuant: P, R, S and L
		return true;
	}

	@Override
	default ModelTransformation<M, ? extends M> transformModel(final M model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		Expression condition = expression.getCondition();

		// Build product model
		LtlProductTransformer<M> ltlTransformer = getLtlTransformer();
		LTLProduct<M> product = ltlTransformer.transform(model, condition, statesOfInterest, ACCEPTANCE_TYPES);
		M productModel        = product.getProductModel();
		BitSet goal           = ltlTransformer.findAcceptingStates(product);

		BitSet prob0                       = getMcModelChecker().computeProb0(productModel, false, null, goal);
		BitSet support                     = BitSetTools.complement(productModel.getNumStates(), prob0);
		BitSet transformedStatesOfInterest = BitSetTools.intersect(product.getTransformedStatesOfInterest(), support);
		if (transformedStatesOfInterest.isEmpty()) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		BitSet prob1                       = getMcModelChecker().computeProb1(productModel, false, null, goal);
		double[] probs                     = getMcModelChecker().computeUntilProbs(productModel, false, null, goal, prob0, prob1);

		BitSet restrict;
		BasicModelTransformation<M,? extends M> scaled;
		if (getSettings().getBoolean(PrismSettings.CONDITIONAL_SCALE_LTL_MINIMIZE)) {
			// Compute equivalence relation and adapt probs
			EquivalenceRelationInteger equivalence = computeEquivalence(product, prob1);
			for (int state : new IterableBitSet(equivalence.getNonRepresentatives())) {
				probs[state] = 0.0;
			}

			// Ensure no states of interest got lost
			if (transformedStatesOfInterest.intersects(equivalence.getNonRepresentatives())) {
				throw new RuntimeException("States of interest must representatives.");
			}

			// Compute reachable states
			restrict = BitSetTools.minus(support, equivalence.getNonRepresentatives());

			// Scale probabilities and build quotient DTMC
			scaled = scale(productModel, probs);
			scaled = quotient(scaled, equivalence).compose(scaled);
		} else {
			// Compute reachable states
			restrict = support;

			// Scale probabilities
			scaled = scale(productModel, probs);
		}
		scaled.setTransformedStatesOfInterest(transformedStatesOfInterest);

		// Restrict to reachable states
		BasicModelTransformation<M, ? extends M> restricted = restrict(scaled, restrict);
		restricted.setTransformedStatesOfInterest(restricted.mapToTransformedModel(transformedStatesOfInterest));

		return restricted.compose(scaled).compose(product);
	}

	default EquivalenceRelationInteger computeEquivalence(LTLProduct<M> product, BitSet states)
	{
		// (s,a) ~ (t,b) iff (s == t) && states(s,a) && states(t,b)
		Object[] equivalenceClasses = new Object[product.getOriginalModel().getNumStates()];
		for (OfInt iterProb1 = new IterableBitSet(states).iterator(); iterProb1.hasNext();) {
			int productState = iterProb1.nextInt();
			int modelState   = product.getModelState(productState);
			Object eqClass = equivalenceClasses[modelState];
			if (eqClass == null) {
				equivalenceClasses[modelState] = productState;
			} else if (eqClass instanceof Integer) {
				eqClass = BitSetTools.asBitSet(productState);
			} else {
				((BitSet) eqClass).set(modelState);
			}
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Iterable<BitSet> classes = (Iterable) new IterableArray.Of<>(equivalenceClasses).filter(set -> (set instanceof BitSet));
		return new EquivalenceRelationInteger(classes);
	}

	BasicModelTransformation<M, ? extends M> quotient(BasicModelTransformation<M, ? extends M> scaled, EquivalenceRelationInteger equivalence);



	public static class CTMC extends ConditionalTransformer.Basic<explicit.CTMC, CTMCModelChecker> implements MCLtlTransformer<explicit.CTMC, CTMCModelChecker>, ScaleTransformer.CTMC
	{
		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public BasicModelTransformation<explicit.CTMC, ? extends explicit.CTMC> quotient(BasicModelTransformation<explicit.CTMC, ? extends explicit.CTMC> scaled, EquivalenceRelationInteger equivalence)
		{
			return CTMCEquiv.transform(scaled.getTransformedModel(), equivalence, false);
		}
	}



	public static class DTMC extends ConditionalTransformer.Basic<explicit.DTMC, DTMCModelChecker> implements MCLtlTransformer<explicit.DTMC, DTMCModelChecker>, ScaleTransformer.DTMC
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> quotient(BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> scaled, EquivalenceRelationInteger equivalence)
		{
			return DTMCEquiv.transform(scaled.getTransformedModel(), equivalence, false);
		}
	}
}
