//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
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

// includes
#include "PrismMTBDD.h"
#include <math.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include "PrismMTBDDGlob.h"
#include "jnipointer.h"
#include "ExportIterations.h"
#include "prism.h"
#include <memory>

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_mtbdd_PrismMTBDD_PM_1NondetUntilWithAlternative
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer t,	// trans matrix
jlong __jlongpointer od,	// odd
jlong __jlongpointer ndm,	// nondeterminism mask
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer ndv,	// nondet vars
jint num_ndvars,
jlong __jlongpointer y,	// 'yes' states
jlong __jlongpointer m,	// 'maybe' states
jlong __jlongpointer altstates,	// states with alternative values
jlong __jlongpointer alt,	// alternative values
jboolean min		// min or max probabilities (true = min, false = max)
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);		// trans matrix
	ODDNode *odd = jlong_to_ODDNode(od);		// odd
	DdNode *mask = jlong_to_DdNode(ndm);		// nondeterminism mask
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	DdNode **ndvars = jlong_to_DdNode_array(ndv);	// nondet vars
	DdNode *yes = jlong_to_DdNode(y);		// 'yes' states
	DdNode *maybe = jlong_to_DdNode(m);		// 'maybe' states
	DdNode *alternativeValueStates = jlong_to_DdNode(altstates);		// alternative value states
	DdNode *alternativeValues = jlong_to_DdNode(alt);		// alternative values

	// mtbdds
	DdNode *a, *sol, *tmp;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	bool done;
	int iters, i;

	// start clocks
	start1 = start2 = util_cpu_time();

	// get a - filter out rows
	PM_PrintToMainLog(env, "\nBuilding iteration matrix MTBDD... ");
	Cudd_Ref(trans);
	Cudd_Ref(maybe);
	a = DD_Apply(ddman, APPLY_TIMES, trans, maybe);
	i = DD_GetNumNodes(ddman, a);
	PM_PrintToMainLog(env, "[nodes=%d] [%.1f Kb]\n", i, i*20.0/1024.0);

	// initial solution:
	// maximum of yes and alternativeValues (where set)
	Cudd_Ref(yes);
	Cudd_Ref(alternativeValueStates);
	Cudd_Ref(alternativeValues);
	sol = DD_Apply(ddman, APPLY_TIMES, alternativeValueStates, alternativeValues);
	sol = DD_Apply(ddman, APPLY_MAX, yes, sol);

	std::unique_ptr<ExportIterations> iterationExport;
	if (PM_GetFlagExportIterations()) {
		iterationExport.reset(new ExportIterations("PM_NondetUntilWithAlternative"));
		iterationExport->exportVector(sol, rvars, num_rvars, odd, 0);
		// iterationExport->exportVector(alternativeValues, rvars, num_rvars, odd, 1);
	}

	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;

	// start iterations
	iters = 0;
	done = false;
	PM_PrintToMainLog(env, "\nStarting iterations...\n");

	while (!done && iters < max_iters) {
		
//		if (iters%20==0) {
//			PM_PrintToMainLog(env, "Iteration %d:\n", iters);
//			DD_PrintTerminalsAndNumbers(ddman, sol, num_rvars);
//		}
	
		iters++;
		
//		printf("\n\n\nsol:\n");
//		DD_ExportDDToDotFile(ddman, sol, stdout);
//		DD_PrintInfoBrief(ddman, sol, num_rvars);

		// matrix-vector multiply
		Cudd_Ref(sol);
		tmp = DD_PermuteVariables(ddman, sol, rvars, cvars, num_rvars);
		Cudd_Ref(a);
		tmp = DD_MatrixMultiply(ddman, a, tmp, cvars, num_cvars, MM_BOULDER);

//		DD_PrintInfoBrief(ddman, tmp, num_rvars+num_ndvars);
		

//		printf("tmp:\n");
//		DD_ExportDDToDotFile(ddman, tmp, stdout);

//		printf("Mask:\n");
//		DD_ExportDDToDotFile(ddman, mask, stdout);

		// do min/max
		if (min) {
			// mask stuff
			Cudd_Ref(mask);
			tmp = DD_Apply(ddman, APPLY_MAX, tmp, mask);
			// abstract
			tmp = DD_MinAbstract(ddman, tmp, ndvars, num_ndvars);
		}
		else {
			// abstract
			tmp = DD_MaxAbstract(ddman, tmp, ndvars, num_ndvars);
		}

//		printf("tmp':\n");
//		DD_ExportDDToDotFile(ddman, tmp, stdout);
//		if (iterationExport)
//			iterationExport->exportVector(tmp, rvars, num_rvars, odd, 1);

		// do min/max with alternative values
		Cudd_Ref(alternativeValues);
		if (min) {
			tmp = DD_Apply(ddman, APPLY_MIN, tmp, alternativeValues);
		} else {
			tmp = DD_Apply(ddman, APPLY_MAX, tmp, alternativeValues);
		}

		// put 1s (for 'yes' states) back into into solution vector
		Cudd_Ref(yes);
		tmp = DD_Apply(ddman, APPLY_MAX, tmp, yes);

		if (iterationExport)
			iterationExport->exportVector(sol, rvars, num_rvars, odd, 0);

		// check convergence
		switch (term_crit) {
		case TERM_CRIT_ABSOLUTE:
			if (DD_EqualSupNorm(ddman, tmp, sol, term_crit_param)) {
				done = true;
			}
			break;
		case TERM_CRIT_RELATIVE:
			if (DD_EqualSupNormRel(ddman, tmp, sol, term_crit_param)) {
				done = true;
			}
			break;
		}

		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PM_PrintToMainLog(env, "Iteration %d: ", iters);
			PM_PrintToMainLog(env, "sol=%d nodes", DD_GetNumNodes(ddman, sol));
			// NB: but tmp was probably bigger than sol (pre min/max-abstract)
			PM_PrintToMainLog(env, ", %.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
		
		// prepare for next iteration
		Cudd_RecursiveDeref(ddman, sol);
		sol = tmp;
	}
							
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;

	// print iterations/timing info
	PM_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
			
	// free memory
	Cudd_RecursiveDeref(ddman, a);
	
	// if the iterative method didn't terminate, this is an error
	if (!done) { Cudd_RecursiveDeref(ddman, sol); PM_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); return 0; }
	
	return ptr_to_jlong(sol);
}

//------------------------------------------------------------------------------
