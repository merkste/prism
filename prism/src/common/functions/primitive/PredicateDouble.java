package common.functions.primitive;

import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

import common.functions.PairPredicate;
import common.functions.Predicate;

@FunctionalInterface
public interface PredicateDouble extends Predicate<Double>, DoublePredicate
{
	public static final PredicateDouble TRUE  = n -> true;
	public static final PredicateDouble FALSE = n -> false;

	public boolean test(double element);

	@Override
	default boolean test(Double element)
	{
		return test(element.doubleValue());
	}

	default <S> Predicate<S> compose(ToDoubleFunction<? super S> function)
	{
		Objects.requireNonNull(function);
		return element -> test(function.applyAsDouble(element));
	}

	default <P, Q> PairPredicate<P, Q> compose(ToDoubleBiFunction<? super P, ? super Q> function)
	{
		Objects.requireNonNull(function);
		return (element1, element2) -> apply(function.applyAsDouble(element1, element2));
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
		return element -> test(element) && predicate.test(element);
	}

	default PredicateDouble or(DoublePredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return element -> test(element) || predicate.test(element);
	}

	default PredicateDouble implies(DoublePredicate predicate)
	{
		Objects.requireNonNull(predicate);
		return element -> !test(element) || predicate.test(element);
	}

	default <T> MappingDouble<T> ite(DoubleFunction<? extends T> function1, DoubleFunction<? extends T> function2)
	{
		Objects.requireNonNull(function1);
		Objects.requireNonNull(function2);
		return element -> test(element) ? function1.apply(element) : function2.apply(element);
	}
}