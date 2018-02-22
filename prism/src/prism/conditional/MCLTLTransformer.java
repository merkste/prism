package prism.conditional;

import jdd.JDD;
import jdd.JDDNode;
import acceptance.AcceptanceType;
import explicit.conditional.transformer.UndefinedTransformationException;
import parser.ast.ExpressionConditional;
import prism.LTLModelChecker.LTLProduct;
import prism.Model;
import prism.ModelExpressionTransformation;
import prism.ModelTransformationNested;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;
import prism.StateValuesMTBDD;
import prism.StochModel;
import prism.StochModelChecker;
import prism.conditional.transform.BasicModelExpressionTransformation;

// FIXME ALG: support PrismSettings.CONDITIONAL_SCALE_LTL_MINIMIZE
public abstract class MCLTLTransformer<M extends ProbModel, C extends ProbModelChecker> extends NewConditionalTransformer.MC<M, C>
{
	public MCLTLTransformer(Prism prism, C modelChecker)
	{
		super(prism, modelChecker);
	}

	@Override
	public boolean canHandleCondition(Model model, ExpressionConditional expression) throws PrismLangException
	{
		return getLtlTransformer().canHandle(model, expression.getCondition());
	}

	@Override
	public boolean canHandleObjective(Model model, ExpressionConditional expression) throws PrismLangException
	{
		// Can handle all ExpressionQuant: P, R, S and L
		return true;
	}

	@Override
	public ModelExpressionTransformation<M, ? extends M> transform(
			M model,
			ExpressionConditional expression,
			JDDNode statesOfInterest)
			throws PrismException {

		// FIXME ALG: allow all acceptance types
		AcceptanceType[] allowedAcceptance = {
				AcceptanceType.REACH,
				AcceptanceType.RABIN,
				AcceptanceType.GENERIC
		};

		LTLProduct<M> ltlProduct = ltlTransformer.transform(model, expression.getCondition(), statesOfInterest, allowedAcceptance);
		JDDNode accepting = getLtlTransformer().findAcceptingStates(ltlProduct);

		prism.getLog().println("\nComputing reachability probabilities...");
		ProbModelChecker mcProduct = modelChecker.createNewModelChecker(prism, ltlProduct.getProductModel(), null);
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

		MCScaledTransformation<M> scaledTransformation = new MCScaledTransformation<>(prism, ltlProduct.getTransformedModel(), probReachGoal, statesOfInterest.copy());

		JDD.Deref(statesOfInterest);
		ModelTransformationNested<M, M, M> transformation = new ModelTransformationNested<M, M, M>(ltlProduct, scaledTransformation);
		return new BasicModelExpressionTransformation<>(transformation, expression, expression.getObjective());
	}



	public static class CTMC extends MCLTLTransformer<StochModel, StochModelChecker> implements NewConditionalTransformer.CTMC
	{
		public CTMC(Prism prism, StochModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}

		@Override
		public boolean canHandle(Model model, ExpressionConditional expression)
		{
			return false;
		}
	}



	public static class DTMC extends MCLTLTransformer<ProbModel, ProbModelChecker> implements NewConditionalTransformer.DTMC
	{
		public DTMC(Prism prism, ProbModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}
	}
}
