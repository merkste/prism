package common.functions;

import java.util.Objects;

@FunctionalInterface
public interface TriplePredicate<Q, R, S> extends TripleMapping<Q, R, S, Boolean>
{
	public static final TriplePredicate<?, ?, ?> TRUE  = (element1, element2, element3) -> true;
	public static final TriplePredicate<?, ?, ?> FALSE = (element1, element2, element3) -> false;

	public boolean test(Q element1, R element2, S element3);

	@Override
	default Boolean apply(Q element1, R element2, S element3)
	{
		return test(element1, element2, element3);
	}

	@Override
	default PairPredicate<R, S> curry(Q element1)
	{
		return (element2, element3) -> test(element1, element2, element3);
	}

	@Override
	default Predicate<S> curry(Q element1, R element2)
	{
		return (element3) -> test(element1, element2, element3);

	}

	/**
	 *  Overridden to ensure that the return type is TriplePredicate and to optimize double negation.
	 */
	default TriplePredicate<Q, R, S> negate()
	{
		return new TriplePredicate<Q, R, S>()
		{
			@Override
			public final boolean test(Q element1, R element2, S element3)
			{
				return !TriplePredicate.this.test(element1, element2, element3);
			}

			@Override
			public TriplePredicate<Q, R, S> negate()
			{
				return TriplePredicate.this;
			}
		};
	}

	default TriplePredicate<Q, R, S> and(TriplePredicate<? super Q, ? super R, ? super S> predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2, element3) -> test(element1, element2, element3) && predicate.test(element1, element2, element3);
	}

	default TriplePredicate<Q, R, S> or(final TriplePredicate<? super Q, ? super R, ? super S> predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2, element3) -> test(element1, element2, element3) || predicate.test(element1, element2, element3);
	}

	default TriplePredicate<Q, R, S> implies(final TriplePredicate<? super Q, ? super R, ? super S> predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2, element3) -> !test(element1, element2, element3) || predicate.test(element1, element2, element3);
	}

	default <T> TripleMapping<Q, R, S, T> ite(TripleMapping<? super Q, ? super R, ? super S, ? extends T> function1, TripleMapping<? super Q, ? super R, ? super S, ? extends T> function2)
	{
		Objects.requireNonNull(function1);
		Objects.requireNonNull(function2);
		return (element1, element2, element3) -> test(element1, element2, element3) ? function1.apply(element1, element2, element3) : function2.apply(element1, element2, element3);
	}

	@SuppressWarnings("unchecked")
	public static <T> Predicate<T> constant(boolean value)
	{
		return (Predicate<T>) (value ?  TRUE : FALSE);
	}
}