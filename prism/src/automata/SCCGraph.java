package automata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import acceptance.AcceptanceOmega;

public class SCCGraph<Symbol> {
	
	protected class Node
	{
		//the DFS depth for which the node has been visited
		private int dfs = 0;
		private int lowlink = 0;
		//the state index
		private int index;
		
		public Node(int index)
		{
			this.index = index;
		}
		
		public int getDfs() {
			return dfs;
		}
		
		public void setDfs(int dfs) {
			this.dfs = dfs;
		}
		
		public int getLowlink() {
			return lowlink;
		}
		
		public void setLowlink(int lowlink) {
			this.lowlink = lowlink;
		}
		
		public int getIndex()
		{
			return index;
		}
		
		public int hashCode() {
			return index ^ dfs ^ lowlink;
		}
		
		public boolean equals(Object o)
		{
			if(o instanceof SCCGraph.Node) {
				@SuppressWarnings("unchecked")
				Node other = (SCCGraph<Symbol>.Node)o;
				return other.getIndex() == getIndex() &&
						other.getDfs() == getDfs() &&
						other.getLowlink() == getLowlink();
			}
			return false;
		}
	}

	//The sccs of the graph
	protected LinkedHashSet<Set<Integer>> states = new LinkedHashSet<>();
	//The maximial depth of the dfs, used for Tarjan's SCC generation
	protected int maxDfs = 0;
	//The DFS stack, used for Tarjan's SCC generation
	protected Stack<Node> stack = new Stack<>();
	//The of yet to visit states, used for Tarjan's SCC generation
	protected Set<Node> unvisited = new LinkedHashSet<>();
	//The underlying deterministic automaton
	protected DA<Symbol, ? extends AcceptanceOmega> da;
	//A topologic ordered version of the SCCs
	protected List<Set<Integer>> orderedCache;
	
	public SCCGraph(DA<Symbol, ? extends AcceptanceOmega> da)
	{
		for(int i = 0; i < da.size(); i++) {
			unvisited.add(new Node(i));
		}
		
		this.da = da;
		
		while(!unvisited.isEmpty()) {
			tarjan(unvisited.stream().findAny().get());
		}
	}
	
	/**
	 * Find node/state with a certain index on the stack 
	 * @param state the index of the state
	 * @return the node, if the state in on the stack
	 */
	protected Optional<Node> getNodeStack(int state)
	{
		return stack.stream().
				filter(node -> node.getIndex() == state).
				findAny();
	}
	
	/**
	 * Find node/state with a certain index in the set of unvisited states
	 * @param state the index of the state
	 * @return the node, if the state is in unvisited
	 */
	protected Optional<Node> getNodeUnvisited(int state)
	{
		return unvisited.stream().
				filter(node -> node.getIndex() == state).
				findAny();
	}
	
	/**
	 * Search for the successors in the SCC graph
	 * @param scc the analyzed scc
	 * @return successors of scc in the SCC graph
	 */
	public Set<Set<Integer>> getSCCSucessors(Set<Integer> scc)
	{
		// Generate the successors states in the DA (not the SCC graph!)
		Set<Integer> succ = getSuccessors(scc);
		List<Set<Integer>> orderedSCCs = topologicOrdered();
		Set<Set<Integer>> result = new LinkedHashSet<>();
		//Go through all sccs, that may appear after scc in the SCC graph
		//Add all SCCs that are not disjoint with the successors
		for(int i = orderedSCCs.indexOf(scc) + 1; i < orderedSCCs.size(); i++) {
			Set<Integer> succCp = new LinkedHashSet<>(succ);
			succCp.retainAll(orderedSCCs.get(i));
			if(!succCp.isEmpty()) {
				result.add(orderedSCCs.get(i));
			}
		}
		return result;
	}
	
	/**
	 * Implementation of Tarjan's SCC generation algorithm
	 * @param node starting node for SCC generation
	 */
	protected void tarjan(Node node)
	{
		node.setDfs(maxDfs);
		node.setLowlink(maxDfs);
		maxDfs++;
		//Push starting node on the stack
		stack.push(node);
		//Remove starting node from the set of unvisited states
		unvisited.remove(node);
		int source = node.getIndex();

		for(int edge = 0; edge < da.getNumEdges(source); edge++) {
			//Get successor
			int sink = da.getEdgeDest(source, edge);
			Optional<Node> possibleUnvisited = getNodeUnvisited(sink);
			
			if(possibleUnvisited.isPresent()) {
				//If successor state is not visited yet
				//start Tarjan recursively
				Node unvisited = possibleUnvisited.get();
				tarjan(unvisited);
				if(node.getLowlink() > unvisited.getLowlink()) {
					//Set lowlink to the lowlink of the smallest successor
					node.setLowlink(unvisited.getLowlink());
				}
			} else {
				//search for successor on the stack
				Optional<Node> possibleStack = getNodeStack(sink);
				if(possibleStack.isPresent()) {
					//If found, then there exists a cycle
					// set lowlink to depth of the first visit
					Node stackNode = possibleStack.get();
					if(node.getLowlink() > stackNode.getDfs()) {
						node.setLowlink(stackNode.getDfs());
					}
				}
			}
		}
		
		if(node.getLowlink() == node.getDfs()) {
			//Enumerate SCC
			Set<Integer> scc = new HashSet<>();

			Node stackNode;
			do {
				stackNode = stack.pop();
				scc.add(stackNode.getIndex());
			} while(stackNode.getIndex() != node.getIndex());
			states.add(scc);
		}
	}
	
	/**
	 * Collect successors in this graph
	 * @param sources the nodes from which the successors should be collected
	 * @return the successors of sources
	 */
	public Set<Integer> getSuccessors(Set<Integer> sources)
	{
		Set<Integer> result = new LinkedHashSet<>();
		
		for(int source : sources) {
			for(int edge = 0; edge < da.getNumEdges(source); edge++) {
				result.add(da.getEdgeDest(source, edge));
			}
		}
		
		return result;
	}

	/**
	 * Collect predecessors in this graph, that have not been already visited
	 * @param sinks the sinks whose predecessors should be collected
	 * @param visited the set of already visited nodes
	 * @return the successors of sources
	 */
	public Set<Integer> getPredecessors(Set<Integer> sinks, Set<Integer> visited)
	{
		//TODO don't regenerate predecessors every time
		Set<Integer> result = new LinkedHashSet<>();
		for(int source = 0; source < da.size(); source++) {
			if(!visited.contains(source)) {
				for(int edge = 0; edge < da.getNumEdges(source); edge++) {
					int sink = da.getEdgeDest(source, edge);
					if(sinks.contains(sink) && !sinks.contains(source)) {
						result.add(source);
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Order the SCCs
	 * If scc2 is an successor of scc1 in the SCC graph, then
	 * index of scc1 < index of scc2 
	 * @return an ordered list of the SCCs
	 */
	public List<Set<Integer>> topologicOrdered()
	{
		//Generate ordering only if it has not been calculated yet
		if(orderedCache == null) {
			//Generate topologic ordered list
			orderedCache = new ArrayList<>(states.size());
			Set<Integer> visited = new LinkedHashSet<>();
			@SuppressWarnings("unchecked")
			Set<Set<Integer>> sccs = (Set<Set<Integer>>) states.clone();
			while(!sccs.isEmpty()) {
				//Collect SCCs without predecessors = root SCCs
				Set<Set<Integer>> rootSccs = sccs.stream().
												filter(scc -> getPredecessors(scc, visited).isEmpty()).
												collect(Collectors.toSet());
				//Remove  root sccs
				sccs.removeAll(rootSccs);
				for(Set<Integer> scc : rootSccs) {
					//Add SCCs without predecessors
					orderedCache.add(scc);
					visited.addAll(scc);
				}
			}
		}
		return orderedCache;
	}
}
