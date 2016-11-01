//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Vincent Nimal <vincent.nimal@comlab.ox.ac.uk> (University of Oxford)
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

import java.util.*;
import java.io.*;
import java.awt.*;

import javax.swing.*;

import explicit.QuantAbstractRefine;
import explicit.quantile.context.helpers.multiStateSolutions.MultiStateSolutionMethod;
import explicit.quantile.dataStructure.CalculatedValues;
import explicit.quantile.topologicalSorting.TopologicalSorting;

import java.util.regex.*;

import settings.*;

public class PrismSettings implements Observer
{
	//Default Constants
	public static final String DEFAULT_STRING = "";
	public static final int DEFAULT_INT = 0;
	public static final double DEFAULT_DOUBLE = 0.0;
	public static final float DEFAULT_FLOAT = 0.0f;
	public static final long DEFAULT_LONG = 0l;
	public static final boolean DEFAULT_BOOLEAN = false;
	public static final Color DEFAULT_COLOUR = Color.white;
	public static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 12);
	public static final FontColorPair DEFAULT_FONT_COLOUR = new FontColorPair(new Font("monospaced", Font.PLAIN, 12), Color.black);
	public static final File DEFAULT_FILE = null;
	
	//Type Constants
	public static final String STRING_TYPE = "s";
	public static final String INTEGER_TYPE = "i";
	public static final String FLOAT_TYPE = "f";
	public static final String DOUBLE_TYPE = "d";
	public static final String LONG_TYPE = "l";
	public static final String BOOLEAN_TYPE = "b";
	public static final String COLOUR_TYPE = "c";
	//public static final String FONT_TYPE = "fo";	  //remove for now cos we don't have a setting for this yet
	public static final String CHOICE_TYPE = "ch";
	public static final String FONT_COLOUR_TYPE = "fct";
	public static final String FILE_TYPE = "fi";
	
	//Property Constant Keys
	//======================
	
	//PRISM
	public static final	String PRISM_ENGINE							= "prism.engine";
	public static final	String PRISM_VERBOSE						= "prism.verbose";
	public static final	String PRISM_FAIRNESS						= "prism.fairness";
	public static final	String PRISM_PRECOMPUTATION					= "prism.precomputation";
	public static final	String PRISM_PROB0							= "prism.prob0";
	public static final	String PRISM_PROB1							= "prism.prob1";
	public static final	String PRISM_PRE_REL					= "prism.preRel";
	public static final	String PRISM_FIX_DEADLOCKS					= "prism.fixDeadlocks";
	public static final	String PRISM_DO_PROB_CHECKS					= "prism.doProbChecks";
	public static final	String PRISM_SUM_ROUND_OFF					= "prism.sumRoundOff";
	public static final	String PRISM_COMPACT						= "prism.compact";
	public static final	String PRISM_LIN_EQ_METHOD					= "prism.linEqMethod";//"prism.iterativeMethod";
	public static final	String PRISM_LIN_EQ_METHOD_PARAM			= "prism.linEqMethodParam";//"prism.overRelaxation";
	public static final	String PRISM_MDP_SOLN_METHOD				= "prism.mdpSolnMethod";
	public static final	String PRISM_MDP_MULTI_SOLN_METHOD			= "prism.mdpMultiSolnMethod";
	public static final	String PRISM_TERM_CRIT						= "prism.termCrit";//"prism.termination";
	public static final	String PRISM_TERM_CRIT_PARAM				= "prism.termCritParam";//"prism.terminationEpsilon";
	public static final	String PRISM_MAX_ITERS						= "prism.maxIters";//"prism.maxIterations";
	public static final String PRISM_EXPORT_ITERATIONS				= "prism.exportIterations";
	public static final String PRISM_DO_REORDER						= "prism.doReorder";
	public static final String PRISM_REORDER_OPTIONS				= "prism.reorderOptions";

	public static final String PRISM_EXPLODE_BITS					= "prism.explodeBits";
	public static final String PRISM_GLOBALIZE_VARIABLES			= "prism.globalizeVariables";
	public static final	String PRISM_CUDD_MAX_MEM					= "prism.cuddMaxMem";
	public static final	String PRISM_CUDD_EPSILON					= "prism.cuddEpsilon";
	public static final	String PRISM_CUDD_MAX_GROWTH				= "prism.cuddMaxGrowth";
	public static final	String PRISM_DD_EXTRA_STATE_VARS				= "prism.ddExtraStateVars";
	public static final	String PRISM_DD_EXTRA_ACTION_VARS				= "prism.ddExtraActionVars";
	public static final	String PRISM_NUM_SB_LEVELS					= "prism.numSBLevels";//"prism.hybridNumLevels";
	public static final	String PRISM_SB_MAX_MEM						= "prism.SBMaxMem";//"prism.hybridMaxMemory";
	public static final	String PRISM_NUM_SOR_LEVELS					= "prism.numSORLevels";//"prism.hybridSORLevels";
	public static final	String PRISM_SOR_MAX_MEM					= "prism.SORMaxMem";//"prism.hybridSORMaxMemory";
	public static final	String PRISM_DO_SS_DETECTION				= "prism.doSSDetect";
	public static final	String PRISM_EXTRA_DD_INFO					= "prism.extraDDInfo";
	public static final	String PRISM_EXTRA_REACH_INFO				= "prism.extraReachInfo";
	public static final String PRISM_SCC_METHOD						= "prism.sccMethod";
	public static final String PRISM_SCC_METHOD_EXPLICIT			= "prism.sccMethodExplicit";
	public static final String PRISM_EC_METHOD						= "prism.ecMethod";
	public static final String PRISM_SYMM_RED_PARAMS					= "prism.symmRedParams";
	public static final	String PRISM_EXACT_ENABLED					= "prism.exact.enabled";
	public static final String PRISM_PTA_METHOD					= "prism.ptaMethod";
	public static final String PRISM_TRANSIENT_METHOD				= "prism.transientMethod";
	public static final String PRISM_AR_OPTIONS					= "prism.arOptions";
	public static final String PRISM_PATH_VIA_AUTOMATA				= "prism.pathViaAutomata";
	public static final String PRISM_NO_DA_SIMPLIFY				= "prism.noDaSimplify";
	public static final String PRISM_EXPORT_ADV					= "prism.exportAdv";
	public static final String PRISM_EXPORT_ADV_FILENAME			= "prism.exportAdvFilename";
	public static final String PRISM_BOUNDS_VIA_COUNTERS			= "prism.boundsViaCounters";
	
	public static final	String PRISM_MULTI_MAX_POINTS				= "prism.multiMaxIters";
	public static final	String PRISM_PARETO_EPSILON					= "prism.paretoEpsilon";
	public static final	String PRISM_EXPORT_PARETO_FILENAME			= "prism.exportParetoFileName";
	
	public static final String PRISM_LTL2DA_TOOL					= "prism.ltl2daTool";
	public static final String PRISM_LTL2DA_SYNTAX					= "prism.ltl2daSyntax";
	public static final String PRISM_ALLOW_LTL2WDBA					= "prism.allowLTL2WDBA";

	public static final	String PRISM_JDD_SANITY_CHECKS					= "prism.ddsanity";

	public static final	String PRISM_PARAM_ENABLED					= "prism.param.enabled";
	public static final	String PRISM_PARAM_PRECISION				= "prism.param.precision";
	public static final	String PRISM_PARAM_SPLIT					= "prism.param.split";
	public static final	String PRISM_PARAM_BISIM					= "prism.param.bisim";
	public static final	String PRISM_PARAM_FUNCTION					= "prism.param.function";
	public static final	String PRISM_PARAM_ELIM_ORDER				= "prism.param.elimOrder";
	public static final	String PRISM_PARAM_RANDOM_POINTS			= "prism.param.randomPoints";
	public static final	String PRISM_PARAM_SUBSUME_REGIONS			= "prism.param.subsumeRegions";
	public static final String PRISM_PARAM_DAG_MAX_ERROR			= "prism.param.functionDagMaxError";

	public static final String PRISM_FAU_EPSILON					= "prism.fau.epsilon";
	public static final String PRISM_FAU_DELTA						= "prism.fau.delta";
	public static final String PRISM_FAU_INTERVALS					= "prism.fau.intervals";
	public static final String PRISM_FAU_INITIVAL					= "prism.fau.initival";
	public static final String PRISM_FAU_ARRAYTHRESHOLD				= "prism.fau.arraythreshold";
    // TODO(JK): Move to Quantile section
	public static final String QUANTILE_USE_BIGSTEP					= "quantile.useBigStep";
	public static final String QUANTILE_USE_TACAS16					= "quantile.useTACAS16";

	//Simulator
	public static final String SIMULATOR_DEFAULT_NUM_SAMPLES		= "simulator.defaultNumSamples";
	public static final String SIMULATOR_DEFAULT_CONFIDENCE			= "simulator.defaultConfidence";
	public static final String SIMULATOR_DEFAULT_WIDTH				= "simulator.defaultWidth";
	public static final String SIMULATOR_DEFAULT_APPROX				= "simulator.defaultApprox";
	public static final String SIMULATOR_DEFAULT_MAX_PATH			= "simulator.defaultMaxPath";
	public static final String SIMULATOR_DECIDE 					= "simulator.decide";
	public static final String SIMULATOR_ITERATIONS_TO_DECIDE		= "simulator.iterationsToDecide";
	public static final String SIMULATOR_MAX_REWARD					= "simulator.maxReward";
	public static final	String SIMULATOR_SIMULTANEOUS				= "simulator.simultaneous";
	public static final String SIMULATOR_FIELD_CHOICE				= "simulator.fieldChoice";
	public static final	String SIMULATOR_NEW_PATH_ASK_VIEW			= "simulator.newPathAskView";
	public static final	String SIMULATOR_RENDER_ALL_VALUES			= "simulator.renderAllValues";
	public static final String SIMULATOR_NETWORK_FILE				= "simulator.networkFile";
	
	//GUI Model
	public static final	String MODEL_AUTO_PARSE						= "model.autoParse";
	public static final	String MODEL_AUTO_MANUAL					= "model.autoManual";
	public static final	String MODEL_PARSE_DELAY					= "model.parseDelay";
	public static final	String MODEL_PRISM_EDITOR_FONT				= "model.prismEditor.font";
	public static final	String MODEL_SHOW_LINE_NUMBERS				= "model.prismEditor.lineNumbers";
	
	//public static final	String MODEL_PRISM_EDITOR_FONT_COLOUR		= "model.prismEditor.fontColour";
	public static final	String MODEL_PRISM_EDITOR_BG_COLOUR			= "model.prismEditor.bgColour";
	public static final	String MODEL_PRISM_EDITOR_NUMERIC_COLOUR	= "model.prismEditor.numericColour";
	public static final	String MODEL_PRISM_EDITOR_NUMERIC_STYLE		= "model.prismEditor.numericStyle";
	public static final	String MODEL_PRISM_EDITOR_IDENTIFIER_COLOUR	  =	"model.prismEditor.identifierColour";
	public static final	String MODEL_PRISM_EDITOR_IDENTIFIER_STYLE	  =	"model.prismEditor.identifierStyle";
	public static final	String MODEL_PRISM_EDITOR_KEYWORD_COLOUR	= "model.prismEditor.keywordColour";
	public static final	String MODEL_PRISM_EDITOR_KEYWORD_STYLE		= "model.prismEditor.keywordStyle";
	public static final	String MODEL_PRISM_EDITOR_COMMENT_COLOUR	= "model.prismEditor.commentColour";
	public static final	String MODEL_PRISM_EDITOR_COMMENT_STYLE		= "model.prismEditor.commentStyle";
	public static final	String MODEL_PEPA_EDITOR_FONT				= "model.pepaEditor.font";
	//public static final	String MODEL_PEPA_EDITOR_FONT_COLOUR		= "model.pepaEditor.fontColour";
	public static final	String MODEL_PEPA_EDITOR_BG_COLOUR			= "model.pepaEditor.bgColour";
	public static final	String MODEL_PEPA_EDITOR_COMMENT_COLOUR		= "model.pepaEditor.commentColour";
	public static final	String MODEL_PEPA_EDITOR_COMMENT_STYLE		= "model.pepaEditor.commentStyle";
	
	//GUI Properties
	public static final	String PROPERTIES_FONT						= "properties.font";
	public static final	String PROPERTIES_SELECTION_COLOUR			= "properties.selectionColour";
	public static final	String PROPERTIES_WARNING_COLOUR			= "properties.warningColour";
	public static final	String PROPERTIES_ADDITION_STRATEGY			= "properties.additionStategy";
	public static final	String PROPERTIES_CLEAR_LIST_ON_LOAD		= "properties.clearListOnLoad";
	
	//GUI Log
	public static final	String LOG_FONT								= "log.font";
	public static final	String LOG_SELECTION_COLOUR					= "log.selectionColour";
	public static final	String LOG_BG_COLOUR						= "log.bgColour";
	public static final	String LOG_BUFFER_LENGTH					= "log.bufferLength";
	
	//GUI Quantile
	public static final String QUANTILE_SCC_METHOD					= "quantile.sccMethod";
	public static final String QUANTILE_VALUES_STORAGE				= "quantile.valuesStorage";
	public static final String QUANTILE_SET_FACTORY					= "quantile.setFactory";
	public static final String QUANTILE_ADAPTIVE_SET_THRESHOLD		= "quantile.adaptiveSetThreshold";
	public static final String QUANTILE_MULTI_STATE_METHOD			= "quantile.multiStateMethod";
	public static final String QUANTILE_USE_ZERO_REWARD_TRY_AND_SET	= "quantile.useZeroRewardTryAndSet";
	public static final String QUANTILE_USE_LP_SOLVER_ONLY			= "quantile.useLpSolverOnly";
	public static final String QUANTILE_LP_SOLVER_ONLY_BOUND		= "quantile.lpSolverOnlyBound";
	public static final String QUANTILE_PARALLEL_POOL_SIZE			= "quantile.parallelPoolSize";
	public static final String QUANTILE_POSITIVE_REWARDS_PARALLEL	= "quantile.positiveRewardsParallel";
	public static final String QUANTILE_ZERO_REWARDS_PARALLEL		= "quantile.zeroRewardsParallel";
	public static final String QUANTILE_DEBUG_LEVEL					= "quantile.debugLevel";
	public static final String QUANTILE_VERIFY_RESULT				= "quantile.verifyResult";
	public static final String QUANTILE_USE_QUANTITATIVE			= "quantile.useQuantitative";
	public static final String QUANTILE_BACKEND_FOR_TIME_BOUND		= "quantile.quantileBackendForTimeBound";
	public static final String QUANTILE_NAIVE_COMPUTATION			= "quantile.naiveComputation";
	public static final String QUANTILE_NAIVE_COMPUTATION_LINEAR	= "quantile.naiveComputationLinear";
	public static final String QUANTILE_CTMC_START_PRECISION		= "quantile.ctmcStartPrecision";
	public static final String QUANTILE_CTMC_PRECISION				= "quantile.ctmcPrecision";

	
	//Defaults, types and constaints
	
	public static final String[] propertyOwnerNames =
	{
		"PRISM",
		"Simulator",
		"Model",
		"Properties",
		"Log",
		"Quantile"
	};
	public static final int[] propertyOwnerIDs =
	{
		PropertyConstants.PRISM,
		PropertyConstants.SIMULATOR,
		PropertyConstants.MODEL,
		PropertyConstants.PROPERTIES,
		PropertyConstants.LOG,
		PropertyConstants.QUANTILE
	};
	
	
	// Property table:
	// * Datatype = type of choice (see type constants at top of this file)
	// * Key = internal key, used programmatically; need to add this to key list above
	// * Display name = display name; used e.g. in GUI options dialog
	// * Version = last public version of PRISM in which this setting did *not* appear
	//   (the main use for this is to allow a new default setting to be provided in a new version of PRISM)
	//   (if the version of the user settings file is older, a new default value will be set in the file)
	// * Default = default value 
	// * Constraints = limitations on possible values 
	// * Comments = explanatory comments; used e.g. in GUI options dialog 
	
	public static final Object[][][] propertyData =
	{
		{	//Datatype:			Key:									Display name:							Version:		Default:																	Constraints:																				Comment:
			//====================================================================================================================================================================================================================================================================================================================================
			
			// ENGINES/METHODS:
			{ CHOICE_TYPE,		PRISM_ENGINE,							"Engine",								"2.1",			"Hybrid",																	"MTBDD,Sparse,Hybrid,Explicit",																		
																			"Which engine (hybrid, sparse, MTBDD, explicit) should be used for model checking." },
			{ BOOLEAN_TYPE,		PRISM_EXACT_ENABLED,					"Do exact model checking",			"4.2.1",			new Boolean(false),															"",
																			"Perform exact model checking." },
																			
			{ CHOICE_TYPE,		PRISM_PTA_METHOD,						"PTA model checking method",			"3.3",			"Stochastic games",																	"Digital clocks,Stochastic games,Backwards reachability",																
																			"Which method to use for model checking of PTAs." },
			{ CHOICE_TYPE,		PRISM_TRANSIENT_METHOD,					"Transient probability computation method",	"3.3",		"Uniformisation",															"Uniformisation,Fast adaptive uniformisation",																
																			"Which method to use for computing transient probabilities in CTMCs." },
			// NUMERICAL SOLUTION OPTIONS:
			{ CHOICE_TYPE,		PRISM_LIN_EQ_METHOD,					"Linear equations method",				"2.1",			"Jacobi",																	"Power,Jacobi,Gauss-Seidel,Backwards Gauss-Seidel,Pseudo-Gauss-Seidel,Backwards Pseudo-Gauss-Seidel,JOR,SOR,Backwards SOR,Pseudo-SOR,Backwards Pseudo-SOR",
																			"Which iterative method to use when solving linear equation systems." },
			{ DOUBLE_TYPE,		PRISM_LIN_EQ_METHOD_PARAM,				"Over-relaxation parameter",			"2.1",			new Double(0.9),															"",																							
																			"Over-relaxation parameter for iterative numerical methods such as JOR/SOR." },
			{ CHOICE_TYPE,		PRISM_MDP_SOLN_METHOD,					"MDP solution method",				"4.0",			"Value iteration",																"Value iteration,Gauss-Seidel,Policy iteration,Modified policy iteration,Linear programming",
																			"Which method to use when solving Markov decision processes." },
			{ CHOICE_TYPE,		PRISM_MDP_MULTI_SOLN_METHOD,			"MDP multi-objective solution method",				"4.0.3",			"Value iteration",											"Value iteration,Gauss-Seidel,Linear programming",
																			"Which method to use when solving multi-objective queries on Markov decision processes." },
			{ CHOICE_TYPE,		PRISM_TERM_CRIT,						"Termination criteria",					"2.1",			"Relative",																	"Absolute,Relative",																		
																			"Criteria to use for checking termination of iterative numerical methods." },
			{ DOUBLE_TYPE,		PRISM_TERM_CRIT_PARAM,					"Termination epsilon",					"2.1",			new Double(1.0E-6),															"0.0,",																						
																			"Epsilon value to use for checking termination of iterative numerical methods." },
			{ INTEGER_TYPE,		PRISM_MAX_ITERS,						"Termination max. iterations",			"2.1",			new Integer(10000),															"0,",																						
																			"Maximum number of iterations to perform if iterative methods do not converge." },
			{ BOOLEAN_TYPE,		PRISM_EXPORT_ITERATIONS,				"Export iterations (debug/visualisation)",			"4.3.11",			false,														"",																						
																			"Export solution vectors for iteration algorithms to iterations.html"},
			// MODEL CHECKING OPTIONS:

			{ BOOLEAN_TYPE,		PRISM_PRECOMPUTATION,					"Use precomputation",					"2.1",			new Boolean(true),															"",																							
																			"Whether to use model checking precomputation algorithms (Prob0, Prob1, etc.), where optional." },
			{ BOOLEAN_TYPE,		PRISM_PROB0,							"Use Prob0 precomputation",				"4.0.2",		new Boolean(true),															"",																							
																			"Whether to use model checking precomputation algorithm Prob0 (if precomputation enabled)." },
			{ BOOLEAN_TYPE,		PRISM_PROB1,							"Use Prob1 precomputation",				"4.0.2",		new Boolean(true),															"",																							
																			"Whether to use model checking precomputation algorithm Prob1 (if precomputation enabled)." },
			{ BOOLEAN_TYPE,		PRISM_PRE_REL,							"Use predecessor relation",		"4.2.1",		new Boolean(true),											"",
																			"Whether to use a pre-computed predecessor relation in several algorithms." },
			{ BOOLEAN_TYPE,		PRISM_FAIRNESS,							"Use fairness",							"2.1",			new Boolean(false),															"",																							
																			"Constrain to fair adversaries when model checking MDPs." },
			{ BOOLEAN_TYPE,		PRISM_FIX_DEADLOCKS,					"Automatically fix deadlocks",			"4.0.3",		new Boolean(true),															"",																							
																			"Automatically fix deadlocks, where necessary, when constructing probabilistic models." },
			{ BOOLEAN_TYPE,		PRISM_DO_PROB_CHECKS,					"Do probability/rate checks",			"2.1",			new Boolean(true),															"",																							
																			"Perform sanity checks on model probabilities/rates when constructing probabilistic models." },
			{ DOUBLE_TYPE,		PRISM_SUM_ROUND_OFF,					"Probability sum threshold",					"2.1",			new Double(1.0E-5),													"0.0,",
																			"Round-off threshold for places where doubles are summed and compared to integers (e.g. checking that probabilities sum to 1 in an update)." },							
			{ BOOLEAN_TYPE,		PRISM_DO_SS_DETECTION,					"Use steady-state detection",			"2.1",			new Boolean(true),															"0,",																						
																			"Use steady-state detection during CTMC transient probability computation." },
			{ CHOICE_TYPE,		PRISM_SCC_METHOD,						"SCC decomposition method",				"3.2",			"Lockstep",																	"Xie-Beerel,Lockstep,SCC-Find",																
																			"Which algorithm to use for (symbolic) decomposition of a graph into strongly connected components (SCCs)." },
			{ CHOICE_TYPE,		PRISM_SCC_METHOD_EXPLICIT,				"SCC decomposition method (explicit)",				"4.2.1",			"Tarjan-Stack",		"Tarjan-Recursive,Tarjan-Stack",
																			"Which algorithm to use for (explicit) decomposition of a graph into strongly connected components (SCCs)." },
			{ CHOICE_TYPE,		PRISM_EC_METHOD,						"End-component computation method",				"4.2.1",			"Default",																	"Default,On-The-Fly",																
																			"Which algorithm to use for end-component calculations." },
			{ STRING_TYPE,		PRISM_SYMM_RED_PARAMS,					"Symmetry reduction parameters",		"3.2",			"",																	"",																
																			"Parameters for symmetry reduction (format: \"i j\" where i and j are the number of modules before and after the symmetric ones; empty string means symmetry reduction disabled)." },
			{ STRING_TYPE,		PRISM_AR_OPTIONS,						"Abstraction refinement options",		"3.3",			"",																	"",																
																			"Various options passed to the asbtraction-refinement engine (e.g. for PTA model checking)." },
			{ BOOLEAN_TYPE,		PRISM_PATH_VIA_AUTOMATA,				"All path formulas via automata",			"4.2.1",			new Boolean(false),									"",
																			"Handle all path formulas via automata constructions." },
			{ BOOLEAN_TYPE,		PRISM_BOUNDS_VIA_COUNTERS,				"All bounds via counter transformation",			"4.2.1",			new Boolean(false),									"",
																			"Handle all bounds via counter constructions." },
			{ BOOLEAN_TYPE,		PRISM_NO_DA_SIMPLIFY,				"Do not simplify deterministic automata",			"4.3",			new Boolean(false),									"",
																			"Do not attempt to simplify deterministic automata, acceptance conditions (for debugging)." },

			{ BOOLEAN_TYPE,		PRISM_EXPLODE_BITS,						"Explode variable storage into individual bits",					"4.3.1",			new Boolean(false),															"",
																			"Explode variable storage into individual bits" },
			{ BOOLEAN_TYPE,		PRISM_GLOBALIZE_VARIABLES,				"Globalize variables",					"4.3",			new Boolean(false),															"",
																			"Globalize variables" },

			// MULTI-OBJECTIVE MODEL CHECKING OPTIONS:
			{ INTEGER_TYPE,		PRISM_MULTI_MAX_POINTS,					"Max. multi-objective corner points",			"4.0.3",			new Integer(50),															"0,",																						
																			"Maximum number of corner points to explore if (value iteration based) multi-objective model checking does not converge." },
			{ DOUBLE_TYPE,		PRISM_PARETO_EPSILON,					"Pareto approximation threshold",			"4.0.3",			new Double(1.0E-2),															"0.0,",																						
																			"Determines to what precision the Pareto curve will be approximated." },
			{ STRING_TYPE,		PRISM_EXPORT_PARETO_FILENAME,			"Pareto curve export filename",			"4.0.3",			"",															"0,",																						
																			"If non-empty, any Pareto curve generated will be exported to this file." },
			// OUTPUT OPTIONS:
			{ BOOLEAN_TYPE,		PRISM_VERBOSE,							"Verbose output",						"2.1",		new Boolean(false),															"",																							
																			"Display verbose output to log." },
			{ BOOLEAN_TYPE,		PRISM_EXTRA_DD_INFO,					"Extra MTBDD information",				"3.1.1",		new Boolean(false),															"0,",																						
																			"Display extra information about (MT)BDDs used during and after model construction." },
			{ BOOLEAN_TYPE,		PRISM_EXTRA_REACH_INFO,					"Extra reachability information",		"3.1.1",		new Boolean(false),															"0,",																						
																			"Display extra information about progress of reachability during model construction." },
			// SPARSE/HYBRID/MTBDD OPTIONS:
			{ BOOLEAN_TYPE,		PRISM_COMPACT,							"Use compact schemes",					"2.1",			new Boolean(true),															"",																							
																			"Use additional optimisations for compressing sparse matrices and vectors with repeated values." },
			{ INTEGER_TYPE,		PRISM_NUM_SB_LEVELS,					"Hybrid sparse levels",					"2.1",			new Integer(-1),															"-1,",																						
																			"Number of MTBDD levels ascended when adding sparse matrices to hybrid engine data structures (-1 means use default)." },
			{ INTEGER_TYPE,		PRISM_SB_MAX_MEM,						"Hybrid sparse memory (KB)",			"2.1",			new Integer(1024),															"0,",																						
																			"Maximum memory usage when adding sparse matrices to hybrid engine data structures (KB)." },
			{ INTEGER_TYPE,		PRISM_NUM_SOR_LEVELS,					"Hybrid GS levels",						"2.1",			new Integer(-1),															"-1,",																						
																			"Number of MTBDD levels descended for hybrid engine data structures block division with GS/SOR." },
			{ INTEGER_TYPE,		PRISM_SOR_MAX_MEM,						"Hybrid GS memory (KB)",				"2.1",			new Integer(1024),															"0,",																						
																			"Maximum memory usage for hybrid engine data structures block division with GS/SOR (KB)." },
			{ STRING_TYPE,		PRISM_CUDD_MAX_MEM,						"CUDD max. memory",				"4.2.1",			new String("1g"),														"",																						
																			"Maximum memory available to CUDD (underlying BDD/MTBDD library), e.g. 125k, 50m, 4g. Note: Restart PRISM after changing this." },
			{ DOUBLE_TYPE,		PRISM_CUDD_EPSILON,						"CUDD epsilon",							"2.1",			new Double(1.0E-15),														"0.0,",																						
																			"Epsilon value used by CUDD (underlying BDD/MTBDD library) for terminal cache comparisons." },
			{ DOUBLE_TYPE,		PRISM_CUDD_MAX_GROWTH,					"CUDD reordering max growth",							"4.3.1",			new Double(1.2),														"0.0,",
																		"The max growth factor for CUDD reordering (e.g. 1.2 = maximal growth of 20%)." },

			{ INTEGER_TYPE,		PRISM_DD_EXTRA_STATE_VARS,				"Extra DD state var allocation",		"4.3.1",			new Integer(20),														"",
																			"Number of extra DD state variables preallocated for use in model transformation." },
			{ INTEGER_TYPE,		PRISM_DD_EXTRA_ACTION_VARS,				"Extra DD action var allocation",		"4.3.1",			new Integer(20),														"",
																			"Number of extra DD action variables preallocated for use in model transformation." },


			{ BOOLEAN_TYPE,		PRISM_DO_REORDER,						"MTBDD reordering",	"4.3.1",	new Boolean(false),			"",
																		"Perform reordering when building the model." },
			{ STRING_TYPE,		PRISM_REORDER_OPTIONS,					"Options for MTBDD reordering",	"4.3.1",	"",			"",
																		"Comma-separated options for reordering (norebuild, beforereach, noconstraints, optimisetrans, converge)." },


			// ADVERSARIES/COUNTEREXAMPLES:
			{ CHOICE_TYPE,		PRISM_EXPORT_ADV,						"Adversary export",						"3.3",			"None",																	"None,DTMC,MDP",																
																			"Type of adversary to generate and export during MDP model checking" },
			{ STRING_TYPE,		PRISM_EXPORT_ADV_FILENAME,				"Adversary export filename",			"3.3",			"adv.tra",																	"",															
																			"Name of file for MDP adversary export (if enabled)" },
																		
			// LTL2DA TOOLS
			{ STRING_TYPE,		PRISM_LTL2DA_TOOL,						"Use external LTL->DA tool",		"4.2.1",			"",		null,
																			"If non-empty, the path to the executable for the external LTL->DA tool."},

			{ CHOICE_TYPE,		PRISM_LTL2DA_SYNTAX,					"LTL syntax for external LTL->DA tool",		"4.2.1",			"LBT",		"LBT,Spin,Spot,Rabinizer",
																			"The syntax for LTL formulas passed to the external LTL->DA tool."},
			{ BOOLEAN_TYPE,		PRISM_ALLOW_LTL2WDBA,					"Allow use of LTL2WDBA conversion for obligation and co-safety formulas",		"4.3.1",			false, 		"",
																			"Allow use of LTL2WDBA conversion for obligation and co-safety formulas."},

			// DEBUG / SANITY CHECK OPTIONS:
			{ BOOLEAN_TYPE,		PRISM_JDD_SANITY_CHECKS,					"Do BDD sanity checks",			"4.3.1",			new Boolean(false),		"",
																			"Perform internal sanity checks during computations (can cause significant slow-down)." },

			// PARAMETRIC MODEL CHECKING
			{ BOOLEAN_TYPE,		PRISM_PARAM_ENABLED,					"Do parametric model checking",			"4.1",			new Boolean(false),															"",
																			"Perform parametric model checking." },
			{ STRING_TYPE,		PRISM_PARAM_PRECISION,					"Parametric model checking precision",	"4.1",			"5/100",																	"",
																			"Maximal volume of area to remain undecided in each step when performing parametric model checking." },
			{ CHOICE_TYPE,		PRISM_PARAM_SPLIT,						"Parametric model checking split method",							"4.1",			"Longest",																	"Longest,All",
																			"Strategy to use when splitting a region during parametric model checking. Either split on longest side, or split on all sides." },
			{ CHOICE_TYPE,		PRISM_PARAM_BISIM,						"Parametric model checking bisimulation method",					"4.1",			"Weak",																		"Weak,Strong,None",
																			"Type of bisimulation used to reduce model size during paramteric model checking. For reward-based properties, weak bisimulation cannot be used." },
			{ CHOICE_TYPE,		PRISM_PARAM_FUNCTION,					"Parametric model checking function representation",				"4.1",			"JAS-cached",																"JAS-cached,JAS,DAG",
																			"Type of representation for functions used during parametric model checking." },
			{ CHOICE_TYPE,		PRISM_PARAM_ELIM_ORDER,					"Parametric model checking state elimination order",			"4.1",			"Backward",																		"Arbitrary,Forward,Forward-reversed,Backward,Backward-reversed,Random",
																			"Order in which states are eliminated during unbounded parametric model checking analysis." },
			{ INTEGER_TYPE,		PRISM_PARAM_RANDOM_POINTS,				"Parametric model checking random evaluations",		"4.1",			new Integer(5),																"",
																			"Number of random points to evaluate per region to increase chance of correctness during parametric model checking." },
			{ BOOLEAN_TYPE,		PRISM_PARAM_SUBSUME_REGIONS,			"Parametric model checking region subsumption",				"4.1",			new Boolean(true),															"",
																			"Subsume adjacent regions during parametric model checking." },
			{ DOUBLE_TYPE,		PRISM_PARAM_DAG_MAX_ERROR,				"Parametric model checking max. DAG error",	"4.1",			new Double(1E-100),															"",
																			"Maximal error probability (i.e. maximum probability of of a wrong result) in DAG function representation used for parametric model checking." },
			// TODO(JK): Move to correct location
			{ BOOLEAN_TYPE,	QUANTILE_USE_BIGSTEP,		"use the symbolic big-step approach",	"4.3.1",	new Boolean(false),	"",	"Use the symbolic big-step approach."},
			{ BOOLEAN_TYPE,	QUANTILE_USE_TACAS16,		"use the TACAS'16 version of symbolic quantile computations",	"4.3.1",	new Boolean(false),	"",	"Use the TACAS'16 variant of symbolic quantile computations"},
			
			// FAST ADAPTIVE UNIFORMISATION																
			{ DOUBLE_TYPE,      PRISM_FAU_EPSILON,						"FAU epsilon",		 					"4.1",   	 	new Double(1E-6),     													"",
																			"For fast adaptive uniformisation (FAU), decides how much probability may be lost due to truncation of birth process." },
			{ DOUBLE_TYPE,      PRISM_FAU_DELTA,						"FAU cut off delta", 					"4.1",   	 	new Double(1E-12),     													"",
																			"For fast adaptive uniformisation (FAU), states whose probability is below this value will be removed." },
			{ INTEGER_TYPE,     PRISM_FAU_ARRAYTHRESHOLD,				"FAU array threshold", 					"4.1",   	 	new Integer(100),    	 													"",
																			"For fast adaptive uniformisation (FAU), after this number of iterations without changes to the state space, storage is switched to a faster, fixed-size data structure." },
			{ INTEGER_TYPE,     PRISM_FAU_INTERVALS,					"FAU time intervals",					"4.1",   	 	new Integer(1),     														"",
																			"For fast adaptive uniformisation (FAU), the time period is split into this number of of intervals." },
			{ DOUBLE_TYPE,      PRISM_FAU_INITIVAL,						"FAU initial time interval",			"4.1",   	 	new Double(1.0),     														"",	
																			"For fast adaptive uniformisation (FAU), the length of initial time interval to analyse." },
		},
		{
			{ INTEGER_TYPE,		SIMULATOR_DEFAULT_NUM_SAMPLES,			"Default number of samples",			"4.0",		new Integer(1000),			"1,",
																			"Default number of samples when using approximate (simulation-based) model checking (CI/ACI/APMC methods)." },
			{ DOUBLE_TYPE,		SIMULATOR_DEFAULT_CONFIDENCE,			"Default confidence parameter",			"4.0",		new Double(0.01),			"0,1",
																			"Default value for the 'confidence' parameter when using approximate (simulation-based) model checking (CI/ACI/APMC/SPRT methods). For CI/ACI, this means that the corresponding 'confidence level' is 100 x (1 - confidence)%; for APMC, this is the probability of the 'approximation' being exceeded; for SPRT, this is the acceptable probability for type I/II errors." },
			{ DOUBLE_TYPE,		SIMULATOR_DEFAULT_WIDTH,				"Default width of confidence interval",	"4.0",		new Double(0.05),			"0,",
																			"Default (half-)width of the confidence interval when using approximate (simulation-based) model checking (CI/ACI/SPRT methods). For SPRT, this refers to the 'indifference' parameter." },
			{ DOUBLE_TYPE,		SIMULATOR_DEFAULT_APPROX,				"Default approximation parameter",		"4.0",		new Double(0.05),			"0,",
																			"Default value for the 'approximation' parameter when using approximate (simulation-based) model checking (APMC method)." },
			{ LONG_TYPE,		SIMULATOR_DEFAULT_MAX_PATH,				"Default maximum path length",			"2.1",		new Long(10000),			"1,",
																			"Default maximum path length when using approximate (simulation-based) model checking." },
			{ BOOLEAN_TYPE,		SIMULATOR_DECIDE,						"Decide S^2=0 or not automatically",	"4.0",		new	Boolean(true),			"",
																			"Let PRISM choose whether, after a certain number of iterations, the standard error is null or not." },
			{ INTEGER_TYPE,		SIMULATOR_ITERATIONS_TO_DECIDE,			"Number of iterations to decide",		"4.0",		new	Integer(10000),			"1,",
																			"Number of iterations to decide whether the standard error is null or not." },
			{ DOUBLE_TYPE,		SIMULATOR_MAX_REWARD,					"Maximum reward",						"4.0",		new	Double(1000.0),			"1,",
																			"Maximum reward for CI/ACI methods. It helps these methods in displaying the progress in case of rewards computation." },
			{ BOOLEAN_TYPE,		SIMULATOR_SIMULTANEOUS,					"Check properties simultaneously",		"2.1",		new Boolean(true),			"",
																			"Check multiple properties simultaneously over the same set of execution paths (simulator only)." },
			{ CHOICE_TYPE,		SIMULATOR_FIELD_CHOICE,					"Values used in dialog",				"2.1",		"Last used values",			"Last used values,Always use defaults",
																			"How to choose values for the simulation dialog: remember previously used values or revert to the defaults each time." },
			{ BOOLEAN_TYPE,		SIMULATOR_NEW_PATH_ASK_VIEW,			"Ask for view configuration",			"2.1",		new Boolean(false),			"",
																			"Display dialog with display options when creating a new simulation path." },
			{ CHOICE_TYPE,		SIMULATOR_RENDER_ALL_VALUES,			"Path render style",					"3.2",		"Render all values",		"Render changes,Render all values",
																			"Display style for paths in the simulator user interface: only show variable values when they change, or show all values regardless." },
			{ FILE_TYPE,		SIMULATOR_NETWORK_FILE,					"Network profile",						"2.1",		new File(""),				"",
																			"File specifying the network profile used by the distributed PRISM simulator." }
		},
		{
			{ BOOLEAN_TYPE,		MODEL_AUTO_PARSE,						"Auto parse",							"2.1",			new Boolean(true),															"",																							"Parse PRISM models automatically as they are loaded/edited in the text editor." },
			{ BOOLEAN_TYPE,		MODEL_AUTO_MANUAL,						"Manual parse for large models",		"2.1",			new Boolean(true),															"",																							"Disable automatic model parsing when loading large PRISM models." },
			{ INTEGER_TYPE,		MODEL_PARSE_DELAY,						"Parse delay (ms)",						"2.1",			new Integer(1000),															"0,",																						"Time delay (after typing has finished) before an automatic re-parse of the model is performed." },
			{ FONT_COLOUR_TYPE,	MODEL_PRISM_EDITOR_FONT,				"PRISM editor font",					"2.1",			new FontColorPair(new Font("monospaced", Font.PLAIN, 12), Color.black),		"",																							"Font used in the PRISM model text editor." },
			{ BOOLEAN_TYPE,		MODEL_SHOW_LINE_NUMBERS,				"PRISM editor line numbers",            "3.2",    new Boolean(true),															"",																							"Enable or disable line numbers in the PRISM model text editor" },
			{ COLOUR_TYPE,		MODEL_PRISM_EDITOR_BG_COLOUR,			"PRISM editor background",				"2.1",			new Color(255,255,255),														"",																							"Background colour for the PRISM model text editor." },
			{ COLOUR_TYPE,		MODEL_PRISM_EDITOR_NUMERIC_COLOUR,		"PRISM editor numeric colour",			"2.1",			new Color(0,0,255),															"",																							"Syntax highlighting colour for numerical values in the PRISM model text editor." },
			{ CHOICE_TYPE,		MODEL_PRISM_EDITOR_NUMERIC_STYLE,		"PRISM editor numeric style",			"2.1",			"Plain",																	"Plain,Italic,Bold,Bold Italic",															"Syntax highlighting style for numerical values in the PRISM model text editor." },
			{ COLOUR_TYPE,		MODEL_PRISM_EDITOR_IDENTIFIER_COLOUR,	"PRISM editor identifier colour",		"2.1",			new Color(255,0,0),															"",																							"Syntax highlighting colour for identifiers values in the PRISM model text editor" },
			{ CHOICE_TYPE,		MODEL_PRISM_EDITOR_IDENTIFIER_STYLE,	"PRISM editor identifier style",		"2.1",			"Plain",																	"Plain,Italic,Bold,Bold Italic",															"Syntax highlighting style for identifiers in the PRISM model text editor." },
			{ COLOUR_TYPE,		MODEL_PRISM_EDITOR_KEYWORD_COLOUR,		"PRISM editor keyword colour",			"2.1",			new Color(0,0,0),															"",																							"Syntax highlighting colour for keywords in the PRISM model text editor" },
			{ CHOICE_TYPE,		MODEL_PRISM_EDITOR_KEYWORD_STYLE,		"PRISM editor keyword style",			"2.1",			"Bold",																		"Plain,Italic,Bold,Bold Italic",															"Syntax highlighting style for keywords in the PRISM model text editor." },
			{ COLOUR_TYPE,		MODEL_PRISM_EDITOR_COMMENT_COLOUR,		"PRISM editor comment colour",			"2.1",			new Color(0,99,0),															"",																							"Syntax highlighting colour for comments in the PRISM model text editor." },
			{ CHOICE_TYPE,		MODEL_PRISM_EDITOR_COMMENT_STYLE,		"PRISM editor comment style",			"2.1",			"Italic",																	"Plain,Italic,Bold,Bold Italic",															"Syntax highlighting style for comments in the PRISM model text editor." },
			{ FONT_COLOUR_TYPE,	MODEL_PEPA_EDITOR_FONT,					"PEPA editor font",						"2.1",			new FontColorPair(new Font("monospaced", Font.PLAIN, 12), Color.black),		"",																							"Font used in the PEPA model text editor." },
			{ COLOUR_TYPE,		MODEL_PEPA_EDITOR_BG_COLOUR,			"PEPA editor background",				"2.1",			new Color(255,250,240),														"",																							"Background colour for the PEPA model text editor." },
			{ COLOUR_TYPE,		MODEL_PEPA_EDITOR_COMMENT_COLOUR,		"PEPA editor comment colour",			"2.1",			new Color(0,99,0),															"",																							"Syntax highlighting colour for comments in the PEPA model text editor." },
			{ CHOICE_TYPE,		MODEL_PEPA_EDITOR_COMMENT_STYLE,		"PEPA editor comment style",			"2.1",			"Italic",																	"Plain,Italic,Bold,Bold Italic",															"Syntax highlighting style for comments in the PEPA model text editor." }
		},
		{
			{ FONT_COLOUR_TYPE,	PROPERTIES_FONT,						"Display font",							"2.1",			new FontColorPair(new Font("monospaced", Font.PLAIN, 12), Color.black),		"",																							"Font used for the properties list." },
			{ COLOUR_TYPE,		PROPERTIES_WARNING_COLOUR,				"Warning colour",						"2.1",			new Color(255,130,130),														"",																							"Colour used to indicate that a property is invalid." },
			{ CHOICE_TYPE,		PROPERTIES_ADDITION_STRATEGY,			"Property addition strategy",			"2.1",			"Warn when invalid",														"Warn when invalid,Do not allow invalid",													"How to deal with properties that are invalid." },
			{ BOOLEAN_TYPE,		PROPERTIES_CLEAR_LIST_ON_LOAD,			"Clear list when load model",			"2.1",			new Boolean(true),															"",																							"Clear the properties list whenever a new model is loaded." }
		},
		{
			{ FONT_COLOUR_TYPE,	LOG_FONT,								"Display font",							"2.1",			new FontColorPair(new Font("monospaced", Font.PLAIN, 12), Color.black),		"",																							"Font used for the log display." },
			{ COLOUR_TYPE,		LOG_BG_COLOUR,							"Background colour",					"2.1",			new Color(255,255,255),														"",																							"Background colour for the log display." },
			{ INTEGER_TYPE,		LOG_BUFFER_LENGTH,						"Buffer length",						"2.1",			new Integer(10000),															"1,",																						"Length of the buffer for the log display." }
		},
		{
			{ CHOICE_TYPE,	QUANTILE_SCC_METHOD,						"method to compute zero-reward SCCs",					"4.1",	TopologicalSorting.QuantileSccMethod.TARJAN_ITERATIVE.toString(),	Arrays.stream(TopologicalSorting.QuantileSccMethod.values()).map(Object::toString).reduce((s, e) -> s + "," + e).orElse(""),	"The method to compute the topological sorting of the model's zero-reward SCCs. NONE deactivates the topological sorting."},
			{ CHOICE_TYPE,	QUANTILE_VALUES_STORAGE,					"storage of positive reward successor values",			"4.1",	CalculatedValues.PreviousValuesStorage.ALL_STATES.toString(),		Arrays.stream(CalculatedValues.PreviousValuesStorage.values()).map(Object::toString).reduce((s, e) -> s + "," + e).orElse(""),	"Should calculated values for each state be stored, or just for states that are successors of positive reward states / transitions."},
			{ CHOICE_TYPE,	QUANTILE_SET_FACTORY,						"implementation exploited for quantile computations",	"4.1",	"ADAPTIVE_SINGLETON_SET",											"ADAPTIVE_SINGLETON_SET,ADAPTIVE_SET,ADAPTIVE_SET_USING_HASHSET,BIT_SET",														"The implementation that is responsible for storing the state-sets used by the quantile-calculations"},
			{ INTEGER_TYPE,	QUANTILE_ADAPTIVE_SET_THRESHOLD,			"threshold for the adaptive sets",						"4.1",	new Integer(1),														"",																																"The threshold which determines when AdaptiveSet should switch to BitSet in order to store the states."},
			{ CHOICE_TYPE,	QUANTILE_MULTI_STATE_METHOD,				"method for solving multiple states",					"4.1",	MultiStateSolutionMethod.VALUE_ITERATION.toString(),				Arrays.stream(MultiStateSolutionMethod.values()).map(Object::toString).reduce((s, e) -> s + "," + e).orElse(""),				"The method that solves states belonging to zero-reward cycles."},
			{ BOOLEAN_TYPE,	QUANTILE_USE_ZERO_REWARD_TRY_AND_SET,		"use try-and-set to compute zero-reward states",		"4.1",	new Boolean(true),													"",																																"Solve zero reward states using try-and-set first. This calculates states which are not in a zero-reward cycle."},
			{ BOOLEAN_TYPE,	QUANTILE_USE_LP_SOLVER_ONLY,				"use LP-solver for everything",							"4.1",	new Boolean(false),													"",																																"Solve the complete quantile using exclusively LP-solver."},
			{ INTEGER_TYPE,	QUANTILE_LP_SOLVER_ONLY_BOUND,				"bound using just LP-solver",							"4.1",	new Integer(20),													"",																																"Set the bound of the LP-solver to the specified value."},
			{ INTEGER_TYPE,	QUANTILE_PARALLEL_POOL_SIZE,				"number of tasks that will run in parallel",			"4.1",	new Integer(1),														"",																																"Set the number of maximal threads that can run in parallel as long as the calculation permits it. Take care to not slow the system by assigning to much parallel threads."},
			{ BOOLEAN_TYPE,	QUANTILE_POSITIVE_REWARDS_PARALLEL,			"parallel computation of positive reward states",		"4.1",	new Boolean(false),													"",																																"Positive reward-states can be calculated independently and therefore parallel."},
			{ BOOLEAN_TYPE,	QUANTILE_ZERO_REWARDS_PARALLEL,				"parallel computation of zero reward states",			"4.1",	new Boolean(false),													"",																																"Using the DAG of the zero reward-states allows to sketch the influence of one zero-reward component on another. This allows usage of parallelism."},
			{ INTEGER_TYPE,	QUANTILE_DEBUG_LEVEL,						"debug information",									"4.1",	new Integer(0),														"",																																"Set the debug level to enable / disable additional and helpful information for the quantile calculations. But be aware, a debug level greater or equals 4 is only recommended for small models, since this will blow up the output enormously."},
			{ BOOLEAN_TYPE,	QUANTILE_VERIFY_RESULT,						"verify result",										"4.1",	new Boolean(false),													"",																																"Verify (where possible) the results of the quantile calculations."},
			{ BOOLEAN_TYPE,	QUANTILE_USE_QUANTITATIVE,					"always quantitative algorithm",						"4.1",	new Boolean(false),													"",																																"Always use algorithms for quantitative probability thresholds."},
			{ BOOLEAN_TYPE,	QUANTILE_BACKEND_FOR_TIME_BOUND,			"use quantile backend for time bounded reachability",	"4.1",	new Boolean(false),													"",																																"When analysing a time-bounded reachability query, the backend for the computation of quantiles can be used for the computation."},
			{ BOOLEAN_TYPE,	QUANTILE_NAIVE_COMPUTATION,					"calculate quantiles the naive way",					"4.1",	new Boolean(false),													"",																																"A quantile query can be calculated straightforward by iteratively increasing the bound up to which the result will be searched."},
			{ BOOLEAN_TYPE,	QUANTILE_NAIVE_COMPUTATION_LINEAR,			"do linear stepping for naive quantile-calculation",	"4.1",	new Boolean(false),													"",																																"The stepping for the naive quantile computation is done exponentially. If a linear stepping is desired one needs to deactivate this flag."},
			{ DOUBLE_TYPE,	QUANTILE_CTMC_START_PRECISION,				"start epsilon for continuous quantile-computation",	"4.1",	new Double(0),														"",																																"Start value for precision of CTMC quantiles. Will be refined during computation if it is needed. Setting to 0 will deactivate the refinement and all calculations will be done using the standard-setting of PRISM." },
			{ DOUBLE_TYPE,	QUANTILE_CTMC_PRECISION,					"termination epsilon for continuous quantiles",			"4.1",	new Double(1.0E-6),													"",																																"Epsilon value to use for checking termination of quantile-calculation for models with continuous time." },
		}
	};
	
	public static final String[] oldPropertyNames =  {"simulator.apmcStrategy", "simulator.engine", "simulator.newPathAskDefault"};
	
	public DefaultSettingOwner[] optionOwners;
	private Hashtable<String,Setting> data;
	private boolean modified;
	
	private ArrayList<PrismSettingsListener> settingsListeners;
	
	/**
	 * Default constructor: set all options to default values. 
	 */
	public PrismSettings()
	{
		optionOwners = new DefaultSettingOwner[propertyOwnerIDs.length];
		
		int counter = 0;
		
		for(int i = 0; i < propertyOwnerIDs.length; i++)
		{
			optionOwners[i] = new DefaultSettingOwner(propertyOwnerNames[i], propertyOwnerIDs[i]);
			for(int j = 0; j < propertyData[i].length; j++)
			{
				counter++;
				Object[] setting = propertyData[i][j];
				String key = (String)setting[1];
				String display = (String)setting[2];
				String version = (String)setting[3];
				Object value = setting[4];
				String constraint = (String)setting[5];
				String comment = (String)setting[6];
				
				Setting set;
				
				if(setting[0].equals(STRING_TYPE))
				{
					set = new SingleLineStringSetting(display, (String)value, comment, optionOwners[i], false);
					set.setKey(key);
					set.setVersion(version);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(INTEGER_TYPE))
				{
					if(constraint.equals(""))
						set = new IntegerSetting(display, (Integer)value, comment, optionOwners[i], false);
					else
						set = new IntegerSetting(display, (Integer)value, comment, optionOwners[i], false, new RangeConstraint(constraint));
					set.setKey(key);
					set.setVersion(version);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(DOUBLE_TYPE))
				{
					//DO constraints for this double
					if(constraint.equals(""))
						set = new DoubleSetting(display, (Double)value, comment, optionOwners[i], false);
					else
						set = new DoubleSetting(display, (Double)value, comment, optionOwners[i], false, new RangeConstraint(constraint));
					set.setKey(key);
					set.setVersion(version);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(BOOLEAN_TYPE))
				{
					//DO constraints for this boolean
					set = new BooleanSetting(display, (Boolean)value, comment, optionOwners[i], false);
					set.setKey(key);
					set.setVersion(version);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(LONG_TYPE))
				{
					if(constraint.equals(""))
						set = new LongSetting(display, (Long)value, comment, optionOwners[i], false);
					else
						set = new LongSetting(display, (Long)value, comment, optionOwners[i], false, new RangeConstraint(constraint));
					set.setKey(key);
					set.setVersion(version);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(COLOUR_TYPE))
				{
					//DO constraints for this Color
					set = new ColorSetting(display, (Color)value, comment, optionOwners[i], false);
					set.setKey(key);
					set.setVersion(version);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(CHOICE_TYPE))
				{
					//DO constraints for this choice
					StringTokenizer tokens = new StringTokenizer(constraint, ",");
					String[] choices = new String[tokens.countTokens()];
					int k = 0;
					while(tokens.hasMoreTokens())
					{
						choices[k++] = tokens.nextToken();
					}
					set = new ChoiceSetting(display, choices, (String)value, comment, optionOwners[i], false);
					set.setKey(key);
					set.setVersion(version);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(FONT_COLOUR_TYPE))
				{
					//DO constraints for this FontColorPair
					set = new FontColorSetting(display, (FontColorPair)value, comment, optionOwners[i], false);
					set.setKey(key);
					set.setVersion(version);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(FILE_TYPE))
				{
					set = new FileSetting(display, (File)value, comment, optionOwners[i], false);
					set.setKey(key);
					set.setVersion(version);
					optionOwners[i].addSetting(set);
				}
				else
				{
					System.err.println("Fatal error when loading properties: unknown setting type "+setting[0]);
				}
			}
			optionOwners[i].addObserver(this);
		}
		
		modified = false;
		
		//populate a hash table with the keys
		populateHashTable(counter);
		settingsListeners = new ArrayList<PrismSettingsListener>();
	}
	
	/**
	 * Copy constructor. 
	 */
	public PrismSettings(PrismSettings settings)
	{
		// Create anew with default values
		// (that way, we get a fresh set of Setting objects)
		this();
		// Then, copy across options
		for (Map.Entry<String,Setting> e : settings.data.entrySet()) {
			try {
				set(e.getKey(), e.getValue().getValue());
			} catch (PrismException ex) {
				// Should never happen
				System.err.println(ex);
			}
		}
	}
	
	private void populateHashTable(int size)
	{
		data = new Hashtable<String,Setting>(size);
		
		for(int i = 0; i < optionOwners.length; i++)
		{
			for(int j = 0; j < optionOwners[i].getNumSettings(); j++)
			{
				data.put(optionOwners[i].getSetting(j).getKey(), optionOwners[i].getSetting(j));
			}
		}
	}
	
	private Setting settingFromHash(String key)
	{
		return data.get(key);
	}
	
	public void addSettingsListener(PrismSettingsListener listener)
	{
		settingsListeners.add(listener);
	}
	
	public void removeSettingsListener(PrismSettingsListener listener)
	{
		settingsListeners.remove(listener);
	}
	
	public void notifySettingsListeners()
	{
		for(int i = 0; i < settingsListeners.size(); i++)
		{
			PrismSettingsListener listener = settingsListeners.get(i);
			listener.notifySettings(this);
		}
	}
	
	public File getLocationForSettingsFile()
	{
		return new File(System.getProperty("user.home")+File.separator+".prism");
	}
	
	public synchronized void saveSettingsFile() throws PrismException
	{
		saveSettingsFile(getLocationForSettingsFile());
	}
	
	public synchronized void saveSettingsFile(File file) throws PrismException
	{
		try
		{
			FileWriter out = new FileWriter(file);
			
			out.write("# PRISM settings file\n");
			out.write("# (created by version "+Prism.getVersion()+")\n");
			
			for(int i = 0; i < optionOwners.length; i++)
			{
				out.write("\n");
				for(int j = 0; j < optionOwners[i].getNumSettings(); j++)
				{
					Setting set = optionOwners[i].getSetting(j);
					//write the key
					out.write(set.getKey()+"=");
					String value = set.toString();
					while(value.indexOf('\n') != -1) //is multiline string
					{
						out.write(value.substring(0,value.indexOf('\n')) + "\\");
						if(value.substring(value.indexOf('\n')).length() > 0)
							value = value.substring(value.indexOf('\n')+1);
						else
							value = "";
					}
					
					out.write(value+"\n");
					
				}
			}
			
			out.close();
			
		}
		catch(IOException e)
		{
			throw new PrismException("Error exporting properties file: "+e.getMessage());
		}
		
		modified = false;
	}
	
	public synchronized void loadSettingsFile() throws PrismException
	{
		loadSettingsFile(getLocationForSettingsFile());
	}
	
	public synchronized void loadSettingsFile(File file) throws PrismException
	{
		BufferedReader reader;
		String line = null;
		String key = "";
		String value;
		int equalsIndex;
		StringBuffer multiline = new StringBuffer();
		boolean inMulti = false;
		String version;
		boolean resaveNeeded = false;
		
		try
		{
			// Read first two lines to extract version
			version = null;
			reader = new BufferedReader(new FileReader(file));
			if (reader.ready()) line = reader.readLine();
			if (reader.ready()) line = reader.readLine();
			Matcher matcher =  Pattern.compile("# \\(created by version (.+)\\)").matcher(line);
			if (matcher.find()) version = matcher.group(1);
			reader.close();
			
			// Do we need to resave the file?
			// (i.e. is the version of the saved settings (a) old? (b) unparseable?)
			if (version == null) version = "0";
			if (Prism.compareVersions(version, Prism.getVersion()) == -1) resaveNeeded = true;
			
			// Read whole file
			reader = new BufferedReader(new FileReader(file));
			while(reader.ready())
			{
				line = reader.readLine();
				if(!inMulti)
				{
					if(line.startsWith("#")) continue; //ignore comments
					equalsIndex = line.indexOf('=');
					if(equalsIndex == -1) continue; //is not a valid property line
					else
					{
						key = line.substring(0, equalsIndex);
						value = (equalsIndex==line.length()-1)?"":line.substring(equalsIndex+1);
						if(value.endsWith("\\"))
						{
							inMulti = true;
							multiline = new StringBuffer(value.substring(0, value.length()-1)); // trim the \ off
						}
						else
						{
							Setting set = settingFromHash(key);
							if(set != null)
							{
								// If the version of the settings file is not newer than the "version" of the setting,
								// and we are re-saving the file, overwrite the setting with the default value 
								if (resaveNeeded && Prism.compareVersions(version, set.getVersion()) <= 0) continue;
								try
								{
									Object valObj = set.parseStringValue(value);
									set.setValue(valObj);
								}
								catch(SettingException ee)
								{
									System.err.println("Warning: PRISM setting \""+key+"\" has invalid value \"" + value + "\"");
								}
							}
							else
							{
								// Warning for unused options disabled for now
								// (it's a pain when you have lots of branches with lots of new options)
								if (false) {
									// Make sure this is not an old PRISM setting and if not print a warning
									boolean isOld = false;
									for (int i = 0; i < oldPropertyNames.length; i++) {
										if (oldPropertyNames[i].equals(key)) {
											isOld = true;
											break;
										}
									}
									if (!isOld)
										System.err.println("Warning: PRISM setting \"" + key + "\" is unknown.");
								}
							}
						}
					}
				}
				else
				{
					//In multiline
					if(line.endsWith("\\"))
					{
						multiline.append(line.substring(0, line.length()-1));
					}
					else
					{
						Setting set = settingFromHash(key);
						if(set != null)
						{
							// If the version of the settings file is not newer than the "version" of the setting,
							// and we are re-saving the file, overwrite the setting with the default value 
							if (resaveNeeded && Prism.compareVersions(version, set.getVersion()) <= 0) continue;
							try
							{
								Object valObj = set.parseStringValue(multiline.toString() + line);
								set.setValue(valObj);
							}
							catch(SettingException ee)
							{
								System.err.println("Warning: PRISM setting \""+key+"\" has invalid value \"" + multiline.toString() + line + "\"");
							}
						}
						else
						{
							System.err.println("Warning: PRISM setting \""+key+"\" is unknown.");
						}
						
						inMulti = false;
						multiline = null;
					}
				}
			}
		}
		catch(IOException e)
		{
			throw new PrismException("Error importing properties file: "+e.getMessage());
		}
		
		modified = false;
		
		// If necessary, resave the preferences file
		if (resaveNeeded) {
			try {
				saveSettingsFile();
			}
			catch (PrismException e) {
			}
		}
		
		notifySettingsListeners();
	}
	
	public synchronized void loadDefaults()
	{
		for(int i = 0; i < propertyOwnerIDs.length; i++)
		{
			
			for(int j = 0; j < propertyData[i].length; j++)
			{
				Setting set = settingFromHash(propertyData[i][j][1].toString());
			
				if(set != null)
				{
					try
					{
						set.setValue(propertyData[i][j][4]);
					}
					catch(SettingException e)
					{
						System.err.println("Warning: Error with default value for PRISM setting \""+set.getName()+"\"");
					}
				}
			}
		}
		
		notifySettingsListeners();
	}

	// HIDDEN OPTIONS
	
	// Export property automaton info?
	protected boolean exportPropAut = false;
	protected String exportPropAutType = "txt";
	protected String exportPropAutFilename = "da.txt";
	
	public void setExportPropAut(boolean b) throws PrismException
	{
		exportPropAut = b;
	}

	public void setExportPropAutType(String s) throws PrismException
	{
		exportPropAutType = s;
	}

	public void setExportPropAutFilename(String s) throws PrismException
	{
		exportPropAutFilename = s;
	}

	public boolean getExportPropAut()
	{
		return exportPropAut;
	}

	public String getExportPropAutType()
	{
		return exportPropAutType;
	}

	public String getExportPropAutFilename()
	{
		return exportPropAutFilename;
	}

	public Set<String> getReorderOptions()
	{
		HashSet<String> opts = new HashSet<String>();
		for (String option : getString(PRISM_REORDER_OPTIONS).split(",")) {
			opts.add(option);
		}
		return opts;
	}

	/**
	 * Set an option by parsing one or more command-line arguments.
	 * Reads the ith argument (assumed to be in the form "-switch")
	 * and also any subsequent arguments required as parameters.
	 * Return the index of the next argument to be read.
	 * @param args Full list of arguments
	 * @param i Index of first argument to read
	 */
	public synchronized int setFromCommandLineSwitch(String args[], int i) throws PrismException
	{
		String s;
		int j;
		double d;
		
		// Process string (remove - and extract any options) 
		Pair<String, Map<String, String>> pair = splitSwitch(args[i]);
		String sw = pair.first;
		Map<String, String> options = pair.second; 
		
		// Note: the order of these switches should match the -help output (just to help keep track of things).
		
		// ENGINES/METHODS:
		
		// Main model checking engine
		if (sw.equals("mtbdd") || sw.equals("m")) {
			set(PRISM_ENGINE, "MTBDD");
		}
		else if (sw.equals("sparse") || sw.equals("s")) {
			set(PRISM_ENGINE, "Sparse");
		}
		else if (sw.equals("hybrid") || sw.equals("h")) {
			set(PRISM_ENGINE, "Hybrid");
		}
		else if (sw.equals("explicit") || sw.equals("ex")) {
			set(PRISM_ENGINE, "Explicit");
		}
		// Exact model checking
		else if (sw.equals("exact")) {
			set(PRISM_EXACT_ENABLED, true);
		}
		// PTA model checking methods
		else if (sw.equals("ptamethod")) {
			if (i < args.length - 1) {
				s = args[++i];
				if (s.equals("digital"))
					set(PRISM_PTA_METHOD, "Digital clocks");
				else if (s.equals("games"))
					set(PRISM_PTA_METHOD, "Stochastic games");
				else if (s.equals("backwards") || s.equals("bw"))
					set(PRISM_PTA_METHOD, "Backwards reachability");
				else
					throw new PrismException("Unrecognised option for -" + sw + " switch (options are: digital, games)");
			} else {
				throw new PrismException("No parameter specified for -" + sw + " switch");
			}
		}
		// Transient methods
		else if (sw.equals("transientmethod")) {
			if (i < args.length - 1) {
				s = args[++i];
				if (s.equals("unif"))
					set(PRISM_TRANSIENT_METHOD, "Uniformisation");
				else if (s.equals("fau"))
					set(PRISM_TRANSIENT_METHOD, "Fast adaptive uniformisation");
				else
					throw new PrismException("Unrecognised option for -" + sw + " switch (options are: unif, fau)");
			} else {
				throw new PrismException("No parameter specified for -" + sw + " switch");
			}
		}

		// NUMERICAL SOLUTION OPTIONS:
		
		// Linear equation solver + MDP soln method
		else if (sw.equals("power") || sw.equals("pow") || sw.equals("pwr")) {
			set(PRISM_LIN_EQ_METHOD, "Power");
		} else if (sw.equals("jacobi") || sw.equals("jac")) {
			set(PRISM_LIN_EQ_METHOD, "Jacobi");
		} else if (sw.equals("gaussseidel") || sw.equals("gs")) {
			set(PRISM_LIN_EQ_METHOD, "Gauss-Seidel");
			set(PRISM_MDP_SOLN_METHOD, "Gauss-Seidel");
			set(PRISM_MDP_MULTI_SOLN_METHOD, "Gauss-Seidel");
		} else if (sw.equals("bgaussseidel") || sw.equals("bgs")) {
			set(PRISM_LIN_EQ_METHOD, "Backwards Gauss-Seidel");
		} else if (sw.equals("pgaussseidel") || sw.equals("pgs")) {
			set(PRISM_LIN_EQ_METHOD, "Pseudo-Gauss-Seidel");
		} else if (sw.equals("bpgaussseidel") || sw.equals("bpgs")) {
			set(PRISM_LIN_EQ_METHOD, "Backwards Pseudo-Gauss-Seidel");
		} else if (sw.equals("jor")) {
			set(PRISM_LIN_EQ_METHOD, "JOR");
		} else if (sw.equals("sor")) {
			set(PRISM_LIN_EQ_METHOD, "SOR");
		} else if (sw.equals("bsor")) {
			set(PRISM_LIN_EQ_METHOD, "Backwards SOR");
		} else if (sw.equals("psor")) {
			set(PRISM_LIN_EQ_METHOD, "Pseudo-SOR");
		} else if (sw.equals("bpsor")) {
			set(PRISM_LIN_EQ_METHOD, "Backwards Pseudo-SOR");
		} else if (sw.equals("valiter")) {
			set(PRISM_MDP_SOLN_METHOD, "Value iteration");
			set(PRISM_MDP_MULTI_SOLN_METHOD, "Value iteration");
		} else if (sw.equals("politer")) {
			set(PRISM_MDP_SOLN_METHOD, "Policy iteration");
		} else if (sw.equals("modpoliter")) {
			set(PRISM_MDP_SOLN_METHOD, "Modified policy iteration");
		} else if (sw.equals("linprog") || sw.equals("lp")) {
			set(PRISM_MDP_SOLN_METHOD, "Linear programming");
			set(PRISM_MDP_MULTI_SOLN_METHOD, "Linear programming");
		}
		// Linear equation solver over-relaxation parameter
		else if (sw.equals("omega")) {
			if (i < args.length - 1) {
				try {
					d = Double.parseDouble(args[++i]);
					set(PRISM_LIN_EQ_METHOD_PARAM, d);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		// Termination criterion (iterative methods)
		else if (sw.equals("relative") || sw.equals("rel")) {
			set(PRISM_TERM_CRIT, "Relative");
		}
		else if (sw.equals("absolute") || sw.equals("abs")) {
			set(PRISM_TERM_CRIT, "Absolute");
		}
		// Termination criterion parameter
		else if (sw.equals("epsilon") || sw.equals("e")) {
			if (i < args.length - 1) {
				try {
					d = Double.parseDouble(args[++i]);
					if (d < 0)
						throw new NumberFormatException("");
					set(PRISM_TERM_CRIT_PARAM, d);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		// Max iters
		else if (sw.equals("maxiters")) {
			if (i < args.length - 1) {
				try {
					j = Integer.parseInt(args[++i]);
					if (j < 0)
						throw new NumberFormatException("");
					set(PRISM_MAX_ITERS, j);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		// export iterations
		else if (sw.equals("exportiterations")) {
			set(PRISM_EXPORT_ITERATIONS, true);
		}
		
		// MODEL CHECKING OPTIONS:
		
		// Precomputation algs off
		else if (sw.equals("nopre")) {
			set(PRISM_PRECOMPUTATION, false);
		}
		else if (sw.equals("noprob0")) {
			set(PRISM_PROB0, false);
		}
		else if (sw.equals("noprob1")) {
			set(PRISM_PROB1, false);
		}
		// Use predecessor relation? (e.g. for precomputation)
		else if (sw.equals("noprerel")) {
			set(PRISM_PRE_REL, false);
		}
		// Fix deadlocks on/off
		else if (sw.equals("fixdl")) {
			set(PRISM_FIX_DEADLOCKS, true);
		}
		else if (sw.equals("nofixdl")) {
			set(PRISM_FIX_DEADLOCKS, false);
		}
		// Fairness on/off
		else if (sw.equals("fair")) {
			set(PRISM_FAIRNESS, true);
		}
		else if (sw.equals("nofair")) {
			set(PRISM_FAIRNESS, false);
		}
		// Prob/rate checks off
		else if (sw.equals("noprobchecks")) {
			set(PRISM_DO_PROB_CHECKS, false);
		}
		// Sum round-off threshold
		else if (sw.equals("sumroundoff")) {
			if (i < args.length - 1) {
				try {
					d = Double.parseDouble(args[++i]);
					if (d < 0)
						throw new NumberFormatException("");
					set(PRISM_SUM_ROUND_OFF, d);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		// No steady-state detection
		else if (sw.equals("nossdetect")) {
			set(PRISM_DO_SS_DETECTION, false);
		}
		// SCC computation algorithm (symbolic)
		else if (sw.equals("sccmethod") || sw.equals("bsccmethod")) {
			if (i < args.length - 1) {
				s = args[++i];
				if (s.equals("xiebeerel"))
					set(PRISM_SCC_METHOD, "Xie-Beerel");
				else if (s.equals("lockstep"))
					set(PRISM_SCC_METHOD, "Lockstep");
				else if (s.equals("sccfind"))
					set(PRISM_SCC_METHOD, "SCC-Find");
				else
					throw new PrismException("Unrecognised option for -" + sw + " switch (options are: xiebeerel, lockstep, sccfind)");
			} else {
				throw new PrismException("No parameter specified for -" + sw + " switch");
			}
		}
		// SCC computation algorithm (explicit)
		else if (sw.equals("sccmethodex") || sw.equals("bsccmethodex")) {
			if (i < args.length - 1) {
				s = args[++i];
				if (s.equals("tarjanrec"))
					set(PRISM_SCC_METHOD_EXPLICIT, "Tarjan-Recursive");
				else if (s.equals("tarjanstack"))
					set(PRISM_SCC_METHOD_EXPLICIT, "Tarjan-Stack");
				else
					throw new PrismException("Unrecognised option for -" + sw + " switch (options are: tarjanrec, tarjanstack)");
			} else {
				throw new PrismException("No parameter specified for -" + sw + " switch");
			}
		}
		// SCC computation algorithm
		else if (sw.equals("ecmethod")) {
			if (i < args.length - 1) {
				s = args[++i];
				if (s.equals("default"))
					set(PRISM_EC_METHOD, "Default");
				else if (s.equals("onthefly"))
					set(PRISM_EC_METHOD, "On-The-Fly");
				else
					throw new PrismException("Unrecognised option for -" + sw + " switch (options are: default,onthefly)");
			} else {
				throw new PrismException("No parameter specified for -" + sw + " switch");
			}
		}
		// Enable symmetry reduction
		else if (sw.equals("symm")) {
			if (i < args.length - 2) {
				set(PRISM_SYMM_RED_PARAMS, args[++i] + " " + args[++i]);
			} else {
				throw new PrismException("-symm switch requires two parameters (num. modules before/after symmetric ones)");
			}
		}
		// Abstraction-refinement engine options string (append if already partially specified)
		else if (sw.equals("aroptions")) {
			if (i < args.length - 1) {
				String arOptions = getString(PRISM_AR_OPTIONS);
				if ("".equals(arOptions))
					arOptions = args[++i].trim();
				else
					arOptions += "," + args[++i].trim();
				set(PRISM_AR_OPTIONS, arOptions);
			} else {
				throw new PrismException("No parameter specified for -" + sw + " switch");
			}
		}
		// Handle all path formulas via automata constructions
		else if (sw.equals("pathviaautomata")) {
			set(PRISM_PATH_VIA_AUTOMATA, true);
		}
		// Don't simplify deterministic automata
		else if (sw.equals("nodasimplify")) {
			set(PRISM_NO_DA_SIMPLIFY, true);
		}
		else if (sw.equals("boundsviacounters")) {
			set(PRISM_BOUNDS_VIA_COUNTERS, true);
		}

		
		// MULTI-OBJECTIVE MODEL CHECKING OPTIONS:
		
		// Max different corner points that will be generated when performing
		// target driven multi-obj verification.
		else if (sw.equals("multimaxpoints")) {
			if (i < args.length - 1) {
				try {
					j = Integer.parseInt(args[++i]);
					if (j < 0)
						throw new NumberFormatException("");
					set(PRISM_MULTI_MAX_POINTS, j);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		// Threshold for approximate Pareto curve generation
		else if (sw.equals("paretoepsilon")) {
			if (i < args.length - 1) {
				try {
					d = Double.parseDouble(args[++i]);
					if (d < 0)
						throw new PrismException("Value for -" + sw + " switch must be non-negative");
					set(PRISM_PARETO_EPSILON, d);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("exportpareto")) {
			if (i < args.length - 1) {
				set(PRISM_EXPORT_PARETO_FILENAME, args[++i]);
			} else {
				throw new PrismException("No file specified for -" + sw + " switch");
			}
		}
		
		// OUTPUT OPTIONS:
		
		// Verbosity
		else if (sw.equals("verbose") || sw.equals("v")) {
			set(PRISM_VERBOSE, true);
		}
		// Extra dd info on
		else if (sw.equals("extraddinfo")) {
			set(PRISM_EXTRA_DD_INFO, true);
		}
		// Extra reach info on
		else if (sw.equals("extrareachinfo")) {
			set(PRISM_EXTRA_REACH_INFO, true);
		}
		
		// SPARSE/HYBRID/MTBDD OPTIONS:
		
		// Turn off compact option for sparse matrix storage
		else if (sw.equals("nocompact")) {
			set(PRISM_COMPACT, false);
		}
		// Sparse bits info
		else if (sw.equals("sbl")) {
			if (i < args.length - 1) {
				try {
					j = Integer.parseInt(args[++i]);
					if (j < -1)
						throw new NumberFormatException();
					set(PRISM_NUM_SB_LEVELS, j);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("sbmax")) {
			if (i < args.length - 1) {
				try {
					j = Integer.parseInt(args[++i]);
					if (j < 0)
						throw new NumberFormatException();
					set(PRISM_SB_MAX_MEM, j);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		// Hybrid SOR info
		else if (sw.equals("sorl") || sw.equals("gsl")) {
			if (i < args.length - 1) {
				try {
					j = Integer.parseInt(args[++i]);
					if (j < -1)
						throw new NumberFormatException();
					set(PRISM_NUM_SOR_LEVELS, j);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("sormax") || sw.equals("gsmax")) {
			if (i < args.length - 1) {
				try {
					j = Integer.parseInt(args[++i]);
					if (j < 0)
						throw new NumberFormatException();
					set(PRISM_SOR_MAX_MEM, j);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("explodebits")) {
			set(PRISM_EXPLODE_BITS, true);
		}
		else if (sw.equals("globalizevariables")) {
			set(PRISM_GLOBALIZE_VARIABLES, true);
		}
		// CUDD settings
		else if (sw.equals("cuddmaxmem")) {
			if (i < args.length - 1) {
				set(PRISM_CUDD_MAX_MEM, args[++i]);
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("cuddepsilon")) {
			if (i < args.length - 1) {
				try {
					d = Double.parseDouble(args[++i]);
					if (d < 0)
						throw new NumberFormatException("");
					set(PRISM_CUDD_EPSILON, d);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		} else if (sw.equals("ddextrastatevars")) {
			if (i < args.length - 1) {
				try {
					int v = Integer.parseInt(args[++i]);
					if (v < 0)
						throw new NumberFormatException("");
					set(PRISM_DD_EXTRA_STATE_VARS, v);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		} else if (sw.equals("ddextraactionvars")) {
			if (i < args.length - 1) {
				try {
					int v = Integer.parseInt(args[++i]);
					if (v < 0)
						throw new NumberFormatException("");
					set(PRISM_DD_EXTRA_ACTION_VARS, v);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("reordermaxgrowth")) {
				if (i < args.length - 1) {
					try {
						d = Double.parseDouble(args[++i]);
						if (d < 0)
							throw new NumberFormatException("");
						set(PRISM_CUDD_MAX_GROWTH, d);
					} catch (NumberFormatException e) {
						throw new PrismException("Invalid value for -" + sw + " switch");
					}
				} else {
					throw new PrismException("No value specified for -" + sw + " switch");
				}
		} else if (sw.equals("reorder")) {
			set(PRISM_DO_REORDER, true);
		} else if (sw.equals("reorderoptions")) {
			if (i < args.length - 1) {
				String reorderArg = args[++i];
				String[] reorderOptions = reorderArg.split(",");
				for (String reorderOption : reorderOptions) {
					switch (reorderOption) {
					case "norebuild":
					case "beforereach":
					case "noconstraints":
					case "optimisetrans":
					case "converge":
						break;
					default:
						throw new PrismException("Invalid option \""+reorderOption+"\" for -" + sw + " switch");
					}
				}
				set(PRISM_REORDER_OPTIONS, reorderArg);
			} else {
				throw new PrismException("No options specified for -" + sw + " switch");
			}
		}
		
		// ADVERSARIES/COUNTEREXAMPLES:
		
		// Export adversary to file
		else if (sw.equals("exportadv")) {
			if (i < args.length - 1) {
				set(PRISM_EXPORT_ADV, "DTMC");
				set(PRISM_EXPORT_ADV_FILENAME, args[++i]);
			} else {
				throw new PrismException("No file specified for -" + sw + " switch");
			}
		}
		// Export adversary to file, as an MDP
		else if (sw.equals("exportadvmdp")) {
			if (i < args.length - 1) {
				set(PRISM_EXPORT_ADV, "MDP");
				set(PRISM_EXPORT_ADV_FILENAME, args[++i]);
			} else {
				throw new PrismException("No file specified for -" + sw + " switch");
			}
		}
		
		// LTL2DA TOOLS
		
		else if (sw.equals("ltl2datool")) {
			if (i < args.length - 1) {
				String filename = args[++i];
				set(PRISM_LTL2DA_TOOL, filename);
			} else {
				throw new PrismException("The -" + sw + " switch requires one argument (path to the executable)");
			}
		}
		else if (sw.equals("ltl2dasyntax")) {
			if (i < args.length - 1) {
				String syntax = args[++i];
				switch (syntax) {
				case "lbt":
					set(PRISM_LTL2DA_SYNTAX, "LBT");
					break;
				case "spin":
					set(PRISM_LTL2DA_SYNTAX, "Spin");
					break;
				case "spot":
					set(PRISM_LTL2DA_SYNTAX, "Spot");
				case "rabinizer":
					set(PRISM_LTL2DA_SYNTAX, "Rabinizer");
					break;
				default:
					throw new PrismException("Unrecognised option for -" + sw + " switch (options are: lbt, spin, spot, rabinizer)");
				}
			} else {
				throw new PrismException("The -" + sw + " switch requires one argument (options are: lbt, spin, spot, rabinizer)");
			}
		}
		else if (sw.equals("quantileBigStep")) {
			// TODO(JK): move to quantile section
			set(QUANTILE_USE_BIGSTEP, true);
		}
		else if (sw.equals("quantileTACAS16")) {
			// TODO(JK): move to quantile section
			set(QUANTILE_USE_TACAS16, true);
		}

		else if (sw.equals("allowltl2wdba")) {
			set(PRISM_ALLOW_LTL2WDBA, true);
		}

		// DEBUGGING / SANITY CHECKS
		else if (sw.equals("ddsanity")) {
			set(PRISM_JDD_SANITY_CHECKS, true);
		}

		// PARAMETRIC MODEL CHECKING:
		
		else if (sw.equals("param")) {
			set(PRISM_PARAM_ENABLED, true);
		}
		else if (sw.equals("paramprecision")) {
			if (i < args.length - 1) {
				set(PRISM_PARAM_PRECISION, args[++i]);
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("paramsplit")) {
			if (i < args.length - 1) {
				s = args[++i];
				if (s.equals("longest"))
					set(PRISM_PARAM_SPLIT, "Longest");
				else if (s.equals("all"))
					set(PRISM_PARAM_SPLIT, "All");
				else
					throw new PrismException("Unrecognised option for -" + sw + " switch (options are: longest, all)");
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("parambisim")) {
			if (i < args.length - 1) {
				s = args[++i];
				if (s.equals("strong"))
					set(PRISM_PARAM_BISIM, "Strong");
				else if (s.equals("weak"))
					set(PRISM_PARAM_BISIM, "Weak");
				else if (s.equals("none"))
					set(PRISM_PARAM_BISIM, "None");
				else
					throw new PrismException("Unrecognised option for -" + sw + " switch (options are: strong, weak, none)");
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("paramfunction")) {
			if (i < args.length - 1) {
				s = args[++i];
				if (s.equals("jascached"))
					set(PRISM_PARAM_FUNCTION, "JAS-cached");
				else if (s.equals("jas"))
					set(PRISM_PARAM_FUNCTION, "JAS");
				else if (s.equals("dag"))
					set(PRISM_PARAM_FUNCTION, "DAG");
				else
					throw new PrismException("Unrecognised option for -" + sw + " switch (options are: jascached, jas, dag)");
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("paramelimorder")) {
			if (i < args.length - 1) {
				s = args[++i];
				if (s.equals("arb"))
					set(PRISM_PARAM_ELIM_ORDER, "Arbitrary");
				else if (s.equals("fw"))
					set(PRISM_PARAM_ELIM_ORDER, "Forward");
				else if (s.equals("fwrev"))
					set(PRISM_PARAM_ELIM_ORDER, "Forward-reversed");
				else if (s.equals("bw"))
					set(PRISM_PARAM_ELIM_ORDER, "Backward");
				else if (s.equals("bwrev"))
					set(PRISM_PARAM_ELIM_ORDER, "Backward-reversed");
				else if (s.equals("rand"))
					set(PRISM_PARAM_ELIM_ORDER, "Random");
				else
					throw new PrismException("Unrecognised option for -" + sw + " switch (options are: arb,fw,fwrev,bw,bwrev,rand)");
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("paramrandompoints")) {
			try {
				j = Integer.parseInt(args[++i]);
				if (j < 0)
					throw new NumberFormatException();
				set(PRISM_PARAM_RANDOM_POINTS, j);
			} catch (NumberFormatException e) {
				throw new PrismException("Invalid value for -" + sw + " switch");
			}
		}
		else if (sw.equals("paramsubsumeregions")) {
			boolean b = Boolean.parseBoolean(args[++i]);
			set(PRISM_PARAM_SUBSUME_REGIONS, b);
		}
		else if (sw.equals("paramdagmaxerror")) {
			try {
				d = Double.parseDouble(args[++i]);
				if (d < 0)
					throw new NumberFormatException();
				set(PRISM_PARAM_DAG_MAX_ERROR, d);
			} catch (NumberFormatException e) {
				throw new PrismException("Invalid value for -" + sw + " switch");
			}
		}
		
		// FAST ADAPTIVE UNIFORMISATION
		
		// Epsilon for fast adaptive uniformisation
		else if (sw.equals("fauepsilon")) {
			if (i < args.length - 1) {
				try {
					d = Double.parseDouble(args[++i]);
					if (d < 0)
						throw new NumberFormatException("");
					set(PRISM_FAU_EPSILON, d);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		// Delta for fast adaptive uniformisation
		else if (sw.equals("faudelta")) {
			if (i < args.length - 1) {
				try {
					d = Double.parseDouble(args[++i]);
					if (d < 0)
						throw new NumberFormatException("");
					set(PRISM_FAU_DELTA, d);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		// Array threshold for fast adaptive uniformisation
		else if (sw.equals("fauarraythreshold")) {
			if (i < args.length - 1) {
				try {
					j = Integer.parseInt(args[++i]);
					if (j < 0)
						throw new NumberFormatException("");
					set(PRISM_FAU_ARRAYTHRESHOLD, j);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}			
		}
		// Number of intervals for fast adaptive uniformisation
		else if (sw.equals("fauintervals")) {
			if (i < args.length - 1) {
				try {
					j = Integer.parseInt(args[++i]);
					if (j < 0)
						throw new NumberFormatException("");
					set(PRISM_FAU_INTERVALS, j);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("fauinitival")) {
			if (i < args.length - 1) {
				try {
					d = Double.parseDouble(args[++i]);
					if (d < 0.0)
						throw new NumberFormatException("");
					set(PRISM_FAU_INITIVAL, d);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}

		// HIDDEN OPTIONS
		
		// export property automaton to file (hidden option)
		else if (sw.equals("exportpropaut")) {
			if (i < args.length - 1) {
				setExportPropAut(true);
				setExportPropAutFilename(args[++i]);
				setExportPropAutType("txt");  // default
				for (Map.Entry<String, String> option : options.entrySet()) {
				    if (option.getKey().equals("txt")) {
						setExportPropAutType("txt");
				    } else if (option.getKey().equals("dot")) {
						setExportPropAutType("dot");
				    } else if (option.getKey().equals("hoa")) {
						setExportPropAutType("hoa");
				    } else {
				    		throw new PrismException("Unknown option \"" + option.getKey() + "\" for -" + sw + " switch"); 
				    }
				}
			} else {
				throw new PrismException("No file specified for -" + sw + " switch");
			}
		}

		// Quantile options
		else if (sw.equals("zeroRewardSccMethod")){
			if (i < args.length-1){
				set(QUANTILE_SCC_METHOD, args[++i]);
			} else
				throw new PrismException("No correct method specified for -" + sw + " switch");
		}
		else if (sw.equals("valuesStorage")){
			if (i < args.length-1){
				set(QUANTILE_VALUES_STORAGE, args[++i]);
			} else
				throw new PrismException("No correct storage specified for -" + sw + " switch");
		}
		else if (sw.equals("setImplementation")){
			if (i < args.length-1){
				set(QUANTILE_SET_FACTORY, args[++i]);
			} else
				throw new PrismException("No correct implementation specified for -" + sw + " switch");
		}
		else if (sw.equals("adaptiveSetThreshold")){
			if (i < args.length-1){
				try {
					int threshold = Integer.parseInt(args[++i]);
					if (threshold < 1)
						throw new PrismException("The threshold for the AdaptiveSet must be greater 0");
					set(QUANTILE_ADAPTIVE_SET_THRESHOLD, threshold);
				} catch (NumberFormatException nfe){
					throw new PrismException("Invalid threshold for switch " + sw);
				}
			} else
				throw new PrismException("No threshold specified for -" + sw + " switch");
		}
		else if (sw.equals("lpSolverOnly"))
			if (i < args.length-1){
				set(QUANTILE_USE_LP_SOLVER_ONLY, true);
				try {
					set(QUANTILE_LP_SOLVER_ONLY_BOUND, Integer.parseInt(args[++i]));
				} catch (NumberFormatException nfe){
					throw new PrismException("Invalid bound for -" + sw + " switch");
				}
			} else
				throw new PrismException("No bound specified for -" + sw + " switch");
		else if (sw.equals("multiStateMethod")){
			if (i < args.length-1){
				set(QUANTILE_MULTI_STATE_METHOD, args[++i]);
			} else
				throw new PrismException("No correct multi-state sets method specified for -" + sw + " switch");
		}
		else if (sw.equals("noTryAndSet"))
			set(QUANTILE_USE_ZERO_REWARD_TRY_AND_SET, false);
		else if (sw.equals("parallelPoolSize")){
			if (i < args.length-1){
				try {
					int poolSize = Integer.parseInt(args[++i]);
					if (poolSize < 1)
						throw new PrismException("The pool size for parallel tasks must be at least 1");
					set(QUANTILE_PARALLEL_POOL_SIZE, poolSize);
				} catch (NumberFormatException nfe){
					throw new PrismException("Invalid pool size for switch " + sw);
				}
			} else
				throw new PrismException("No valid size specified for -" + sw + " switch");
		}
		else if (sw.equals("parallelForPositiveRewards"))
			set(QUANTILE_POSITIVE_REWARDS_PARALLEL, true);
		else if (sw.equals("parallelForZeroRewards"))
			set(QUANTILE_ZERO_REWARDS_PARALLEL, true);
		else if (sw.equals("debugQuantile")){
			if (i < args.length-1){
				try {
					set(QUANTILE_DEBUG_LEVEL, Integer.parseInt(args[++i]));
				} catch (NumberFormatException nfe){
					throw new PrismException("Invalid debug level for switch " + sw);
				}
			} else
				throw new PrismException("No debug level specified for -" + sw + " switch");
		}
		else if (sw.equals("quantileverify"))
			set(QUANTILE_VERIFY_RESULT, true);
		else if (sw.equals("quantilequantitative"))
			set(QUANTILE_USE_QUANTITATIVE, true);
		else if (sw.equals("quantileBackendForTimeBound"))
			set(QUANTILE_BACKEND_FOR_TIME_BOUND, true);
		else if (sw.equals("quantileNaive"))
			set(QUANTILE_NAIVE_COMPUTATION, true);
		else if (sw.equals("quantileNaiveLinear"))
			set(QUANTILE_NAIVE_COMPUTATION_LINEAR, true);
		else if (sw.equals("quantileCtmcPrecision")){
			if (i < args.length - 1) {
				try {
					d = Double.parseDouble(args[++i]);
					if (d < 0)
						throw new NumberFormatException("");
					set(QUANTILE_CTMC_PRECISION, d);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		else if (sw.equals("quantileCtmcStartPrecision")){
			if (i < args.length - 1) {
				try {
					d = Double.parseDouble(args[++i]);
					if (d < 0)
						throw new NumberFormatException("");
					set(QUANTILE_CTMC_START_PRECISION, d);
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid value for -" + sw + " switch");
				}
			} else {
				throw new PrismException("No value specified for -" + sw + " switch");
			}
		}
		// unknown switch - error
		else {
			throw new PrismException("Invalid switch -" + sw + " (type \"prism -help\" for full list)");
		}
		
		return i + 1;
	}
	
	/**
	 * Split a switch of the form -switch:options into parts.
	 * The latter can be empty, in which case the : is optional.
	 * When present, the options is a comma-separated list of "option" or "option=value" items.
	 * The switch itself can be prefixed with either 1 or 2 hyphens.
	 * 
	 * @return a pair containing the switch name and a mapping from options to values.  
	 */
	private static Pair<String, Map<String, String>> splitSwitch(String sw) throws PrismException
	{
		// Remove "-"
		sw = sw.substring(1);
		// Remove optional second "-" (i.e. we allow switches of the form --sw too)
		if (sw.charAt(0) == '-')
			sw = sw.substring(1);
		// Extract options, if present
		int i = sw.indexOf(':');
		Map<String,String> map = new HashMap<String, String>();
		if (i != -1) {
			String optionsString = sw.substring(i + 1);
			sw = sw.substring(0, i);
			String options[] = optionsString.split(",");
			for (String option : options) {
				int j = option.indexOf("=");
				if (j == -1) {
					map.put(option, null);
				} else {
					map.put(option.substring(0,j), option.substring(j+1));
				}
			}
		}
		return new Pair<String, Map<String,String>>(sw, map);
	}

	/**
	 * Print a fragment of the -help message,
	 * i.e. a list of the command-line switches handled by this class.
	 */
	public static void printHelp(PrismLog mainLog)
	{
		mainLog.println();
		mainLog.println("ENGINES/METHODS:");
		mainLog.println("-mtbdd (or -m) ................. Use the MTBDD engine");
		mainLog.println("-sparse (or -s) ................ Use the Sparse engine");
		mainLog.println("-hybrid (or -h) ................ Use the Hybrid engine [default]");
		mainLog.println("-explicit (or -ex) ............. Use the explicit engine");
		mainLog.println("-exact ......................... Perform exact (arbitrary precision) model checking");
		mainLog.println("-ptamethod <name> .............. Specify PTA engine (games, digital, backwards) [default: games]");
		mainLog.println("-transientmethod <name> ........ CTMC transient analysis methof (unif, fau) [default: unif]");
		mainLog.println();
		mainLog.println("SOLUTION METHODS (LINEAR EQUATIONS):");
		mainLog.println("-power (or -pow, -pwr) ......... Use the Power method for numerical computation");
		mainLog.println("-jacobi (or -jac) .............. Use Jacobi for numerical computation [default]");
		mainLog.println("-gaussseidel (or -gs) .......... Use Gauss-Seidel for numerical computation");
		mainLog.println("-bgaussseidel (or -bgs) ........ Use Backwards Gauss-Seidel for numerical computation");
		mainLog.println("-pgaussseidel (or -pgs) ........ Use Pseudo Gauss-Seidel for numerical computation");
		mainLog.println("-bpgaussseidel (or -bpgs) ...... Use Backwards Pseudo Gauss-Seidel for numerical computation");
		mainLog.println("-jor ........................... Use JOR for numerical computation");
		mainLog.println("-sor ........................... Use SOR for numerical computation");
		mainLog.println("-bsor .......................... Use Backwards SOR for numerical computation");
		mainLog.println("-psor .......................... Use Pseudo SOR for numerical computation");
		mainLog.println("-bpsor ......................... Use Backwards Pseudo SOR for numerical computation");
		mainLog.println("-omega <x> ..................... Set over-relaxation parameter (for JOR/SOR/...) [default: 0.9]");
		mainLog.println();
		mainLog.println("SOLUTION METHODS (MDPS):");
		mainLog.println("-valiter ....................... Use value iteration for solving MDPs [default]");
		mainLog.println("-gaussseidel (or -gs) .......... Use Gauss-Seidel value iteration for solving MDPs");
		mainLog.println("-politer ....................... Use policy iteration for solving MDPs");
		mainLog.println("-modpoliter .................... Use modified policy iteration for solving MDPs");
		mainLog.println();
		mainLog.println("SOLUTION METHOD SETTINGS");
		mainLog.println("-relative (or -rel) ............ Use relative error for detecting convergence [default]");
		mainLog.println("-absolute (or -abs) ............ Use absolute error for detecting convergence");
		mainLog.println("-epsilon <x> (or -e <x>) ....... Set value of epsilon (for convergence check) [default: 1e-6]");
		mainLog.println("-maxiters <n> .................. Set max number of iterations [default: 10000]");
		mainLog.println();
		mainLog.println("MODEL CHECKING OPTIONS:");
		mainLog.println("-nopre ......................... Skip precomputation algorithms (where optional)");
		mainLog.println("-noprob0 ....................... Skip precomputation algorithm Prob0 (where optional)");
		mainLog.println("-noprob1 ....................... Skip precomputation algorithm Prob1 (where optional)");
		mainLog.println("-noprerel ...................... Do not pre-compute/use predecessor relation, e.g. for precomputation");
		mainLog.println("-fair .......................... Use fairness (for model checking of MDPs)");
		mainLog.println("-nofair ........................ Don't use fairness (for model checking of MDPs) [default]");
		mainLog.println("-fixdl ......................... Automatically put self-loops in deadlock states [default]");
		mainLog.println("-nofixdl ....................... Do not automatically put self-loops in deadlock states");
		mainLog.println("-noprobchecks .................. Disable checks on model probabilities/rates");
		mainLog.println("-sumroundoff <x> ............... Set probability sum threshold [default: 1-e5]");
		mainLog.println("-zerorewardcheck ............... Check for absence of zero-reward loops");
		mainLog.println("-nossdetect .................... Disable steady-state detection for CTMC transient computations");
		mainLog.println("-sccmethod <name> .............. Specify (symbolic) SCC computation method (xiebeerel, lockstep, sccfind)");
		mainLog.println("-symm <string> ................. Symmetry reduction options string");
		mainLog.println("-aroptions <string> ............ Abstraction-refinement engine options string");
		mainLog.println("-pathviaautomata ............... Handle all path formulas via automata constructions");
		mainLog.println("-nodasimplify .................. Do not attempt to simplify deterministic automata, acceptance conditions");
		mainLog.println("-exportadv <file> .............. Export an adversary from MDP model checking (as a DTMC)");
		mainLog.println("-exportadvmdp <file> ........... Export an adversary from MDP model checking (as an MDP)");
		mainLog.println("-ltl2datool <exec> ............. Run executable <exec> to convert LTL formulas to deterministic automata");
		mainLog.println("-ltl2dasyntax <x> .............. Specify output format for -ltl2datool switch (lbt, spin, spot, rabinizer)");
		mainLog.println("-allowltl2wdba ................. Activate LTL-to-weak-deterministic Büchi translation for obligation LTL formulas");
		mainLog.println("-reorder ....................... Perform symbolic reordering after building model");
		mainLog.println("-reorderoptions <x,y,z> ........ Reorder options: norebuild beforereach noconstraints optimisetrans converge");
		mainLog.println("-reordermaxgrowth <x> .......... Max growth parameter for CUDD reordering (double value x, default 1.2 = 120%");
		mainLog.println("-explodebits ................... Model file transformation: Convert variables to single bits with views");
		mainLog.println("-globalizevariables ............ Model file transformation: Make all variables global");
		mainLog.println("-boundsviacounters ............. Handle all bounds via counter transformation");
		
		mainLog.println();
		mainLog.println("MULTI-OBJECTIVE MODEL CHECKING:");
		mainLog.println("-linprog (or -lp) .............. Use linear programming for multi-objective model checking");
		mainLog.println("-multimaxpoints <n> ............ Maximal number of corner points for (valiter-based) multi-objective");
		mainLog.println("-paretoepsilon <x> ............. Threshold for Pareto curve approximation");
		mainLog.println("-exportpareto <file> ........... When computing Pareto curves, export points to a file");
		mainLog.println();
		mainLog.println("OUTPUT OPTIONS:");
		mainLog.println("-verbose (or -v) ............... Verbose mode: print out state lists and probability vectors");
		mainLog.println("-extraddinfo ................... Display extra info about some (MT)BDDs");
		mainLog.println("-extrareachinfo ................ Display extra info about progress of reachability");
		mainLog.println();
		mainLog.println("SPARSE/HYBRID/MTBDD OPTIONS:");
		mainLog.println("-nocompact ..................... Switch off \"compact\" sparse storage schemes");
		mainLog.println("-sbl <n> ....................... Set number of levels (for hybrid engine) [default: -1]");
		mainLog.println("-sbmax <n> ..................... Set memory limit (KB) (for hybrid engine) [default: 1024]");
		mainLog.println("-gsl <n> (or sorl <n>) ......... Set number of levels for hybrid GS/SOR [default: -1]");
		mainLog.println("-gsmax <n> (or sormax <n>) ..... Set memory limit (KB) for hybrid GS/SOR [default: 1024]");
		mainLog.println("-cuddmaxmem <n> ................ Set max memory for CUDD package, e.g. 125k, 50m, 4g [default: 1g]");
		mainLog.println("-cuddepsilon <x> ............... Set epsilon value for CUDD package [default: 1e-15]");
		mainLog.println("-ddsanity ...................... Enable internal sanity checks (causes slow-down)");
		mainLog.println("-ddextrastatevars <n> .......... Set the number of preallocated state vars [default: 20]");
		mainLog.println("-ddextraactionvars <n> ......... Set the number of preallocated action vars [default: 20]");
		mainLog.println();
		mainLog.println("PARAMETRIC MODEL CHECKING OPTIONS:");
		mainLog.println("-param <vals> .................. Do parametric model checking with parameters (and ranges) <vals>");
		mainLog.println("-paramprecision <x> ............ Set max undecided region for parameter synthesis [default: 5/100]");
		mainLog.println("-paramsplit <name> ............. Set method to split parameter regions (longest,all) [default: longest]");
		mainLog.println("-parambisim <name> ............. Set bisimulation minimisation for parameter synthesis (weak,strong,none) [default: weak]");
		mainLog.println("-paramfunction <name> .......... Set function representation for parameter synthesis (jascached,jas) [default: jascached]");
		mainLog.println("-paramelimorder <name> ......... Set elimination order for parameter synthesis (arb,fw,fwrev,bw,bwrev,rand) [default: bw]");
		mainLog.println("-paramrandompoints <n> ......... Set number of random points to evaluate per region [default: 5]");
		mainLog.println("-paramsubsumeregions <b> ....... Subsume adjacent regions during analysis [default: true]");
		mainLog.println("-paramdagmaxerror <b> .......... Maximal error probability allowed for DAG function representation [default: 1E-100]");
		mainLog.println();
		mainLog.println("FAST ADAPTIVE UNIFORMISATION (FAU) OPTIONS:");
		mainLog.println("-fauepsilon <x> ................ Set probability threshold of birth process in FAU [default: 1e-6]");
		mainLog.println("-faudelta <x> .................. Set probability threshold for irrelevant states in FAU [default: 1e-12]");
		mainLog.println("-fauarraythreshold <x> ......... Set threshold when to switch to sparse matrix in FAU [default: 100]");
		mainLog.println("-fauintervals <x> .............. Set number of intervals to divide time intervals into for FAU [default: 1]");
		mainLog.println("-fauinitival <x> ............... Set length of additional initial time interval for FAU [default: 1.0]");
		mainLog.println();
		mainLog.println("QUANTILE OPTIONS:");
		mainLog.println("-zeroRewardSccMethod <name> .... Specify the method to calculate the topological sorting of zero-reward components (" + Arrays.toString(TopologicalSorting.QuantileSccMethod.values()) + ") [default: " + TopologicalSorting.QuantileSccMethod.TARJAN_ITERATIVE + "]");
		mainLog.println("-valuesStorage <name> .......... Specify the method to store the values for the successors of positive reward states / transitions (" + Arrays.toString(CalculatedValues.PreviousValuesStorage.values()) + ") [default: " + CalculatedValues.PreviousValuesStorage.ALL_STATES + "]");
		mainLog.println("-setImplementation <name> ...... Specify the implementation to store the states of the sets involved in quantile computations (ADAPTIVE_SINGLETON_SET, ADAPTIVE_SET, ADAPTIVE_SET_USING_HASHSET, BIT_SET) [default: ADAPTIVE_SINGLETON_SET]");
		mainLog.println("-adaptiveSetThreshold <n> ...... Specify the threshold when AdaptiveSet should bring BitSet into play (must be > 0) [default: 1]");
		mainLog.println("-lpSolverOnly <n> .............. Use the LP solver exclusively for all calculations (the number gives the bound for the calculations)");
		mainLog.println("-multiStateMethod <name>........ Specify the method to solve multi-state sets (" + Arrays.toString(MultiStateSolutionMethod.values()) + ") [default: " + MultiStateSolutionMethod.VALUE_ITERATION + "]");
		mainLog.println("-noTryAndSet ................... Switch the try-and-set off for the calculation of zero-reward states");
		mainLog.println("-parallelPoolSize <n> .......... Specify the number of parallel threads for quantile-calculations");
		mainLog.println("-parallelForPositiveRewards .... Exploit parallelism of J8 for parallel computations of positive-reward states. The degree of parallelity is controllable via -parallelPoolSize <n>");
		mainLog.println("-parallelForZeroRewards ........ Exploit parallelism of J8 for parallel computations of zero-reward states. The degree of parallelity is controllable via -parallelPoolSize <n>");
		mainLog.println("-debugQuantile <n> ............. Set the debugging output to the given level");
		mainLog.println("-quantileverify ................ Verify the results of the quantile calculations");
		mainLog.println("-quantileBackendForTimeBound ... Use the quantile backend for the computation of time bounded reachability properties");
		mainLog.println("-quantileNaive ................. Use the naive approach for the computation of quantile queries");
		mainLog.println("-quantileNaiveLinear ........... Specify the stepping for the naive computation of quantile queries to be linear");
		mainLog.println("-quantileCtmcStartPrecision <x>  Set precision for quantile-calculations using CTMCs. Whenever a more precise computation is needed, the precision will adapt automatically. Setting to 0 will deactivate this behaviour and all calculations will be carried out using PRISMs standard-setting [default: 0]");
		mainLog.println("-quantileCtmcPrecision <x> ..... Set value of epsilon for quantile-convergence in CTMCs [default: 1e-6]");
	}

	/**
	 * Print a -help xxx message, i.e. display help on a specific switch {@code sw}.
	 * Return true iff help was available for this switch.
	 */
	public static boolean printHelpSwitch(PrismLog mainLog, String sw)
	{
		// -aroptions
		if (sw.equals("aroptions")) {
			mainLog.println("Switch: -aroptions <string>\n");
			mainLog.println("<string> is a comma-separated list of options regarding abstraction-refinement:");
			QuantAbstractRefine.printOptions(mainLog);
			return true;
		}
		// -reorderoptions
		else if (sw.equals("reorderoptions")) {
			mainLog.println("Switch: -reorderoptions <string>\n");
			mainLog.println("<string> is a comma-separated list of options for MTBDD reordering:");
			mainLog.println(" norebuild:     Do not rebuild the symbolic model after reordering.\n" +
			                "                This can be useful together with the -exportreordered switch\n" +
			                "                if you are only interested in the reordered PRISM model");
			mainLog.println(" beforereach:   Perform the reordering before the transition function is\n" +
			                "                restricted to the reachable part of the state space. This may\n" +
			                "                sometimes result in a better ordering.");
			mainLog.println(" noconstraints: Perform the reordering without any constraint on the variables.\n" +
			                "                This will not result in a variable order that can be actually used\n" +
			                "                in PRISM but may be interesting for a size comparison.");
			mainLog.println(" optimisetrans: Remove most of the symbolic representations other than the transition\n" +
			                "                function, so that the variable order for that function is optimised.\n " +
			                "                This sometimes results in a better ordering.");
			mainLog.println(" converge:      Perform reordering repeatedly, until no more benefits are achieved.");

			return true;
		}
		return false;
	}
	
	/**
	 * Set the value for an option, with the option key given as a String,
	 * and the value as an Object of appropriate type or a String to be parsed.
	 * For options of type ChoiceSetting, either a String or (0-indexed) Integer can be used.
	 */
	public synchronized void set(String key, Object value) throws PrismException
	{
		Setting set = settingFromHash(key);
		if (set == null)
			throw new PrismException("Property " + key + " is not valid");
		try
		{
			String oldValueString = set.toString();
			if (value instanceof String) {
				set.setValue(set.parseStringValue((String) value));
			} else if (value instanceof Integer && set instanceof ChoiceSetting) {
				int iv = ((Integer)value).intValue();
				((ChoiceSetting) set).setSelectedIndex(iv);
			} else {
				set.setValue(value);
			}
			notifySettingsListeners();
			if (!set.toString().equals(oldValueString))
				modified = true;
		}
		catch(SettingException e)
		{
			throw new PrismException(e.getMessage());
		}
	}
	
	/**
	 * Set the value for an option of type CHOICE_TYPE,
	 * with the option key given as a String, and the value as an integer index.
	 * This method exists to allow setting directly using 1-indexed values.
	 */
	public synchronized void setChoice(String key, int value) throws PrismException
	{
		// Adjust by 1
		set(key, value - 1);
	}
	
	public synchronized void setFileSelector(String key, FileSelector select)
	{
		Setting set = settingFromHash(key);
		if (set instanceof FileSetting) {
			FileSetting fset = (FileSetting)set;
			fset.setFileSelector(select);
		}
	}
	
	public synchronized String getString(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof SingleLineStringSetting)
		{
			return ((SingleLineStringSetting)set).getStringValue();
		}
		else if(set instanceof MultipleLineStringSetting)
		{
			return ((MultipleLineStringSetting)set).getStringValue();
		}
		else if(set instanceof ChoiceSetting)
		{
			return ((ChoiceSetting)set).getStringValue();
		}
		else return DEFAULT_STRING;
	}
	
	public synchronized int getInteger(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof IntegerSetting)
		{
			return ((IntegerSetting)set).getIntegerValue();
		}
		else if(set instanceof ChoiceSetting)
		{
			return ((ChoiceSetting)set).getCurrentIndex();
		}
		else return DEFAULT_INT;
	}
	
	public synchronized double getDouble(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof DoubleSetting)
		{
			return ((DoubleSetting)set).getDoubleValue();
		}
		else return DEFAULT_DOUBLE;
	}
	
	public synchronized boolean getBoolean(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof BooleanSetting)
		{
			return ((BooleanSetting)set).getBooleanValue();
		}
		else return DEFAULT_BOOLEAN;
	}
	
	public synchronized long getLong(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof LongSetting)
		{
			return ((LongSetting)set).getLongValue();
		}
		else return DEFAULT_LONG;
	}
	
	public synchronized int getChoice(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof ChoiceSetting)
		{
			// Adjust by 1
			return ((ChoiceSetting)set).getCurrentIndex() + 1;
		}
		else return DEFAULT_INT;
	}
	
	public synchronized Color getColor(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof ColorSetting)
		{
			return ((ColorSetting)set).getColorValue();
		}
		else return DEFAULT_COLOUR;
	}
	
	public synchronized FontColorPair getFontColorPair(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof FontColorSetting)
		{
			return ((FontColorSetting)set).getFontColorValue();
		}
		else return DEFAULT_FONT_COLOUR;
	}
	
	public synchronized File getFile(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof FileSetting)
		{
			return ((FileSetting)set).getFileValue();
		}
		else return DEFAULT_FILE;
	}
	
	public boolean isModified()
	{
		return modified;
	}
	
	public void update(Observable obs, Object obj)
	{
		modified = true;
		notifySettingsListeners();
	}
	
	public static void main(String[]args)
	{
		PrismSettings set = new PrismSettings();
		
		JFrame jf = new JFrame("Prism Settings");
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		ArrayList<DefaultSettingOwner> owners = new ArrayList<DefaultSettingOwner>();
		for(int i = 0; i < set.optionOwners.length; i++)
		{
			owners.add(set.optionOwners[i]);
		}
		
		SettingTable pt = new SettingTable(jf);
		pt.setOwners(owners);
		
		for(int i = 0; i < owners.size(); i++)
		{
			SettingOwner a = (SettingOwner)owners.get(i);
			a.setDisplay(pt);
		}
		
		jf.getContentPane().add(pt);
		jf.getContentPane().setSize(100, 300);
		
		jf.pack();
		jf.setVisible(true);
	}
}
