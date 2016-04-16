package explicit.conditional.transformer.mc;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import common.BitSetTools;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import prism.PrismException;
import prism.PrismLangException;
import explicit.BasicModelTransformation;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.TerminalTransformation;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.DTMCDisjointUnion;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.DTMCView;

public class MCUntilTransformer extends MCConditionalTransformer
{
	private ConditionalReachabilityTransformer transformer;

	public MCUntilTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
		transformer = new ConditionalReachabilityTransformer(modelChecker);
	}

	@Override
	protected boolean canHandleCondition(final DTMC model, final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		final Expression until = ExpressionInspector.removeNegation(condition);
		return ExpressionInspector.isSimpleUntilFormula(until);
	}

	@Override
	protected boolean canHandleObjective(final DTMC model, final ExpressionConditional expression)
	{
		// FIXME ALG: steady state computation
		return !ExpressionInspector.isSteadyStateReward(expression.getObjective());
	}

	@Override
	protected ModelTransformation<DTMC, DTMC> transformModel(final DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		Expression condition = expression.getCondition();
		final boolean absorbing = !requiresSecondMode(expression);



		final Expression until = ExpressionInspector.normalizeExpression(condition);
		final BitSet remain = getRemainStates(model, until);
		final BitSet goal = getGoalStates(model, until);
		final boolean negated = until instanceof ExpressionUnaryOp;
		final boolean collapse = !absorbing;
		
		// 1. create mode 1 == conditional part
		final TerminalTransformation<DTMC, DTMC> mode1 = transformer.transformModel(model, remain, goal, negated, statesOfInterest, collapse);
		getLog().println("Mode 1 has " + mode1.getTransformedModel().getNumStates() + " states");
		
		// 2. create transformed model
		final Map<Integer, Integer> terminalLookup = mode1.getTerminalMapping();
		final DTMCView transformedModel;
		if (absorbing) {
			getLog().println("Mode 2 has " + 0 + " states");
			// make terminal states absorbing
			transformedModel = DTMCAlteredDistributions.addSelfLoops(mode1.getTransformedModel(), BitSetTools.asBitSet(terminalLookup.keySet()));
		} else {
			// mode 2 == submodel reachable from terminal states
			final DTMCRestricted mode2 = new DTMCRestricted(model, terminalLookup.values());
			getLog().println("Mode 2 has " + mode2.getNumStates() + " states");
		
			// union of mode1 and mode2
			// FIXME ALG: code duplication, building identify map
			final Map<Integer, Integer> identify = new HashMap<>(terminalLookup);
			for (Entry<Integer, Integer> id : terminalLookup.entrySet()) {
				identify.put(id.getKey(), mode2.mapStateToRestrictedModel(id.getValue()));
			}
			transformedModel = DTMCDisjointUnion.DTMCUnion(mode1.getTransformedModel(), mode2, identify);
		}
		// sane, as long as mode 1 is already restricted
		ModelTransformation<DTMC, DTMC> union = new BasicModelTransformation<>(mode1.getTransformedModel(), transformedModel, mode1.getTransformedStatesOfInterest());
		ModelTransformation<DTMC, DTMC> nested = new ModelTransformationNested<>(mode1, union);
		
		return nested;
	}

	protected BitSet getRemainStates(final DTMC model, final Expression expression) throws PrismException
	{
		final ExpressionTemporal until = (ExpressionTemporal) ExpressionInspector.removeNegation(expression);
		return modelChecker.checkExpression(model, until.getOperand1(), null).getBitSet();
	}

	protected BitSet getGoalStates(final DTMC model, final Expression expression) throws PrismException
	{
		final ExpressionTemporal until = (ExpressionTemporal) ExpressionInspector.removeNegation(expression);
		return modelChecker.checkExpression(model, until.getOperand2(), null).getBitSet();
	}

	protected boolean requiresSecondMode(final ExpressionConditional expression)
	{
		final Expression condition = ExpressionInspector.trimUnaryOperations(expression.getCondition());
		if (!ExpressionInspector.isUnboundedSimpleUntilFormula(condition)) {
			// can optimize unbounded simple until conditions only
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