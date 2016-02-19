package common.functions.primitive;

import java.util.Objects;

import common.functions.PairPredicate;

@FunctionalInterface
public interface PairPredicateInt extends PairPredicate<Integer, Integer>
{
	public static final PairPredicateInt TRUE  = (element1, element2) -> true;
	public static final PairPredicateInt FALSE = (element1, element2) -> false;

	public abstract boolean test(int element1, int element2);

	@Override
	default boolean test(Integer element1, Integer element2)
	{
		return test(element1.intValue(), element2.intValue());
	}

	@Override
	default PredicateInt curry(Integer element1)
	{
		return curry(element1.intValue());
	}

	default PredicateInt curry(int element1)
	{
		return element2 -> test(element1, element2);
	}

	@Override
	default PairPredicateInt negate()
	{
		return new PairPredicateInt()
		{
			@Override
			public boolean test(int element1, int element2)
			{
				return !PairPredicateInt.this.test(element1, element2);
			}

			@Override
			public PairPredicateInt negate()
			{
				return PairPredicateInt.this;
			}
		};
	}

	default PairPredicateInt and(PairPredicateInt predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> test(element1, element2) && predicate.test(element1, element2);
	}

	default PairPredicateInt or(PairPredicateInt predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> test(element1, element2) || predicate.test(element1, element2);
	}

	default PairPredicateInt implies(PairPredicateInt predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> !test(element1, element2) || predicate.test(element1, element2);
	}

	/**
	 *  Overridden to ensure that the return type is PairPredicateDouble.
	 */
	@Override
	default PairPredicateInt inverse()
	{
		return (element1, element2) -> test(element2, element1);
	}
}