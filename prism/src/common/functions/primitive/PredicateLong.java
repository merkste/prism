package common.functions.primitive;

import java.util.Objects;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;

import common.functions.PairPredicate;
import common.functions.Predicate;

@FunctionalInterface
public interface PredicateLong extends Predicate<Long>, LongPredicate
{
	public static final PredicateLong TRUE  = n -> true;
	public static final PredicateLong FALSE = n -> false;

	public boolean test(long element);

	@Override
	default boolean test(Long element)
	{
		return test(element.longValue());
	}

	default <S> Predicate<S> compose(ToLongFunction<? super S> function)
	{
		Objects.requireNonNull(function);
		return element -> test(function.applyAsLong(element));
	}

	default <P, Q> PairPredicate<P, Q> compose(ToLongBiFunction<? super P, ? super Q> function)
	{
		Objects.requireNonNull(function);
		return (element1, element2) -> apply(function.applyAsLong(element1, element2));
	}

	/**
	 *  Overridden to ensure that the return type is PredicateLong and to optimize long negation.
	 */
	@Override
	default PredicateLong negate()
	{
		return new PredicateLong()
		{
			@Override
			public boolean test(long element)
			{
				return !PredicateLong.this.test(element);
			}

			@Override
			public PredicateLong negate()
			{
				return PredicateLong.this;
			}
		};
	}

	default PredicateLong and(LongPredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return element -> test(element) && predicate.test(element);
	}

	default PredicateLong or(LongPredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return element -> test(element) || predicate.test(element);
	}

	default PredicateLong implies(LongPredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return element -> !test(element) || predicate.test(element);
	}

	default <T> MappingLong<T> ite(LongFunction<? extends T> function1, LongFunction<? extends T> function2)
	{
		Objects.requireNonNull(function1);
		Objects.requireNonNull(function2);
		return element -> test(element) ? function1.apply(element) : function2.apply(element);
	}
}