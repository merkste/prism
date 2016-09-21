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
		public RelOp negate(boolean keepStrictness) throws PrismLangException
		{
			return (keepStrictness ? LT : LEQ);
		}
	},
	GEQ(">=") {
		@Override
		public boolean isLowerBound()
		{
			return true;
		}

		@Override
		public RelOp negate(boolean keepStrictness) throws PrismLangException
		{
			return (keepStrictness ? LEQ : LT);
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
		public RelOp negate(boolean keepStrictness) throws PrismLangException
		{
			return (keepStrictness ? GT : GEQ);
		}
	},
	LEQ("<=") {
		@Override
		public boolean isUpperBound()
		{
			return true;
		}

		@Override
		public RelOp negate(boolean keepStrictness) throws PrismLangException
		{
			return (keepStrictness ? GEQ : GT);
		}
	},
	COMPUTE_VALUES("=") {
		@Override
		public RelOp negate(boolean keepStrictness) throws PrismLangException
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
	 * <br>
	 * Equivalent to {@code negate(false)}.
	 */
	public RelOp negate() throws PrismLangException
	{
		return negate(false);
	}

	/**
	 * Returns the negated form of this operator.
	 * Depending on the flag {@code keepStrictness},
	 * the strictness is preserved. E.g., with
	 * {@code keepStrictness == true} the operator "&lt;"
	 * is turned into "&gt;", with {@code keepStrictness == false}
	 * the operator "&lt;" is turned into "&gt=;"
	 */
	public abstract RelOp negate(boolean keepStrictness) throws PrismLangException;

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