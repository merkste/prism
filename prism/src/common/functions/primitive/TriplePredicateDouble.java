package common.functions.primitive;

import java.util.Objects;

import common.functions.TriplePredicate;

@FunctionalInterface
public interface TriplePredicateDouble extends TriplePredicate<Double, Double, Double>
{
	public static final TriplePredicateDouble TRUE  = (element1, element2, element3) -> true;
	public static final TriplePredicateDouble FALSE = (element1, element2, element3) -> false;

	public boolean test(double element1, double element2, double element3);

	@Override
	default boolean test(Double element1, Double element2, Double element3)
	{
		return test(element1.doubleValue(), element2.doubleValue(), element3.doubleValue());
	}

	@Override
	default PairPredicateDouble curry(Double element1)
	{
		return curry(element1.doubleValue());
	}

	default PairPredicateDouble curry(double element1)
	{
		return (element2, element3) -> test(element1, element2, element3);
	}

	@Override
	default PredicateDouble curry(Double element1, Double element2)
	{
		return curry(element1.doubleValue(), element2.doubleValue());
	}

	default PredicateDouble curry(double element1, double element2)
	{
		return (element3) -> test(element1, element2, element3);
	}

	/**
	 *  Overridden to ensure that the return type is TriplePredicate and to optimize double negation.
	 */
	@Override
	default TriplePredicateDouble negate()
	{
		return new TriplePredicateDouble()
		{
			@Override
			public boolean test(double element1, double element2, double element3)
			{
				return !TriplePredicateDouble.this.test(element1, element2, element3);
			}

			@Override
			public TriplePredicateDouble negate()
			{
				return TriplePredicateDouble.this;
			}
		};
	}

	default TriplePredicateDouble and(TriplePredicateDouble predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2, element3) -> test(element1, element2, element3) && predicate.test(element1, element2, element3);

	}

	default TriplePredicateDouble or(TriplePredicateDouble predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2, element3) -> test(element1, element2, element3) || predicate.test(element1, element2, element3);

	}

	default TriplePredicateDouble implies(TriplePredicateDouble predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2, element3) -> !test(element1, element2, element3) || predicate.test(element1, element2, element3);

	}
}