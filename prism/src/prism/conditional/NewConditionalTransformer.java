package prism.conditional;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import mtbdd.PrismMTBDD;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.Model;
import prism.ModelTransformation;
import prism.ModelType;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.StateValues;
import prism.conditional.SimplePathProperty.Until;
import prism.conditional.transform.LTLProductTransformer;
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
	default boolean canHandle(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		return canHandleModelType(model)
		       && canHandleObjective(model, expression)
		       && canHandleCondition(model, expression);
	}

	boolean canHandleModelType(Model model);

	default boolean canHandleObjective(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		// can handle probabilities only
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		OpRelOpBound oprel = objective.getRelopBoundInfo(getModelChecker().getConstantValues());
		// can handle maximal probabilities only
		return oprel.getMinMax(model.getModelType()).isMax();
	}

	boolean canHandleCondition(Model model,ExpressionConditional expression)
			throws PrismLangException;

	/**
	 * Throw an exception, iff the transformer cannot handle the model and expression.
	 */
	default void checkCanHandle(Model model, ExpressionConditional expression) throws PrismException
	{
		if (! canHandle(model, expression)) {
			throw new PrismException("Cannot transform " + model.getModelType() + " for " + expression);
		}
	}

	ModelTransformation<M, M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException;

	PrismLog getLog();

	MC getModelChecker();

	@SuppressWarnings("unchecked")
	default MC getModelChecker(M model) throws PrismException
	{
		// Create fresh model checker for model
		return (MC) getModelChecker().createModelChecker(model);
	}

	default LTLProductTransformer<M> getLtlTransformer()
	{
		return new LTLProductTransformer<M>(getModelChecker());
	}



	public static abstract class Basic<M extends ProbModel, MC extends StateModelChecker> extends PrismComponent implements NewConditionalTransformer<M, MC>
	{
		protected MC modelChecker;

		public Basic(MC modelChecker) {
			super(modelChecker);
			this.modelChecker = modelChecker;
		}

		@Override
		public MC getModelChecker()
		{
			return modelChecker;
		}
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

		public JDDNode computeProb0(ProbModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0(model, until.getRemain(), until.getGoal());
			}
		}

		public JDDNode computeProb1(ProbModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1(model, until.getRemain(), until.getGoal());
			}
		}

		public JDDNode computeUntilProbs(ProbModel model, Until until) throws PrismException
		{
			ProbModelChecker mc = getModelChecker(model);
			StateValues probabilities = mc.checkProbUntil(until.getRemain(), until.getGoal(), false);
			if (until.isNegated()) {
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
			JDDNode prob0E = computeProb0E(model, remain, goal);
			JDDNode prob1A = computeProb1A(model, remain, goal, prob0E);
			JDD.Deref(prob0E);
			return prob1A;
		}

		public JDDNode computeProb1A(NondetModel model, JDDNode remain, JDDNode goal, JDDNode prob0E)
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
			return PrismMTBDD.Prob1A(trans01, reach, nondetMask, rowVars, colVars, nondetVars, prob0E, goal);
		}

		public JDDNode computeProb1E(NondetModel model, JDDNode remain, JDDNode goal)
		{
			JDDNode prob0A = computeProb0A(model, remain, goal);
			JDDNode prob1E = computeProb1E(model, remain, goal, prob0A);
			JDD.Deref(prob0A);
			return prob1E;
		}

		public JDDNode computeProb1E(NondetModel model, JDDNode remain, JDDNode goal, JDDNode prob0A)
		{
			if (remain == null) {
				remain = model.getReach();
			}
			JDDNode trans01    = model.getTrans01();
			JDDNode reach      = model.getReach();
			JDDVars rowVars    = model.getAllDDRowVars();
			JDDVars colVars    = model.getAllDDColVars();
			JDDVars nondetVars = model.getAllDDNondetVars();
			return PrismMTBDD.Prob1E(trans01, reach, rowVars, colVars, nondetVars, remain, goal, prob0A);
		}

		public JDDNode computeProb0E(NondetModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1E(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0E(model, until.getRemain(), until.getGoal());
			}
		}

		public JDDNode computeProb0A(NondetModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1A(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0A(model, until.getRemain(), until.getGoal());
			}
		}

		public JDDNode computeProb1E(NondetModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0E(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1E(model, until.getRemain(), until.getGoal());
			}
		}

		public JDDNode computeProb1A(NondetModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0A(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1A(model, until.getRemain(), until.getGoal());
			}
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

		public JDDNode computeUntilMinProbs(NondetModel model, Until until)
				throws PrismException
		{
			NondetModelChecker mc = getModelChecker(model);
			StateValues probabilities;
			if (until.isNegated()) {
				// Pmin(¬φ) = 1 - Pmax(φ);
				probabilities = mc.checkProbUntil(until.getRemain(), until.getGoal(), false, false);
				probabilities.subtractFromOne();
			} else {
				probabilities = mc.checkProbUntil(until.getRemain(), until.getGoal(), false, true);
			}
			return probabilities.convertToStateValuesMTBDD().getJDDNode();
		}
	}
}
