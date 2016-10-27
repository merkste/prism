package automata.finite;

import java.util.HashSet;

public class MultiEdge<Symbol> extends Edge<Symbol> {
	private HashSet<State> sinks;

	public MultiEdge(State source, EdgeLabel<Symbol> label, HashSet<State> sinks)
	{
		this.label = label;
		this.source = source;
		this.sinks = sinks;
	}

	public HashSet<State> getSinks() {
		return sinks;
	}

	public void setSinks(HashSet<State> sinks) {
		this.sinks = sinks;
	}
	
	public void addSink(State state) {
		this.sinks.add(state);
	}
	
	public void addSinks(HashSet<State> states) {
		this.sinks.addAll(states);
	}
	
	public void removeSinks(HashSet<State> states) {
		this.sinks.removeAll(states);
	}
	
	public boolean hasSourceState(State state) {
		return source.equals(state);
	}
	
	public boolean hasSinkState(State state) {
		return sinks.contains(state);
	}
	
	@Override
	public boolean containsState(State state) {
		return hasSourceState(state) || hasSinkState(state);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		
		result.append(source);
		result.append(" -[ ");
		result.append(label);
		result.append(" ]- ");
		result.append(sinks);
		
		return result.toString();
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		return result ^ sinks.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof MultiEdge<?>) {
			@SuppressWarnings("unchecked")
			MultiEdge<Symbol> other = (MultiEdge<Symbol>)o;
			return super.equals(other) &&
					getSinks().equals(other.getSinks());
		} else {
			return false;
		}
	}
}
