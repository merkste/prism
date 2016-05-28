package prism.conditional;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import mtbdd.PrismMTBDD;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.Model;
import prism.ModelTransformation;
import prism.ModelType;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.StateValues;
import prism.conditional.SimplePathProperty.Until;
import prism.PrismComponent;

public interface NewConditionalTransformer<M extends ProbModel, MC extends StateModelChecker>
{
	default String getName() {
		Class<?> type = this.getClass();
		type = type.getEnclosingClass() == null ? type : type.getEnclosingClass();
		return type.getSimpleName();
	}

	/**
	 * Test whether the transformer can handle a model and a conditional expression.
	 * 
	 * @return True iff this transformation type can handle the expression.
	 * @throws PrismLangException if the expression is broken
	 */
	@SuppressWarnings("unchecked")
	default boolean canHandle(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		return canHandleModelType(model)
		       && canHandleObjective((M) model, expression)
		       && canHandleCondition((M) model, expression);
	}

	boolean canHandleModelType(Model model);

	boolean canHandleObjective(M model,ExpressionConditional expression)
			throws PrismLangException;

	boolean canHandleCondition(M model,ExpressionConditional expression)
			throws PrismLangException;

	/**
	 * Throw an exception, iff the transformer cannot handle the model and expression.
	 */
	default void checkCanHandle(M model, ExpressionConditional expression) throws PrismException
	{
		if (! canHandle(model, expression)) {
			throw new PrismException("Cannot transform " + model.getModelType() + " for " + expression);
		}
	}

	public abstract ModelTransformation<M, M> transform(final M model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException;

	PrismLog getLog();

//	LTLProductTransformer<M> getLtlTransformer();

	MC getModelChecker();

	MC getModelChecker(M model) throws PrismException;

	default JDDNode computeStates(M model, Expression expression)
			throws PrismException
	{
		return getModelChecker(model).checkExpressionDD(expression, JDD.Constant(1));
	}



	public static abstract class Basic<M extends ProbModel, MC extends StateModelChecker> extends PrismComponent implements NewConditionalTransformer<M, MC>
	{
		protected MC modelChecker;
//		protected LTLProductTransformer<M> ltlTransformer;

		public Basic(MC modelChecker) {
			super(modelChecker);
			this.modelChecker = modelChecker;
		}

		@Override
		public MC getModelChecker()
		{
			return modelChecker;
		}

		@SuppressWarnings("unchecked")
		@Override
		public MC getModelChecker(M model) throws PrismException
		{
			// Create fresh model checker for model
			return (MC) modelChecker.createModelChecker(model);
		}

//		public LTLProductTransformer<M> getLtlTransformer()
//		{
//			if (ltlTransformer == null) {
//				ltlTransformer = new LTLProductTransformer<M>(modelChecker);
//			}
//			return ltlTransformer;
//		}
	}

	public static abstract class DTMC extends Basic<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.DTMC) && (model instanceof ProbModel);
		}

		public JDDNode computeProb0(ProbModel model, JDDNode remain, JDDNode goal)
		{
			if (remain == null) {
				remain = model.getReach();
			}
			JDDNode trans01 = model.getTrans01();
			JDDNode reach   = model.getReach();
			JDDVars rowVars = model.getAllDDRowVars();
			JDDVars colVars = model.getAllDDColVars();
			return PrismMTBDD.Prob0(trans01, reach, rowVars, colVars, remain, goal);
		}

		public JDDNode computeProb1(ProbModel model, JDDNode remain, JDDNode goal)
		{
			if (remain == null) {
				remain = model.getReach();
			}
			JDDNode prob0 = computeProb0(model, remain, goal);
			JDDNode prob1 = computeProb1(model, remain, goal, prob0);
			JDD.Deref(prob0);
			return prob1;
		}

		public JDDNode computeProb1(ProbModel model, JDDNode remain, JDDNode goal, JDDNode prob0)
		{
			if (remain == null) {
				remain = model.getReach();
			}
			JDDNode trans01 = model.getTrans01();
			JDDNode reach   = model.getReach();
			JDDVars rowVars = model.getAllDDRowVars();
			JDDVars colVars = model.getAllDDColVars();
			return PrismMTBDD.Prob1(trans01, reach, rowVars, colVars, remain, goal, prob0);
		}

		protected JDDNode computeUntilProbs(ProbModel model, Until until) throws PrismException
		{
			ProbModelChecker mc = getModelChecker(model);
			StateValues probabilities = mc.checkProbUntil(until.getRemain(), until.getGoal(), false);
			if (until.isNegated()) {
				probabilities.subtractFromOne();
			}
			return probabilities.convertToStateValuesMTBDD().getJDDNode();
		}

//		protected JDDNode computeUntilProbs(ProbModel model, JDDNode remain, JDDNode goal, boolean negated) throws PrismException
//		{
//			if (remain == null) {
//				remain = model.getReach();
//			}
//			ProbModelChecker mc = getModelChecker(model);
//			StateValues probabilities = mc.checkProbUntil(remain, goal, false);
//			if (negated) {
//				probabilities.subtractFromOne();
//			}
//			return probabilities.convertToStateValuesMTBDD().getJDDNode();
//		}
	}



	public static abstract class MDP extends Basic<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public boolean canHandleModelType(Model model)
		{
			return (model.getModelType() == ModelType.MDP) && (model instanceof NondetModel);
		}

		public JDDNode computeProb0A(NondetModel model, JDDNode remain, JDDNode goal)
		{
			if (remain == null) {
				remain = model.getReach();
			}
			JDDNode trans01    = model.getTrans01();
			JDDNode reach      = model.getReach();
			JDDVars rowVars    = model.getAllDDRowVars();
			JDDVars colVars    = model.getAllDDColVars();
			JDDVars nondetVars = model.getAllDDNondetVars();
			return PrismMTBDD.Prob0A(trans01, reach, rowVars, colVars, nondetVars, remain, goal);
		}

		public JDDNode computeProb0E(NondetModel model, JDDNode remain, JDDNode goal)
		{
			if (remain == null) {
				remain = model.getReach();
			}
			JDDNode trans01    = model.getTrans01();
			JDDNode reach      = model.getReach();
			JDDNode nondetMask = model.getNondetMask();
			JDDVars rowVars    = model.getAllDDRowVars();
			JDDVars colVars    = model.getAllDDColVars();
			JDDVars nondetVars = model.getAllDDNondetVars();
			return PrismMTBDD.Prob0E(trans01, reach, nondetMask, rowVars, colVars, nondetVars, remain, goal);
		}

		public JDDNode computeProb1A(NondetModel model, JDDNode remain, JDDNode goal)
		{
			if (remain == null) {
				remain = model.getReach();
			}
			JDDNode trans01    = model.getTrans01();
			JDDNode reach      = model.getReach();
			JDDNode nondetMask = model.getNondetMask();
			JDDVars rowVars    = model.getAllDDRowVars();
			JDDVars colVars    = model.getAllDDColVars();
			JDDVars nondetVars = model.getAllDDNondetVars();

			JDDNode no     = PrismMTBDD.Prob0E(trans01, reach, nondetMask, rowVars, colVars, nondetVars, remain, goal);
			JDDNode prob1A = PrismMTBDD.Prob1A(trans01, reach, nondetMask, rowVars, colVars, nondetVars, no, goal);
			JDD.Deref(no);
			return prob1A;
		}

		public JDDNode computeProb1E(NondetModel model, JDDNode remain, JDDNode goal)
		{
			if (remain == null) {
				remain = model.getReach();
			}
			JDDNode trans01    = model.getTrans01();
			JDDNode reach      = model.getReach();
			JDDVars rowVars    = model.getAllDDRowVars();
			JDDVars colVars    = model.getAllDDColVars();
			JDDVars nondetVars = model.getAllDDNondetVars();

			JDDNode no     = PrismMTBDD.Prob0A(trans01, reach, rowVars, colVars, nondetVars, remain, goal);
			JDDNode prob1E = PrismMTBDD.Prob1E(trans01, reach, rowVars, colVars, nondetVars, remain, goal, no);
			JDD.Deref(no);
			return prob1E;
		}

		public JDDNode computeUntilMaxProbs(NondetModel model, Until until)
				throws PrismException
		{
			NondetModelChecker mc = getModelChecker(model);
			StateValues probabilities;
			if (until.isNegated()) {
				// Pmax(¬φ) = 1 - Pmin(φ);
				probabilities = mc.checkProbUntil(until.getRemain(), until.getGoal(), false, true);
				probabilities.subtractFromOne();
			} else {
				probabilities = mc.checkProbUntil(until.getRemain(), until.getGoal(), false, false);
			}
			return probabilities.convertToStateValuesMTBDD().getJDDNode();
		}

//		public JDDNode computeUntilMaxProbs(NondetModel model, JDDNode remain, JDDNode goal, boolean negated)
//				throws PrismException
//		{
//			if (remain == null) {
//				remain = model.getReach();
//			}
//			NondetModelChecker mc = getModelChecker(model);
//			StateValues probabilities;
//			if (negated) {
//				// Pmax(¬φ) = 1 - Pmin(φ);
//				probabilities = mc.checkProbUntil(remain, goal, false, true);
//				probabilities.subtractFromOne();
//			} else {
//				probabilities = mc.checkProbUntil(remain, goal, false, false);
//			}
//			return probabilities.convertToStateValuesMTBDD().getJDDNode();
//		}
	}
}
