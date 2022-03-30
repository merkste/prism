//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

import java.io.File;
import java.util.BitSet;
import java.util.List;
import java.util.PrimitiveIterator.OfInt;

import common.IterableBitSet;
import parser.ast.Coalition;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLongRun;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionSS;
import parser.ast.ExpressionStrategy;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.ast.RewardStruct;
import parser.ast.TemporalOperatorBound;
import parser.ast.TemporalOperatorBounds;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypePathBool;
import parser.type.TypePathDouble;
import prism.*;
import prism.SteadyStateProbs.SteadyStateProbsExplicit;
import explicit.DTMCModelChecker.ReachBsccComputer;
import explicit.rewards.ConstructRewards;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;
import explicit.rewards.STPGRewards;

/**
 * Super class for explicit-state probabilistic model checkers.
 */
public class ProbModelChecker extends NonProbModelChecker
{
	// Flags/settings
	// (NB: defaults do not necessarily coincide with PRISM)

	// Method used to solve linear equation systems
	protected LinEqMethod linEqMethod = LinEqMethod.GAUSS_SEIDEL;
	// Method used to solve MDPs
	protected MDPSolnMethod mdpSolnMethod = MDPSolnMethod.GAUSS_SEIDEL;
	// Iterative numerical method termination criteria
	protected TermCrit termCrit = TermCrit.RELATIVE;
	// Parameter for iterative numerical method termination criteria
	protected double termCritParam = 1e-8;
	// Max iterations for numerical solution
	protected int maxIters = 100000;
	// Use precomputation algorithms in model checking?
	protected boolean precomp = true;
	protected boolean prob0 = true;
	protected boolean prob1 = true;
	// Use predecessor relation? (e.g. for precomputation)
	protected boolean preRel = true;
	// Direction of convergence for value iteration (lfp/gfp)
	protected ValIterDir valIterDir = ValIterDir.BELOW;
	// Method used for numerical solution
	protected SolnMethod solnMethod = SolnMethod.VALUE_ITERATION;
	// Is non-convergence of an iterative method an error?
	protected boolean errorOnNonConverge = true;
	// Adversary export
	protected boolean exportAdv = false;
	protected String exportAdvFilename;

	// Enums for flags/settings

	// Method used for numerical solution
	public enum LinEqMethod {
		POWER, JACOBI, GAUSS_SEIDEL, BACKWARDS_GAUSS_SEIDEL, JOR, SOR, BACKWARDS_SOR;
		public String fullName()
		{
			switch (this) {
			case POWER:
				return "Power method";
			case JACOBI:
				return "Jacobi";
			case GAUSS_SEIDEL:
				return "Gauss-Seidel";
			case BACKWARDS_GAUSS_SEIDEL:
				return "Backwards Gauss-Seidel";
			case JOR:
				return "JOR";
			case SOR:
				return "SOR";
			case BACKWARDS_SOR:
				return "Backwards SOR";
			default:
				return this.toString();
			}
		}
	};

	// Method used for solving MDPs
	public enum MDPSolnMethod {
		VALUE_ITERATION, GAUSS_SEIDEL, POLICY_ITERATION, MODIFIED_POLICY_ITERATION, LINEAR_PROGRAMMING;
		public String fullName()
		{
			switch (this) {
			case VALUE_ITERATION:
				return "Value iteration";
			case GAUSS_SEIDEL:
				return "Gauss-Seidel";
			case POLICY_ITERATION:
				return "Policy iteration";
			case MODIFIED_POLICY_ITERATION:
				return "Modified policy iteration";
			case LINEAR_PROGRAMMING:
				return "Linear programming";
			default:
				return this.toString();
			}
		}
	};

	// Iterative numerical method termination criteria
	public enum TermCrit {
		ABSOLUTE, RELATIVE
	};

	// Direction of convergence for value iteration (lfp/gfp)
	public enum ValIterDir {
		BELOW, ABOVE
	};

	// Method used for numerical solution
	public enum SolnMethod {
		VALUE_ITERATION, GAUSS_SEIDEL, POLICY_ITERATION, MODIFIED_POLICY_ITERATION, LINEAR_PROGRAMMING
	};

	/**
	 * Create a new ProbModelChecker, inherit basic state from parent (unless null).
	 */
	public ProbModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);

		// If present, initialise settings from PrismSettings
		if (settings != null) {
			String s;
			// PRISM_LIN_EQ_METHOD
			s = settings.getString(PrismSettings.PRISM_LIN_EQ_METHOD);
			if (s.equals("Power")) {
				setLinEqMethod(LinEqMethod.POWER);
			} else if (s.equals("Jacobi")) {
				setLinEqMethod(LinEqMethod.JACOBI);
			} else if (s.equals("Gauss-Seidel")) {
				setLinEqMethod(LinEqMethod.GAUSS_SEIDEL);
			} else if (s.equals("Backwards Gauss-Seidel")) {
				setLinEqMethod(LinEqMethod.BACKWARDS_GAUSS_SEIDEL);
			} else if (s.equals("JOR")) {
				setLinEqMethod(LinEqMethod.JOR);
			} else if (s.equals("SOR")) {
				setLinEqMethod(LinEqMethod.SOR);
			} else if (s.equals("Backwards SOR")) {
				setLinEqMethod(LinEqMethod.BACKWARDS_SOR);
			} else {
				throw new PrismNotSupportedException("Explicit engine does not support linear equation solution method \"" + s + "\"");
			}
			// PRISM_MDP_SOLN_METHOD
			s = settings.getString(PrismSettings.PRISM_MDP_SOLN_METHOD);
			if (s.equals("Value iteration")) {
				setMDPSolnMethod(MDPSolnMethod.VALUE_ITERATION);
			} else if (s.equals("Gauss-Seidel")) {
				setMDPSolnMethod(MDPSolnMethod.GAUSS_SEIDEL);
			} else if (s.equals("Policy iteration")) {
				setMDPSolnMethod(MDPSolnMethod.POLICY_ITERATION);
			} else if (s.equals("Modified policy iteration")) {
				setMDPSolnMethod(MDPSolnMethod.MODIFIED_POLICY_ITERATION);
			} else if (s.equals("Linear programming")) {
				setMDPSolnMethod(MDPSolnMethod.LINEAR_PROGRAMMING);
			} else {
				throw new PrismNotSupportedException("Explicit engine does not support MDP solution method \"" + s + "\"");
			}
			// PRISM_TERM_CRIT
			s = settings.getString(PrismSettings.PRISM_TERM_CRIT);
			if (s.equals("Absolute")) {
				setTermCrit(TermCrit.ABSOLUTE);
			} else if (s.equals("Relative")) {
				setTermCrit(TermCrit.RELATIVE);
			} else {
				throw new PrismNotSupportedException("Unknown termination criterion \"" + s + "\"");
			}
			// PRISM_TERM_CRIT_PARAM
			setTermCritParam(settings.getDouble(PrismSettings.PRISM_TERM_CRIT_PARAM));
			// PRISM_MAX_ITERS
			setMaxIters(settings.getInteger(PrismSettings.PRISM_MAX_ITERS));
			// PRISM_PRECOMPUTATION
			setPrecomp(settings.getBoolean(PrismSettings.PRISM_PRECOMPUTATION));
			// PRISM_PROB0
			setProb0(settings.getBoolean(PrismSettings.PRISM_PROB0));
			// PRISM_PROB1
			setProb1(settings.getBoolean(PrismSettings.PRISM_PROB1));
			// PRISM_USE_PRE
			setPreRel(settings.getBoolean(PrismSettings.PRISM_PRE_REL));
			// PRISM_FAIRNESS
			if (settings.getBoolean(PrismSettings.PRISM_FAIRNESS)) {
				throw new PrismNotSupportedException("The explicit engine does not support model checking under fairness");
			}

			// PRISM_EXPORT_ADV
			s = settings.getString(PrismSettings.PRISM_EXPORT_ADV);
			if (!(s.equals("None")))
				setExportAdv(true);
			// PRISM_EXPORT_ADV_FILENAME
			setExportAdvFilename(settings.getString(PrismSettings.PRISM_EXPORT_ADV_FILENAME));
		}
	}

	// Settings methods

	/**
	 * Inherit settings (and the log) from another ProbModelChecker object.
	 * For model checker objects that inherit a PrismSettings object, this is superfluous
	 * since this has been done already.
	 */
	public void inheritSettings(ProbModelChecker other)
	{
		super.inheritSettings(other);
		setLinEqMethod(other.getLinEqMethod());
		setMDPSolnMethod(other.getMDPSolnMethod());
		setTermCrit(other.getTermCrit());
		setTermCritParam(other.getTermCritParam());
		setMaxIters(other.getMaxIters());
		setPrecomp(other.getPrecomp());
		setProb0(other.getProb0());
		setProb1(other.getProb1());
		setValIterDir(other.getValIterDir());
		setSolnMethod(other.getSolnMethod());
		setErrorOnNonConverge(other.geterrorOnNonConverge());
	}

	/**
	 * Print summary of current settings.
	 */
	public void printSettings()
	{
		super.printSettings();
		mainLog.print("linEqMethod = " + linEqMethod + " ");
		mainLog.print("mdpSolnMethod = " + mdpSolnMethod + " ");
		mainLog.print("termCrit = " + termCrit + " ");
		mainLog.print("termCritParam = " + termCritParam + " ");
		mainLog.print("maxIters = " + maxIters + " ");
		mainLog.print("precomp = " + precomp + " ");
		mainLog.print("prob0 = " + prob0 + " ");
		mainLog.print("prob1 = " + prob1 + " ");
		mainLog.print("valIterDir = " + valIterDir + " ");
		mainLog.print("solnMethod = " + solnMethod + " ");
		mainLog.print("errorOnNonConverge = " + errorOnNonConverge + " ");
	}

	// Set methods for flags/settings

	/**
	 * Set verbosity level, i.e. amount of output produced.
	 */
	public void setVerbosity(int verbosity)
	{
		this.verbosity = verbosity;
	}

	/**
	 * Set method used to solve linear equation systems.
	 */
	public void setLinEqMethod(LinEqMethod linEqMethod)
	{
		this.linEqMethod = linEqMethod;
	}

	/**
	 * Set method used to solve MDPs.
	 */
	public void setMDPSolnMethod(MDPSolnMethod mdpSolnMethod)
	{
		this.mdpSolnMethod = mdpSolnMethod;
	}

	/**
	 * Set termination criteria type for numerical iterative methods.
	 */
	public void setTermCrit(TermCrit termCrit)
	{
		this.termCrit = termCrit;
	}

	/**
	 * Set termination criteria parameter (epsilon) for numerical iterative methods.
	 */
	public void setTermCritParam(double termCritParam)
	{
		this.termCritParam = termCritParam;
	}

	/**
	 * Set maximum number of iterations for numerical iterative methods.
	 */
	public void setMaxIters(int maxIters)
	{
		this.maxIters = maxIters;
	}

	/**
	 * Set whether or not to use precomputation (Prob0, Prob1, etc.).
	 */
	public void setPrecomp(boolean precomp)
	{
		this.precomp = precomp;
	}

	/**
	 * Set whether or not to use Prob0 precomputation
	 */
	public void setProb0(boolean prob0)
	{
		this.prob0 = prob0;
	}

	/**
	 * Set whether or not to use Prob1 precomputation
	 */
	public void setProb1(boolean prob1)
	{
		this.prob1 = prob1;
	}

	/**
	 * Set whether or not to use pre-computed predecessor relation
	 */
	public void setPreRel(boolean preRel)
	{
		this.preRel = preRel;
	}

	/**
	 * Set direction of convergence for value iteration (lfp/gfp).
	 */
	public void setValIterDir(ValIterDir valIterDir)
	{
		this.valIterDir = valIterDir;
	}

	/**
	 * Set method used for numerical solution.
	 */
	public void setSolnMethod(SolnMethod solnMethod)
	{
		this.solnMethod = solnMethod;
	}

	/**
	 * Set whether non-convergence of an iterative method an error
	 */
	public void setErrorOnNonConverge(boolean errorOnNonConverge)
	{
		this.errorOnNonConverge = errorOnNonConverge;
	}

	public void setExportAdv(boolean exportAdv)
	{
		this.exportAdv = exportAdv;
	}

	public void setExportAdvFilename(String exportAdvFilename)
	{
		this.exportAdvFilename = exportAdvFilename;
	}

	// Get methods for flags/settings

	public int getVerbosity()
	{
		return verbosity;
	}

	public LinEqMethod getLinEqMethod()
	{
		return linEqMethod;
	}

	public MDPSolnMethod getMDPSolnMethod()
	{
		return mdpSolnMethod;
	}

	public TermCrit getTermCrit()
	{
		return termCrit;
	}

	public double getTermCritParam()
	{
		return termCritParam;
	}

	public int getMaxIters()
	{
		return maxIters;
	}

	public boolean getPrecomp()
	{
		return precomp;
	}

	public boolean getProb0()
	{
		return prob0;
	}

	public boolean getProb1()
	{
		return prob1;
	}

	public boolean getPreRel()
	{
		return preRel;
	}

	public ValIterDir getValIterDir()
	{
		return valIterDir;
	}

	public SolnMethod getSolnMethod()
	{
		return solnMethod;
	}

	/**
	 * Is non-convergence of an iterative method an error?
	 */
	public boolean geterrorOnNonConverge()
	{
		return errorOnNonConverge;
	}

	// Model checking functions

	@Override
	public StateValues checkExpression(Model model, Expression expr, BitSet statesOfInterest) throws PrismException
	{
		StateValues res;

		// Conditional expression
		if (expr instanceof ExpressionConditional) {
			res = checkExpressionConditional(model, (ExpressionConditional) expr, statesOfInterest);
		}
		// <<>> or [[]] operator
		else if (expr instanceof ExpressionStrategy) {
			res = checkExpressionStrategy(model, (ExpressionStrategy) expr, statesOfInterest);
		}
		// P operator
		else if (expr instanceof ExpressionProb) {
			res = checkExpressionProb(model, (ExpressionProb) expr, statesOfInterest);
		}
		// R operator
		else if (expr instanceof ExpressionReward) {
			res = checkExpressionReward(model, (ExpressionReward) expr, statesOfInterest);
		}
		// L operator
		else if (expr instanceof ExpressionLongRun) {
			res = checkExpressionLongRun(model, (ExpressionLongRun) expr, statesOfInterest);
		}
		// S operator
		else if (expr instanceof ExpressionSS) {
			res = checkExpressionSteadyState(model, (ExpressionSS) expr, statesOfInterest);
		}
		// Otherwise, use the superclass
		else {
			res = super.checkExpression(model, expr, statesOfInterest);
		}

		return res;
	}

	/**
	 * Model check a <<>> or [[]] operator expression and return the values for the statesOfInterest.
	 * * @param statesOfInterest the states of interest, see checkExpression()
	 */
	protected StateValues checkExpressionStrategy(Model model, ExpressionStrategy expr, BitSet statesOfInterest) throws PrismException
	{
		// Only support <<>>/[[]] for MDPs right now
		if (!(this instanceof MDPModelChecker))
			throw new PrismNotSupportedException("The " + expr.getOperatorString() + " operator is only supported for MDPs currently");

		// Will we be quantifying universally or existentially over strategies/adversaries?
		boolean forAll = !expr.isThereExists();
		
		// Extract coalition info
		Coalition coalition = expr.getCoalition();
		// For non-games (i.e., models with a single player), deal with the coalition operator here and then remove it
		if (coalition != null && !model.getModelType().multiplePlayers()) {
			if (coalition.isEmpty()) {
				// An empty coalition negates the quantification ("*" has no effect)
				forAll = !forAll;
			}
			coalition = null;
		}

		// For now, just support a single expression (which may encode a Boolean combination of objectives)
		List<Expression> exprs = expr.getOperands();
		if (exprs.size() > 1) {
			throw new PrismException("Cannot currently check strategy operators wth lists of expressions");
		}
		Expression exprSub = exprs.get(0);
		// Pass onto relevant method:
		// P operator
		if (exprSub instanceof ExpressionProb) {
			return checkExpressionProb(model, (ExpressionProb) exprSub, forAll, coalition, statesOfInterest);
		}
		// R operator
		else if (exprSub instanceof ExpressionReward) {
			return checkExpressionReward(model, (ExpressionReward) exprSub, forAll, coalition, statesOfInterest);
		}
		// Anything else is an error 
		else {
			throw new PrismException("Unexpected operators in " + expr.getOperatorString() + " operator");
		}
	}

	/**
	 * Model check a conditional expression and return the values for all states.
	 */
	protected StateValues checkExpressionConditional(Model model, ExpressionConditional expr, BitSet statesOfInterest) throws PrismException
	{
		// functionality is provided by derived model checkers
		throw new PrismNotSupportedException("Missing support for conditional expressions.");
	}

	/**
	 * Model check a P operator expression and return the values for the statesOfInterest.
 	 * @param statesOfInterest the states of interest, see checkExpression()
	 */
	protected StateValues checkExpressionProb(Model model, ExpressionProb expr, BitSet statesOfInterest) throws PrismException
	{
		// Use the default semantics for a standalone P operator
		// (i.e. quantification over all strategies, and no game-coalition info)
		return checkExpressionProb(model, expr, true, null, statesOfInterest);
	}
	
	/**
	 * Model check a P operator expression and return the values for the states of interest.
	 * @param model The model
	 * @param expr The P operator expression
	 * @param forAll Are we checking "for all strategies" (true) or "there exists a strategy" (false)? [irrelevant for numerical (=?) queries] 
	 * @param coalition If relevant, info about which set of players this P operator refers to
	 * @param statesOfInterest the states of interest, see checkExpression()
	 */
	protected StateValues checkExpressionProb(Model model, ExpressionProb expr, boolean forAll, Coalition coalition, BitSet statesOfInterest) throws PrismException
	{
		// Get info from P operator
		OpRelOpBound opInfo = expr.getRelopBoundInfo(constantValues);
		MinMax minMax = opInfo.getMinMax(model.getModelType(), forAll);

		// Compute probabilities
		StateValues probs = checkProbPathFormula(model, expr.getExpression(), minMax, statesOfInterest);

		// Print out probabilities
		if (getVerbosity() > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			probs.print(mainLog);
		}

		// For =? properties, just return values
		if (opInfo.isNumeric()) {
			return probs;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			BitSet sol = probs.getBitSetFromInterval(opInfo.getRelOp(), opInfo.getBound());
			probs.clear();
			return StateValues.createFromBitSet(sol, model);
		}
	}

	/**
	 * Compute probabilities for the contents of a P operator.
	 * @param statesOfInterest the states of interest, see checkExpression()
	 */
	protected StateValues checkProbPathFormula(Model model, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// Test whether this is a simple path formula (i.e. PCTL)
		// and whether we want to use the corresponding algorithms
		boolean useSimplePathAlgo = expr.isSimplePathFormula();

		if (useSimplePathAlgo &&
		    settings.getBoolean(PrismSettings.PRISM_PATH_VIA_AUTOMATA) &&
		    LTLModelChecker.isSupportedLTLFormula(model.getModelType(), expr)) {
			// If PRISM_PATH_VIA_AUTOMATA is true, we want to use the LTL engine
			// whenever possible
			useSimplePathAlgo = false;
		}

		if (useSimplePathAlgo) {
			return checkProbPathFormulaSimple(model, expr, minMax, statesOfInterest);
		} else {
			return checkProbPathFormulaLTL(model, expr, false, minMax, statesOfInterest);
		}
	}

	/**
	 * Compute probabilities for a simple, non-LTL path operator.
	 */
	protected StateValues checkProbPathFormulaSimple(Model model, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		boolean negated = false;
		StateValues probs = null;

		expr = Expression.convertSimplePathFormulaToCanonicalForm(expr);
		ExpressionTemporal exprTemp = Expression.getTemporalOperatorForSimplePathFormula(expr);
		
		// Here, in the generic method, reward bounds and multiple bounds are not handled.
		// If they can be handled, they will have been processed already by a checkProbPathFormulaSimple
		// function in a derived model checker class.
		if (exprTemp.getBounds().hasRewardBounds())
			throw new PrismNotSupportedException("Reward bounds are not supported in the generic case.");
		if (model.getModelType().continuousTime()) {
			if (exprTemp.getBounds().hasStepBounds()) {
				throw new PrismNotSupportedException("Step bounds are not supported for continuous time models.");
			}
			if (exprTemp.getBounds().countTimeBoundsContinuous() > 1) {
				throw new PrismNotSupportedException("Multiple time bounds are not supported for continuous time models.");
			}
		} else {
			if (exprTemp.getBounds().countStepBounds() > 1) {
				throw new PrismNotSupportedException("Multiple step bounds are not supported by the generic calculator.");
			}
		}

		// Negation
		if (expr instanceof ExpressionUnaryOp &&
		    ((ExpressionUnaryOp)expr).getOperator() == ExpressionUnaryOp.NOT) {
			negated = true;
			minMax = minMax.negate();
			expr = ((ExpressionUnaryOp)expr).getOperand();
		}

		if (expr instanceof ExpressionTemporal) {
			exprTemp = (ExpressionTemporal) expr;

			// Next
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				probs = checkProbNext(model, exprTemp, minMax, statesOfInterest);
			}
			// Until
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				if (exprTemp.hasBounds()) {
					probs = checkProbBoundedUntil(model, exprTemp, minMax, statesOfInterest);
				} else {
					probs = checkProbUntil(model, exprTemp, minMax, statesOfInterest);
				}
			}
		}

		if (probs == null)
			throw new PrismException("Unrecognised path operator in P operator");

		if (negated) {
			// Subtract from 1 for negation
			probs.timesConstant(-1.0);
			probs.plusConstant(1.0);
		}

		return probs;
	}

	/**
	 * Compute probabilities for a next operator.
	 */
	protected StateValues checkProbNext(Model model, ExpressionTemporal expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		if (expr.getBounds().hasRewardBounds()) {
			throw new PrismException("Reachability reward operator with reward bounds is not supported.");
		}

		// Model check the operand for all states
		BitSet target = checkExpression(model, expr.getOperand2(), null).getBitSet();

		// Compute/return the probabilities
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case CTMC:
			res = ((CTMCModelChecker) this).computeNextProbs((CTMC) model, target);
			break;
		case DTMC:
			res = ((DTMCModelChecker) this).computeNextProbs((DTMC) model, target);
			break;
		case MDP:
			res = ((MDPModelChecker) this).computeNextProbs((MDP) model, target, minMax.isMin());
			break;
		case STPG:
			res = ((STPGModelChecker) this).computeNextProbs((STPG) model, target, minMax.isMin1(), minMax.isMin2());
			break;
		default:
			throw new PrismNotSupportedException("Cannot model check " + expr + " for " + model.getModelType() + "s");
		}
		if (result != null) {
			result.setStrategy(res.strat);
		}
		return StateValues.createFromDoubleArray(res.soln, model);
	}

	/**
	 * Compute probabilities for a bounded until operator.
	 */
	protected StateValues checkProbBoundedUntil(Model model, ExpressionTemporal expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// This method just handles discrete time
		// Continuous-time model checkers will override this method

		// Get info from bounded until
		Integer lowerBound;
		IntegerBound bounds;
		int i;

		// get and check bounds information
		bounds = IntegerBound.fromExpressionTemporal(expr, constantValues, true);

		// Model check operands for all states
		BitSet remain = checkExpression(model, expr.getOperand1(), null).getBitSet();
		BitSet target = checkExpression(model, expr.getOperand2(), null).getBitSet();

		if (bounds.hasLowerBound()) {
			lowerBound = bounds.getLowestInteger();
		} else {
			lowerBound = 0;
		}

		Integer windowSize = null;  // unbounded

		if (bounds.hasUpperBound()) {
			windowSize = bounds.getHighestInteger() - lowerBound;
		}

		// compute probabilities for Until<=windowSize
		StateValues sv = null;

		if (windowSize == null) {
			// unbounded
			ModelCheckerResult res = null;
			switch (model.getModelType()) {
			case DTMC:
				res = ((DTMCModelChecker) this).computeUntilProbs((DTMC) model, remain, target);
				break;
			case MDP:
				res = ((MDPModelChecker) this).computeUntilProbs((MDP) model, remain, target, minMax.isMin());
				break;
			case STPG:
				res = ((STPGModelChecker) this).computeUntilProbs((STPG) model, remain, target, minMax.isMin1(), minMax.isMin2());
				break;
			default:
				throw new PrismException("Cannot model check " + expr + " for " + model.getModelType() + "s");
			}
			result.setStrategy(res.strat);
			sv = StateValues.createFromDoubleArray(res.soln, model);
		} else if (windowSize == 0) {
			// A trivial case: windowSize=0 (prob is 1 in target states, 0 otherwise)
			sv = StateValues.createFromBitSetAsDoubles(target, model);
		} else {
			// Otherwise: numerical solution
			ModelCheckerResult res = null;

			switch (model.getModelType()) {
			case DTMC:
				res = ((DTMCModelChecker) this).computeBoundedUntilProbs((DTMC) model, remain, target, windowSize);
				break;
			case MDP:
				res = ((MDPModelChecker) this).computeBoundedUntilProbs((MDP) model, remain, target, windowSize, minMax.isMin());
				break;
			case STPG:
				res = ((STPGModelChecker) this).computeBoundedUntilProbs((STPG) model, remain, target, windowSize, minMax.isMin1(), minMax.isMin2());
				break;
			default:
				throw new PrismNotSupportedException("Cannot model check " + expr + " for " + model.getModelType() + "s");
			}
			result.setStrategy(res.strat);
			sv = StateValues.createFromDoubleArray(res.soln, model);
		}

		// perform lowerBound restricted next-step computations to
		// deal with lower bound.
		if (lowerBound > 0) {
			double[] probs = sv.getDoubleArray();

			for (i = 0; i < lowerBound; i++) {
				switch (model.getModelType()) {
				case DTMC:
					probs = ((DTMCModelChecker) this).computeRestrictedNext((DTMC) model, remain, probs);
					break;
				case MDP:
					probs = ((MDPModelChecker) this).computeRestrictedNext((MDP) model, remain, probs, minMax.isMin());
					break;
				case STPG:
					// TODO (JK): Figure out if we can handle lower bounds for STPG in the same way
					throw new PrismNotSupportedException("Lower bounds not yet supported for STPGModelChecker");
				default:
					throw new PrismNotSupportedException("Cannot model check " + expr + " for " + model.getModelType() + "s");
				}
			}

			sv = StateValues.createFromDoubleArray(probs, model);
		}

		return sv;
	}

	/**
	 * Compute probabilities for an (unbounded) until operator.
	 */
	protected StateValues checkProbUntil(Model model, ExpressionTemporal expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// Model check operands for all states
		BitSet remain = checkExpression(model, expr.getOperand1(), null).getBitSet();
		BitSet target = checkExpression(model, expr.getOperand2(), null).getBitSet();

		// Compute/return the probabilities
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case CTMC:
			res = ((CTMCModelChecker) this).computeUntilProbs((CTMC) model, remain, target);
			break;
		case DTMC:
			res = ((DTMCModelChecker) this).computeUntilProbs((DTMC) model, remain, target);
			break;
		case MDP:
			res = ((MDPModelChecker) this).computeUntilProbs((MDP) model, remain, target, minMax.isMin());
			break;
		case STPG:
			res = ((STPGModelChecker) this).computeUntilProbs((STPG) model, remain, target, minMax.isMin1(), minMax.isMin2());
			break;
		default:
			throw new PrismNotSupportedException("Cannot model check " + expr + " for " + model.getModelType() + "s");
		}
		if (result != null) {
			result.setStrategy(res.strat);
		}
		return StateValues.createFromDoubleArray(res.soln, model);
	}

	/**
	 * Compute probabilities for an LTL path formula
	 */
	protected StateValues checkProbPathFormulaLTL(Model model, Expression expr, boolean qual, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// To be overridden by subclasses
		throw new PrismNotSupportedException("Computation not implemented yet");
	}

	/**
	 * Model check an R operator expression and return the values for all states.
	 */
	protected StateValues checkExpressionReward(Model model, ExpressionReward expr, BitSet statesOfInterest) throws PrismException
	{
		// Use the default semantics for a standalone R operator
		// (i.e. quantification over all strategies, and no game-coalition info)
		return checkExpressionReward(model, expr, true, null, statesOfInterest);
	}
	
	/**
	 * Model check an R operator expression and return the values for all states.
	 */
	protected StateValues checkExpressionReward(Model model, ExpressionReward expr, boolean forAll, Coalition coalition, BitSet statesOfInterest) throws PrismException
	{
		// Get info from R operator
		OpRelOpBound opInfo = expr.getRelopBoundInfo(constantValues);
		MinMax minMax = opInfo.getMinMax(model.getModelType(), forAll);

		// Build rewards
		RewardStruct rewStruct = expr.getRewardStructByIndexObject(modelInfo, constantValues);
		mainLog.println("Building reward structure...");
		Rewards rewards = constructRewards(model, rewStruct, false);  // false = don't allow negative rewards

		// Compute rewards
		StateValues rews = checkRewardFormula(model, rewards, expr.getExpression(), minMax, statesOfInterest);

		// Print out rewards
		if (getVerbosity() > 5) {
			mainLog.print("\nRewards (non-zero only) for all states:\n");
			rews.print(mainLog);
		}

		// For =? properties, just return values
		if (opInfo.isNumeric()) {
			return rews;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			BitSet sol = rews.getBitSetFromInterval(opInfo.getRelOp(), opInfo.getBound());
			rews.clear();
			return StateValues.createFromBitSet(sol, model);
		}
	}


	/**
	 * Construct rewards from a reward structure and a model.
	 * <br>
	 * If {@code allowNegativeRewards} is true, the rewards may be positive and negative, i.e., weights.
	 */
	protected Rewards constructRewards(Model model, RewardStruct rewStruct, boolean allowNegativeRewards) throws PrismException
	{
		Rewards rewards;
		ConstructRewards constructRewards = new ConstructRewards(mainLog);

		if (allowNegativeRewards)
			constructRewards.allowNegativeRewards();

		switch (model.getModelType()) {
		case CTMC:
		case DTMC:
			rewards = constructRewards.buildMCRewardStructure((DTMC) model, rewStruct, constantValues);
			break;
		case MDP:
			rewards = constructRewards.buildMDPRewardStructure((MDP) model, rewStruct, constantValues);
			break;
		default:
			throw new PrismNotSupportedException("Cannot build rewards for " + model.getModelType() + "s");
		}
		return rewards;
	}

	public void exportStateRewardsToFileExpl(Model model, RewardStruct rewardStruct,String filename,int exportType, boolean noexportheaders, int precision) throws PrismException{

		int numStates = model.getNumStates();
		int nonZeroRews = 0;
		Rewards modelRewards;

		PrismLog out;
		if (filename != null) {
			out = PrismFileLog.create(filename, false);
		} else {
			out = mainLog;
		}


		if (exportType != Prism.EXPORT_PLAIN) {
			throw new PrismNotSupportedException("Exporting state rewards in the requested format is currently not supported by the explicit engine");
		}

		try{
			modelRewards = constructRewards(model, rewardStruct, true);
		} catch (PrismException e) {
			if(e.getMessage()=="Explicit engine does not yet handle transition rewards for D/CTMCs"){
				//
				if(!noexportheaders){
					out.println("# State rewards");
					out.println(numStates + " 0");
				}
				return;
			} else {
				throw e;
			}
		}


		switch (model.getModelType()) {
		case DTMC:
		case CTMC:
			MCRewards mcRewards = (MCRewards) modelRewards;
			for (int s = 0; s < numStates; s++) {
				double d = mcRewards.getStateReward(s);
				if (d != 0) {
					nonZeroRews++;
				}
			}
			if(!noexportheaders){
				String rewardStructName = rewardStruct.getName();
				if(rewardStructName!=null){
					out.println("# Reward Structure: \"" + rewardStructName + "\"");
				}
				out.println("# State rewards");
			}

			out.println(numStates + " " + nonZeroRews);
			for (int s = 0; s < numStates; s++) {
				double d = mcRewards.getStateReward(s);
				if (d != 0) {
					out.println(s + " " + PrismUtils.formatDouble(precision, d));
				}
			}
			break;
		case MDP:
		case STPG:
			MDPRewards mdpRewards = (MDPRewards) modelRewards;
			for (int s = 0; s < numStates; s++) {
				double d = mdpRewards.getStateReward(s);
				if (d != 0) {
					nonZeroRews++;
				}
			}
			if(!noexportheaders){
				String rewardStructName = rewardStruct.getName();
				if(rewardStructName!=null){
					out.println("# Reward Structure: \"" + rewardStructName + "\"");
				}
				out.println("# State rewards");
			}

			out.println(numStates + " " + nonZeroRews);
			for (int s = 0; s < numStates; s++) {
				double d = mdpRewards.getStateReward(s);
				if (d != 0) {
					out.println(s + " " + PrismUtils.formatDouble(precision, d));
				}
			}
			break;
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet export state rewards for " + model.getModelType() + "s");
		}

		out.close();
	}
	
	/**
	 * Compute rewards for the contents of an R operator.
	 */
	protected StateValues checkRewardFormula(Model model, Rewards modelRewards, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		StateValues rewards = null;

		if (expr.getType() instanceof TypePathDouble) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			switch (exprTemp.getOperator()) {
			case ExpressionTemporal.R_I:
				rewards = checkRewardInstantaneous(model, modelRewards, exprTemp, minMax);
				break;
			case ExpressionTemporal.R_C:
				if (exprTemp.hasBounds()) {
					rewards = checkRewardCumulative(model, modelRewards, exprTemp, minMax);
				} else {
					rewards = checkRewardTotal(model, modelRewards, exprTemp, minMax);
				}
				break;
			default:
				throw new PrismNotSupportedException("Explicit engine does not yet handle the " + exprTemp.getOperatorSymbol() + " reward operator");
			}
		} else if (expr.getType() instanceof TypePathBool || expr.getType() instanceof TypeBool) {
			rewards = checkRewardPathFormula(model, modelRewards, expr, minMax, statesOfInterest);
		}

		if (rewards == null)
			throw new PrismException("Unrecognised operator in R operator");

		return rewards;
	}

	/**
	 * Compute rewards for an instantaneous reward operator.
	 */
	protected StateValues checkRewardInstantaneous(Model model, Rewards modelRewards, ExpressionTemporal expr, MinMax minMax) throws PrismException
	{
		if (expr.getBounds().hasRewardBounds()) {
			throw new PrismNotSupportedException("Instantaneous reward operator with reward bounds is not supported.");
		}

		// Get time bound
		double t = expr.getBounds().getStandardBound(model.getModelType()).getUpperBound().evaluateDouble(constantValues);

		// Compute/return the rewards
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case DTMC:
			res = ((DTMCModelChecker) this).computeInstantaneousRewards((DTMC) model, (MCRewards) modelRewards, t);
			break;
		case CTMC:
			res = ((CTMCModelChecker) this).computeInstantaneousRewards((CTMC) model, (MCRewards) modelRewards, t);
			break;
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet handle the " + expr.getOperatorSymbol() + " reward operator for " + model.getModelType()
					+ "s");
		}
		result.setStrategy(res.strat);
		return StateValues.createFromDoubleArray(res.soln, model);
	}

	/**
	 * Compute rewards for a cumulative reward operator.
	 */
	protected StateValues checkRewardCumulative(Model model, Rewards modelRewards, ExpressionTemporal expr, MinMax minMax) throws PrismException
	{
		int timeInt = -1;
		double timeDouble = -1;

		if (expr.getBounds().hasRewardBounds()) {
			throw new PrismNotSupportedException("Cumulative reward operator with reward bounds is not supported.");
		}

		TemporalOperatorBound bound = expr.getBounds().getStandardBound(model.getModelType());
		if (bound == null || bound.getUpperBound() == null) {
			throw new PrismException("This is not a cumulative reward operator");
		}

		// Get time bound
		if (model.getModelType().continuousTime()) {
			timeDouble = bound.getUpperBound().evaluateDouble(constantValues);
			if (timeDouble < 0) {
				throw new PrismException("Invalid time bound " + timeDouble + " in cumulative reward formula");
			}
		} else {
			timeInt = bound.getUpperBound().evaluateInt(constantValues);
			if (timeInt < 0) {
				throw new PrismException("Invalid time bound " + timeInt + " in cumulative reward formula");
			}
		}

		// Compute/return the rewards
		// A trivial case: "C<=0" (prob is 1 in target states, 0 otherwise)
		if (timeInt == 0 || timeDouble == 0.0) {
			return new StateValues(TypeDouble.getInstance(), model.getNumStates(), 0.0);
		}
		// Otherwise: numerical solution
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case DTMC:
			res = ((DTMCModelChecker) this).computeCumulativeRewards((DTMC) model, (MCRewards) modelRewards, timeInt);
			break;
		case CTMC:
			res = ((CTMCModelChecker) this).computeCumulativeRewards((CTMC) model, (MCRewards) modelRewards, timeDouble);
			break;
		case MDP:
			res = ((MDPModelChecker) this).computeCumulativeRewards((MDP) model, (MDPRewards) modelRewards, timeInt, minMax.isMin());
			break;
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet handle the " + expr.getOperatorSymbol() + " reward operator for " + model.getModelType()
					+ "s");
		}
		result.setStrategy(res.strat);
		return StateValues.createFromDoubleArray(res.soln, model);
	}

	/**
	 * Compute expected rewards for a total reward operator.
	 */
	protected StateValues checkRewardTotal(Model model, Rewards modelRewards, ExpressionTemporal expr, MinMax minMax) throws PrismException
	{
		if (expr.getBounds().hasRewardBounds()) {
			throw new PrismNotSupportedException("Cumulative reward operator with reward bounds is not supported.");
		}

		if (expr.hasBounds()) {
			throw new PrismException("This is not a total reward operator");
		}

		// Compute/return the rewards
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case DTMC:
			res = ((DTMCModelChecker) this).computeTotalRewards((DTMC) model, (MCRewards) modelRewards);
			break;
		case CTMC:
			res = ((CTMCModelChecker) this).computeTotalRewards((CTMC) model, (MCRewards) modelRewards);
			break;
		case MDP:
			break;
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet handle the " + expr.getOperatorSymbol() + " reward operator for " + model.getModelType()
					+ "s");
		}
		result.setStrategy(res.strat);
		return StateValues.createFromDoubleArray(res.soln, model);
	}

	/**
	 * Compute rewards for a path formula in a reward operator.
	 */
	protected StateValues checkRewardPathFormula(Model model, Rewards modelRewards, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		if (Expression.isReachWithStateFormula(expr)) {
			return checkRewardReach(model, modelRewards, (ExpressionTemporal) expr, minMax, statesOfInterest);
		}
		else if (Expression.isCoSafeLTLSyntactic(expr, true)) {
			return checkRewardCoSafeLTL(model, modelRewards, expr, minMax, statesOfInterest);
		}
		throw new PrismException("R operator contains a path formula that is not syntactically co-safe: " + expr);
	}
	
	/**
	 * Compute rewards for a reachability reward operator.
	 */
	protected StateValues checkRewardReach(Model model, Rewards modelRewards, ExpressionTemporal expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// No time bounds allowed
		if (expr.hasBounds()) {
			throw new PrismNotSupportedException("R operator cannot contain a bounded F operator: " + expr);
		}
		
		// Model check the operand for all states
		BitSet target = checkExpression(model, expr.getOperand2(), null).getBitSet();

		// Compute/return the rewards
		ModelCheckerResult res = null;
		switch (model.getModelType()) {
		case DTMC:
			res = ((DTMCModelChecker) this).computeReachRewards((DTMC) model, (MCRewards) modelRewards, target);
			break;
		case CTMC:
			res = ((CTMCModelChecker) this).computeReachRewards((CTMC) model, (MCRewards) modelRewards, target);
			break;
		case MDP:
			res = ((MDPModelChecker) this).computeReachRewards((MDP) model, (MDPRewards) modelRewards, target, minMax.isMin());
			break;
		case STPG:
			res = ((STPGModelChecker) this).computeReachRewards((STPG) model, (STPGRewards) modelRewards, target, minMax.isMin1(), minMax.isMin2());
			break;
		default:
			throw new PrismNotSupportedException("Explicit engine does not yet handle the " + expr.getOperatorSymbol() + " reward operator for " + model.getModelType()
					+ "s");
		}
		result.setStrategy(res.strat);
		return StateValues.createFromDoubleArray(res.soln, model);
	}

	/**
	 * Compute rewards for a co-safe LTL reward operator.
	 */
	protected StateValues checkRewardCoSafeLTL(Model model, Rewards modelRewards, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// To be overridden by subclasses
		throw new PrismException("Computation not implemented yet");
	}

	/**
	 * Model check relativized long-run, i.e., L operator.
	 *
	 * @param model a Model
	 * @param expr a long-run expression
	 * @param statesOfInterest states for which the expression has to be computed
	 * @return the relativized long-run value for each state of interest
	 * @throws PrismException
	 */
	public StateValues checkExpressionLongRun(Model model, ExpressionLongRun expr, BitSet statesOfInterest) throws PrismException
	{
		return checkConditionalExpressionLongRun(model, expr, null, statesOfInterest);
	}

	/**
	 * Model check relativized long-run, i.e., L operator, under a condition.
	 *
	 * @param dtmc a DTMC
	 * @param expr a long-run expression
	 * @param statesOfInterest states for which the expression has to be computed
	 * @param condition path event under which the relativized long-run is considered
	 * @return the relativized long-run value for each state of interest
	 * @throws PrismException
	 */
	// FIXME ALG: check types
	public StateValues checkConditionalExpressionLongRun(Model model, ExpressionLongRun expr, Expression condition, BitSet statesOfInterest) throws PrismException
	{
		if (model.getModelType() != ModelType.DTMC && model.getModelType() != ModelType.CTMC) {
			throw new PrismNotSupportedException("Explicit engine does not yet handle the L operator for " + model.getModelType() + "s");
		}
		assert model instanceof DTMC;
		assert this instanceof MCModelChecker;

		BitSet states = checkExpression(model, expr.getStates(), null).getBitSet();
		assert states != null : "Booolean result expected.";

		StateValues values = checkExpression(model, expr.getExpression(), states);
		assert values.getType() != TypeBool.getInstance() : "Non-Boolean values expected.";

		ReachBsccComputer<MCModelChecker<?>> reachComputer = new DTMCModelChecker.ReachBsccComputer<>((MCModelChecker<?>) this, (DTMC) model, condition);

		StateValues resultValues = computeLongRun((DTMC) model, values, states, reachComputer, statesOfInterest);
		OpRelOpBound opInfo = expr.getRelopBoundInfo(constantValues);
		// For =? properties, just return values
		if (opInfo.isNumeric()) {
			return resultValues;
		}
		// Otherwise, compare against bound to get set of satisfying states
		BitSet resultBooleans = resultValues.getBitSetFromInterval(opInfo.getRelOp(), opInfo.getBound());
		resultValues.clear();
		return StateValues.createFromBitSet(resultBooleans, model);
	}

	// FIXME ALG: check types
	protected StateValues computeLongRun(DTMC dtmc, StateValues values, BitSet states, ReachBsccComputer<?> reachComputer, BitSet statesOfInterest)
			throws PrismException
	{
		assert this instanceof MCModelChecker;
		// Store num states, fix statesOfInterest
		int numStates = dtmc.getNumStates();
		if (statesOfInterest == null) {
			statesOfInterest = new BitSet(numStates);
			statesOfInterest.flip(0, numStates);
		}

		// 1. compute steady-state probabilities or fetch from cache
		SteadyStateProbsExplicit steadyStateProbsBscc;
		if (SteadyStateCache.getInstance().isEnabled()) {
			SteadyStateCache cache = SteadyStateCache.getInstance();
			if (cache.containsSteadyStateProbs(dtmc)) {
				mainLog.println("\nTaking steady-state probabilities from cache.");
				steadyStateProbsBscc = cache.getSteadyStateProbs(dtmc);
			} else {
				mainLog.println("\nComputing steady-state probabilities.");
				steadyStateProbsBscc = SteadyStateProbs.computeCompact((MCModelChecker<?>) this, dtmc);
				mainLog.println("\nCaching steady-state probabilities.");
				cache.storeSteadyStateProbs(dtmc, steadyStateProbsBscc, settings);
			}
		} else {
			steadyStateProbsBscc = SteadyStateProbs.computeSimple((MCModelChecker<?>) this, dtmc);
		}
		BitSet nonBsccStates = steadyStateProbsBscc.getNonBsccStates();
		StateValues steadyStateProbs = StateValues.createFromDoubleArray(steadyStateProbsBscc.getSteadyStateProbabilities(), dtmc);

		// 2. compute weighted sums for each state-of-interest
		double[] numerator   = new double[numStates];
		double[] denominator = new double[numStates];
		// weightedValues = values x steady (filter by states)
		StateValues weightedValues = values.deepCopy();
		weightedValues.times(steadyStateProbs, states);
		// skip reach computation if each state-of-interest is in a bscc
		boolean computeReachProbs = statesOfInterest.intersects(nonBsccStates);
		for (BitSet bscc : steadyStateProbsBscc.getBSCCs()) {
			// compute intersection of states and current BSCC
			BitSet statesInBscc = (BitSet) bscc.clone();
			statesInBscc.and(states);
			if (statesInBscc.isEmpty()) {
				// no state in current BSCC, skip BSCC
				continue;
			}
			// compute probability to reach BSCC
			double[] reachProbs;
			if (computeReachProbs) {
				// if some state-of-interest is not in a BSCC:
				reachProbs = reachComputer.computeUntilProbs(nonBsccStates, bscc, statesOfInterest);
			} else {
				// each state-of-interest is in a BSCC
				BitSet probs = (BitSet) bscc.clone();
				probs.and(statesOfInterest);
				reachProbs = Utils.bitsetToDoubleArray(probs, numStates);
			}
			// compute weighted sum
			double sumWeight = (double) weightedValues.sumOverBitSet(statesInBscc);
			double sumSteady = (double) steadyStateProbs.sumOverBitSet(statesInBscc);
			for (OfInt iter = new IterableBitSet(statesOfInterest).iterator(); iter.hasNext();) {
				int state = iter.nextInt();
				numerator[state]   += reachProbs[state] * sumWeight;
				denominator[state] += reachProbs[state] * sumSteady;
			}
			// remove visited bscc states
			states.andNot(bscc);
			if (states.isEmpty()) {
				// no states left, skip remaining BSCCs
				break;
			}
		}

		// 3. compute final quotient
		double[] quotient = numerator;
		for (OfInt iter = new IterableBitSet(statesOfInterest).iterator(); iter.hasNext();) {
			int state = iter.nextInt();
			quotient[state] /= denominator[state];
		}
		return StateValues.createFromDoubleArray(quotient, dtmc);
	}

	/**
	 * Model check an S operator expression and return the values for all states.
	 */
	protected StateValues checkExpressionSteadyState(Model model, ExpressionSS expr, BitSet statesOfInterest) throws PrismException
	{
		// Get info from S operator
		OpRelOpBound opInfo = expr.getRelopBoundInfo(constantValues);
		MinMax minMax = opInfo.getMinMax(model.getModelType());

		// Compute probabilities
		StateValues probs = checkSteadyStateFormula(model, expr.getExpression(), minMax, statesOfInterest);

		// Print out probabilities
		if (getVerbosity() > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			probs.print(mainLog);
		}

		// For =? properties, just return values
		if (opInfo.isNumeric()) {
			return probs;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			BitSet sol = probs.getBitSetFromInterval(opInfo.getRelOp(), opInfo.getBound());
			probs.clear();
			return StateValues.createFromBitSet(sol, model);
		}
	}

	/**
	 * Compute steady-state probabilities for an S operator.
	 */
	protected StateValues checkSteadyStateFormula(Model model, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		throw new PrismNotSupportedException("Explicit engine does not yet handle the S operator for " + model.getModelType() + "s");
	}

	// Utility methods for probability distributions

	/**
	 * Generate a probability distribution, stored as a StateValues object, from a file.
	 * If {@code distFile} is null, so is the return value.
	 */
	public StateValues readDistributionFromFile(File distFile, Model model) throws PrismException
	{
		StateValues dist = null;

		if (distFile != null) {
			mainLog.println("\nImporting probability distribution from file \"" + distFile + "\"...");
			// Build an empty vector 
			dist = new StateValues(TypeDouble.getInstance(), model);
			// Populate vector from file
			dist.readFromFile(distFile);
		}

		return dist;
	}

	/**
	 * Build a probability distribution, stored as a StateValues object,
	 * from the initial states info of the current model: either probability 1 for
	 * the (single) initial state or equiprobable over multiple initial states.
	 */
	public StateValues buildInitialDistribution(Model model) throws PrismException
	{
		StateValues dist = null;

		// Build an empty vector 
		dist = new StateValues(TypeDouble.getInstance(), model);
		// Populate vector (equiprobable over initial states)
		double d = 1.0 / model.getNumInitialStates();
		for (int in : model.getInitialStates()) {
			dist.setDoubleValue(in, d);
		}

		return dist;
	}

	// Model transformations

	protected <M extends Model> ModelExpressionTransformation<M, M> replaceBoundsWithCounters(M model, Expression expr, TemporalOperatorBounds bounds, BitSet statesOfInterest, boolean keepSingleTimeBound)
			throws PrismLangException, PrismException
	{
		List<TemporalOperatorBound> boundsToReplace = bounds.getStepBoundsForDiscreteTime();
		if (keepSingleTimeBound && !boundsToReplace.isEmpty()) {
			boundsToReplace.remove(0);
		}
		boundsToReplace.addAll(bounds.getRewardBounds());

		return CounterTransformation.replaceBoundsWithCounters(this, model, expr, boundsToReplace, statesOfInterest);
	}
}
