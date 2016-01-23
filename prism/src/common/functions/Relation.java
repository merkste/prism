package common.functions;

import common.functions.primitive.AbstractPairPredicateDoubleDouble;
import common.functions.primitive.AbstractPredicateDouble;
import common.functions.primitive.PairPredicateDoubleDouble;
import common.functions.primitive.PredicateDouble;

public enum Relation implements PairPredicateDoubleDouble
{
	GT(">") {
		@Override
		public boolean getBoolean(final double x, final double y)
		{
			return x > y;
		}

		@Override
		public Relation not()
		{
			return LEQ;
		}

		public Relation inverse()
		{
			return LT;
		};
	},
	GEQ(">=") {
		@Override
		public boolean getBoolean(final double x, final double y)
		{
			return x >= y;
		}

		@Override
		public Relation not()
		{
			return LT;
		}

		public Relation inverse()
		{
			return LEQ;
		};
	},
	LT("<") {
		@Override
		public boolean getBoolean(final double x, final double y)
		{
			return x < y;
		}

		@Override
		public Relation not()
		{
			return GEQ;
		}

		public Relation inverse()
		{
			return GT;
		};
	},
	LEQ("<=") {
		@Override
		public boolean getBoolean(final double x, final double y)
		{
			return x <= y;
		}

		@Override
		public Relation not()
		{
			return GT;
		}

		public Relation inverse()
		{
			return GEQ;
		};
	},
	EQ("==") {
		@Override
		public boolean getBoolean(double x, double y)
		{
			return x == y;
		}

		@Override
		public Relation not()
		{
			return NEQ;
		}

		public Relation inverse()
		{
			return EQ;
		};
	},
	NEQ("!=") {
		@Override
		public boolean getBoolean(double x, double y)
		{
			return x != y;
		}

		@Override
		public Relation not()
		{
			return EQ;
		}

		public Relation inverse()
		{
			return NEQ;
		};
	};

	private final String symbol;

	private Relation(final String symbol)
	{
		this.symbol = symbol;
	}

	@Override
	public String toString()
	{
		return symbol;
	}

	@Override
	public Boolean apply(final Double x, final Double y)
	{
		return getBoolean(x.doubleValue(), y.doubleValue());
	}

	@Override
	public boolean getBoolean(final Double x, final Double y)
	{
		return getBoolean(x.doubleValue(), y.doubleValue());
	}

	@Override
	public abstract boolean getBoolean(final double x, final double y);

	@Override
	public PredicateDouble curry(final Double x)
	{
		return curry(x.doubleValue());
	}

	@Override
	public PredicateDouble curry(final double x)
	{
		return new AbstractPredicateDouble()
		{
			@Override
			public boolean test(final double y)
			{
				return Relation.this.getBoolean(x, y);
			}

			@Override
			public String toString()
			{
				return "(" + x + " " + Relation.this.toString() + " _)";
			}
		};
	}

	@Override
	public PairPredicateDoubleDouble and(final PairPredicate<? super Double, ? super Double> predicate)
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double x, final double y)
			{
				return Relation.this.getBoolean(x, y) && predicate.getBoolean(x, y);
			}
		};
	}

	@Override
	public PairPredicateDoubleDouble and(final PairPredicateDoubleDouble predicate)
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double x, final double y)
			{
				return Relation.this.getBoolean(x, y) && predicate.getBoolean(x, y);
			}
		};
	}

	@Override
	public PairPredicateDoubleDouble or(final PairPredicate<? super Double, ? super Double> predicate)
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double x, final double y)
			{
				return Relation.this.getBoolean(x, y) || predicate.getBoolean(x, y);
			}
		};
	}

	@Override
	public PairPredicateDoubleDouble or(final PairPredicateDoubleDouble predicate)
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double x, final double y)
			{
				return Relation.this.getBoolean(x, y) || predicate.getBoolean(x, y);
			}
		};
	}

	@Override
	public PairPredicateDoubleDouble implies(final PairPredicate<? super Double, ? super Double> predicate)
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double x, final double y)
			{
				return (!Relation.this.getBoolean(x, y)) || predicate.getBoolean(x, y);
			}
		};
	}

	@Override
	public PairPredicateDoubleDouble implies(final PairPredicateDoubleDouble predicate)
	{
		return new AbstractPairPredicateDoubleDouble()
		{
			@Override
			public final boolean getBoolean(final double x, final double y)
			{
				return (!Relation.this.getBoolean(x, y)) || predicate.getBoolean(x, y);
			}
		};
	}

	@Override
	public abstract Relation inverse();

	/**
	 * GT(y).getBoolean(x) == (x > y)
	 *
	 * @param y right-hand side argument
	 * @return (? > y)
	 */
	public static PredicateDouble GT(final double y)
	{
		return GT.inverse().curry(y);
	}

	/**
	 * GEQ(y).getBoolean(x) := (x >= y)
	 *
	 * @param y right-hand side argument
	 * @return (? >= y)
	 */
	public static PredicateDouble GEQ(final double y)
	{
		return GEQ.inverse().curry(y);
	}

	/**
	 * LT(y).getBoolean(x) := (x < y)
	 *
	 * @param y right-hand side argument
	 * @return (? < y)
	 */
	public static PredicateDouble LT(final double y)
	{
		return LT.inverse().curry(y);
	}

	/**
	 * LEQ(y).getBoolean(x) := (x <= y)
	 *
	 * @param y right-hand side argument
	 * @return (? <= y)
	 */
	public static PredicateDouble LEQ(final double y)
	{
		return LEQ.inverse().curry(y);
	}

	/**
	 * EQ(y).getBoolean(x) := (x == y)
	 *
	 * @param y right-hand side argument
	 * @return (? == y)
	 */
	public static PredicateDouble EQ(final double y)
	{
		return EQ.inverse().curry(y);
	}

	/**
	 * NEQ(y).getBoolean(x) := (x != y)
	 *
	 * @param y right-hand side argument
	 * @return (? != y)
	 */
	public static PredicateDouble NEQ(final double y)
	{
		return NEQ.inverse().curry(y);
	}

	public static final void main(final String[] args)
	{
		int x, y;

		x = 1;
		y = 2;
		System.out.println(x + " " + GT + "  " + y + " = " + GT.getBoolean(x, y));
		System.out.println(x + " " + GEQ + " " + y + " = " + GEQ.getBoolean(x, y));
		System.out.println(x + " " + LT + "  " + y + " = " + LT.getBoolean(x, y));
		System.out.println(x + " " + LEQ + " " + y + " = " + LEQ.getBoolean(x, y));
		System.out.println(x + " " + EQ + " " + y + " = " + EQ.getBoolean(x, y));
		System.out.println(x + " " + NEQ + " " + y + " = " + NEQ.getBoolean(x, y));
		System.out.println();

		x = 3;
		y = 0;
		System.out.println(x + " " + GT + "  " + y + " = " + GT.getBoolean(x, y));
		System.out.println(x + " " + GEQ + " " + y + " = " + GEQ.getBoolean(x, y));
		System.out.println(x + " " + LT + "  " + y + " = " + LT.getBoolean(x, y));
		System.out.println(x + " " + LEQ + " " + y + " = " + LEQ.getBoolean(x, y));
		System.out.println(x + " " + EQ + " " + y + " = " + EQ.getBoolean(x, y));
		System.out.println(x + " " + NEQ + " " + y + " = " + NEQ.getBoolean(x, y));
		System.out.println();

		x = 4;
		y = 4;
		System.out.println(x + " " + GT + "  " + y + " = " + GT.getBoolean(x, y));
		System.out.println(x + " " + GEQ + " " + y + " = " + GEQ.getBoolean(x, y));
		System.out.println(x + " " + LT + "  " + y + " = " + LT.getBoolean(x, y));
		System.out.println(x + " " + LEQ + " " + y + " = " + LEQ.getBoolean(x, y));
		System.out.println(x + " " + EQ + " " + y + " = " + EQ.getBoolean(x, y));
		System.out.println(x + " " + NEQ + " " + y + " = " + NEQ.getBoolean(x, y));
		System.out.println();
	}
}