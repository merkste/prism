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
import explicit.conditional.ConditionalTransformer;
import explicit.conditional.transformer.UndefinedTransformationException;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;

//FIXME ALG: add comment
public interface MCNextTransformer<M extends explicit.DTMC,C extends ProbModelChecker> extends ScaleTransformer<M,C>
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
		BasicModelTransformation<M, ? extends M> scaled  = scale(pivoted.getTransformedModel(), originProbs, targetProbs);
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



	public static class CTMC extends ConditionalTransformer.Basic<explicit.CTMC, CTMCModelChecker> implements MCNextTransformer<explicit.CTMC, CTMCModelChecker>, ScaleTransformer.CTMC
	{
		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}



	public static class DTMC extends ConditionalTransformer.Basic<explicit.DTMC, DTMCModelChecker> implements MCNextTransformer<explicit.DTMC, DTMCModelChecker>, ScaleTransformer.DTMC
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}
}