package common.functions;

import common.functions.primitive.PairPredicateDouble;
import common.functions.primitive.PredicateDouble;
import prism.PrismUtils;

public class CloseDoubleRelation
{
	public static final double EPSILON = 1.0e-12d;

	public static final PairPredicateDouble C_GT = closeGT(EPSILON);
	public static final PairPredicateDouble C_GEQ = closeGEQ(EPSILON);
	public static final PairPredicateDouble C_LT = closeLT(EPSILON);
	public static final PairPredicateDouble C_LEQ = closeLEQ(EPSILON);
	public static final PairPredicateDouble C_EQ = closeEQ(EPSILON);
	public static final PairPredicateDouble C_NEQ = closeNEQ(EPSILON);

	public static PairPredicateDouble closeGT(final double epsilon)
	{
		return Relation.GT.and(closeNEQ(epsilon));
	}

	public static PairPredicateDouble closeGEQ(final double epsilon)
	{
		return Relation.GEQ.or(closeEQ(epsilon));
	}

	public static PairPredicateDouble closeLT(final double epsilon)
	{
		return Relation.LT.and(closeNEQ(epsilon));
	}

	public static PairPredicateDouble closeLEQ(final double epsilon)
	{
		return Relation.LEQ.or(closeEQ(epsilon));
	}

	public static PairPredicateDouble closeEQ(final double epsilon)
	{
		return (d1, d2) -> PrismUtils.doublesAreCloseAbs(d1, d2, epsilon);
	}

	public static PairPredicateDouble closeNEQ(final double epsilon)
	{
		return closeEQ(epsilon).negate();
	}

	/**
	 * C_GT(y).getBoolean(x) == (x > y) && (x !~ y)
	 * 
	 * @param y right-hand side argument
	 * @return (? > y) && (? !~ y)
	 */
	public static PredicateDouble C_GT(final double y)
	{
		return C_GT.inverse().curry(y);
	}

	/**
	 * C_GEQ(y).getBoolean(x) := (x >= y) || (x ~~ y)
	 * 
	 * @param y right-hand side argument
	 * @return (? >= y) || (? ~~ y)
	 */
	public static PredicateDouble C_GEQ(final double y)
	{
		return C_GEQ.inverse().curry(y);
	}

	/**
	 * C_LT(y).getBoolean(x) := (x < y) && (x !~ y)
	 * 
	 * @param y right-hand side argument
	 * @return (? < y) && (? !~ y)
	 */
	public static PredicateDouble C_LT(final double y)
	{
		return C_LT.inverse().curry(y);
	}

	/**
	 * C_LEQ(y).getBoolean(x) := (x <= y) || (x !~ y)
	 * 
	 * @param y right-hand side argument
	 * @return (? <= y) || (? ~~ y)
	 */
	public static PredicateDouble C_LEQ(final double y)
	{
		return C_LEQ.inverse().curry(y);
	}

	/**
	 * C_EQ(y).getBoolean(x) := (x ~~ y)
	 * 
	 * @param y right-hand side argument
	 * @return (? ~~ y)
	 */
	public static PredicateDouble C_EQ(final double y)
	{
		return C_EQ.inverse().curry(y);
	}

	/**
	 * C_NEQ(y).getBoolean(x) := (x !~ y)
	 * 
	 * @param y right-hand side argument
	 * @return (? !~ y)
	 */
	public static PredicateDouble C_NEQ(final double y)
	{
		return C_NEQ.inverse().curry(y);
	}
}