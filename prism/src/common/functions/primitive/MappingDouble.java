package common.functions.primitive;

import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

import common.functions.Mapping;
import common.functions.PairMapping;

@FunctionalInterface
public interface MappingDouble<T> extends Mapping<Double, T>, DoubleFunction<T>
{
	@Override
	default T apply(Double element)
	{
		return apply(element.doubleValue());
	}

	default <P> Mapping<P, T> compose(ToDoubleFunction<? super P> function)
	{
		Objects.requireNonNull(function);
		return each -> apply(function.applyAsDouble(each));
	}

	default <P, Q> PairMapping<P, Q, T> compose(ToDoubleBiFunction<? super P, ? super Q> function)
	{
		Objects.requireNonNull(function);
		return (element1, element2) -> apply(function.applyAsDouble(element1, element2));
	}

	public static <T> MappingDouble<T> constant(T value)
	{
		return each -> value;
	}
}