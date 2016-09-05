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

import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;

/**
 * Class for exporting / visualising the value vectors in
 * a numerical iteration algorithm.
 *
 */
class ExportIterations {
	PrismLog log;

	public ExportIterations(String title) throws PrismException
	{
		this(title, PrismFileLog.create("iterations.html"));
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
		log.println("<body><svg></svg>");
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
			log.print(soln[i]);
		}
		log.print("]," + type + ");\n");
	}

	/** Print footer, export log */
	public void close()
	{
		log.println("init();display(0);\n</script>\n</body></html>");
	}
}