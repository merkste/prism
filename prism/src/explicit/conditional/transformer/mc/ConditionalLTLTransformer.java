package explicit.conditional.transformer.mc;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import acceptance.AcceptanceType;
import parser.ast.Expression;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.conditional.transformer.TerminalTransformation;
import explicit.conditional.transformer.LTLProductTransformer;

public class ConditionalLTLTransformer extends PrismComponent
{
	private static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.RABIN, AcceptanceType.STREETT, AcceptanceType.GENERIC};

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

	public TerminalTransformation<DTMC, DTMC> transformModel(final DTMC model, final Expression ltlFormula, final BitSet statesOfInterest)
			throws PrismException
	{
		// 1. create LTL product
		final LTLProduct<DTMC> ltlProduct = ltlTransformer.transform(model, ltlFormula, statesOfInterest, ACCEPTANCE_TYPES);
		final BitSet goal = ltlTransformer.findAcceptingStates(ltlProduct);

		// 2. create reachability transformation
		final BitSet ltlStatesOfInterest = ltlProduct.getTransformedStatesOfInterest();
		final TerminalTransformation<DTMC, DTMC> reachabilityTransformation = reachabilityTransformer.transformModel(ltlProduct.getProductModel(),
				null, goal, ltlStatesOfInterest);

		// 3. create mapping from original to reachability model
		final Integer[] mapping = buildMapping(ltlProduct, reachabilityTransformation);

		// 4. create mapping of terminals from reachability model to original model
		final Map<Integer, Integer> terminalLookup = buildTerminalLookup(ltlProduct, reachabilityTransformation);

		return new TerminalTransformation<DTMC, DTMC>(model, reachabilityTransformation.getTransformedModel(), mapping, terminalLookup);
	}

	public Integer[] buildMapping(final LTLProduct<DTMC> ltlProduct, final TerminalTransformation<DTMC, DTMC> terminalTransformation)
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

	// FIXME ALG: similar code in ConditionalReachabilityTransformer, ConditionalNextTransformer
	public Map<Integer, Integer> buildTerminalLookup(final LTLProduct<DTMC> product, final TerminalTransformation<DTMC, DTMC> terminalTransformation)
	{
		final Map<Integer, Integer> reachabilityTerminalLookup = terminalTransformation.getTerminalMapping();

		final Map<Integer, Integer> terminalLookup = new HashMap<>(reachabilityTerminalLookup.size());
		for (Entry<Integer, Integer> entry : reachabilityTerminalLookup.entrySet()) {
			terminalLookup.put(entry.getKey(), product.getModelState(entry.getValue()));
		}
		return terminalLookup;
	}
}