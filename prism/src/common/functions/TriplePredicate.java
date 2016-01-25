package common.functions;

@FunctionalInterface
public interface TriplePredicate<Q, R, S> extends TripleMapping<Q, R, S, Boolean>
{
	public boolean getBoolean(final Q element1, final R element2, final S element3);

	@Override
	default Boolean apply(final Q element1, final R element2, final S element3)
	{
		return getBoolean(element1, element2, element3);
	}

	@Override
	default PairPredicate<R, S> curry(final Q element1)
	{
		return new PairPredicate<R, S>()
		{
			@Override
			public boolean test(final R element2, final S element3)
			{
				return TriplePredicate.this.getBoolean(element1, element2, element3);
			}
		};
	}

	@Override
	default Predicate<S> curry(final Q element1, final R element2)
	{
		return new Predicate<S>()
		{
			@Override
			public boolean test(final S element3)
			{
				return TriplePredicate.this.getBoolean(element1, element2, element3);
			}
		};
	}

	default TriplePredicate<Q, R, S> negate()
	{
		return new TriplePredicate<Q, R, S>()
		{
			@Override
			public final boolean getBoolean(final Q element1, final R element2, final S element3)
			{
				return !TriplePredicate.this.getBoolean(element1, element2, element3);
			}

			@Override
			public TriplePredicate<Q, R, S> negate()
			{
				return TriplePredicate.this;
			}
		};
	}

	default TriplePredicate<Q, R, S> and(final TriplePredicate<? super Q, ? super R, ? super S> predicate)
	{
		return new TriplePredicate<Q, R, S>()
		{
			@Override
			public final boolean getBoolean(final Q element1, final R element2, final S element3)
			{
				return TriplePredicate.this.getBoolean(element1, element2, element3) && predicate.getBoolean(element1, element2, element3);
			}
		};
	}

	default TriplePredicate<Q, R, S> or(final TriplePredicate<? super Q, ? super R, ? super S> predicate)
	{
		return new TriplePredicate<Q, R, S>()
		{
			@Override
			public final boolean getBoolean(final Q element1, final R element2, final S element3)
			{
				return TriplePredicate.this.getBoolean(element1, element2, element3) || predicate.getBoolean(element1, element2, element3);
			}
		};
	}

	default TriplePredicate<Q, R, S> implies(final TriplePredicate<? super Q, ? super R, ? super S> predicate)
	{
		return new TriplePredicate<Q, R, S>()
		{
			@Override
			public final boolean getBoolean(final Q element1, final R element2, final S element3)
			{
				return (!TriplePredicate.this.getBoolean(element1, element2, element3)) || predicate.getBoolean(element1, element2, element3);
			}
		};
	}
}