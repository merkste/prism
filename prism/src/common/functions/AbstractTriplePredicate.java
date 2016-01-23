package common.functions;

public abstract class AbstractTriplePredicate<Q, R, S> implements TriplePredicate<Q, R, S>
{
	@Override
	public Boolean apply(final Q element1, final R element2, final S element3)
	{
		return getBoolean(element1, element2, element3);
	}

	@Override
	public PairPredicate<R, S> curry(final Q element1)
	{
		return new AbstractPairPredicate<R, S>()
		{
			@Override
			public boolean getBoolean(final R element2, final S element3)
			{
				return AbstractTriplePredicate.this.getBoolean(element1, element2, element3);
			}
		};
	}

	@Override
	public Predicate<S> curry(final Q element1, final R element2)
	{
		return new AbstractPredicate<S>()
		{
			@Override
			public boolean test(final S element3)
			{
				return AbstractTriplePredicate.this.getBoolean(element1, element2, element3);
			}
		};
	}

	@Override
	public TriplePredicate<Q, R, S> not()
	{
		return new AbstractTriplePredicate<Q, R, S>()
		{
			@Override
			public final boolean getBoolean(final Q element1, final R element2, final S element3)
			{
				return !AbstractTriplePredicate.this.getBoolean(element1, element2, element3);
			}

			@Override
			public TriplePredicate<Q, R, S> not()
			{
				return AbstractTriplePredicate.this;
			}
		};
	}

	@Override
	public TriplePredicate<Q, R, S> and(final TriplePredicate<? super Q, ? super R, ? super S> predicate)
	{
		return new AbstractTriplePredicate<Q, R, S>()
		{
			@Override
			public final boolean getBoolean(final Q element1, final R element2, final S element3)
			{
				return AbstractTriplePredicate.this.getBoolean(element1, element2, element3) && predicate.getBoolean(element1, element2, element3);
			}
		};
	}

	@Override
	public TriplePredicate<Q, R, S> or(final TriplePredicate<? super Q, ? super R, ? super S> predicate)
	{
		return new AbstractTriplePredicate<Q, R, S>()
		{
			@Override
			public final boolean getBoolean(final Q element1, final R element2, final S element3)
			{
				return AbstractTriplePredicate.this.getBoolean(element1, element2, element3) || predicate.getBoolean(element1, element2, element3);
			}
		};
	}

	@Override
	public TriplePredicate<Q, R, S> implies(final TriplePredicate<? super Q, ? super R, ? super S> predicate)
	{
		return new AbstractTriplePredicate<Q, R, S>()
		{
			@Override
			public final boolean getBoolean(final Q element1, final R element2, final S element3)
			{
				return (!AbstractTriplePredicate.this.getBoolean(element1, element2, element3)) || predicate.getBoolean(element1, element2, element3);
			}
		};
	}
}