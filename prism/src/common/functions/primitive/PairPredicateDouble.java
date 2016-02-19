package common.functions.primitive;

import java.util.Objects;

import common.functions.PairPredicate;

@FunctionalInterface
public interface PairPredicateDouble extends PairPredicate<Double, Double>
{
	public static final PairPredicateDouble TRUE  = (element1, element2) -> true;
	public static final PairPredicateDouble FALSE = (element1, element2) -> false;

	public boolean test(double element1, double element2);

	@Override
	default boolean test(Double element1, Double element2)
	{
		return test(element1.doubleValue(), element2.doubleValue());
	}

	@Override
	default PredicateDouble curry(Double element1)
	{
		return curry(element1.doubleValue());
	}

	default PredicateDouble curry(double element1)
	{
		return element2 -> test(element1, element2);
	}

	/**
	 *  Overridden to ensure that the return type is PairPredicateDouble and to optimize double negation.
	 */
	@Override
	default PairPredicateDouble negate()
	{
		return new PairPredicateDouble()
		{
			@Override
			public boolean test(double element1, double element2)
			{
				return !PairPredicateDouble.this.test(element1, element2);
			}

			@Override
			public PairPredicateDouble negate()
			{
				return PairPredicateDouble.this;
			}
		};
	}

	default PairPredicateDouble and(PairPredicateDouble predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> test(element1, element2) && predicate.test(element1, element2);
	}

	default PairPredicateDouble or(PairPredicateDouble predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> test(element1, element2) || predicate.test(element1, element2);
	}

	default PairPredicateDouble implies(PairPredicateDouble predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> !test(element1, element2) || predicate.test(element1, element2);
	}

	/**
	 *  Overridden to ensure that the return type is PairPredicateDouble.
	 */
	@Override
	default PairPredicateDouble inverse()
	{
		return (element1, element2) -> test(element2, element1);
	}
}