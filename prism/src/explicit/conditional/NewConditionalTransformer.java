package explicit.conditional;

import java.util.BitSet;

import explicit.DTMCModelChecker;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.PredecessorRelation;
import explicit.ProbModelChecker;
import explicit.StateModelChecker;
import explicit.conditional.SimplePathProperty.Until;
import explicit.conditional.transformer.LTLProductTransformer;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.ModelType;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.PrismComponent;

public interface NewConditionalTransformer<M extends Model, MC extends StateModelChecker>
{
	default String getName() {
		Class<?> type = this.getClass();
		type = type.getEnclosingClass() == null ? type : type.getEnclosingClass();
		return type.getSimpleName();
	}

	/**
	 * Test whether the transformer can handle a model and a conditional expression.
	 * 
	 * @return True iff this transformation type can handle the expression.
	 * @throws PrismLangException if the expression is broken
	 */
	@SuppressWarnings("unchecked")
	default boolean canHandle(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		return canHandleModelType(model)
		       && canHandleObjective((M) model, expression)
		       && canHandleCondition((M) model, expression);
	}

	boolean canHandleModelType(Model model);

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
		return oprel.getMinMax(model.getModelType()).isMax();
	}

	boolean canHandleCondition(M model,ExpressionConditional expression)
			throws PrismLangException;

	/**
	 * Throw an exception, iff the transformer cannot handle the model and expression.
	 */
	default void checkCanHandle(M model, ExpressionConditional expression) throws PrismException
	{
		if (! canHandle(model, expression)) {
			throw new PrismException("Cannot transform " + model.getModelType() + " for " + expression);
		}
	}

	ModelTransformation<M, M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest) throws PrismException;

	PrismLog getLog();

	MC getModelChecker();

	MC getModelChecker(M model) throws PrismException;

	LTLProductTransformer<M> getLtlTransformer();

	/**
	 * Subtract probabilities from one in-place.
	 * 
	 * @param probabilities
	 * @return argument array altered to hold result
	 */
	static double[] subtractFromOne(final double[] probabilities)
	{
		// FIXME ALG: code dupe in ConditionalReachabilityTransformer::negateProbabilities
		for (int state = 0; state < probabilities.length; state++) {
			probabilities[state] = 1 - probabilities[state];
		}
		return probabilities;
	}



	public static abstract class Basic<M extends Model, MC extends ProbModelChecker> extends PrismComponent implements NewConditionalTransformer<M, MC>
	{
		protected MC modelChecker;
		protected LTLProductTransformer<M> ltlTransformer;

		public Basic(MC modelChecker) {
			super(modelChecker);
			this.modelChecker = modelChecker;
		}

		@Override
		public MC getModelChecker()
		{
			return modelChecker;
		}

		@SuppressWarnings("unchecked")
		@Override
		public MC getModelChecker(M model) throws PrismException
		{
			// Create fresh model checker for model
			return (MC) modelChecker.createModelChecker(model.getModelType());
		}

		@Override
		public LTLProductTransformer<M> getLtlTransformer()
		{
			if (ltlTransformer == null) {
				ltlTransformer = new LTLProductTransformer<M>(modelChecker);
			}
			return ltlTransformer;
		}
	}

	public static abstract class DTMC extends Basic<explicit.DTMC, DTMCModelChecker>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.DTMC) && (model instanceof explicit.DTMC);
		}

		public BitSet computeProb0(explicit.DTMC model, BitSet remain, BitSet goal)
		{
			PredecessorRelation pre = model.getPredecessorRelation(modelChecker, true);
			return modelChecker.prob0(model, remain, goal, pre);
		}

		public BitSet computeProb1(explicit.DTMC model, BitSet remain, BitSet goal)
		{
			PredecessorRelation pre = model.getPredecessorRelation(modelChecker, true);
			return modelChecker.prob1(model, remain, goal, pre);
		}

		public BitSet computeProb0(explicit.DTMC model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0(model, until.getRemain(), until.getGoal());
			}
		}

		public BitSet computeProb1(explicit.DTMC model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1(model, until.getRemain(), until.getGoal());
			}
		}

		public double[] computeUntilProbs(explicit.DTMC model, Until until) throws PrismException
		{
			// FIXME ALG: consider precomputation
			double[] probabilities = modelChecker.computeUntilProbs(model, until.getRemain(), until.getGoal()).soln;
			if (until.isNegated()) {
				return subtractFromOne(probabilities);
			} else {
				return probabilities;
			}
		}

	}



	public static abstract class MDP extends Basic<explicit.MDP, MDPModelChecker>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.MDP) && (model instanceof explicit.MDP);
		}

		public BitSet computeProb0A(explicit.MDP model, BitSet remain, BitSet goal)
		{
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return modelChecker.prob0(model, remain, goal, false, null, pre);
		}

		public BitSet computeProb0E(explicit.MDP model, BitSet remain, BitSet goal)
		{
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return modelChecker.prob0(model, remain, goal, true, null, pre);
		}

		public BitSet computeProb1A(explicit.MDP model, BitSet remain, BitSet goal)
		{
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return modelChecker.prob1(model, remain, goal, true, null, pre);
		}

		public BitSet computeProb1E(explicit.MDP model, BitSet remain, BitSet goal)
		{
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return modelChecker.prob1(model, remain, goal, false, null, pre);
		}

		public BitSet computeProb0E(explicit.MDP model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1E(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0E(model, until.getRemain(), until.getGoal());
			}
		}

		public BitSet computeProb0A(explicit.MDP model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1A(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0A(model, until.getRemain(), until.getGoal());
			}
		}

		public BitSet computeProb1E(explicit.MDP model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0E(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1E(model, until.getRemain(), until.getGoal());
			}
		}

		public BitSet computeProb1A(explicit.MDP model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0A(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1A(model, until.getRemain(), until.getGoal());
			}
		}

		public double[] computeUntilMaxProbs(explicit.MDP model, Until until)
				throws PrismException
		{
			if (until.isNegated()) {
				// Pmax(¬φ) = 1 - Pmin(φ);
				double[] probabilities = modelChecker.computeUntilProbs(model, until.getRemain(), until.getGoal(), true).soln;
				return subtractFromOne(probabilities);
			} else {
				return modelChecker.computeUntilProbs(model, until.getRemain(), until.getGoal(), false).soln;
			}
		}

		public double[] computeUntilMinProbs(explicit.MDP model, Until until)
				throws PrismException
		{
			if (until.isNegated()) {
				// Pmin(¬φ) = 1 - Pmax(φ);
				double[] probabilities = modelChecker.computeUntilProbs(model, until.getRemain(), until.getGoal(), false).soln;
				return subtractFromOne(probabilities);
			} else {
				return  modelChecker.computeUntilProbs(model, until.getRemain(), until.getGoal(), true).soln;
			}
		}
	}
}
