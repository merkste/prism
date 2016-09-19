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

package explicit;

import dv.DoubleVector;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;

/**
 * Class for exporting / visualising the value vectors in
 * a numerical iteration algorithm.
 *
 */
public class ExportIterations {
	private static String defaultFilename = "iterations.html";

	private PrismLog log;

	public ExportIterations(String title) throws PrismException
	{
		this(title, PrismFileLog.create(defaultFilename));
	}

	/**
	 * Constructor.
	 * @param log the log used for export
	 */
	public ExportIterations(String title, PrismLog log)
	{
		this.log = log;

		log.println("<!DOCTYPE html>");
		log.println("<html><head>");
		log.println("<meta charset=\"utf-8\">");
		log.println("<title>" + title + "</title>");
		log.println("<link rel='stylesheet' href='https://wwwtcs.inf.tu-dresden.de/~klein/intern/interval-vis/vis-vectors.css'>");
		log.println("<script src=\"https://d3js.org/d3.v4.min.js\"></script>");
		log.println("<body>");
		log.println("<h1>" + title + "</h1>");
		log.println("<svg></svg>");
		log.println("<script src=\"https://wwwtcs.inf.tu-dresden.de/~klein/intern/interval-vis/vis-vectors.js\"></script>");
		log.println("<script>");
	}

	/**
	 * Export the given vector.
	 * @param soln the value vector
	 */
	public void exportVector(double[] soln)
	{
		exportVector(soln, 0);
	}

	/**
	 * Export the given vector.
	 * @param soln the value vector
	 * @param type the vector type (0 = normal, VI from below, 1 = VI from above)
	 */
	public void exportVector(double[] soln, int type) 
	{
		log.print("addVector([");
		for (int i = 0; i < soln.length; i++) {
			if (i>0) log.print(",");
			double d = soln[i];
			exportValue(d);
		}
		log.print("]," + type + ");\n");
	}

	/**
	 * Export the given vector.
	 * @param soln the value vector
	 */
	public void exportVector(DoubleVector soln)
	{
		exportVector(soln, 0);
	}

	/**
	 * Export the given vector.
	 * @param soln the value vector
	 * @param type the vector type (0 = normal, VI from below, 1 = VI from above)
	 */
	public void exportVector(DoubleVector soln, int type) 
	{
		log.print("addVector([");
		for (int i = 0, n = soln.getSize(); i < n; i++) {
			if (i>0) log.print(",");
			double d = soln.getElement(i);
			exportValue(d);
		}
		log.print("]," + type + ");\n");
	}

	private void exportValue(double d)
	{
		if (d == Double.POSITIVE_INFINITY) {
			log.print("Infinity");
		} else if (d == Double.NEGATIVE_INFINITY) {
			log.print("-Infinity");
		} else {
			log.print(d);
		}
	}

/** Print footer, export log */
	public void close()
	{
		log.println("init();\n</script>\n</body></html>");
	}

	public static void setDefaultFilename(String newDefaultFilename)
	{
		defaultFilename = newDefaultFilename;
	}

	public static String getDefaultFilename()
	{
		return defaultFilename;
	}

	public static void resetDefaultFilename()
	{
		defaultFilename = "iterations.html";
	}
}
