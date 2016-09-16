//==============================================================================
//
//	Copyright (c) 2016-
//	Authors:
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


#ifndef EXPORT_ITERATIONS_H
#define EXPORT_ITERATIONS_H

#include <cstdio>
#include <string>
#include <math.h>

#include <cudd.h>
#include <dd.h>
#include "dv.h"
#include "prism.h"
#include <odd.h>

class ExportIterations {
private:
	FILE *fp;

public:
	/**
	 * Constructor, provide filename.
	 */
	ExportIterations(const char* title = "", const char* filename = get_export_iterations_filename()) {
		fp = fopen(filename, "w");

		fprintf(fp, "<!DOCTYPE html>\n");
		fprintf(fp, "<html><head>\n");
		fprintf(fp, "<meta charset=\"utf-8\">\n");
		fprintf(fp, "<title>%s</title>\n", title);
		fprintf(fp, "<link rel='stylesheet' href='https://wwwtcs.inf.tu-dresden.de/~klein/intern/interval-vis/vis-vectors.css'>\n");
		fprintf(fp, "<script src=\"https://d3js.org/d3.v4.min.js\"></script>\n");
		fprintf(fp, "<body>\n");
		fprintf(fp, "<h1>%s</h1>\n", title);
		fprintf(fp, "<svg></svg>\n");
		fprintf(fp, "<script src=\"https://wwwtcs.inf.tu-dresden.de/~klein/intern/interval-vis/vis-vectors.js\"></script>\n");
		fprintf(fp, "<script>\n");
	}

	/**
	 * Export the given vector, with size n and given type (0 = normal, VI from below, 1 = VI from above)
	 */
	void exportVector(double *soln, int n, int type) {
		fprintf(fp, "addVector([");
		for (int i = 0; i < n; i++) {
			if (i>0) fprintf(fp, ",");
			double d = soln[i];
			if (isinf(d)) {
				if (d > 0)
					fprintf(fp, "Infinity");
				else
					fprintf(fp, "-Infinity");
			} else {
				fprintf(fp, "%.17g", soln[i]);
			}
		}
		fprintf(fp, "],%d);\n", type);
	}

	/**
	 * Export the given MTBDD vector, with num_rvars row variables,
	 * odd for reachable state space and type (0= normal, VI from below, 1 = VI from above)
	 */
	void exportVector(DdNode *dd, DdNode **rvars, int num_rvars, ODDNode* odd, int type)
	{
		double* vec = mtbdd_to_double_vector(ddman, dd, rvars, num_rvars, odd);

		// get number of states
		int n = odd->eoff + odd->toff;

		exportVector(vec, n, type);
		delete[] vec;
	}

	/** Destructor, close file */
	~ExportIterations() {
		fprintf(fp, "\ninit();\n</script>\n</body></html>\n");
		fclose(fp);
	}
};

#endif
