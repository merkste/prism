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
#include "PrismHybrid.h"
#include <math.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include <dv.h>
#include "sparse.h"
#include "hybrid.h"
#include "PrismHybridGlob.h"
#include "jnipointer.h"
#include "prism.h"
#include <new>
#include <limits>
#include <string>

// local prototypes
static void mult_rec(HDDNode *hdd, int level, int row_offset, int col_offset);
static void mult_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset);
static void mult_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset);
static PlainOrDistVector* get_vector(JNIEnv *env, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd, double* kbt, const char* name);

// globals (used by local functions)
//static HDDNode *zero;
//static int num_levels;
//static bool compact_sm;
static double *sm_dist;
static int sm_dist_shift;
static int sm_dist_mask;
static double *soln = NULL, *soln2 = NULL, *soln3 = NULL;

static void print_vector(JNIEnv* env, double* v, int n, const char* name)
{
	PH_PrintToMainLog(env, "Vector %s:\n", name);
	for (int i=0;i<n;i++) {
		PH_PrintToMainLog(env, "%d: %g\n", i, v[i]);
	}
}

static void print_vector(JNIEnv* env, PlainOrDistVector& v, const char* name)
{
	PH_PrintToMainLog(env, "Vector %s:\n", name);
	long n=v.n;
	for (long i=0;i<n;i++) {
		PH_PrintToMainLog(env, "%ld: %g\n", i, v.getValue(i));
	}
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
	PlainOrDistVector &sRew;
	DdNode** rvars;
	int nrvars;
	int taRew;
	int curIteration;
	bool lower;
	bool min;

	HDDNode *zero;
	int num_levels;

	PositiveRewRecursion(double *soln, CalculatedProbabilities& store, PlainOrDistVector& sRew, DdNode** rvars, int nrvars, int curIteration, bool lower, bool min) :
		soln(soln), store(store), sRew(sRew), rvars(rvars), nrvars(nrvars), curIteration(curIteration), lower(lower), min(min)
	{
	}

	void forAction(HDDMatrix *hddm, int taRew, DdNode *tsaRew)
	{
		this->taRew = taRew;
		zero = hddm->zero;
		num_levels = hddm->num_levels;
		mult_rec(hddm->top, tsaRew, 0, 0, 0);
	}

	void mult_rec(HDDNode *hdd,  DdNode *tsaRew, int level, int row_offset, int col_offset)
	{
		HDDNode *e, *t;

		// if it's the zero node
		if (hdd == zero) {
			return;
		}
#if 0
	// or if we've reached a submatrix
	// (check for non-null ptr but, equivalently, we could just check if level==l_sm)
	else if (hdd->sm.ptr) {
		if (!compact_sm) {
			mult_rm((RMSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		} else {
			mult_cmsr((CMSRSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		}
		return;
	}
#endif
		// or if we've reached the bottom
		else if (level == num_levels) {
			//printf("(%d,%d)=%f\n", row_offset, col_offset, hdd->type.val);

			assert(Cudd_IsConstant(tsaRew));
			int rew = sRew.getValue(row_offset) + taRew + Cudd_V(tsaRew);
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
				if (!min) {
					// for max, we can simply ignore them
					return;
				} else {
					// for min, we set the storedValue to 0
					storedValue = 0.0;
				}
			} else {
				storedValue = store.getForLevel(rewLevel)[col_offset];
			}

			if (soln[row_offset] < 0) soln[row_offset] = 0;
			double prob = hdd->type.val;
			soln[row_offset] += storedValue * prob;
			return;
		}

		// otherwise recurse
		DdNode *tsaRew_t, *tsaRew_e;
		int index = rvars[level]->index;
		if (Cudd_IsConstant(tsaRew) || index < tsaRew->index) {
			tsaRew_t = tsaRew_e = tsaRew;
		} else {
			tsaRew_t = Cudd_T(tsaRew);
			tsaRew_e = Cudd_E(tsaRew);
		}

		e = hdd->type.kids.e;
		if (e != zero) {
			mult_rec(e->type.kids.e, tsaRew_e, level+1, row_offset, col_offset);
			mult_rec(e->type.kids.t, tsaRew_e, level+1, row_offset, col_offset+e->off.val);
		}
		t = hdd->type.kids.t;
		if (t != zero) {
			mult_rec(t->type.kids.e, tsaRew_t, level+1, row_offset+hdd->off.val, col_offset);
			mult_rec(t->type.kids.t, tsaRew_t, level+1, row_offset+hdd->off.val, col_offset+t->off.val);
		}
	}
};

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_hybrid_PrismHybrid_PH_1NondetProbQuantile
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
jlong __jlongpointer _tarew,  // trans rewards, depending only on the action
jlong __jlongpointer _tsarew, // trans rewards, depending on states and action
jlong __jlongpointer _base,   // base probabilities (x_s,0)
jlong __jlongpointer o,	// 'one' states (always value 1)
jlong __jlongpointer z,	// 'zero' states (always value 0)
jlong __jlongpointer _infStates,	// 'infinity' states
jlong __jlongpointer _sPos,	// 'maybe' states (at least one positive reward transition)
jlong __jlongpointer _sZero,	// 'maybe' states (with at least one zero reward transition)
jlong __jlongpointer _maxRewForState,	// max reward per state
jlong __jlongpointer _statesOfInterest,		// states of interest
jstring _thresholdOp,  // the threshold operator
jdouble threshold,	// the threshold
jboolean min,		// min or max probabilities (true = min, false = max)
jboolean lower		// lower reward bound computation?
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
	DdNode *transActRews = jlong_to_DdNode(_tarew);  // trans rewards, depending only on the action
	DdNode *transStateActRews = jlong_to_DdNode(_tsarew); // trans rewards, depending on states and action
	DdNode *base = jlong_to_DdNode(_base);   // base probabilities (x_s,0)
	DdNode *oneStates = jlong_to_DdNode(o);	// 'one' states (always value 1)
	DdNode *zeroStates = jlong_to_DdNode(z);	// 'zero' states (always value 0)
	DdNode *infStates = jlong_to_DdNode(_infStates);	// 'infinity' states
	DdNode *statesWithPos = jlong_to_DdNode(_sPos);	// 'maybe' states (at least one positive reward transition)
	DdNode *statesWithZero = jlong_to_DdNode(_sZero);	// 'maybe' states (with at least one zero reward transition)
	DdNode *maxRewForState = jlong_to_DdNode(_maxRewForState);	// max reward per state
	DdNode *statesOfInterest = jlong_to_DdNode(_statesOfInterest);	// states of interest

	const char *thresholdOp = env->GetStringUTFChars(_thresholdOp, 0);
	enum {LT, LEQ, GT, GEQ} relOp;
	if (strcmp(thresholdOp, "<") == 0) {
		relOp = LT;
	} else if (strcmp(thresholdOp, "<=") == 0) {
		relOp = LEQ;
	} else if (strcmp(thresholdOp, ">") == 0) {
		relOp = GT;
	} else if (strcmp(thresholdOp, ">=") == 0) {
		relOp = GEQ;
	} else {
		PH_SetErrorMessage("Unknown threshold operator: %s", thresholdOp);
		return 0;
	}

	// mtbdds
//	DdNode *a = NULL;
	// model stats
	int n, nm;
	// flags
	bool compact_y;
	// matrix mtbdds
	HDDMatrices *hddmsPositive = NULL;
	HDDMatrix *hddm = NULL;
	HDDNode *hdd = NULL;
	// vectors
	double *tmpsoln = NULL;
	double *solnQuantiles = NULL;
	PlainOrDistVector *vBase = NULL, *vOneStates = NULL, *vZeroStates = NULL, *vStateRews = NULL;
	std::list<long> *todo = NULL, *infStateList = NULL;
	int* vTaRews = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, iters;
	double x, sup_norm, kb, kbt;
	bool done;

	// exception handling around whole function
	try {
	
	// start clocks
	start1 = start2 = util_cpu_time();

	// get number of states
	n = odd->eoff + odd->toff;
	
	// build hdds for matrix
	PH_PrintToMainLog(env, "\nBuilding hybrid MTBDD matrices for positive reward fragment... ");
	hddmsPositive = build_hdd_matrices_mdp(transPositive, NULL, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	nm = hddmsPositive->nm;
	kb = hddmsPositive->mem_nodes;
	kbt = kb;
	PH_PrintToMainLog(env, "[nm=%d, levels=%d, nodes=%d] ", hddmsPositive->nm, hddmsPositive->num_levels, hddmsPositive->num_nodes);
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");

#if 0
	// add sparse bits
	PH_PrintToMainLog(env, "Adding sparse bits... ");
	add_sparse_matrices_mdp(hddms, compact);
	kb = hddms->mem_sm;
	kbt += kb;
	PH_PrintToMainLog(env, "[levels=%d-%d, num=%d, compact=%d/%d] ", hddms->l_sm_min, hddms->l_sm_max, hddms->num_sm, hddms->compact_sm, hddms->nm);
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
#endif

	vBase = get_vector(env, base, rvars, num_rvars, odd, &kbt, "base probabilities");
	vZeroStates = get_vector(env, zeroStates, rvars, num_rvars, odd, &kbt, "zero states");
	vOneStates = get_vector(env, oneStates, rvars, num_rvars, odd, &kbt, "one states");
	vStateRews = get_vector(env, stateRews, rvars, num_rvars, odd, &kbt, "state rewards");

	PH_PrintToMainLog(env, "Allocating list of states of interest... ");
	todo = mtbdd01_to_list(ddman, statesOfInterest, rvars, num_rvars, odd);
	PH_PrintToMainLog(env, "%ld entries\n", todo->size());

	PH_PrintToMainLog(env, "Allocating list of infinity states... ");
	infStateList = mtbdd01_to_list(ddman, infStates, rvars, num_rvars, odd);
	PH_PrintToMainLog(env, "%ld entries\n", infStateList->size());

	// create solution/iteration vectors
	PH_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = new double[n];
	soln3 = new double[n];
	solnQuantiles = new double[n];
	kb = n*8.0/1024.0;
	kbt += 3*kb;
	PH_PrintMemoryToMainLog(env, "[3 x ", kb, "]\n");

	int window = (int)DD_FindMax(ddman, maxRewForState);

	PH_PrintToMainLog(env, "Allocating probability vector storage (for %d levels)... ", window +1);
	CalculatedProbabilities store(window, n);
	kbt += store.getKb();
	PH_PrintMemoryToMainLog(env, "[", store.getKb(), "]\n");

	PH_PrintToMainLog(env, "Allocating/populating storage for action rewards (for %d actions)... \n", nm);
	vTaRews = new int[nm];
	for (i = 0; i < nm; i++) {
		DdNode* cube = hddmsPositive->cubes[i];
		Cudd_Ref(cube);
		Cudd_Ref(transActRews);
		DdNode* ta = DD_Apply(ddman, APPLY_TIMES, cube, transActRews);
		ta = DD_MaxAbstract(ddman, ta, ndvars, num_ndvars);
		if (Cudd_IsConstant(ta)) {
			double v = Cudd_V(ta);
			int iv = (int)v;
			if ((double)iv != v) {
				PH_SetErrorMessage("Require integer rewards, found %g", v);
				// TODO: Memory leak!
				return ptr_to_jlong(NULL);
			}
			vTaRews[i] = iv;
			PH_PrintToMainLog(env, "taRew[%d] = %d\n", i, iv);
			Cudd_RecursiveDeref(ddman, ta);
		} else {
			PH_SetErrorMessage("Something went quite wrong (1)");
			// TODO: Memory leak!
			return ptr_to_jlong(NULL);
		}
	}

	// print total memory usage
	PH_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");

	for (i = 0; i < n; i++) {
		solnQuantiles[i] = -1.0;
	}

	for (int s : *infStateList) {
		solnQuantiles[s] = std::numeric_limits<double>::infinity();
	}

	// remove infinity states from the todo list
	for (auto it = todo->begin(); it != todo->end(); ) {
		int s = *it;
		if (solnQuantiles[s] == std::numeric_limits<double>::infinity()) {
			it = todo->erase(it);
		} else {
			++it;
		}
	}

	store.storeForLevel(0, *vBase);
	delete vBase;
	vBase = NULL;

	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;

	PH_PrintToMainLog(env, "\nStarting iterations...\n");

	bool done = false;
	iters = 1;
	while (!done)  // TODO maxiters?
	{
		soln2 = store.advance();

		PH_PrintToMainLog(env, "Iteration %d\n", iters);
		PositiveRewRecursion posRewCompute(soln2, store, *vStateRews, rvars, num_rvars, iters, lower, min);

		/*
		 * Pre-seeding: for min, set zeroStates, for max, set one states
		 */
		if (min) {
					JDDNode zeroStates = q.getZeroStates(i==0);
					result = JDD.Apply(JDD.TIMES, result, JDD.Not(zeroStates.copy()));
					statesWithValue = JDD.Or(statesWithValue, zeroStates);
				} else {
					// max
					JDDNode oneStates = q.getOneStates(i==0);
					result = JDD.Apply(JDD.MAX, result, oneStates.copy());
					statesWithValue = JDD.Or(statesWithValue, oneStates);
				}

		for (i = 0; i < nm; i++) {
			int taRew = vTaRews[i];
			posRewCompute.forAction(hddmsPositive->choices[i], taRew, transStateActRews);
			std::string name = "after action " + std::to_string(i);
			print_vector(env, soln2, n, name.c_str());
		}



		// sort out anything that's still -1
		// (should just be yes/no states)
		// TODO: clamp to zero?
		for (i = 0; i < n; i++) {
			if (soln2[i] < 0) {
				soln2[i] = vOneStates->getValue(i);
			}
		}

		// check if we are done
		for (auto it = todo->begin(); it != todo->end(); ) {
			int s = *it;
			bool sDone = false;
			switch (relOp) {
			case LT:
				sDone = soln2[s] < threshold;
				break;
			case LEQ:
				sDone = soln2[s] <= threshold;
				break;
			case GT:
				sDone = soln2[s] > threshold;
				break;
			case GEQ:
				sDone = soln2[s] >= threshold;
				break;
			}

			if (sDone) {
				solnQuantiles[s] = iters;
				it = todo->erase(it);
			} else {
				++it;
			}
		}

		++iters;
		done = todo->empty();
	}


#if 0
		// start iterations
	iters = 0;
	done = false;
	
	while (!done && iters < max_iters) {
		
		iters++;
		
		// initialise array for storing mins/maxs to -1s
		// (allows us to keep track of rows not visited)
		for (i = 0; i < n; i++) {
			soln2[i] = -1;
		}
		
		// do matrix multiplication and min/max
		for (i = 0; i < nm; i++) {
			
			// store stuff to be used globally
			hddm = hddms->choices[i];
			hdd = hddm->top;
			zero = hddm->zero;
			num_levels = hddm->num_levels;
			compact_sm = hddm->compact_sm;
			if (compact_sm) {
				sm_dist = hddm->dist;
				sm_dist_shift = hddm->dist_shift;
				sm_dist_mask = hddm->dist_mask;
			}
			
			// start off all -1
			// (allows us to keep track of rows not visited)
			for (j = 0; j < n; j++) {
				soln3[j] = -1;
			}
			
			// matrix multiply
			mult_rec(hdd, 0, 0, 0);
			
			// min/max
			for (j = 0; j < n; j++) {
				if (soln3[j] >= 0) {
					if (soln2[j] < 0) {
						soln2[j] = soln3[j];
					} else if (min) {
						if (soln3[j] < soln2[j]) soln2[j] = soln3[j];
					} else {
						if (soln3[j] > soln2[j]) soln2[j] = soln3[j];
					}
				}
			}
		}
		
		// sort out anything that's still -1
		// (should just be yes/no states)
		for (i = 0; i < n; i++) {
			if (soln2[i] < 0) {
				soln2[i] = (!compact_y) ? (yes_vec[i]) : (yes_dist->dist[yes_dist->ptrs[i]]);
			}
		}
		
		// check convergence
		sup_norm = 0.0;
		for (i = 0; i < n; i++) {
			x = fabs(soln2[i] - soln[i]);
			if (term_crit == TERM_CRIT_RELATIVE) {
				x /= soln2[i];
			}
			if (x > sup_norm) sup_norm = x;
		}
		if (sup_norm < term_crit_param) {
			done = true;
		}
		
		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PH_PrintToMainLog(env, "Iteration %d: max %sdiff=%f", iters, (term_crit == TERM_CRIT_RELATIVE)?"relative ":"", sup_norm);
			PH_PrintToMainLog(env, ", %.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
		
		// prepare for next iteration
		tmpsoln = soln;
		soln = soln2;
		soln2 = tmpsoln;
	}
#endif
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iterations/timing info
	PH_PrintToMainLog(env, "\nQuantile iterations: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);

	// if the iterative method didn't terminate, this is an error
	if (!done) { delete soln; soln = NULL; PH_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); }

	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PH_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}

	// TODO: correct cleanup on error
	PH_PrintToMainLog(env, "Results\n");
	for (i=0;i<n;i++) {
		PH_PrintToMainLog(env, "%d = %g\n", i, solnQuantiles[i]);
	}

	// free memory
	if (hddmsPositive) delete hddmsPositive;
	if (vBase) delete vBase;
	if (vOneStates) delete vOneStates;
	if (vZeroStates) delete vZeroStates;
	if (vStateRews) delete vStateRews;
	if (vTaRews) delete[] vTaRews;

	if (todo) delete todo;
	if (infStateList) delete infStateList;

	if (soln3) delete[] soln3;

	env->ReleaseStringUTFChars(_thresholdOp, thresholdOp);

	return ptr_to_jlong(solnQuantiles);
}

//------------------------------------------------------------------------------

static PlainOrDistVector* get_vector(JNIEnv *env, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd, double *kbt, const char* name)
{
	PH_PrintToMainLog(env, "Creating vector for %s... ", name);
	PlainOrDistVector *vec = mtbdd_to_plain_or_dist_vector(ddman, dd, vars, num_vars, odd, compact);
	*kbt += vec->getKb();
	if (vec->compact()) PH_PrintToMainLog(env, "[dist=%d, compact] ", vec->dist->num_dist);
	PH_PrintMemoryToMainLog(env, "[", vec->getKb(), "]\n");

	print_vector(env, *vec, name);
	return vec;
}

//------------------------------------------------------------------------------

//-----------------------------------------------------------------------------------

static void mult_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset)
{
	int i2, j2, l2, h2;
	int sm_n = rmsm->n;
	int sm_nnz = rmsm->nnz;
	double *sm_non_zeros = rmsm->non_zeros;
	unsigned char *sm_row_counts = rmsm->row_counts;
	int *sm_row_starts = (int *)rmsm->row_counts;
	bool sm_use_counts = rmsm->use_counts;
	unsigned int *sm_cols = rmsm->cols;
	
	// loop through rows of submatrix
	l2 = sm_nnz; h2 = 0;
	for (i2 = 0; i2 < sm_n; i2++) {
		
		// loop through entries in this row
		if (!sm_use_counts) { l2 = sm_row_starts[i2]; h2 = sm_row_starts[i2+1]; }
		else { l2 = h2; h2 += sm_row_counts[i2]; }
		for (j2 = l2; j2 < h2; j2++) {
			int r = row_offset + i2;
			if (soln3[r] < 0) soln3[r] = 0;
			soln3[r] += soln[col_offset + sm_cols[j2]] * sm_non_zeros[j2];
			//printf("(%d,%d)=%f\n", row_offset + i2, col_offset + sm_cols[j2], sm_non_zeros[j2]);
		}
	}
}

//-----------------------------------------------------------------------------------

static void mult_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset)
{
	int i2, j2, l2, h2;
	int sm_n = cmsrsm->n;
	int sm_nnz = cmsrsm->nnz;
	unsigned char *sm_row_counts = cmsrsm->row_counts;
	int *sm_row_starts = (int *)cmsrsm->row_counts;
	bool sm_use_counts = cmsrsm->use_counts;
	unsigned int *sm_cols = cmsrsm->cols;
	
	// loop through rows of submatrix
	l2 = sm_nnz; h2 = 0;
	for (i2 = 0; i2 < sm_n; i2++) {
		
		// loop through entries in this row
		if (!sm_use_counts) { l2 = sm_row_starts[i2]; h2 = sm_row_starts[i2+1]; }
		else { l2 = h2; h2 += sm_row_counts[i2]; }
		for (j2 = l2; j2 < h2; j2++) {
			int r = row_offset + i2;
			if (soln3[r] < 0) soln3[r] = 0;
			soln3[r] += soln[col_offset + (int)(sm_cols[j2] >> sm_dist_shift)] * sm_dist[(int)(sm_cols[j2] & sm_dist_mask)];
			//printf("(%d,%d)=%f\n", row_offset + i2, col_offset + (int)(sm_cols[j2] >> sm_dist_shift), sm_dist[(int)(sm_cols[j2] & sm_dist_mask)]);
		}
	}
}

//------------------------------------------------------------------------------

