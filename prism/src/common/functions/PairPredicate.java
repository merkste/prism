package common.functions;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

@FunctionalInterface
public interface PairPredicate<R, S> extends PairMapping<R, S, Boolean>, BiPredicate<R, S>
{
	public static final PairPredicate<?, ?> TRUE  = (element1, element2) -> true;
	public static final PairPredicate<?, ?> FALSE = (element1, element2) -> false;

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

	default <T> PairMapping<R, S, T> ite(BiFunction<? super R, ? super S, ? extends T> function1, BiFunction<? super R, ? super S, ? extends T> function2)
	{
		Objects.requireNonNull(function1);
		Objects.requireNonNull(function2);
		return (element1, element2) -> test(element1, element2) ? function1.apply(element1, element2) : function2.apply(element1, element2);
	}

	@SuppressWarnings("unchecked")
	public static <T> Predicate<T> constant(boolean value)
	{
		return (Predicate<T>) (value ?  TRUE : FALSE);
	}
}