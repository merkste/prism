package explicit.modelviews;

import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;
import java.util.TreeMap;
import java.util.function.IntFunction;

import common.IterableStateSet;
import common.iterable.FunctionalIterator;
import common.iterable.MappingIterator;
import explicit.DTMC;
import explicit.DTMCExplicit;
import explicit.Distribution;
import explicit.rewards.MCRewards;
import prism.Pair;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;

public abstract class MCView extends ModelView implements DTMC, Cloneable
{
	public MCView()
	{
		super();
	}

	public MCView(final ModelView model)
	{
		super(model);
	}



	//--- Object ---

	@Override
	public String toString()
	{
		final IntFunction<Entry<Integer, Distribution>> getDistribution = new IntFunction<Entry<Integer, Distribution>>()
		{
			@Override
			public final Entry<Integer, Distribution> apply(final int state)
			{
				final Distribution distribution = new Distribution(getTransitionsIterator(state));
				return new AbstractMap.SimpleImmutableEntry<>(state, distribution);
			}
		};
		String s = "trans: [ ";
		final IterableStateSet states = new IterableStateSet(getNumStates());
		final Iterator<Entry<Integer, Distribution>> distributions = states.iterator().map(getDistribution);
		while (distributions.hasNext()) {
			final Entry<Integer, Distribution> dist = distributions.next();
			s += dist.getKey() + ": " + dist.getValue();
			if (distributions.hasNext()) {
				s += ", ";
			}
		}
		return s + " ]";
	}



	//--- Model ---

	@Override
	public int getNumTransitions()
	{
		// FIXME ALG: use sum abstraction ?
		int numTransitions = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			numTransitions += getNumTransitions(state);
		}
		return numTransitions;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		final Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state);
		return new MappingIterator.From<>(transitions, Entry::getKey);
	}

	@Override
	public void exportToPrismExplicitTra(final PrismLog log, int precision)
	{
		// Output transitions to .tra file
		log.print(getNumStates() + " " + getNumTransitions() + "\n");
		final TreeMap<Integer, Double> sorted = new TreeMap<>();
		for (int state = 0, max = getNumStates(); state < max; state++) {
			// Extract transitions and sort by destination state index (to match PRISM-exported files)
			for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
				final Entry<Integer, Double> transition = transitions.next();
				sorted.put(transition.getKey(), transition.getValue());
			}
			// Print out (sorted) transitions
			for (Entry<Integer, Double> transition : sorted.entrySet()) {
				// Note use of PrismUtils.formatDouble to match PRISM-exported files
				log.print(state + " " + transition.getKey() + " " + PrismUtils.formatDouble(precision,transition.getValue()) + "\n");
			}
			sorted.clear();
		}
	}

	@Override
	public void exportToPrismLanguage(final String filename) throws PrismException
	{
		try (FileWriter out = new FileWriter(filename)) {
			out.write(getModelType().keyword() + "\n");
			out.write("module M\nx : [0.." + (getNumStates() - 1) + "];\n");
			final TreeMap<Integer, Double> sorted = new TreeMap<Integer, Double>();
			for (int state = 0, max = getNumStates(); state < max; state++) {
				// Extract transitions and sort by destination state index (to match PRISM-exported files)
				for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
					final Entry<Integer, Double> transition = transitions.next();
					sorted.put(transition.getKey(), transition.getValue());
				}
				// Print out (sorted) transitions
				out.write("[]x=" + state + "->");
				boolean first = true;
				for (Entry<Integer, Double> transition : sorted.entrySet()) {
					if (first)
						first = false;
					else
						out.write("+");
					// Note use of PrismUtils.formatDouble to match PRISM-exported files
					out.write(PrismUtils.formatDouble(transition.getValue()) + ":(x'=" + transition.getKey() + ")");
				}
				out.write(";\n");
				sorted.clear();
			}
			out.write("endmodule\n");
		} catch (IOException e) {
			throw new PrismException("Could not export " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	@Override
	public String infoString()
	{
		String s = "";
		s += getNumStates() + " states (" + getNumInitialStates() + " initial)";
		s += ", " + getNumTransitions() + " transitions";
		return s;
	}

	@Override
	public String infoStringTable()
	{
		String s = "";
		s += "States:      " + getNumStates() + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		return s;
	}

	



	//--- DTMC ---

	@Override
	public int getNumTransitions(final int state)
	{
		return FunctionalIterator.extend(getTransitionsIterator(state)).count();
	}

	@Override
	public Iterator<Entry<Integer, Pair<Double, Object>>> getTransitionsAndActionsIterator(final int state)
	{
		final Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state);
		return new MappingIterator.From<>(transitions, transition -> DTMCExplicit.attachAction(transition, null));
	}

	@Override
	public void prob0step(final BitSet subset, final BitSet u, final BitSet result)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates()).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			boolean hasTransitionToU = false;
			for (Iterator<Integer> successors = getSuccessorsIterator(state); successors.hasNext();) {
				if (u.get(successors.next())) {
					hasTransitionToU = true;
					break;
				}
			}
			result.set(state, hasTransitionToU);
		}
	}

	@Override
	public void prob1step(final BitSet subset, final BitSet u, final BitSet v, final BitSet result)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates()).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			boolean allTransitionsToU = true;
			boolean hasTransitionToV = false;
			for (Iterator<Integer> successors = getSuccessorsIterator(state); successors.hasNext();) {
				final int successor = successors.next();
				if (!u.get(successor)) {
					allTransitionsToU = false;
					break;
				}
				hasTransitionToV = hasTransitionToV || v.get(successor);
			}
			result.set(state, allTransitionsToU && hasTransitionToV);
		}
	}

	@Override
	public void mvMult(final double[] vect, final double[] result, final BitSet subset, final boolean complement)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			result[state] = mvMultSingle(state, vect);
		}
	}

	@Override
	public double mvMultSingle(final int state, final double[] vect)
	{
		double d = 0.0;
		for (final Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
			final Entry<Integer, Double> transition = transitions.next();
			final int target = transition.getKey();
			final double probability = transition.getValue();
			d += probability * vect[target];
		}
		return d;
	}

	@Override
	public double mvMultGS(final double[] vect, final BitSet subset, final boolean complement, final boolean absolute)
	{
		double maxDiff = 0.0;
		for (OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			final double d = mvMultJacSingle(state, vect);
			final double diff = absolute ? (Math.abs(d - vect[state])) : (Math.abs(d - vect[state]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[state] = d;
		}
		// Use this code instead for backwards Gauss-Seidel
		/*for (int state : new IterableStateSet(subset, getNumStates(), complement).backwards()) {
			final double d = mvMultJacSingle(state, vect);
			final double diff = absolute ? (Math.abs(d - vect[state])) : (Math.abs(d - vect[state]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[state] = d;
		}*/
		return maxDiff;
	}

	@Override
	public double mvMultGS(double vect[], IterableStateSet subset, boolean absolute)
	{
		double maxDiff = 0.0;
		for (OfInt states = subset.iterator(); states.hasNext();) {
			int s = states.nextInt();
			double d = mvMultJacSingle(s, vect);
			double diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[s] = d;
		}
		// Use this code instead for backwards Gauss-Seidel
		/*for (s = numStates - 1; s >= 0; s--) {
			if (subset.get(s)) {
				d = mvMultJacSingle(s, vect);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}*/
		return maxDiff;
	}

	@Override
	public double mvMultJacSingle(final int state, final double[] vect)
	{
		double diag = 1.0;
		double d = 0.0;
		for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
			final Entry<Integer, Double> transition = transitions.next();
			final int target = transition.getKey();
			final double probability = transition.getValue();
			if (target != state) {
				d += probability * vect[target];
			} else {
				diag -= probability;
			}
		}
		if (diag > 0) {
			d /= diag;
		}
		return d;
	}

	@Override
	public void mvMultRew(final double[] vect, final MCRewards mcRewards, final double[] result, final BitSet subset, final boolean complement)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			result[state] = mvMultRewSingle(state, vect, mcRewards);
		}
	}

	@Override
	public double mvMultRewSingle(final int state, final double[] vect, final MCRewards mcRewards)
	{
		double d = mcRewards.getStateReward(state);
		for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
			final Entry<Integer, Double> transition = transitions.next();
			int target = transition.getKey();
			double probability = transition.getValue();
			d += probability * vect[target];
		}
		return d;
	}

	@Override
	public void vmMult(final double[] vect, final double[] result)
	{
		// Initialise result to 0
		Arrays.fill(result, 0);
		// Go through matrix elements (by row)
		for (int state = 0, max = getNumStates(); state < max; state++) {
			for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
				final Entry<Integer, Double> transition = transitions.next();
				final int target = transition.getKey();
				final double prob = transition.getValue();
				result[target] += prob * vect[state];
			}
		}
	}



	//--- ModelView ---

	/**
	 * @see explicit.DTMCExplicit#exportTransitionsToDotFile(int, PrismLog) DTMCExplicit
	 **/
	@Override
	protected void exportTransitionsToDotFile(final int state, final PrismLog out)
	{
		for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
			final Entry<Integer, Double> transition = transitions.next();
			out.print(state + " -> " + transition.getKey() + " [ label=\"");
			out.print(transition.getValue() + "\" ];\n");
		}
	}
}