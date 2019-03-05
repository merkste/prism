package explicit.conditional.scale;

import java.util.BitSet;

import common.BitSetTools;
import common.iterable.Interval;
import explicit.BasicModelTransformation;
import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ProbModelChecker;
import explicit.ReachabilityComputer;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.MCConditionalTransformer;
import explicit.conditional.ConditionalTransformer;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.modelviews.CTMCAlteredDistributions;
import explicit.modelviews.CTMCRestricted;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.Restriction;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;

public interface MCNextTransformer<M extends explicit.DTMC,C extends ProbModelChecker> extends MCConditionalTransformer<M,C>
{
	@Override
	default boolean canHandleCondition(final Model model, final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		try {
			if (!(condition instanceof ExpressionTemporal && condition.isSimplePathFormula())) {
				// can handle simple path conditions only
				return false;
			}
		} catch (PrismLangException e) {
			// condition cannot be checked whether it is a simple formula
			return false;
		}
		final ExpressionTemporal temporal = (ExpressionTemporal) condition;
		if (!(temporal.getOperator() == ExpressionTemporal.P_X)) {
			// can handle next conditions only
			return false;
		}
		if (temporal.hasBounds()) {
			// can handle unbounded conditions only
			return false;
		}
		return true;
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

		Expression next = ExpressionInspector.normalizeExpression(condition);
		BitSet goal     = getGoalStates(model, next);
		boolean negated = Expression.isNot(next);

		int numStates = model.getNumStates();
		final Interval states = new Interval(numStates);
		double[] originProbs               = getMcModelChecker().computeNextProbs(model, negated, goal);
		double[] targetProbs               = states.mapToDouble((int s) -> goal.get(s) ? 1.0 : 0.0).collect(new double[numStates]);
		BitSet support                     = BitSetTools.asBitSet(states.filter((int state) -> originProbs[state] > 0.0));
		BitSet transformedStatesOfInterest = BitSetTools.intersect(statesOfInterest, support);
		if (transformedStatesOfInterest.isEmpty()) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

		// Switch in pivot states to copy of model
		BitSet pivotStates                               = getPivotStates(model, goal, negated);
		BasicModelTransformation<M, ? extends M> pivoted = pivot(model, pivotStates);
		pivoted.setTransformedStatesOfInterest(transformedStatesOfInterest);

		// Scale probabilities
		BasicModelTransformation<M, ? extends M> scaled  = scale(pivoted, originProbs, targetProbs);
		scaled.setTransformedStatesOfInterest(transformedStatesOfInterest);

		// Restrict to reachable states
		ReachabilityComputer reachability                    = new ReachabilityComputer(model);
		BitSet restrictPrePivot                              = transformedStatesOfInterest;
		BitSet restrictAndPivot                              = BitSetTools.intersect(reachability.computeSucc(restrictPrePivot), pivotStates);
		BitSet restrictSuccPivot                             = reachability.computeSuccStar(restrictAndPivot);
		BitSet restrict                                      = BitSetTools.union(restrictPrePivot, BitSetTools.shiftUp(restrictSuccPivot, numStates));
		BasicModelTransformation<M, ? extends M> restricted  = restrict(scaled, restrict);
		restricted.setTransformedStatesOfInterest(restricted.mapToTransformedModel(transformedStatesOfInterest));

		return restricted.compose(scaled).compose(pivoted);
	}

	default BitSet getPivotStates(final M model, final BitSet goal, final boolean negated)
	{
		if (! negated) {
			return goal;
		}
		return BitSetTools.complement(model.getNumStates(), goal);
	}

	default BitSet getGoalStates(final M model, final Expression expression) throws PrismException
	{
		ExpressionTemporal next = (ExpressionTemporal) ExpressionInspector.removeNegation(expression);
		return getModelChecker(model).checkExpression(model, next.getOperand2(), null).getBitSet();
	}

	BasicModelTransformation<M, ? extends M> pivot(M model, BitSet pivotStates);

	BasicModelTransformation<M, ? extends M> scale(BasicModelTransformation<M, ? extends M> pivoted, double[] originProbs, double[] targetProbs);

	BasicModelTransformation<M, ? extends M> restrict(BasicModelTransformation<M, ? extends M> scaled, BitSet restrict);


	public static class CTMC extends ConditionalTransformer.Basic<explicit.CTMC, CTMCModelChecker> implements MCNextTransformer<explicit.CTMC, CTMCModelChecker>, MCConditionalTransformer.CTMC
	{
		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public BasicModelTransformation<explicit.CTMC, CTMCRestricted> restrict(BasicModelTransformation<explicit.CTMC, ? extends explicit.CTMC> scaled, BitSet restrict)
		{
			return CTMCRestricted.transform(scaled.getTransformedModel(), restrict, Restriction.TRANSITIVE_CLOSURE_SAFE);
		}

		@Override
		public BasicModelTransformation<explicit.CTMC, CTMCAlteredDistributions> scale(BasicModelTransformation<explicit.CTMC, ? extends explicit.CTMC> pivoted, double[] originProbs,
				double[] targetProbs)
		{
			return MCScaledTransformation.transform(pivoted.getTransformedModel(), originProbs, targetProbs);
		}

		@Override
		public BasicModelTransformation<explicit.CTMC, CTMCAlteredDistributions> pivot(explicit.CTMC model, BitSet pivotStates)
		{
			return MCPivotTransformation.transform(model, pivotStates);
		}
	}



	public static class DTMC extends ConditionalTransformer.Basic<explicit.DTMC, DTMCModelChecker> implements MCNextTransformer<explicit.DTMC, DTMCModelChecker>, MCConditionalTransformer.DTMC
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public BasicModelTransformation<explicit.DTMC, DTMCRestricted> restrict(BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> scaled, BitSet restrict)
		{
			return DTMCRestricted.transform(scaled.getTransformedModel(), restrict, Restriction.TRANSITIVE_CLOSURE_SAFE);
		}

		@Override
		public BasicModelTransformation<explicit.DTMC, DTMCAlteredDistributions> scale(BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> pivoted, double[] originProbs,
				double[] targetProbs)
		{
			return MCScaledTransformation.transform(pivoted.getTransformedModel(), originProbs, targetProbs);
		}

		@Override
		public BasicModelTransformation<explicit.DTMC, DTMCAlteredDistributions> pivot(explicit.DTMC model, BitSet pivotStates)
		{
			return MCPivotTransformation.transform(model, pivotStates);
		}
	}

}