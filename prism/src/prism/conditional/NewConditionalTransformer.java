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
import prism.PrismComponent;

public interface NewConditionalTransformer<M extends Model, MC extends StateModelChecker>
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

	JDDNode computeProb0A(M model, JDDNode remain, JDDNode goal);

	JDDNode computeProb0E(M model, JDDNode remain, JDDNode goal);

	JDDNode computeProb1A(M model, JDDNode remain, JDDNode goal);

	JDDNode computeProb1E(M model, JDDNode remain, JDDNode goal);

	JDDNode computeUntilMaxProbs(NondetModel model, JDDNode remain, JDDNode goal, boolean negated)
			throws PrismException;




	public static abstract class Basic<M extends Model, MC extends StateModelChecker> extends PrismComponent implements NewConditionalTransformer<M, MC>
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

		@Override
		public JDDNode computeProb0A(ProbModel model, JDDNode remain, JDDNode goal)
		{
			return computeProb0(model, remain, goal);
		}

		@Override
		public JDDNode computeProb0E(ProbModel model, JDDNode remain, JDDNode goal)
		{
			return computeProb0(model, remain, goal);
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

		@Override
		public JDDNode computeProb1A(ProbModel model, JDDNode remain, JDDNode goal)
		{
			return computeProb1(model, remain, goal);
		}

		@Override
		public JDDNode computeProb1E(ProbModel model, JDDNode remain, JDDNode goal)
		{
			return computeProb1(model, remain, goal);
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

		public JDDNode computeUntilMaxProbs(NondetModel model, JDDNode remain, JDDNode goal, boolean negated)
				throws PrismException
		{
			ProbModelChecker mc = getModelChecker(model);
			StateValues probabilities = mc.checkProbUntil(remain, goal, false);
			if (negated) {
				probabilities.subtractFromOne();
			}
			return probabilities.convertToStateValuesMTBDD().getJDDNode();
		}
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

		@Override
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

		@Override
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

		@Override
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

//			JDDNode no     = JDD.And(reach.copy(), JDD.Not(JDD.Or(remain.copy(), goal.copy())));
//			// min
//			if (min) {
//				// no: "min prob = 0" equates to "there exists an adversary prob equals 0"
//				no = PrismMTBDD.Prob0E(tr01, reach, nondetMask, allDDRowVars, allDDColVars, allDDNondetVars, b1, b2);
//			}
//			// max
//			else {
//				// no: "max prob = 0" equates to "for all adversaries prob equals 0"
//				no = PrismMTBDD.Prob0A(tr01, reach, allDDRowVars, allDDColVars, allDDNondetVars, b1, b2);
//			}
			JDDNode no     = PrismMTBDD.Prob0E(trans01, reach, nondetMask, rowVars, colVars, nondetVars, remain, goal);
			JDDNode prob1A = PrismMTBDD.Prob1A(trans01, reach, nondetMask, rowVars, colVars, nondetVars, no, goal);
			JDD.Deref(no);
			return prob1A;
		}

		@Override
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

		public JDDNode computeUntilMaxProbs(NondetModel model, JDDNode remain, JDDNode goal, boolean negated)
				throws PrismException
		{
			if (remain == null) {
				remain = model.getReach();
			}
			NondetModelChecker mc = getModelChecker(model);
			StateValues probabilities;
			if (negated) {
				// Pmax(¬φ) = 1 - Pmin(φ);
				probabilities = mc.checkProbUntil(remain, goal, false, true);
				probabilities.subtractFromOne();
			} else {
				probabilities = mc.checkProbUntil(remain, goal, false, false);
			}
			return probabilities.convertToStateValuesMTBDD().getJDDNode();
		}
	}
}
