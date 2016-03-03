package prism.conditional;

import jdd.JDD;
import jdd.JDDNode;
import acceptance.AcceptanceType;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.UndefinedTransformationException;
import acceptance.AcceptanceReachDD;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.LTLModelChecker;
import prism.ModelTransformation;
import prism.ModelTransformationNested;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;
import prism.StateValuesMTBDD;

public class MCLTLTransformer extends MCConditionalTransformer {

	public MCLTLTransformer(ProbModelChecker modelChecker, Prism prism) {
		super(modelChecker, prism);
	}

	@Override
	protected boolean canHandleCondition(ProbModel model, ExpressionConditional expression) throws PrismLangException
	{
		final Expression condition = expression.getCondition();
		return LTLModelChecker.isSupportedLTLFormula(model.getModelType(), condition);
	}

	@Override
	protected boolean canHandleObjective(ProbModel model, ExpressionConditional expression) throws PrismLangException
	{
		// cannot handle steady state computation yet
		return !(ExpressionInspector.isSteadyStateReward(expression.getObjective()));
	}

	@Override
	public ModelTransformation<ProbModel, ProbModel> transform(
			ProbModel model,
			ExpressionConditional expression,
			JDDNode statesOfInterest)
			throws PrismException {

		AcceptanceType[] allowedAcceptance = {
				AcceptanceType.REACH,
				AcceptanceType.RABIN,
				AcceptanceType.GENERIC
		};

		LTLModelChecker ltlMC = new LTLModelChecker(prism);
		LTLModelChecker.LTLProduct<ProbModel> ltlProduct = ltlMC.constructProductMC(modelChecker, model, expression.getCondition(), statesOfInterest.copy(), allowedAcceptance);

		JDDNode accepting;
		if (ltlProduct.getAcceptance().getType().equals(AcceptanceType.REACH)) {
			prism.getLog().println("\nSkipping BSCC computation...");
			accepting = ((AcceptanceReachDD)ltlProduct.getAcceptance()).getGoalStates();
		} else {
			prism.getLog().println("\nComputing accepting BSCCs...");
			accepting = ltlMC.findAcceptingBSCCs(ltlProduct.getAcceptance(), ltlProduct.getProductModel());
			//StateValuesMTBDD.print(prism.getLog(), acceptingBSCCs.copy(), ltlProduct.getProductModel(), "acceptingBSCC");
		}
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
		return new ModelTransformationNested<ProbModel, ProbModel, ProbModel>(ltlProduct, scaledTransformation);
	}
}
