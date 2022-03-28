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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;

import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationIntUnbounded;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.ModelType;
import parser.ast.RelOp;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.PrismUtils;
import acceptance.AcceptanceReach;
import acceptance.AcceptanceType;
import common.BitSetTools;
import common.IterableBitSet;
import common.IterableStateSet;
import common.StopWatch;
import explicit.conditional.ConditionalMCModelChecker;
import explicit.rewards.MCRewards;
import explicit.rewards.Rewards;

/**
 * Explicit-state model checker for discrete-time Markov chains (DTMCs).
 */
public class DTMCModelChecker extends ProbModelChecker implements MCModelChecker<DTMC>
{
	/**
	 * Create a new DTMCModelChecker, inherit basic state from parent (unless null).
	 */
	public DTMCModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	@Override
	public DTMCModelChecker createDTMCModelChecker()
	{
		return this;
	}

	// Model checking functions

	@Override
	public StateValues checkExpressionConditional(Model model, ExpressionConditional expression, BitSet statesOfInterest) throws PrismException
	{
		return new ConditionalMCModelChecker.DTMC(this).checkExpression((DTMC) model, expression, statesOfInterest);
	}

	@Override
	protected StateValues checkProbPathFormulaSimple(Model model, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// In continuous time case, defer to the standard handling without special treatments for rewards
		if (model.getModelType().continuousTime()) {
			return super.checkProbPathFormulaSimple(model, expr, minMax, statesOfInterest);
		}

		expr = Expression.convertSimplePathFormulaToCanonicalForm(expr);
		ExpressionTemporal exprTemp = Expression.getTemporalOperatorForSimplePathFormula(expr);

		if (exprTemp.getBounds().hasRewardBounds() ||
		    exprTemp.getBounds().countTimeBoundsDiscrete() > 1) {
			// Replace all time/reward bounds with counters except for first time bound
			// which is handled by standard simple path formula procedure.
			ModelExpressionTransformation<DTMC, DTMC> transformed = replaceBoundsWithCounters((DTMC) model, expr, exprTemp.getBounds(), statesOfInterest, true);
			mainLog.println("\nPerforming actual calculations for\n");
			mainLog.println(model.getModelType() + ": " + transformed.getTransformedModel().infoString());
			mainLog.println("Formula: " + transformed.getTransformedExpression() +"\n");

			// We can now delegate to ProbModelChecker.checkProbPathFormulaSimple as there is
			// at most one time / step bound remaining
			StateValues svTransformed = super.checkProbPathFormulaSimple(transformed.getTransformedModel(), transformed.getTransformedExpression(), minMax, transformed.getTransformedStatesOfInterest());
			return transformed.projectToOriginalModel(svTransformed);
		}
		// We are fine, delegate to ProbModelChecker.checkProbPathFormulaSimple
		return super.checkProbPathFormulaSimple(model, expr, minMax, statesOfInterest);
	}

	@Override
	protected StateValues checkProbPathFormulaLTL(Model model, Expression expr, boolean qual, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		LTLModelChecker mcLtl;
		StateValues probsProduct, probs;
		LTLModelChecker.LTLProduct<DTMC> product;
		DTMCModelChecker mcProduct;

		// For LTL model checking routines
		mcLtl = new LTLModelChecker(this);

		// Build product of Markov chain and automaton
		AcceptanceType[] allowedAcceptance = {
				AcceptanceType.RABIN,
				AcceptanceType.REACH,
				AcceptanceType.GENERIC
		};
		product = mcLtl.constructProductMC(this, (DTMC)model, expr, statesOfInterest, allowedAcceptance);

		// Output product, if required
		if (getExportProductTrans()) {
				mainLog.println("\nExporting product transition matrix to file \"" + getExportProductTransFilename() + "\"...");
				product.getProductModel().exportToPrismExplicitTra(getExportProductTransFilename());
		}
		if (getExportProductStates()) {
			mainLog.println("\nExporting product state space to file \"" + getExportProductStatesFilename() + "\"...");
			PrismFileLog out = new PrismFileLog(getExportProductStatesFilename());
			VarList newVarList = (VarList) modulesFile.createVarList().clone();
			String daVar = "_da";
			while (newVarList.getIndex(daVar) != -1) {
				daVar = "_" + daVar;
			}
			newVarList.addVar(0, new Declaration(daVar, new DeclarationIntUnbounded()), 1, null);
			int precision = settings.getInteger(PrismSettings.PRISM_EXPORT_MODEL_PRECISION);
			product.getProductModel().exportStates(Prism.EXPORT_PLAIN, newVarList, out,precision);
			out.close();
		}
		
		// Find accepting states + compute reachability probabilities
		BitSet acc;
		if (product.getAcceptance() instanceof AcceptanceReach) {
			mainLog.println("\nSkipping BSCC computation since acceptance is defined via goal states...");
			acc = ((AcceptanceReach)product.getAcceptance()).getGoalStates();
		} else {
			mainLog.println("\nFinding accepting BSCCs...");
			acc = mcLtl.findAcceptingBSCCs(product.getProductModel(), product.getAcceptance());
		}
		mainLog.println("\nComputing reachability probabilities...");
		mcProduct = new DTMCModelChecker(this);
		mcProduct.inheritSettings(this);
		probsProduct = StateValues.createFromDoubleArray(mcProduct.computeReachProbs(product.getProductModel(), acc).soln, product.getProductModel());

		// Mapping probabilities in the original model
		probs = product.projectToOriginalModel(probsProduct);
		probsProduct.clear();

		return probs;
	}

	/**
	 * Compute rewards for a co-safe LTL reward operator.
	 */
	protected StateValues checkRewardCoSafeLTL(Model model, Rewards modelRewards, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		LTLModelChecker mcLtl;
		MCRewards productRewards;
		StateValues rewardsProduct, rewards;
		DTMCModelChecker mcProduct;
		LTLModelChecker.LTLProduct<DTMC> product;

		// For LTL model checking routines
		mcLtl = new LTLModelChecker(this);
		mcLtl.disallowSimplificationsBasedOnModel();

		// Build product of Markov chain and automaton
		AcceptanceType[] allowedAcceptance = {
				AcceptanceType.RABIN,
				AcceptanceType.REACH
		};
		product = mcLtl.constructProductMC(this, (DTMC)model, expr, statesOfInterest, allowedAcceptance);
		
		// Adapt reward info to product model
		productRewards = ((MCRewards) modelRewards).liftFromModel(product);
		
		// Output product, if required
		if (getExportProductTrans()) {
				mainLog.println("\nExporting product transition matrix to file \"" + getExportProductTransFilename() + "\"...");
				product.getProductModel().exportToPrismExplicitTra(getExportProductTransFilename());
		}
		if (getExportProductStates()) {
			mainLog.println("\nExporting product state space to file \"" + getExportProductStatesFilename() + "\"...");
			PrismFileLog out = new PrismFileLog(getExportProductStatesFilename());
			VarList newVarList = (VarList) modulesFile.createVarList().clone();
			String daVar = "_da";
			while (newVarList.getIndex(daVar) != -1) {
				daVar = "_" + daVar;
			}
			newVarList.addVar(0, new Declaration(daVar, new DeclarationIntUnbounded()), 1, null);
			int precision = settings.getInteger(PrismSettings.PRISM_EXPORT_MODEL_PRECISION);
			product.getProductModel().exportStates(Prism.EXPORT_PLAIN, newVarList, out, precision);
			out.close();
		}
		
		// Find accepting states + compute reachability rewards
		BitSet acc;
		if (product.getAcceptance() instanceof AcceptanceReach) {
			mainLog.println("\nSkipping BSCC computation since acceptance is defined via goal states...");
			acc = ((AcceptanceReach)product.getAcceptance()).getGoalStates();
		} else {
			mainLog.println("\nFinding accepting BSCCs...");
			acc = mcLtl.findAcceptingBSCCs(product.getProductModel(), product.getAcceptance());
		}
		mainLog.println("\nComputing reachability probabilities...");
		mcProduct = new DTMCModelChecker(this);
		mcProduct.inheritSettings(this);
		rewardsProduct = StateValues.createFromDoubleArray(mcProduct.computeReachRewards(product.getProductModel(), productRewards, acc).soln, product.getProductModel());
		
		// Mapping rewards in the original model
		rewards = product.projectToOriginalModel(rewardsProduct);
		rewardsProduct.clear();
		
		return rewards;
	}
	
	public ModelCheckerResult computeInstantaneousRewards(DTMC dtmc, MCRewards mcRewards, double t) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;
		int right = (int) t;

		// Store num states
		n = dtmc.getNumStates();

		// Start backwards transient computation
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting backwards instantaneous rewards computation...");

		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];

		// Initialise solution vectors.
		for (i = 0; i < n; i++)
			soln[i] = mcRewards.getStateReward(i);

		// Start iterations
		for (iters = 0; iters < right; iters++) {
			// Matrix-vector multiply
			dtmc.mvMult(soln, soln2, null, false);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished backwards transient computation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Backwards transient instantaneous rewards computation");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	public ModelCheckerResult computeCumulativeRewards(DTMC dtmc, MCRewards mcRewards, double t) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;
		int right = (int) t;

		// Store num states
		n = dtmc.getNumStates();

		// Start backwards transient computation
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting backwards cumulative rewards computation...");

		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];

		// Start iterations
		for (iters = 0; iters < right; iters++) {
			// Matrix-vector multiply plus adding rewards
			dtmc.mvMult(soln, soln2, null, false);
			for (i = 0; i < n; i++) {
				soln2[i] += mcRewards.getStateReward(i);
			}
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished backwards transient computation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Backwards cumulative rewards computation");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	public ModelCheckerResult computeTotalRewards(DTMC dtmc, MCRewards mcRewards) throws PrismException
	{
		ModelCheckerResult res = null;
		int n, numBSCCs = 0;
		long timer;

		// Switch to a supported method, if necessary
		if (!(linEqMethod == LinEqMethod.POWER)) {
			linEqMethod = LinEqMethod.POWER;
			mainLog.printWarning("Switching to linear equation solution method \"" + linEqMethod.fullName() + "\"");
		}

		// Store num states
		n = dtmc.getNumStates();

		// Start total rewards computation
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting total reward computation...");

		// Compute bottom strongly connected components (BSCCs)
		SCCConsumerStore sccStore = new SCCConsumerStore(this, dtmc);
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, dtmc, sccStore);
		sccComputer.computeSCCs();
		List<BitSet> bsccs = sccStore.getBSCCs();
		numBSCCs = bsccs.size();

		// Find BSCCs with non-zero reward
		BitSet bsccsNonZero = new BitSet();
		for (int b = 0; b < numBSCCs; b++) {
			BitSet bscc = bsccs.get(b);
			for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1)) {
				if (mcRewards.getStateReward(i) > 0) {
					bsccsNonZero.or(bscc);
					break;
				}
			}
		}
		mainLog.print("States in non-zero reward BSCCs: " + bsccsNonZero.cardinality());
		
		// Find states with infinite reward (those reach a non-zero reward BSCC with prob > 0)
		BitSet inf;
		if (preRel) {
			inf = prob0(dtmc, null, bsccsNonZero, dtmc.getPredecessorRelation(this, true));
		} else {
			inf = prob0(dtmc, null, bsccsNonZero);
		}
		inf.flip(0, n);
		int numInf = inf.cardinality();
		mainLog.println(", inf=" + numInf + ", maybe=" + (n - numInf));
		
		// Compute rewards
		// (do this using the functions for "reward reachability" properties but with no targets)
		switch (linEqMethod) {
		case POWER:
			res = computeReachRewardsValIter(dtmc, mcRewards, new BitSet(), inf, null, null);
			break;
		case GAUSS_SEIDEL:
			if (dtmc instanceof DTMCSparse) {
				res = computeReachRewardsGaussSeidel((DTMCSparse) dtmc, mcRewards, new BitSet(), inf, null, null);
				break;
			}
		default:
			throw new PrismException("Unknown linear equation solution method " + linEqMethod.fullName());
		}

		// Finished total reward computation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Total reward computation");
		mainLog.println(" took " + timer / 1000.0 + " seconds.");

		// Return results
		return res;
	}

	// Steady-state/transient probability computation

	/**
	 * Compute steady-state probability distribution (forwards).
	 * Start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doSteadyState(DTMC dtmc) throws PrismException
	{
		return doSteadyState(dtmc, (StateValues) null);
	}

	/**
	 * Compute steady-state probability distribution (forwards).
	 * Optionally, use the passed in file initDistFile to give the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doSteadyState(DTMC dtmc, File initDistFile) throws PrismException
	{
		StateValues initDist = readDistributionFromFile(initDistFile, dtmc);
		return doSteadyState(dtmc, initDist);
	}

	/**
	 * Compute steady-state probability distribution (forwards).
	 * Optionally, use the passed in vector initDist as the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 * For reasons of efficiency, when a vector is passed in, it will be trampled over,
	 * so if you wanted it, take a copy. 
	 * @param dtmc The DTMC
	 * @param initDist Initial distribution (will be overwritten)
	 */
	public StateValues doSteadyState(DTMC dtmc, StateValues initDist) throws PrismException
	{
		StateValues initDistNew = (initDist == null) ? buildInitialDistribution(dtmc) : initDist;
		ModelCheckerResult res = computeSteadyStateProbs(dtmc, initDistNew.getDoubleArray());
		return StateValues.createFromDoubleArray(res.soln, dtmc);
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Optionally, use the passed in vector initDist as the initial probability distribution (time step 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 * For reasons of efficiency, when a vector is passed in, it will be trampled over,
	 * so if you wanted it, take a copy. 
	 * @param dtmc The DTMC
	 * @param k Time step
	 * @param initDist Initial distribution (will be overwritten)
	 */
	public StateValues doTransient(DTMC dtmc, int k, double initDist[]) throws PrismException
	{
		throw new PrismNotSupportedException("Not implemented yet");
	}

	// Numerical computation functions

	/**
	 * Compute next=state probabilities.
	 * i.e. compute the probability of being in a state in {@code target} in the next step.
	 * @param dtmc The DTMC
	 * @param target Target states
	 */
	public ModelCheckerResult computeNextProbs(DTMC dtmc, BitSet target) throws PrismException
	{
		ModelCheckerResult res = null;
		int n;
		double soln[], soln2[];
		long timer;

		timer = System.currentTimeMillis();

		// Store num states
		n = dtmc.getNumStates();

		// Create/initialise solution vector(s)
		soln = Utils.bitsetToDoubleArray(target, n);
		soln2 = new double[n];

		// Next-step probabilities 
		dtmc.mvMult(soln, soln2, null, false);

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln2;
		res.numIters = 1;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Given a value vector x, compute the probability:
	 *   v(s) = Sum_s' P(s,s')*x(s')   for s labeled with a,
	 *   v(s) = 0                      for s not labeled with a.
	 *
	 * @param dtmc the DTMC model
	 * @param a the set of states labeled with a
	 * @param x the value vector
	 */
	protected double[] computeRestrictedNext(DTMC dtmc, BitSet a, double[] x)
	{
		double[] soln;
		int n;

		// Store num states
		n = dtmc.getNumStates();

		// initialized to 0.0
		soln = new double[n];

		// Next-step probabilities multiplication
		// restricted to a states
		dtmc.mvMult(x, soln, a, false);

		return soln;
	}

	/**
	 * Compute reachability probabilities.
	 * i.e. compute the probability of reaching a state in {@code target}.
	 * @param dtmc The DTMC
	 * @param target Target states
	 */
	public ModelCheckerResult computeReachProbs(DTMC dtmc, BitSet target) throws PrismException
	{
		return computeReachProbs(dtmc, null, target, null, null);
	}

	/**
	 * Compute until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 */
	public ModelCheckerResult computeUntilProbs(DTMC dtmc, BitSet remain, BitSet target) throws PrismException
	{
		return computeReachProbs(dtmc, remain, target, null, null);
	}

	/**
	 * Compute reachability/until probabilities.
	 * i.e. compute the min/max probability of reaching a state in {@code target},
	 * while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param init Optionally, an initial solution vector (may be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult computeReachProbs(DTMC dtmc, BitSet remain, BitSet target, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet no, yes;
		int n, numYes, numNo;
		long timer, timerProb0, timerProb1;
		PredecessorRelation pre = null;
		// Local copy of setting
		LinEqMethod linEqMethod = this.linEqMethod;

		// Switch to a supported method, if necessary
		if (!(linEqMethod == LinEqMethod.POWER || linEqMethod == LinEqMethod.GAUSS_SEIDEL)) {
			linEqMethod = LinEqMethod.GAUSS_SEIDEL;
			mainLog.printWarning("Switching to linear equation solution method \"" + linEqMethod.fullName() + "\"");
		}

		// Start probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting probabilistic reachability...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		dtmc.checkForDeadlocks(target);

		// Store num states
		n = dtmc.getNumStates();

		// Optimise by enlarging target set (if more info is available)
		if (init != null && known != null && !known.isEmpty()) {
			BitSet targetNew = (BitSet) target.clone();
			for (int i : new IterableBitSet(known)) {
				if (init[i] == 1.0) {
					targetNew.set(i);
				}
			}
			target = targetNew;
		}

		// If required, export info about target states
		if (getExportTarget()) {
			BitSet bsInit = new BitSet(n);
			for (int i = 0; i < n; i++) {
				bsInit.set(i, dtmc.isInitialState(i));
			}
			List<BitSet> labels = Arrays.asList(bsInit, target);
			List<String> labelNames = Arrays.asList("init", "target");
			mainLog.println("\nExporting target states info to file \"" + getExportTargetFilename() + "\"...");
			exportLabels(dtmc, labels, labelNames, Prism.EXPORT_PLAIN, new PrismFileLog(getExportTargetFilename()));
		}

		if (precomp && (prob0 || prob1) && preRel) {
			pre = dtmc.getPredecessorRelation(this, true);
		}

		// Precomputation
		timerProb0 = System.currentTimeMillis();
		if (precomp && prob0) {
			if (preRel) {
				no = prob0(dtmc, remain, target, pre);
			} else {
				no = prob0(dtmc, remain, target);
			}
		} else {
			no = new BitSet();
		}
		timerProb0 = System.currentTimeMillis() - timerProb0;
		timerProb1 = System.currentTimeMillis();
		if (precomp && prob1) {
			if (preRel) {
				yes = prob1(dtmc, remain, target, pre);
			} else {
				yes = prob1(dtmc, remain, target);
			}
		} else {
			yes = (BitSet) target.clone();
		}
		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numYes = yes.cardinality();
		numNo = no.cardinality();
		mainLog.println("target=" + target.cardinality() + ", yes=" + numYes + ", no=" + numNo + ", maybe=" + (n - (numYes + numNo)));

		// Compute probabilities
		switch (linEqMethod) {
		case POWER:
			res = computeReachProbsValIter(dtmc, no, yes, init, known);
			break;
		case GAUSS_SEIDEL:
			res = computeReachProbsGaussSeidel(dtmc, no, yes, init, known);
			break;
		default:
			throw new PrismException("Unknown linear equation solution method " + linEqMethod.fullName());
		}

		// Finished probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Probabilistic reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;
		res.timeProb0 = timerProb0 / 1000.0;
		res.timePre = (timerProb0 + timerProb1) / 1000.0;

		return res;
	}


	/**
	 * Prob0 precomputation algorithm (using predecessor relation),
	 * i.e. determine the states of a DTMC which, with probability 0,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: {@code null} means "all states")
	 * @param target Target states
	 * @param pre The predecessor relation
	 */
	@Override
	public BitSet prob0(DTMC dtmc, BitSet remain, BitSet target, PredecessorRelation pre)
	{

		// Start precomputation
		long timer = System.currentTimeMillis();
		mainLog.println("Starting Prob0...");

		int numStates = dtmc.getNumStates();

		// Special case: no target states
		if (target.isEmpty()) {
			BitSet soln = new BitSet(numStates);
			soln.set(0, numStates);
			return soln;
		}

		// calculate all states that can reach 'target'
		// while remaining in 'remain' in the underlying graph,
		// where all the 'target' states are made absorbing
		BitSet canReachTarget = pre.calculatePreStar(remain, target, target);

		// in-place complement: prob0 = S\Pre*(target)
		BitSet result = canReachTarget;
		result.flip(0, numStates);

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Prob0");
		mainLog.println(" took " + timer / 1000.0 + " seconds.");

		return result;
	}

	/**
	 * Prob0 precomputation algorithm (using a fixed-point computation),
	 * i.e. determine the states of a DTMC which, with probability 0,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: {@code null} means "all")
	 * @param target Target states
	 */
	public BitSet prob0(DTMC dtmc, BitSet remain, BitSet target)
	{
		int n, iters;
		BitSet u, soln, unknown;
		boolean u_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		mainLog.println("Starting Prob0...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			soln = new BitSet(dtmc.getNumStates());
			soln.set(0, dtmc.getNumStates());
			return soln;
		}

		// Initialise vectors
		n = dtmc.getNumStates();
		u = new BitSet(n);
		soln = new BitSet(n);

		// Determine set of states actually need to perform computation for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		if (remain != null)
			unknown.and(remain);

		// Fixed point loop
		iters = 0;
		u_done = false;
		// Least fixed point - should start from 0 but we optimise by
		// starting from 'target', thus bypassing first iteration
		u.or(target);
		soln.or(target);
		while (!u_done) {
			iters++;
			// Single step of Prob0
			dtmc.prob0step(unknown, u, soln);
			// Check termination
			u_done = soln.equals(u);
			// u = soln
			u.clear();
			u.or(soln);
		}

		// Negate
		u.flip(0, n);

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Prob0");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		return u;
	}

	/**
	 * Prob1 precomputation algorithm (using predecessor relation),
	 * i.e. determine the states of a DTMC which, with probability 1,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param pre The predecessor relation of the DTMC
	 */
	@Override
	public BitSet prob1(DTMC dtmc, BitSet remain, BitSet target, PredecessorRelation pre)
	{
		// Implements the constrained reachability algorithm from
		// Baier, Katoen: Principles of Model Checking (Corollary 10.31 Qualitative Constrained Reachability)

		// Start precomputation
		long timer = System.currentTimeMillis();
		mainLog.println("Starting Prob1...");

		// Special case: no 'target' states
		if (target.isEmpty()) {
			// empty set
			return new BitSet(0);
		}

		int numStates = dtmc.getNumStates();

		// mark all states in 'target' and all states not in 'remain' as absorbing
		BitSet absorbing;
		if (remain != null) {
			// union of complement of remain and target
			absorbing = BitSetTools.complement(numStates, remain);
			absorbing.or(target);
		} else {
			// for remain == null, remain consists of all states
			// thus, absorbing = target
			absorbing = (BitSet) target.clone();
		}

		// M' = DTMC where all 'absorbing' states are considered to be absorbing

		// set of states satisfying E [ F target ] in M'
		// Pre*(target)
		BitSet canReachTarget = pre.calculatePreStar(null, target, absorbing);

		// set of states satisfying A [ G !target ] in M'
		// in-place complement: S\Pre*(target)
		BitSet canNotReachTarget = canReachTarget;
		canNotReachTarget.flip(0, numStates);

		// set of states that can reach canNotReachTarget in M'
		// Pre*(S\Pre*(target))
		BitSet probTargetNot1 = pre.calculatePreStar(null, canNotReachTarget, absorbing);

		// set of states that can never reach canNotReachTarget in M'
		// in-place complement: S\Pre*(S\Pre*(target))
		BitSet result = probTargetNot1;
		result.flip(0, numStates);

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Prob1");
		mainLog.println(" took " + timer / 1000.0 + " seconds.");

		return result;
	}

	/**
	 * Prob1 precomputation algorithm (using a fixed-point computation)
	 * i.e. determine the states of a DTMC which, with probability 1,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: {@code null} means "all")
	 * @param target Target states
	 */
	public BitSet prob1(DTMC dtmc, BitSet remain, BitSet target)
	{
		int n, iters;
		BitSet u, v, soln, unknown;
		boolean u_done, v_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		mainLog.println("Starting Prob1...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			return new BitSet(dtmc.getNumStates());
		}

		// Initialise vectors
		n = dtmc.getNumStates();
		u = new BitSet(n);
		v = new BitSet(n);
		soln = new BitSet(n);

		// Determine set of states actually need to perform computation for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		if (remain != null)
			unknown.and(remain);

		// Nested fixed point loop
		iters = 0;
		u_done = false;
		// Greatest fixed point
		u.set(0, n);
		while (!u_done) {
			v_done = false;
			// Least fixed point - should start from 0 but we optimise by
			// starting from 'target', thus bypassing first iteration
			v.clear();
			v.or(target);
			soln.clear();
			soln.or(target);
			while (!v_done) {
				iters++;
				// Single step of Prob1
				dtmc.prob1step(unknown, u, v, soln);
				// Check termination (inner)
				v_done = soln.equals(v);
				// v = soln
				v.clear();
				v.or(soln);
			}
			// Check termination (outer)
			u_done = v.equals(u);
			// u = v
			u.clear();
			u.or(v);
		}

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Prob1");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		return u;
	}

	/**
	 * Compute reachability probabilities using value iteration.
	 * @param dtmc The DTMC
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	protected ModelCheckerResult computeReachProbsValIter(DTMC dtmc, BitSet no, BitSet yes, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[], initVal;
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting value iteration...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;

		// Initialise solution vectors. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 1.0/0.0 if in yes/no; (3) passed in initial value; (4) initVal
		// where initVal is 0.0 or 1.0, depending on whether we converge from below/above. 
		initVal = (valIterDir == ValIterDir.BELOW) ? 0.0 : 1.0;
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = known.get(i) ? init[i] : yes.get(i) ? 1.0 : no.get(i) ? 0.0 : init[i];
			} else {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : init[i];
			}
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : initVal;
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(yes);
		unknown.andNot(no);
		if (known != null)
			unknown.andNot(known);

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			iters++;
			// Matrix-vector multiply
			dtmc.mvMult(soln, soln2, unknown, false);
			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, termCritParam, termCrit == TermCrit.ABSOLUTE);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Value iteration");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Non-convergence is an error (usually)
		if (!done && errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.";
			msg += "\nConsider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute reachability probabilities using Gauss-Seidel.
	 * @param dtmc The DTMC
	 * @param no Probability 0 states
	 * @param yes Probability 1 states
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	protected ModelCheckerResult computeReachProbsGaussSeidel(DTMC dtmc, BitSet no, BitSet yes, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], initVal, maxDiff;
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting Gauss-Seidel...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector
		soln = (init == null) ? new double[n] : init;

		// Initialise solution vector. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 1.0/0.0 if in yes/no; (3) passed in initial value; (4) initVal
		// where initVal is 0.0 or 1.0, depending on whether we converge from below/above. 
		initVal = (valIterDir == ValIterDir.BELOW) ? 0.0 : 1.0;
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					soln[i] = known.get(i) ? init[i] : yes.get(i) ? 1.0 : no.get(i) ? 0.0 : init[i];
			} else {
				for (i = 0; i < n; i++)
					soln[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : init[i];
			}
		} else {
			for (i = 0; i < n; i++)
				soln[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : initVal;
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(yes);
		unknown.andNot(no);
		if (known != null)
			unknown.andNot(known);
		IterableStateSet subset = new IterableStateSet(unknown, n);

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			iters++;
			// Matrix-vector multiply
			maxDiff = dtmc.mvMultGS(soln, subset, termCrit == TermCrit.ABSOLUTE);
			// Check termination
			done = maxDiff < termCritParam;
		}

		// Finished Gauss-Seidel
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Gauss-Seidel");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Non-convergence is an error (usually)
		if (!done && errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.";
			msg += "\nConsider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute bounded reachability probabilities.
	 * i.e. compute the probability of reaching a state in {@code target} within k steps.
	 * @param dtmc The DTMC
	 * @param target Target states
	 * @param k Bound
	 */
	public ModelCheckerResult computeBoundedReachProbs(DTMC dtmc, BitSet target, int k) throws PrismException
	{
		return computeBoundedReachProbs(dtmc, null, target, k, null, null);
	}

	/**
	 * Compute bounded until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * within k steps, and while remaining in states in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param k Bound
	 */
	public ModelCheckerResult computeBoundedUntilProbs(DTMC dtmc, BitSet remain, BitSet target, int k) throws PrismException
	{
		return computeBoundedReachProbs(dtmc, remain, target, k, null, null);
	}

	/**
	 * Compute bounded reachability/until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * within k steps, and while remaining in states in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param k Bound
	 * @param init Initial solution vector - pass null for default
	 * @param results Optional array of size b+1 to store (init state) results for each step (null if unused)
	 */
	public ModelCheckerResult computeBoundedReachProbs(DTMC dtmc, BitSet remain, BitSet target, int k, double init[], double results[]) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;

		// Start bounded probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting bounded probabilistic reachability...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;

		// Initialise solution vectors. Use passed in initial vector, if present
		if (init != null) {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 1.0 : init[i];
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 1.0 : 0.0;
		}
		// Store intermediate results if required
		// (compute min/max value over initial states for first step)
		if (results != null) {
			// TODO: whether this is min or max should be specified somehow
			results[0] = Utils.minMaxOverArraySubset(soln2, dtmc.getInitialStates(), true);
		}

		// Determine set of states actually need to perform computation for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		if (remain != null)
			unknown.and(remain);

		// Start iterations
		iters = 0;
		while (iters < k) {

			iters++;
			// Matrix-vector multiply
			dtmc.mvMult(soln, soln2, unknown, false);
			// Store intermediate results if required
			// (compute min/max value over initial states for this step)
			if (results != null) {
				// TODO: whether this is min or max should be specified somehow
				results[iters] = Utils.minMaxOverArraySubset(soln2, dtmc.getInitialStates(), true);
			}
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished bounded probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Bounded probabilistic reachability");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	/**
	 * Compute expected reachability rewards.
	 * @param dtmc The DTMC
	 * @param mcRewards The rewards
	 * @param target Target states
	 */
	public ModelCheckerResult computeReachRewards(DTMC dtmc, MCRewards mcRewards, BitSet target) throws PrismException
	{
		return computeReachRewards(dtmc, mcRewards, target, null, null);
	}

	/**
	 * Compute expected reachability rewards.
	 * @param dtmc The DTMC
	 * @param mcRewards The rewards
	 * @param target Target states
	 * @param init Optionally, an initial solution vector (may be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult computeReachRewards(DTMC dtmc, MCRewards mcRewards, BitSet target, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet inf;
		int n, numTarget, numInf;
		long timer, timerProb1;
		// Local copy of setting
		LinEqMethod linEqMethod = this.linEqMethod;

		// Switch to a supported method, if necessary
		if (!(linEqMethod == LinEqMethod.POWER || (linEqMethod == LinEqMethod.GAUSS_SEIDEL && dtmc instanceof DTMCSparse))) {
			linEqMethod = LinEqMethod.POWER;
			mainLog.printWarning("Switching to linear equation solution method \"" + linEqMethod.fullName() + "\"");
		}

		// Start expected reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting expected reachability...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		dtmc.checkForDeadlocks(target);

		// Store num states
		n = dtmc.getNumStates();

		// Optimise by enlarging target set (if more info is available)
		if (init != null && known != null && !known.isEmpty()) {
			BitSet targetNew = (BitSet) target.clone();
			for (int i : new IterableBitSet(known)) {
				if (init[i] == 1.0) {
					targetNew.set(i);
				}
			}
			target = targetNew;
		}

		// Precomputation (not optional)
		timerProb1 = System.currentTimeMillis();
		if (preRel) {
			PredecessorRelation pre = dtmc.getPredecessorRelation(this, true);
			inf = prob1(dtmc, null, target, pre);
		} else {
			inf = prob1(dtmc, null, target);
		}
		inf.flip(0, n);
		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numTarget = target.cardinality();
		numInf = inf.cardinality();
		mainLog.println("target=" + numTarget + ", inf=" + numInf + ", rest=" + (n - (numTarget + numInf)));

		// Compute rewards
		switch (linEqMethod) {
		case POWER:
			res = computeReachRewardsValIter(dtmc, mcRewards, target, inf, init, known);
			break;
		case GAUSS_SEIDEL:
			if (dtmc instanceof DTMCSparse) {
				res = computeReachRewardsGaussSeidel((DTMCSparse) dtmc, mcRewards, target, inf, init, known);
				break;
			}
		default:
			throw new PrismException("Unknown linear equation solution method " + linEqMethod.fullName());
		}

		// Finished expected reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Expected reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;
		res.timePre = timerProb1 / 1000.0;

		return res;
	}

	/**
	 * Compute expected reachability rewards using value iteration.
	 * @param dtmc The DTMC
	 * @param mcRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult computeReachRewardsValIter(DTMC dtmc, MCRewards mcRewards, BitSet target, BitSet inf, double init[], BitSet known)
			throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting value iteration...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;

		// Initialise solution vectors. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 0.0/infinity if in target/inf; (3) passed in initial value; (4) 0.0
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = known.get(i) ? init[i] : target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			} else {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			}
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : 0.0;
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		unknown.andNot(inf);
		if (known != null)
			unknown.andNot(known);

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			//mainLog.println(soln);
			iters++;
			// Matrix-vector multiply
			dtmc.mvMultRew(soln, mcRewards, soln2, unknown, false);
			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, termCritParam, termCrit == TermCrit.ABSOLUTE);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Value iteration");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Non-convergence is an error (usually)
		if (!done && errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.";
			msg += "\nConsider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute expected reachability rewards using value iteration.
	 * @param dtmc The DTMC
	 * @param mcRewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param init Optionally, an initial solution vector (will be overwritten)
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult computeReachRewardsGaussSeidel(DTMCSparse dtmc, MCRewards mcRewards, BitSet target, BitSet inf, double init[], BitSet known)
			throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], maxDiff;
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting Gauss-Seidel...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector
		soln = (init == null) ? new double[n] : init;

		// Initialise solution vector. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 0.0/infinity if in target/inf; (3) passed in initial value; (4) 0.0
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					soln[i] = known.get(i) ? init[i] : target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			} else {
				for (i = 0; i < n; i++)
					soln[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			}
		} else {
			for (i = 0; i < n; i++)
				soln[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : 0.0;
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet(n);
		unknown.set(0, n);
		unknown.andNot(target);
		unknown.andNot(inf);
		if (known != null)
			unknown.andNot(known);
		IterableStateSet subset = new IterableStateSet(unknown, n);

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			iters++;
			// Matrix-vector multiply
			maxDiff = dtmc.mvMultRewGS(soln,  mcRewards, subset, termCrit == TermCrit.ABSOLUTE);
			// Check termination
			done = maxDiff < termCritParam;
		}

		// Finished Gauss-Seidel
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Gauss-Seidel");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Non-convergence is an error (usually)
		if (!done && errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.";
			msg += "\nConsider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	// L operator

	protected static class ReachBsccComputer<T extends MCModelChecker<?>>
	{
		T mc;
		DTMC model;
		String bsccName                     = null;
		String nonBsccName                  = null;
		ExpressionProb untilBscc            = null;
		ExpressionConditional condReachBscc = null;

		public ReachBsccComputer(T mc, DTMC model, Expression condition)
		{
			this.mc = mc;
			this.model = model;
			if (condition != null) {
				bsccName      = model.addUniqueLabel("current_bscc", new BitSet(0));
				nonBsccName   = model.addUniqueLabel("non_bscc_states", new BitSet(0));
				untilBscc     = new ExpressionProb(Expression.Until(new ExpressionLabel(nonBsccName), new ExpressionLabel(bsccName)), RelOp.COMPUTE_VALUES.toString(), null);
				condReachBscc = new ExpressionConditional(untilBscc, condition);
			}
		}

		public ReachBsccComputer<T> clear()
		{
			if (bsccName != null) {
				model.getLabelStates(bsccName).clear();
				model.getLabelStates(nonBsccName).clear();
				bsccName      = null;
				nonBsccName   = null;
				untilBscc     = null;
				condReachBscc = null;
			}
			return this;
		}

		public double[] computeUntilProbs(BitSet nonBsccStates, BitSet bsccStates, BitSet statesOfInterest) throws PrismException
		{
			if (bsccName == null) {
				return mc.createDTMCModelChecker().computeUntilProbs(model, nonBsccStates, bsccStates).soln;
			}
			// switch label to current BSCC and compute conditional reach probs
			model.addLabel(bsccName, bsccStates);
			model.addLabel(nonBsccName, nonBsccStates);
			return mc.checkExpressionConditional(model, condReachBscc, statesOfInterest).getDoubleArray();
		}
	}

	/**
	 * An interface for a post-processor, taking a solution vector over
	 * the whole state space and applying some post-processing on the
	 * solution for a given BSCC (with the state indices given by a BitSet).
	 * <br>
	 * This post-processor may only assume that the values in the solution vector
	 * corresponding to the BSCC states are valid and may only write to those values,
	 * the other values in the vector should not be changed.
	 */
	@FunctionalInterface
	public interface BSCCPostProcessor {
		public void apply(double soln[], BitSet bscc);
	};

	/**
	 * Compute (forwards) steady-state probabilities
	 * i.e. compute the long-run probability of being in each state,
	 * assuming the initial distribution {@code initDist}.
	 * For space efficiency, the initial distribution vector will be modified and values over-written,
	 * so if you wanted it, take a copy.
	 * @param dtmc The DTMC
	 * @param initDist Initial distribution (will be overwritten)
	 */
	public ModelCheckerResult computeSteadyStateProbs(DTMC dtmc, double initDist[]) throws PrismException
	{
		return computeSteadyStateProbs(dtmc, initDist, null);
	}

	/**
	 * Compute (forwards) steady-state probabilities
	 * i.e. compute the long-run probability of being in each state,
	 * assuming the initial distribution {@code initDist}. 
	 * For space efficiency, the initial distribution vector will be modified and values over-written,  
	 * so if you wanted it, take a copy. 
	 * @param dtmc The DTMC
	 * @param initDist Initial distribution (will be overwritten)
	 * @param processor Post-processor for the values of each BSCC (optional: null means no post-processing)
	 */
	public ModelCheckerResult computeSteadyStateProbs(DTMC dtmc, double initDist[], BSCCPostProcessor bsccPostProcessor) throws PrismException
	{
		StopWatch watch = new StopWatch().start();

		// Store num states
		int numStates = dtmc.getNumStates();
		// Create results vector
		double[] solnProbs = new double[numStates];

		// Compute bottom strongly connected components (BSCCs)
		SCCConsumerStore sccStore = new SCCConsumerStore(this, dtmc);
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, dtmc, sccStore);
		sccComputer.computeSCCs();
		List<BitSet> bsccs = sccStore.getBSCCs();
		BitSet notInBSCCs = sccStore.getNotInBSCCs();
		int numBSCCs = bsccs.size();

		// Compute support of initial distribution
		int numInit = 0;
		BitSet init = new BitSet();
		for (int i = 0; i < numStates; i++) {
			if (initDist[i] > 0)
				init.set(i);
				numInit++;
		}
		// Determine whether initial states are all in the same BSCC
		int initInOneBSCC = -1;
		for (int b = 0; b < numBSCCs; b++) {
			// test subset via setminus
			BitSet notInB = (BitSet) init.clone();
			notInB.andNot(bsccs.get(b));
			if (notInB.isEmpty()) {
				// all init states in b
				// >> finish
				initInOneBSCC = b;
				break;
			} else if (notInB.cardinality() < numInit) {
				// some init states in b and some not
				// >> abort
				break;
			}
			// no init state in b
			// >> try next BSCC
		}

		// If all initial states are in the same BSCC, it's easy...
		// Just compute steady-state probabilities for the BSCC
		if (initInOneBSCC > -1) {
			mainLog.println("\nInitial states are all in one BSCC (so no reachability probabilities computed)");
			BitSet bscc = bsccs.get(initInOneBSCC);
			computeSteadyStateProbsForBSCC(dtmc, bscc, solnProbs, bsccPostProcessor);
		}

		// Otherwise, have to consider all the BSCCs
		else {
			// Compute probability of reaching each BSCC from initial distribution 
			double[] probBSCCs = new double[numBSCCs];
			for (int b = 0; b < numBSCCs; b++) {
				mainLog.println("\nComputing probability of reaching BSCC " + (b + 1));
				BitSet bscc = bsccs.get(b);
				// Compute probabilities
				double[] reachProbs = computeUntilProbs(dtmc, notInBSCCs, bscc).soln;
				// Compute probability of reaching BSCC, which is dot product of
				// vectors for initial distribution and probabilities of reaching it
				probBSCCs[b] = 0.0;
				for (int i = 0; i < numStates; i++) {
					probBSCCs[b] += initDist[i] * reachProbs[i];
				}
				mainLog.print("\nProbability of reaching BSCC " + (b + 1) + ": " + probBSCCs[b] + "\n");
			}

			// Compute steady-state probabilities for each BSCC 
			for (int b = 0; b < numBSCCs; b++) {
				mainLog.println("\nComputing steady-state probabilities for BSCC " + (b + 1));
				BitSet bscc = bsccs.get(b);
				// Compute steady-state probabilities for the BSCC
				computeSteadyStateProbsForBSCC(dtmc, bscc, solnProbs, bsccPostProcessor);
				// Multiply by BSCC reach prob
				for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1))
					solnProbs[i] *= probBSCCs[b];
			}
		}

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = solnProbs;
		res.timeTaken = watch.elapsedSeconds();
		return res;
	}

	/**
	 * Compute steady-state probabilities for an S operator.
	 */
	@Override
	protected StateValues checkSteadyStateFormula(Model model, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		if (model.getModelType() != ModelType.DTMC) {
			throw new PrismNotSupportedException("Explicit engine does not yet handle the S operator for " + model.getModelType() + "s");
		}

		// Model check operand for all states
		BitSet bits = checkExpression(model, expr, null).getBitSet();
		assert bits != null : "Booolean result expected.";
		double[] values = Utils.bitsetToDoubleArray(bits, model.getNumStates());

		if (settings.getBoolean(PrismSettings.PRISM_COMPUTE_S_VIA_L)) {
			// Consider all states
			BitSet states = new BitSet(model.getNumStates());
			states.flip(0, model.getNumStates());
			ReachBsccComputer<DTMCModelChecker> reachComputer = new ReachBsccComputer<>(this, (DTMC) model, null);
			return computeLongRun((DTMC) model, StateValues.createFromDoubleArray(values, model), states, reachComputer, statesOfInterest);
		} else {
			ModelCheckerResult result = computeSteadyStateBackwardsProbs((DTMC) model, values);
			return StateValues.createFromDoubleArray(result.soln, model);
		}
	}

	/**
	 * Perform (backwards) steady-state probabilities, as required for (e.g. CSL) model checking.
	 * Compute, for each initial state s, the sum over all states s'
	 * of the steady-state probability of being in s'
	 * multiplied by the corresponding probability in the vector {@code multProbs}.
	 * If {@code multProbs} is null, it is assumed to be all 1s.
	 * @param dtmc The DTMC
	 * @param multProbs Multiplication vector (optional: null means all 1s)
	 */
	public ModelCheckerResult computeSteadyStateBackwardsProbs(DTMC dtmc, double multProbs[]) throws PrismException
	{
		StopWatch watch = new StopWatch().start();

		// Store num states
		int numStates = dtmc.getNumStates();

		// Compute bottom strongly connected components (BSCCs)
		SCCConsumerStore sccStore = new SCCConsumerStore(this, dtmc);
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, dtmc, sccStore);
		sccComputer.computeSCCs();
		List<BitSet> bsccs = sccStore.getBSCCs();
		BitSet notInBSCCs = sccStore.getNotInBSCCs();
		int numBSCCs = bsccs.size();

		// Compute steady-state probability for each BSCC...
		double[] probBSCCs = new double[numBSCCs];
		double[] ssProbs = new double[numStates];
		for (int b = 0; b < numBSCCs; b++) {
			mainLog.println("\nComputing steady state probabilities for BSCC " + (b + 1));
			BitSet bscc = bsccs.get(b);
			// Compute steady-state probabilities for the BSCC
			computeSteadyStateProbsForBSCC(dtmc, bscc, ssProbs);
			// Compute weighted sum of probabilities with multProbs
			probBSCCs[b] = 0.0;
			if (multProbs == null) {
				for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1)) {
					probBSCCs[b] += ssProbs[i];
				}
			} else {
				for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1)) {
					probBSCCs[b] += multProbs[i] * ssProbs[i];
				}
			}
			mainLog.print("\nValue for BSCC " + (b + 1) + ": " + probBSCCs[b] + "\n");
		}

		// Create/initialise prob vector
		double[] soln = new double[numStates];
		for (int i = 0; i < numStates; i++) {
			soln[i] = 0.0;
		}

		// If every state is in a BSCC, it's much easier...
		if (notInBSCCs.isEmpty()) {
			mainLog.println("\nAll states are in BSCCs (so no reachability probabilities computed)");
			for (int b = 0; b < numBSCCs; b++) {
				BitSet bscc = bsccs.get(b);
				for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1))
					soln[i] += probBSCCs[b];
			}
		}

		// Otherwise we have to do more work...
		else {
			// Compute probabilities of reaching each BSCC...
			for (int b = 0; b < numBSCCs; b++) {
				// Skip BSCCs with zero probability
				if (probBSCCs[b] == 0.0)
					continue;
				mainLog.println("\nComputing probabilities of reaching BSCC " + (b + 1));
				BitSet bscc = bsccs.get(b);
				// Compute probabilities
				double[] reachProbs = computeUntilProbs(dtmc, notInBSCCs, bscc).soln;
				// Multiply by value for BSCC, add to total
				for (int i = 0; i < numStates; i++) {
					soln[i] += reachProbs[i] * probBSCCs[b];
				}
			}
		}

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = soln;
		res.timeTaken = watch.elapsedSeconds();
		return res;
	}

	/**
	 * @see DTMCModelChecker#computeSteadyStateProbsForBSCC(DTMC, BitSet, double[], BSCCPostProcessor)
	 */
	public ModelCheckerResult computeSteadyStateProbsForBSCC(DTMC dtmc, BitSet states, double result[]) throws PrismException
	{
		return computeSteadyStateProbsForBSCC(dtmc, states, result, null);
	}

	/**
	 * Compute steady-state probabilities for a BSCC
	 * i.e. compute the long-run probability of being in each state of the BSCC.
	 * No initial distribution is specified since it does not affect the result.
	 * The result will be stored in the relevant portion of a full vector,
	 * whose size equals the number of states in the DTMC.
	 * Optionally, pass in an existing vector to be used for this purpose;
	 * only the entries of this vector are changed that correspond to the BSCC states.
	 * <p>
	 * To ensure convergence, we use the iteration matrix<br/>
	 * {@code P = (Q * deltaT + I)} where<br/>
	 * {@code Q} is the generator matrix,
	 * {@code deltaT} a preconditioning factor and
	 * {@code I} is the the identity matrix.<br/>
	 * See <em>William J. Stewart: "Introduction to the Numerical Solution of Markov Chains"</em> p.124 for details.
	 * </p>
	 * @param dtmc The DTMC
	 * @param states The BSCC to be analysed
	 * @param bsccPostProcessor Post-processor for the values of each BSCC (optional: null means no post-processing)
	 * @param result Storage for result (ignored if null)
	 */
	public ModelCheckerResult computeSteadyStateProbsForBSCC(DTMC dtmc, BitSet states, double result[], BSCCPostProcessor bsccPostProcessor) throws PrismException
	{
		if (dtmc.getModelType() != ModelType.DTMC && dtmc.getModelType() != ModelType.CTMC) {
			throw new PrismNotSupportedException("Explicit engine currently does not support steady-state computation for " + dtmc.getModelType());
		}

		// Currently, only the power method is implemented
		if (!PrismSettings.PRISM_METHOD_STEADY_STATE.equals("Power")) {
			mainLog.printWarning("Switching to linear equation solution method \"" + LinEqMethod.POWER.fullName() + "\"\n");
		}

		IterableBitSet bscc = new IterableBitSet(states);

		// Start value iteration
		mainLog.println("Starting value iteration...");
		StopWatch watch = new StopWatch(mainLog).start("value iteration");

		// Store num states
		int numStates = dtmc.getNumStates();

		// Create solution vector(s)
		// Use the passed in vector, if present
		double[] soln = result == null ? new double[numStates] : result;
		double[] diagsQ = new double[numStates];
		double maxDiagsQ = 0.0;

		// Initialise the solution vector with an equiprobable distribution
		// over the BSCC states.
		// Additionally, compute the diagonal entries of the generator matrix Q.
		// Recall that the entries of the generator matrix are given by
		//     Q(s,t) = prob(s,t)   for s != t
		// and Q(s,s) = -sum_{s!=t} prob(s,t),
		// i.e., diagsQ[s] = -sum_{s!=t} prob(s,t).
		// Furthermore, compute max |diagsQ[s]|.
		double equiprob = 1.0 / states.cardinality();
		for (OfInt iter = bscc.iterator(); iter.hasNext();) {
			int state = iter.nextInt();

			// Equiprobable for BSCC states.
			soln[state] = equiprob;

			// Note: diagsQ[state] = 0.0, as it was freshly created
			// Compute negative exit rate (ignoring a possible self-loop)
			for (Iterator<Entry<Integer, Double>> transitions = dtmc.getTransitionsIterator(state); transitions.hasNext();) {
				Entry<Integer, Double> trans = transitions.next();
				int target = trans.getKey();
				double prob = trans.getValue();
				if (state != target) {
					diagsQ[state] -= prob;
				}
			}

			// Note: If there are no outgoing transitions, diagsQ[state] = 0, which is fine

			// Update maximal absolute diagonal entry value of Q
			// As diagsQ[s] <= 0, Math.abs(diagsQ[s]) = -diagsQ[s]
			maxDiagsQ = Math.max(maxDiagsQ, -diagsQ[state]);
		}

		// Compute preconditioning factor deltaT
		// In William J. Stewart: "Introduction to the Numerical Solution of Markov Chains",
		// deltaT = 0.99 / maxDiagsQ is proposed;
		// in the symbolic engines deltaT is computed as 0.99 / max exit[s], i.e., where
		// the denominator corresponds to the maximal exit rate (where self loops are included).
		// Currently, use the same deltaT values as in the symbolic engines,
		// so for DTMCs, as exit[s]=1 for all states, deltaT is 0.99:
		double deltaT = 0.99;
		// TODO: Test and switch to deltaT computed as below, should lead to faster convergence.
		// double deltaT = 0.99 / maxDiagsQ;

		// create copy of the solution vector
		double[] soln2 = soln.clone();

		// Start iterations
		int iters = 0;
		boolean done = false;
		while (!done && iters < maxIters) {
			iters++;
			// Do vector-matrix multiplication step in (deltaT*Q + I)
			dtmc.vmMultPowerSteadyState(soln, soln2, diagsQ, deltaT, bscc);
			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, bscc, termCritParam, termCrit == TermCrit.ABSOLUTE);
			// Swap vectors for next iter
			double[] tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished value iteration
		watch.stop();
		mainLog.println("Power method: " + iters + " iterations in " + watch.elapsedSeconds() + " seconds.");

		// normalise solution
		PrismUtils.normalise(soln, bscc);

		// Apply post processing
		if (bsccPostProcessor != null) {
			bsccPostProcessor.apply(soln, states);
		}

		if (result != null && result != soln) {
			// If result vector was passed in as method argument,
			// it can be the case that result does not point to the current soln vector (most recent values)
			// but to the soln2 vector.
			// In that case, we copy the relevant values from soln to result.
			for (OfInt iter = bscc.iterator(); iter.hasNext();) {
				int state = iter.nextInt();
				result[state] = soln[state];
			}
		}

		// NOTE: from here on, don't change the values of result/soln,
		// as the values may have already been copied to the result vector above
		//
		// store only one result vector, free temporary vectors
		result = soln;
		soln = soln2 = null;

		// Non-convergence is an error (usually)
		if (!done && errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.\n" +
			             "Consider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = result;
		res.numIters = iters;
		res.timeTaken = watch.elapsedSeconds();

		return res;
	}

	/**
	 * Compute transient probabilities
	 * i.e. compute the probability of being in each state at time step {@code k},
	 * assuming the initial distribution {@code initDist}. 
	 * For space efficiency, the initial distribution vector will be modified and values over-written,  
	 * so if you wanted it, take a copy. 
	 * @param dtmc The DTMC
	 * @param k Time step
	 * @param initDist Initial distribution (will be overwritten)
	 */
	public ModelCheckerResult computeTransientProbs(DTMC dtmc, int k, double initDist[]) throws PrismException
	{
		throw new PrismNotSupportedException("Not implemented yet");
	}

	/**
	 * Simple test program.
	 */
	public static void main(String args[])
	{
		DTMCModelChecker mc;
		DTMCSimple dtmc;
		ModelCheckerResult res;
		try {
			// Two examples of building and solving a DTMC

			int version = 2;
			if (version == 1) {

				// 1. Read in from .tra and .lab files
				//    Run as: PRISM_MAINCLASS=explicit.DTMCModelChecker bin/prism dtmc.tra dtmc.lab target_label
				mc = new DTMCModelChecker(null);
				dtmc = new DTMCSimple();
				dtmc.buildFromPrismExplicit(args[0]);
				//System.out.println(dtmc);
				Map<String, BitSet> labels = mc.loadLabelsFile(args[1]);
				//System.out.println(labels);
				BitSet target = labels.get(args[2]);
				if (target == null)
					throw new PrismException("Unknown label \"" + args[2] + "\"");
				for (int i = 3; i < args.length; i++) {
					if (args[i].equals("-nopre"))
						mc.setPrecomp(false);
				}
				res = mc.computeReachProbs(dtmc, target);
				System.out.println(res.soln[0]);

			} else {

				// 2. Build DTMC directly
				//    Run as: PRISM_MAINCLASS=explicit.DTMCModelChecker bin/prism
				//    (example taken from p.14 of Lec 5 of http://www.prismmodelchecker.org/lectures/pmc/) 
				mc = new DTMCModelChecker(null);
				dtmc = new DTMCSimple(6);
				dtmc.setProbability(0, 1, 0.1);
				dtmc.setProbability(0, 2, 0.9);
				dtmc.setProbability(1, 0, 0.4);
				dtmc.setProbability(1, 3, 0.6);
				dtmc.setProbability(2, 2, 0.1);
				dtmc.setProbability(2, 3, 0.1);
				dtmc.setProbability(2, 4, 0.5);
				dtmc.setProbability(2, 5, 0.3);
				dtmc.setProbability(3, 3, 1.0);
				dtmc.setProbability(4, 4, 1.0);
				dtmc.setProbability(5, 5, 0.3);
				dtmc.setProbability(5, 4, 0.7);
				System.out.println(dtmc);
				BitSet target = new BitSet();
				target.set(4);
				BitSet remain = new BitSet();
				remain.set(1);
				remain.flip(0, 6);
				System.out.println(target);
				System.out.println(remain);
				res = mc.computeUntilProbs(dtmc, remain, target);
				System.out.println(res.soln[0]);
			}
		} catch (PrismException e) {
			System.out.println(e);
		}
	}
}
