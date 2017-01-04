package explicit.conditional.prototype.virtual;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import common.BitSetTools;
import common.iterable.FunctionalIterable;
import parser.ast.ExpressionConditional;
import prism.PrismException;
import prism.PrismLangException;
import explicit.BasicModelTransformation;
import explicit.DTMCModelChecker;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.mc.MCConditionalTransformer;
import explicit.modelviews.DTMCDisjointUnion;
import explicit.modelviews.DTMCEquiv;
import explicit.modelviews.DTMCRestricted;
import explicit.modelviews.EquivalenceRelationInteger;

@Deprecated
public class MCLTLTransformer extends MCConditionalTransformer
{
	public static final boolean DONT_NORMALIZE = false;
	public static final boolean RESTRICT       = true;

	protected ConditionalLTLTransformer transformer;

	public MCLTLTransformer(final DTMCModelChecker modelChecker) throws PrismException
	{
		super(modelChecker);
		transformer = new ConditionalLTLTransformer(modelChecker);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		return transformer.canHandle(model, expression.getCondition());
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression)
	{
		// cannot handle steady state computation yet
		return !(ExpressionInspector.isSteadyStateReward(expression.getObjective()));
	}

	@Override
	protected ModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformModel(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		return transformModelOld(model, expression, statesOfInterest);
	}

	protected ModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformModelOld(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
					throws PrismException
	{
		// 1. create mode 1 == conditional part
		final TerminalTransformation<explicit.DTMC, explicit.DTMC> mode1 = transformer.transformModel(model, expression.getCondition(), statesOfInterest);
		getLog().println("Mode 1 has " + mode1.getTransformedModel().getNumStates() + " states");

		// 2. create mode 2 == submodel reachable from terminal states
		final Map<Integer, Integer> terminalLookup = mode1.getTerminalMapping();
		final DTMCRestricted mode2 = new DTMCRestricted(model, terminalLookup.values());
		getLog().println("Mode 2 has " + mode2.getNumStates() + " states");

		// 3. create union model
		final Map<Integer, Integer> identify = new HashMap<>(terminalLookup);
		for (Entry<Integer, Integer> id : terminalLookup.entrySet()) {
			identify.put(id.getKey(), mode2.mapStateToRestrictedModel(id.getValue()));
		}
		final explicit.DTMC transformedModel = DTMCDisjointUnion.DTMCUnion(mode1.getTransformedModel(), mode2, identify);
		ModelTransformation<explicit.DTMC, explicit.DTMC> union = new BasicModelTransformation<>(mode1.getTransformedModel(), transformedModel, mode1.getTransformedStatesOfInterest());
		ModelTransformation<explicit.DTMC, explicit.DTMC> nested = new ModelTransformationNested<>(mode1, union);

		return nested;
	}

	protected ModelTransformation<explicit.DTMC, ? extends explicit.DTMC> transformModelNew(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		// 1. create mode 1 == conditional part
		final TerminalTransformation<explicit.DTMC, explicit.DTMC> mode1 = transformer.transformModel(model, expression.getCondition(), statesOfInterest);
		getLog().println("Mode 1 has " + mode1.getTransformedModel().getNumStates() + " states");

		// 2. create mode 2 == submodel reachable from terminal states
		final Map<Integer, Integer> terminalLookup = mode1.getTerminalMapping();
		final DTMCRestricted mode2 = new DTMCRestricted(model, terminalLookup.values());
		getLog().println("Mode 2 has " + mode2.getNumStates() + " states");

		// 3. create union model
		DTMCDisjointUnion unionModel = new DTMCDisjointUnion(mode1.getTransformedModel(), mode2);
		BasicModelTransformation<explicit.DTMC, explicit.DTMC> union = new BasicModelTransformation<>(mode1.getTransformedModel(), unionModel, mode1.getTransformedStatesOfInterest());

		// 4. identify terminal states in mode1 and mode2
		Function<Entry<Integer, Integer>, BitSet> asEqClass = pair -> BitSetTools.asBitSet(pair.getKey(), mode2.mapStateToRestrictedModel(pair.getValue()) + unionModel.offset);
		FunctionalIterable<BitSet> identify = FunctionalIterable.extend(terminalLookup.entrySet()).map(asEqClass);

		BasicModelTransformation<explicit.DTMC, ? extends explicit.DTMC> equiv = DTMCEquiv.transform(unionModel, new EquivalenceRelationInteger(identify), DONT_NORMALIZE, RESTRICT);

		return equiv.compose(union).compose(mode1);
	}
}