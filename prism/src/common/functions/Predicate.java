package common.functions;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface Predicate<T> extends Mapping<T, Boolean>, java.util.function.Predicate<T>
{
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
		return each -> test(function.apply(each));
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
		return each -> test(each) && predicate.test(each);
	}

	@Override
	default Predicate<T> or(java.util.function.Predicate<? super T> predicate)
	{
		Objects.requireNonNull(predicate);
		return each -> test(each) || predicate.test(each);
	}

	default Predicate<T> implies(java.util.function.Predicate<? super T> predicate)
	{
		Objects.requireNonNull(predicate);
		return each -> !test(each) || predicate.test(each);
	}
}