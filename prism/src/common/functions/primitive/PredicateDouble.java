package common.functions.primitive;

import java.util.Objects;
import java.util.function.DoublePredicate;
import java.util.function.ToDoubleFunction;

import common.functions.Predicate;

@FunctionalInterface
public interface PredicateDouble extends Predicate<Double>, DoublePredicate
{
	public boolean test(double element);

	@Override
	default boolean test(Double element)
	{
		return test(element.doubleValue());
	}

	default <S> Predicate<S> compose(ToDoubleFunction<? super S> function)
	{
		Objects.requireNonNull(function);
		return each -> test(function.applyAsDouble(each));
	}

	/**
	 *  Overridden to ensure that the return type is PredicateDouble and to optimize double negation.
	 */
	@Override
	default PredicateDouble negate()
	{
		return new PredicateDouble()
		{
			@Override
			public boolean test(double element)
			{
				return !PredicateDouble.this.test(element);
			}

			@Override
			public PredicateDouble negate()
			{
				return PredicateDouble.this;
			}
		};
	}

	default PredicateDouble and(DoublePredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return each -> test(each) && predicate.test(each);
	}

	default PredicateDouble or(DoublePredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return each -> test(each) || predicate.test(each);
	}

	default PredicateDouble implies(DoublePredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return each -> !test(each) || predicate.test(each);
	}
}