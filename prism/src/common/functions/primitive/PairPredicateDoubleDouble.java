package common.functions.primitive;

import common.functions.PairPredicate;

public interface PairPredicateDoubleDouble extends PairPredicate<Double, Double>
{
	public boolean test(double element1, double element2);

	public PredicateDouble curry(double element1);

	@Override
	public PairPredicateDoubleDouble not();

	public PairPredicateDoubleDouble and(PairPredicateDoubleDouble predicate);

	public PairPredicateDoubleDouble or(PairPredicateDoubleDouble predicate);

	public PairPredicateDoubleDouble implies(PairPredicateDoubleDouble predicate);

	@Override
	public PairPredicateDoubleDouble inverse();
}