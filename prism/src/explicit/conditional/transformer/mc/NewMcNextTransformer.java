package explicit.conditional.transformer.mc;

import java.util.BitSet;

import common.BitSetTools;
import common.iterable.Interval;
import explicit.BasicModelTransformation;
import explicit.DTMCModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ReachabilityComputer;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.Restriction;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;

public class NewMcNextTransformer extends MCConditionalTransformer
{
	public NewMcNextTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression)
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
		if (ExpressionInspector.isSteadyStateReward(expression.getObjective())) {
			// cannot handle steady state computation yet
			return false;
		}
		return true;
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression)
	{
		// cannot handle steady state computation yet
		return !ExpressionInspector.isSteadyStateReward(expression.getObjective());
	}

	@Override
	protected ModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformModel(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		Expression condition = expression.getCondition();

		Expression next = ExpressionInspector.normalizeExpression(condition);
		BitSet goal     = getGoalStates(model, next);
		boolean negated = Expression.isNot(next);

		int numStates = model.getNumStates();
		final Interval states = new Interval(numStates);
		double[] originProbs               = computeNextProbs(model, negated, goal);
		double[] targetProbs               = states.map((int s) -> goal.get(s) ? 1.0 : 0.0).collect(new double[numStates]);
		BitSet support                     = BitSetTools.asBitSet(states.filter((int state) -> originProbs[state] > 0.0));
		BitSet transformedStatesOfInterest = BitSetTools.intersect(statesOfInterest, support);
		if (transformedStatesOfInterest.isEmpty()) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

		// Switch in pivot states to copy of model
		BitSet pivotStates                                     = getPivotStates(model, goal, negated);
		BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> pivoted = McPivotTransformation.transform(model, pivotStates);
		pivoted.setTransformedStatesOfInterest(transformedStatesOfInterest);

		// Scale probabilities
		BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> scaled = McScaledTransformation.transform(pivoted.getTransformedModel(), originProbs, targetProbs);
		scaled.setTransformedStatesOfInterest(transformedStatesOfInterest);

		// Restrict to reachable states
		ReachabilityComputer reachability                          = new ReachabilityComputer(model);
		BitSet restrictPrePivot                                    = transformedStatesOfInterest;
		BitSet restrictAndPivot                                    = BitSetTools.intersect(reachability.computeSucc(restrictPrePivot), pivotStates);
		BitSet restrictSuccPivot                                   = reachability.computeSuccStar(restrictAndPivot);
		BitSet restrict                                            = BitSetTools.union(restrictPrePivot, BitSetTools.shiftUp(restrictSuccPivot, numStates));
		BasicModelTransformation<explicit.DTMC, DTMCRestricted> restricted  = DTMCRestricted.transform(scaled.getTransformedModel(), restrict, Restriction.TRANSITIVE_CLOSURE_SAFE);
		restricted.setTransformedStatesOfInterest(restricted.mapToTransformedModel(transformedStatesOfInterest));

		return restricted.compose(scaled).compose(pivoted);
	}

	protected BitSet getPivotStates(final explicit.DTMC model, final BitSet goal, final boolean negated)
	{
		if (! negated) {
			return goal;
		}
		return BitSetTools.complement(model.getNumStates(), goal);
	}

	protected BitSet getGoalStates(final explicit.DTMC model, final Expression expression) throws PrismException
	{
		ExpressionTemporal next = (ExpressionTemporal) ExpressionInspector.removeNegation(expression);
		return getModelChecker(model).checkExpression(model, next.getOperand2(), null).getBitSet();
	}
}