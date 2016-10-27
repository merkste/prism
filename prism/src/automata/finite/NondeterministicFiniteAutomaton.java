package automata.finite;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
/**
 * Class to store a non-deterministic finite automaton.
 */
public class NondeterministicFiniteAutomaton<Symbol> extends FiniteAutomaton<Symbol, MultiEdge<Symbol>> {
	
	
	public void generateEdges() {
		HashSet<MultiEdge<Symbol>> edges = new HashSet<>();
		for(HashSet<MultiEdge<Symbol>> outEdges : outgoingEdges.values()) {
			edges.addAll(outEdges);
		}
		this.edges = edges;
	}
	
	private void mergeOutgoingEdge(MultiEdge<Symbol> edge) {
		State source = edge.getSource();
		// Prepare map if necessary
		if(!outgoingEdges.containsKey(source)) {
			outgoingEdges.put(source, new HashSet<MultiEdge<Symbol>>());
		}
		
		// If we have outgoing edges from our new source, we
		// merge our edge into one of them with the right label.
		boolean labelFound = false;
		for(MultiEdge<Symbol> candidate : outgoingEdges.get(source)) {
			if(candidate.getLabel().equals(edge.getLabel())) {
				labelFound = true;
				candidate.getSinks().addAll(edge.getSinks());
				break;
			}
		}
		// If we cannot find the label, then the edge is the new outgoing edge.
		if(!labelFound) {
			outgoingEdges.get(source).add(edge);
		}
	}
	
	private void unmergeOutgoingEdge(MultiEdge<Symbol> edge) {
		State source = edge.getSource();
		
		MultiEdge<Symbol> outEdge = null;
		for(MultiEdge<Symbol> candidate : outgoingEdges.get(source)) {
			if(candidate.getLabel().equals(edge.getLabel())) {
				candidate.removeSinks(edge.getSinks());
				outEdge = candidate;
				break;
			}
		}
		
		outEdge.removeSinks(edge.getSinks());

		// Remove empty edges
		if(outEdge.getSinks().isEmpty()) {
			outgoingEdges.get(source).remove(outEdge);
		}
	}
	
	private void mergeIncomingEdge(MultiEdge<Symbol> edge) {
		for(State sink : edge.getSinks()) {
			// Prepare maps if necessary
			if(!incomingEdges.containsKey(sink)) {
				incomingEdges.put(sink, new HashSet<MultiEdge<Symbol>>());
			}
			incomingEdges.get(sink).add(edge);
		}
	}
	
	private void unmergeIncomingEdge(MultiEdge<Symbol> edge) {
		for(State sink : edge.getSinks()) {
			incomingEdges.get(sink).remove(edge);
		}
	}
	
	public void mergeEdge(MultiEdge<Symbol> edge) {
		edges.add(edge);
		mergeOutgoingEdge(edge);
		mergeIncomingEdge(edge);
	}
	
	public void unmergeEdge(MultiEdge<Symbol> edge) {
		edges.remove(edge);
		unmergeOutgoingEdge(edge);
		unmergeIncomingEdge(edge);
	}
	
	// Mutating functions. They always change the object in place.
	public void concat(NondeterministicFiniteAutomaton<Symbol> nfa) {
		// Incorporate the automatons structure
		incorporate(nfa);
		
		// For all initial states of nfa
		for(State initState : nfa.getInitialStates()) {
			// ... and their edges
			HashSet<MultiEdge<Symbol>> initEdgesOut = nfa.getOutgoingEdges(initState);
			HashSet<MultiEdge<Symbol>> initEdgesIn  = nfa.getIncomingEdges(initState);
			
			// [init] -> S becomes [acc] -> S
			for(MultiEdge<Symbol> e : initEdgesOut) {
				for(State accState : acceptingStates) {
					// Modify the sinks if they contain a loop
					HashSet<State> newSinks = new HashSet<>(e.getSinks());
					if(newSinks.contains(initState)) {
						newSinks.remove(initState);
						newSinks.add(accState);
					}
					mergeEdge(new MultiEdge<>(accState, e.getLabel(), newSinks));
				}
			}
			
			// S -> [init] becomes S -> [acc]
			for(MultiEdge<Symbol> e : initEdgesIn) {
				// If this edge comes from an initial state, we need to forget about it.
				HashSet<State> newSinks = new HashSet<>(acceptingStates);
				mergeEdge(new MultiEdge<>(e.getSource(), e.getLabel(), newSinks));
			}
		}
		
		// The initial states stay the same
		// The accepting states are the states of nfa
		acceptingStates = new HashSet<>(nfa.getAcceptingStates());
		
		// We need to remove the initial states and their (old) edges
		for(State initState : nfa.getInitialStates()) {
			removeState(initState);
		}
	}
	
	public void disjoin(final NondeterministicFiniteAutomaton<Symbol> nfa) {
		incorporate(nfa);
		
		initialStates.addAll(nfa.getInitialStates());
		acceptingStates.addAll(nfa.getAcceptingStates());
	}
	
	public void kleene() {
		// Make all edges going to an accepting state a restart
		for(State accState : acceptingStates) {
			for(MultiEdge<Symbol> edge : incomingEdges.get(accState)) {
				edge.addSinks(initialStates);
			}
		}
		
		acceptingStates.addAll(initialStates);
	}
	
	public void complete() {
		// Create trap state with self-loop
		State trap = newState();
		//System.out.println("The trap is " + trap);
		
		HashSet<State> trapSingleton = new HashSet<>(1);
		trapSingleton.add(trap);
		/*
		MultiEdge<Symbol> selfLoop = new MultiEdge<>(trap, new EdgeLabel<Symbol>(true), trapSingleton);
		mergeEdge(selfLoop);
		*/
		
		// This will be used to iterate over the values
		BitSet value = new BitSet();
		value.set(0, apList.size(), false);
		
		// Iterate over all possible values
		while(value.cardinality() <= apList.size()) {
			//System.out.println(value + " " + outgoingEdges);
			EdgeLabel<Symbol> label = new EdgeLabel<Symbol>(apList, value);
			for(State state : states) {
				if(getSuccessors(state, label).size() == 0) {
					MultiEdge<Symbol> trapEdge = new MultiEdge<>(state, new EdgeLabel<>(label), trapSingleton);
					//System.out.println(" Adding " + trapEdge);
					mergeEdge(trapEdge);
				}
			}
			
			// Increase value, set the first clear bit and unset everything behind
			int firstUnsetBit = value.nextClearBit(0);
			value.set(firstUnsetBit);
			for(int i = 0; i<firstUnsetBit; i++) {
				value.clear(i);
			}
		}
	}
	
	public void negate() {
		//TODO: STUB
	}
	
	/* NAVIGATION */
	@Override
	public HashSet<State> getSuccessors(State state) {
		HashSet<State> result = new HashSet<State>();
		if(!outgoingEdges.containsKey(state)) {
			outgoingEdges.put(state, new HashSet<MultiEdge<Symbol>>());
		}
		for(MultiEdge<Symbol> edge : outgoingEdges.get(state)) {
			result.addAll(edge.getSinks());
		}
		return result;
	}
	
	@Override
	public HashSet<State> getSuccessors(State state, EdgeLabel<Symbol> label) {
		MultiEdge<Symbol> edge = getEdgeByLabel(state, label);
		
		/*
		return Optional.ofNullable(edge)
				.map(MultiEdge::getDestinations)
				.orElse(new BitSet());
		*/
		
		return (edge == null) ? new HashSet<State>() : edge.getSinks();
	}
	
	public void reverse() {
		// Swap initial and accepting states
		HashSet<State> oldInitial   = initialStates;
		HashSet<State> oldAccepting = acceptingStates;
		setAcceptingStates(oldInitial);
		setInitialStates(oldAccepting);
		
		// Build reversed edges
		Iterator<MultiEdge<Symbol>> it = edges.iterator();
		Set<MultiEdge<Symbol>> toAdd = new HashSet<MultiEdge<Symbol>>();
		while(it.hasNext()) {
			//For each sink ...
			MultiEdge<Symbol> edge = it.next();
			for(State sink : edge.getSinks()) {
				// ... build a source singleton set ...
				HashSet<State> source = new HashSet<State>();
				source.add(edge.getSource());
				// ... and add the corresponding multiedge ...
				//mergeEdge(new MultiEdge<Symbol>(sink, edge.getLabel(), source));
				MultiEdge<Symbol> reversedEdge = new MultiEdge<Symbol>(sink, edge.getLabel(), source);
				toAdd.add(reversedEdge);
			}
			// Unmerge edge
			it.remove();
			//unmergeOutgoingEdge(edge);
			//unmergeIncomingEdge(edge);
		}
		
		//for(MultiEdge<Symbol> edge : edges) {
			//addEdge(edge);
		//}
		//Clear unsynchronized edges
		outgoingEdges.clear();
		incomingEdges.clear();

		edges.addAll(toAdd);
		for(MultiEdge<Symbol> edge : edges) {
				mergeOutgoingEdge(edge);
				mergeIncomingEdge(edge);
		}
	}
	
	//private void addEdge(MultiEdge<Symbol> edge) {
		//edges.add(edge);
//
		//if(!outgoingEdges.containsKey(edge.getSource())) {
			//outgoingEdges.put(edge.getSource(),
					//new LinkedHashSet<MultiEdge<Symbol>>());
		//}
		//outgoingEdges.get(edge.getSource()).add(edge);
		//
		//for(State sink : edge.getSinks()) {
			//if(!incomingEdges.containsKey(sink)) {
				//incomingEdges.put(sink,
						//new LinkedHashSet<MultiEdge<Symbol>>());
			//}
			//incomingEdges.get(sink).add(edge);
		//}
	//}

	public DeterministicFiniteAutomaton<Symbol> determinize() {
		trim(); // Remove the cruft
		//System.out.println(" After trim: " + toDot());
		
		// We need a mapping of the new states to a set of NFA states
		HashMap<State, HashSet<State>> nfaStates = new HashMap<>();
		
		DeterministicFiniteAutomaton<Symbol> dfa = new DeterministicFiniteAutomaton<Symbol>();
		
		//Take over the symbols
		dfa.setApList(apList);
		
		// The new initial state is the set of all old initial states
		State dfaInitState = dfa.newState(initialStates.toString());
		dfa.addInitialState(dfaInitState);
		nfaStates.put(dfaInitState, initialStates);
		// The new initial state is accepting iff it contains an old accepting state
		for(State initNfaState : initialStates) {
			if(isAcceptingState(initNfaState)) {
				dfa.addAcceptingState(dfaInitState);
				break;
			}
		}
		
		// Now we expand everything we have not seen yet
		LinkedList<State> toExplore = new LinkedList<>();
		toExplore.add(dfaInitState);
		
		while(!toExplore.isEmpty()) {
			State source = toExplore.remove();
			/* DEBUG *///// System.out.println("Exploring " + source);
			
			// Now we need to iterate over every possible AP combination
			int numberOfAPs = apList.size();
			
			// apToggle represents the positive/negative occurrence of APs
			BitSet apToggle = new BitSet(numberOfAPs);
			
			// theTruth signifies that every AP is important
			BitSet theTruth = new BitSet(numberOfAPs);
			theTruth.set(0, numberOfAPs);
			
			while(apToggle.length() <= numberOfAPs) {
				// Figure out if this goes somewhere
				HashSet<State> sinkNfaStates = new HashSet<>();
				EdgeLabel<Symbol> aps = new EdgeLabel<Symbol>(apList, (BitSet)apToggle.clone(), theTruth);
				/* DEBUG */// System.out.println("| labels" + aps);
				
				// For all corresponding NFA states ...
				for(State nfaState : nfaStates.get(source)) {
					// ... and all their outgoing edges ...
					//System.out.println(" >" + nfaState + "|" + outgoingEdges);
					for(MultiEdge<Symbol> nfaEdge : outgoingEdges.get(nfaState)) {
						EdgeLabel<Symbol> nfaLabel = nfaEdge.getLabel();
						// ... if the edge matches add the nfaSuccessor to the sink
						if(nfaLabel.intersects(aps)) {
							sinkNfaStates.addAll(nfaEdge.getSinks());
						}
					}
				}
				
				// Unless we did not find any successors ...
				if(!sinkNfaStates.isEmpty()) {
					// ... we first check whether we have seen them before.
					State sink = null;
					for(State otherDfaState : dfa.getStates()) {
						HashSet<State> otherNfaStates = nfaStates.get(otherDfaState);
						if(sinkNfaStates.equals(otherNfaStates)) {
							sink = otherDfaState;
							break;
						}
					}

					// If we have not seen the state before ...
					if(sink == null) {
						// ... we generate a corresponding state ...
						sink = dfa.newState(sinkNfaStates.toString());
						nfaStates.put(sink, sinkNfaStates);
						
						// ... check whether it is accepting ...
						for(State sinkNfaState : sinkNfaStates) {
							if(isAcceptingState(sinkNfaState)) {
								dfa.addAcceptingState(sink);
								break;
							}
						}
						
						// ... and we need to explore this further.
						if(!toExplore.contains(sink)) {
							toExplore.addLast(sink);
						}
						/* DEBUG */// System.out.println(" > Found a new state! " + sink);
					}
					
					// Finally we need to add the edge
					SingleEdge<Symbol> edge = new SingleEdge<Symbol>(source, aps, sink);
					dfa.addEdge(edge);
					/* DEBUG */// System.out.println(" > Adding edge " + edge);
				}
				
				// Increment apToggle (binary counting)
				int nextAP = apToggle.nextClearBit(0);
				apToggle.clear(0,nextAP);
				apToggle.set(nextAP);
			}
		}
		
		return dfa;
	};
	
	
	/**
	 * Generate a representation in GraphViz's dot format.
	 * States are black, red when accepting, blue when initial and violet when both.
	 * @return
	 */
	public String toDot() {
		StringBuffer result = new StringBuffer();
		result.append("digraph G {\n");
		
		for(State state : states) {
    		String color = "black";
    		
    		if (isAcceptingState(state)) { color = "red"; }
    		if (isInitialState(state)) { color = "blue"; }
    		if (isAcceptingState(state) && isInitialState(state)) { color = "violet"; }
    		
    		result.append(state
    				+ "[shape=box, color=" + color + ","
    				+ " label=" + state + "]\n");
		}
		
		for(MultiEdge<Symbol> edge : edges) {
			String sourceLabel = edge.getSource().toString();
			String symLabel = "\"" + edge.getLabel() + "\"";
			for(State sink : edge.getSinks()) {
				String sinkLabel = "\"" + sink + "\"";
				result.append(sourceLabel
						+ " -> " + sinkLabel
						+ "[label=" + symLabel + "];\n");
			}
		}
		
		result.append("}");
		return result.toString();
	}
}