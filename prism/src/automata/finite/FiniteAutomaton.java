package automata.finite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

public abstract class FiniteAutomaton<Symbol, E extends Edge<Symbol>> {
	protected List<Symbol> apList;
	
	protected HashSet<State> states;
	protected HashSet<E> edges;
	
	protected HashMap<State, HashSet<E>> outgoingEdges;
	protected HashMap<State, HashSet<E>> incomingEdges;
	
	protected HashSet<State> initialStates;
	protected HashSet<State> acceptingStates;
	
	public FiniteAutomaton()
	{
		apList = new ArrayList<>();
		
		initialStates   = new HashSet<>();
		acceptingStates = new HashSet<>();
		
		states = new HashSet<>();
		edges = new HashSet<>();
		
		outgoingEdges = new HashMap<>();
		incomingEdges = new HashMap<>();
	}
	
	public HashSet<State> getStates() {
		return states;
	}

	public void setStates(HashSet<State> states) {
		this.states = states;
	}

	public HashMap<State, HashSet<E>> getOutgoingEdges() {
		return outgoingEdges;
	}

	public void setOutgoingEdges(HashMap<State, HashSet<E>> outgoingEdges) {
		this.outgoingEdges = outgoingEdges;
	}

	public HashMap<State, HashSet<E>> getIncomingEdges() {
		return incomingEdges;
	}

	public void setIncomingEdges(HashMap<State, HashSet<E>> incomingEdges) {
		this.incomingEdges = incomingEdges;
	}
	
	public List<Symbol> getApList() {
		return apList;
	}

	public void setApList(List<Symbol> apList) {
		this.apList = apList;
	}
	
	public void addAp(Symbol ap) {
		this.apList.add(ap);
	}
	
	protected HashSet<E> getEdges() {
		return edges;
	}

	protected void setEdges(HashSet<E> edges) {
		this.edges = edges;
	}
	
	public void addInitialState(State state) {
		initialStates.add(state);
	}
	
	public void addAcceptingState(State state) {
		acceptingStates.add(state);
	}
	
	protected HashSet<State> getInitialStates() {
		return initialStates;
	}

	protected void setInitialStates(HashSet<State> initialStates) {
		this.initialStates = initialStates;
	}

	protected HashSet<State> getAcceptingStates() {
		return acceptingStates;
	}

	protected void setAcceptingStates(HashSet<State> acceptingStates) {
		this.acceptingStates = acceptingStates;
	}
	
	public State newState() {
		State state = new State();
		return addState(state);
	}
	
	public State newState(String name) {
		State state = new State(name);
		return addState(state);
	}
	
	public State addState(State state) {
		states.add(state);
		
		outgoingEdges.put(state, new HashSet<E>());
		incomingEdges.put(state, new HashSet<E>());
		
		return state;
	}
	
	public void removeState(State state) {
		// Remove from state sets
		states.remove(state);
		acceptingStates.remove(state);
		initialStates.remove(state);
		
		// Regenerate all the edges
		ArrayList<E> incidingEdges = new ArrayList<>();
		for(E edge : edges) {
			if(edge.containsState(state)) {
				incidingEdges.add(edge);
			}
		}

		for(E edge : incidingEdges) {
			removeEdge(edge);
		}
			
		outgoingEdges.remove(state);
		incomingEdges.remove(state);
	}
	
	public void removeEdge(E edge) {
		edges.remove(edge);
		for(HashSet<E> inedges  : incomingEdges.values()) {
			inedges.remove(edge);
		}
		for(HashSet<E> outedges : outgoingEdges.values()) {
			outedges.remove(edge);
		}
	}
	
	public E getEdgeByLabel(State state, EdgeLabel<Symbol> label) {
		for(E candidate : outgoingEdges.get(state)) {
			if(candidate.getLabel().intersects(label)) {
				return candidate;
			}
		}
		// This Edge goes nowhere
		return null;
	}
	
	public HashSet<E> getOutgoingEdges(State state) {
		return outgoingEdges.get(state);
	}
	
	public HashSet<E> getIncomingEdges(State state) {
		return incomingEdges.get(state);
	}
	
	protected void incorporate(final FiniteAutomaton<Symbol,E> other) {
		states.addAll(other.getStates());
		edges.addAll(other.getEdges());
		outgoingEdges.putAll(other.getOutgoingEdges());
		incomingEdges.putAll(other.getIncomingEdges());
		for(Symbol ap : other.getApList()) {
			if(!apList.contains(ap)) { apList.add(ap); }
		}
	}
	
	public boolean isAcceptingState(State state) {
		return acceptingStates.contains(state);
	}
	
	public boolean isInitialState(State state) {
		return initialStates.contains(state);
	}
	
	public abstract HashSet<State> getSuccessors(State state);
	
	public abstract HashSet<State> getSuccessors(State state, EdgeLabel<Symbol> label);
	
	public HashSet<State> getPredecessors(State state) {
		HashSet<State> result = new HashSet<State>();
		if(!incomingEdges.containsKey(state)) {
			incomingEdges.put(state, new HashSet<>());
		}
		for(Edge<Symbol> edge : incomingEdges.get(state)) {
			result.add(edge.getSource());
		}
		return result;
	}
	
	/**
	 * get the predecessors for a certain state and label
	 * @param state the sink state
	 * @param label the label for which the source states should be collected
	 * @return the predecessors
	 */
	public Set<State> getPredecessors(State state, EdgeLabel<Symbol> label) {
		Set<State> result = new HashSet<State>();
		for(Edge<Symbol> edge : incomingEdges.get(state)) {
			//If edge label agrees with label, add source state
			if(edge.getLabel().equals(label)) {
				result.add(edge.getSource());
			}
		}
		return result;
	}
	
	/**
	 * get the predecessors for a set of possible sink states and a label
	 * @param set of the sink states
	 * @param label the label for which the source states should be collected
	 * @return the predecessors
	 */
	public Set<State> getPredecssors(Set<State> states, EdgeLabel<Symbol> label) {
		Set<State> result = new HashSet<State>();
		for(State state : states) {
			result.addAll(getPredecessors(state, label));
		}
		return result;
	}

	public HashSet<State> getSuccessors(HashSet<State> states) {
		HashSet<State> result = new HashSet<State>();
		for(State state : states) {
			result.addAll(getSuccessors(state));
		}
		return result;
	}
	
	public HashSet<State> getSuccessors(HashSet<State> states, EdgeLabel<Symbol> label) {
		HashSet<State> result = new HashSet<State>();
		for(State state : states) {
			result.addAll(getSuccessors(state, label));
		}
		return result;
	}
	
	/**
	 * get the predecessors for a set of possible sink states regardless the label
	 * @param set of the sink states
	 * @return the predecessors
	 */
	public HashSet<State> getPredecessors(HashSet<State> states) {
		HashSet<State> result = new HashSet<State>();
		for(State state : states) {
			result.addAll(getPredecessors(state));
		}
		return result;
	}
	
	public HashSet<State> reachableStates() {
		HashSet<State> reachable = new HashSet<State>();
		Queue<State> unexplored  = new LinkedList<State>();
		
		// Initial states are reachable
		reachable.addAll(initialStates);
		unexplored.addAll(initialStates);
		
		// Get all successors
		while(!unexplored.isEmpty()) {
			State current = unexplored.remove();
			for(State next : getSuccessors(current)) {
				if(!reachable.contains(next)) {
					unexplored.add(next);
				}
				reachable.add(next);
			}
		}
		return reachable;
	}
	
	public HashSet<State> coreachableStates() {
		HashSet<State> coreachable = new HashSet<State>();
		Queue<State> unexplored  = new LinkedList<State>();
		
		// Accepting states are reachable
		coreachable.addAll(acceptingStates);
		unexplored.addAll(acceptingStates);
		
		// Get all successors
		while(!unexplored.isEmpty()) {
			State current = unexplored.remove();
			for(State next : getPredecessors(current)) {
				if(!coreachable.contains(next)) {
					unexplored.add(next);
				}
				coreachable.add(next);
			}
		}
		return coreachable;
	}
	
	public boolean isAcyclic() {
		for(State initState : getInitialStates()) {
			if(!isAcyclic(initState, new Stack<>())) { return false; }
		}
		return true;
	}
	
	private boolean isAcyclic(State state, Stack<State> stack) {
		if(stack.contains(state)) { return false; }
		
		stack.push(state);
		for(State succ : getSuccessors(state)) {
			if(!isAcyclic(succ, stack)) { return false; }
		}
		stack.pop();
		
		return true;
	}
	
	public int getLongestPathLength() {
		int longestInitLength = -1;
		for(State initState : getInitialStates()) {
			longestInitLength = Math.max(longestInitLength, getLongestPathLength(initState));
		}
		return longestInitLength;
	}
	
	private int getLongestPathLength(State state) {
		int longestSuccLength = -1;
		for(State succ : getSuccessors(state)) {
			longestSuccLength = Math.max(longestSuccLength, getLongestPathLength(succ));
		}
		return longestSuccLength+1;
	}
	
	public Set<EdgeLabel<Symbol>> getIncomingEdgeLabels(State state) {
		Set<EdgeLabel<Symbol>> result = new HashSet<EdgeLabel<Symbol>>();
		for(Edge<Symbol> edge : getIncomingEdges(state)) {
			result.add(edge.getLabel());
		}
		return result;
	}
	
	public Set<EdgeLabel<Symbol>> getIncomingEdgeLabels(Set<State> states) {
		Set<EdgeLabel<Symbol>> result = new HashSet<EdgeLabel<Symbol>>();
		for(State state : states) {
			result.addAll(getIncomingEdgeLabels(state));
		}
		return result;
	}

	public Set<EdgeLabel<Symbol>> getOutgoingEdgeLabels(State state) {
		Set<EdgeLabel<Symbol>> result = new HashSet<EdgeLabel<Symbol>>();
		for(Edge<Symbol> edge : getOutgoingEdges(state)) {
			result.add(edge.getLabel());
		}
		return result;
	}
	
	
	/**
	 * trim reduces the automaton to all its reachable and co-reachable states and the corresponding edges
	 */
	public void trim() {
		// remStates are all states except the newStates
		HashSet<State> remStates = new HashSet<>(states);
		
		// newStates are reachable AND coreachable
		states = reachableStates();
		states.retainAll(coreachableStates());
		
		remStates.removeAll(states);
		
		acceptingStates.retainAll(states);
		initialStates.retainAll(states);
		
		// newEdges are all edges that do not contain a remState
		HashSet<E> remEdges = new HashSet<>();
		for(E edge : edges) {
			boolean remEdge = false;
			for(State remState : remStates) {
				if(edge.containsState(remState)) {
					remEdge = true;
					break;
				}
			}
			if(remEdge) {
				remEdges.add(edge);
			} else {
				//newEdges.add(edge);
			}
		}
		edges.removeAll(remEdges);
		
		for(State state : states) {
			outgoingEdges.get(state).removeAll(remEdges);
			incomingEdges.get(state).removeAll(remEdges);
		}
		
		for(State state : remStates) {
			outgoingEdges.remove(state);
			incomingEdges.remove(state);
		}
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("NFA of size " + states.size());
		result.append("\n");
		
		result.append(" States: ");
		for(State s : states) {
			result.append(s + " ");
		}
		result.append("\n");
		
		result.append(" | I: ");
		for(State s : initialStates) {
			result.append(s + " ");
		}
		result.append("\n");
		
		result.append(" | A: ");
		for(State s : acceptingStates) {
			result.append(s + " ");
		}
		result.append("\n");
		
		result.append(" APS: ");
		for(Symbol s : apList) {
			result.append(s + " ");
		}
		result.append("\n");
		
		result.append(" EDGES:\n");
		for(E edge : edges) {
			result.append("  " + edge.toString() + "\n");
		}
		
		result.append(" OUTEDGES:\n");
		result.append(outgoingEdges.toString());
		result.append("\n");
		
		result.append(" INEDGES:\n");
		result.append(incomingEdges.toString());
		result.append("\n");
		
		return result.toString();
	}
}
