package common.functions.primitive;

import common.functions.TriplePredicate;

public interface TriplePredicateDoubleDoubleDouble extends TriplePredicate<Double, Double, Double>
{
	public boolean getBoolean(double element1, double element2, double element3);

	public PairPredicateDoubleDouble curry(double element1);

	public PredicateDouble curry(double element1, double element2);

	@Override
	public TriplePredicateDoubleDoubleDouble not();
}