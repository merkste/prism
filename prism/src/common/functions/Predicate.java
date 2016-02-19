package common.functions;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface Predicate<T> extends Mapping<T, Boolean>, java.util.function.Predicate<T>
{
	public static final Predicate<?> TRUE  = element -> true;
	public static final Predicate<?> FALSE = element -> false;

	@Override
	default Boolean apply(T element)
	{
		return test(element);
	}

	/**
	 *  Overridden to ensure that the return type is Predicate.
	 */
	@Override
	default <S> Predicate<S> compose(Function<? super S, ? extends T> function)
	{
		Objects.requireNonNull(function);
		return element -> test(function.apply(element));
	}

	@Override
	default <P, Q> PairPredicate<P, Q> compose(BiFunction<? super P, ? super Q, ? extends T> function)
	{
		Objects.requireNonNull(function);
		return (element1, element2) -> apply(function.apply(element1, element2));
	}

	@Override
	default <P, Q, R> TriplePredicate<P, Q, R> compose(TripleMapping<? super P, ? super Q, ? super R, ? extends T> function)
	{
		Objects.requireNonNull(function);
		return (element1, element2, element3) -> apply(function.apply(element1, element2, element3));
	}

	/**
	 *  Overridden to ensure that the return type is Predicate and to optimize double negation.
	 */
	@Override
	default Predicate<T> negate()
	{
		return new Predicate<T>()
		{
			@Override
			public boolean test(T element)
			{
				return !Predicate.this.test(element);
			}

			@Override
			public Predicate<T> negate()
			{
				return Predicate.this;
			}
		};
	}

	@Override
	default Predicate<T> and(java.util.function.Predicate<? super T> predicate)
	{
		Objects.requireNonNull(predicate);
		return element -> test(element) && predicate.test(element);
	}

	@Override
	default Predicate<T> or(java.util.function.Predicate<? super T> predicate)
	{
		Objects.requireNonNull(predicate);
		return element -> test(element) || predicate.test(element);
	}

	default Predicate<T> implies(java.util.function.Predicate<? super T> predicate)
	{
		Objects.requireNonNull(predicate);
		return element -> !test(element) || predicate.test(element);
	}

	default <U> Mapping<T, U> ite(Function<? super T, ? extends U> function1, Function<? super T, ? extends U> function2)
	{
		Objects.requireNonNull(function1);
		Objects.requireNonNull(function2);
		return element -> test(element) ? function1.apply(element) : function2.apply(element);
	}

	@SuppressWarnings("unchecked")
	public static <T> Predicate<T> constant(boolean value)
	{
		return (Predicate<T>) (value ?  TRUE : FALSE);
	}
}