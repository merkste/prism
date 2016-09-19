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
#include "ExportIterations.h"
#include <memory>
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
//static double *sm_dist;
//static int sm_dist_shift;
//static int sm_dist_mask;
//static double *soln = NULL, *soln2 = NULL, *soln3 = NULL;
static const bool debug = false;

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

static void print_vector(JNIEnv* env, std::vector<bool>& v, const char* name)
{
	PH_PrintToMainLog(env, "Bitvector %s:\n", name);
	long n=v.size();
	for (long i=0;i<n;i++) {
		if (v[i]) {
			PH_PrintToMainLog(env, "%ld\n", i);
		}
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
			if (debug) printf("(%d,%d)=%f\n", row_offset, col_offset, hdd->type.val);

			assert(Cudd_IsConstant(tsaRew));
			int rew = sRew.getValue(row_offset) + taRew + (int)Cudd_V(tsaRew);
			if (debug) printf("rew = %d (%d + %d + %d)\n", rew, (int)sRew.getValue(row_offset), taRew, (int)Cudd_V(tsaRew));
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

			if (soln[row_offset] < 0) soln[row_offset] = 0;
			double prob = hdd->type.val;
			soln[row_offset] += storedValue * prob;
			if (debug) printf("row = %d, stored=%g, probs=%g, result=%g\n", row_offset, storedValue, prob, soln[row_offset]);
			if (debug) printf("soln(%d)=%f\n", row_offset, soln[row_offset]);
			return;
		}

		// otherwise recurse
		DdNode *tsaRew_t, *tsaRew_e;
		int index = rvars[level]->index;
		if (Cudd_IsConstant(tsaRew)) {
			tsaRew_t = tsaRew_e = tsaRew;
			//printf("tsaRecursion (constant %g) %p %p %p: %d r=%d %d t=%d e=%d\n", Cudd_V(tsaRew), tsaRew, tsaRew_t, tsaRew_e, level, rvars[level]->index, tsaRew->index, tsaRew_t->index, tsaRew_e->index);
		} else if (index < tsaRew->index) {
			tsaRew_t = tsaRew_e = tsaRew;
			// printf("tsaRecursion (same) %p %p %p: l=%d r=%d %d t=%d e=%d\n", tsaRew, tsaRew_t, tsaRew_e, level, rvars[level]->index, tsaRew->index, tsaRew_t->index, tsaRew_e->index);
		} else {
			tsaRew_t = Cudd_T(tsaRew);
			tsaRew_e = Cudd_E(tsaRew);
			// printf("tsaRecursion (recur) %p %p %p: l=%d r=%d %d t=%d e=%d\n", tsaRew, tsaRew_t, tsaRew_e, level, rvars[level]->index, tsaRew->index, tsaRew_t->index, tsaRew_e->index);
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

struct ZeroRewRecursion {
	double* soln;
	double* soln3;

	HDDNode *zero;
	int num_levels;
	bool compact_sm;
	double *sm_dist;
	int sm_dist_shift;
	int sm_dist_mask;

	ZeroRewRecursion(double *soln, double *soln3) :
		soln(soln), soln3(soln3)
	{
	}

	void forAction(HDDMatrix *hddm)
	{
		zero = hddm->zero;
		num_levels = hddm->num_levels;

		compact_sm = hddm->compact_sm;
		if (compact_sm) {
			sm_dist = hddm->dist;
			sm_dist_shift = hddm->dist_shift;
			sm_dist_mask = hddm->dist_mask;
		}
		mult_rec(hddm->top, 0, 0, 0);
	}

	void mult_rec(HDDNode *hdd, int level, int row_offset, int col_offset)
	{
		HDDNode *e, *t;

		// if it's the zero node
		if (hdd == zero) {
			return;
		}
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
		// or if we've reached the bottom
		else if (level == num_levels) {
			if (debug) printf("(%d,%d)=%f\n", row_offset, col_offset, hdd->type.val);
			if (soln3[row_offset] < 0) soln3[row_offset] = 0;
			soln3[row_offset] += soln[col_offset] * hdd->type.val;
			if (debug) printf("soln3(%d)=%f\n", row_offset, soln3[row_offset]);
			return;
		}
		// otherwise recurse
		e = hdd->type.kids.e;
		if (e != zero) {
			mult_rec(e->type.kids.e, level+1, row_offset, col_offset);
			mult_rec(e->type.kids.t, level+1, row_offset, col_offset+e->off.val);
		}
		t = hdd->type.kids.t;
		if (t != zero) {
			mult_rec(t->type.kids.e, level+1, row_offset+hdd->off.val, col_offset);
			mult_rec(t->type.kids.t, level+1, row_offset+hdd->off.val, col_offset+t->off.val);
		}
	}

	void mult_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset)
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
				if (debug) printf("(%d,%d)=%f\n", row_offset + i2, col_offset + sm_cols[j2], sm_non_zeros[j2]);
				if (debug) printf("soln3(%d)=%f\n", r, soln3[r]);
			}
		}
	}

	void mult_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset)
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
				if (debug) printf("(%d,%d)=%f\n", row_offset + i2, col_offset + (int)(sm_cols[j2] >> sm_dist_shift), sm_dist[(int)(sm_cols[j2] & sm_dist_mask)]);
				if (debug) printf("soln3(%d)=%f\n", r, soln3[r]);
			}
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
	int n, nm, nmZero;
	// flags
	bool compact_y;
	// matrix mtbdds
	HDDMatrices *hddmsPositive = NULL;
	HDDMatrices *hddmsZero = NULL;
	HDDMatrix *hddm = NULL;
	HDDNode *hdd = NULL;
	// vectors
	double *tmpsoln = NULL;
	double *soln = NULL, *soln2 = NULL, *soln3 = NULL;
	double *solnQuantiles = NULL;
	PlainOrDistVector *vBase = NULL, *vStateRews = NULL;
	std::vector<bool> *vOneStates = NULL, *vZeroStates = NULL;
	std::list<long> *todo = NULL, *infStateList = NULL;
	int* vTaRews = NULL;
	DdNode** vTsaRews = NULL;
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
	
	// build hdds for matrix (positive reward fragment)
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


	if (transZero != Cudd_ReadZero(ddman)) {
		// build hdds for matrix (zero reward fragment)
		PH_PrintToMainLog(env, "\nBuilding hybrid MTBDD matrices for zero reward fragment... ");
		hddmsZero = build_hdd_matrices_mdp(transZero, NULL, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
		nmZero = hddmsZero->nm;
		kb = hddmsZero->mem_nodes;
		kbt += kb;
		PH_PrintToMainLog(env, "[nm=%d, levels=%d, nodes=%d] ", hddmsZero->nm, hddmsZero->num_levels, hddmsZero->num_nodes);
		PH_PrintMemoryToMainLog(env, "[", kb, "]\n");

		// add sparse bits
		PH_PrintToMainLog(env, "Adding sparse bits... ");
		add_sparse_matrices_mdp(hddmsZero, compact);
		kb = hddmsZero->mem_sm;
		kbt += kb;
		PH_PrintToMainLog(env, "[levels=%d-%d, num=%d, compact=%d/%d] ", hddmsZero->l_sm_min, hddmsZero->l_sm_max, hddmsZero->num_sm, hddmsZero->compact_sm, hddmsZero->nm);
		PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	}

	vBase = get_vector(env, base, rvars, num_rvars, odd, &kbt, "base probabilities");
	vStateRews = get_vector(env, stateRews, rvars, num_rvars, odd, &kbt, "state rewards");

	PH_PrintToMainLog(env, "Allocating bitsets for one and zero states... ");
	vZeroStates = mtbdd01_to_bool_vector(ddman, zeroStates, rvars, num_rvars, odd);
	if (debug) print_vector(env, *vZeroStates, "zero states");
	vOneStates = mtbdd01_to_bool_vector(ddman, oneStates, rvars, num_rvars, odd);
	if (debug) print_vector(env, *vOneStates, "one states");

	PH_PrintToMainLog(env, "Allocating list of states of interest... ");
	todo = mtbdd01_to_list(ddman, statesOfInterest, rvars, num_rvars, odd);
	PH_PrintToMainLog(env, "%ld entries\n", todo->size());

	PH_PrintToMainLog(env, "Allocating list of infinity states... ");
	infStateList = mtbdd01_to_list(ddman, infStates, rvars, num_rvars, odd);
	PH_PrintToMainLog(env, "%ld entries\n", infStateList->size());

	// create solution/iteration vectors
	PH_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = new double[n];
	soln2 = new double[n];
	soln3 = new double[n];
	solnQuantiles = new double[n];
	kb = n*8.0/1024.0;
	kbt += 4*kb;
	PH_PrintMemoryToMainLog(env, "[4 x ", kb, "]\n");

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
			if (debug) {
				PH_PrintToMainLog(env, "taRew[%d] = %d\n", i, iv);
			}
			Cudd_RecursiveDeref(ddman, ta);
		} else {
			PH_SetErrorMessage("Something went quite wrong (1)");
			// TODO: Memory leak!
			return ptr_to_jlong(NULL);
		}
	}

	PH_PrintToMainLog(env, "Allocating/populating storage for state-action rewards (for %d actions)... \n", nm);
	vTsaRews = new DdNode*[nm];
	for (i = 0; i < nm; i++) {
		DdNode* cube = hddmsPositive->cubes[i];
		Cudd_Ref(cube);
		Cudd_Ref(transStateActRews);
		DdNode* tsa = DD_Apply(ddman, APPLY_TIMES, cube, transStateActRews);
		tsa = DD_MaxAbstract(ddman, tsa, ndvars, num_ndvars);
		vTsaRews[i] = tsa;
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

	std::unique_ptr<ExportIterations> iterationExport;
	if (PH_GetFlagExportIterations()) {
		iterationExport.reset(new ExportIterations("PH_NondetProbQuantile", "quantile.html"));
		iterationExport->exportVector(store.getForLevel(0), n, 0);
	}

	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;

	PH_PrintToMainLog(env, "\nStarting iterations...\n");

	iters = 0;
	// check against thresholds if we are done (for i = 0)
	for (auto it = todo->begin(); it != todo->end(); ) {
		int s = *it;
		bool sDone = false;
		switch (relOp) {
		case LT:
			sDone = vBase->getValue(s) < threshold;
			break;
		case LEQ:
			sDone = vBase->getValue(s) <= threshold;
			break;
		case GT:
			sDone = vBase->getValue(s) > threshold;
			break;
		case GEQ:
			sDone = vBase->getValue(s) >= threshold;
			break;
		}

		if (sDone) {
			solnQuantiles[s] = iters;
			it = todo->erase(it);
		} else {
			++it;
		}
	}

	done = todo->empty();

	iters = 1;
	while (!done)
	{
		double* solnPos = store.advance();

		// reset solnPos to -1.0 everywhere
		for (i = 0; i < n; i++) {
			solnPos[i] = -1.0;
		}

		PH_PrintToMainLog(env, "Quantile iteration %d\n", iters);
		long startPosRew = util_cpu_time();
		PositiveRewRecursion posRewCompute(soln3, store, *vStateRews, rvars, num_rvars, iters, lower, min);


		for (i = 0; i < nm; i++) {
			int taRew = vTaRews[i];

			for (j = 0; j < n; j++) {
				soln3[j] = -1;
			}

			posRewCompute.forAction(hddmsPositive->choices[i], taRew, vTsaRews[i]);
			if (debug) {
				std::string name = "soln3 after action " + std::to_string(i);
				print_vector(env, soln3, n, name.c_str());
			}

			// min/max
			for (j = 0; j < n; j++) {
				if (soln3[j] >= 0) {
					if (solnPos[j] < 0) {
						solnPos[j] = soln3[j];
					} else if (min) {
						if (soln3[j] < solnPos[j]) solnPos[j] = soln3[j];
					} else {
						if (soln3[j] > solnPos[j]) solnPos[j] = soln3[j];
					}
				}
			}

			if (debug) {
				std::string name = "solnPos after action " + std::to_string(i);
				print_vector(env, solnPos, n, name.c_str());
			}
		}

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
		PH_PrintToMainLog(env, "Quantile iteration %d, positive rewards in %.2f seconds\n", iters, ((double)(util_cpu_time() - startPosRew)/1000));

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

		if (hddmsZero == NULL) {
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

			ZeroRewRecursion zeroRewCompute(soln, soln3);

			// do matrix multiplication and min/max
			for (i = 0; i < nmZero; i++) {
				// start off all -1
				// (allows us to keep track of rows not visited)
				for (j = 0; j < n; j++) {
					soln3[j] = -1;
				}

				// matrix multiply
				zeroRewCompute.forAction(hddmsZero->choices[i]);

				if (debug) {
					std::string name = "soln3 for zIters = " + std::to_string(zIters) + " and action " + std::to_string(i);
					print_vector(env, soln3, n, name.c_str());
				}

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

				if (debug) {
					std::string name = "soln2 for zIters = " + std::to_string(zIters) + " and action " + std::to_string(i);
					print_vector(env, soln2, n, name.c_str());
				}
			}

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
			sup_norm = 0.0;
			for (i = 0; i < n; i++) {
				x = fabs(soln2[i] - soln[i]);
				if (term_crit == TERM_CRIT_RELATIVE) {
					x /= soln2[i];
				}
				if (x > sup_norm) sup_norm = x;
			}
			if (sup_norm < term_crit_param) {
				zDone = true;
			}

			// print occasional status update
			if ((util_cpu_time() - start3) > UPDATE_DELAY) {
				PH_PrintToMainLog(env, "Quantile iteration %d, zero reward iteration %d: max %sdiff=%f", iters, zIters, (term_crit == TERM_CRIT_RELATIVE)?"relative ":"", sup_norm);
				PH_PrintToMainLog(env, ", %.2f sec so far\n", ((double)(util_cpu_time() - start3)/1000));
				start3 = util_cpu_time();
			}

			if (debug)
				print_vector(env, soln2, n, "after iteration");

			// prepare for next iteration
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		PH_PrintToMainLog(env, "Quantile iteration %d, zero reward in %.2f seconds\n", iters, ((double)(util_cpu_time() - startZeroRew)/1000));

		// store computed values...
		for (j = 0; j < n; j++) {
			solnPos[j] = soln[j];
		}

		if (iterationExport) {
			iterationExport->exportVector(store.getForLevel(iters), n, 0);
		}

		// check against thresholds if we are done
		for (auto it = todo->begin(); it != todo->end(); ) {
			int s = *it;
			bool sDone = false;
			switch (relOp) {
			case LT:
				sDone = soln[s] < threshold;
				break;
			case LEQ:
				sDone = soln[s] <= threshold;
				break;
			case GT:
				sDone = soln[s] > threshold;
				break;
			case GEQ:
				sDone = soln[s] >= threshold;
				break;
			}

			if (sDone) {
				solnQuantiles[s] = iters;
				it = todo->erase(it);
			} else {
				++it;
			}
		}
		done = todo->empty();

		++iters;
	}

	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iterations/timing info
	PH_PrintToMainLog(env, "Quantile iterations: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);

	// if the iterative method didn't terminate, this is an error
	if (!done) { delete soln; soln = NULL; PH_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); }

	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PH_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}

	// TODO: correct cleanup on error
	if (debug) {
		PH_PrintToMainLog(env, "Results\n");
		for (i=0;i<n;i++) {
			PH_PrintToMainLog(env, "%d = %g\n", i, solnQuantiles[i]);
		}
	}

	// free memory
	if (hddmsPositive) delete hddmsPositive;
	if (hddmsZero) delete hddmsZero;

	if (vBase) delete vBase;
	if (vOneStates) delete vOneStates;
	if (vZeroStates) delete vZeroStates;
	if (vStateRews) delete vStateRews;
	if (vTaRews) delete[] vTaRews;
	if (vTsaRews) {
		for (i = 0; i < nm; i++)
			Cudd_RecursiveDeref(ddman, vTsaRews[i]);
		delete[] vTsaRews;
	}

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

	if (debug)
		print_vector(env, *vec, name);
	return vec;
}

//------------------------------------------------------------------------------
#if 0
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

#endif
