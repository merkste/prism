package common.functions.primitive;

import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import common.functions.Mapping;

@FunctionalInterface
public interface MappingInt<T> extends Mapping<Integer, T>, IntFunction<T>
{
	@Override
	default T apply(Integer element)
	{
		return apply(element.intValue());
	}

	default <P> Mapping<P, T> compose(ToIntFunction<? super P> function)
	{
		Objects.requireNonNull(function);
		return each -> apply(function.applyAsInt(each));
	}

	public static <T> MappingInt<T> constant(T value)
	{
		return each -> value;
	}
}