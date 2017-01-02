package explicit.conditional.transformer.mc;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import common.BitSetTools;
import common.iterable.FunctionalIterable;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import prism.PrismException;
import prism.PrismLangException;
import explicit.BasicModelTransformation;
import explicit.DTMCModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.TerminalTransformation;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.DTMCDisjointUnion;
import explicit.modelviews.DTMCEquiv;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.EquivalenceRelationInteger;

public class MCUntilTransformer extends MCConditionalTransformer
{
	public static final boolean DONT_NORMALIZE = false;
	public static final boolean RESTRICT       = true;

	private ConditionalReachabilityTransformer transformer;

	public MCUntilTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
		transformer = new ConditionalReachabilityTransformer(modelChecker);
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
	protected ModelTransformation<explicit.DTMC, explicit.DTMC> transformModel(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		return transformModelOld(model, expression, statesOfInterest);
	}

	protected ModelTransformation<explicit.DTMC, explicit.DTMC> transformModelOld(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		Expression condition = expression.getCondition();
		final boolean absorbing = !requiresSecondMode(expression);

		final Expression until = ExpressionInspector.normalizeExpression(condition);
		final BitSet remain    = getRemainStates(model, until);
		final BitSet goal      = getGoalStates(model, until);
		final boolean negated  = until instanceof ExpressionUnaryOp;
		final boolean collapse = !absorbing;

		// 1. create mode 1 == conditional part
		final TerminalTransformation<explicit.DTMC, explicit.DTMC> mode1 = transformer.transformModel(model, remain, goal, negated, statesOfInterest, collapse);
		getLog().println("Mode 1 has " + mode1.getTransformedModel().getNumStates() + " states");

		// 2. create transformed model
		final Map<Integer, Integer> terminalLookup = mode1.getTerminalMapping();
		final explicit.DTMC transformedModel;
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
		ModelTransformation<explicit.DTMC, explicit.DTMC> union = new BasicModelTransformation<>(mode1.getTransformedModel(), transformedModel, mode1.getTransformedStatesOfInterest());
		ModelTransformation<explicit.DTMC, explicit.DTMC> nested = new ModelTransformationNested<>(mode1, union);
		
		return nested;
	}

	protected ModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformModelNew(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		Expression condition = expression.getCondition();
		final boolean absorbing = !requiresSecondMode(expression);

		final Expression until = ExpressionInspector.normalizeExpression(condition);
		final BitSet remain    = getRemainStates(model, until);
		final BitSet goal      = getGoalStates(model, until);
		final boolean negated  = until instanceof ExpressionUnaryOp;
		final boolean collapse = !absorbing;

		// 1. create mode 1 == conditional part
		final TerminalTransformation<explicit.DTMC, explicit.DTMC> mode1 = transformer.transformModel(model, remain, goal, negated, statesOfInterest, collapse);
		getLog().println("Mode 1 has " + mode1.getTransformedModel().getNumStates() + " states");

		// 2. create transformed model
		final Map<Integer, Integer> terminalLookup = mode1.getTerminalMapping();
		BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformation;
		if (absorbing) {
			getLog().println("Mode 2 has " + 0 + " states");
			// make terminal states absorbing
			DTMCAlteredDistributions transformedModel = DTMCAlteredDistributions.trapStates(mode1.getTransformedModel(), BitSetTools.asBitSet(terminalLookup.keySet()));
			transformation = new BasicModelTransformation<>(mode1.getTransformedModel(), transformedModel, mode1.getTransformedStatesOfInterest());
		} else {
			// mode 2 == submodel reachable from terminal states
			final DTMCRestricted mode2 = new DTMCRestricted(model, terminalLookup.values());
			getLog().println("Mode 2 has " + mode2.getNumStates() + " states");

			DTMCDisjointUnion unionModel = new DTMCDisjointUnion(mode1.getTransformedModel(), mode2);
			BasicModelTransformation<explicit.DTMC, explicit.DTMC> union = new BasicModelTransformation<>(mode1.getTransformedModel(), unionModel, mode1.getTransformedStatesOfInterest());

			Function<Entry<Integer, Integer>, BitSet> asEqClass = pair -> BitSetTools.asBitSet(pair.getKey(), mode2.mapStateToRestrictedModel(pair.getValue()) + unionModel.offset);
			FunctionalIterable<BitSet> identify = FunctionalIterable.extend(terminalLookup.entrySet()).map(asEqClass);

			BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> equiv = DTMCEquiv.transform(unionModel, new EquivalenceRelationInteger(identify), DONT_NORMALIZE, RESTRICT);

			transformation = equiv.compose(union);
		}
		// sane, as long as mode 1 is already restricted
		return transformation.compose(mode1);
	}

	protected BitSet getRemainStates(final explicit.DTMC model, final Expression expression) throws PrismException
	{
		final ExpressionTemporal until = (ExpressionTemporal) ExpressionInspector.removeNegation(expression);
		return modelChecker.checkExpression(model, until.getOperand1(), null).getBitSet();
	}

	protected BitSet getGoalStates(final explicit.DTMC model, final Expression expression) throws PrismException
	{
		final ExpressionTemporal until = (ExpressionTemporal) ExpressionInspector.removeNegation(expression);
		return modelChecker.checkExpression(model, until.getOperand2(), null).getBitSet();
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