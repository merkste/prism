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

#ifndef DV_H
#define DV_H

//------------------------------------------------------------------------------

#include <util.h>
#include <cudd.h>
#include <odd.h>
#include <vector>
#include <list>

// Flags for building Windows DLLs
#ifdef __MINGW32__
	#define EXPORT __declspec(dllexport)
#else
	#define EXPORT
#endif

// constants

#define DV_GREATER_THAN_EQUALS	1
#define DV_GREATER_THAN			2
#define DV_LESS_THAN_EQUALS		3
#define DV_LESS_THAN			4
#define DV_INTERVAL				5
#define DV_CLOSE_ABS			6
#define DV_CLOSE_REL			7

// distinct vectors

typedef struct DistVector DistVector;

struct DistVector
{
	double *dist;
	int num_dist;
	unsigned short *ptrs;
	
	EXPORT DistVector();
	EXPORT ~DistVector();
};

/**
 * Wrapper for a DistVector (compact) or a plain double vector.
 * Destroys the vector on destruction.
 */
struct PlainOrDistVector
{
	const double *vec;
	const DistVector *dist;
	const long n; // number of states in the model

	/** Constructor, store double[] vector (deleted on destruction) */
	PlainOrDistVector(double *vec, long n) : vec(vec), dist(NULL), n(n) {}

	/** Constructor, store DistVector (deleted on destruction) */
	PlainOrDistVector(DistVector *dist, long n) : vec(NULL), dist(dist), n(n) {}

	/** Destructor, free the vector */
	~PlainOrDistVector()
	{
		if (compact()) {
			delete dist;
		} else {
			delete[] vec;
		}
	}

	/** Is the vector a compact vector (DistVector) */
	bool compact() const
	{
		return dist != NULL;
	}

	double getValue(int i) const
	{
		if (compact()) {
			return dist->dist[dist->ptrs[i]];
		} else {
			return vec[i];
		}
	}

	/** Get the memory requirements for the vector in kB*/
	double getKb() const
	{
		if (compact()) {
			return (dist->num_dist*8.0+n*2.0)/1024.0;
		} else {
			return n*8.0/1024.0;
		}
	}

};

// function prototypes

EXPORT double *mtbdd_to_double_vector(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd);
EXPORT double *mtbdd_to_double_vector(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd, double *res);
EXPORT PlainOrDistVector* mtbdd_to_plain_or_dist_vector(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd, bool tryToCompact);
EXPORT std::vector<bool>* mtbdd01_to_bool_vector(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd);
EXPORT std::list<long>* mtbdd01_to_list(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd);
EXPORT DdNode *double_vector_to_mtbdd(DdManager *ddman, double *vec, DdNode **vars, int num_vars, ODDNode *odd);
EXPORT DdNode *double_vector_to_bdd(DdManager *ddman, double *vec, int rel_op, double value, DdNode **vars, int num_vars, ODDNode *odd);
EXPORT DdNode *double_vector_to_bdd(DdManager *ddman, double *vec, int rel_op, double value1, double value2, DdNode **vars, int num_vars, ODDNode *odd);

EXPORT void filter_double_vector(DdManager *ddman, double *vec, DdNode *filter, double d, DdNode **vars, int num_vars, ODDNode *odd);
EXPORT void max_double_vector_mtbdd(DdManager *ddman, double *vec, DdNode *vec2, DdNode **vars, int num_vars, ODDNode *odd);
EXPORT double get_first_from_bdd(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, ODDNode *odd);
EXPORT double min_double_vector_over_bdd(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, ODDNode *odd);
EXPORT double max_double_vector_over_bdd(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, ODDNode *odd);
EXPORT double sum_double_vector_over_bdd(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, ODDNode *odd);
EXPORT double sum_double_vector_over_mtbdd(DdManager *ddman, double *vec, DdNode *mult, DdNode **vars, int num_vars, ODDNode *odd);
EXPORT void sum_double_vector_over_dd_vars(DdManager *ddman, double *vec, double *vec2, DdNode **vars, int num_vars, int first_var, int last_var, ODDNode *odd, ODDNode *odd2);

EXPORT DistVector *double_vector_to_dist(double *v, int n);

EXPORT bool doubles_are_close_abs(double d1, double d2, double epsilon);
EXPORT bool doubles_are_close_rel(double d1, double d2, double epsilon);

//------------------------------------------------------------------------------

#endif

//------------------------------------------------------------------------------
