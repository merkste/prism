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
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.conditional.transformer.TerminalTransformation;
import explicit.conditional.transformer.LTLProductTransformer;

public class ConditionalLTLTransformer extends PrismComponent
{
	// FIXME ALG: allow all acceptance types
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

		// 3. compose Transformations
		ModelTransformation<DTMC, DTMC> nested = new ModelTransformationNested<>(ltlProduct, reachabilityTransformation);

		// 4. create mapping of terminals from reachability model to original model
		final Map<Integer, Integer> terminalLookup = buildTerminalLookup(ltlProduct, reachabilityTransformation);

		return new TerminalTransformation<DTMC, DTMC>(nested, terminalLookup);
	}

	// FIXME ALG: similar code in ConditionalReachabilityTransformer, ConditionalNextTransformer
	/**
	 * Build Mapping terminal states in transformed model -> terminal states in original model
	 */
	public Map<Integer, Integer> buildTerminalLookup(final LTLProduct<DTMC> product, final TerminalTransformation<DTMC, DTMC> transformation)
	{
		final Map<Integer, Integer> reachabilityTerminalLookup = transformation.getTerminalMapping();

		final Map<Integer, Integer> terminalLookup = new HashMap<>(reachabilityTerminalLookup.size());
		for (Entry<Integer, Integer> entry : reachabilityTerminalLookup.entrySet()) {
			terminalLookup.put(entry.getKey(), product.getModelState(entry.getValue()));
		}
		return terminalLookup;
	}
}