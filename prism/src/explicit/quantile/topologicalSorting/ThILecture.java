package explicit.quantile.topologicalSorting;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import common.iterable.collections.BitSetWrapper;
import common.iterable.collections.SetFactory;
import explicit.quantile.dataStructure.RewardWrapper;

public class ThILecture extends SCCCalculator
{
	// not yet visited
	private static final byte BLUE = 0;
	// visit happens right now at this very moment
	private static final byte GREEN = 1;
	// visit already terminated
	private static final byte BLACK = 2;
	// int is a primitive data type -> no reference on it -> needs to be specified here
	private int blackeningNumber;
	//maps the blackening number to the corresponding state
	Map<Integer, Integer> blackeningNumbers;
	Map<Integer, Byte> color;
	Map<Integer, Set<Integer>> zeroRewardEdges;

	public ThILecture(RewardWrapper model, SetFactory setFactory, Set<Integer> theStates)
	{
		super(model, setFactory, theStates);
		blackeningNumbers = new HashMap<Integer, Integer>();
		blackeningNumber = 0;
		color = new HashMap<Integer, Byte>();
		for (int state : states)
			color.put(state, BLUE);
		zeroRewardEdges = new HashMap<Integer, Set<Integer>>();
	}

	@Override
	public void calculateSCCs()
	{
		for (int state : states) {
			for (int choice : model.getZeroRewardChoices(state)) {
				if (color.get(state) == BLUE)
					DFS(state);
			}
		}
		storage.notifyDone();
		depthFirstSearchInverted(flipEdges(zeroRewardEdges));
	}

	private void DFS(int state)
	{
		color.put(state, GREEN);
		for (int choice : model.getZeroRewardChoices(state)) {
			for (Iterator<Integer> successorsIterator = model.getSuccessorsIterator(state, choice); successorsIterator.hasNext();){
				int successor = successorsIterator.next();
				if (states.contains(successor)) {
					addEdge(zeroRewardEdges, state, successor);
					if (color.get(successor) == BLUE)
						DFS(successor);
				}
			}
		}
		color.put(state, BLACK);
		blackeningNumber++;
		blackeningNumbers.put(blackeningNumber, state);
		return;
	}

	private void depthFirstSearchInverted(Map<Integer, Set<Integer>> flippedEdges)
	{
		Map<Integer, Byte> color = new HashMap<Integer, Byte>();
		for (int state : states)
			color.put(state, BLUE);
		while (blackeningNumber > 0) {
			int state = blackeningNumbers.get(blackeningNumber);
			blackeningNumbers.remove(blackeningNumber);
			if (color.get(state) == BLUE)
				DFSinverted(flippedEdges, state, color);
			blackeningNumber--;
		}
		//all non-trivial SCCs are computed, therefore I need to put all remaining states into their trivial SCCs
		addTrivialSCCs();
		return;
	}

	private void DFSinverted(Map<Integer, Set<Integer>> flippedEdges, int state, Map<Integer, Byte> color)
	{
		color.put(state, GREEN);
		Set<Integer> successors = flippedEdges.get(state);
		if (successors != null) {
			for (int successor : successors) {
				if (color.get(successor) == BLUE) {
					joinSCCs(state, successor);
					DFSinverted(flippedEdges, successor, color);
				}
			}
		}
		color.put(state, BLACK);
		return;
	}

	private void addEdge(Map<Integer, Set<Integer>> edges, int state, int successor)
	{
		Set<Integer> taggedEdges = edges.get(state);
		if (taggedEdges == null)
			taggedEdges = new BitSetWrapper(new BitSet(Math.max(state, successor)));
		taggedEdges.add(successor);
		edges.put(state, taggedEdges);
		return;
	}

	private void addTrivialSCCs()
	{
		for (int state : states) {
			boolean inSCC = false;
			for (Set<Integer> SCC : storage.getSCCs()) {
				if (SCC.contains(state)) {
					inSCC = true;
					break;
				}
			}
			if (!inSCC) {
				Set<Integer> trivialSCC = setFactory.getSet();
				trivialSCC.add(state);
				storage.notifyNextSCC(trivialSCC);
			}
		}
	}

	private Map<Integer, Set<Integer>> flipEdges(Map<Integer, Set<Integer>> edges)
	{
		Map<Integer, Set<Integer>> flippedEdges = new HashMap<Integer, Set<Integer>>();
		for (int state : edges.keySet()) {
			Set<Integer> successors = edges.get(state);
			for (int successor : successors) {
				Set<Integer> predecessors = flippedEdges.get(successor);
				if (predecessors == null)
					predecessors = new BitSetWrapper(new BitSet(state+1));
				predecessors.add(state);
				flippedEdges.put(successor, predecessors);
			}
		}
		return flippedEdges;
	}

	private void joinSCCs(int state, int successor)
	{
		Set<Integer> stateSCC = getSCC(storage.getSCCs(), state, true);
		Set<Integer> successorSCC = getSCC(storage.getSCCs(), successor, true);
		//join the SCCs
		stateSCC.addAll(successorSCC);
		storage.notifyNextSCC(stateSCC);
	}

	private Set<Integer> getSCC(List<Set<Integer>> sccs, int state)
	{
		return getSCC(sccs, state, false);
	}

	private Set<Integer> getSCC(List<Set<Integer>> sccs, int state, boolean delete)
	{
		Iterator<Set<Integer>> iterator = sccs.iterator();
		while (iterator.hasNext()) {
			Set<Integer> scc = iterator.next();
			if (scc.contains(state)) {
				if (delete)
					iterator.remove();
				return scc;
			}
		}
		//a SCC containing this state was not found, therefore we return the trivial SCC for this state
		Set<Integer> stateSCC = setFactory.getSet();
		stateSCC.add(state);
		return stateSCC;
	}
}