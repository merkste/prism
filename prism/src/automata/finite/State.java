package automata.finite;

// TODO: IDs may run out
public class State implements Comparable<State> {
	private static int nextId = 0;
	
	public final int id;
	public String name;
	
	public State() {
		this.id   = nextId++;
		this.name = null;
	}
	
	public State(String name) {
		this.id   = nextId++;
		this.name = name;
	}


	@Override
	public String toString() {
		if (name == null) { return "" + id; }
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		State other = (State) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public int compareTo(State o) {
		return o.id - id;
	}
}
