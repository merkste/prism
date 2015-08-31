package explicit.conditional.transformer;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import common.BitSetTools;
import acceptance.AcceptanceType;
import parser.ast.Expression;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.conditional.LTLProductTransformer;

public class ConditionalLTLTransformer extends PrismComponent
{
	private final LTLProductTransformer<DTMC> ltlTransformer;
	private final ConditionalReachabilityTransformer reachabilityTransformer;

	public ConditionalLTLTransformer(final DTMCModelChecker modelChecker) throws PrismException
	{
		super(modelChecker);
		ltlTransformer = new LTLProductTransformer<DTMC>(modelChecker);
		reachabilityTransformer = new ConditionalReachabilityTransformer(modelChecker);
	}

	public boolean canHandle(final DTMC model, final Expression expression) throws PrismLangException
	{
		return ltlTransformer.canHandle(model, expression);
	}

	public ConditionalTerminalTransformation<DTMC, DTMC> transformModel(final DTMC model, final Expression ltlFormula, final BitSet statesOfInterest)
			throws PrismException
	{
		// 1. create LTL product
		final LTLProduct<DTMC> ltlProduct = ltlTransformer.transform(model, ltlFormula, statesOfInterest, AcceptanceType.RABIN);
		final BitSet goal = ltlTransformer.getGoalStates(ltlProduct);

		// 2. create reachability transformation
		// FIXME ALG: how to handle initial states ?
		// FIXME ALG: assumes initial states == states of interest
		final BitSet ltlStatesOfInterest = BitSetTools.asBitSet(ltlProduct.getProductModel().getInitialStates());
		final ConditionalTerminalTransformation<DTMC, DTMC> reachabilityTransformation = reachabilityTransformer.transformModel(ltlProduct.getProductModel(),
				null, goal, ltlStatesOfInterest);

		// 3. create mapping from original to reachability model
		final Integer[] mapping = buildMapping(ltlProduct, reachabilityTransformation);

		// 4. create mapping of terminals from reachability model to original model
		final Map<Integer, Integer> terminalLookup = buildTerminalLookup(ltlProduct, reachabilityTransformation);

		return new ConditionalTerminalTransformation<DTMC, DTMC>(model, reachabilityTransformation.getTransformedModel(), mapping, terminalLookup);
	}

	public Integer[] buildMapping(final LTLProduct<DTMC> ltlProduct, final ConditionalTerminalTransformation<DTMC, DTMC> terminalTransformation)
	{
		final Integer[] ltlMapping = buildLTLMapping(ltlProduct);

		// FIXME ALG: consider mapping impl as fn instead of array, move to DMTCRestricted ?
		final Integer[] mapping = new Integer[ltlProduct.getOriginalModel().getNumStates()];
		for (int state = 0; state < mapping.length; state++) {
			final Integer ltlState = ltlMapping[state];
			if (ltlState != null) {
				mapping[state] = terminalTransformation.mapToTransformedModel(ltlState);
			}
		}
		return mapping;
	}

	public Integer[] buildLTLMapping(final LTLProduct<DTMC> ltlProduct)
	{
		final Integer[] ltlMapping = new Integer[ltlProduct.getOriginalModel().getNumStates()];

		for (Integer productState : ltlProduct.getProductModel().getInitialStates()) {
			// get the state index of the corresponding state in the original model
			final Integer modelState = ltlProduct.getModelState(productState);
			assert ltlMapping[modelState] == null : "do not map state twice";
			ltlMapping[modelState] = productState;
		}
		return ltlMapping;
	}

	// FIXME ALG: code duplication, see ConditionalLTLTransformer
	public Map<Integer, Integer> buildTerminalLookup(final LTLProduct<DTMC> product, final ConditionalTerminalTransformation<DTMC, DTMC> terminalTransformation)
	{
		final Map<Integer, Integer> reachabilityTerminalLookup = terminalTransformation.getTerminalMapping();

		final Map<Integer, Integer> terminalLookup = new HashMap<>(reachabilityTerminalLookup.size());
		for (Entry<Integer, Integer> entry : reachabilityTerminalLookup.entrySet()) {
			terminalLookup.put(entry.getKey(), product.getModelState(entry.getValue()));
		}
		return terminalLookup;
	}
}