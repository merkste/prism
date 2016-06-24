package prism.conditional;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import mtbdd.PrismMTBDD;
import parser.ast.Expression;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.PrismComponent;
import prism.PrismException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.StateValues;
import prism.conditional.SimplePathProperty.Until;
import prism.conditional.transform.LTLProductTransformer;

public interface ModelChecker<M extends ProbModel, MC extends StateModelChecker>
{
	MC getModelChecker();

	MC getModelChecker(M model) throws PrismException;

	default JDDNode computeStates(M model, Expression expression)
			throws PrismException
	{
		return getModelChecker(model).checkExpressionDD(expression, JDD.Constant(1));
	}

	default JDDNode computeSuccStar(M model, JDDNode states)
	{
		return PrismMTBDD.Reachability(model.getTransReln(), model.getAllDDRowVars(), model.getAllDDColVars(), states);
	}



	public static abstract class Basic<M extends ProbModel, MC extends StateModelChecker> extends PrismComponent implements ModelChecker<M, MC>
	{
		protected MC modelChecker;
		protected LTLProductTransformer<M> ltlTransformer;

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

		public LTLProductTransformer<M> getLtlTransformer()
		{
			if (ltlTransformer == null) {
				ltlTransformer = new LTLProductTransformer<M>(modelChecker);
			}
			return ltlTransformer;
		}
	}

	public static abstract class DTMC extends Basic<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		public JDDNode computeProb0(ProbModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0(model, until.getRemain(), until.getGoal());
			}
		}

		protected JDDNode computeProb0(ProbModel model, JDDNode remain, JDDNode goal)
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

		protected JDDNode computeProb1(ProbModel model, JDDNode remain, JDDNode goal)
		{
			JDDNode prob0 = computeProb0(model, remain, goal);
			JDDNode prob1 = computeProb1(model, remain, goal, prob0);
			JDD.Deref(prob0);
			return prob1;
		}

		public JDDNode computeProb1(ProbModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1(model, until.getRemain(), until.getGoal());
			}
		}

		protected JDDNode computeProb1(ProbModel model, JDDNode remain, JDDNode goal, JDDNode prob0)
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

		public JDDNode computeUntilProbs(ProbModel model, Until until) throws PrismException
		{
			return computeProb0(model, until.getRemain(), until.getGoal());
		}

		protected JDDNode computeUntilProbs(ProbModel model, JDDNode remain, JDDNode goal, boolean negated) throws PrismException
		{
			if (remain == null) {
				remain = model.getReach();
			}
			ProbModelChecker mc       = getModelChecker(model);
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

		public JDDNode computeProb0A(NondetModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1A(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0A(model, until.getRemain(), until.getGoal());
			}
		}

		protected JDDNode computeProb0A(NondetModel model, JDDNode remain, JDDNode goal)
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

		public JDDNode computeProb0E(NondetModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb1E(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb0E(model, until.getRemain(), until.getGoal());
			}
		}

		protected JDDNode computeProb0E(NondetModel model, JDDNode remain, JDDNode goal)
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

		public JDDNode computeProb1A(NondetModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0A(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1A(model, until.getRemain(), until.getGoal());
			}
		}

		protected JDDNode computeProb1A(NondetModel model, JDDNode remain, JDDNode goal)
		{
			JDDNode prob0E = computeProb0E(model, remain, goal);
			JDDNode prob1A = computeProb1A(model, remain, goal, prob0E);
			JDD.Deref(prob0E);
			return prob1A;
		}

		protected JDDNode computeProb1A(NondetModel model, JDDNode remain, JDDNode goal, JDDNode prob0E)
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

		public JDDNode computeProb1E(NondetModel model, Until until)
		{
			if (until.isNegated()) {
				return computeProb0E(model, until.getRemain(), until.getGoal());
			} else {
				return computeProb1E(model, until.getRemain(), until.getGoal());
			}
		}

		protected JDDNode computeProb1E(NondetModel model, JDDNode remain, JDDNode goal)
		{
			JDDNode prob0A = computeProb0A(model, remain, goal);
			JDDNode prob1E = computeProb1E(model, remain, goal, prob0A);
			JDD.Deref(prob0A);
			return prob1E;
		}

		protected JDDNode computeProb1E(NondetModel model, JDDNode remain, JDDNode goal, JDDNode prob0A)
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

		public JDDNode computeUntilMaxProbs(NondetModel model, Until until)
				throws PrismException
		{
			return computeUntilProbs(model, until.getRemain(), until.getGoal(), until.isNegated(), false);
		}

		public JDDNode computeUntilMinProbs(NondetModel model, Until until)
				throws PrismException
		{
			return computeUntilProbs(model, until.getRemain(), until.getGoal(), until.isNegated(), true);
		}

		protected JDDNode computeUntilProbs(NondetModel model, JDDNode remain, JDDNode goal, boolean negated, boolean minimum)
				throws PrismException
		{
			if (remain == null) {
				remain = model.getReach();
			}
			NondetModelChecker mc = getModelChecker(model);
			StateValues probabilities;
			if (negated) {
				// Pmax(¬φ) = 1 - Pmin(φ);
				// Pmin(¬φ) = 1 - Pmax(φ);
				probabilities = mc.checkProbUntil(remain, goal, false, !minimum);
				probabilities.subtractFromOne();
			} else {
				probabilities = mc.checkProbUntil(remain, goal, false, minimum);
			}
			return probabilities.convertToStateValuesMTBDD().getJDDNode();
		}
	}
}
