package explicit.modelviews;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;
import java.util.TreeMap;

import common.IterableStateSet;
import common.iterable.ChainedIterator;
import common.iterable.FunctionalIterator;
import common.iterable.MappingIterator;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;
import strat.MDStrategy;
import explicit.DTMCFromMDPAndMDStrategy;
import explicit.Distribution;
import explicit.IncomingChoiceRelationSparseCombined;
import explicit.MDP;
import explicit.Model;
import explicit.PredecessorRelation;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;

public abstract class MDPView extends ModelView implements MDP, Cloneable
{
	public MDPView()
	{
		super();
	}

	public MDPView(final MDPView model)
	{
		super(model);
	}



	//--- Object ---

	@Override
	public String toString()
	{
		String s = "[ ";
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			if (state > 0)
				s += ", ";
			s += state + ": ";
			s += "[";
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				if (choice > 0)
					s += ",";
				final Object action = getAction(state, choice);
				if (action != null)
					s += action + ":";
				s += new Distribution(getTransitionsIterator(state, choice));
			}
			s += "]";
		}
		s += " ]";
		return s;
	}



	//--- Model ---

	@Override
	public ModelType getModelType()
	{
		return ModelType.MDP;
	}

	@Override
	public int getNumTransitions()
	{
		// FIXME ALG: use sum abstraction ?
		int numTransitions = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++)
				numTransitions += getNumTransitions(state, choice);
		}
		return numTransitions;
	}

	@Override
	public void exportToPrismExplicitTra(final PrismLog out, int precision)
	{
		final int numStates = getNumStates();
		// Output transitions to .tra file
		out.print(numStates + " " + getNumChoices() + " " + getNumTransitions() + "\n");
		final TreeMap<Integer, Double> sorted = new TreeMap<>();
		for (int state = 0; state < numStates; state++) {
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				// Extract transitions and sort by destination state index (to match PRISM-exported files)
				for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
					final Entry<Integer, Double> trans = transitions.next();
					sorted.put(trans.getKey(), trans.getValue());
				}
				// Print out (sorted) transitions
				for (Entry<Integer, Double> e : sorted.entrySet()) {
					// Note use of PrismUtils.formatDouble to match PRISM-exported files
					out.print(state + " " + choice + " " + e.getKey() + " " + PrismUtils.formatDouble(precision,e.getValue()));
					final Object action = getAction(state, choice);
					out.print(action == null ? "\n" : (" " + action + "\n"));
				}
				sorted.clear();
			}
		}
	}

	@Override
	public void exportToPrismLanguage(final String filename) throws PrismException
	{
		try (FileWriter out = new FileWriter(filename)) {
			// Output transitions to PRISM language file
			out.write(getModelType().keyword() + "\n");
			final int numStates = getNumStates();
			out.write("module M\nx : [0.." + (numStates - 1) + "];\n");
			final TreeMap<Integer, Double> sorted = new TreeMap<Integer, Double>();
			for (int state = 0; state < numStates; state++) {
				for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
					// Extract transitions and sort by destination state index (to match PRISM-exported files)
					for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
						final Entry<Integer, Double> trans = transitions.next();
						sorted.put(trans.getKey(), trans.getValue());
					}
					// Print out (sorted) transitions
					final Object action = getAction(state, choice);
					out.write(action != null ? ("[" + action + "]") : "[]");
					out.write("x=" + state + "->");
					boolean first = true;
					for (Entry<Integer, Double> e : sorted.entrySet()) {
						if (first)
							first = false;
						else
							out.write("+");
						// Note use of PrismUtils.formatDouble to match PRISM-exported files
						out.write(PrismUtils.formatDouble(e.getValue()) + ":(x'=" + e.getKey() + ")");
					}
					out.write(";\n");
					sorted.clear();
				}
			}
			out.write("endmodule\n");
		} catch (IOException e) {
			throw new PrismException("Could not export " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	@Override
	public String infoString()
	{
		final int numStates = getNumStates();
		String s = "";
		s += numStates + " states (" + getNumInitialStates() + " initial)";
		s += ", " + getNumTransitions() + " transitions";
		s += ", " + getNumChoices() + " choices";
		s += ", dist max/avg = " + getMaxNumChoices() + "/" + PrismUtils.formatDouble2dp(((double) getNumChoices()) / numStates);
		return s;
	}

	@Override
	public String infoStringTable()
	{
		final int numStates = getNumStates();
		String s = "";
		s += "States:      " + numStates + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		s += "Choices:     " + getNumChoices() + "\n";
		s += "Max/avg:     " + getMaxNumChoices() + "/" + PrismUtils.formatDouble2dp(((double) getNumChoices()) / numStates) + "\n";
		return s;
	}

	@Override
	public IncomingChoiceRelationSparseCombined getPredecessorRelation(PrismComponent parent, boolean storeIfNew)
	{
		if (predecessorRelation != null) {
			return (IncomingChoiceRelationSparseCombined) predecessorRelation;
		}

		final PredecessorRelation pre = IncomingChoiceRelationSparseCombined.forModel(parent, this);

		if (storeIfNew) {
			predecessorRelation = pre;
		}
		return (IncomingChoiceRelationSparseCombined) pre;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		final int numChoices = getNumChoices(state);
		@SuppressWarnings("unchecked")
		final Iterator<Integer>[] successorIterators = new Iterator[numChoices];
		for (int choice = 0; choice < numChoices; choice++) {
			successorIterators[choice] = getSuccessorsIterator(state, choice);
		}

		return new ChainedIterator.Of<>(successorIterators).distinct();
	}



	//--- NondetModel ---

	@Override
	public int getNumChoices()
	{
		// FIXME ALG: use sum abstraction ?
		int numChoices = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			numChoices += getNumChoices(state);
		}
		return numChoices;
	}

	@Override
	public int getMaxNumChoices()
	{
		// FIXME ALG: use some abstraction IteratorTools.max ?
		int maxNumChoices = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			maxNumChoices = Math.max(maxNumChoices, getNumChoices(state));
		}
		return maxNumChoices;
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		final HashSet<Object> actions = new HashSet<Object>();
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			final int numChoices = getNumChoices(state);
			if (numChoices <= 1) {
				continue;
			}
			actions.clear();
			for (int choice = 0; choice < numChoices; choice++) {
				if (!actions.add(getAction(state, choice))) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int getNumTransitions(final int state, final int choice)
	{
		return FunctionalIterator.extend(getTransitionsIterator(state, choice)).count();
	}

	@Override
	public boolean allSuccessorsInSet(final int state, final int choice, final BitSet set)
	{
		for (Iterator<Integer> successors = getSuccessorsIterator(state, choice); successors.hasNext();) {
			if (!set.get(successors.next())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean someSuccessorsInSet(final int state, final int choice, final BitSet set)
	{
		for (Iterator<Integer> successors = getSuccessorsIterator(state, choice); successors.hasNext();) {
			if (set.get(successors.next())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		final Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice);
		return new MappingIterator.ToInt<>(transitions, Entry::getKey);
	}

	@Override
	public Model constructInducedModel(final MDStrategy strat)
	{
		return new DTMCFromMDPAndMDStrategy(this, strat);
	}

	@Override
	public void exportToDotFileWithStrat(final PrismLog out, final BitSet mark, final int[] strat, int precision)
	{
		out.print("digraph " + getModelType() + " {\nsize=\"8,5\"\nnode [shape=box];\n");
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			if (mark != null && mark.get(state))
				out.print(state + " [style=filled  fillcolor=\"#cccccc\"]\n");
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				final String style = (strat[state] == choice) ? ",color=\"#ff0000\",fontcolor=\"#ff0000\"" : "";
				final Object action = getAction(state, choice);
				final String nij = "n" + state + "_" + choice;
				out.print(state + " -> " + nij + " [ arrowhead=none,label=\"" + choice);
				if (action != null) {
					out.print(":" + action);
				}
				out.print("\"" + style + " ];\n");
				out.print(nij + " [ shape=point,height=0.1,label=\"\"" + style + " ];\n");
				for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
					Entry<Integer, Double> trans = transitions.next();
					out.print(nij + " -> " + trans.getKey() + " [ label=\"" + trans.getValue() + "\"" + style + " ];\n");
				}
			}
		}
		out.print("}\n");
	}



	//--- MDP ---

	@Override
	public void prob0step(final BitSet subset, final BitSet u, final boolean forall, final BitSet result)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates()).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			boolean value = forall; // there exists or for all
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				value = someSuccessorsInSet(state, choice, u);
				if (value != forall) {
					break;
				}
			}
			result.set(state, value);
		}
	}

	@Override
	public void prob1Astep(final BitSet subset, final BitSet u, final BitSet v, final BitSet result)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates()).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			boolean value = true;
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				if (!(allSuccessorsInSet(state, choice, u) && someSuccessorsInSet(state, choice, v))) {
					value = false;
					break;
				}
			}
			result.set(state, value);
		}
	}

	@Override
	public void prob1Estep(final BitSet subset, final BitSet u, final BitSet v, final BitSet result, final int strat[])
	{
		int stratCh = -1;
		for (OfInt states = new IterableStateSet(subset, getNumStates()).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			boolean value = false;
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				if (allSuccessorsInSet(state, choice, u) && someSuccessorsInSet(state, choice, v)) {
					value = true;
					// If strategy generation is enabled, remember optimal choice
					if (strat != null) {
						stratCh = choice;
					}
					break;
				}
			}
			// If strategy generation is enabled, store optimal choice
			// (only if this the first time we add the state to S^yes)
			if (strat != null && value && !result.get(state)) {
				strat[state] = stratCh;
			}
			// Store result
			result.set(state, value);
		}
	}

	@Override
	public void prob1step(final BitSet subset, final BitSet u, final BitSet v, final boolean forall, final BitSet result)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates()).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			boolean value = forall; // there exists or for all
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				value = someSuccessorsInSet(state, choice, v) && allSuccessorsInSet(state, choice, u);
				if (value != forall) {
					break;
				}
			}
			result.set(state, value);
		}
	}

	@Override
	public boolean prob1stepSingle(final int state, final int choice, final BitSet u, final BitSet v)
	{
		return someSuccessorsInSet(state, choice, v) && allSuccessorsInSet(state, choice, u);
	}

	@Override
	public void mvMultMinMax(final double[] vect, final boolean min, final double[] result, final BitSet subset, final boolean complement, final int[] strat)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			result[state] = mvMultMinMaxSingle(state, vect, min, strat);
		}
	}

	@Override
	public void mvMultMinMax(final double[] vect, final boolean min, final double[] result, final IterableStateSet subset, final int[] strat)
	{
		for (OfInt states = subset.iterator(); states.hasNext();) {
			final int state = states.nextInt();
			result[state] = mvMultMinMaxSingle(state, vect, min, strat);
		}
	}

	@Override
	public double mvMultMinMaxSingle(final int state, final double[] vect, final boolean min, final int[] strat)
	{
		int stratCh = -1;
		double minmax = 0;
		boolean first = true;

		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			// Compute sum for this distribution
			double d = 0.0;
			for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				final Entry<Integer, Double> trans = transitions.next();
				final int target = trans.getKey();
				final double probability = trans.getValue();
				d += probability * vect[target];
			}

			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null)
					stratCh = choice;
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[state] = stratCh;
			} else if (strat[state] == -1 || minmax > vect[state]) {
				strat[state] = stratCh;
			}
		}

		return minmax;
	}

	@Override
	public List<Integer> mvMultMinMaxSingleChoices(final int state, final double vect[], final boolean min, final double val)
	{
		// Create data structures to store strategy
		final List<Integer> result = new ArrayList<Integer>();
		// One row of matrix-vector operation 
		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			// Compute sum for this distribution
			double d = 0.0;
			for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				final Entry<Integer, Double> trans = transitions.next();
				final int target = trans.getKey();
				final double probability = trans.getValue();
				d += probability * vect[target];
			}
			// Store strategy info if value matches
			if (PrismUtils.doublesAreClose(val, d, 1e-12, false)) {
				result.add(choice);
			}
		}

		return result;
	}

	@Override
	public double mvMultSingle(final int state, final int choice, final double[] vect)
	{
		// Compute sum for this distribution
		double d = 0.0;
		for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
			final Entry<Integer, Double> trans = transitions.next();
			final int target = trans.getKey();
			final double probability = trans.getValue();
			d += probability * vect[target];
		}

		return d;
	}

	@Override
	public double mvMultGSMinMax(final double[] vect, final boolean min, final BitSet subset, final boolean complement, final boolean absolute,
			final int[] strat)
	{
		double maxDiff = 0.0;

		for (OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			final double d = mvMultJacMinMaxSingle(state, vect, min, strat);
			final double diff = absolute ? (Math.abs(d - vect[state])) : (Math.abs(d - vect[state]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[state] = d;
		}
		// Use this code instead for backwards Gauss-Seidel
		/*for (s = numStates - 1; s >= 0; s--) {
			if (subset.get(s)) {
				d = mvMultJacMinMaxSingle(s, vect, min, strat);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}*/
		return maxDiff;
	}

	@Override
	public double mvMultGSMinMax(final double[] vect, final boolean min, IterableStateSet subset, final boolean absolute,
			final int[] strat)
	{
		double maxDiff = 0.0;

		for (OfInt states = subset.iterator(); states.hasNext();) {
			final int state = states.nextInt();
			final double d = mvMultJacMinMaxSingle(state, vect, min, strat);
			final double diff = absolute ? (Math.abs(d - vect[state])) : (Math.abs(d - vect[state]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[state] = d;
		}
		// Use this code instead for backwards Gauss-Seidel
		/*for (s = numStates - 1; s >= 0; s--) {
			if (subset.get(s)) {
				d = mvMultJacMinMaxSingle(s, vect, min, strat);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}*/
		return maxDiff;
	}

	@Override
	public double mvMultJacMinMaxSingle(final int state, final double[] vect, final boolean min, final int[] strat)
	{
		int stratCh = -1;
		double minmax = 0;
		boolean first = true;

		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			double diag = 1.0;
			// Compute sum for this distribution
			double d = 0.0;
			for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				final Entry<Integer, Double> trans = transitions.next();
				final int target = trans.getKey();
				final double probability = trans.getValue();
				if (target != state) {
					d += probability * vect[target];
				} else {
					diag -= probability;
				}
			}
			if (diag > 0)
				d /= diag;
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null) {
					stratCh = choice;
				}
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[state] = stratCh;
			} else if (strat[state] == -1 || minmax > vect[state]) {
				strat[state] = stratCh;
			}
		}

		return minmax;
	}

	@Override
	public double mvMultJacSingle(final int state, final int choice, final double[] vect)
	{
		double diag = 1.0;
		// Compute sum for this distribution
		double d = 0.0;
		for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
			final Entry<Integer, Double> trans = transitions.next();
			final int target = trans.getKey();
			final double prob = trans.getValue();
			if (target != state) {
				d += prob * vect[target];
			} else {
				diag -= prob;
			}
		}
		if (diag > 0)
			d /= diag;

		return d;
	}

	@Override
	public void mvMultRewMinMax(final double[] vect, final MDPRewards mdpRewards, final boolean min, final double[] result, final BitSet subset,
			final boolean complement, final int[] strat)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			result[state] = mvMultRewMinMaxSingle(state, vect, mdpRewards, min, strat);
		}
	}

	@Override
	public void mvMultRewMinMax(final double[] vect, final MDPRewards mdpRewards, final boolean min, final double[] result, final IterableStateSet subset,
			final int[] strat)
	{
		for (OfInt states = subset.iterator(); states.hasNext();) {
			final int state = states.nextInt();
			result[state] = mvMultRewMinMaxSingle(state, vect, mdpRewards, min, strat);
		}
	}

	@Override
	public double mvMultRewMinMaxSingle(final int state, final double[] vect, final MDPRewards mdpRewards, final boolean min, final int[] strat)
	{
		int stratCh = -1;
		double minmax = 0;
		boolean first = true;

		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			// Compute sum for this distribution
			double d = mdpRewards.getTransitionReward(state, choice);
			for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				final Entry<Integer, Double> trans = transitions.next();
				final int target = trans.getKey();
				final double probability = trans.getValue();
				d += probability * vect[target];
			}
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null)
					stratCh = choice;
			}
			first = false;
		}
		// Add state reward (doesn't affect min/max)
		minmax += mdpRewards.getStateReward(state);
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[state] = stratCh;
			} else if (strat[state] == -1 || minmax > vect[state]) {
				strat[state] = stratCh;
			}
		}

		return minmax;
	}

	@Override
	public double mvMultRewSingle(final int state, final int choice, final double[] vect, final MCRewards mcRewards)
	{
		// Compute sum for this distribution
		// TODO: use transition rewards when added to DTMCss
		// d = mcRewards.getTransitionReward(s);
		double d = 0;
		for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
			final Entry<Integer, Double> trans = transitions.next();
			final int target = trans.getKey();
			final double probabiltiy = trans.getValue();
			d += probabiltiy * vect[target];
		}
		d += mcRewards.getStateReward(state);

		return d;
	}

	@Override
	public double mvMultRewGSMinMax(final double[] vect, final MDPRewards mdpRewards, final boolean min, final BitSet subset, final boolean complement,
			final boolean absolute, final int[] strat)
	{
		double maxDiff = 0.0;

		for (OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			final double d = mvMultRewJacMinMaxSingle(state, vect, mdpRewards, min, strat);
			double diff = absolute ? (Math.abs(d - vect[state])) : (Math.abs(d - vect[state]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[state] = d;
		}
		// Use this code instead for backwards Gauss-Seidel
		/*for (s = numStates - 1; s >= 0; s--) {
			if (subset.get(s)) {
				d = mvMultRewJacMinMaxSingle(s, vect, mdpRewards, min);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}*/
		return maxDiff;
	}

	@Override
	public double mvMultRewGSMinMax(final double[] vect, final MDPRewards mdpRewards, final boolean min, final IterableStateSet subset,
			final boolean absolute, final int[] strat)
	{
		double maxDiff = 0.0;

		for (OfInt states = subset.iterator(); states.hasNext();) {
			final int state = states.nextInt();
			final double d = mvMultRewJacMinMaxSingle(state, vect, mdpRewards, min, strat);
			double diff = absolute ? (Math.abs(d - vect[state])) : (Math.abs(d - vect[state]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[state] = d;
		}
		// Use this code instead for backwards Gauss-Seidel
		/*for (s = numStates - 1; s >= 0; s--) {
			if (subset.get(s)) {
				d = mvMultRewJacMinMaxSingle(s, vect, mdpRewards, min);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}*/
		return maxDiff;
	}

	@Override
	public double mvMultRewJacMinMaxSingle(final int state, final double[] vect, final MDPRewards mdpRewards, final boolean min, final int[] strat)
	{
		int stratCh = -1;
		double minmax = 0;
		boolean first = true;

		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			double diag = 1.0;
			// Compute sum for this distribution
			double d = mdpRewards.getTransitionReward(state, choice);
			for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				final Entry<Integer, Double> trans = transitions.next();
				final int target = trans.getKey();
				final double prob = trans.getValue();
				if (target != state) {
					d += prob * vect[target];
				} else {
					diag -= prob;
				}
			}
			if (diag > 0)
				d /= diag;
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null) {
					stratCh = choice;
				}
			}
			first = false;
		}
		// Add state reward (doesn't affect min/max)
		minmax += mdpRewards.getStateReward(state);
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[state] = stratCh;
			} else if (strat[state] == -1 || minmax > vect[state]) {
				strat[state] = stratCh;
			}
		}

		return minmax;
	}

	@Override
	public List<Integer> mvMultRewMinMaxSingleChoices(final int state, final double[] vect, final MDPRewards mdpRewards, final boolean min, final double val)
	{
		// Create data structures to store strategy
		final List<Integer> result = new ArrayList<Integer>();

		// One row of matrix-vector operation
		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			// Compute sum for this distribution
			double d = mdpRewards.getTransitionReward(state, choice);
			for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				final Entry<Integer, Double> trans = transitions.next();
				final int target = trans.getKey();
				final double probability = trans.getValue();
				d += probability * vect[target];
			}
			d += mdpRewards.getStateReward(state);
			// Store strategy info if value matches
			//if (PrismUtils.doublesAreClose(val, d, termCritParam, termCrit == TermCrit.ABSOLUTE)) {
			if (PrismUtils.doublesAreClose(val, d, 1e-12, false)) {
				result.add(choice);
				//res.add(distrs.getAction());
			}
		}

		return result;
	}

	@Override
	public void mvMultRight(final int[] states, final int[] strat, final double[] source, final double[] dest)
	{
		for (int state : states) {
			for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, strat[state]); transitions.hasNext();) {
				final Entry<Integer, Double> trans = transitions.next();
				final int target = trans.getKey();
				final double probability = trans.getValue();
				dest[target] += probability * source[state];
			}
		}
	}



	//--- ModelView ---

	/**
	 * @see explicit.MDPExplicit#exportTransitionsToDotFile(int, PrismLog) MDPExplicit
	 **/
	@Override
	protected void exportTransitionsToDotFile(int state, PrismLog out)
	{
		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			final Object action = getAction(state, choice);
			final String nij = "n" + state + "_" + choice;
			out.print(state + " -> " + nij + " [ arrowhead=none,label=\"" + choice);
			if (action != null)
				out.print(":" + action);
			out.print("\" ];\n");
			out.print(nij + " [ shape=point,width=0.1,height=0.1,label=\"\" ];\n");
			for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				Entry<Integer, Double> trans = transitions.next();
				out.print(nij + " -> " + trans.getKey() + " [ label=\"" + trans.getValue() + "\" ];\n");
			}
		}
	}
}