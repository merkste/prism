package common.functions;

import common.functions.primitive.PairPredicateDouble;
import common.functions.primitive.PredicateDouble;

public enum Relation implements PairPredicateDouble
{
	GT(">") {
		@Override
		public boolean test(double x, double y)
		{
			return x > y;
		}

		@Override
		public Relation negate()
		{
			return LEQ;
		}

		@Override
		public Relation inverse()
		{
			return LT;
		};
	},
	GEQ(">=") {
		@Override
		public boolean test(double x, double y)
		{
			return x >= y;
		}

		@Override
		public Relation negate()
		{
			return LT;
		}

		@Override
		public Relation inverse()
		{
			return LEQ;
		};
	},
	LT("<") {
		@Override
		public boolean test(double x, double y)
		{
			return x < y;
		}

		@Override
		public Relation negate()
		{
			return GEQ;
		}

		@Override
		public Relation inverse()
		{
			return GT;
		};
	},
	LEQ("<=") {
		@Override
		public boolean test(double x, double y)
		{
			return x <= y;
		}

		@Override
		public Relation negate()
		{
			return GT;
		}

		@Override
		public Relation inverse()
		{
			return GEQ;
		};
	},
	EQ("==") {
		@Override
		public boolean test(double x, double y)
		{
			return x == y;
		}

		@Override
		public Relation negate()
		{
			return NEQ;
		}

		@Override
		public Relation inverse()
		{
			return EQ;
		};
	},
	NEQ("!=") {
		@Override
		public boolean test(double x, double y)
		{
			return x != y;
		}

		@Override
		public Relation negate()
		{
			return EQ;
		}

		@Override
		public Relation inverse()
		{
			return NEQ;
		};
	};

	private final String symbol;

	private Relation(String symbol)
	{
		this.symbol = symbol;
	}

	@Override
	public String toString()
	{
		return symbol;
	}

	@Override
	public boolean test(Double x, Double y)
	{
		return test(x.doubleValue(), y.doubleValue());
	}

	@Override
	public PredicateDouble curry(double x)
	{
		return new PredicateDouble()
		{
			@Override
			public boolean test(double y)
			{
				return Relation.this.test(x, y);
			}

			@Override
			public String toString()
			{
				return "(" + x + " " + Relation.this.toString() + " _)";
			}
		};
	}

	@Override
	public abstract Relation inverse();

	/**
	 * GT(y).getBoolean(x) := (x > y)
	 *
	 * @param y right-hand side argument
	 * @return (? > y)
	 */
	public static PredicateDouble GT(double y)
	{
		return GT.inverse().curry(y);
	}

	/**
	 * GEQ(y).getBoolean(x) := (x >= y)
	 *
	 * @param y right-hand side argument
	 * @return (? >= y)
	 */
	public static PredicateDouble GEQ(double y)
	{
		return GEQ.inverse().curry(y);
	}

	/**
	 * LT(y).getBoolean(x) := (x < y)
	 *
	 * @param y right-hand side argument
	 * @return (? < y)
	 */
	public static PredicateDouble LT(double y)
	{
		return LT.inverse().curry(y);
	}

	/**
	 * LEQ(y).getBoolean(x) := (x <= y)
	 *
	 * @param y right-hand side argument
	 * @return (? <= y)
	 */
	public static PredicateDouble LEQ(double y)
	{
		return LEQ.inverse().curry(y);
	}

	/**
	 * EQ(y).getBoolean(x) := (x == y)
	 *
	 * @param y right-hand side argument
	 * @return (? == y)
	 */
	public static PredicateDouble EQ(double y)
	{
		return EQ.inverse().curry(y);
	}

	/**
	 * NEQ(y).getBoolean(x) := (x != y)
	 *
	 * @param y right-hand side argument
	 * @return (? != y)
	 */
	public static PredicateDouble NEQ(double y)
	{
		return NEQ.inverse().curry(y);
	}

	public static void main(String[] args)
	{
		int x, y;

		x = 1;
		y = 2;
		System.out.println(x + " " + GT + "  " + y + " = " + GT.test(x, y));
		System.out.println(x + " " + GEQ + " " + y + " = " + GEQ.test(x, y));
		System.out.println(x + " " + LT + "  " + y + " = " + LT.test(x, y));
		System.out.println(x + " " + LEQ + " " + y + " = " + LEQ.test(x, y));
		System.out.println(x + " " + EQ + " " + y + " = " + EQ.test(x, y));
		System.out.println(x + " " + NEQ + " " + y + " = " + NEQ.test(x, y));
		System.out.println();

		x = 3;
		y = 0;
		System.out.println(x + " " + GT + "  " + y + " = " + GT.test(x, y));
		System.out.println(x + " " + GEQ + " " + y + " = " + GEQ.test(x, y));
		System.out.println(x + " " + LT + "  " + y + " = " + LT.test(x, y));
		System.out.println(x + " " + LEQ + " " + y + " = " + LEQ.test(x, y));
		System.out.println(x + " " + EQ + " " + y + " = " + EQ.test(x, y));
		System.out.println(x + " " + NEQ + " " + y + " = " + NEQ.test(x, y));
		System.out.println();

		x = 4;
		y = 4;
		System.out.println(x + " " + GT + "  " + y + " = " + GT.test(x, y));
		System.out.println(x + " " + GEQ + " " + y + " = " + GEQ.test(x, y));
		System.out.println(x + " " + LT + "  " + y + " = " + LT.test(x, y));
		System.out.println(x + " " + LEQ + " " + y + " = " + LEQ.test(x, y));
		System.out.println(x + " " + EQ + " " + y + " = " + EQ.test(x, y));
		System.out.println(x + " " + NEQ + " " + y + " = " + NEQ.test(x, y));
		System.out.println();
	}
}