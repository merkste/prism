package explicit.conditional;

import java.util.BitSet;
import java.util.Objects;

import explicit.DTMCModelChecker;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.ModelExpressionTransformation;
import explicit.PredecessorRelation;
import explicit.ProbModelChecker;
import explicit.StateModelChecker;
import explicit.conditional.SimplePathProperty.Finally;
import explicit.conditional.SimplePathProperty.Globally;
import explicit.conditional.SimplePathProperty.Next;
import explicit.conditional.SimplePathProperty.Reach;
import explicit.conditional.SimplePathProperty.Release;
import explicit.conditional.SimplePathProperty.TemporalOperator;
import explicit.conditional.SimplePathProperty.Until;
import explicit.conditional.SimplePathProperty.WeakUntil;
import explicit.conditional.transformer.LTLProductTransformer;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.PrismComponent;

public interface NewConditionalTransformer<M extends Model, MC extends StateModelChecker>
{
	public static final BitSet ALL_STATES = null;

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
	default boolean canHandle(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		return canHandleModelType(model)
		       && canHandleObjective(model, expression)
		       && canHandleCondition(model, expression);
	}

	boolean canHandleModelType(Model model);

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

	boolean canHandleCondition(Model model,ExpressionConditional expression)
			throws PrismLangException;

	/**
	 * Throw an exception, iff the transformer cannot handle the model and expression.
	 */
	default void checkCanHandle(Model model, ExpressionConditional expression) throws PrismException
	{
		if (! canHandle(model, expression)) {
			throw new PrismException("Cannot transform " + model.getModelType() + " for " + expression);
		}
	}

	ModelExpressionTransformation<M, ? extends M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest) throws PrismException;

	PrismLog getLog();

	MC getModelChecker();

	LTLProductTransformer<M> getLtlTransformer();

	default SimplePathProperty<M> computeSimplePathProperty(M model, Expression expression)
			throws PrismException
	{
		Expression trimmed = ExpressionInspector.trimUnaryOperations(expression);

		if (!trimmed.isSimplePathFormula() || Expression.containsTemporalTimeBounds(trimmed) || Expression.containsTemporalRewardBounds(trimmed))
		{
			throw new IllegalArgumentException("expected unbounded simple path formula");
		}

		boolean negated = Expression.isNot(trimmed);
		ExpressionTemporal temporal;
		if (negated) {
			temporal = (ExpressionTemporal) ExpressionInspector.removeNegation(trimmed);
		} else {
			temporal = (ExpressionTemporal) trimmed;
		}

		MC mc                     = getModelChecker();
		TemporalOperator operator = TemporalOperator.fromConstant(temporal.getOperator());
		BitSet goal, remain, stop;
		switch (operator) {
		case Next:
			goal = mc.checkExpression(model, temporal.getOperand2(), ALL_STATES).getBitSet();
			return new Next<>(model, negated, goal);
		case Finally:
			goal = mc.checkExpression(model, temporal.getOperand2(), ALL_STATES).getBitSet();
			return new Finally<>(model, negated, goal);
		case Globally:
			remain = mc.checkExpression(model, temporal.getOperand2(), ALL_STATES).getBitSet();
			return new Globally<>(model, negated, remain);
		case Until:
			remain = mc.checkExpression(model, temporal.getOperand1(), ALL_STATES).getBitSet();
			goal   = mc.checkExpression(model, temporal.getOperand2(), ALL_STATES).getBitSet();
			return new Until<>(model, negated, remain, goal);
		case Release:
			stop   = mc.checkExpression(model, temporal.getOperand1(), ALL_STATES).getBitSet();
			remain = mc.checkExpression(model, temporal.getOperand2(), ALL_STATES).getBitSet();
			return new Release<>(model, negated, stop, remain);
		case WeakUntil:
			remain = mc.checkExpression(model, temporal.getOperand1(), ALL_STATES).getBitSet();
			goal   = mc.checkExpression(model, temporal.getOperand2(), ALL_STATES).getBitSet();
			return new WeakUntil<>(model, negated, remain, goal);
		default:
			throw new IllegalArgumentException("unsupported temporal operator arity");
		}
	}

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
			return model instanceof explicit.DTMC;
		}

		public BitSet computeProb0(explicit.DTMC model, boolean negated, BitSet remain, BitSet goal)
		{
			if (negated) {
				return computeProb1(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			PredecessorRelation pre = model.getPredecessorRelation(modelChecker, true);
			return modelChecker.prob0(model, remain, goal, pre);
		}

		public BitSet computeProb1(explicit.DTMC model, boolean negated, BitSet remain, BitSet goal)
		{
			if (negated) {
				return computeProb0(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			PredecessorRelation pre = model.getPredecessorRelation(modelChecker, true);
			return modelChecker.prob1(model, remain, goal, pre);
		}

		public double[] computeUntilProbs(explicit.DTMC model, boolean negated, BitSet remain, BitSet goal)
				throws PrismException
		{
			// FIXME ALG: consider precomputation
			Objects.requireNonNull(goal);
			double[] probabilities = modelChecker.computeUntilProbs(model, remain, goal).soln;
			if (negated) {
				return subtractFromOne(probabilities);
			}
			return probabilities;
		}

		public BitSet computeProb0(Reach<explicit.DTMC> reach)
		{
			if (reach instanceof Finally) {
				return computeProb0((Finally<explicit.DTMC>) reach);
			}
			if (reach instanceof Globally) {
				return computeProb0(((Globally<explicit.DTMC>)reach).asFinally());
			}
			return computeProb0(reach.asUntil());
		}

		public BitSet computeProb0(Finally<explicit.DTMC> eventually)
		{
			return computeProb0(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public BitSet computeProb0(Until<explicit.DTMC> until)
		{
			return computeProb0(until.getModel(), until.isNegated(),until.getRemain(), until.getGoal());
		}

		public BitSet computeProb1(Reach<explicit.DTMC> reach)
		{
			if (reach instanceof Finally) {
				return computeProb1((Finally<explicit.DTMC>) reach);
			}
			if (reach instanceof Globally) {
				return computeProb1(((Globally<explicit.DTMC>)reach).asFinally());
			}
			return computeProb1(reach.asUntil());
		}

		public BitSet computeProb1(Finally<explicit.DTMC> eventually)
		{
			return computeProb1(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public BitSet computeProb1(Until<explicit.DTMC> until)
		{
			return computeProb1(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}

		public double[] computeProbs(Reach<explicit.DTMC> reach)
				throws PrismException
		{
			if (reach instanceof Finally) {
				return computeProbs((Finally<explicit.DTMC>) reach);
			}
			if (reach instanceof Globally) {
				return computeProbs(((Globally<explicit.DTMC>)reach).asFinally());
			}
			return computeProbs(reach.asUntil());
		}

		public double[] computeProbs(Finally<explicit.DTMC> eventually)
				throws PrismException
		{
			return computeUntilProbs(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public double[] computeProbs(Until<explicit.DTMC> until)
				throws PrismException
		{
			return computeUntilProbs(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
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
			return model instanceof explicit.MDP;
		}

		public BitSet computeProb0A(explicit.MDP model, boolean negated, BitSet remain, BitSet goal)
		{
			if (negated) {
				return computeProb1A(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return modelChecker.prob0(model, remain, goal, false, null, pre);
		}

		public BitSet computeProb0E(explicit.MDP model, boolean negated, BitSet remain, BitSet goal)
		{
			if (negated) {
				return computeProb1E(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return modelChecker.prob0(model, remain, goal, true, null, pre);
		}

		public BitSet computeProb1A(explicit.MDP model, boolean negated, BitSet remain, BitSet goal)
		{
			if (negated) {
				return computeProb0A(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return modelChecker.prob1(model, remain, goal, true, null, pre);
		}

		public BitSet computeProb1E(explicit.MDP model, boolean negated, BitSet remain, BitSet goal)
		{
			if (negated) {
				return computeProb0E(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return modelChecker.prob1(model, remain, goal, false, null, pre);
		}

		public double[] computeUntilMaxProbs(explicit.MDP model, boolean negated, BitSet remain, BitSet goal)
				throws PrismException
		{
			if (negated) {
				// Pmax(¬φ) = 1 - Pmin(φ);
				double[] probabilities = computeUntilMinProbs(model, false, remain, goal);
				return subtractFromOne(probabilities);
			}
			Objects.requireNonNull(goal);
			return modelChecker.computeUntilProbs(model, remain, goal, false).soln;
		}

		public double[] computeUntilMinProbs(explicit.MDP model, boolean negated, BitSet remain, BitSet goal)
				throws PrismException
		{
			if (negated) {
				// Pmin(¬φ) = 1 - Pmax(φ);
				double[] probabilities = computeUntilMaxProbs(model, false, remain, goal);
				return subtractFromOne(probabilities);
			}
			Objects.requireNonNull(goal);
			return modelChecker.computeUntilProbs(model, remain, goal, true).soln;
		}

		public BitSet computeProb0E(Reach<explicit.MDP> reach)
		{
			if (reach instanceof Finally) {
				return computeProb0E((Finally<explicit.MDP>) reach);
			}
			if (reach instanceof Globally) {
				return computeProb0E(((Globally<explicit.MDP>)reach).asFinally());
			}
			return computeProb0E(reach.asUntil());
		}

		public BitSet computeProb0E(Finally<explicit.MDP> eventually)
		{
			return computeProb0E(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public BitSet computeProb0E(Until<explicit.MDP> until)
		{
			return computeProb0E(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}

		public BitSet computeProb0A(Reach<explicit.MDP> reach)
		{
			if (reach instanceof Finally) {
				return computeProb0A((Finally<explicit.MDP>) reach);
			}
			if (reach instanceof Globally) {
				return computeProb0A(((Globally<explicit.MDP>)reach).asFinally());
			}
			return computeProb0A(reach.asUntil());
		}

		public BitSet computeProb0A(Finally<explicit.MDP> eventually)
		{
			return computeProb0A(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public BitSet computeProb0A(Until<explicit.MDP> until)
		{
			return computeProb0A(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}

		public BitSet computeProb1E(Reach<explicit.MDP> reach)
		{
			if (reach instanceof Finally) {
				return computeProb1E((Finally<explicit.MDP>) reach);
			}
			if (reach instanceof Globally) {
				return computeProb1E(((Globally<explicit.MDP>)reach).asFinally());
			}
			return computeProb1E(reach.asUntil());
		}

		public BitSet computeProb1E(Finally<explicit.MDP> eventually)
		{
			return computeProb1E(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public BitSet computeProb1E(Until<explicit.MDP> until)
		{
			return computeProb1E(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}

		public BitSet computeProb1A(Reach<explicit.MDP> reach)
		{
			if (reach instanceof Finally) {
				return computeProb1A((Finally<explicit.MDP>) reach);
			}
			if (reach instanceof Globally) {
				return computeProb1A(((Globally<explicit.MDP>)reach).asFinally());
			}
			return computeProb1A(reach.asUntil());
		}

		public BitSet computeProb1A(Finally<explicit.MDP> eventually)
		{
			return computeProb1A(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public BitSet computeProb1A(Until<explicit.MDP> until)
		{
			return computeProb1A(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}

		public double[] computeMaxProbs(Reach<explicit.MDP> reach)
				throws PrismException
		{
			if (reach instanceof Finally) {
				return computeMaxProbs((Finally<explicit.MDP>) reach);
			}
			if (reach instanceof Globally) {
				return computeMaxProbs(((Globally<explicit.MDP>)reach).asFinally());
			}
			return computeMaxProbs(reach.asUntil());
		}

		public double[] computeMaxProbs(Finally<explicit.MDP> eventually)
				throws PrismException
		{
			return computeUntilMaxProbs(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public double[] computeMaxProbs(Until<explicit.MDP> until)
				throws PrismException
		{
			return computeUntilMaxProbs(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}

		public double[] computeMinProbs(Reach<explicit.MDP> reach)
				throws PrismException
		{
			if (reach instanceof Finally) {
				return computeMinProbs((Finally<explicit.MDP>) reach);
			}
			if (reach instanceof Globally) {
				return computeMinProbs(((Globally<explicit.MDP>)reach).asFinally());
			}
			return computeMinProbs(reach.asUntil());
		}

		public double[] computeMinProbs(Finally<explicit.MDP> eventually)
				throws PrismException
		{
			return computeUntilMinProbs(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
		}

		public double[] computeMinProbs(Until<explicit.MDP> until)
				throws PrismException
		{
			return computeUntilMinProbs(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
		}
	}
}
