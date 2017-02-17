//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden, Germany)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import param.BigRational;
import parser.ast.Expression;
import parser.ast.ExpressionFilter;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;

/** Wrapper class for executing Storm model checking */
public class StormWrapper extends PrismComponent
{
	private Prism prism;
	private boolean verbose;

	/** Parametric model checking mode? */
	private boolean parametric = false;

	public StormWrapper(Prism prism)
	{
		super(prism);
		this.prism = prism;
		verbose = prism.getSettings().getBoolean(PrismSettings.PRISM_STORM_VERBOSE);
	}

	private String getStormPath()
	{
		String path = prism.getSettings().getString(PrismSettings.PRISM_STORM_PATH);
		if (path.isEmpty()) {
			path = System.getenv("STORM_BIN");
		}
		if (path == null) {
			path = "./storm";
		}

		return path;
	}

	private ArrayList<String> runStorm(List<String> arguments, boolean logOutput) throws IOException, PrismException
	{
		File storm_output = File.createTempFile("storm-", ".log", null);

		ProcessBuilder builder = new ProcessBuilder(arguments);
		builder.redirectOutput(storm_output);
		builder.redirectErrorStream(true);

		// if we are running under the Nailgun environment, setup the
		// environment to include the environment variables of the Nailgun client
		PrismNG.setupChildProcessEnvironment(builder);

		mainLog.println(">> [Executing Storm]");

		boolean first = true;
		for (String argument : arguments) {
			if (first) {
				mainLog.print(">> " + argument);
				first = false;
				continue;
			}
			if (argument.startsWith("--")) {
				mainLog.print("\n>>  " + argument + " ");
			} else {
				mainLog.print(argument);
			}
		}
		mainLog.println();

		long time = System.currentTimeMillis();
		Process p = builder.start();
		p.getInputStream().close();

		int rv;
		while (true) {
			try {
				rv = p.waitFor();
				break;
			} catch (InterruptedException e) {
			}
		}
		time = System.currentTimeMillis() - time;

		boolean error = rv != 0;

		ArrayList<String> output = new ArrayList<String>();
		BufferedReader r = new BufferedReader(new FileReader(storm_output));
		String line;
		while ((line = r.readLine()) != null) {
			output.add(line);
			if (verbose || logOutput || error) {
				prism.getLog().println(">> " + line);
			}
		}
		r.close();

		storm_output.delete();

		if (error) {
			output.add("");
			output.add("ERROR: Storm terminated with return value " + rv);
		}

		prism.getLog().println("[Storm execution time: " + (time / 1000.0) + "s]");

		return output;
	}

	private String translateExpression(Expression expr)
	{
		String asString = expr.toString();

		// hacky, for conditional:
		asString = asString.replaceAll("\\]\\[", "||");

		return asString;
	}

	class StormResult {
		public Result result;
		public String buildTime;
		public String mcTime;
		public String error;
		public String resultString;
	}

	private StormResult filterStormOutput(Expression expr, ArrayList<String> output) throws PrismException
	{
		StormResult stormResult = new StormResult();

		Pattern reResult = Pattern.compile("Result \\((.+?)\\): (.+)");
		Pattern reError = Pattern.compile("ERROR (.* caused storm to terminate.*)", Pattern.CASE_INSENSITIVE);
		Pattern reErrorRv = Pattern.compile("ERROR: (Storm terminated with return value .+)");
		Pattern reBuildTime = Pattern.compile("Time for model construction: (.+)\\.");
		Pattern reMcTime = Pattern.compile("Time for model checking: (.+)\\.");

		for (int i = 0; i < output.size(); i++) {
			String line = output.get(i);

			java.util.regex.Matcher m = reResult.matcher(line);
			if (m.matches()) {
				String explanation = m.group(1);
				String res = m.group(2);

				Pattern approxRe = Pattern.compile("(.+)\\s*\\(approx\\..+\\)");
				java.util.regex.Matcher mApprox = approxRe.matcher(res);
				if (mApprox.matches()) {
					// strip (approx. ....) part
					res = mApprox.group(1);
				}
				res = res.trim();

				Object v = null;
				if (res.equals("true") || res.equals("false")) {
					v = Boolean.parseBoolean(res);
				} else if (res.equals("inf")) {
					v = Double.POSITIVE_INFINITY;
				} else if (res.equals("-inf")) {
					v = Double.NEGATIVE_INFINITY;
				} else if (res.equals("nan")) {
					v = Double.NaN;
				} else {
					try {
						if (prism.getSettings().getBoolean(PrismSettings.PRISM_EXACT_ENABLED)) {
							v = new BigRational(res);
						} else {
							v = Double.parseDouble(res);
						}
					} catch (NumberFormatException e) {
						// Otherwise, just store the string as the result
						// e.g., for parametric model checking
						v = res;
					}
				}

				if (v != null) {
					Result result = new Result();
					result.setResult(v);
					result.setExplanation(explanation);

					// Print result to log
					String resultString = "Result";
					if (!("Result".equals(expr.getResultName())))
						resultString += " (" + expr.getResultName().toLowerCase() + ")";
					resultString += ": " + result.getResultString();
					stormResult.resultString = resultString;

					stormResult.result = result;
				}
			}

			m = reBuildTime.matcher(line);
			if (m.matches()) {
				if (stormResult.buildTime == null) {
					stormResult.buildTime = m.group(1);
				}
			}

			m = reMcTime.matcher(line);
			if (m.find()) {
				if (stormResult.mcTime == null) {
					stormResult.mcTime = m.group(1);
				}
			}

			m = reErrorRv.matcher(line);
			if (m.find()) {
				stormResult.error = m.group(1);
				return stormResult;
			}

			m = reError.matcher(line);
			if (m.find()) {
				String error = m.group(1);
				while (++i < output.size()) {
					line = output.get(i);
					if (line.equals("")) {
						// stop error parsing on single blank line
						break;
					}
					error += "\n" + line;
				}

				if (error.endsWith(".")) {
					error = error.substring(0, error.length()-1);
				}
				stormResult.error = error;
				return stormResult;
			}

			if (line.contains("skipped, because the modelling formalism is currently unsupported")) {
				stormResult.error = "Modelling formalism or property type currently not supported by storm";
				return stormResult;
			}
		}

		return stormResult;
	}

	/**
	 * Call Storm to build the model from the ModulesFile.
	 * Prints the Storm output to the log
	 */
	public void build(ModulesFile mf) throws PrismException
	{
		String model = prepareModelSource(mf, null);
		// goStorm with logging output so we can see the model statistics
		goStorm(model, null, true);
	}

	/**
	 * Check the expression for the given model via Storm.
	 * @param expr The expression
	 * @param mf The ModulesFile (for model source & constant definitions)
	 * @param pf The PropertiesFile (for label and constant definitions)
	 */
	public Result check(Expression expr, ModulesFile mf, PropertiesFile pf) throws PrismException
	{
		if (prism.getFairness()) {
			throw new PrismNotSupportedException("Storm does currently not support fairness");
		}

		String model = prepareModelSource(mf, pf);

		ArrayList<String> output = goStorm(model, translateExpression(expr), false);

		// add default filter to expression for result output
		expr = ExpressionFilter.addDefaultFilterIfNeeded(expr, mf.getInitialStates() == null);

		// filter the output and return the result
		StormResult result = filterStormOutput(expr, output);

		if (result.buildTime != null) {
			mainLog.println("Time for model construction: " + result.buildTime + " (as reported by storm)");
		}

		if (result.mcTime != null) {
			mainLog.println("Time for model checking: " + result.mcTime + " (as reported by storm)");
		}

		if (result.error != null) {
			throw new PrismNotSupportedException(result.error);
		}

		if (result.resultString != null) {
			mainLog.print("\n" + result.resultString + "\n");
		}

		return result.result;
	}

	/**
	 * Check the expression for the given model via Storm's parametric engine.
	 * @param expr The expression
	 * @param mf The ModulesFile (for model source & constant definitions)
	 * @param pf The PropertiesFile (for label and constant definitions)
	 * @param paramNames names of the parametric variables
	 * @param paramLowerBounds lower bounds on the values of the parametric variables
	 * @param paramUpperBounds upper bounds on the values of the parametric variables
	 */
	public Result checkParametric(Expression expr, ModulesFile mf, PropertiesFile pf, String[] paramNames, String[] paramLowerBounds, String[] paramUpperBounds) throws PrismException
	{
		parametric = true;

		prism.getLog().printWarning("Ignoring bounds on the parametric variables " + Arrays.toString(paramNames));

		return check(expr, mf, pf);

	}

	private String prepareModelSource(ModulesFile mf, PropertiesFile pf) throws PrismLangException
	{
		mf = (ModulesFile) mf.deepCopy();

		// we resolve constants so we don't have to pass constants to Storm
		mf = (ModulesFile) mf.replaceConstants(mf.getConstantValues());

		for (int i = 0; i < mf.getConstantValues().getNumValues(); i++) {
			// and remove all resolved constant definitions from the ModulesFile
			// the parametric constants remain (if there are any)
			mf.getConstantList().removeConstant(mf.getConstantValues().getName(i), true);
		}

		if (pf != null) {
			// replace the labels in the ModuleFile with the combined list of labels
			// in ModuleFile and PropertyFile, so that all labels are accessible to
			// Storm
			mf.setLabelList(pf.getCombinedLabelList());
		}

		String cleanedModel = mf.toString();

		return cleanedModel;
	}

	/**
	 * Call Storm with the given model and property expression.
	 *
	 * @param model the model source code
	 * @param expr the property (may be {@code null} for only building model
	 * @return the Storm output
	 */
	private ArrayList<String> goStorm(String model, String expr, boolean logOutput) throws PrismException {
		if (prism.getSettings().getBoolean(PrismSettings.PRISM_STORM_DEBUG)) {
			mainLog.println("PRISM module file (as passed to Storm):");
			mainLog.printSeparator();
			mainLog.println(model);
			mainLog.printSeparator();
		}

		try {
			File model_file = File.createTempFile("storm-", ".pm", null);
			model_file.deleteOnExit();
			FileWriter w = new FileWriter(model_file);
			w.append(model);
			w.close();

			List<String> cmdLine = new ArrayList<String>();
			cmdLine.add(getStormPath());

			cmdLine.add("--prism");
			cmdLine.add(model_file.getAbsolutePath());

			if (expr != null) {
				cmdLine.add("--prop");
				cmdLine.add(expr);
			}

			cmdLine.add("--prismcompat");

			boolean symbolicEngine = false;

			if (parametric) {
				cmdLine.add("--parametric");
			}
			// ---- exact mode? --------------------------
			else if (getSettings().getBoolean(PrismSettings.PRISM_EXACT_ENABLED)) {
				cmdLine.add("--exact");
			}
			else {
				// ---- engine switch -----------------------
				symbolicEngine = true;
				cmdLine.add("--engine");
				switch (prism.getEngine()) {
				case Prism.MTBDD:
					cmdLine.add("dd");
					break;
				case Prism.HYBRID:
					mainLog.println("\nThere is no equivalent to the hybrid engine in Storm, using MTBDD engine instead.\n");
					cmdLine.add("dd");
					break;
				case Prism.EXPLICIT:
					cmdLine.add("sparse");
					symbolicEngine = false;
					break;
				case Prism.SPARSE:
					cmdLine.add("hybrid");
					break;
				default:
					throw new PrismNotSupportedException("Unknown engine");
				}
			}

			// ---- precision ----------------------------

			cmdLine.add("--precision");
			cmdLine.add(Double.toString(prism.getTermCritParam()));

			if (prism.getTermCrit() == Prism.ABSOLUTE) {
				cmdLine.add("--absolute");
			}

			// ---- symbolic engine parameters -----------

			if (symbolicEngine) {
				long cuddMaxMem = PrismUtils.convertMemoryStringtoKB(prism.getCUDDMaxMem());
				cuddMaxMem /= 1024;
				cmdLine.add("--cudd:maxmem");
				cmdLine.add(Long.toString(cuddMaxMem));
				cmdLine.add("--sylvan:maxmem");
				cmdLine.add(Long.toString(cuddMaxMem));

				cmdLine.add("--cudd:precision");
				cmdLine.add(Double.toString(prism.getCUDDEpsilon()));
			}

			// ---- maxiters ------------------------------
			cmdLine.add("--maxiter");
			cmdLine.add(Integer.toString(prism.getMaxIters()));

			// ---- pass along extra options --------------

			String extraOpt = prism.getSettings().getString(PrismSettings.PRISM_STORM_OPTIONS);
			if (!extraOpt.isEmpty()) {
				String[] extraOpts = extraOpt.split(" +");
				for (String s : extraOpts) {
					cmdLine.add(s);
				}
			}

			// Actually run Storm
			ArrayList<String> output = runStorm(cmdLine, logOutput);

			return output;
		} catch (IOException e) {
			throw new PrismException(e.toString());
		}
	}

}
