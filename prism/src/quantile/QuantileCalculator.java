package quantile;

import java.util.List;

import jdd.Clearable;
import jdd.JDD;
import jdd.JDDNode;
import jdd.TemporaryJDDRefs;
import parser.ast.Expression;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionQuantileProbNormalForm;
import parser.ast.ExpressionTemporal;
import parser.ast.RelOp;
import parser.ast.TemporalOperatorBound;
import prism.IntegerBound;
import prism.Model;
import prism.ModelType;
import prism.NondetModel;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.StateModelChecker;
import prism.StateValues;
import prism.StochModel;

public abstract class QuantileCalculator extends PrismComponent implements Clearable {
	protected QuantileCalculatorContext qcc;
	protected ReachabilityQuantile q;

	public QuantileCalculator(PrismComponent parent, StateModelChecker mc, Model model, JDDNode stateRewards, JDDNode transRewards, JDDNode goalStates, JDDNode remainStates) throws PrismException
	{
		super(parent);
		qcc = new QuantileCalculatorContext(this, model, mc, stateRewards, transRewards, goalStates, remainStates);
	}
	
	private void setReachabilityQuantile(ReachabilityQuantile q) {
		this.q = q;
	}

	public void clear()
	{
		qcc.clear();
	}

	protected JDDNode getStatesFromTransition(JDDNode tr) {
		JDDNode tr01_r = JDD.GreaterThan(tr, 0.0);
		if (qcc.getModel().getModelType() == ModelType.MDP) {
			tr01_r = JDD.ThereExists(tr01_r, ((NondetModel)qcc.getModel()).getAllDDNondetVars());
		}
		JDDNode states_with_tr_r = JDD.ThereExists(tr01_r, qcc.getModel().getAllDDColVars());
		states_with_tr_r = JDD.And(states_with_tr_r, qcc.getModel().getReach().copy());

		return states_with_tr_r;
	}

	/**
	 * <br>[DEREFS: tr, REFS: <i>result</i>]
	 */
	protected JDDNode thereExistsChoiceStates(JDDNode tr, NondetModel ndModel) {
		JDDNode tr01 = JDD.GreaterThan(tr, 0.0);
		JDDNode states_with_tr = JDD.ThereExists(tr01, ndModel.getAllDDNondetVars());
		states_with_tr = JDD.ThereExists(states_with_tr, ndModel.getAllDDColVars());
		states_with_tr = JDD.And(states_with_tr, ndModel.getReach().copy());
		
		return states_with_tr;
	}

	public abstract StateValues iteration(JDDNode statesOfInterest, RelOp relOp, List<Double> thresholdsP, int result_adjustment) throws PrismException;
	public abstract JDDNode stepZeroReward(final JDDNode xTau, final JDDNode tauStates, final int i, boolean min) throws PrismException;

	/** Compute the values x_s,bound */
	public abstract StateValues computeForBound(int bound) throws PrismException;

	public static StateValues checkExpressionQuantile(PrismComponent parent, StateModelChecker mc, Model model, ExpressionQuantileProbNormalForm expr, JDDNode statesOfInterest) throws PrismException {
		// restrict statesOfInterest to the reachable part of the model
		statesOfInterest = JDD.And(statesOfInterest, model.getReach().copy());

		// try-with-resource: activeRefs, hold the currenty active JDDNodes
		// on exception or on leaving the try block, they will be automaticall derefed
		try (TemporaryJDDRefs activeRefs = new TemporaryJDDRefs()) {
			activeRefs.register(statesOfInterest);

			expr = expr.deepCopy();
			expr.findAllBoundQuantileVariables();

			if (model.getModelType() == ModelType.DTMC) {
				throw new PrismException("Symbolic quantiles currently only for MDP and CTMC.");
			}

			Object rs = expr.getRewardStructIndex();

			JDDNode stateRewards = null;
			JDDNode transRewards = null;

			if (rs != null && model.getModelType() == ModelType.CTMC) {
				throw new PrismNotSupportedException("Quantile computations with reward bounds are not supported for CTMCs");
			}

			int i;
			// get reward info
			if (rs == null) {
				// step 
				stateRewards = JDD.Constant(1.0);
				transRewards = JDD.Constant(0.0);
			} else {
				if (model.getNumRewardStructs() == 0) {
					throw new PrismException("Model has no rewards specified");
				} else if (rs instanceof Expression) {
					i = ((Expression) rs).evaluateInt(mc.getConstantValues());
					rs = new Integer(i); // for better error reporting below
					stateRewards = model.getStateRewards(i - 1).copy();
					transRewards = model.getTransRewards(i - 1).copy();
				} else if (rs instanceof String) {
					stateRewards = model.getStateRewards((String) rs).copy();
					transRewards = model.getTransRewards((String) rs).copy();
				} else {
					throw new PrismException("Unknown reward info "+rs.getClass());
				}
			}

			activeRefs.register(stateRewards);
			activeRefs.register(transRewards);

			if (stateRewards == null || transRewards == null) {
				throw new PrismException("Invalid reward structure index \"" + rs + "\"");
			}

			RelOp relP = expr.getProbabilityRelation();
			List<Double> thresholdsP = expr.getProbabilityThresholds(mc.getConstantValues());

			if (thresholdsP.size()==0) {
				throw new PrismException("At least one threshold is needed for quantile: "+expr);
			}

			if (!relP.isStrict()) {
				throw new PrismNotSupportedException("Non-strict probability bounds not yet supported for symbolic quantile");
			}

			ExpressionTemporal pathFormula = Expression.getTemporalOperatorForSimplePathFormula(expr.getInnerFormula().getExpression());
			if (pathFormula.getOperator() != ExpressionTemporal.P_F &&
			    pathFormula.getOperator() != ExpressionTemporal.P_U) {
				throw new PrismNotSupportedException("Only support F and U in quantile");
			}

			TemporalOperatorBound rewardBound = pathFormula.getBounds().getBounds().get(0);
			if (rewardBound.hasLowerBound() && rewardBound.hasUpperBound()) {
				throw new PrismLangException("Can not have upper and lower bound in quantile");
			}

			if (!rewardBound.hasLowerBound() && !rewardBound.hasUpperBound()) {
				// TODO: handle
				throw new PrismLangException("Trivial case: no reward bound");
			}

			boolean rewardBoundLower = rewardBound.hasLowerBound();
			boolean rewardBoundStrict = rewardBoundLower? rewardBound.lowerBoundIsStrict() : rewardBound.upperBoundIsStrict();
			// TODO: complain about strictness

			JDDNode goalStates = mc.checkExpressionDD(pathFormula.getOperand2(), JDD.Constant(1));
			activeRefs.register(goalStates);

			JDDNode remainStates;
			if (pathFormula.getOperator() == ExpressionTemporal.P_U) {
				remainStates = mc.checkExpressionDD(pathFormula.getOperand1(), JDD.Constant(1));
			} else {
				remainStates = JDD.Constant(1.0);
			}
			activeRefs.register(remainStates);

/*		if (!remainStates.equals(JDD.ONE)) {
			throw new PrismException("Currently, restricted reachability is not yet supported.");
		}*/


			// --- Reward normalization
			if (model.getModelType() == ModelType.DTMC) {
				if (!transRewards.equals(JDD.ZERO)) {
					throw new PrismException("Quantiles for DTMC are not supported for transition rewards");
				}
			}

			// TODO: ensure integer rewards...

			// --- Calculator generation

			activeRefs.release(stateRewards, transRewards, goalStates, remainStates);
			QuantileCalculator qc;
			String engine = parent.getSettings().getString(PrismSettings.PRISM_ENGINE);

			if (model.getModelType() == ModelType.CTMC) {
				qc = new QuantileCalculatorCTMCSearch(parent, mc, (StochModel)model, stateRewards, transRewards, goalStates, remainStates, expr.chooseIntervalUpperBound());
			} else if (parent.getSettings().getBoolean(PrismSettings.QUANTILE_USE_BIGSTEP)) {
				qc = new QuantileCalculatorSymbolicBigStep(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);
			} else if (parent.getSettings().getBoolean(PrismSettings.QUANTILE_USE_TACAS16)) {
				qc = new QuantileCalculatorSymbolicTACAS16(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);
				qc.getLog().println("Using TACAS'16 variant of quantile computation...");
			} else if (engine.equals("Sparse")) {
				qc = new QuantileCalculatorSparse(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);
			} else if (engine.equals("Hybrid")) {
				qc = new QuantileCalculatorHybrid(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);
			} else {
				qc = new QuantileCalculatorSymbolic(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);
			}
			activeRefs.register(qc);

			ReachabilityQuantile q;
			boolean universal = expr.isUniversal();
			if (universal) {
				if (rewardBoundLower) {
					q = new ReachabilityLowerRewardBoundUniversal(qc, qc.qcc);
				} else {
					q = new ReachabilityUpperRewardBoundUniversal(qc, qc.qcc);
				}
			} else {
				if (rewardBoundLower) {
					q = new ReachabilityLowerRewardBoundExistential(qc, qc.qcc);
				} else {
					q = new ReachabilityUpperRewardBoundExistential(qc, qc.qcc);
				}
			}
			activeRefs.register(q);
			qc.setReachabilityQuantile(q);

			int result_adjustment = expr.getResultAdjustment(); 

			StateValues result = qc.iteration(statesOfInterest, relP, thresholdsP, result_adjustment);
			
			return result;
		}
	}

	public static StateValues checkRewardBoundedSimplePathFormula(PrismComponent parent, StateModelChecker mc, Model model, ExpressionTemporal exprTemp, boolean min, JDDNode statesOfInterest) throws PrismException {
		JDD.Deref(statesOfInterest);

		// try-with-resource: activeRefs, hold the currenty active JDDNodes
		// on exception or on leaving the try block, they will be automaticall derefed
		try (TemporaryJDDRefs activeRefs = new TemporaryJDDRefs()) {
			if (model.getModelType() != ModelType.MDP) {
				throw new PrismException("Symbolic reward bounded probability computations via the quantile engine currently only for MDP.");
			}

			if (exprTemp.getBounds().countBounds() != 1) {
				throw new PrismNotSupportedException("Only one bound");
			}
			TemporalOperatorBound rewardBound = exprTemp.getBounds().getBounds().get(0);
			Object rs = rewardBound.getRewardStructureIndex();

			JDDNode stateRewards = null;
			JDDNode transRewards = null;

			int i;
			// get reward info
			if (rs == null) {
				// step
				stateRewards = JDD.Constant(1.0);
				transRewards = JDD.Constant(0.0);
			} else {
				if (model.getNumRewardStructs() == 0) {
					throw new PrismException("Model has no rewards specified");
				} else if (rs instanceof Expression) {
					i = ((Expression) rs).evaluateInt(mc.getConstantValues());
					rs = new Integer(i); // for better error reporting below
					stateRewards = model.getStateRewards(i - 1).copy();
					transRewards = model.getTransRewards(i - 1).copy();
				} else if (rs instanceof String) {
					stateRewards = model.getStateRewards((String) rs).copy();
					transRewards = model.getTransRewards((String) rs).copy();
				} else {
					throw new PrismException("Unknown reward info "+rs.getClass());
				}
			}

			activeRefs.register(stateRewards);
			activeRefs.register(transRewards);

			if (stateRewards == null || transRewards == null) {
				throw new PrismException("Invalid reward structure index \"" + rs + "\"");
			}

			if (exprTemp.getOperator() != ExpressionTemporal.P_F &&
			    exprTemp.getOperator() != ExpressionTemporal.P_U) {
				throw new PrismNotSupportedException("Only support F and U in quantile");
			}

			if (rewardBound.hasLowerBound() && rewardBound.hasUpperBound()) {
				throw new PrismLangException("Can not have upper and lower bound in quantile");
			}

			if (!rewardBound.hasLowerBound() && !rewardBound.hasUpperBound()) {
				// TODO: handle
				throw new PrismLangException("Trivial case: no reward bound");
			}

			int bound;
			if (rewardBound.hasLowerBound()) {
				bound = rewardBound.getLowerBound().evaluateInt(mc.getConstantValues());
				if (rewardBound.lowerBoundIsStrict()) {
					bound += 1;
				}
			} else {
				bound = rewardBound.getUpperBound().evaluateInt(mc.getConstantValues());
				if (rewardBound.upperBoundIsStrict()) {
					bound -= 1;
				}
			}

			if (bound < 0) {
				throw new PrismException("Invalid effective bound: " + bound);
			}

			JDDNode goalStates = mc.checkExpressionDD(exprTemp.getOperand2(), JDD.Constant(1));
			activeRefs.register(goalStates);

			JDDNode remainStates;
			if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				remainStates = mc.checkExpressionDD(exprTemp.getOperand1(), JDD.Constant(1));
			} else {
				remainStates = JDD.Constant(1.0);
			}
			activeRefs.register(remainStates);

			// --- Reward normalization
			if (model.getModelType() == ModelType.DTMC) {
				if (!transRewards.equals(JDD.ZERO)) {
					throw new PrismException("Quantiles for DTMC are not supported for transition rewards");
				}
			}

			// TODO: ensure integer rewards...

			// --- Calculator generation

			activeRefs.release(stateRewards, transRewards, goalStates, remainStates);
			QuantileCalculator qc;
			String engine = parent.getSettings().getString(PrismSettings.PRISM_ENGINE);
			if (parent.getSettings().getBoolean(PrismSettings.QUANTILE_USE_TACAS16) ||
			    engine.equals("Hybrid") || engine.equals("Sparse")) {
				qc = new QuantileCalculatorSymbolicTACAS16(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);
				qc.getLog().println("Using TACAS'16 variant of quantile computation...");
			} else {
				qc = new QuantileCalculatorSymbolic(parent, mc, model, stateRewards, transRewards, goalStates, remainStates);
			}
			activeRefs.register(qc);

			ReachabilityQuantile q;
			boolean rewardBoundLower = rewardBound.hasLowerBound();
			boolean universal;
			if ((!rewardBoundLower && !min) || (rewardBoundLower && min)){
				universal = false;
			} else {
				universal = true;
			}

			if (universal) {
				if (rewardBoundLower) {
					q = new ReachabilityLowerRewardBoundUniversal(qc, qc.qcc);
				} else {
					q = new ReachabilityUpperRewardBoundUniversal(qc, qc.qcc);
				}
			} else {
				if (rewardBoundLower) {
					q = new ReachabilityLowerRewardBoundExistential(qc, qc.qcc);
				} else {
					q = new ReachabilityUpperRewardBoundExistential(qc, qc.qcc);
				}
			}
			activeRefs.register(q);
			((QuantileCalculator)qc).setReachabilityQuantile(q);

			StateValues result = qc.computeForBound(bound);

			return result;
		}
	}

}