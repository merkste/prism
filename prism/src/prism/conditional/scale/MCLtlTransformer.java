package prism.conditional.scale;

import jdd.JDD;
import jdd.JDDNode;
import acceptance.AcceptanceType;
import explicit.conditional.transformer.UndefinedTransformationException;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.LTLModelChecker.LTLProduct;
import prism.Model;
import prism.ModelTransformation;
import prism.ModelTransformationNested;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;
import prism.StateValuesMTBDD;
import prism.StochModel;
import prism.StochModelChecker;
import prism.conditional.ConditionalTransformer;
import prism.conditional.transformer.LtlProductTransformer;

//FIXME ALG: support PrismSettings.CONDITIONAL_SCALE_LTL_MINIMIZE
//FIXME ALG: add comment
public interface MCLtlTransformer<M extends ProbModel, C extends ProbModelChecker> extends ScaleTransformer<M, C>
{
	public static final AcceptanceType[] ACCEPTANCE_TYPES = AcceptanceType.allTypes();

	@Override
	default boolean canHandleCondition(Model model, ExpressionConditional expression) throws PrismLangException
	{
		return getLtlTransformer().canHandle(model, expression.getCondition());
	}

	@Override
	default boolean canHandleObjective(Model model, ExpressionConditional expression) throws PrismLangException
	{
		// Can handle all ExpressionQuant: P, R, S and L
		return true;
	}

	@Override
	default ModelTransformation<M, ? extends M> transformModel(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		Expression condition = expression.getCondition();

		// Build product model
		LtlProductTransformer<M> ltlTransformer = getLtlTransformer();
		LTLProduct<M> ltlProduct = ltlTransformer.transform(model, condition, statesOfInterest.copy(), ACCEPTANCE_TYPES);
		JDDNode accepting = ltlTransformer.findAcceptingStates(ltlProduct);

		C mcProduct = getModelChecker(ltlProduct.getProductModel());
		StateValues probsProduct = mcProduct.checkProbUntil(ltlProduct.getProductModel().getReach(), accepting, false);

		JDD.Deref(accepting);

		StateValuesMTBDD probsProductMTBDD = probsProduct.convertToStateValuesMTBDD();
		//probsProductMTBDD.print(prism.getLog());
		JDDNode probReachGoal = probsProductMTBDD.getJDDNode().copy();
		//StateValuesMTBDD.print(prism.getLog(), probReachGoal.copy(), ltlProduct.getProductModel(), "probReachGoal");
		probsProductMTBDD.clear();

		final JDDNode support = JDD.Apply(JDD.GREATERTHAN, probReachGoal.copy(), JDD.ZERO.copy());
		final boolean satisfiable = JDD.AreIntersecting(support, statesOfInterest);
		JDD.Deref(support);
		if (! satisfiable) {
			// FIXME ALG: Deref JDDNodes!
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

		MCScaledTransformation<M> scaledTransformation = new MCScaledTransformation<>(getModelChecker(), ltlProduct.getTransformedModel(), probReachGoal, statesOfInterest.copy());

		JDD.Deref(statesOfInterest);
		return new ModelTransformationNested<M, M, M>(ltlProduct, scaledTransformation);
	}



	public static class CTMC extends ConditionalTransformer.Basic<StochModel, StochModelChecker> implements MCLtlTransformer<StochModel, StochModelChecker>, ScaleTransformer.CTMC
	{
		public CTMC(StochModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}



	public static class DTMC extends ConditionalTransformer.Basic<ProbModel, ProbModelChecker> implements MCLtlTransformer<ProbModel, ProbModelChecker>, ScaleTransformer.DTMC
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}
}
