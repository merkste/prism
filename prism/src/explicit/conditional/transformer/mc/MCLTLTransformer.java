package explicit.conditional.transformer.mc;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import parser.ast.ExpressionConditional;
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

public class MCLTLTransformer extends MCConditionalTransformer
{
	private ConditionalLTLTransformer transformer;

	public MCLTLTransformer(final DTMCModelChecker modelChecker) throws PrismException
	{
		super(modelChecker);
		transformer = new ConditionalLTLTransformer(modelChecker);
	}

	@Override
	protected boolean canHandleCondition(final DTMC model, final ExpressionConditional expression) throws PrismLangException
	{
		return transformer.canHandle(model, expression.getCondition());
	}

	@Override
	protected boolean canHandleObjective(final DTMC model, final ExpressionConditional expression)
	{
		// cannot handle steady state computation yet
		return !(ExpressionInspector.isSteadyStateReward(expression.getObjective()));
	}

	@Override
	protected ModelTransformation<DTMC, DTMC> transformModel(final DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		// 1. create mode 1 == conditional part
		final TerminalTransformation<DTMC, DTMC> mode1 = transformer.transformModel(model, expression.getCondition(), statesOfInterest);
		getLog().println("Mode 1 has " + mode1.getTransformedModel().getNumStates() + " states");

		// 2. create mode 2 == submodel reachable from terminal states
		final Map<Integer, Integer> terminalLookup = mode1.getTerminalMapping();
		final DTMCRestricted mode2 = new DTMCRestricted(model, terminalLookup.values());
		getLog().println("Mode 2 has " + mode2.getNumStates() + " states");

		// 3. create union model
		// FIXME ALG: code duplication, building identify map
		final Map<Integer, Integer> identify = new HashMap<>(terminalLookup);
		for (Entry<Integer, Integer> id : terminalLookup.entrySet()) {
			identify.put(id.getKey(), mode2.mapStateToRestrictedModel(id.getValue()));
		}
		final DTMCRestricted transformedModel = DTMCDisjointUnion.DTMCUnion(mode1.getTransformedModel(), mode2, identify);
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
}