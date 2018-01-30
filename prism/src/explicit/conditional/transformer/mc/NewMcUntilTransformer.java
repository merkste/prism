package explicit.conditional.transformer.mc;

import java.util.BitSet;
import java.util.function.IntFunction;

import common.BitSetTools;
import explicit.BasicModelTransformation;
import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ProbModelChecker;
import explicit.ReachabilityComputer;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.NewConditionalTransformer;
import explicit.conditional.transformer.UndefinedTransformationException;
import explicit.modelviews.CTMCAlteredDistributions;
import explicit.modelviews.CTMCRestricted;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.Restriction;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;

public interface NewMcUntilTransformer<M extends explicit.DTMC, C extends ProbModelChecker> extends MCConditionalTransformer<M,C>
{
	@Override
	default boolean canHandleCondition(final Model model, final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		final Expression until = ExpressionInspector.removeNegation(condition);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
	}

	@Override
	default boolean canHandleObjective(final Model model, final ExpressionConditional expression)
	{
		// Can handle all ExpressionQuant: P, R and S
		return true;
	}

	@Override
	default ModelTransformation<M, ? extends M> transformModel(final M model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		boolean deadlock     = !requiresSecondMode(expression);
		Expression condition = expression.getCondition();

		Expression until = ExpressionInspector.normalizeExpression(condition);
		BitSet remain    = getRemainStates(model, until);
		BitSet goal      = getGoalStates(model, until);
		boolean negated  = Expression.isNot(until);

		BitSet prob0                       = getMcModelChecker().computeProb0(model, negated, remain, goal);
		BitSet support                     = BitSetTools.complement(model.getNumStates(), prob0);
		BitSet transformedStatesOfInterest = BitSetTools.intersect(statesOfInterest, support);
		if (transformedStatesOfInterest.isEmpty()) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		BitSet prob1                       = getMcModelChecker().computeProb1(model, negated, remain, goal);
		double[] probs                     = getMcModelChecker().computeUntilProbs(model, negated, remain, goal, prob0, prob1);

		ReachabilityComputer reachability = new ReachabilityComputer(model);
		BitSet pivotStates, restrict;
		BasicModelTransformation<M, ? extends M> pivoted;
		if (deadlock) {
			// Transform pivot states to traps
			pivotStates     = getPivotStates(model, remain, goal, negated);
			M deadlockModel = deadlock(model, pivotStates);
			pivoted         = new BasicModelTransformation<>(model, deadlockModel, transformedStatesOfInterest);

			// Compute reachable states
			BitSet restrictPrePivot  = reachability.computeSuccStar(statesOfInterest, BitSetTools.minus(support, pivotStates));
			BitSet restrictAndPivot  = BitSetTools.intersect(reachability.computeSucc(restrictPrePivot), pivotStates);
			restrict                 = BitSetTools.union(restrictPrePivot, restrictAndPivot);
		} else {
			// Switch in pivot states to copy of model
			pivotStates  = prob1;
			pivoted      = pivot(model, pivotStates);
			M pivotModel = pivoted.getTransformedModel();
			int offset   = model.getNumStates();

			// Adapt states of interest
			BitSet statesOfInterestAndPivot = BitSetTools.intersect(transformedStatesOfInterest, pivotStates);
			BitSet statesOfInterestNonPivot = BitSetTools.minus(transformedStatesOfInterest, pivotStates);
			transformedStatesOfInterest     = BitSetTools.union(statesOfInterestNonPivot, BitSetTools.shiftUp(statesOfInterestAndPivot, offset));

			// Allow states of interest in pivot states
			IntFunction<Integer> mapToTransformedModel = state -> pivotStates.get(state) ? state + offset : state;
			pivoted = new BasicModelTransformation<>(pivotModel, pivotModel, transformedStatesOfInterest, mapToTransformedModel).compose(pivoted);

			// Compute reachable states
			BitSet restrictPrePivot  = reachability.computeSuccStar(statesOfInterest, BitSetTools.minus(support, pivotStates));
			BitSet restrictAndPivot  = BitSetTools.intersect(reachability.computeSucc(restrictPrePivot), pivotStates);
			BitSet restrictSuccPivot = reachability.computeSuccStar(BitSetTools.union(statesOfInterestAndPivot, restrictAndPivot));
			restrict                 = BitSetTools.union(restrictPrePivot, BitSetTools.shiftUp(restrictSuccPivot, model.getNumStates()));
		}
		assert BitSetTools.isSubset(pivotStates, prob1) : "Pivot states must have probability 1";

		// Scale probabilities
		BasicModelTransformation<M, ? extends M> scaled = scale(pivoted, probs);
		scaled.setTransformedStatesOfInterest(transformedStatesOfInterest);

		// Restrict to reachable states
		BasicModelTransformation<M, ? extends M> restricted  = restrict(scaled, restrict);
		restricted.setTransformedStatesOfInterest(restricted.mapToTransformedModel(transformedStatesOfInterest));

		return restricted.compose(scaled).compose(pivoted);
	}

	default BitSet getRemainStates(final M model, final Expression expression) throws PrismException
	{
		ExpressionTemporal until = (ExpressionTemporal) ExpressionInspector.removeNegation(expression);
		return getModelChecker(model).checkExpression(model, until.getOperand1(), null).getBitSet();
	}

	default BitSet getPivotStates(final M model, final BitSet remain, final BitSet goal, final boolean negated)
	{
		if (! negated) {
			return goal;
		}
		// terminal = ! (remain | goal)
		int numStates = model.getNumStates();
		if (goal == null || goal.cardinality() == numStates || remain == null || remain.cardinality() == numStates) {
			// shortcut if either set equals all states
			return new BitSet();
		}
		BitSet terminals = BitSetTools.union(remain, goal);
		terminals.flip(0, numStates);
		return terminals;
	}

	default BitSet getGoalStates(final M model, final Expression expression) throws PrismException
	{
		ExpressionTemporal until = (ExpressionTemporal) ExpressionInspector.removeNegation(expression);
		return getModelChecker(model).checkExpression(model, until.getOperand2(), null).getBitSet();
	}

	default boolean requiresSecondMode(final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.trimUnaryOperations(expression.getCondition());
		if (!ExpressionInspector.isUnboundedSimpleUntilFormula(condition)) {
			// can optimize non-negated unbounded simple until conditions only
			return true;
		}
		final ExpressionTemporal conditionPath = (ExpressionTemporal) condition;

		final Expression objective = expression.getObjective();
		final Expression objectiveSubExpr;
		if (ExpressionInspector.isReachablilityReward(objective)) {
			objectiveSubExpr = ((ExpressionReward) objective).getExpression();
		} else if (objective instanceof ExpressionProb) {
			objectiveSubExpr = ((ExpressionProb) objective).getExpression();
			if (! ExpressionInspector.isUnboundedSimpleUntilFormula(objectiveSubExpr)) {
				return true;
			}
		} else {
			return true;
		}
		final ExpressionTemporal objectivePath = (ExpressionTemporal) objectiveSubExpr;

		Expression conditionGoal = ExpressionInspector.trimUnaryOperations(conditionPath.getOperand2());
		Expression objectiveGoal = ExpressionInspector.trimUnaryOperations(objectivePath.getOperand2());
		if (conditionGoal != null && objectiveGoal != null) {
			try {
				return !objectiveGoal.syntacticallyEquals(conditionGoal);
			} catch (PrismLangException e) {}
		}
		return true;
	}

	M deadlock(M model, BitSet pivotStates);

	BasicModelTransformation<M, ? extends M> pivot(M model, BitSet pivotStates);

	BasicModelTransformation<M, ? extends M> scale(BasicModelTransformation<M, ? extends M> pivoted, double[] probs);

	BasicModelTransformation<M, ? extends M> restrict(BasicModelTransformation<M, ? extends M> scaled, BitSet restrict);

	

	public class CTMC extends NewConditionalTransformer.Basic<explicit.CTMC, CTMCModelChecker> implements NewMcUntilTransformer<explicit.CTMC, CTMCModelChecker>, MCConditionalTransformer.CTMC
	{
		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public explicit.CTMC deadlock(explicit.CTMC model, BitSet pivotStates)
		{
			return CTMCAlteredDistributions.trapStates(model, pivotStates);
		}

		@Override
		public BasicModelTransformation<explicit.CTMC, CTMCAlteredDistributions> pivot(explicit.CTMC model, BitSet pivotStates)
		{
			return McPivotTransformation.transform(model, pivotStates);
		}

		@Override
		public BasicModelTransformation<explicit.CTMC, CTMCAlteredDistributions> scale(BasicModelTransformation<explicit.CTMC, ? extends explicit.CTMC> pivoted, double[] probs)
		{
			return McScaledTransformation.transform(pivoted.getTransformedModel(), probs);
		}

		@Override
		public BasicModelTransformation<explicit.CTMC, CTMCRestricted> restrict(BasicModelTransformation<explicit.CTMC, ? extends explicit.CTMC> scaled, BitSet restrict)
		{
			return CTMCRestricted.transform(scaled.getTransformedModel(), restrict, Restriction.TRANSITIVE_CLOSURE_SAFE);
		}
	}



	public class DTMC extends NewConditionalTransformer.Basic<explicit.DTMC, DTMCModelChecker> implements NewMcUntilTransformer<explicit.DTMC, DTMCModelChecker>, MCConditionalTransformer.DTMC
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public explicit.DTMC deadlock(explicit.DTMC model, BitSet pivotStates)
		{
			return DTMCAlteredDistributions.trapStates(model, pivotStates);
		}

		@Override
		public BasicModelTransformation<explicit.DTMC, DTMCAlteredDistributions> pivot(explicit.DTMC model, BitSet pivotStates)
		{
			return McPivotTransformation.transform(model, pivotStates);
		}

		@Override
		public BasicModelTransformation<explicit.DTMC, DTMCAlteredDistributions> scale(BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> pivoted, double[] probs)
		{
			return McScaledTransformation.transform(pivoted.getTransformedModel(), probs);
		}

		@Override
		public BasicModelTransformation<explicit.DTMC, DTMCRestricted> restrict(BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> scaled, BitSet restrict)
		{
			return DTMCRestricted.transform(scaled.getTransformedModel(), restrict, Restriction.TRANSITIVE_CLOSURE_SAFE);
		}
	}
}