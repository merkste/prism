package common.iterable;

import java.util.OptionalDouble;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

public interface IterableDouble extends Iterable<Double>, FunctionalPrimitiveIterable<Double, DoubleConsumer>
{
	@Override
	FunctionalPrimitiveIterator.OfDouble iterator();

	// Transforming Methods
	default IterableDouble chain(IterableDouble... iterables)
	{
		switch (iterables.length) {
		case 0:
			return this;
		case 1:
			return new ChainedIterable.OfDouble(this, iterables[0]);
		default:
			return new ChainedIterable.OfDouble(this, new ChainedIterable.OfDouble(iterables));
		}
	}

	@Override
	default IterableDouble dedupe()
	{
		return FilteringIterable.dedupe(this);
	}

	@Override
	default IterableDouble drop(int n)
	{
		return new IterableDouble()
		{
			@Override
			public FunctionalPrimitiveIterator.OfDouble iterator()
			{
				return iterator().drop(n);
			}
		};
	}

	@Override
	default FunctionalIterable<Double> filter(Predicate<? super Double> predicate)
	{
		if (predicate instanceof DoublePredicate) {
			return filter((DoublePredicate) predicate);
		}
		return FunctionalPrimitiveIterable.super.filter(predicate);
	}

	default IterableDouble filter(DoublePredicate predicate)
	{
		return new FilteringIterable.OfDouble(this, predicate);
	}

	@SuppressWarnings("unchecked")
	default <T> FunctionalIterable<T> flatMap(Function<? super Double, ? extends Iterable<? extends T>> function)
	{
		if (function instanceof DoubleFunction) {
			return flatMap((DoubleFunction<? extends Iterable<? extends T>>) function);
		}
		return FunctionalPrimitiveIterable.super.flatMap(function);
	}

	default <T> FunctionalIterable<T> flatMap(DoubleFunction<? extends Iterable<? extends T>> function)
	{
		return new ChainedIterable.Of<>(map(function));
	}

	default double headDouble()
	{
		return iterator().headDouble();
	}

	@SuppressWarnings("unchecked")
	@Override
	default <T> FunctionalIterable<T> map(Function<? super Double, ? extends T> function)
	{
		if (function instanceof DoubleFunction) {
			return map((DoubleFunction<? extends T>) function);
		}
		if (function instanceof DoubleUnaryOperator) {
			return (FunctionalIterable<T>) map((DoubleUnaryOperator) function);
		}
		if (function instanceof DoubleToIntFunction) {
			return (FunctionalIterable<T>) map((DoubleToIntFunction) function);
		}
		if (function instanceof DoubleToLongFunction) {
			return (FunctionalIterable<T>) map((DoubleToLongFunction) function);
		}
		return FunctionalPrimitiveIterable.super.map(function);
	}

	default <T> FunctionalIterable<T> map(DoubleFunction<? extends T> function)
	{
		return new MappingIterable.FromDouble<>(this, function);
	}

	default IterableDouble map(DoubleUnaryOperator function)
	{
		return new MappingIterable.FromDoubleToDouble(this, function);
	}

	default IterableInt map(DoubleToIntFunction function)
	{
		return new MappingIterable.FromDoubleToInt(this, function);
	}

	default IterableLong map(DoubleToLongFunction function)
	{
		return new MappingIterable.FromDoubleToLong(this, function);
	}

	@Override
	default IterableDouble tail()
	{
		return drop(1);
	}

	@Override
	default IterableDouble take(int n)
	{
		return new IterableDouble()
		{
			@Override
			public FunctionalPrimitiveIterator.OfDouble iterator()
			{
				return iterator().take(n);
			}
		};
	}

	// Accumulations Methods (Consuming)
	default boolean anyMatch(DoublePredicate predicate)
	{
		return iterator().anyMatch(predicate);
	}

	default double[] collect(double[] array)
	{
		return iterator().collect(array);
	}

	default double[] collect(double[] array, int offset)
	{
		return iterator().collect(array, offset);
	}

	default int collectAndCount(double[] array)
	{
		return iterator().collectAndCount(array);
	}

	default int collectAndCount(double[] array, int offset)
	{
		return iterator().collectAndCount(array, offset);
	}

	default OptionalDouble detect(DoublePredicate predicate)
	{
		return iterator().detect(predicate);
	}

	default OptionalDouble reduce(DoubleBinaryOperator accumulator)
	{
		return iterator().reduce(accumulator);
	}

	default double reduce(double identity, DoubleBinaryOperator accumulator)
	{
		return iterator().reduce(identity, accumulator);
	}

	default double sum()
	{
		return iterator().sum();
	}

}