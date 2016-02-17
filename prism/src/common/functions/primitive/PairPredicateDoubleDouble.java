package common.functions.primitive;

import common.functions.PairPredicate;

public interface PairPredicateDoubleDouble extends PairPredicate<Double, Double>
{
	public boolean getBoolean(final double element1, final double element2);

	public PredicateDouble curry(final double element1);

	@Override
	public PairPredicateDoubleDouble not();

	public PairPredicateDoubleDouble and(final PairPredicateDoubleDouble predicate);

	public PairPredicateDoubleDouble or(final PairPredicateDoubleDouble predicate);

	public PairPredicateDoubleDouble implies(final PairPredicateDoubleDouble predicate);

	@Override
	public PairPredicateDoubleDouble inverse();
}