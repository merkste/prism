package common.functions.primitive;

import java.util.Objects;

import common.functions.TriplePredicate;

@FunctionalInterface
public interface TriplePredicateLong extends TriplePredicate<Long, Long, Long>
{
	public static final TriplePredicateLong TRUE  = (element1, element2, element3) -> true;
	public static final TriplePredicateLong FALSE = (element1, element2, element3) -> false;

	public boolean test(long element1, long element2, long element3);

	@Override
	default boolean test(Long element1, Long element2, Long element3)
	{
		return test(element1.longValue(), element2.longValue(), element3.longValue());
	}

	@Override
	default PairPredicateLong curry(Long element1)
	{
		return curry(element1.longValue());
	}

	default PairPredicateLong curry(long element1)
	{
		return (element2, element3) -> test(element1, element2, element3);
	}

	@Override
	default PredicateLong curry(Long element1, Long element2)
	{
		return curry(element1.longValue(), element2.longValue());
	}

	default PredicateLong curry(long element1, long element2)
	{
		return (element3) -> test(element1, element2, element3);
	}

	/**
	 *  Overridden to ensure that the return type is TriplePredicate and to optimize long negation.
	 */
	@Override
	default TriplePredicateLong negate()
	{
		return new TriplePredicateLong()
		{
			@Override
			public boolean test(long element1, long element2, long element3)
			{
				return !TriplePredicateLong.this.test(element1, element2, element3);
			}

			@Override
			public TriplePredicateLong negate()
			{
				return TriplePredicateLong.this;
			}
		};
	}

	default TriplePredicateLong and(TriplePredicateLong predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2, element3) -> test(element1, element2, element3) && predicate.test(element1, element2, element3);

	}

	default TriplePredicateLong or(TriplePredicateLong predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2, element3) -> test(element1, element2, element3) || predicate.test(element1, element2, element3);

	}

	default TriplePredicateLong implies(TriplePredicateLong predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2, element3) -> !test(element1, element2, element3) || predicate.test(element1, element2, element3);

	}
}