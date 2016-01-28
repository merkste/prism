package explicit.quantile.topologicalSorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import common.iterable.collections.SetFactory;
import explicit.quantile.dataStructure.Pair;
import explicit.quantile.dataStructure.RewardWrapper;

public class TopologicalSorting
{
	public enum QuantileSccMethod {
		TARJAN_RECURSIVE {
			@Override
			protected SCCCalculator getSCCCalculator(RewardWrapper model, Set<Integer> states)
			{
				return new TarjanRecursive(model, setFactory, states);
			}
		},
		TARJAN_ITERATIVE {
			@Override
			protected SCCCalculator getSCCCalculator(RewardWrapper model, Set<Integer> states)
			{
				return new TarjanIterative(model, setFactory, states);
			}
		},
		THI_LECTURE {
			@Override
			protected SCCCalculator getSCCCalculator(RewardWrapper model, Set<Integer> states)
			{
				return new ThILecture(model, setFactory, states);
			}

			@Override
			public boolean requiresPostProcessing()
			{
				return true;
			}
		},
		NONE {
			@Override
			protected SCCCalculator getSCCCalculator(RewardWrapper model, Set<Integer> states)
			{
				throw new RuntimeException("No proper SCC calculation method is defined");
			}
		};
		protected SetFactory setFactory;
		
		protected abstract SCCCalculator getSCCCalculator(RewardWrapper model, Set<Integer> states);

		public List<Set<Integer>> getSCCs(RewardWrapper model, Set<Integer> states)
		{
			SCCCalculator sccCalculator = getSCCCalculator(model, states);
			sccCalculator.calculateSCCs();
			return sccCalculator.getSCCs();
		}

		public boolean requiresPostProcessing()
		{
			return false;
		}
	}

	private final QuantileSccMethod sccMethod;
	protected RewardWrapper model;
	protected Set<Integer> states;
	protected Map<Integer, Set<Integer>> sccLUT;

	public TopologicalSorting(QuantileSccMethod theSccMethod, RewardWrapper theModel, SetFactory theSetFactory, Set<Integer> theStates)
	{
		sccMethod = theSccMethod;
		sccMethod.setFactory = theSetFactory;
		model = theModel;
		states = theStates;
	}

	public List<Set<Integer>> doTopologicalSorting4sequentialComputation()
	{
		List<Set<Integer>> sccs = sccMethod.getSCCs(model, states);
		if (sccMethod.requiresPostProcessing()) {
			//XXX: unschoen, das sollte ThILecture direkt uebernehmen, und nicht hier passieren
			buildSccLookUpTable(sccs);
			return calculateTopologicalSCCorder(sccs);
		}
		//Tarjan already calculates a reversed topological sorting of the SCCs
		return sccs;
	}

	public List<Set<Set<Integer>>> doTopologicalSorting4parallelComputationExactRelations(final List<Set<Integer>> sccs)
	{
		final Pair<int[], Map<Integer, Set<Integer>>> luts = buildIndexSetLookUpTables(sccs);
		final int[] stateToRepresentative = luts.getFirst();
		final Map<Integer, Set<Integer>> representativeToSet = luts.getSecond();
		final Pair<Map<Integer, Set<Integer>>, Map<Integer, Set<Integer>>> zeroRewardDagInformation = deriveZeroRewardSccDagInformationExactRelations(sccs, stateToRepresentative);
		final Map<Integer, Set<Integer>> zeroRewardPredecessors = zeroRewardDagInformation.getFirst();
		final Map<Integer, Set<Integer>> zeroRewardSuccessors = zeroRewardDagInformation.getSecond();
		final List<Set<Set<Integer>>> calculationOrder = new LinkedList<>();
		while (! zeroRewardSuccessors.isEmpty()){
			calculationOrder.add(getZeroRewardSccSinksExactRelations(zeroRewardPredecessors, zeroRewardSuccessors, representativeToSet));
		}
		return calculationOrder;
	}

	public List<Set<Set<Integer>>> doTopologicalSorting4parallelComputation(final List<Set<Integer>> sccs)
	{
		final Pair<int[], Map<Integer, Set<Integer>>> luts = buildIndexSetLookUpTables(sccs);
		final int[] stateToRepresentative = luts.getFirst();
		final Map<Integer, Set<Integer>> representativeToSet = luts.getSecond();
		final Pair<Map<Integer, Set<Integer>>, Map<Integer, Integer>> zeroRewardDagInformation = deriveZeroRewardSccDagInformation(sccs, stateToRepresentative);
		final Map<Integer, Set<Integer>> zeroRewardPredecessors = zeroRewardDagInformation.getFirst();
		final Map<Integer, Integer> zeroRewardSuccessors = zeroRewardDagInformation.getSecond();
		final List<Set<Set<Integer>>> calculationOrder = new LinkedList<>();
		while (! zeroRewardSuccessors.isEmpty()){
			calculationOrder.add(getZeroRewardSccSinks(zeroRewardPredecessors, zeroRewardSuccessors, representativeToSet));
		}
		return calculationOrder;
	}

	private Set<Set<Integer>> getZeroRewardSccSinksExactRelations(final Map<Integer, Set<Integer>> zeroRewardPredecessors, final Map<Integer, Set<Integer>> zeroRewardSuccessors, final Map<Integer, Set<Integer>> representativeToSet)
	{
		final Set<Set<Integer>> zeroRewardSinks = new HashSet<>();
		final BitSet removeInNextIteration = new BitSet();
		for (final Iterator<Integer> keyIterator = zeroRewardSuccessors.keySet().iterator(); keyIterator.hasNext();){
			final Integer key = keyIterator.next();
			if (zeroRewardSuccessors.get(key).isEmpty() && (! removeInNextIteration.get(key))){
				zeroRewardSinks.add(representativeToSet.get(key));
				keyIterator.remove();
				Set<Integer> predecessors = zeroRewardPredecessors.get(key);
				if (predecessors != null){
					for (Integer predecessor : predecessors){
						final Set<Integer> successorsOfKey = zeroRewardSuccessors.get(predecessor);
						successorsOfKey.remove(key);
						if (successorsOfKey.isEmpty()){
							removeInNextIteration.set(predecessor);
						}
					}
					zeroRewardPredecessors.remove(key);
				}
			}
		}
		return zeroRewardSinks;
	}

	private Set<Set<Integer>> getZeroRewardSccSinks(final Map<Integer, Set<Integer>> zeroRewardPredecessors, final Map<Integer, Integer> zeroRewardSuccessors, final Map<Integer, Set<Integer>> representativeToSet)
	{
		final Set<Set<Integer>> setsWithoutSuccessors = new HashSet<>();
		final BitSet removeInNextIteration = new BitSet();
		for (final Iterator<Entry<Integer, Integer>> successorsIterator = zeroRewardSuccessors.entrySet().iterator(); successorsIterator.hasNext();){
			final Entry<Integer, Integer> current = successorsIterator.next();
			final Integer index = current.getKey();
			if (current.getValue() == 0 && (! removeInNextIteration.get(index))){
				setsWithoutSuccessors.add(representativeToSet.get(index));
				successorsIterator.remove();
				Set<Integer> predecessors = zeroRewardPredecessors.get(index);
				if (predecessors != null){
					for (Integer predecessor : predecessors){
						final int numberOfSuccessors = zeroRewardSuccessors.get(predecessor) - 1;
						if (numberOfSuccessors == 0){
							removeInNextIteration.set(predecessor);
						}
						zeroRewardSuccessors.put(predecessor, numberOfSuccessors);
					}
					zeroRewardPredecessors.remove(index);
				}
			}
		}
		return setsWithoutSuccessors;
	}

	private Pair<Map<Integer, Set<Integer>>, Map<Integer, Set<Integer>>> deriveZeroRewardSccDagInformationExactRelations(final List<Set<Integer>> sccs, final int[] lut)
	{
		final int numberOfSccs = sccs.size();
		final Map<Integer, Set<Integer>> zeroRewardPredecessors = new HashMap<>(numberOfSccs);
		final Map<Integer, Set<Integer>> zeroRewardSuccessors = new HashMap<>(numberOfSccs);
		for (final Set<Integer> scc : sccs){
			final Integer representative = getRepresentative(scc);
			final Set<Integer> successors = new HashSet<>();
			for (final Integer successor : model.getZeroRewardSuccessors(scc, states)){
				final int successorRepresentative = lut[successor];
				successors.add(successorRepresentative);
				final Set<Integer> predecessors = zeroRewardPredecessors.getOrDefault(successorRepresentative, new HashSet<>());
				predecessors.add(representative);
				zeroRewardPredecessors.put(successorRepresentative, predecessors);
			}
			zeroRewardSuccessors.put(representative, successors);
		}
		return new Pair<>(zeroRewardPredecessors, zeroRewardSuccessors);
	}

	private Pair<Map<Integer, Set<Integer>>, Map<Integer, Integer>> deriveZeroRewardSccDagInformation(final List<Set<Integer>> sccs, final int[] lut)
	{
		final int numberOfSccs = sccs.size();
		final Map<Integer, Set<Integer>> zeroRewardPredecessors = new HashMap<>(numberOfSccs);
		final Map<Integer, Integer> zeroRewardSuccessors = new HashMap<>(numberOfSccs);
		for (final Set<Integer> scc : sccs){
			final Integer representative = getRepresentative(scc);
			int successors = 0;
			for (final Integer successorRepresentative : model.getZeroRewardSuccessors(scc, states, lut)){
				successors++;
				Set<Integer> predecessors = zeroRewardPredecessors.get(successorRepresentative);
				if (predecessors == null){
					//XXX:
					//XXX: ? is 5 a good choice ?
					//XXX:
					predecessors = new HashSet<>(5);
					predecessors.add(representative);
					zeroRewardPredecessors.put(successorRepresentative, predecessors);
				} else {
					predecessors.add(representative);
				}
			}
			zeroRewardSuccessors.put(representative, successors);
		}
		return new Pair<>(zeroRewardPredecessors, zeroRewardSuccessors);
	}

	private Integer getRepresentative(final Set<Integer> set)
	{
		for (final Integer index : set){
			return index;
		}
		throw new RuntimeException("The given set is empty!!!");
	}

	private Pair<int[], Map<Integer, Set<Integer>>> buildIndexSetLookUpTables(final List<Set<Integer>> indexSets)
	{
		//XXX: bei BitSetInterface kann man die Groesse auf states.length() (- 1) festsetzen, das ist immer <= getNumStates() und reicht somit aus
		final int[] stateToRepresentative = new int[model.getNumStates()];
		//XXX: eventuell kann man hierauf verzichten
		Arrays.fill(stateToRepresentative, -1);
		final Map<Integer, Set<Integer>> representativeToSet = new HashMap<>(indexSets.size());
		for (Set<Integer> indizes : indexSets){
			final Integer representative = getRepresentative(indizes);
			representativeToSet.put(representative, indizes);
			for (Integer index : indizes){
				stateToRepresentative[index] = representative;
			}
		}
		return new Pair<>(stateToRepresentative, representativeToSet);
	}

	//XXX: diese ganzen methoden sind nur fuer ThILecture da und eigentlich total haesslich
	private void buildSccLookUpTable(List<Set<Integer>> sccs)
	{
		sccLUT = new HashMap<Integer, Set<Integer>>();
		for (Set<Integer> scc : sccs)
			for (int state : scc)
				sccLUT.put(state, scc);
	}

	private List<Set<Integer>> calculateTopologicalSCCorder(List<Set<Integer>> sccs)
	{
		if (sccs.size() == 1)
			return sccs;
		List<Set<Integer>> order = pickZeroIndegreeSCCfirst(countIndegrees(sccs));
		Collections.reverse(order);
		return order;
	}

	private Map<Set<Integer>, Integer> countIndegrees(List<Set<Integer>> sccs)
	{
		Map<Set<Integer>, Integer> indegrees = new HashMap<Set<Integer>, Integer>(sccs.size());
		for (Set<Integer> scc : sccs)
			indegrees.put(scc, 0);
		for (Set<Integer> scc : sccs)
			increaseIndegreeForSCC(scc, indegrees);
		return indegrees;
	}

	private List<Set<Integer>> pickZeroIndegreeSCCfirst(Map<Set<Integer>, Integer> indegrees)
	{
		List<Set<Integer>> order = new ArrayList<Set<Integer>>();
		while (!indegrees.isEmpty()) {
			Iterator<Map.Entry<Set<Integer>, Integer>> iterator = indegrees.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<Set<Integer>, Integer> entry = iterator.next();
				Set<Integer> scc = entry.getKey();
				if (indegrees.get(scc) == 0) {
					order.add(scc);
					iterator.remove();
					if (indegrees.isEmpty())
						return order;
					decreaseIndegreeForSCC(scc, indegrees);
				}
			}
		}
		return order;
	}

	private void increaseIndegreeForSCC(Set<Integer> scc, Map<Set<Integer>, Integer> indegrees)
	{
		changeIndegreeForSCC(scc, indegrees, true);
	}

	private void decreaseIndegreeForSCC(Set<Integer> scc, Map<Set<Integer>, Integer> indegrees)
	{
		changeIndegreeForSCC(scc, indegrees, false);
	}

	private void changeIndegreeForSCC(Set<Integer> scc, Map<Set<Integer>, Integer> indegrees, boolean increase)
	{
		for (int successor : model.getZeroRewardSuccessors(scc))
			if (states.contains(successor)) {
				//pick only those successors which belong to the zero reward states
				Set<Integer> successorSCC = sccLUT.get(successor);
				if (increase)
					indegrees.put(successorSCC, indegrees.get(successorSCC) + 1);
				else
					indegrees.put(successorSCC, indegrees.get(successorSCC) - 1);
			}
	}
}