package common.iterable;

import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;

public interface IterableLong extends Iterable<Long>, FunctionalPrimitiveIterable<Long, LongConsumer>
{
	@Override
	public FunctionalPrimitiveIterator.OfLong iterator();

	// Transforming Methods
	default IterableLong chain(IterableLong... iterables)
	{
		switch (iterables.length) {
		case 0:
			return this;
		case 1:
			return new ChainedIterable.OfLong(this, iterables[0]);
		default:
			return new ChainedIterable.OfLong(this, new ChainedIterable.OfLong(iterables));
		}
	}

	@Override
	default IterableLong dedupe()
	{
		return FilteringIterable.dedupe(this);
	}

	@Override
	default IterableLong drop(int n)
	{
		return new IterableLong()
		{
			@Override
			public FunctionalPrimitiveIterator.OfLong iterator()
			{
				return iterator().drop(n);
			}
		};
	}

	@Override
	default FunctionalIterable<Long> filter(Predicate<? super Long> predicate)
	{
		if (predicate instanceof LongPredicate) {
			return filter((LongPredicate) predicate);
		}
		return FunctionalPrimitiveIterable.super.filter(predicate);
	}
	
	default IterableLong filter(LongPredicate predicate)
	{
		return new FilteringIterable.OfLong(this, predicate);
	}

	@SuppressWarnings("unchecked")
	default <T> FunctionalIterable<T> flatMap(Function<? super Long, ? extends Iterable<? extends T>> function)
	{
		if (function instanceof LongFunction) {
			return flatMap((LongFunction<? extends Iterable<? extends T>>) function);
		}
		return FunctionalPrimitiveIterable.super.flatMap(function);
	}

	default <T> FunctionalIterable<T> flatMap(LongFunction<? extends Iterable<? extends T>> function)
	{
		return new ChainedIterable.Of<>(map(function));
	}

	default long headLong()
	{
		return iterator().headLong();
	}

	@SuppressWarnings("unchecked")
	@Override
	default <T> FunctionalIterable<T> map(Function<? super Long, ? extends T> function)
	{
		if (function instanceof LongFunction) {
			return map((LongFunction<? extends T>) function);
		}
		if (function instanceof LongToDoubleFunction) {
			return (FunctionalIterable<T>) map((LongToDoubleFunction) function);
		}
		if (function instanceof LongToIntFunction) {
			return (FunctionalIterable<T>) map((LongToIntFunction) function);
		}
		if (function instanceof LongUnaryOperator) {
			return (FunctionalIterable<T>) map((LongUnaryOperator) function);
		}
		return FunctionalPrimitiveIterable.super.map(function);
	}

	default <T> FunctionalIterable<T> map(LongFunction<? extends T> function)
	{
		return new MappingIterable.FromLong<>(this, function);
	}

	default IterableDouble map(LongToDoubleFunction function)
	{
		return new MappingIterable.FromLongToDouble(this, function);
	}

	default IterableInt map(LongToIntFunction function)
	{
		return new MappingIterable.FromLongToInt(this, function);
	}

	default IterableLong map(LongUnaryOperator function)
	{
		return new MappingIterable.FromLongToLong(this, function);
	}

	@Override
	default IterableLong tail()
	{
		return drop(1);
	}

	@Override
	default IterableLong take(int n)
	{
		return new IterableLong()
		{
			@Override
			public FunctionalPrimitiveIterator.OfLong iterator()
			{
				return iterator().take(n);
			}
		};
	}

	// Accumulations Methods (Consuming)
	default long[] collect(long[] array)
	{
		return iterator().collect(array);
	}

	default long[] collect(long[] array, int offset)
	{
		return iterator().collect(array, offset);
	}

	default int collectAndCount(long[] array)
	{
		return iterator().collectAndCount(array);
	}

	default int collectCount(long[] array, int offset)
	{
		return iterator().collectAndCount(array, offset);
	}

	default OptionalLong detect(LongPredicate predicate)
	{
		return iterator().detect(predicate);
	}

	default OptionalLong reduce(LongBinaryOperator accumulator)
	{
		return iterator().reduce(accumulator);
	}

	default long reduce(long identity, LongBinaryOperator accumulator)
	{
		return iterator().reduce(identity, accumulator);
	}

	default long sum()
	{
		return iterator().sum();
	}
}