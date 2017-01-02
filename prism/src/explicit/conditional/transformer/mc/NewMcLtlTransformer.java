package explicit.conditional.transformer.mc;

import java.util.BitSet;
import java.util.PrimitiveIterator.OfInt;

import acceptance.AcceptanceType;
import common.BitSetTools;
import common.iterable.IterableArray;
import common.iterable.IterableBitSet;
import explicit.BasicModelTransformation;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.PredecessorRelation;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.LTLProductTransformer;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.modelviews.DTMCEquiv;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.EquivalenceRelationInteger;
import explicit.modelviews.Restriction;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;

public class NewMcLtlTransformer extends MCConditionalTransformer
{
	public static final AcceptanceType[] ACCEPTANCE_TYPES = AcceptanceType.allTypes();

	protected final LTLProductTransformer<explicit.DTMC> ltlTransformer;

	public NewMcLtlTransformer(final DTMCModelChecker modelChecker) throws PrismException
	{
		super(modelChecker);
		ltlTransformer = new LTLProductTransformer<explicit.DTMC>(modelChecker);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		return ltlTransformer.canHandle(model, expression.getCondition());
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression)
	{
		// cannot handle steady state computation yet
		return !(ExpressionInspector.isSteadyStateReward(expression.getObjective()));
	}

	@Override
	protected ModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformModel(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		Expression condition = expression.getCondition();

		// Build product model
		LTLProduct<explicit.DTMC> product = ltlTransformer.transform(model, condition, statesOfInterest, ACCEPTANCE_TYPES);
		explicit.DTMC productModel        = product.getProductModel();
		BitSet goal              = ltlTransformer.findAcceptingStates(product);

		BitSet prob0                       = computeProb0(productModel, null, goal, false);
		BitSet support                     = BitSetTools.complement(productModel.getNumStates(), prob0);
		BitSet transformedStatesOfInterest = BitSetTools.intersect(product.getTransformedStatesOfInterest(), support);
		if (transformedStatesOfInterest.isEmpty()) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		BitSet prob1                       = computeProb1(productModel, null, goal, false);
		double[] probs                     = computeUntilProbs(productModel, null, goal, false, prob0, prob1);

		BitSet restrict;
		BasicModelTransformation<explicit.DTMC,? extends explicit.DTMC> scaled;
		if (settings.getBoolean(PrismSettings.CONDITIONAL_SCALE_LTL_MINIMIZE)) {
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
			scaled = McScaledTransformation.transform(productModel, probs);
			scaled = DTMCEquiv.transform(scaled.getTransformedModel(), equivalence, false).compose(scaled);
		} else {
			// Compute reachable states
			restrict = support;

			// Scale probabilities
			scaled = McScaledTransformation.transform(productModel, probs);
		}
		scaled.setTransformedStatesOfInterest(transformedStatesOfInterest);

		// Restrict to reachable states
		BasicModelTransformation<explicit.DTMC, DTMCRestricted> restricted = DTMCRestricted.transform(scaled.getTransformedModel(), restrict, Restriction.TRANSITIVE_CLOSURE_SAFE);
		restricted.setTransformedStatesOfInterest(restricted.mapToTransformedModel(transformedStatesOfInterest));

		return restricted.compose(scaled).compose(product);
	}

	public EquivalenceRelationInteger computeEquivalence(LTLProduct<explicit.DTMC> product, BitSet states)
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

	public BitSet computeProb0(final explicit.DTMC model, final BitSet remain, final BitSet goal, final boolean negated) throws PrismException
	{
		PredecessorRelation pre = model.getPredecessorRelation(modelChecker, true);
		if (negated) {
			return modelChecker.prob1(model, remain, goal, pre);
		} else {
			return modelChecker.prob0(model, remain, goal, pre);
		}
	}

	public BitSet computeProb1(final explicit.DTMC model, final BitSet remain, final BitSet goal, final boolean negated) throws PrismException
	{
		PredecessorRelation pre = model.getPredecessorRelation(modelChecker, true);
		if (negated) {
			return modelChecker.prob0(model, remain, goal, pre);
		} else {
			return modelChecker.prob1(model, remain, goal, pre);
		}
	}

	public double[] computeUntilProbs(final explicit.DTMC model, final BitSet remain, final BitSet goal, final boolean negated, final BitSet prob0, final BitSet prob1)
			throws PrismException
	{
		double[] init = new double[model.getNumStates()]; // initialized with 0.0's
		BitSet setToOne = negated ? prob0 : prob1;
		for (OfInt iter = new IterableBitSet(setToOne).iterator(); iter.hasNext();) {
			init[iter.nextInt()] = 1.0;
		}
		BitSet known = BitSetTools.union(prob0, prob1);
		double[] probabilities = modelChecker.computeReachProbs(model, remain, goal, init, known).soln;
		return negated ? negateProbabilities(probabilities) : probabilities;
	}


	public static double[] negateProbabilities(final double[] probabilities)
	{
		for (int state = 0; state < probabilities.length; state++) {
			probabilities[state] = 1 - probabilities[state];
		}
		return probabilities;
	}
}