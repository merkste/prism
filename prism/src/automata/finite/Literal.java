package automata.finite;

public class Literal<Symbol> {
	private Symbol symbol;
	private boolean signum;
	
	public Literal(Symbol symbol, boolean signum) {
		this.symbol = symbol;
		this.signum = signum;
	}
	
	public boolean positive() {
		return signum == true;
	}
	
	public boolean negative() {
		return signum == false;
	}
	
	public Symbol getSymbol() {
		return symbol;
	}
	
	public boolean getSignum() {
		return signum;
	}
}
