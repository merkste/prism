//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Carlos S. Bederian (Universidad Nacional de Cordoba)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package prism;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import acceptance.AcceptanceGenRabinDD;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceOmegaDD;
import acceptance.AcceptanceRabin;
import acceptance.AcceptanceRabinDD;
import acceptance.AcceptanceStreettDD;
import acceptance.AcceptanceType;
import automata.DA;
import automata.LTL2DA;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.type.TypeBool;
import parser.type.TypePathBool;

/**
 * LTL model checking functionality
 */
public class LTLModelChecker extends PrismComponent
{
	private boolean allowSimplificationsBasedOnModel = true;

	public class LTLProduct<M extends Model> extends Product<M> {
		private AcceptanceOmegaDD acceptance;

		public LTLProduct(M productModel, M originalModel, AcceptanceOmegaDD acceptance, JDDNode startMask, JDDVars daRowVars)
		{
			super(productModel, originalModel, startMask, daRowVars);
			this.acceptance = acceptance;
		}

		public AcceptanceOmegaDD getAcceptance() {
			return acceptance;
		}

		public void setAcceptance(AcceptanceOmegaDD acceptance) {
			this.acceptance = acceptance;
		}

		public void clear() {
			super.clear();
			acceptance.clear();
		}
	}

	/**
	 * Create a new DTMCModelChecker, inherit basic state from parent (unless null).
	 */
	public LTLModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	public void disallowSimplificationsBasedOnModel()
	{
		allowSimplificationsBasedOnModel = false;
	}

	/**
	 * Returns {@code true} if expression {@code expr} is a formula that can be handled by
	 * LTLModelChecker for the given ModelType.
	 */
	public static boolean isSupportedLTLFormula(ModelType modelType, Expression expr) throws PrismLangException
	{
		if (!expr.isPathFormula(true)) {
			return false;
		}
		if (Expression.containsTemporalTimeBounds(expr)) {
			if (modelType.continuousTime()) {
				// Only support temporal bounds for discrete time models
				return false;
			}
			
			if (!expr.isSimplePathFormula()) {
				// Only support temporal bounds for simple path formulas
				return false;
			}
		}

		if (Expression.isHOA(expr)) {
			return true;
		}

		return true;
	}

	/**
	 * Extract maximal state formula from an LTL path formula, model check them (with passed in model checker) and
	 * replace them with ExpressionLabel objects L0, L1, etc. Expression passed in is modified directly, but the result
	 * is also returned. As an optimisation, expressions that results in true/false for all states are converted to an
	 * actual true/false, and duplicate results (or their negations) reuse the same label. BDDs giving the states which
	 * satisfy each label are put into the vector labelDDs, which should be empty when this function is called.
	 */
	public Expression checkMaximalStateFormulas(ModelChecker mc, Model model, Expression expr, Vector<JDDNode> labelDDs) throws PrismException
	{
		// A state formula
		if (expr.getType() instanceof TypeBool) {
			// Model check
			JDDNode dd = mc.checkExpressionDD(expr, model.getReach().copy());
			if (allowSimplificationsBasedOnModel) {
				// Detect special cases (true, false) for optimisation
				if (dd.equals(JDD.ZERO)) {
					JDD.Deref(dd);
					return Expression.False();
				}
				if (dd.equals(model.getReach())) {
					JDD.Deref(dd);
					return Expression.True();
				}
				// See if we already have an identical result
				// (in which case, reuse it)
				int i = labelDDs.indexOf(dd);
				if (i != -1) {
					JDD.Deref(dd);
					return new ExpressionLabel("L" + i);
				}
				// Also, see if we already have the negation of this result
				// (in which case, reuse it)
				JDD.Ref(dd);
				JDD.Ref(model.getReach());
				JDDNode ddNeg = JDD.And(JDD.Not(dd), model.getReach());
				i = labelDDs.indexOf(ddNeg);
				JDD.Deref(ddNeg);
				if (i != -1) {
					JDD.Deref(dd);
					return Expression.Not(new ExpressionLabel("L" + i));
				}
			}
			// Otherwise, add result to list, return new label
			labelDDs.add(dd);
			return new ExpressionLabel("L" + (labelDDs.size() - 1));
		}
		// A path formula (recurse, modify, return)
		else if (expr.getType() instanceof TypePathBool) {
			if (expr instanceof ExpressionBinaryOp) {
				ExpressionBinaryOp exprBinOp = (ExpressionBinaryOp) expr;
				exprBinOp.setOperand1(checkMaximalStateFormulas(mc, model, exprBinOp.getOperand1(), labelDDs));
				exprBinOp.setOperand2(checkMaximalStateFormulas(mc, model, exprBinOp.getOperand2(), labelDDs));
			} else if (expr instanceof ExpressionUnaryOp) {
				ExpressionUnaryOp exprUnOp = (ExpressionUnaryOp) expr;
				exprUnOp.setOperand(checkMaximalStateFormulas(mc, model, exprUnOp.getOperand(), labelDDs));
			} else if (expr instanceof ExpressionTemporal) {
				ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
				if (exprTemp.getOperand1() != null) {
					exprTemp.setOperand1(checkMaximalStateFormulas(mc, model, exprTemp.getOperand1(), labelDDs));
				}
				if (exprTemp.getOperand2() != null) {
					exprTemp.setOperand2(checkMaximalStateFormulas(mc, model, exprTemp.getOperand2(), labelDDs));
				}
			}
		}
		return expr;
	}

	/**
	 * Construct a deterministic automaton (DA) for an LTL formula, having first extracted maximal state formulas
	 * and model checked them with the passed in model checker. The maximal state formulas are assigned labels
	 * (L0, L1, etc.) which become the atomic propositions in the resulting DA. JDDNodes giving the states which
	 * satisfy each label are put into the vector {@code labelBS}, which should be empty when this function is called.
	 *
	 * @param mc a StateModelChecker, used for checking maximal state formulas
	 * @param model the model
	 * @param expr a path expression, i.e. the LTL formula
	 * @param labelBS empty vector to be filled with JDDNodes for subformulas 
	 * @param allowedAcceptance the allowed acceptance types
	 * @return the DA
	 */
	public DA<BitSet,? extends AcceptanceOmega> constructDAForLTLFormula(StateModelChecker mc, Model model, Expression expr, Vector<JDDNode> labelDDs, AcceptanceType... allowedAcceptance) throws PrismException
	{
		if (Expression.containsTemporalRewardBounds(expr)) {
			throw new PrismNotSupportedException("Can not handle reward bounds via deterministic automata.");
		}

		if (Expression.containsTemporalTimeBounds(expr)) {
			if (model.getModelType().continuousTime()) {
				throw new PrismException("DA construction for time-bounded operators not supported for " + model.getModelType()+".");
			}

			if (!expr.isSimplePathFormula()) {
				throw new PrismException("Time-bounded operators not supported in LTL: " + expr);
			}
		}
		
		long time;
		DA<BitSet, ? extends AcceptanceOmega> da;

		if (Expression.isHOA(expr)) {
			LTL2DA ltl2da = new LTL2DA(this);
			time = System.currentTimeMillis();
			mainLog.println("Parsing and constructing HOA automaton for "+expr);
			PrismPaths paths = new PrismPaths(mc.getModulesFile(),
			                                  mc.getPropertiesFile());
			Vector<Expression> apExpressions = new Vector<Expression>();
			da = ltl2da.fromExpressionHOA(expr, paths, apExpressions, allowedAcceptance);

			mainLog.println("Determining states satisfying atomic proposition labels of the automaton...");
			for (int i=0; i<da.getAPList().size(); i++) {
				Expression apExpression = apExpressions.get(i);
				apExpression.typeCheck();
				JDDNode labelStates = mc.checkExpressionDD(apExpression, model.getReach().copy());
				labelDDs.add(labelStates);
				da.getAPList().set(i, "L"+i);
			}
		} else {
			// Model check maximal state formulas
			Expression ltl = checkMaximalStateFormulas(mc, model, expr.deepCopy(), labelDDs);

			// Convert LTL formula to deterministic automaton (DA)
			mainLog.println("\nBuilding deterministic automaton (for " + ltl + ")...");
			time  = System.currentTimeMillis();
			LTL2DA ltl2da = new LTL2DA(this);
			da = ltl2da.convertLTLFormulaToDA(ltl, mc.getConstantValues(), allowedAcceptance);
		}
		da.checkForCanonicalAPs(labelDDs.size());
		mainLog.println(da.getAutomataType()+" has " + da.size() + " states, " + da.getAcceptance().getSizeStatistics() + ".");
		time = System.currentTimeMillis() - time;
		mainLog.println("Time for deterministic automaton translation: " + time / 1000.0 + " seconds.");
		// If required, export DA
		if (getSettings().getExportPropAut()) {
			mainLog.println("Exporting DA to file \"" + getSettings().getExportPropAutFilename() + "\"...");
			PrintStream out = PrismUtils.newPrintStream(getSettings().getExportPropAutFilename());
			da.print(out, getSettings().getExportPropAutType());
			out.close();
		}

		return da;
	}

	/**
	 * Construct the product of a DA and a DTMC/CTMC.
	 * @param da The DA
	 * @param model The DTMC/CTMC
	 * @param labelDDs BDDs giving the set of states for each AP in the DRA
	 */
	public ProbModel constructProductMC(DA<BitSet, ? extends AcceptanceOmega> da, ProbModel model, Vector<JDDNode> labelDDs) throws PrismException
	{
		return constructProductMC(da, model, labelDDs, null, null, true);
	}

	/**
	 * Construct the product of a DA and a DTMC/CTMC.
	 * @param da The DA
	 * @param model The  DTMC/CTMC
	 * @param labelDDs BDDs giving the set of states for each AP in the DA
	 * @param daDDRowVarsCopy (Optionally) empty JDDVars object to obtain copy of DD row vars for DA
	 * @param daDDColVarsCopy (Optionally) empty JDDVars object to obtain copy of DD col vars for DA
	 */
	public ProbModel constructProductMC(DA<BitSet, ? extends AcceptanceOmega> da, ProbModel model, Vector<JDDNode> labelDDs, JDDVars daDDRowVarsCopy, JDDVars daDDColVarsCopy)
			throws PrismException
	{
		return constructProductMC(da, model, labelDDs, daDDRowVarsCopy, daDDColVarsCopy, true);
	}

	/**
	 * Construct the product of a DA and a DTMC/CTMC.
	 * @param da The DA
	 * @param model The  DTMC/CTMC
	 * @param labelDDs BDDs giving the set of states for each AP in the DA
	 * @param daDDRowVarsCopy (Optionally) empty JDDVars object to obtain copy of DD row vars for DA
	 * @param daDDColVarsCopy (Optionally) empty JDDVars object to obtain copy of DD col vars for DA
	 * @param allInit Do we assume that all states of the original model are initial states?
	 *        (just for the purposes of reachability)
	 */
	public ProbModel constructProductMC(DA<BitSet, ? extends AcceptanceOmega> da, ProbModel model, Vector<JDDNode> labelDDs, JDDVars daDDRowVarsCopy, JDDVars daDDColVarsCopy,
			boolean allInit) throws PrismException
	{
		// Existing model - dds, vars, etc.
		JDDVars varDDRowVars[];
		JDDVars varDDColVars[];
		JDDVars allDDRowVars;
		JDDVars allDDColVars;
		VarList varList;
		// New (product) model - dds, vars, etc.
		JDDNode newTrans, newStart;
		JDDVars newVarDDRowVars[], newVarDDColVars[];
		JDDVars newAllDDRowVars, newAllDDColVars;
		ModelVariablesDD newModelVariables;
		VarList newVarList;
		String daVar;
		// DA stuff
		JDDVars daDDRowVars, daDDColVars;
		// Misc
		int i, n;
		boolean before;

		// Get details of old model (no copy, does not need to be cleaned up)
		varDDRowVars = model.getVarDDRowVars();
		varDDColVars = model.getVarDDColVars();
		allDDRowVars = model.getAllDDRowVars();
		allDDColVars = model.getAllDDColVars();
		varList = model.getVarList();

		// Create a (new, unique) name for the variable that will represent DA states
		daVar = "_da";
		while (varList.getIndex(daVar) != -1) {
			daVar = "_" + daVar;
		}

		newModelVariables = model.getModelVariables().copy();
		
		// See how many new dd vars will be needed for DA
		// and whether there is room to put them before rather than after the existing vars
		// (if DA only has one state, we add an extra dummy state)
		n = (int) Math.ceil(PrismUtils.log2(da.size()));
		n = Math.max(n, 1);
		before = newModelVariables.canPrependExtraStateVariable(n);
		
		daDDRowVars = new JDDVars();
		daDDColVars = new JDDVars();
		// Create the new dd variables
		JDDVars daVars = newModelVariables.allocateExtraStateVariable(n, daVar, before);
		
		for (i = 0; i < n; i++) {
			daDDRowVars.addVar(daVars.getVar(2*i));
			daDDColVars.addVar(daVars.getVar(2*i+1));
		}
		
		// Create/populate new lists
		newVarDDRowVars = new JDDVars[varDDRowVars.length + 1];
		newVarDDColVars = new JDDVars[varDDRowVars.length + 1];
		newVarDDRowVars[before ? 0 : varDDRowVars.length] = daDDRowVars.copy();
		newVarDDColVars[before ? 0 : varDDColVars.length] = daDDColVars.copy();
		for (i = 0; i < varDDRowVars.length; i++) {
			newVarDDRowVars[before ? i + 1 : i] = varDDRowVars[i].copy();
			newVarDDColVars[before ? i + 1 : i] = varDDColVars[i].copy();
		}
		if (before) {
			newAllDDRowVars = daDDRowVars.copy();
			newAllDDColVars = daDDColVars.copy();
			newAllDDRowVars.copyVarsFrom(allDDRowVars);
			newAllDDColVars.copyVarsFrom(allDDColVars);
		} else {
			newAllDDRowVars = allDDRowVars.copy();
			newAllDDColVars = allDDColVars.copy();
			newAllDDRowVars.copyVarsFrom(daDDRowVars);
			newAllDDColVars.copyVarsFrom(daDDColVars);
		}
		newVarList = (VarList) varList.clone();
		// NB: if DA only has one state, we add an extra dummy state
		Declaration decl = new Declaration(daVar, new DeclarationInt(Expression.Int(0), Expression.Int(Math.max(da.size() - 1, 1))));
		newVarList.addVar(before ? 0 : varList.getNumVars(), decl, 1, model.getConstantValues());

		// Build transition matrix for product
		newTrans = buildTransMask(da, labelDDs, allDDRowVars, allDDColVars, daDDRowVars, daDDColVars);
		JDD.Ref(model.getTrans());
		newTrans = JDD.Apply(JDD.TIMES, model.getTrans(), newTrans);

		// Build set of initial states for product
		// Note, by default, we take product of *all* states of the original model, not just its initial states.
		// Initial states are only used for reachability in this instance.
		// We need to ensure that the product model includes states corresponding to all
		// states of the original model (because we compute probabilities for all of them)
		// but some of these may not be reachable from the initial state of the product model.
		// Optionally (if allInit is false), we don't do this - maybe because we only care about result for the initial state
		// Note that we reset the initial states after reachability, corresponding to just the initial states of the original model.  
		newStart = buildStartMask(da, labelDDs, daDDRowVars);
		JDD.Ref(allInit ? model.getReach() : model.getStart());
		newStart = JDD.And(allInit ? model.getReach() : model.getStart(), newStart);

		// Create a new model model object to store the product model
		ProbModel modelProd = new ProbModel(
		// New transition matrix/start state
				newTrans, newStart,
				// Don't pass in any rewards info
				new JDDNode[0], new JDDNode[0], new String[0],
				// New list of all row/col vars
				newAllDDRowVars, newAllDDColVars,
				// New model variables
				newModelVariables,
				// Module info (unchanged)
				model.getNumModules(),
				model.getModuleNames(),
				JDDVars.copyArray(model.getModuleDDRowVars()),
				JDDVars.copyArray(model.getModuleDDColVars()),
				// New var info
				model.getNumVars() + 1, newVarList, newVarDDRowVars, newVarDDColVars,
				// Constants (no change)
				model.getConstantValues());

		// Do reachability/etc. for the new model
		modelProd.doReachability();
		modelProd.filterReachableStates();
		modelProd.findDeadlocks(false);
		if (modelProd.getDeadlockStates().size() > 0) {
			// Assuming original model has no deadlocks, neither should product
			throw new PrismException("Model-"+da.getAutomataType()+" product has deadlock states");
		}

		/*
		// Reset initial state
		newStart = buildStartMask(da, labelDDs, daDDRowVars);
		JDD.Ref(model.getStart());
		newStart = JDD.And(model.getStart(), newStart);
		modelProd.setStart(newStart);
*/

		// if possible, return copies of the DA DD variables via the method parameters
		if (daDDRowVarsCopy != null)
			daDDRowVarsCopy.copyVarsFrom(daDDRowVars);
		if (daDDColVarsCopy != null)
			daDDColVarsCopy.copyVarsFrom(daDDColVars);

		daDDRowVars.derefAll();
		daDDColVars.derefAll();

		return modelProd;
	}

	public LTLProduct<ProbModel> constructProductMC(ProbModelChecker mc, ProbModel model, Expression expr, AcceptanceType... allowedAcceptance) throws PrismException
	{
		Vector<JDDNode> labelDDs = new Vector<JDDNode>();
		DA<BitSet, ? extends AcceptanceOmega> da;
		ProbModel modelProduct;
		JDDNode startMask;
		JDDVars daDDRowVars, daDDColVars;

		da = constructDAForLTLFormula(mc, model, expr, labelDDs, allowedAcceptance);

		// Build product of Markov chain and automaton
		// (note: might be a CTMC - StochModelChecker extends this class)
		mainLog.println("\nConstructing MC-"+da.getAutomataType()+" product...");
		daDDRowVars = new JDDVars();
		daDDColVars = new JDDVars();
		modelProduct = constructProductMC(da, model, labelDDs, daDDRowVars, daDDColVars);
		mainLog.println();
		modelProduct.printTransInfo(mainLog, false);

		AcceptanceOmegaDD acceptance = da.getAcceptance().toAcceptanceDD(daDDRowVars);

		startMask = buildStartMask(da, labelDDs, daDDRowVars);
		JDD.Ref(model.getReach());
		startMask = JDD.And(model.getReach(), startMask);

		daDDColVars.derefAll();
		for (int i = 0; i < labelDDs.size(); i++) {
			JDD.Deref(labelDDs.get(i));
		}

		return new LTLProduct<ProbModel>(modelProduct, model, acceptance, startMask, daDDRowVars);
	}

	/**
	 * Construct the product of a DA and an MDP.
	 * @param da The DA
	 * @param model The MDP
	 * @param labelDDs BDDs giving the set of states for each AP in the DA
	 */
	public NondetModel constructProductMDP(DA<BitSet, ? extends AcceptanceOmega> da, NondetModel model, Vector<JDDNode> labelDDs) throws PrismException
	{
		return constructProductMDP(da, model, labelDDs, null, null, true, null);
	}

	/**
	 * Construct the product of a DA and an MDP.
	 * @param da The DA
	 * @param model The MDP
	 * @param labelDDs BDDs giving the set of states for each AP in the DA
	 * @param daDDRowVarsCopy (Optionally) empty JDDVars object to obtain copy of DD row vars for DA
	 * @param daDDColVarsCopy (Optionally) empty JDDVars object to obtain copy of DD col vars for DA
	 */
	public NondetModel constructProductMDP(DA<BitSet, ? extends AcceptanceOmega> da, NondetModel model, Vector<JDDNode> labelDDs, JDDVars daDDRowVarsCopy, JDDVars daDDColVarsCopy)
			throws PrismException
	{
		return constructProductMDP(da, model, labelDDs, daDDRowVarsCopy, daDDColVarsCopy, true, null);
	}

	/**
	 * Construct the product of a DA and an MDP.
	 * @param da The DA
	 * @param model The MDP
	 * @param labelDDs BDDs giving the set of states for each AP in the DA
	 * @param daDDRowVarsCopy (Optionally) empty JDDVars object to obtain copy of DD row vars for DA
	 * @param daDDColVarsCopy (Optionally) empty JDDVars object to obtain copy of DD col vars for DA
	 * @param allInit Do we assume that all states of the original model are initial states?
	 *        (just for the purposes of reachability) If not, the required initial states should be given
	 * @param init The initial state(s) (of the original model) used to build the product;
	 *        if null; we just take the existing initial states from model.getStart().
	 */
	public NondetModel constructProductMDP(DA<BitSet, ? extends AcceptanceOmega> da, NondetModel model, Vector<JDDNode> labelDDs, JDDVars daDDRowVarsCopy, JDDVars daDDColVarsCopy,
			boolean allInit, JDDNode init) throws PrismException
	{
		// Existing model - dds, vars, etc.
		JDDVars varDDRowVars[];
		JDDVars varDDColVars[];
		JDDVars allDDRowVars;
		JDDVars allDDColVars;
		VarList varList;
		// New (product) model - dds, vars, etc.
		JDDNode newTrans, newStart;
		JDDVars newVarDDRowVars[], newVarDDColVars[];
		JDDVars newAllDDRowVars, newAllDDColVars;
		ModelVariablesDD newModelVariables;
		VarList newVarList;
		String daVar;
		// DA stuff
		JDDVars daDDRowVars, daDDColVars;
		// Misc
		int i, j, n;
		boolean before;

		// Get details of old model (no copy, does not need to be cleaned up)
		varDDRowVars = model.getVarDDRowVars();
		varDDColVars = model.getVarDDColVars();
		allDDRowVars = model.getAllDDRowVars();
		allDDColVars = model.getAllDDColVars();
		varList = model.getVarList();

		// Create a (new, unique) name for the variable that will represent DA states
		daVar = "_da";
		while (varList.getIndex(daVar) != -1) {
			daVar = "_" + daVar;
		}

		newModelVariables = model.getModelVariables().copy();
		
		// See how many new dd vars will be needed for DA
		// and whether there is room to put them before rather than after the existing vars
		// (if DA only has one state, we add an extra dummy state)
		n = (int) Math.ceil(PrismUtils.log2(da.size()));
		n = Math.max(n, 1);
		before = newModelVariables.canPrependExtraStateVariable(n);

		daDDRowVars = new JDDVars();
		daDDColVars = new JDDVars();
		// Create the new dd variables
		JDDVars daVars = newModelVariables.allocateExtraStateVariable(n, daVar, before);
		
		for (i = 0; i < n; i++) {
			daDDRowVars.addVar(daVars.getVar(2*i));
			daDDColVars.addVar(daVars.getVar(2*i+1));
		}
		
		// Create/populate new lists
		newVarDDRowVars = new JDDVars[varDDRowVars.length + 1];
		newVarDDColVars = new JDDVars[varDDRowVars.length + 1];
		newVarDDRowVars[before ? 0 : varDDRowVars.length] = daDDRowVars.copy();
		newVarDDColVars[before ? 0 : varDDColVars.length] = daDDColVars.copy();
		for (i = 0; i < varDDRowVars.length; i++) {
			newVarDDRowVars[before ? i + 1 : i] = varDDRowVars[i].copy();
			newVarDDColVars[before ? i + 1 : i] = varDDColVars[i].copy();
		}
		if (before) {
			newAllDDRowVars = daDDRowVars.copy();
			newAllDDColVars = daDDColVars.copy();
			newAllDDRowVars.copyVarsFrom(allDDRowVars);
			newAllDDColVars.copyVarsFrom(allDDColVars);
		} else {
			newAllDDRowVars = allDDRowVars.copy();
			newAllDDColVars = allDDColVars.copy();
			newAllDDRowVars.copyVarsFrom(daDDRowVars);
			newAllDDColVars.copyVarsFrom(daDDColVars);
		}
		newVarList = (VarList) varList.clone();
		// NB: if DA only has one state, we add an extra dummy state
		Declaration decl = new Declaration(daVar, new DeclarationInt(Expression.Int(0), Expression.Int(Math.max(da.size() - 1, 1))));
		newVarList.addVar(before ? 0 : varList.getNumVars(), decl, 1, model.getConstantValues());


		// Build transition matrix for product
		newTrans = buildTransMask(da, labelDDs, allDDRowVars, allDDColVars, daDDRowVars, daDDColVars);
		JDD.Ref(model.getTrans());
		newTrans = JDD.Apply(JDD.TIMES, model.getTrans(), newTrans);

		// Build set of initial states for product
		// Note, by default, we take product of *all* states of the original model, not just its initial states.
		// Initial states are only used for reachability in this instance.
		// We need to ensure that the product model includes states corresponding to all
		// states of the original model (because we compute probabilities for all of them)
		// but some of these may not be reachable from the initial state of the product model.
		// Optionally (if allInit is false), we don't do this - maybe because we only care about result for the initial state
		// Note that we reset the initial states after reachability, corresponding to just the initial states of the original model.
		// The initial state of the original model can be overridden by passing in 'init'.
		newStart = buildStartMask(da, labelDDs, daDDRowVars);
		JDD.Ref(allInit ? model.getReach() : init != null ? init : model.getStart());
		newStart = JDD.And(allInit ? model.getReach() : init != null ? init : model.getStart(), newStart);

		// Create a new model model object to store the product model
		NondetModel modelProd = new NondetModel(
		// New transition matrix/start state
				newTrans, newStart,
				// Don't pass in any rewards info
				new JDDNode[0], new JDDNode[0], new String[0],
				// New list of all row/col vars
				newAllDDRowVars, newAllDDColVars,
				// Nondet variables (unchanged)
				model.getAllDDSchedVars().copy(),
				model.getAllDDSynchVars().copy(),
				model.getAllDDChoiceVars().copy(),
				model.getAllDDNondetVars().copy(),
				// New model variables
				newModelVariables,
				// Module info (unchanged)
				model.getNumModules(),
				model.getModuleNames(),
				JDDVars.copyArray(model.getModuleDDRowVars()),
				JDDVars.copyArray(model.getModuleDDColVars()),
				// New var info
				model.getNumVars() + 1, newVarList, newVarDDRowVars, newVarDDColVars,
				// Constants (no change)
				model.getConstantValues());

		// Copy action info MTBDD across directly
		// If present, just needs filtering to reachable states,
		// which will get done below.
		if (model.getTransActions() != null) {
			JDD.Ref(model.getTransActions());
			modelProd.setTransActions(model.getTransActions());
		}
		// Also need to copy set of action label strings
		modelProd.setSynchs(new Vector<String>(model.getSynchs()));

		// Do reachability/etc. for the new model
		modelProd.doReachability();
		modelProd.filterReachableStates();
		modelProd.findDeadlocks(false);
		if (modelProd.getDeadlockStates().size() > 0) {
			// Assuming original model has no deadlocks, neither should product
			throw new PrismException("Model-DA product has deadlock states");
		}

		/*
		// Reset initial state
		newStart = buildStartMask(da, labelDDs, daDDRowVars);
		JDD.Ref(init != null ? init : model.getStart());
		newStart = JDD.And(init != null ? init : model.getStart(), newStart);
		modelProd.setStart(newStart);
		 */
		
		//try { prism.exportStatesToFile(modelProd, Prism.EXPORT_PLAIN, new java.io.File("prod.sta")); }
		//catch (java.io.FileNotFoundException e) {}

		// return copies of the DA DD variables via the method parameters if possible
		if (daDDRowVarsCopy != null)
			daDDRowVarsCopy.copyVarsFrom(daDDRowVars);
		if (daDDColVarsCopy != null)
			daDDColVarsCopy.copyVarsFrom(daDDColVars);

		daDDRowVars.derefAll();
		daDDColVars.derefAll();

		return modelProd;
	}

	/**
	 * Builds a (referenced) mask BDD representing all possible transitions in a product built with
	 * DA {@code da}, i.e. all the transitions ((s,q),(s',q')) where q' = delta(q, label(s')) in the DA.
	 * So the BDD is over column variables for model states (permuted from those found in the BDDs in
	 * {@code labelDDs}) and row/col variables for the DA (from {@code daDDRowVars}, {@code daDDColVars}).
	 */
	public JDDNode buildTransMask(DA<BitSet, ? extends AcceptanceOmega> da, Vector<JDDNode> labelDDs, JDDVars allDDRowVars, JDDVars allDDColVars, JDDVars daDDRowVars,
			JDDVars daDDColVars)
	{
		JDDNode daMask, label, exprBDD, transition;
		int i, j, k, numAPs, numStates, numEdges;

		numAPs = da.getAPList().size();
		daMask = JDD.Constant(0);
		// Iterate through all (states and) transitions of DA
		numStates = da.size();
		for (i = 0; i < numStates; i++) {
			numEdges = da.getNumEdges(i);
			for (j = 0; j < numEdges; j++) {
				// Build a transition label BDD for each edge
				label = JDD.Constant(1);
				for (k = 0; k < numAPs; k++) {
					// Get the expression BDD for AP k (via label "Lk")
					exprBDD = labelDDs.get(Integer.parseInt(da.getAPList().get(k).substring(1)));
					JDD.Ref(exprBDD);
					if (!da.getEdgeLabel(i, j).get(k)) {
						exprBDD = JDD.Not(exprBDD);
					}
					label = JDD.And(label, exprBDD);
				}
				// Switch label BDD to col vars
				label = JDD.PermuteVariables(label, allDDRowVars, allDDColVars);
				// Build a BDD for the edge
				transition = JDD.SetMatrixElement(JDD.Constant(0), daDDRowVars, daDDColVars, i, da.getEdgeDest(i, j), 1);
				// Now get the conjunction of the two
				transition = JDD.And(transition, label);
				// Add edge BDD to the DA transition mask
				daMask = JDD.Or(daMask, transition);
			}
		}

		return daMask;
	}

	/**
	 * Builds a (referenced) mask BDD representing all possible "start" states for a product built with
	 * DA {@code da}, i.e. all the states (s,q) where q = delta(q_init, label(s)) in the DA.
	 * So the BDD is over row variables for model states (as found in the BDDs in {@code labelDDs})
	 * and row variables for the DA (from {@code daDDRowVars}).
	 */
	public JDDNode buildStartMask(DA<BitSet,? extends AcceptanceOmega> da, Vector<JDDNode> labelDDs, JDDVars daDDRowVars)
	{
		JDDNode startMask, label, exprBDD, dest, tmp;
		int i, j, k, numAPs, numEdges;

		numAPs = da.getAPList().size();
		startMask = JDD.Constant(0);
		// Iterate through all transitions of start state of DA
		i = da.getStartState();
		numEdges = da.getNumEdges(i);
		for (j = 0; j < numEdges; j++) {
			// Build a transition label BDD for each edge
			label = JDD.Constant(1);
			for (k = 0; k < numAPs; k++) {
				// Get the expression BDD for AP k (via label "Lk")
				exprBDD = labelDDs.get(Integer.parseInt(da.getAPList().get(k).substring(1)));
				JDD.Ref(exprBDD);
				if (!da.getEdgeLabel(i, j).get(k)) {
					exprBDD = JDD.Not(exprBDD);
				}
				label = JDD.And(label, exprBDD);
			}
			// Build a BDD for the DA destination state
			dest = JDD.Constant(0);
			dest = JDD.SetVectorElement(dest, daDDRowVars, da.getEdgeDest(i, j), 1);

			// Now get the conjunction of the two
			tmp = JDD.And(dest, label);

			// Add this destination to our start mask
			startMask = JDD.Or(startMask, tmp);
		}

		return startMask;
	}

	/**
	 * Find the set of accepting BSCCs in a model wrt an omega acceptance condition.
	 * @param acceptance the acceptance condition, with BDD based storage
	 * @param model The model
	 * @return A referenced BDD for the union of all states in accepting BSCCs
	 */
	public JDDNode findAcceptingBSCCs(AcceptanceOmegaDD acceptance, ProbModel model) throws PrismException
	{
		JDDNode allAcceptingStates;
		List<JDDNode> vectBSCCs;

		allAcceptingStates = JDD.Constant(0);
		// Compute all BSCCs for model
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, model);
		sccComputer.computeBSCCs();
		vectBSCCs = sccComputer.getBSCCs();
		JDD.Deref(sccComputer.getNotInBSCCs());

		// Go through the BSCC
		for (JDDNode bscc : vectBSCCs) {
			// Check for acceptance
			if (acceptance.isBSCCAccepting(bscc)) {
				// This BSCC is accepting: add to allAcceptingStates
				JDD.Ref(bscc);
				allAcceptingStates = JDD.Or(allAcceptingStates, bscc);
			}
			JDD.Deref(bscc);
		}

		return allAcceptingStates;
	}

	/**
	 * Find the set of states in accepting end components (ECs) in a nondeterministic model wrt an acceptance condition.
	 * @param acceptance the acceptance condition
	 * @param model The model
	 * @param daDDRowVars BDD row variables for the DA part of the model
	 * @param daDDColVars BDD column variables for the DA part of the model
	 * @param fairness Consider fairness?
	 * @return A referenced BDD for the union of all states in accepting MECs
	 */
	public JDDNode findAcceptingECStates(AcceptanceOmegaDD acceptance, NondetModel model, JDDVars daDDRowVars, JDDVars daDDColVars, boolean fairness)
			throws PrismException
	{
		switch (acceptance.getType()) {
		case RABIN:
			return findAcceptingECStatesForRabin((AcceptanceRabinDD) acceptance, model, null, null, fairness);
		case GENERALIZED_RABIN:
			return findAcceptingECStatesForGeneralizedRabin((AcceptanceGenRabinDD) acceptance, model, daDDRowVars, daDDColVars, fairness);
		case STREETT:
			return findAcceptingECStatesForStreett((AcceptanceStreettDD) acceptance, model, fairness);
		
		default:
			throw new PrismNotSupportedException("Computing the accepting EC states for "+acceptance.getTypeName()+" acceptance is not yet implemented (symbolic engine)");
		}
	}

	/**
	 * Find the set of states in accepting end components (ECs) in a nondeterministic model wrt a Rabin acceptance condition.
	 * @param acceptance the Rabin acceptance condition
	 * @param model The model
	 * @param draDDRowVars BDD row variables for the DRA part of the model
	 * @param draDDColVars BDD column variables for the DRA part of the model
	 * @param fairness Consider fairness?
	 * @return A referenced BDD for the union of all states in accepting MECs
	 */
	public JDDNode findAcceptingECStatesForRabin(AcceptanceRabinDD acceptance, NondetModel model, JDDVars draDDRowVars, JDDVars draDDColVars, boolean fairness)
			throws PrismException
	{
		JDDNode acceptingStates = null, allAcceptingStates, acceptanceVector_L_not, acceptanceVector_K, candidateStates;
		int i;

		allAcceptingStates = JDD.Constant(0);

		if (acceptance.size() > 1) {
			acceptanceVector_L_not = JDD.Constant(0);
			acceptanceVector_K = JDD.Constant(0);
			ArrayList<JDDNode> statesLnot = new ArrayList<JDDNode>();
			ArrayList<JDDNode> statesK = new ArrayList<JDDNode>();

			for (i = 0; i < acceptance.size(); i++) {
				JDDNode tmpLnot = JDD.Not(acceptance.get(i).getL());
				JDDNode tmpK = acceptance.get(i).getK();
				statesLnot.add(tmpLnot);
				JDD.Ref(tmpLnot);
				acceptanceVector_L_not = JDD.Or(acceptanceVector_L_not, tmpLnot);
				statesK.add(tmpK);
				JDD.Ref(tmpK);
				acceptanceVector_K = JDD.Or(acceptanceVector_K, tmpK);
			}

			if (draDDRowVars != null && draDDColVars != null) {
				JDD.Ref(model.getTrans01());
				JDD.Ref(acceptanceVector_L_not);
				candidateStates = JDD.Apply(JDD.TIMES, model.getTrans01(), acceptanceVector_L_not);
				acceptanceVector_L_not = JDD.PermuteVariables(acceptanceVector_L_not, draDDRowVars, draDDColVars);
				candidateStates = JDD.Apply(JDD.TIMES, candidateStates, acceptanceVector_L_not);
				candidateStates = JDD.ThereExists(candidateStates, model.getAllDDColVars());
				candidateStates = JDD.ThereExists(candidateStates, model.getAllDDNondetVars());
			} else {
				JDD.Ref(model.getReach());
				candidateStates = JDD.And(model.getReach(), acceptanceVector_L_not);
			}
			// find all maximal end components
			List<JDDNode> allecs = findMECStates(model, candidateStates, acceptanceVector_K);
			JDD.Deref(acceptanceVector_K);
			JDD.Deref(candidateStates);

			for (i = 0; i < acceptance.size(); i++) {
				// build the acceptance vectors L_i and K_i
				acceptanceVector_L_not = statesLnot.get(i);
				acceptanceVector_K = statesK.get(i);
				for (JDDNode ec : allecs) {
					// build bdd of accepting states (under L_i) in the product model
					List<JDDNode> ecs;
					JDD.Ref(ec);
					JDD.Ref(acceptanceVector_L_not);
					candidateStates = JDD.And(ec, acceptanceVector_L_not);
					if (candidateStates.equals(ec)) {
						//mainLog.println(" ------------- ec is not modified ------------- ");
						ecs = new Vector<JDDNode>();
						// store copy of ec in ecs
						JDD.Ref(ec);
						ecs.add(ec);
					} else if (candidateStates.equals(JDD.ZERO)) {
						//mainLog.println(" ------------- ec is ZERO ------------- ");
						JDD.Deref(candidateStates);
						continue;
					} else { // recompute maximal end components
						//mainLog.println(" ------------- ec is recomputed ------------- ");
						if (draDDRowVars != null && draDDColVars != null) {
							JDD.Ref(model.getTrans01());
							JDD.Ref(candidateStates);
							JDDNode newcandidateStates = JDD.Apply(JDD.TIMES, model.getTrans01(), candidateStates);
							candidateStates = JDD.PermuteVariables(candidateStates, draDDRowVars, draDDColVars);
							newcandidateStates = JDD.Apply(JDD.TIMES, candidateStates, newcandidateStates);
							newcandidateStates = JDD.ThereExists(newcandidateStates, model.getAllDDColVars());
							candidateStates = JDD.ThereExists(newcandidateStates, model.getAllDDNondetVars());
						}
						ecs = findMECStates(model, candidateStates, acceptanceVector_K);
					}
					JDD.Deref(candidateStates);

					// find ECs in acceptingStates that are accepting under K_i
					acceptingStates = JDD.Constant(0);
					for (JDDNode set : ecs) {
						if (JDD.AreIntersecting(set, acceptanceVector_K))
							acceptingStates = JDD.Or(acceptingStates, set);
						else
							JDD.Deref(set);
					}
					allAcceptingStates = JDD.Or(allAcceptingStates, acceptingStates);
				}
				JDD.Deref(acceptanceVector_K);
				JDD.Deref(acceptanceVector_L_not);
			}
			for (JDDNode ec : allecs)
				JDD.Deref(ec);
		} else {
			// Go through the DRA acceptance pairs (L_i, K_i) 
			for (i = 0; i < acceptance.size(); i++) {
				// Build BDDs for !L_i and K_i
				JDDNode statesLi_not = JDD.Not(acceptance.get(i).getL());
				JDDNode statesK_i = acceptance.get(i).getK();
				// Find states in the model for which there are no transitions leaving !L_i
				// (this will allow us to reduce the problem to finding MECs, not ECs)
				if (draDDRowVars != null && draDDColVars != null) {
					// TODO: I don't think this next step is needed,
					// since the ECComputer restricts the model in this way anyway
					JDD.Ref(model.getTrans01());
					JDD.Ref(statesLi_not);
					candidateStates = JDD.Apply(JDD.TIMES, model.getTrans01(), statesLi_not);
					statesLi_not = JDD.PermuteVariables(statesLi_not, draDDRowVars, draDDColVars);
					candidateStates = JDD.Apply(JDD.TIMES, candidateStates, statesLi_not);
					candidateStates = JDD.ThereExists(candidateStates, model.getAllDDColVars());
					candidateStates = JDD.ThereExists(candidateStates, model.getAllDDNondetVars());
				} else {
					JDD.Ref(model.getReach());
					candidateStates = JDD.And(model.getReach(), statesLi_not);
				}
				// Normal case (no fairness): find accepting MECs within !L_i
				if (!fairness) {
					List<JDDNode> ecs = findMECStates(model, candidateStates);
					JDD.Deref(candidateStates);
					acceptingStates = filteredUnion(ecs, statesK_i);
				}
				// For case of fairness...
				else {
					// Compute the backward set of S x !L_i
					JDD.Ref(candidateStates);
					JDDNode tmp = JDD.And(candidateStates, statesK_i);
					JDD.Ref(model.getTrans01());
					JDDNode edges = JDD.ThereExists(model.getTrans01(), model.getAllDDNondetVars());
					JDDNode filterStates = backwardSet(model, tmp, edges);
					// Filter out states that can't reach a state in !L'_i
					candidateStates = JDD.And(candidateStates, filterStates);
					// Find accepting states in S x !L_i
					acceptingStates = findFairECs(model, candidateStates);
				}

				// Add states to our destination BDD
				allAcceptingStates = JDD.Or(allAcceptingStates, acceptingStates);
			}
		}

		return allAcceptingStates;
	}

	/**
	 * Find the set of states in accepting end components (ECs) in a nondeterministic model wrt a Generalized Rabin acceptance condition.
	 * @param acceptance the Generalized Rabin acceptance condition
	 * @param model The model
	 * @param draDDRowVars BDD row variables for the DRA part of the model
	 * @param draDDColVars BDD column variables for the DRA part of the model
	 * @param fairness Consider fairness?
	 * @return A referenced BDD for the union of all states in accepting MECs
	 */
	public JDDNode findAcceptingECStatesForGeneralizedRabin(AcceptanceGenRabinDD acceptance, NondetModel model, JDDVars draDDRowVars, JDDVars draDDColVars, boolean fairness)
			throws PrismException
	{
		
		if (fairness) {
			throw new PrismNotSupportedException("Accepting end-component computation for generalized Rabin is currently not supported with fairness");
		}

		JDDNode allAcceptingStates;

		allAcceptingStates = JDD.Constant(0);

		// Go through the GR acceptance pairs (L_i, K_i_1, ..., K_i_n) 
		for (int i = 0; i < acceptance.size(); i++) {
					
			// Filter out L_i states from the model and find the MECs
			JDDNode notL = JDD.Not(acceptance.get(i).getL());
			JDD.Ref(model.getTrans01());
			JDD.Ref(notL);
			JDDNode candidateStates = JDD.Apply(JDD.TIMES, model.getTrans01(), notL);
			notL = JDD.PermuteVariables(notL, draDDRowVars, draDDColVars);
			candidateStates = JDD.Apply(JDD.TIMES, candidateStates,	notL);
			candidateStates = JDD.ThereExists(candidateStates, model.getAllDDColVars());
			candidateStates = JDD.ThereExists(candidateStates, model.getAllDDNondetVars());
			List<JDDNode> mecs = findMECStates(model, candidateStates);
			JDD.Deref(candidateStates);

			// Check which MECs are accepting for this pair, calculate union
			JDDNode acceptingStates = JDD.Constant(0);
			for (int k = 0; k < mecs.size(); k++) {
				// Is the induced BSCC by this MEC accepting?
				// (note we only really need to check K_i_1, ..., K_i_n here, not L too,
				// but this should not really affect efficiency)
				if (acceptance.get(i).isBSCCAccepting(mecs.get(k))) {
					acceptingStates = JDD.Or(acceptingStates, mecs.get(k));
				} else {
					JDD.Deref(mecs.get(k));
				}
			}
			// Add to the set of accepting states for all pairs
			allAcceptingStates = JDD.Or(allAcceptingStates, acceptingStates);
		}

		return allAcceptingStates;
	}

	public JDDNode findAcceptingECStatesForStreett(AcceptanceStreettDD acceptance, NondetModel model, boolean fairness)
			throws PrismException
	{
		class ECandPairs {
			JDDNode ddMEC;
			BitSet activePairs;

			ECandPairs(JDDNode ddMEC, BitSet activePairs)
			{
				this.ddMEC = ddMEC;
				this.activePairs = activePairs;
			}

			void clear()
			{
				JDD.Deref(ddMEC);
			}
		}

		if (fairness) {
			throw new PrismException("Currently, can not compute fair MECs for Streett (symbolic engine)");
		}
		
		JDDNode allAcceptingStates = JDD.Constant(0);
		BitSet allPairs = new BitSet();
		allPairs.set(0, acceptance.size());

		int steps = 0;
		int accepting = 0;
		
		Stack<ECandPairs> todo = new Stack<ECandPairs>();
		List<JDDNode> mecs = findMECStates(model, JDD.ONE);
		for (JDDNode mec : mecs) {
			ECandPairs ecp = new ECandPairs(mec, allPairs);
			todo.push(ecp);
		}

		getLog().println("Found "+todo.size()+" global MECs");

		while (!todo.empty()) {
			ECandPairs ecp = todo.pop();

			steps++;

			BitSet newActivePairs = (BitSet)ecp.activePairs.clone();
			JDDNode restrict = null;

			// check for acceptance
			boolean allAccepting = true;
			for (int pair = ecp.activePairs.nextSetBit(0);
				 pair != -1;
				 pair = ecp.activePairs.nextSetBit(pair + 1)) {

				if (!acceptance.get(pair).isBSCCAccepting(ecp.ddMEC)) {
					// this pair is not accepting
					if (restrict == null) {
						restrict = ecp.ddMEC.copy();
					}
					restrict = JDD.And(restrict, JDD.Not(acceptance.get(pair).getR()));
					newActivePairs.clear(pair);
					allAccepting = false;
				}
			}

			if (allAccepting) {
				allAcceptingStates = JDD.Or(allAcceptingStates, ecp.ddMEC.copy());
				accepting++;
			} else if (restrict.equals(JDD.ZERO)) {
				// nothing to do
			} else {
				mecs = findMECStates(model, restrict);
				for (JDDNode mec : mecs) {
					ECandPairs newEcp = new ECandPairs(mec, newActivePairs);
					todo.push(newEcp);
				}
			}
			// cleanup
			ecp.clear();
			if (restrict != null) JDD.Deref(restrict);
		}

		getLog().println("Tested "+steps+" potential (M)ECs, found "+accepting+" to be accepting.");
		
		return allAcceptingStates;
	}

	public JDDNode findMultiAcceptingStates(DA<BitSet,AcceptanceRabin> dra, NondetModel model, JDDVars draDDRowVars, JDDVars draDDColVars, boolean fairness,
			List<JDDNode> allecs, List<JDDNode> statesH, List<JDDNode> statesL) throws PrismException
	{
		JDDNode acceptingStates = null, allAcceptingStates, candidateStates;
		JDDNode acceptanceVector_H, acceptanceVector_L;
		int i;

		allAcceptingStates = JDD.Constant(0);

		// for each acceptance pair (H_i, L_i) in the DRA, build H'_i = S x H_i
		// and compute the maximal ECs in H'_i
		for (i = 0; i < dra.getAcceptance().size(); i++) {
			// build the acceptance vectors H_i and L_i
			acceptanceVector_H = statesH.get(i);
			acceptanceVector_L = statesL.get(i);
			for (JDDNode ec : allecs) {
				// build bdd of accepting states (under H_i) in the product model
				List<JDDNode> ecs = null;
				JDD.Ref(ec);
				JDD.Ref(acceptanceVector_H);
				candidateStates = JDD.And(ec, acceptanceVector_H);
				if (candidateStates.equals(ec)) {
					//mainLog.println(" ------------- ec is not modified ------------- ");
					ecs = new Vector<JDDNode>();
					//JDDNode ec1 = ec;
					//JDD.Ref(ec);
					ecs.add(ec);
					//JDD.Deref(candidateStates);
				} else if (candidateStates.equals(JDD.ZERO)) {
					//mainLog.println(" ------------- ec is ZERO ------------- ");
					JDD.Deref(candidateStates);
					continue;
				} else { // recompute maximal end components
					//mainLog.println(" ------------- ec is recomputed ------------- ");
					JDD.Ref(model.getTrans01());
					JDD.Ref(candidateStates);
					JDDNode newcandidateStates = JDD.Apply(JDD.TIMES, model.getTrans01(), candidateStates);
					candidateStates = JDD.PermuteVariables(candidateStates, draDDRowVars, draDDColVars);
					newcandidateStates = JDD.Apply(JDD.TIMES, candidateStates, newcandidateStates);
					newcandidateStates = JDD.ThereExists(newcandidateStates, model.getAllDDColVars());
					candidateStates = JDD.ThereExists(newcandidateStates, model.getAllDDNondetVars());
					ecs = findMECStates(model, candidateStates, acceptanceVector_L);
					JDD.Deref(candidateStates);
				}

				//StateListMTBDD vl;
				//int count = 0;
				acceptingStates = JDD.Constant(0);
				for (JDDNode set : ecs) {
					if (JDD.AreIntersecting(set, acceptanceVector_L))
						acceptingStates = JDD.Or(acceptingStates, set);
					else
						JDD.Deref(set);
				}
				// Add states to our destination BDD
				allAcceptingStates = JDD.Or(allAcceptingStates, acceptingStates);
			}
			JDD.Deref(acceptanceVector_L);
			JDD.Deref(acceptanceVector_H);
		}

		return allAcceptingStates;
	}

	public void findMultiConflictAcceptingStates(DA<BitSet,AcceptanceRabin>[] dra, NondetModel model, JDDVars[] draDDRowVars, JDDVars[] draDDColVars, List<JDDNode> targetDDs,
			List<List<JDDNode>> allstatesH, List<List<JDDNode>> allstatesL, List<JDDNode> combinations, List<List<Integer>> combinationIDs)
			throws PrismException
	{
		List<queueElement> queue = new ArrayList<queueElement>();
		int sp = 0;

		for (int i = 0; i < dra.length; i++) {
			List<Integer> ids = new ArrayList<Integer>();
			ids.add(i);
			queueElement e = new queueElement(allstatesH.get(i), allstatesL.get(i), targetDDs.get(i), ids, i + 1);
			queue.add(e);
		}

		while (sp < queue.size()) {
			computeCombinations(dra, model, draDDRowVars, draDDColVars, targetDDs, allstatesH, allstatesL, queue, sp);
			sp++;
		}

		// subtract children from targetDD
		for (queueElement e : queue)
			if (e.children != null) {
				JDDNode newtarget = e.targetDD;
				//JDD.Ref(newtarget);
				for (queueElement e1 : e.children) {
					JDD.Ref(e1.targetDD);
					newtarget = JDD.And(newtarget, JDD.Not(e1.targetDD));
				}
				//JDD.Deref(e.targetDD);
				e.targetDD = newtarget;
			}
		targetDDs.clear();
		for (int i = 0; i < dra.length; i++) {
			targetDDs.add(queue.get(i).targetDD);
		}
		for (int i = dra.length; i < queue.size(); i++) {
			combinations.add(queue.get(i).targetDD);
			combinationIDs.add(queue.get(i).draIDs);
		}
	}

	private void computeCombinations(DA<BitSet,AcceptanceRabin>[] dra, NondetModel model, JDDVars[] draDDRowVars, JDDVars[] draDDColVars, List<JDDNode> targetDDs,
			List<List<JDDNode>> allstatesH, List<List<JDDNode>> allstatesL, List<queueElement> queue, int sp) throws PrismException
	{
		queueElement e = queue.get(sp);
		int bound = queue.size();
		//StateListMTBDD vl = null;
		//mainLog.println("  ------------- Processing " + e.draIDs + ": -------------");

		for (int i = e.next; i < dra.length; i++) {
			List<JDDNode> newstatesH = new ArrayList<JDDNode>();
			List<JDDNode> newstatesL = new ArrayList<JDDNode>();
			//if(e.draIDs.size() >= 2 || sp > 0 /*|| queue.size() > 3*/)
			//	break;
			//mainLog.println("             combinations " + e.draIDs + ", " + i + ": ");
			JDDNode allAcceptingStates = JDD.Constant(0);
			// compute conjunction of e and next
			List<JDDNode> nextstatesH = allstatesH.get(i);
			List<JDDNode> nextstatesL = allstatesL.get(i);
			JDD.Ref(e.targetDD);
			JDD.Ref(targetDDs.get(i));
			JDDNode intersection = JDD.And(e.targetDD, targetDDs.get(i));
			for (int j = 0; j < e.statesH.size(); j++) {
				JDD.Ref(intersection);
				JDD.Ref(e.statesH.get(j));
				JDDNode candidateStates = JDD.And(intersection, e.statesH.get(j));
				for (int k = 0; k < nextstatesH.size(); k++) {
					JDD.Ref(candidateStates);
					JDD.Ref(nextstatesH.get(k));
					JDDNode candidateStates1 = JDD.And(candidateStates, nextstatesH.get(k));

					// Find end components in candidateStates1
					JDD.Ref(model.getTrans01());
					JDD.Ref(candidateStates1);
					JDDNode newcandidateStates = JDD.Apply(JDD.TIMES, model.getTrans01(), candidateStates1);
					/*for(int x=0; x<e.draIDs.size(); x++)
						candidateStates1 = JDD.PermuteVariables(candidateStates1, draDDRowVars[e.draIDs.get(x)], draDDColVars[e.draIDs.get(x)]);
					candidateStates1 = JDD.PermuteVariables(candidateStates1, draDDRowVars[i], draDDColVars[i]);*/
					candidateStates1 = JDD.PermuteVariables(candidateStates1, model.getAllDDRowVars(), model.getAllDDColVars());
					newcandidateStates = JDD.Apply(JDD.TIMES, candidateStates1, newcandidateStates);
					newcandidateStates = JDD.ThereExists(newcandidateStates, model.getAllDDColVars());
					candidateStates1 = JDD.ThereExists(newcandidateStates, model.getAllDDNondetVars());
					JDD.Ref(e.statesL.get(j));
					JDD.Ref(nextstatesL.get(k));
					JDDNode acceptanceVector_L = JDD.And(e.statesL.get(j), nextstatesL.get(k));
					List<JDDNode> ecs = null;
					ecs = findMECStates(model, candidateStates1, acceptanceVector_L);
					JDD.Deref(candidateStates1);

					// For each ec, test if it has non-empty intersection with L states
					if (ecs != null) {
						boolean valid = false;
						for (JDDNode set : ecs) {
							if (JDD.AreIntersecting(set, acceptanceVector_L)) {
								allAcceptingStates = JDD.Or(allAcceptingStates, set);
								valid = true;
							} else
								JDD.Deref(set);
						}
						if (valid) {
							//mainLog.println("          adding j = " + j + ", k = " + k + " to nextstateH & L ");
							JDD.Ref(e.statesH.get(j));
							JDD.Ref(nextstatesH.get(k));
							JDDNode ttt = JDD.And(e.statesH.get(j), nextstatesH.get(k));
							newstatesH.add(ttt);
							JDD.Ref(acceptanceVector_L);
							newstatesL.add(acceptanceVector_L);
						}
					}
					//if(!valid)
					JDD.Deref(acceptanceVector_L);
				}
				JDD.Deref(candidateStates);
			}
			JDD.Deref(intersection);

			if (!newstatesH.isEmpty() /*&& i+1 < dra.length*/) {
				// generate a new element and put it into queue
				List<Integer> ids = new ArrayList<Integer>(e.draIDs);
				ids.add(i);
				queueElement e1 = new queueElement(newstatesH, newstatesL, allAcceptingStates, ids, i + 1);
				queue.add(e1);
				// add link to e
				e.addChildren(e1);
			} else
				JDD.Deref(allAcceptingStates);

			/*String s = "";
			for(int j=0; j<e.draIDs.size(); j++) 
				s += e.draIDs.*/
			/*vl = new StateListMTBDD(allAcceptingStates, model);
			vl.print(mainLog);
			mainLog.flush();*/
		}

		// add children generated by other elements to e
		for (int i = bound - 1; i > sp; i--) {
			queueElement e2 = queue.get(i);
			if (e2.draIDs.size() <= e.draIDs.size())
				break;
			if (e2.draIDs.containsAll(e.draIDs))
				e.addChildren(e2);
		}

		if (e.draIDs.size() > 1) {
			//mainLog.println("          releaseing statesH & L ");
			for (int i = 0; i < e.statesH.size(); i++) {
				JDD.Deref(e.statesH.get(i));
				JDD.Deref(e.statesL.get(i));
			}
		}
	}

	/**
	 * Returns all end components in candidateStates under fairness assumptions
	 * 
	 * @param candidateStates Set of candidate states S x H_i (dereferenced after calling this function)
	 * @return S referenced BDD with the maximal stable set in c
	 */
	private JDDNode findFairECs(NondetModel model, JDDNode candidateStates)
	{

		JDDNode old = JDD.Constant(0);
		JDDNode current = candidateStates;

		while (!current.equals(old)) {
			JDD.Deref(old);
			JDD.Ref(current);
			old = current;

			JDD.Ref(current);
			JDD.Ref(model.getTrans01());
			// Select transitions starting in current
			JDDNode currTrans01 = JDD.And(model.getTrans01(), current);
			JDD.Ref(current);
			// mask of transitions that end outside current
			JDDNode mask = JDD.Not(JDD.PermuteVariables(current, model.getAllDDRowVars(), model.getAllDDColVars()));
			mask = JDD.And(currTrans01, mask);
			// mask of states that have bad transitions
			mask = JDD.ThereExists(mask, model.getAllDDColVars());
			mask = JDD.ThereExists(mask, model.getAllDDNondetVars());
			// Filter states with bad transitions
			current = JDD.And(current, JDD.Not(mask));
		}
		JDD.Deref(old);
		return current;
	}

	/**
	 * Return the set of states that reach nodes through edges
	 * Refs: result
	 * Derefs: nodes, edges
	 */
	private JDDNode backwardSet(NondetModel model, JDDNode nodes, JDDNode edges)
	{
		JDDNode old = JDD.Constant(0);
		JDDNode current = nodes;
		while (!current.equals(old)) {
			JDD.Deref(old);
			JDD.Ref(current);
			old = current;
			JDD.Ref(current);
			JDD.Ref(edges);
			current = JDD.Or(current, preimage(model, current, edges));
		}
		JDD.Deref(edges);
		JDD.Deref(old);
		return current;
	}

	/**
	 * Return the preimage of nodes in edges Refs: result Derefs: edges, nodes
	 */
	// FIXME: Refactor this out (duplicated in SCCComputers)
	private JDDNode preimage(NondetModel model, JDDNode nodes, JDDNode edges)
	{
		JDDNode tmp;

		// Get transitions that end at nodes
		tmp = JDD.PermuteVariables(nodes, model.getAllDDRowVars(), model.getAllDDColVars());
		tmp = JDD.And(edges, tmp);
		// Get pre(nodes)
		tmp = JDD.ThereExists(tmp, model.getAllDDColVars());
		return tmp;
	}

	/**
	 * Find (states of) all maximal end components (MECs) contained within {@code states}.
	 * @param states BDD of the set of containing states
	 * @return a vector of (referenced) BDDs representing the ECs
	 */
	public List<JDDNode> findMECStates(NondetModel model, JDDNode states) throws PrismException
	{
		ECComputer ecComputer = ECComputer.createECComputer(this, model);
		ecComputer.computeMECStates(states, null);
		return ecComputer.getMECStates();
	}

	/**
	 * Find (states of) all accepting maximal end components (MECs) contained within {@code states},
	 * where acceptance is defined as those which intersect with {@code filter}.
	 * (If {@code filter} is null, the acceptance condition is trivially satisfied.)
	 * @param states BDD of the set of containing states
	 * @param filter BDD for the set of accepting states
	 * @return a vector of (referenced) BDDs representing the ECs
	 */
	public List<JDDNode> findMECStates(NondetModel model, JDDNode states, JDDNode filter) throws PrismException
	{
		ECComputer ecComputer = ECComputer.createECComputer(this, model);
		ecComputer.computeMECStates(states, filter);
		return ecComputer.getMECStates();
	}

	/**
	 * Find all maximal end components (ECs) contained within {@code states}
	 * and whose states have no outgoing transitions.
	 * @param states BDD of the set of containing states
	 * @return a vector of (referenced) BDDs representing the ECs
	 */
	public List<JDDNode> findBottomEndComponents(NondetModel model, JDDNode states) throws PrismException
	{
		List<JDDNode> ecs = findMECStates(model, states);
		List<JDDNode> becs = new Vector<JDDNode>();
		JDDNode out;

		for (JDDNode scc : ecs) {
			JDD.Ref(model.getTrans01());
			JDD.Ref(scc);
			out = JDD.And(model.getTrans01(), scc);
			JDD.Ref(scc);
			out = JDD.And(out, JDD.Not(JDD.PermuteVariables(scc, model.getAllDDRowVars(), model.getAllDDColVars())));
			if (out.equals(JDD.ZERO)) {
				becs.add(scc);
			} else {
				JDD.Deref(scc);
			}
			JDD.Deref(out);
		}
		return becs;
	}

	public JDDNode maxStableSetTrans1(NondetModel model, JDDNode b)
	{

		JDD.Ref(b);
		JDD.Ref(model.getTrans());
		// Select transitions starting in b
		JDDNode currTrans = JDD.Apply(JDD.TIMES, model.getTrans(), b);
		JDDNode mask = JDD.PermuteVariables(b, model.getAllDDRowVars(), model.getAllDDColVars());
		// Select transitions starting in current and ending in current
		mask = JDD.Apply(JDD.TIMES, currTrans, mask);
		// Sum all successor probabilities for each (state, action) tuple
		mask = JDD.SumAbstract(mask, model.getAllDDColVars());
		// If the sum for a (state,action) tuple is 1,
		// there is an action that remains in the stable set with prob 1
		mask = JDD.GreaterThan(mask, 1 - settings.getDouble(PrismSettings.PRISM_SUM_ROUND_OFF));
		// select the transitions starting in these tuples
		JDD.Ref(model.getTrans01());
		JDDNode stableTrans01 = JDD.And(model.getTrans01(), mask);
		// Abstract over actions
		return stableTrans01;
	}

	/**
	 * Return the union of sets from {@code sets} which have a non-empty intersection with {@code filter}.
	 * @param sets List of BDDs representing sets (dereferenced after calling this function)
	 * @param filter BDD of states to test for intersection (dereferenced after calling)
	 * @return Referenced BDD representing the filtered union
	 */
	private JDDNode filteredUnion(List<JDDNode> sets, JDDNode filter)
	{
		JDDNode union = JDD.Constant(0);
		for (JDDNode set : sets) {
			if (JDD.AreIntersecting(set, filter))
				union = JDD.Or(union, set);
			else
				JDD.Deref(set);
		}
		JDD.Deref(filter);
		return union;
	}
}

class queueElement
{
	List<JDDNode> statesH;
	List<JDDNode> statesL;
	JDDNode targetDD;
	List<Integer> draIDs;
	int next;
	List<queueElement> children;

	public queueElement(List<JDDNode> statesH, List<JDDNode> statesL, JDDNode targetDD, List<Integer> draIDs, int next)
	{
		this.statesH = statesH;
		this.statesL = statesL;
		this.targetDD = targetDD;
		this.draIDs = draIDs;
		this.next = next;
	}

	public void addChildren(queueElement child)
	{
		if (children == null)
			children = new ArrayList<queueElement>();
		children.add(child);
	}
}
