package common.iterable;

import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

public interface IterableInt extends Iterable<Integer>, FunctionalPrimitiveIterable<Integer, IntConsumer>
{
	@Override
	public FunctionalPrimitiveIterator.OfInt iterator();

	// Transforming Methods
	default IterableInt chain(IterableInt... iterables)
	{
		return new ChainedIterable.OfInt(this, new ChainedIterable.OfInt(iterables));
	}

	@Override
	default IterableInt dedupe()
	{
		return FilteringIterable.dedupe(this);
	}

	@Override
	default IterableInt drop(int n)
	{
		return new IterableInt()
		{
			@Override
			public FunctionalPrimitiveIterator.OfInt iterator()
			{
				return iterator().drop(n);
			}
		};
	}

	@Override
	default FunctionalIterable<Integer> filter(Predicate<? super Integer> predicate)
	{
		if (predicate instanceof IntPredicate) {
			return filter((IntPredicate) predicate);
		}
		return FunctionalPrimitiveIterable.super.filter(predicate);
	}

	default IterableInt filter(IntPredicate predicate)
	{
		return new FilteringIterable.OfInt(this, predicate);
	}

	@SuppressWarnings("unchecked")
	default <T> FunctionalIterable<T> flatMap(Function<? super Integer, ? extends Iterable<? extends T>> function)
	{
		if (function instanceof IntFunction) {
			return flatMap((IntFunction<? extends Iterable<? extends T>>) function);
		}
		return FunctionalPrimitiveIterable.super.flatMap(function);
	}

	default <T> FunctionalIterable<T> flatMap(IntFunction<? extends Iterable<? extends T>> function)
	{
		return new ChainedIterable.Of<>(map(function));
	}

	default int headInt()
	{
		return iterator().headInt();
	}

	@SuppressWarnings("unchecked")
	@Override
	default <T> FunctionalIterable<T> map(Function<? super Integer, ? extends T> function)
	{
		if (function instanceof IntFunction) {
			return map((IntFunction<? extends T>) function);
		}
		if (function instanceof IntToDoubleFunction) {
			return (FunctionalIterable<T>) map((IntToDoubleFunction) function);
		}
		if (function instanceof IntUnaryOperator) {
			return (FunctionalIterable<T>) map((IntUnaryOperator) function);
		}
		if (function instanceof IntToLongFunction) {
			return (FunctionalIterable<T>) map((IntToLongFunction) function);
		}
		return FunctionalPrimitiveIterable.super.map(function);
	}

	default <T> FunctionalIterable<T> map(IntFunction<? extends T> function)
	{
		return new MappingIterable.FromInt<>(this, function);
	}

	default IterableDouble map(IntToDoubleFunction function)
	{
		return new MappingIterable.FromIntToDouble(this, function);
	}

	default IterableInt map(IntUnaryOperator function)
	{
		return new MappingIterable.FromIntToInt(this, function);
	}

	default IterableLong map(IntToLongFunction function)
	{
		return new MappingIterable.FromIntToLong(this, function);
	}

	@Override
	default IterableInt tail()
	{
		return drop(1);
	}

	@Override
	default IterableInt take(int n)
	{
		return new IterableInt()
		{
			@Override
			public FunctionalPrimitiveIterator.OfInt iterator()
			{
				return iterator().take(n);
			}
		};
	}

	// Accumulations Methods (Consuming)
	default boolean anyMatch(IntPredicate predicate)
	{
		return iterator().anyMatch(predicate);
	}

	default int[] collect(int[] array)
	{
		return iterator().collect(array);
	}

	default int[] collect(int[] array, int offset)
	{
		return iterator().collect(array, offset);
	}

	default int collectAndCount(int[] array)
	{
		return iterator().collectAndCount(array);
	}

	default int collectAndCount(int[] array, int offset)
	{
		return iterator().collectAndCount(array, offset);
	}

	default OptionalInt detect(IntPredicate predicate)
	{
		return iterator().detect(predicate);
	}

	default OptionalInt reduce(IntBinaryOperator accumulator)
	{
		return iterator().reduce(accumulator);
	}

	default int reduce(int identity, IntBinaryOperator accumulator)
	{
		return iterator().reduce(identity, accumulator);
	}

	default int sum()
	{
		return iterator().sum();
	}
}