package automata.finite;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import common.iterable.IterableBitSet;

public class EdgeLabel<Symbol> {
	private final List<Symbol> symbols;
	private final HashMap<Symbol, Integer> indizes;
	private BitSet relevant;
	private BitSet value;
	private boolean defaultValue;
	
	public EdgeLabel() {
		symbols = new ArrayList<>();
		indizes = new HashMap<>();
		relevant = new BitSet();
		value = new BitSet();
		defaultValue = true;
	}
	
	public EdgeLabel(EdgeLabel<Symbol> edgeLabel) {
		symbols = new ArrayList<>(edgeLabel.symbols);
		indizes = new HashMap<>(edgeLabel.indizes);
		relevant = (BitSet)edgeLabel.relevant.clone();
		value = (BitSet)edgeLabel.value.clone();
		defaultValue = edgeLabel.defaultValue;
		
	}
	
	public EdgeLabel(boolean defaultValue) {
		symbols = new ArrayList<>();
		indizes = new HashMap<>();
		relevant = new BitSet();
		value = new BitSet();
		this.defaultValue = defaultValue;
	}
	
	public EdgeLabel(List<Symbol> symbols) {
		this.symbols = symbols;
		
		indizes = new HashMap<>();
		for(int i = 0; i < symbols.size(); i++) {
			indizes.put(symbols.get(i), i);
		}
		
		value = new BitSet();
		relevant = new BitSet();
		defaultValue = true;
	}
	
	public EdgeLabel(List<Symbol> symbols, BitSet value) {
		this.symbols = symbols;
		
		indizes = new HashMap<>();
		for(int i = 0; i < symbols.size(); i++) {
			indizes.put(symbols.get(i), i);
		}
		this.value = value;
		
		relevant = new BitSet();
		relevant.set(0,symbols.size());
		
		defaultValue = true;
	}
	
	public EdgeLabel(List<Symbol> symbols, BitSet value, BitSet relevant) {
		this.symbols = new ArrayList<>(symbols);
		
		indizes = new HashMap<>();
		
		for(int i = 0; i < symbols.size(); i++) {
			indizes.put(symbols.get(i), i);
		}
		
		this.value = value;
		this.relevant = relevant;
		defaultValue = true;
	}
	

	public BitSet getRelevant() {
		return relevant;
	}

	public BitSet getValue() {
		return value;
	}

	public boolean isTrue() {
		return indizes.isEmpty() && defaultValue;
	}
	
	public boolean isFalse() {
		return indizes.isEmpty() && !defaultValue;
	}
	
	public boolean matches(Literal<Symbol> literal) {
		int idx = indizes.getOrDefault(literal, -1);
		
		if(idx < 0) { return defaultValue; }
		
		return ((value.get(idx) == literal.getSignum()) ||relevant.get(idx));
	}
	
	public boolean matches(Collection<Literal<Symbol>> literals) {
		for(Literal<Symbol> l : literals) {
			if(!matches(l)) { return false; }
		}
		return true;
	}
	
	public void setValue(Symbol ap, boolean b) {
		int idx = indizes.get(ap);
		value.set(idx, b);
		relevant.set(idx);
	}
	
	public boolean getValue(Symbol ap) {
		int idx = indizes.getOrDefault(ap, -1);
		
		if(idx < 0 || !relevant.get(idx)) { return defaultValue; }
		
		return value.get(idx);
	}
	
	public ArrayList<Symbol> relevantSymbols() {
		ArrayList<Symbol> result = new ArrayList<>();
		for(Integer i : IterableBitSet.getSetBits(getRelevant())) {
			result.add(symbols.get(i));
		}
		return result;
	}
	
	public boolean intersects(EdgeLabel<Symbol> other) {
		if(isTrue() || other.isTrue()) {
			return true;
		}

		if(isFalse()) {
			if(other.isFalse()) {
				return true;
			}
		}
		
		ArrayList<Symbol> relevantSymbols = relevantSymbols();
		relevantSymbols.retainAll(other.relevantSymbols());
		
		for(Symbol s : relevantSymbols) {
			if(getValue(s) != other.getValue(s)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString() {		
		if(isTrue())  { return "true"; }
		if(isFalse()) { return "false"; }
		
		StringBuffer result = new StringBuffer();
		for(Integer i : IterableBitSet.getSetBits(getRelevant())) {
			if(getValue().get(i)) {
				result.append("+");
			} else {
				result.append("-");
			}
			result.append(symbols.get(i));
		}
		return result.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof EdgeLabel<?>) {
			@SuppressWarnings("unchecked")
			EdgeLabel<Symbol> other = (EdgeLabel<Symbol>)o;
			
			//Check for default cases
			if(isTrue()) {
				return other.isTrue();
			}
			if(isFalse()) {
				return other.isFalse();
			}

			//Check for non-default cases
			return getValue().equals(other.getValue()) &&
					getRelevant().equals(other.getRelevant());
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		if(isTrue()) { return 0;}
		if(isFalse()) {return -1;}
		
		return value.hashCode() ^ relevant.hashCode();
	}
}
