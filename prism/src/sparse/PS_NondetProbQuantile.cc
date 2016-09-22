//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

// includes
#include "PrismSparse.h"
#include <math.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include <dv.h>
#include "sparse.h"
#include "PrismSparseGlob.h"
#include "jnipointer.h"
#include "prism.h"
#include "ExportIterations.h"
#include "Measures.h"
#include <memory>
#include <vector>
#include <map>
#include <new>
#include <limits>
#include <string>

// local prototypes
static PlainOrDistVector* get_vector(JNIEnv *env, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd, double* kbt, const char* name);

// global
static const bool debug = false;

enum relOp_t {LT, LEQ, GT, GEQ};

static void print_vector(JNIEnv* env, double* v, int n, const char* name)
{
	PS_PrintToMainLog(env, "Vector %s:\n", name);
	for (int i=0;i<n;i++) {
		PS_PrintToMainLog(env, "%d: %g\n", i, v[i]);
	}
}

static void print_vector(JNIEnv* env, PlainOrDistVector& v, const char* name)
{
	PS_PrintToMainLog(env, "Vector %s:\n", name);
	long n=v.n;
	for (long i=0;i<n;i++) {
		PS_PrintToMainLog(env, "%ld: %g\n", i, v.getValue(i));
	}
}

static void print_vector(JNIEnv* env, std::vector<bool>& v, const char* name)
{
	PS_PrintToMainLog(env, "Bitvector %s:\n", name);
	long n=v.size();
	for (long i=0;i<n;i++) {
		if (v[i]) {
			PS_PrintToMainLog(env, "%ld\n", i);
		}
	}
}

inline bool check_treshold(enum relOp_t relOp, double value, double threshold) {
	switch (relOp) {
	case LT:
		return value < threshold;
	case LEQ:
		return value <= threshold;
	case GT:
		return value > threshold;
	case GEQ:
		return value >= threshold;
	}
	assert(false);
	return false;
}

struct CalculatedProbabilities
{
	unsigned int window;
	unsigned int n;
	unsigned int offset;

	double** soln;

	CalculatedProbabilities(unsigned int window, int n) : window(window), n(n)
	{
		soln = new double*[window + 1];
		for (int i = 0; i < window + 1; i++) {
			soln[i] = new double[n];
			for (int j = 0; j < n; j++)
				soln[i][j] = 0.0;
		}
		offset = 0;
	}

	~CalculatedProbabilities()
	{
		for (int i = 0; i < window+1; i++) {
			delete[] soln[i];
		}
		delete[] soln;
	}

	double getKb()
	{
		return ((window+1.0)*n) * sizeof(double) / 1024.0;
	}

	int getIndex(int level) const
	{
		return level % (window + 1);
	}

	double* getForLevel(int level) const
	{
		return soln[getIndex(level)];
	}

	void storeForLevel(int level, PlainOrDistVector& vec)
	{
		double* v = getForLevel(level);
		for (int i = 0; i < n; i++) {
			v[i] = vec.getValue(i);
		}
	}

	double* advance()
	{
		++offset;
		double* res = getForLevel(offset);
		return res;
	}

};

struct PositiveRewRecursion {
	double* soln;
	const CalculatedProbabilities& store;
	PlainOrDistVector &sRews;
	int curIteration;
	bool lower;
	bool min;

	PositiveRewRecursion(double *soln, CalculatedProbabilities& store, PlainOrDistVector& sRews, int curIteration, bool lower, bool min) :
		soln(soln), store(store), sRews(sRews), curIteration(curIteration), lower(lower), min(min)
	{
	}

	void run(NDSparseMatrix *ndsmPositive, NDSparseMatrix *ndsm_r, int n) {
		// store local copies of stuff
		// firstly for transition matrix
		double *non_zeros = ndsmPositive->non_zeros;
		unsigned char *row_counts = ndsmPositive->row_counts;
		int *row_starts = (int *)ndsmPositive->row_counts;
		unsigned char *choice_counts = ndsmPositive->choice_counts;
		int *choice_starts = (int *)ndsmPositive->choice_counts;
		bool use_counts = ndsmPositive->use_counts;
		unsigned int *cols = ndsmPositive->cols;
		// and then for transition rewards matrix
		// (note: we don't need row_counts/row_starts for
		// this since choice structure mirrors transition matrix)
		double *non_zeros_r = ndsm_r->non_zeros;
		unsigned char *choice_counts_r = ndsm_r->choice_counts;
		int *choice_starts_r = (int *)ndsm_r->choice_counts;
		bool use_counts_r = ndsm_r->use_counts;
		unsigned int *cols_r = ndsm_r->cols;

		int i, j, k, k_r, l1, h1, l2, h2, l2_r, h2_r;

		// do matrix multiplication and min/max
		h1 = h2 = h2_r = 0;
		// loop through states
		for (i = 0; i < n; i++) {
			double d1 = 0.0; // initial value doesn't matter
			bool first = true; // (because we also remember 'first')
			// get pointers to nondeterministic choices for state i
			if (!use_counts) { l1 = row_starts[i]; h1 = row_starts[i+1]; }
			else { l1 = h1; h1 += row_counts[i]; }

			// loop through those choices
			for (j = l1; j < h1; j++) {
				double d2 = 0;
				// get pointers to transitions
				if (!use_counts) { l2 = choice_starts[j]; h2 = choice_starts[j+1]; }
				else { l2 = h2; h2 += choice_counts[j]; }
				// and get pointers to transition rewards
				if (!use_counts_r) { l2_r = choice_starts_r[j]; h2_r = choice_starts_r[j+1]; }
				else { l2_r = h2_r; h2_r += choice_counts_r[j]; }
				// loop through transitions
				for (k = l2; k < h2; k++) {
					// find corresponding transition reward if any
					k_r = l2_r; while (k_r < h2_r && cols_r[k_r] != cols[k]) k_r++;

					// if there is one, handle
					if (k_r < h2_r) {
						int tRew = non_zeros_r[k_r];
						int row = i;
						int col = cols[k];
						double prob = non_zeros[k];

						d2 += handle_terminal(row, col, tRew, prob);
						k_r++;
					} else {
						// only state reward
						int tRew = 0;
						int row = i;
						int col = cols[k];
						double prob = non_zeros[k];

						d2 += handle_terminal(row, col, tRew, prob);
						k_r++;
					}
				}
				// see if this value is the min/max so far
				if (first || (min&&(d2<d1)) || (!min&&(d2>d1))) {
					d1 = d2;
				}
				first = false;
			}
			// set vector element
			// (if there were no choices from this state, do nothing)
			if (h1 > l1)
				soln[i] = d1;
		}
	}

	double handle_terminal(int row_offset, int col_offset, int tRew, double prob) {
		int rew = sRews.getValue(row_offset) + tRew;
		if (debug) printf("rew = %d (%d + %d)\n", rew, (int)sRews.getValue(row_offset), tRew);
		// Compute the target level for this transition by
		// doing the necessary subtraction:
		//  upper bound = i - r
		//  lower bound = max{0, i-r}
		int rewLevel = curIteration - rew;
		if (lower && rewLevel < 0) {
			rewLevel = 0;
		}

		double storedValue;
		if (rewLevel < 0) {
			// the transitions with reward r are invalid
			// we set the storedValue to 0
			storedValue = 0.0;
		} else {
			storedValue = store.getForLevel(rewLevel)[col_offset];
		}

		if (debug) printf("row = %d, stored=%g, probs=%g, result=%g\n", row_offset, storedValue, prob, storedValue * prob);
		return storedValue * prob;
	}

};

struct ZeroRewRecursion {
	double* soln;
	double* soln2;
	bool min;

	ZeroRewRecursion(double *soln, double *soln2, bool min) :
		soln(soln), soln2(soln2), min(min)
	{
	}

	void run(NDSparseMatrix *ndsmZero, int n)
	{
		// store local copies of stuff
		// firstly for transition matrix
		double *non_zeros = ndsmZero->non_zeros;
		unsigned char *row_counts = ndsmZero->row_counts;
		int *row_starts = (int *)ndsmZero->row_counts;
		unsigned char *choice_counts = ndsmZero->choice_counts;
		int *choice_starts = (int *)ndsmZero->choice_counts;
		bool use_counts = ndsmZero->use_counts;
		unsigned int *cols = ndsmZero->cols;

		int i, j, k, l1, h1, l2, h2;

		// do matrix multiplication and min/max
		h1 = h2 = 0;
		for (i = 0; i < n; i++) {
			double d1 = 0.0; // initial value doesn't matter
			bool first = true; // (because we also remember 'first')
			if (!use_counts) { l1 = row_starts[i]; h1 = row_starts[i+1]; }
			else { l1 = h1; h1 += row_counts[i]; }
			for (j = l1; j < h1; j++) {
				double d2 = 0;
				if (!use_counts) { l2 = choice_starts[j]; h2 = choice_starts[j+1]; }
				else { l2 = h2; h2 += choice_counts[j]; }
				for (k = l2; k < h2; k++) {
					d2 += non_zeros[k] * soln[cols[k]];
				}
				if (first || (min&&(d2<d1)) || (!min&&(d2>d1))) {
					d1 = d2;
					first = false;
				}
				// set vector element
				// (if no choices, keep -1)
				if (h1 > l1) {
					soln2[i] = d1;
				}
			}
		}
	}

};


//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1NondetProbQuantile
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer tPos,	// trans matrix for actions that have positive reward (also, all transitions for pos rew states)
jlong __jlongpointer tZero,	// trans matrix for zero reward actions
jlong __jlongpointer od,	// odd
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer ndv,	// nondet vars
jint num_ndvars,
jlong __jlongpointer _srew,   // state rewards
jlong __jlongpointer _trew,  // trans rewards
jlong __jlongpointer _base,   // base probabilities (x_s,0)
jlong __jlongpointer o,	// 'one' states (always value 1)
jlong __jlongpointer z,	// 'zero' states (always value 0)
jlong __jlongpointer _infValues,	// 'infinity' state values
jlong __jlongpointer _maxRewForState,	// max reward per state
jlong __jlongpointer _statesOfInterest,		// states of interest
jstring _thresholdOp,  // the threshold operator
jdoubleArray _thresholds,	// the threshold
jboolean min,		// min or max probabilities (true = min, false = max)
jboolean lower,		// lower reward bound computation?
jboolean printResultsAsTheyHappen  // print results as they happen
)
{
	// cast function parameters
	DdNode *transPositive = jlong_to_DdNode(tPos);		// trans matrix for actions that have positive reward (also, all transitions for pos rew states)
	DdNode *transZero = jlong_to_DdNode(tZero);		// trans matrix
	ODDNode *odd = jlong_to_ODDNode(od); 		// reachable states
	DdNode **rvars = jlong_to_DdNode_array(rv); 	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv); 	// col vars
	DdNode **ndvars = jlong_to_DdNode_array(ndv);	// nondet vars

	DdNode *stateRews = jlong_to_DdNode(_srew);   // state rewards
	DdNode *transRews = jlong_to_DdNode(_trew);  // trans rewards
	DdNode *base = jlong_to_DdNode(_base);   // base probabilities (x_s,0)
	DdNode *oneStates = jlong_to_DdNode(o);	// 'one' states (always value 1)
	DdNode *zeroStates = jlong_to_DdNode(z);	// 'zero' states (always value 0)
	DdNode *infValues = jlong_to_DdNode(_infValues);	// 'infinity' state values
	DdNode *maxRewForState = jlong_to_DdNode(_maxRewForState);	// max reward per state
	DdNode *statesOfInterest = jlong_to_DdNode(_statesOfInterest);	// states of interest

	enum relOp_t relOp;
	const char *thresholdOp = env->GetStringUTFChars(_thresholdOp, 0);
	if (strcmp(thresholdOp, "<") == 0) {
		relOp = LT;
	} else if (strcmp(thresholdOp, "<=") == 0) {
		relOp = LEQ;
	} else if (strcmp(thresholdOp, ">") == 0) {
		relOp = GT;
	} else if (strcmp(thresholdOp, ">=") == 0) {
		relOp = GEQ;
	} else {
		PS_SetErrorMessage("Unknown threshold operator: %s", thresholdOp);
		return 0;
	}

	std::vector<double> thresholds;
	int numThresholds = env->GetArrayLength(_thresholds);
	double *__thresholds = env->GetDoubleArrayElements(_thresholds, NULL);
	for (int i = 0; i < numThresholds; i++) {
		thresholds.push_back(__thresholds[i]);
	}
	env->ReleaseDoubleArrayElements(_thresholds, __thresholds, JNI_ABORT);

	// mtbdds
//	DdNode *a = NULL;
	// model stats
	int n, ncPos, nc_r, ncZero;
	long nnzPos, nnz_r, nnzZero;
	// sparse matrix
	NDSparseMatrix *ndsmPositive = NULL, *ndsm_r = NULL, *ndsmZero = NULL;
	// vectors
	double *tmpsoln = NULL;
	double *soln = NULL, *soln2 = NULL;
	std::map<double, double*> solnQuantiles;
	PlainOrDistVector *vBase = NULL, *vStateRews = NULL, *vInf = NULL;
	std::vector<bool> *vOneStates = NULL, *vZeroStates = NULL;
	std::list<long> *todo = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, iters;
	double kb, kbt;
	bool done;

	// exception handling around whole function
	try {
	
	// start clocks
	start1 = start2 = util_cpu_time();

	// get number of states
	n = odd->eoff + odd->toff;

	// build sparse matrix (positive reward fragment)
	PS_PrintToMainLog(env, "\nBuilding sparse matrix (positive reward fragment)... ");
	ndsmPositive = build_nd_sparse_matrix(ddman, transPositive, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	// get number of transitions/choices
	nnzPos = ndsmPositive->nnz;
	ncPos = ndsmPositive->nc;
	kb = ndsmPositive->mem;
	kbt = kb;
	// print out info
	PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%d, k=%d] ", n, ncPos, nnzPos, ndsmPositive->k);
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");

	// build sparse matrix (rewards)
	PS_PrintToMainLog(env, "Building sparse matrix (transition rewards)... ");
	ndsm_r = build_sub_nd_sparse_matrix(ddman, transPositive, transRews, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	// get number of transitions/choices
	nnz_r = ndsm_r->nnz;
	nc_r = ndsm_r->nc;
	// print out info
	PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%d, k=%d] ", n, nc_r, nnz_r, ndsm_r->k);
	kb = (nnz_r*12.0+nc_r*4.0+n*4.0)/1024.0;
	kbt += kb;
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");

	if (transZero != Cudd_ReadZero(ddman)) {
		// build sparse matrix (zero reward fragment)
		PS_PrintToMainLog(env, "\nBuilding sparse matrix (zero reward fragment)... ");
		ndsmZero = build_nd_sparse_matrix(ddman, transZero, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
		// get number of transitions/choices
		nnzZero = ndsmZero->nnz;
		ncZero = ndsmZero->nc;
		kb = ndsmZero->mem;
		kbt = kb;
		// print out info
		PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%d, k=%d] ", n, ncZero, nnzZero, ndsmZero->k);
		PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	}

	vBase = get_vector(env, base, rvars, num_rvars, odd, &kbt, "base probabilities");
	vStateRews = get_vector(env, stateRews, rvars, num_rvars, odd, &kbt, "state rewards");

	PS_PrintToMainLog(env, "Allocating bitsets for one and zero states... ");
	vZeroStates = mtbdd01_to_bool_vector(ddman, zeroStates, rvars, num_rvars, odd);
	if (debug) print_vector(env, *vZeroStates, "zero states");
	vOneStates = mtbdd01_to_bool_vector(ddman, oneStates, rvars, num_rvars, odd);
	if (debug) print_vector(env, *vOneStates, "one states");

	PS_PrintToMainLog(env, "Allocating list of states of interest... ");
	todo = mtbdd01_to_list(ddman, statesOfInterest, rvars, num_rvars, odd);
	PS_PrintToMainLog(env, "%ld entries\n", todo->size());

	vInf = get_vector(env, infValues, rvars, num_rvars, odd, &kbt, "infinity state values");

	// create solution/iteration vectors
	PS_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = new double[n];
	soln2 = new double[n];
	kb = n*8.0/1024.0;
	kbt += 2*kb;
	PS_PrintMemoryToMainLog(env, "[2 x ", kb, "]\n");

	PS_PrintToMainLog(env, "Allocating solution vectors... ");
	for (i = 0; i < thresholds.size(); i++) {
		double *v = new double[n];
		for (j = 0; j < n; j++)
			v[j] = -1.0;
		// TODO: check for multiple writes
		solnQuantiles[thresholds[i]] = v;
	}
	kb = thresholds.size() * n*8.0/1024.0;
	kbt += kb;
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");

	int window = (int)DD_FindMax(ddman, maxRewForState);

	PS_PrintToMainLog(env, "Allocating probability vector storage (for %d levels)... ", window +1);
	CalculatedProbabilities store(window, n);
	kbt += store.getKb();
	PS_PrintMemoryToMainLog(env, "[", store.getKb(), "]\n");

	// print total memory usage
	PS_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");

	// measure for convergence termination check
	MeasureSupNorm measure(term_crit == TERM_CRIT_RELATIVE);

	// process thresholds against infinity values
	for (auto it = todo->begin(); it != todo->end(); ) {
		int s = *it;
		bool sDone = true;
		for (double threshold : thresholds) {
			if (solnQuantiles[threshold][s] != -1)
				continue;

			// for infinity, check negation of threshold
			if (!check_treshold(relOp, vInf->getValue(s), threshold)) {
				solnQuantiles[threshold][s] = std::numeric_limits<double>::infinity();
				if (printResultsAsTheyHappen) {
					PS_PrintToMainLog(env, "FYI: Results for threshold %g and state %d = Infinity\n", threshold, s);
				}
			} else {
				sDone = false;
			}
		}
		if (sDone) {
			it = todo->erase(it);
		} else {
			++it;
		}
	}

	store.storeForLevel(0, *vBase);

	std::unique_ptr<ExportIterations> iterationExport;
	if (PS_GetFlagExportIterations()) {
		iterationExport.reset(new ExportIterations("PS_NondetProbQuantile", "quantile.html"));
		iterationExport->exportVector(store.getForLevel(0), n, 0);
	}

	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;

	PS_PrintToMainLog(env, "\nStarting iterations...\n");

	iters = -1;
	if (!todo->empty()) {
		iters = 0;
		// check against thresholds (for i = 0)
		for (auto it = todo->begin(); it != todo->end(); ) {
			int s = *it;
			bool sDone = true;
			for (double threshold : thresholds) {
				if (solnQuantiles[threshold][s] != -1)
					continue;

				if (check_treshold(relOp, vBase->getValue(s), threshold)) {
					solnQuantiles[threshold][s] = 0;
					if (printResultsAsTheyHappen) {
						PS_PrintToMainLog(env, "FYI: Results for threshold %g and state %d = 0\n", threshold, s);
					}
				} else {
					sDone = false;
				}
			}
			if (sDone) {
				it = todo->erase(it);
			} else {
				++it;
			}
		}
	}

	done = todo->empty();

	while (!done)
	{
		++iters;

		double* solnPos = store.advance();

		// reset solnPos to -1.0 everywhere
		for (i = 0; i < n; i++) {
			solnPos[i] = -1.0;
		}

		PS_PrintToMainLog(env, "Quantile iteration %d\n", iters);
		long startPosRew = util_cpu_time();
		PositiveRewRecursion posRewCompute(solnPos, store, *vStateRews, iters, lower, min);

		posRewCompute.run(ndsmPositive, ndsm_r, n);

		/*
		 * Post-processing: set one states and set zero states
		 */
		for (i = 0; i < n; i++) {
			if ((*vOneStates)[i]) {
				solnPos[i] = 1.0;
			} else if ((*vZeroStates)[i]) {
				solnPos[i] = 0;
			}
		}

		if (debug) {
			print_vector(env, solnPos, n, "solnPos after postprocessing");
		}
		PS_PrintToMainLog(env, "Quantile iteration %d, positive rewards in %.2f seconds\n", iters, ((double)(util_cpu_time() - startPosRew)/1000));

		// ------------------ Zero reward fragment --------------------------

		// start iterations
		int zIters = 0;
		bool zDone = false;

		// initialise with values computed with positive rewards
		for (j = 0; j < n; j++) {
			soln[j] = solnPos[j];
			if (soln[j] < 0)  // for negative (no pos reward transitions) initialise with 0
				soln[j] = 0.0;
		}

		if (ndsmZero == NULL) {
			zDone = true;
		}

		long startZeroRew = start3 = util_cpu_time();
		while (!zDone && zIters < max_iters) {
			zIters++;

			if (debug) {
				std::string name = "soln for zIters = " + std::to_string(zIters);
				print_vector(env, soln, n, name.c_str());
			}

			// initialise array for storing mins/maxs to -1s
			// (allows us to keep track of rows not visited)
			for (i = 0; i < n; i++) {
				soln2[i] = -1;
			}

			ZeroRewRecursion zeroRewCompute(soln, soln2, min);

			// matrix multiply
			zeroRewCompute.run(ndsmZero, n);

			if (debug) {
				std::string name = "soln2 for zIters = " + std::to_string(zIters) +" after actions";
				print_vector(env, soln2, n, name.c_str());
			}

			// do additional min/max for posRew, reset zero/one states
			for (j = 0; j < n; j++) {
				if ((*vOneStates)[j]) {
					soln2[j] = 1.0;
				} else if ((*vZeroStates)[j]) {
					soln2[j] = 0.0;
				} else if (solnPos[j] >= 0) {
					if (soln2[j] < 0) {
						soln2[j] = solnPos[j];
					} else if (min) {
						if (solnPos[j] < soln2[j]) soln2[j] = solnPos[j];
					} else {
						if (solnPos[j] > soln2[j]) soln2[j] = solnPos[j];
					}
				}
				assert(soln2[j] >= 0);
			}

			if (debug) {
				std::string name = "soln2 for zIters = " + std::to_string(zIters) +" after min/max with solnPos";
				print_vector(env, soln2, n, name.c_str());
			}

			// check convergence
			measure.reset();
			measure.measure(soln, soln2, n);
			if (measure.value() < term_crit_param) {
				zDone = true;
			}

			// print occasional status update
			if ((util_cpu_time() - start3) > UPDATE_DELAY) {
				PS_PrintToMainLog(env, "Quantile iteration %d, zero reward iteration %d: max %sdiff=%f", iters, zIters, measure.isRelative()?"relative ":"", measure.value());
				PS_PrintToMainLog(env, ", %.2f sec so far\n", ((double)(util_cpu_time() - start3)/1000));
				start3 = util_cpu_time();
			}

			if (debug)
				print_vector(env, soln2, n, "after iteration");

			// prepare for next iteration
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		PS_PrintToMainLog(env, "Quantile iteration %d, zero reward in %.2f seconds\n", iters, ((double)(util_cpu_time() - startZeroRew)/1000));

		// store computed values...
		for (j = 0; j < n; j++) {
			solnPos[j] = soln[j];
		}

		if (iterationExport) {
			iterationExport->exportVector(store.getForLevel(iters), n, 0);
		}

		// check against thresholds
		for (auto it = todo->begin(); it != todo->end(); ) {
			int s = *it;
			bool sDone = true;
			for (double threshold : thresholds) {
				if (solnQuantiles[threshold][s] != -1)
					continue;

				if (check_treshold(relOp, soln[s], threshold)) {
					solnQuantiles[threshold][s] = iters;
					if (printResultsAsTheyHappen) {
						PS_PrintToMainLog(env, "FYI: Results for threshold %g and state %d = %d\n", threshold, s, iters);
					}
				} else {
					sDone = false;
				}
			}
			if (sDone) {
				it = todo->erase(it);
			} else {
				++it;
			}
		}
		done = todo->empty();
	}

	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iterations/timing info
	iters++;  // we increase +1 because we are interested in the number of iterations
	PS_PrintToMainLog(env, "Quantile iterations: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	PS_PrintToMainLog(env, "Quantile calculations finished for all states of interest in %d iterations.\n", iters);

	// if the iterative method didn't terminate, this is an error
	if (!done) { delete soln; soln = NULL; PS_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); }

	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PS_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}

	// result output
	if (thresholds.size() > 1) {
		for (auto& entry : solnQuantiles) {
			PS_PrintToMainLog(env, "\n---------------------------------------------------------------------");
			PS_PrintToMainLog(env, "\nResults for threshold %g:\n", entry.first);
			for (j = 0; j < n; j++) {
				double v = entry.second[j];
				if (v < 0)
					continue;
				else {
					if (isinf(v) && v > 0) {
						PS_PrintToMainLog(env, "%d:=Infinity\n", j, v);
					} else {
						PS_PrintToMainLog(env, "%d:=%.1f\n", j, v);
					}
				}
			}
		}
		PS_PrintToMainLog(env, "\n---------------------------------------------------------------------");
	}

	double *result = solnQuantiles.rbegin()->second;
	solnQuantiles.erase(solnQuantiles.rbegin()->first);
	for (auto& entry : solnQuantiles) {
		delete[] entry.second;
	}

	// free memory
	if (ndsmPositive) delete ndsmPositive;
	if (ndsmZero) delete ndsmZero;
	if (ndsm_r) delete ndsm_r;

	if (vBase) delete vBase;
	if (vOneStates) delete vOneStates;
	if (vZeroStates) delete vZeroStates;
	if (vStateRews) delete vStateRews;

	if (soln) delete[] soln;
	if (soln2) delete[] soln2;
	if (todo) delete todo;

	env->ReleaseStringUTFChars(_thresholdOp, thresholdOp);

	return ptr_to_jlong(result);
}

//------------------------------------------------------------------------------

static PlainOrDistVector* get_vector(JNIEnv *env, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd, double *kbt, const char* name)
{
	PS_PrintToMainLog(env, "Creating vector for %s... ", name);
	PlainOrDistVector *vec = mtbdd_to_plain_or_dist_vector(ddman, dd, vars, num_vars, odd, compact);
	*kbt += vec->getKb();
	if (vec->compact()) PS_PrintToMainLog(env, "[dist=%d, compact] ", vec->dist->num_dist);
	PS_PrintMemoryToMainLog(env, "[", vec->getKb(), "]\n");

	if (debug)
		print_vector(env, *vec, name);
	return vec;
}

//------------------------------------------------------------------------------
