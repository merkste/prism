package prism.conditional;

import jdd.JDD;
import jdd.JDDNode;
import acceptance.AcceptanceType;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.UndefinedTransformationException;
import parser.ast.ExpressionConditional;
import prism.LTLModelChecker;
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
import prism.conditional.transform.BasicModelExpressionTransformation;

// FIXME ALG: support PrismSettings.CONDITIONAL_SCALE_LTL_MINIMIZE
public class MCLTLTransformer extends MCConditionalTransformer {

	public MCLTLTransformer(ProbModelChecker modelChecker, Prism prism) {
		super(modelChecker, prism);
	}

	@Override
	public boolean canHandleCondition(Model model, ExpressionConditional expression) throws PrismLangException
	{
		return getLtlTransformer().canHandle(model, expression.getCondition());
	}

	@Override
	public boolean canHandleObjective(Model model, ExpressionConditional expression) throws PrismLangException
	{
		// cannot handle steady state computation yet
		return !(ExpressionInspector.isSteadyStateReward(expression.getObjective()));
	}

	@Override
	public ModelExpressionTransformation<ProbModel, ? extends ProbModel> transform(
			ProbModel model,
			ExpressionConditional expression,
			JDDNode statesOfInterest)
			throws PrismException {

		// FIXME ALG: allow all acceptance types
		AcceptanceType[] allowedAcceptance = {
				AcceptanceType.REACH,
				AcceptanceType.RABIN,
				AcceptanceType.GENERIC
		};

		LTLModelChecker ltlMC = new LTLModelChecker(prism);
		LTLModelChecker.LTLProduct<ProbModel> ltlProduct = ltlMC.constructProductMC(modelChecker, model, expression.getCondition(), statesOfInterest.copy(), allowedAcceptance);

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
			throw new UndefinedTransformationException("condition is not satisfiable");
		}

		MCScaledTransformation scaledTransformation = new MCScaledTransformation(prism, ltlProduct.getTransformedModel(), probReachGoal, statesOfInterest.copy());

		JDD.Deref(statesOfInterest);
		ModelTransformationNested<ProbModel, ProbModel, ProbModel> transformation = new ModelTransformationNested<ProbModel, ProbModel, ProbModel>(ltlProduct, scaledTransformation);
		return new BasicModelExpressionTransformation<>(transformation, expression, expression.getObjective());
	}
}
