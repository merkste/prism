//==============================================================================
//	
//	Copyright (c) 2015-
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

package common;

import prism.PrismLog;

/**
 * Stop watch for keeping track of the runtime of some computation,
 * optionally printing the elapsed time to the log.
 */
public class StopWatch
{
	/** The log */
	private PrismLog log;

	/** An (optional) task description */
	private String taskDescription;

	/** For storing the time */
	private long time = 0;

	/** Constructor, no log and no output */
	public StopWatch()
	{
	}

	/** Constructor, stores log for output */
	public StopWatch(PrismLog log)
	{
		this.log = log;
	}

	/** Start the stop watch (without task description) */
	public void start()
	{
		start(null);
	}

	/** Start the stop watch, store task description (may be {@code null}) */
	public void start(String taskDescription)
	{
		this.taskDescription = taskDescription;
		time = System.currentTimeMillis();
	}

	/**
	 * Stop the stop watch.
	 * If a task description and a log was given, output
	 * elapsed time.
	 */
	public void stop()
	{
		stop(null);
	}

	/**
	 * Stop the stop watch, optionally taking extra text for output.
	 * If a log and a task description / extra text was given, output
	 * elapsed time.
	 */
	public void stop(String extraText)
	{
		time = System.currentTimeMillis() - time;
		if (log != null) {
			if (taskDescription != null) {
				log.print("Time for " + taskDescription + ": " + time / 1000.0 + " seconds");
				if (extraText == null) {
					log.println(".");
				} else {
					log.print(extraText);
					log.println(".");
				}
			} else if (extraText != null) {
				log.print("Time : " + time / 1000.0 + " seconds "+extraText+".");
			}
		}
	}

	/** Get the number of elapsed milliseconds (after having called stop). */
	public long elapsedMillis()
	{
		return time;
	}
}
