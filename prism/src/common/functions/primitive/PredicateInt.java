package common.functions.primitive;

import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import common.functions.PairPredicate;
import common.functions.Predicate;

@FunctionalInterface
public interface PredicateInt extends Predicate<Integer>, IntPredicate
{
	public static final PredicateInt TRUE  = n -> true;
	public static final PredicateInt FALSE = n -> false;

	public boolean test(int element);

	default boolean test(Integer element)
	{
		return test(element.intValue());
	}

	default <S> Predicate<S> compose(ToIntFunction<? super S> function)
	{
		Objects.requireNonNull(function);
		return element -> test(function.applyAsInt(element));
	}

	default <P, Q> PairPredicate<P, Q> compose(ToIntBiFunction<? super P, ? super Q> function)
	{
		Objects.requireNonNull(function);
		return (element1, element2) -> apply(function.applyAsInt(element1, element2));
	}

	/**
	 *  Overridden to ensure that the return type is PredicateInt and to optimize double negation.
	 */
	@Override
	default PredicateInt negate()
	{
		return new PredicateInt()
		{
			@Override
			public boolean test(int element)
			{
				return !PredicateInt.this.test(element);
			}

			@Override
			public PredicateInt negate()
			{
				return PredicateInt.this;
			}
		};
	}

	default PredicateInt and(IntPredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return element -> test(element) && predicate.test(element);
	}

	default PredicateInt or(IntPredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return element -> test(element) || predicate.test(element);
	}

	default PredicateInt implies(IntPredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return element -> !test(element) || predicate.test(element);
	}

	default <T> MappingInt<T> ite(IntFunction<? extends T> function1, IntFunction<? extends T> function2)
	{
		Objects.requireNonNull(function1);
		Objects.requireNonNull(function2);
		return element -> test(element) ? function1.apply(element) : function2.apply(element);
	}
}