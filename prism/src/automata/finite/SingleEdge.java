package automata.finite;

public class SingleEdge<Symbol> extends Edge<Symbol> {
	private State sink;
	
	public SingleEdge(State source, EdgeLabel<Symbol> label, State sink)
	{
		this.label = label;
		this.source = source;
		this.sink = sink;
	}

	public State getSink() {
		return sink;
	}

	public void setSink(State sink) {
		this.sink = sink;
	}
	
	public boolean hasSourceState(State state) {
		return source.equals(state);
	}
	
	public boolean hasSinkState(State state) {
		return sink.equals(state);
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
		result.append(sink);
		
		return result.toString();
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		return result ^ sink.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof SingleEdge<?>) {
			@SuppressWarnings("unchecked")
			SingleEdge<Symbol> other = (SingleEdge<Symbol>)o;
			return super.equals(other) &&
					getSink().equals(other.getSink());
		} else {
			return false;
		}
	}
}
