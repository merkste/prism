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
import explicit.modelviews.DTMCDisjointUnion;
import explicit.modelviews.DTMCEquiv;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.EquivalenceRelationInteger;

public class MCNextTransformer extends MCConditionalTransformer
{
	public static final boolean DONT_NORMALIZE = false;
	public static final boolean RESTRICT       = true;

	private ConditionalNextTransformer transformer;

	public MCNextTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
		transformer = new ConditionalNextTransformer(modelChecker);
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
	protected ModelTransformation<explicit.DTMC, explicit.DTMC> transformModel(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		return transformModelOld(model, expression, statesOfInterest);
	}

	protected ModelTransformation<explicit.DTMC, explicit.DTMC> transformModelOld(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		// 1. create mode 1 == conditional part
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		final boolean negated = Expression.isNot(condition);
		final BitSet goal = getGoalStates(model, condition);
		final TerminalTransformation<explicit.DTMC, explicit.DTMC> mode1 = transformer.transformModel(model, goal, negated, statesOfInterest);
		getLog().println("Mode 1 has " + mode1.getTransformedModel().getNumStates() + " states");

		// 2. create mode 2 == submodel reachable from terminal states
		final Map<Integer, Integer> terminalLookup = mode1.getTerminalMapping();
		final DTMCRestricted mode2 = new DTMCRestricted(model, terminalLookup.values());
		getLog().println("Mode 2 has " + mode2.getNumStates() + " states");

		// 3. create union model
		// FIXME ALG: code duplication, building identify map
		final Map<Integer, Integer> identify = new HashMap<>(terminalLookup.size());
		for (Entry<Integer, Integer> id : terminalLookup.entrySet()) {
			identify.put(id.getKey(), mode2.mapStateToRestrictedModel(id.getValue()));
		}
		// FIXME ALG: consider making restriction optional
		final explicit.DTMC transformedModel = DTMCDisjointUnion.DTMCUnion(mode1.getTransformedModel(), mode2, identify);
		// sane, as long as mode 1 is already restricted
		ModelTransformation<explicit.DTMC, explicit.DTMC> union = new BasicModelTransformation<>(mode1.getTransformedModel(), transformedModel, mode1.getTransformedStatesOfInterest());
		ModelTransformation<explicit.DTMC, explicit.DTMC> nested = new ModelTransformationNested<>(mode1, union);

		return nested;
	}

	protected ModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformModelNew(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		// 1. create mode 1 == conditional part
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		final boolean negated = Expression.isNot(condition);
		final BitSet goal = getGoalStates(model, condition);
		final TerminalTransformation<explicit.DTMC, explicit.DTMC> mode1 = transformer.transformModel(model, goal, negated, statesOfInterest);
		getLog().println("Mode 1 has " + mode1.getTransformedModel().getNumStates() + " states");

		// 2. create mode 2 == submodel reachable from terminal states
		final Map<Integer, Integer> terminalLookup = mode1.getTerminalMapping();
		final DTMCRestricted mode2 = new DTMCRestricted(model, terminalLookup.values());
		getLog().println("Mode 2 has " + mode2.getNumStates() + " states");

		// 3. create union model
		// FIXME ALG: code duplication, building identify map
		DTMCDisjointUnion unionModel = new DTMCDisjointUnion(mode1.getTransformedModel(), mode2);
		BasicModelTransformation<explicit.DTMC, explicit.DTMC> union = new BasicModelTransformation<>(mode1.getTransformedModel(), unionModel, mode1.getTransformedStatesOfInterest());

		Function<Entry<Integer, Integer>, BitSet> asEqClass = pair -> BitSetTools.asBitSet(pair.getKey(), mode2.mapStateToRestrictedModel(pair.getValue()) + unionModel.offset);
		FunctionalIterable<BitSet> identify = FunctionalIterable.extend(terminalLookup.entrySet()).map(asEqClass);

		BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> equiv = DTMCEquiv.transform(unionModel, new EquivalenceRelationInteger(identify), DONT_NORMALIZE, RESTRICT);

		return equiv.compose(union).compose(mode1);
	}

	protected BitSet getGoalStates(final explicit.DTMC model, final Expression expression) throws PrismException
	{
		final ExpressionTemporal next = getExpressionTemporal(expression);
		return modelChecker.checkExpression(model, next.getOperand2(), null).getBitSet();
	}

	protected ExpressionTemporal getExpressionTemporal(final Expression expression) throws PrismLangException
	{
		if (Expression.isNot(expression)) {
			return getExpressionTemporal(((ExpressionUnaryOp) expression).getOperand());
		}
		if (expression instanceof ExpressionTemporal) {
			return (ExpressionTemporal) expression;
		}
		throw new PrismLangException("expected (negated) temporal formula but found", expression);
	}
}