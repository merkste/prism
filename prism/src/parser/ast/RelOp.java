package parser.ast;

import prism.PrismLangException;

/**
 * Class to represent a relational operator (or similar) found in a P/R/S operator.
 */
public enum RelOp
{
	GT(">") {
		@Override
		public boolean isLowerBound()
		{
			return true;
		}

		@Override
		public boolean isStrict()
		{
			return true;
		}

		@Override
		public RelOp negate() throws PrismLangException
		{
			return LEQ;
		}
	},
	GEQ(">=") {
		@Override
		public boolean isLowerBound()
		{
			return true;
		}

		@Override
		public RelOp negate() throws PrismLangException
		{
			return LT;
		}
	},
	LT("<") {
		@Override
		public boolean isUpperBound()
		{
			return true;
		}

		@Override
		public boolean isStrict()
		{
			return true;
		}

		@Override
		public RelOp negate() throws PrismLangException
		{
			return GEQ;
		}
	},
	LEQ("<=") {
		@Override
		public boolean isUpperBound()
		{
			return true;
		}

		@Override
		public RelOp negate() throws PrismLangException
		{
			return GT;
		}
	},
	COMPUTE_VALUES("=") {
		@Override
		public RelOp negate() throws PrismLangException
		{
			throw new PrismLangException("Cannot negate " + this);
		}
	};

	private final String symbol;

	private RelOp(String symbol)
	{
		this.symbol = symbol;
	}

	@Override
	public String toString()
	{
		return symbol;
	}

	/**
	 * Returns true if this corresponds to a lower bound (i.e. &gt;, &gt;=).
	 */
	public boolean isLowerBound()
	{
		return false;
	}

	/**
	 * Returns true if this corresponds to an upper bound (i.e. &lt;, &lt;=).
	 */
	public boolean isUpperBound()
	{
		return false;
	}

	/**
	 * Returns true if this is a strict bound (i.e. &lt; or &gt;).
	 */
	public boolean isStrict()
	{
		return false;
	}

	/**
	 * Returns the negated form of this operator.
	 */
	public abstract RelOp negate() throws PrismLangException;

	/**
	 * Returns the RelOp object corresponding to a (string) symbol,
	 * e.g. parseSymbol("&lt;=") returns RelOp.LEQ. Returns null if invalid.
	 * @param symbol The symbol to look up
	 * @return
	 */
	public static RelOp parseSymbol(String symbol)
	{
		for (RelOp relop : RelOp.values()) {
			if (relop.toString().equals(symbol)) {
				return relop;
			}
		}
		return null;
	}
}