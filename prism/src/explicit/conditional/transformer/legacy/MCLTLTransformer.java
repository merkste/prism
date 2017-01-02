package explicit.conditional.transformer.legacy;

import java.util.BitSet;

import acceptance.AcceptanceType;
import parser.ast.ExpressionConditional;
import prism.PrismException;
import prism.PrismLangException;
import explicit.BasicModelTransformation;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.Model;
import explicit.ModelTransformation;
import explicit.ModelTransformationNested;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.LTLProductTransformer;
import explicit.conditional.transformer.mc.MCConditionalTransformer;

@Deprecated
public class MCLTLTransformer extends MCConditionalTransformer
{
	protected LTLProductTransformer<explicit.DTMC> ltlTransformer;
	protected MCUntilTransformer untilTransformer;

	public MCLTLTransformer(final DTMCModelChecker modelChecker) throws PrismException
	{
		super(modelChecker);
		ltlTransformer = new LTLProductTransformer<>(modelChecker);
		untilTransformer = new MCUntilTransformer(modelChecker);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		return ltlTransformer.canHandle(model, expression.getCondition());
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression)
	{
		// cannot handle steady state computation yet
		return !(ExpressionInspector.isSteadyStateReward(expression.getObjective()));
	}

	@Override
	public ModelTransformation<explicit.DTMC, explicit.DTMC> transformModel(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		final LTLProduct<explicit.DTMC> ltlTransformation = ltlTransformer.transform(model, expression.getCondition(), statesOfInterest, AcceptanceType.RABIN);

		final Integer[] ltlMapping = new Integer[model.getNumStates()];
		for (Integer productState : ltlTransformation.getProductModel().getInitialStates()) {
			// get the state index of the corresponding state in the original model
			final Integer modelState = ltlTransformation.getModelState(productState);
			assert modelState != null : "first state should be set";
			assert ltlMapping[modelState] == null : "do not map state twice";
			ltlMapping[modelState] = productState;
		}

		final BitSet goal = ltlTransformer.findAcceptingStates(ltlTransformation);
		final BasicModelTransformation<explicit.DTMC, explicit.DTMC> untilTransformation = untilTransformer.transformModel(ltlTransformation.getProductModel(), goal);

		return new ModelTransformationNested<>(ltlTransformation, untilTransformation);
	}
}