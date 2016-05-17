package explicit.conditional.transformer.mc;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import parser.ast.Expression;
import parser.ast.ExpressionConditional;
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
import explicit.modelviews.DTMCDisjointUnion;
import explicit.modelviews.DTMCRestricted;

public class MCNextTransformer extends MCConditionalTransformer
{
	private ConditionalNextTransformer transformer;

	public MCNextTransformer(final DTMCModelChecker modelChecker)
	{
		super(modelChecker);
		transformer = new ConditionalNextTransformer(modelChecker);
	}

	@Override
	protected boolean canHandleCondition(final DTMC model, final ExpressionConditional expression)
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
	protected boolean canHandleObjective(final DTMC model, final ExpressionConditional expression)
	{
		// cannot handle steady state computation yet
		return !ExpressionInspector.isSteadyStateReward(expression.getObjective());
	}

	@Override
	protected ModelTransformation<DTMC, DTMC> transformModel(final DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		// 1. create mode 1 == conditional part
		final Expression condition = ExpressionInspector.normalizeExpression(expression.getCondition());
		final boolean negated = Expression.isNot(condition);
		final BitSet goal = getGoalStates(model, condition);
		final TerminalTransformation<DTMC, DTMC> mode1 = transformer.transformModel(model, goal, negated, statesOfInterest);
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
		final DTMCRestricted transformedModel = DTMCDisjointUnion.DTMCUnion(mode1.getTransformedModel(), mode2, identify);
		// sane, as long as mode 1 is already restricted
		ModelTransformation<DTMC, DTMC> union = new BasicModelTransformation<>(mode1.getTransformedModel(), transformedModel, mode1.getTransformedStatesOfInterest());
		ModelTransformation<DTMC, DTMC> nested = new ModelTransformationNested<>(mode1, union);

		return nested;

//		// 4. create model transformation
//		// FIXME ALG: consider ModelExpressionTransformationNested
//		// FIXME ALG: refactor to remove tedious code duplication
//		final Integer[] mapping = new Integer[model.getNumStates()];
//		for (int state = 0; state < mapping.length; state++) {
//			mapping[state] = mode1.mapToTransformedModel(state);
//		}
//
//		return new BasicModelTransformation<DTMC, DTMC>(model, transformedModel, mapping);
	}

	protected BitSet getGoalStates(final DTMC model, final Expression expression) throws PrismException
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