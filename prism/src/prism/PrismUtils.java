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

package prism;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.PrimitiveIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.iterable.FunctionalPrimitiveIterable.IterableInt;

/**
 * Various general-purpose utility methods in Java
 */
public class PrismUtils
{
	// Threshold for comparison of doubles
	public static double epsilonDouble = 1e-12;

	/**
	 * Compute logarithm of x to base b.
	 */
	public static double log(double x, double b)
	{
		// If base is <=0 or ==1 (or +Inf/NaN), then result is NaN
		if (b <= 0 || b == 1 || (Double.isInfinite(b)) || Double.isNaN(b))
			return Double.NaN;

		// Otherwise, log_b (x) is log(x) / log(b)
		return Math.log(x) / Math.log(b);
	}

	/**
	 * Compute logarithm of x to base 2.
	 */
	public static double log2(double x)
	{
		return Math.log(x) / Math.log(2);
	}

	/**
	 * See if two doubles are within epsilon of each other (absolute error).
	 */
	public static boolean doublesAreCloseAbs(double d1, double d2, double epsilon)
	{
		// Deal with infinite cases
		if (Double.isInfinite(d1)) {
			return Double.isInfinite(d2) && (d1 > 0) == (d2 > 0);
		} else if (Double.isInfinite(d2)) {
			return false;
		}
		// Compute/check error
		return (Math.abs(d1 - d2) < epsilon);
	}

	/**
	 * See if two doubles are within epsilon of each other (relative error).
	 */
	public static boolean doublesAreCloseRel(double d1, double d2, double epsilon)
	{
		// Deal with infinite cases
		if (Double.isInfinite(d1)) {
			return Double.isInfinite(d2) && (d1 > 0) == (d2 > 0);
		} else if (Double.isInfinite(d2)) {
			return false;
		}
		// Compute/check error
		d1 = Math.abs(d1);
		d2 = Math.abs(d2);
		// For two (near) zero values, return true, for just one, return false
		if (d1 < epsilonDouble)
			return (d2 < epsilonDouble);
		return (Math.abs(d1 - d2) / d1 < epsilon);
	}

	/**
	 * See if two doubles are within epsilon of each other (relative or absolute error).
	 * @param abs Absolute if true, relative if false
	 */
	public static boolean doublesAreClose(double d1, double d2, double epsilon, boolean abs)
	{
		if (abs) {
			return doublesAreCloseAbs(d1, d2, epsilon);
		} else {
			return doublesAreCloseRel(d1, d2, epsilon);
		}
	}

	/**
	 * See if two arrays of doubles are all within epsilon of each other (relative or absolute error).
	 */
	public static boolean doublesAreClose(double d1[], double d2[], double epsilon, boolean abs)
	{
		int n = Math.min(d1.length, d2.length);
		if (abs) {
			for (int i = 0; i < n; i++) {
				if (!doublesAreCloseAbs(d1[i], d2[i], epsilon))
					return false;
			}
		} else {
			for (int i = 0; i < n; i++) {
				if (!doublesAreCloseRel(d1[i], d2[i], epsilon))
					return false;
			}
		}
		return true;
	}

	/**
	 * See if, for all the entries given by the {@code indizes}
	 * iterator, two arrays of doubles are all within epsilon of each other (relative or absolute error).
	 * <br>
	 * Considers Inf == Inf and -Inf == -Inf.
	 */
	public static boolean doublesAreClose(double d1[], double d2[], IterableInt indizes, double epsilon, boolean abs)
	{
		return doublesAreClose(d1, d2, indizes.iterator(), epsilon, abs);
	}

	/**
	 * See if, for all the entries given by the {@code indizes}
	 * iterator, two arrays of doubles are all within epsilon of each other (relative or absolute error).
	 * <br>
	 * Considers Inf == Inf and -Inf == -Inf.
	 */
	public static boolean doublesAreClose(double d1[], double d2[], PrimitiveIterator.OfInt indizes, double epsilon, boolean abs)
	{
		if (abs) {
			while (indizes.hasNext()) {
				int i = indizes.nextInt();
				if (!doublesAreCloseAbs(d1[i], d2[i], epsilon))
					return false;
			}
		} else {
			while (indizes.hasNext()) {
				int i = indizes.nextInt();
				if (!doublesAreCloseRel(d1[i], d2[i], epsilon))
					return false;
			}
		}
		return true;
	}

	/**
	 * See if two doubles are (nearly) equal.
	 */
	public static boolean doublesAreEqual(double d1, double d2)
	{
		return doublesAreCloseAbs(d1, d2, epsilonDouble);
	}

	/**
	 * Normalise the given entries in the vector in-place such that that they sum to 1,
	 * I.e., for all indizes of entries, set<br>
	 * {@code vector[s] = vector[s] / sum}, where<br>
	 * {@code sum = sum_{s in entries} (vector[s])<br>
	 * If {@code sum = 0.0}, all entries are set to {@code NaN}.
	 * @param vector the vector
	 * @param entries Iterable over the entries (must not contain duplicates)
	 * @return the altered vector
	 */
	public static double[] normalise(double[] vector, IterableInt entries)
	{
		double sum = 0.0;
		for (PrimitiveIterator.OfInt iter = entries.iterator(); iter.hasNext();) {
			int state = iter.nextInt();
			sum += vector[state];
		}
		for (PrimitiveIterator.OfInt iter = entries.iterator(); iter.hasNext();) {
			int state = iter.nextInt();
			vector[state] /= sum;
		}
		return vector;
	}

	/**
	 * Format a large integer, represented by a double, as a string. Un
	 */
	public static String bigIntToString(double d)
	{
		if (d <= Long.MAX_VALUE) {
			return "" + Math.round(d);
		} else {
			return "" + d;
		}
	}

	/**
	 * Modify a filename f, appending a counter i just before the filetype extension. 
	 */
	public static String addCounterSuffixToFilename(String f, int i)
	{
		return addSuffixToFilename(f, "" + i);
	}

	/**
	 * Modify a filename f, appending a string s just before the filetype extension. 
	 */
	public static String addSuffixToFilename(String f, String s)
	{
		int j = f.lastIndexOf(".");
		if (j != -1) {
			return f.substring(0, j) + s + f.substring(j);
		} else {
			return f + s;
		}
	}

	/**
	 * Format a fraction as a percentage to 1 decimal place.
	 */
	public static String formatPercent1dp(double frac)
	{
		return formatterPercent1dp.format(frac);
	}

	private static DecimalFormat formatterPercent1dp = new DecimalFormat("#0.0%", DecimalFormatSymbols.getInstance(Locale.UK));

	/**
	 * Format a double to 2 decimal places.
	 */
	public static String formatDouble2dp(double d)
	{
		return formatterDouble2dp.format(d);
	}

	private static DecimalFormat formatterDouble2dp = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.UK));

	/**
	 * Format a double, as would be done by printf's %.17g.
	 * Preserving full double precision requires 17 = ceil(log(2^(52+1))) + 1 decimal places,
	 * since the mantissa has 52+1 bits and one additional place is needed to tell close values apart.
	 */
	public static String formatDouble(double d)
	{
		return formatDouble(17, d);
	}

	/**
	 * Format a double, as would be done by printf's %.(prec)g
	 * @param prec precision (significant digits) >= 1
	 */
	public static String formatDouble(int prec, double d)
{
	if (prec < 1)
		throw new IllegalArgumentException("Precision has to be >= 1; got " + prec);
	// Use no locale to avoid . being changed to , in some countries.
	// To match C's printf, we have to tweak the Java version,
	// strip trailing zeros after the .
	String result = String.format((Locale)null, "%." + prec + "g", d);
	// if there are only zeros after the . (e.g., .000000), strip them including the .
	result = result.replaceFirst("\\.0+(e|$)", "$1");
	// handle .xxxx0000
	// we first match .xxx until there are only zeros before the end (or e)
	// as we match reluctantly (using the *?), all trailing zeros are captured
	// by the 0+ part
	return result.replaceFirst("(\\.[0-9]*?)0+(e|$)", "$1$2");
}

	/**
	 * Create a string for a list of objects, with a specified separator,
	 * e.g. ["a","b","c"], "," -&gt; "a,b,c"
	 */
	public static String joinString(List<?> objs, String separator)
	{
		String s = "";
		boolean first = true;
		for (Object obj : objs) {
			if (first) {
				first = false;
			} else {
				s += separator; 
			}
			s += obj.toString();
		}
		return s;
	}
	
	/**
	 * Create a string for an array of objects, with a specified separator,
	 * e.g. ["a","b","c"], "," -&gt; "a,b,c"
	 */
	public static String joinString(Object[] objs, String separator)
	{
		String s = "";
		boolean first = true;
		for (Object obj : objs) {
			if (first) {
				first = false;
			} else {
				s += separator; 
			}
			s += obj.toString();
		}
		return s;
	}
	
	/**
	 * Check for any cycles in an 2D boolean array representing a graph.
	 * Useful for checking for cyclic dependencies in connected definitions.
	 * Returns the lowest index of a node contained in a cycle, if there is one, -1 if not.  
	 * @param matrix Square matrix of connections: {@code matr[i][j] == true} iff
	 * there is a connection from {@code i} to {@code j}.
	 */
	public static int findCycle(boolean matrix[][])
	{
		int n = matrix.length;
		int firstCycle = -1;
		// Go through nodes
		for (int i = 0; i < n; i++) {
			// See if there is a cycle yet
			for (int j = 0; j < n; j++) {
				if (matrix[j][j]) {
					firstCycle = j;
					break;
				}
			}
			// If so, stop
			if (firstCycle != -1)
				break;
			// Extend dependencies
			for (int j = 0; j < n; j++) {
				for (int k = 0; k < n; k++) {
					if (matrix[j][k]) {
						for (int l = 0; l < n; l++) {
							matrix[j][l] |= matrix[k][l];
						}
					}
				}
			}
		}
		return firstCycle;
	}

	/**
	 * Convert a string representing an amount of memory (e.g. 125k, 50m, 4g) to the value in KB.
	 * If the letter prefix is omitted, we assume it is "k" (i.e. KB).
	 */
	public static long convertMemoryStringtoKB(String mem) throws PrismException
	{
		Pattern p = Pattern.compile("([0-9]+)([kmg]?)");
		Matcher m = p.matcher(mem);
		if (!m.matches()) {
			throw new PrismException("Invalid amount of memory \"" + mem + "\"");
		}
		long num;
		try {
			num = Long.parseLong(m.group(1));
		} catch (NumberFormatException e) {
			throw new PrismException("Invalid amount of memory \"" + mem + "\"");
		}
		switch (m.group(2)) {
		case "":
		case "k":
			return num;
		case "m":
			return num * 1024;
		case "g":
			return num * (1024 * 1024);
		default:
			// Shouldn't happen
			throw new PrismException("Invalid amount of memory \"" + mem + "\"");
		}
	}

	/**
	 * Convert a string representing an amount of time (e.g. 5s, 5m, 5h, 5d, 5w) to the value
	 * in seconds.
	 * If the unit is omitted, we assume it is seconds.
	 */
	public static int convertTimeStringtoSeconds(String time) throws PrismException
	{
		Pattern p = Pattern.compile("([0-9]+)([smhdw]?)");
		Matcher m = p.matcher(time);
		if (!m.matches()) {
			throw new PrismException("Invalid time value \"" + time + "\"");
		}
		int value;
		try {
			value = Integer.parseInt(m.group(1));
		} catch (NumberFormatException e) {
			throw new PrismException("Invalid time value \"" + time + "\"");
		}
		switch (m.group(2)) {
		case "":
		case "s":  // seconds
			return value;
		case "m":  // minutes
			return value * 60;
		case "h":  // hours
			return value * (60 * 60);
		case "d":  // days
			return value * (24 * 60 * 60);
		case "w":  // weeks
			return value * (7 * 24 * 60 * 60);
		default:
			// Shouldn't happen
			throw new PrismException("Invalid time value \"" + time + "\"");
		}
	}

	/**
	 * Convert a number of bytes to a string representing the amount of memory (e.g. 125k, 50m, 4g).
	 */
	public static String convertBytesToMemoryString(long bytes) throws PrismException
	{
		String units[] = { "b", "k", "m", "g" };
		for (int i = 3; i > 0; i--) {
			long pow = 1 << (i * 10);
			if (bytes >= pow) {
				return (bytes % pow == 0 ? (bytes / pow) : String.format(Locale.UK, "%.1f", ((double) bytes) / pow)) + units[i];
			}
		}
		return bytes + units[0];
		
		/*for (String s : new String[] { "1g", "1500m", "2g", "1000m", "1024m", "1" }) {
			System.out.println(s + " => " + PrismUtils.convertMemoryStringtoKB(s) * 1024 + " => " + PrismUtils.convertBytesToMemoryString(PrismUtils.convertMemoryStringtoKB(s) * 1024));
		}*/
	}
	
	/**
	 * Utility method to create a new PrintStream for a file, but any errors are converted to PrismExceptions
	 */
	public static PrintStream newPrintStream(String filename) throws PrismException
	{
		try {
			return new PrintStream(filename);
		} catch (FileNotFoundException e) {
			throw new PrismException("File \"" + filename + "\" could not opened for output");
		}
	}

	/**
	 * Compare two version numbers of PRISM (strings).
	 * Example ordering: { "1", "2.0", "2.1.alpha", "2.1.alpha.r5555", "2.1.alpha.r5557", "2.1.beta", "2.1.beta4", "2.1", "2.1.dev", "2.1.dev.r6666", "2.1.dev1", "2.1.dev2", "2.1.2", "2.9", "3", "3.4"};
	 * Returns: 1 if v1&gt;v2, -1 if v1&lt;v2, 0 if v1=v2
	 */
	public static int compareVersions(String v1, String v2)
	{
		String ss1[], ss2[], tmp[];
		int i, n, x;
		double s1 = 0, s2 = 0;
		boolean s1num, s2num;

		// Exactly equal
		if (v1.equals(v2))
			return 0;
		// Otherwise split into sections
		ss1 = v1.split("\\.");
		ss2 = v2.split("\\.");
		// Pad if one is shorter
		n = Math.max(ss1.length, ss2.length);
		if (ss1.length < n) {
			tmp = new String[n];
			for (i = 0; i < ss1.length; i++)
				tmp[i] = ss1[i];
			for (i = ss1.length; i < n; i++)
				tmp[i] = "";
			ss1 = tmp;
		}
		if (ss2.length < n) {
			tmp = new String[n];
			for (i = 0; i < ss2.length; i++)
				tmp[i] = ss2[i];
			for (i = ss2.length; i < n; i++)
				tmp[i] = "";
			ss2 = tmp;
		}
		// Loop through sections of string
		for (i = 0; i < n; i++) {
			// 2.1.alpha < 2.1, etc.
			// 2.1.alpha < 2.1.alpha2 < 2.1.alpha3, etc.
			// so replace alphax with -10000+x
			if (ss1[i].matches("alpha.*")) {
				try {
					if (ss1[i].length() == 5)
						x = 0;
					else
						x = Integer.parseInt(ss1[i].substring(5));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss1[i] = "" + (-10000 + x);
			}
			if (ss2[i].matches("alpha.*")) {
				try {
					if (ss2[i].length() == 5)
						x = 0;
					else
						x = Integer.parseInt(ss2[i].substring(5));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss2[i] = "" + (-10000 + x);
			}
			// 2.1.beta < 2.1, etc.
			// 2.1.beta < 2.1.beta2 < 2.1.beta3, etc.
			// so replace betax with -100+x
			if (ss1[i].matches("beta.*")) {
				try {
					if (ss1[i].length() == 4)
						x = 0;
					else
						x = Integer.parseInt(ss1[i].substring(4));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss1[i] = "" + (-100 + x);
			}
			if (ss2[i].matches("beta.*")) {
				try {
					if (ss2[i].length() == 4)
						x = 0;
					else
						x = Integer.parseInt(ss2[i].substring(4));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss2[i] = "" + (-100 + x);
			}
			// 2 < 2.1, etc.
			// so treat 2 as 2.0
			if (ss1[i].equals(""))
				ss1[i] = "0";
			if (ss2[i].equals(""))
				ss2[i] = "0";
			// 2.1 < 2.1.dev, etc.
			// 2.1.dev < 2.1.dev2 < 2.1.dev3, etc.
			// so replace devx with 0.5+x/1000
			if (ss1[i].matches("dev.*")) {
				try {
					if (ss1[i].length() == 3)
						x = 0;
					else
						x = Integer.parseInt(ss1[i].substring(3));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss1[i] = "" + (0.5 + x / 1000.0);
			}
			if (ss2[i].matches("dev.*")) {
				try {
					if (ss2[i].length() == 3)
						x = 0;
					else
						x = Integer.parseInt(ss2[i].substring(3));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss2[i] = "" + (0.5 + x / 1000.0);
			}
			// replace rx (e.g. as in 4.0.alpha.r5555) with x
			if (ss1[i].matches("r.*")) {
				try {
					x = Integer.parseInt(ss1[i].substring(1));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss1[i] = "" + x;
			}
			if (ss2[i].matches("r.*")) {
				try {
					x = Integer.parseInt(ss2[i].substring(1));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss2[i] = "" + x;
			}
			// See if strings are integers
			try {
				s1num = true;
				s1 = Double.parseDouble(ss1[i]);
			} catch (NumberFormatException e) {
				s1num = false;
			}
			try {
				s2num = true;
				s2 = Double.parseDouble(ss2[i]);
			} catch (NumberFormatException e) {
				s2num = false;
			}
			if (s1num && s2num) {
				if (s1 < s2)
					return -1;
				if (s1 > s2)
					return 1;
				if (s1 == s2)
					continue;
			}
		}

		return 0;
	}
}

//------------------------------------------------------------------------------
