package common.functions;

import java.util.function.BiPredicate;

public abstract class AbstractPairPredicate<R, S> extends AbstractPairMapping<R, S, Boolean>implements PairPredicate<R, S>
{
	@Override
	public Boolean apply(final R element1, final S element2)
	{
		return test(element1, element2);
	}

	@Override
	public Predicate<S> curry(final R element1)
	{
		return new AbstractPredicate<S>()
		{
			@Override
			public boolean test(final S element2)
			{
				return AbstractPairPredicate.this.test(element1, element2);
			}
		};
	}

	@Override
	public PairPredicate<R, S> negate()
	{
		return new AbstractPairPredicate<R, S>()
		{
			@Override
			public final boolean test(final R element1, final S element2)
			{
				return !AbstractPairPredicate.this.test(element1, element2);
			}

			@Override
			public AbstractPairPredicate<R, S> negate()
			{
				return AbstractPairPredicate.this;
			}
		};
	}

	@Override
	public PairPredicate<R, S> and(BiPredicate<? super R, ? super S> predicate)
	{
		return new AbstractPairPredicate<R, S>()
		{
			@Override
			public final boolean test(final R element1, final S element2)
			{
				return AbstractPairPredicate.this.test(element1, element2) && predicate.test(element1, element2);
			}
		};
	}

	@Override
	public PairPredicate<R, S> or(BiPredicate<? super R, ? super S> predicate)
	{
		return new AbstractPairPredicate<R, S>()
		{
			@Override
			public final boolean test(final R element1, final S element2)
			{
				return AbstractPairPredicate.this.test(element1, element2) || predicate.test(element1, element2);
			}
		};
	}

	@Override
	public PairPredicate<R, S> implies(BiPredicate<? super R, ? super S> predicate)
	{
		return new AbstractPairPredicate<R, S>()
		{
			@Override
			public final boolean test(final R element1, final S element2)
			{
				return (!AbstractPairPredicate.this.test(element1, element2)) || predicate.test(element1, element2);
			}
		};
	}

	@Override
	public PairPredicate<S, R> inverse()
	{
		return new AbstractPairPredicate<S, R>()
		{
			@Override
			public final boolean test(final S element1, final R element2)
			{
				return AbstractPairPredicate.this.test(element2, element1);
			}
		};
	}
}