package explicit.conditional.transformer.legacy;

import java.util.BitSet;

import acceptance.AcceptanceType;
import parser.ast.ExpressionConditional;
import prism.PrismException;
import prism.PrismLangException;
import explicit.BasicModelTransformation;
import explicit.DTMC;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.LTLProductTransformer;
import explicit.conditional.transformer.mc.MCConditionalTransformer;

@Deprecated
public class MCLTLTransformer extends MCConditionalTransformer
{
	protected LTLProductTransformer<DTMC> ltlTransformer;
	protected MCUntilTransformer untilTransformer;

	public MCLTLTransformer(final DTMCModelChecker modelChecker) throws PrismException
	{
		super(modelChecker);
		ltlTransformer = new LTLProductTransformer<>(modelChecker);
		untilTransformer = new MCUntilTransformer(modelChecker);
	}

	@Override
	protected boolean canHandleCondition(final DTMC model, final ExpressionConditional expression) throws PrismLangException
	{
		return ltlTransformer.canHandle(model, expression.getCondition());
	}

	@Override
	protected boolean canHandleObjective(final DTMC model, final ExpressionConditional expression)
	{
		// cannot handle steady state computation yet
		return !(ExpressionInspector.isSteadyStateReward(expression.getObjective()));
	}

	@Override
	public ModelTransformation<DTMC, DTMC> transformModel(final DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		final LTLProduct<DTMC> ltlTransformation = ltlTransformer.transform(model, expression.getCondition(), statesOfInterest, AcceptanceType.RABIN);

		final Integer[] ltlMapping = new Integer[model.getNumStates()];
		for (Integer productState : ltlTransformation.getProductModel().getInitialStates()) {
			// get the state index of the corresponding state in the original model
			final Integer modelState = ltlTransformation.getModelState(productState);
			assert modelState != null : "first state should be set";
			assert ltlMapping[modelState] == null : "do not map state twice";
			ltlMapping[modelState] = productState;
		}

		final BitSet goal = ltlTransformer.findAcceptingStates(ltlTransformation);
		final BasicModelTransformation<DTMC, DTMC> untilTransformation = untilTransformer.transformModel(ltlTransformation.getProductModel(), goal);

		final Integer[] mapping = new Integer[model.getNumStates()];
		for (int state = 0; state < mapping.length; state++) {
			final Integer ltlState = ltlMapping[state];
			mapping[state] = (ltlState == null) ? null : untilTransformation.mapToTransformedModel(ltlState);
		}

		return new ModelTransformationNested<>(ltlTransformation, untilTransformation);
	}
}