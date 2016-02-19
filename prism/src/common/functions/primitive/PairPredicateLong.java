package common.functions.primitive;

import java.util.Objects;

import common.functions.PairPredicate;

@FunctionalInterface
public interface PairPredicateLong extends PairPredicate<Long, Long>
{
	public static final PairPredicateLong TRUE  = (element1, element2) -> true;
	public static final PairPredicateLong FALSE = (element1, element2) -> false;

	public boolean test(long element1, long element2);

	@Override
	default boolean test(Long element1, Long element2)
	{
		return test(element1.longValue(), element2.longValue());
	}

	@Override
	default PredicateLong curry(Long element1)
	{
		return curry(element1.longValue());
	}

	default PredicateLong curry(long element1)
	{
		return element2 -> test(element1, element2);
	}

	/**
	 *  Overridden to ensure that the return type is PairPredicateLong and to optimize long negation.
	 */
	@Override
	default PairPredicateLong negate()
	{
		return new PairPredicateLong()
		{
			@Override
			public boolean test(long element1, long element2)
			{
				return !PairPredicateLong.this.test(element1, element2);
			}

			@Override
			public PairPredicateLong negate()
			{
				return PairPredicateLong.this;
			}
		};
	}

	default PairPredicateLong and(PairPredicateLong predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> test(element1, element2) && predicate.test(element1, element2);
	}

	default PairPredicateLong or(PairPredicateLong predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> test(element1, element2) || predicate.test(element1, element2);
	}

	default PairPredicateLong implies(PairPredicateLong predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> !test(element1, element2) || predicate.test(element1, element2);
	}

	/**
	 *  Overridden to ensure that the return type is PairPredicateLong.
	 */
	@Override
	default PairPredicateLong inverse()
	{
		return (element1, element2) -> test(element2, element1);
	}
}