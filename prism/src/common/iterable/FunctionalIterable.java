package common.iterable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public interface FunctionalIterable<E> extends Iterable<E>
{
	@Override
	FunctionalIterator<E> iterator();

	public static <E> FunctionalIterable<E> extend(Iterable<E> iterable)
	{
		if (iterable instanceof FunctionalIterable) {
			return (FunctionalIterable<E>) iterable;
		}

		return new FunctionalIterable<E>()
		{
			@Override
			public FunctionalIterator<E> iterator()
			{
				return FunctionalIterator.extend(iterable.iterator());
			}
		};
	}

	// Testing
	default boolean isEmpty()
	{
		return iterator().hasNext();
	}

	// Transforming Methods
	@SuppressWarnings("unchecked")
	default FunctionalIterable<E> chain(Iterable<? extends E>... iterables)
	{
		switch (iterables.length) {
		case 0:
			return this;
		case 1:
			return new ChainedIterable.Of<>(this, iterables[0]);
		default:
			return new ChainedIterable.Of<>(this, new ChainedIterable.Of<>(iterables));
		}
	}

	default FunctionalIterable<E> chain(Iterable<Iterable<? extends E>> iterables)
	{
		return new ChainedIterable.Of<>(this, new ChainedIterable.Of<>(iterables));
	}

	default FunctionalIterable<E> dedupe()
	{
		return FilteringIterable.Of.dedupe(this);
	}

	default FunctionalIterable<E> drop(int n)
	{
		return new FunctionalIterable<E>()
		{
			@Override
			public FunctionalIterator<E> iterator()
			{
				return iterator().drop(n);
			}
		};
	}

	default FunctionalIterable<E> filter(Predicate<? super E> function)
	{
		return new FilteringIterable.Of<>(this, function);
	}

	default <T> FunctionalIterable<T> flatMap(Function<? super E, ? extends Iterable<? extends T>> function)
	{
		return new ChainedIterable.Of<T>(map(function));
	}

	default <T> IterableDouble flatMapToDouble(Function<? super E, IterableDouble> function)
	{
		return new ChainedIterable.OfDouble(map(function));
	}

	default <T> IterableInt flatMapToInt(Function<? super E, IterableInt> function)
	{
		return new ChainedIterable.OfInt(map(function));
	}

	default <T> IterableLong flatMapToLong(Function<? super E, IterableLong> function)
	{
		return new ChainedIterable.OfLong(map(function));
	}

	default E head()
	{
		return iterator().head();
	}

	default <T> FunctionalIterable<T> map(Function<? super E, ? extends T> function)
	{
		return new MappingIterable.From<>(this, function);
	}

	default IterableInt map(ToIntFunction<? super E> function)
	{
		return new MappingIterable.ToInt<>(this, function);
	}

	default IterableDouble map(ToDoubleFunction<? super E> function)
	{
		return new MappingIterable.ToDouble<>(this, function);
	}

	default IterableLong map(ToLongFunction<? super E> function)
	{
		return new MappingIterable.ToLong<>(this, function);
	}

	default FunctionalIterable<E> tail()
	{
		return drop(1);
	}

	default FunctionalIterable<E> take(int n)
	{
		return new FunctionalIterable<E>()
		{
			@Override
			public FunctionalIterator<E> iterator()
			{
				return iterator().take(n);
			}
		};
	}

	// Accumulations Methods (Consuming)
	default boolean allMatch(Predicate<? super E> predicate)
	{
		return iterator().allMatch(predicate);
	}

	default boolean anyMatch(Predicate<? super E> predicate)
	{
		return iterator().anyMatch(predicate);
	}

	default <C extends Collection<? super E>> C collect(Supplier<? extends C> constructor)
	{
		return iterator().collect(constructor);
	}

	default <C extends Collection<? super E>> C collect(C collection)
	{
		return iterator().collect(collection);
	}

	default E[] collect(E[] array)
	{
		return iterator().collect(array);
	}

	default E[] collect(E[] array, int offset)
	{
		return iterator().collect(array, offset);
	}

	default int collectAndCount(Collection<? super E> collection)
	{
		return iterator().collectAndCount(collection);
	}

	default int collectAndCount(E[] array)
	{
		return iterator().collectAndCount(array);
	}

	default int collectAndCount(E[] array, int offset)
	{
		return iterator().collectAndCount(array, offset);
	}

	default Optional<E> detect(Predicate<? super E> predicate)
	{
		return iterator().detect(predicate);
	}

	default boolean contains(Object obj)
	{
		return iterator().contains(obj);
	}

	default int count()
	{
		return iterator().count();
	}

	default int count(Predicate<? super E> predicate)
	{
		return iterator().count(predicate);
	}

	default Optional<E> reduce(BinaryOperator<E> accumulator)
	{
		return iterator().reduce(accumulator);
	}

	default <T> T reduce(T identity, BiFunction<T, ? super E, T> accumulator)
	{
		return iterator().reduce(identity, accumulator);
	}
}
