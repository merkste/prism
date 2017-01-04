package explicit.conditional.transformer.mc;

import java.util.BitSet;
import java.util.function.IntFunction;

import common.BitSetTools;
import explicit.BasicModelTransformation;
import explicit.DTMCModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ReachabilityComputer;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.UndefinedTransformationException;
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

public class NewMcUntilTransformer extends MCConditionalTransformer
{
	public NewMcUntilTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		final Expression until = ExpressionInspector.removeNegation(condition);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression)
	{
		// FIXME ALG: steady state computation
		return !ExpressionInspector.isSteadyStateReward(expression.getObjective());
	}

	@Override
	protected ModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformModel(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		boolean deadlock     = !requiresSecondMode(expression);
		Expression condition = expression.getCondition();

		Expression until = ExpressionInspector.normalizeExpression(condition);
		BitSet remain    = getRemainStates(model, until);
		BitSet goal      = getGoalStates(model, until);
		boolean negated  = Expression.isNot(until);

		BitSet prob0                       = computeProb0(model, negated, remain, goal);
		BitSet support                     = BitSetTools.complement(model.getNumStates(), prob0);
		BitSet transformedStatesOfInterest = BitSetTools.intersect(statesOfInterest, support);
		if (transformedStatesOfInterest.isEmpty()) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		BitSet prob1                       = computeProb1(model, negated, remain, goal);
		double[] probs                     = computeUntilProbs(model, negated, remain, goal, prob0, prob1);

		ReachabilityComputer reachability = new ReachabilityComputer(model);
		BitSet pivotStates, restrict;
		BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> pivoted;
		if (deadlock) {
			// Transform pivot states to traps
			pivotStates                            = getPivotStates(model, remain, goal, negated);
			DTMCAlteredDistributions deadlockModel = DTMCAlteredDistributions.trapStates(model, pivotStates);
			pivoted                                = new BasicModelTransformation<>(model, deadlockModel, transformedStatesOfInterest);

			// Compute reachable states
			BitSet restrictPrePivot  = reachability.computeSuccStar(statesOfInterest, BitSetTools.minus(support, pivotStates));
			BitSet restrictAndPivot  = BitSetTools.intersect(reachability.computeSucc(restrictPrePivot), pivotStates);
			restrict                 = BitSetTools.union(restrictPrePivot, restrictAndPivot);
		} else {
			// Switch in pivot states to copy of model
			pivotStates     = prob1;
			pivoted         = McPivotTransformation.transform(model, pivotStates);
			explicit.DTMC pivotModel = pivoted.getTransformedModel();
			int offset      = model.getNumStates();

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
		BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> scaled = McScaledTransformation.transform(pivoted.getTransformedModel(), probs);
		scaled.setTransformedStatesOfInterest(transformedStatesOfInterest);

		// Restrict to reachable states
		BasicModelTransformation<explicit.DTMC, DTMCRestricted> restricted  = DTMCRestricted.transform(scaled.getTransformedModel(), restrict, Restriction.TRANSITIVE_CLOSURE_SAFE);
		restricted.setTransformedStatesOfInterest(restricted.mapToTransformedModel(transformedStatesOfInterest));

		return restricted.compose(scaled).compose(pivoted);
	}

	protected BitSet getRemainStates(final explicit.DTMC model, final Expression expression) throws PrismException
	{
		ExpressionTemporal until = (ExpressionTemporal) ExpressionInspector.removeNegation(expression);
		return getModelChecker(model).checkExpression(model, until.getOperand1(), null).getBitSet();
	}

	protected BitSet getPivotStates(final explicit.DTMC model, final BitSet remain, final BitSet goal, final boolean negated)
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

	protected BitSet getGoalStates(final explicit.DTMC model, final Expression expression) throws PrismException
	{
		ExpressionTemporal until = (ExpressionTemporal) ExpressionInspector.removeNegation(expression);
		return getModelChecker(model).checkExpression(model, until.getOperand2(), null).getBitSet();
	}

	protected boolean requiresSecondMode(final ExpressionConditional expression)
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
}