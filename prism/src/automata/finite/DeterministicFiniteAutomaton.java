package automata.finite;

import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class DeterministicFiniteAutomaton<Symbol> extends FiniteAutomaton<Symbol,SingleEdge<Symbol>> {
	
	@Override
	public HashSet<State> getSuccessors(State state) {
		HashSet<State> result = new HashSet<State>();
		for(SingleEdge<Symbol> edge : outgoingEdges.get(state)) {
			result.add(edge.getSink());
		}
		return result;
	}
	
	@Override
	public HashSet<State> getSuccessors(State state, EdgeLabel<Symbol> label) {
		SingleEdge<Symbol> edge = getEdgeByLabel(state, label);
		HashSet<State> result = new HashSet<>();
		if(edge != null) { result.add(edge.getSink()); }
		return result;
	}
	
	public State getSuccessor(State state, EdgeLabel<Symbol> label) {
		for(State succ : getSuccessors(state, label)) {
			return succ;
		}
		return null;
	}

	public State getInitialState() {
		for(State s : getInitialStates()) {
			return s;
		}
		System.out.println("NO INITIAL STATE");
		return null;
	}
	
	public void addEdge(SingleEdge<Symbol> edge) {
		edges.add(edge);
		
		State source = edge.getSource();
		State sink   = edge.getSink();
		// Prepare maps if necessary
		if(!outgoingEdges.containsKey(source)) {
			outgoingEdges.put(source, new HashSet<SingleEdge<Symbol>>());
		}
		if(!incomingEdges.containsKey(sink)) {
			incomingEdges.put(sink, new HashSet<SingleEdge<Symbol>>());
		}

		outgoingEdges.get(source).add(edge);
		incomingEdges.get(sink).add(edge);
	}

    public NondeterministicFiniteAutomaton<Symbol> toNFA() {
        NondeterministicFiniteAutomaton<Symbol> nfa = new NondeterministicFiniteAutomaton<Symbol>();
        
        //for(Symbol ap: getApList()){
        //	nfa.addAp(ap);
        //}
        nfa.setApList(apList);

        for(State state: getStates()) {
            nfa.addState(state);
            if(isAcceptingState(state)) {
            	nfa.addAcceptingState(state);
            }
        }
		nfa.addInitialState(getInitialState());

        for(SingleEdge<Symbol> edge : edges) {
            LinkedHashSet<State> sinkSet = new LinkedHashSet<State>();
            sinkSet.add(edge.getSink());
            MultiEdge<Symbol> nfaEdge = new MultiEdge<Symbol>(edge.getSource(), edge.getLabel(), sinkSet);

            State source = edge.getSource();
            State sink = edge.getSink();
            nfa.getIncomingEdges().get(sink).add(nfaEdge);
            nfa.getOutgoingEdges().get(source).add(nfaEdge);
        }

        nfa.generateEdges();
        return nfa;
    }
    
    /**
     * DFA minimization as in Brzozowski'63
     * @return the DFA with a minimal number of states
     */
    public DeterministicFiniteAutomaton<Symbol> minimize() {
    	//reverse
    	NondeterministicFiniteAutomaton<Symbol> reversed = toNFA();
    	reversed.reverse();
    	
    	//Powerset construction for the reversed automaton
    	NondeterministicFiniteAutomaton<Symbol> minimalReversedDFA = reversed.determinize().toNFA();
    	//reverse the reversed DFA
    	minimalReversedDFA.reverse();
    	//Powerset construction for the reversed^2 automaton
    	DeterministicFiniteAutomaton<Symbol> minimized = minimalReversedDFA.determinize();
    	//Make automaton complete
    	minimized.complete();
    	return minimized;
    }
    
    public Set<State> getPredecessors(Set<State> states, EdgeLabel<Symbol> symbol) {
    	return states.stream()
    				.map(state -> getPredecessors(state, symbol))
    				.reduce(
    						new HashSet<State>(),
    						(m1, m2) -> {
    							m1.addAll(m2);
    							return m1;
    						});
    }

	public void complete() {
		trim();
		
		if(states.isEmpty()) {
			//If automaton is empty add one initial state with a true loop
			State trap = newState();
			addInitialState(trap);
			SingleEdge<Symbol> trapEdge = new SingleEdge<>(trap, new EdgeLabel<>(true) ,trap);
			addEdge(trapEdge);
		} else {
			// Create trap state with self-loop
			State trap = newState();
			//System.out.println("The trap is " + trap);
		
			/*
			MultiEdge<Symbol> selfLoop = new MultiEdge<>(trap, new EdgeLabel<Symbol>(true), trapSingleton);
			mergeEdge(selfLoop);
			*/
		
			// This will be used to iterate over the values
			BitSet value = new BitSet();
			value.set(0, apList.size(), false);
		
			// Save if an edge to the trap state has been added
			boolean edgeToTrapState = false;
			// Iterate over all possible values
			while(value.cardinality() <= apList.size()) {
				//System.out.println(value + " " + outgoingEdges);
				EdgeLabel<Symbol> label = new EdgeLabel<Symbol>(apList, value);
				for(State state : states) {
					if(getSuccessors(state, label).size() == 0) {
						SingleEdge<Symbol> trapEdge = new SingleEdge<>(state, new EdgeLabel<>(label), trap);
						addEdge(trapEdge);
						if(state != trap) {
							edgeToTrapState = true;
						}
						//System.out.println(" Adding " + trapEdge);
					}
				}
				
				// Increase value, set the first clear bit and unset everything behind
				int firstUnsetBit = value.nextClearBit(0);
				value.set(firstUnsetBit);
				for(int i = 0; i<firstUnsetBit; i++) {
					value.clear(i);
				}
			}
			
			// Remove unnecessary trap state
			if(!edgeToTrapState) {
				removeState(trap);
			}
		}
	}
}
