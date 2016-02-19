package common.functions.primitive;

import java.util.Objects;

import common.functions.TriplePredicate;

@FunctionalInterface
public interface TriplePredicateInt extends TriplePredicate<Integer, Integer, Integer>
{
	public static final TriplePredicateInt TRUE  = (element1, element2, element3) -> true;
	public static final TriplePredicateInt FALSE = (element1, element2, element3) -> false;

	public boolean test(int element1, int element2, int element3);

	@Override
	default boolean test(Integer element1, Integer element2, Integer element3)
	{
		return test(element1.intValue(), element2.intValue(), element3.intValue());
	}

	@Override
	default PairPredicateInt curry(Integer element1)
	{
		return curry(element1.intValue());
	}

	default PairPredicateInt curry(int element1)
	{
		return (element2, element3) -> test(element1, element2, element3);
	}

	@Override
	default PredicateInt curry(Integer element1, Integer element2)
	{
		return curry(element1.intValue(), element2.intValue());
	}

	default PredicateInt curry(int element1, int element2)
	{
		return (element3) -> test(element1, element2, element3);
	}

	/**
	 *  Overridden to ensure that the return type is TriplePredicate and to optimize double negation.
	 */
	@Override
	default TriplePredicateInt negate()
	{
		return new TriplePredicateInt()
		{
			@Override
			public boolean test(int element1, int element2, int element3)
			{
				return !TriplePredicateInt.this.test(element1, element2, element3);
			}

			@Override
			public TriplePredicateInt negate()
			{
				return TriplePredicateInt.this;
			}
		};
	}

	default TriplePredicateInt and(TriplePredicateInt predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2, element3) -> test(element1, element2, element3) && predicate.test(element1, element2, element3);

	}

	default TriplePredicateInt or(TriplePredicateInt predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2, element3) -> test(element1, element2, element3) || predicate.test(element1, element2, element3);

	}

	default TriplePredicateInt implies(TriplePredicateInt predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2, element3) -> !test(element1, element2, element3) || predicate.test(element1, element2, element3);

	}
}