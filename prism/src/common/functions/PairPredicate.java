package common.functions;

import java.util.function.BiPredicate;

public interface PairPredicate<R, S> extends PairMapping<R, S, Boolean>, BiPredicate<R, S>
{
	public boolean test(R element1, S element2);

	@Override
	default Boolean apply(final R element1, final S element2)
	{
		return test(element1, element2);
	}

	@Override
	default Predicate<S> curry(final R element1)
	{
		return new Predicate<S>()
		{
			@Override
			public boolean test(final S element2)
			{
				return PairPredicate.this.test(element1, element2);
			}
		};
	}

	@Override
	default PairPredicate<R, S> negate()
	{
		return new PairPredicate<R, S>()
		{
			@Override
			public final boolean test(final R element1, final S element2)
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
		return new PairPredicate<R, S>()
		{
			@Override
			public final boolean test(final R element1, final S element2)
			{
				return PairPredicate.this.test(element1, element2) && predicate.test(element1, element2);
			}
		};
	}

	@Override
	default PairPredicate<R, S> or(BiPredicate<? super R, ? super S> predicate)
	{
		return new PairPredicate<R, S>()
		{
			@Override
			public final boolean test(final R element1, final S element2)
			{
				return PairPredicate.this.test(element1, element2) || predicate.test(element1, element2);
			}
		};
	}

	default PairPredicate<R, S> implies(BiPredicate<? super R, ? super S> predicate)
	{
		return new PairPredicate<R, S>()
		{
			@Override
			public final boolean test(final R element1, final S element2)
			{
				return (!PairPredicate.this.test(element1, element2)) || predicate.test(element1, element2);
			}
		};
	}

	default PairPredicate<S, R> inverse()
	{
		return new PairPredicate<S, R>()
		{
			@Override
			public final boolean test(final S element1, final R element2)
			{
				return PairPredicate.this.test(element2, element1);
			}
		};
	}
}