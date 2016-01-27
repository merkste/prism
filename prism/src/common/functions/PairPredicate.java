package common.functions;

import java.util.Objects;
import java.util.function.BiPredicate;

@FunctionalInterface
public interface PairPredicate<R, S> extends PairMapping<R, S, Boolean>, BiPredicate<R, S>
{
	@Override
	default Boolean apply(R element1, S element2)
	{
		return test(element1, element2);
	}

	@Override
	default Predicate<S> curry(R element1)
	{
		return element2 -> test(element1, element2);
	}

	/**
	 *  Overridden to ensure that the return type is PairPredicate and to optimize double negation.
	 */
	@Override
	default PairPredicate<R, S> negate()
	{
		return new PairPredicate<R, S>()
		{
			@Override
			public boolean test(R element1, S element2)
			{
				return !PairPredicate.this.test(element1, element2);
			}

			@Override
			public PairPredicate<R, S> negate()
			{
				return PairPredicate.this;
			}
		};
	}

	@Override
	default PairPredicate<R, S> and(BiPredicate<? super R, ? super S> predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> test(element1, element2) && predicate.test(element1, element2);
	}

	@Override
	default PairPredicate<R, S> or(BiPredicate<? super R, ? super S> predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> test(element1, element2) || predicate.test(element1, element2);
	}

	default PairPredicate<R, S> implies(BiPredicate<? super R, ? super S> predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> !test(element1, element2) || predicate.test(element1, element2);
	}

	default PairPredicate<S, R> inverse()
	{
		return (element1, element2) -> test(element2, element1);
	}
}