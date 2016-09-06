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

class ExportIterations {
private:
	FILE *fp;

public:
	/**
	 * Constructor, provide filename.
	 */
	ExportIterations(const char* title = "", const char* filename = "iterations.html") {
		fp = fopen(filename, "w");

		fprintf(fp, "<!DOCTYPE html>\n");
		fprintf(fp, "<html><head>\n");
		fprintf(fp, "<meta charset=\"utf-8\">\n");
		fprintf(fp, "<title>%s</title>", title);
		fprintf(fp, "<link rel='stylesheet' href='https://wwwtcs.inf.tu-dresden.de/~klein/intern/interval-vis/vis-vectors.css'>\n");
		fprintf(fp, "<script src=\"https://d3js.org/d3.v4.min.js\"></script>\n");
		fprintf(fp, "<body><svg></svg>\n");
		fprintf(fp, "<script src=\"https://wwwtcs.inf.tu-dresden.de/~klein/intern/interval-vis/vis-vectors.js\"></script>\n");
		fprintf(fp, "<script>");
	}

	/**
	 * Export the given vector, with size n and given type (0 = normal, VI from below, 1 = VI from above)
	 */
	void exportVector(double *soln, int n, int type) {
		fprintf(fp, "addVector([");
		for (int i = 0; i < n; i++) {
			if (i>0) fprintf(fp, ",");
			fprintf(fp, "%f", soln[i]);
		}
		fprintf(fp, "],0);\n");
	}

	/** Destructor, close file */
	~ExportIterations() {
		fprintf(fp, "init();\n</script>\n</body></html>\n");
		fclose(fp);
	}
};

#endif
