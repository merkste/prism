package common.functions.primitive;

import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

import common.functions.Predicate;

@FunctionalInterface
public interface PredicateInt extends Predicate<Integer>
{
	public boolean test(int element);

	default boolean test(Integer element)
	{
		return test(element.intValue());
	}

	default <S> Predicate<S> compose(ToIntFunction<? super S> function)
	{
		Objects.requireNonNull(function);
		return each -> test(function.applyAsInt(each));
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
		return each -> test(each) && predicate.test(each);
	}

	default PredicateInt or(IntPredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return each -> test(each) || predicate.test(each);
	}

	default PredicateInt implies(IntPredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return each -> !test(each) || predicate.test(each);
	}
}