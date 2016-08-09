package common.iterable;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import common.iterable.FunctionalPrimitiveIterator.OfDouble;
import common.iterable.FunctionalPrimitiveIterator.OfInt;
import common.iterable.FunctionalPrimitiveIterator.OfLong;

public interface FunctionalIterator<E> extends Iterator<E>
{
	@SuppressWarnings("unchecked")
	public static <E> FunctionalIterator<E> extend(Iterator<E> iterator)
	{
		if (iterator instanceof FunctionalIterator) {
			return (FunctionalIterator<E>) iterator;
		}
		if ((iterator instanceof PrimitiveIterator.OfDouble) || (iterator instanceof PrimitiveIterator.OfInt) || (iterator instanceof PrimitiveIterator.OfLong)) {
			return extend((PrimitiveIterator<E,?>) iterator);
		}
		return new FunctionalIterator<E>()
		{
			@Override
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			@Override
			public E next()
			{
				return iterator.next();
			}

			@Override
			public void forEachRemaining(Consumer<? super E> action)
			{
				iterator.forEachRemaining(action);
			}

			@Override
			public Iterator<E> unwrap()
			{
				return iterator;
			}
		};
	}

	@SuppressWarnings("unchecked")
	public static <E, E_CONS> FunctionalPrimitiveIterator<E,E_CONS> extend(PrimitiveIterator<E, E_CONS> iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator) {
			return (FunctionalPrimitiveIterator<E, E_CONS>) iterator;
		}
		if (iterator instanceof PrimitiveIterator.OfDouble) {
			return (FunctionalPrimitiveIterator<E, E_CONS>) new OfDouble()
			{
				@Override
				public boolean hasNext()
				{
					return ((PrimitiveIterator.OfDouble) iterator).hasNext();
				}

				@Override
				public double nextDouble()
				{
					return ((PrimitiveIterator.OfDouble) iterator).nextDouble();
				}

				@Override
				public void forEachRemaining(DoubleConsumer action)
				{
					((PrimitiveIterator.OfDouble) iterator).forEachRemaining(action);
				}

				@Override
				public PrimitiveIterator.OfDouble unwrap()
				{
					return (PrimitiveIterator.OfDouble) iterator;
				}
			};
		}
		if (iterator instanceof PrimitiveIterator.OfInt) {
			return (FunctionalPrimitiveIterator<E, E_CONS>) new OfInt()
			{
				@Override
				public boolean hasNext()
				{
					return ((PrimitiveIterator.OfInt) iterator).hasNext();
				}

				@Override
				public int nextInt()
				{
					return ((PrimitiveIterator.OfInt) iterator).nextInt();
				}

				@Override
				public void forEachRemaining(IntConsumer action)
				{
					((PrimitiveIterator.OfInt) iterator).forEachRemaining(action);
				}

				@Override
				public PrimitiveIterator.OfInt unwrap()
				{
					return (PrimitiveIterator.OfInt) iterator;
				}
			};
		}
		if (iterator instanceof PrimitiveIterator.OfLong) {
			return (FunctionalPrimitiveIterator<E, E_CONS>) new OfLong()
			{
				@Override
				public boolean hasNext()
				{
					return ((PrimitiveIterator.OfLong) iterator).hasNext();
				}

				@Override
				public long nextLong()
				{
					return ((PrimitiveIterator.OfLong) iterator).nextLong();
				}

				@Override
				public void forEachRemaining(LongConsumer action)
				{
					((PrimitiveIterator.OfLong) iterator).forEachRemaining(action);
				}

				@Override
				public PrimitiveIterator.OfLong unwrap()
				{
					return (PrimitiveIterator.OfLong) iterator;
				}
			};
		}
		throw new IllegalArgumentException("Unknown primitive iterator type, cannot extend.");
	}



	/**
	 * Unwrap a nested iterator if this instance is a decorating wrapper.
	 * Use to avoid repeated indirections, especially in loops.
	 * 
	 * @return {@code this} instance or the wrapped iterator.
	 */
	default Iterator<E> unwrap()
	{
		return this;
	}

	// Transforming Methods
	@SuppressWarnings("unchecked")
	default FunctionalIterator<E> chain(Iterator<? extends E>... iterators)
	{
		switch (iterators.length) {
		case 0:
			return this;
		case 1:
			return new ChainedIterator.Of<>(unwrap(), iterators[0]);
		default:
			return new ChainedIterator.Of<>(unwrap(), new ChainedIterator.Of<>(iterators));
		}
	}

	default FunctionalIterator<E> chain(Iterator<Iterator<? extends E>> iterators)
	{
		return new ChainedIterator.Of<>(unwrap(), new ChainedIterator.Of<>(iterators));
	}

	default FunctionalIterator<E> dedupe()
	{
		return FilteringIterator.dedupe(unwrap());
	}

	default FunctionalIterator<E> drop(int n)
	{
		Iterator<E> iterator = unwrap();
		return new FunctionalIterator<E>()
		{
			int count=n;

			@Override
			public E next()
			{
				while (count>0) {
					count--;
					iterator.next();
				}
				return iterator.next();
			}

			@Override
			public boolean hasNext()
			{
				while (count>0) {
					if (iterator.hasNext()) {
						count--;
						iterator.next();
					} else {
						count = 0;
					}
				}
				return iterator.hasNext();
			}
		};
	}

	default FunctionalIterator<E> filter(Predicate<? super E> function)
	{
		return new FilteringIterator.Of<>(unwrap(), function);
	}

	default <T> FunctionalIterator<T> flatMap(Function<? super E, ? extends Iterator<? extends T>> function)
	{
		return new ChainedIterator.Of<>(map(function));
	}

	default <T> FunctionalIterator<T> map(Function<? super E, ? extends T> function)
	{
		return new MappingIterator.From<>(unwrap(), function);
	}

	default FunctionalPrimitiveIterator.OfInt map(ToIntFunction<? super E> function)
	{
		return new MappingIterator.ToInt<>(unwrap(), function);
	}

	default FunctionalPrimitiveIterator.OfDouble map(ToDoubleFunction<? super E> function)
	{
		return new MappingIterator.ToDouble<>(unwrap(), function);
	}

	default FunctionalPrimitiveIterator.OfLong map(ToLongFunction<? super E> function)
	{
		return new MappingIterator.ToLong<>(unwrap(), function);
	}

	default FunctionalIterator<E> take(int n)
	{
		Iterator<E> iterator = unwrap();
		return new FunctionalIterator<E>()
		{
			int count=n;

			@Override
			public E next()
			{
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				count--;
				return iterator.next();
			}

			@Override
			public boolean hasNext()
			{
				return count > 0 && iterator.hasNext();
			}
		};
	}

	// Accumulations Methods (Consuming)
	default boolean allMatch(Predicate<? super E> predicate)
	{
		Iterator<E> self = unwrap();
		while (self.hasNext()) {
			if (! predicate.test(self.next())) {
				return false;
			}
		}
		return true;
	}

	default boolean anyMatch(Predicate<? super E> predicate)
	{
		return detect(predicate).isPresent();
	}

	default <C extends Collection<? super E>> C collect(Supplier<? extends C> constructor)
	{
		C collection = constructor.get();
		collect(collection);
		return collection;
	}

	default <C extends Collection<? super E>> C collect(C collection)
	{
		Iterator<E> self = unwrap();
		while (self.hasNext()) {
			collection.add(self.next());
		}
		return collection;
	}

	default E[] collect(E[] array)
	{
		return collect(array, 0);
	}

	default E[] collect(E[] array, int offset)
	{
		Iterator<E> self = unwrap();
		int count = offset;
		while (self.hasNext()) {
			array[count++] = self.next();
		}
		return array;
	}

	default int collectAndCount(Collection<? super E> collection)
	{
		Iterator<E> self = unwrap();
		int count = 0;
		while (self.hasNext()) {
			count++;
			collection.add(self.next());
		}
		return count;
	}

	default int collectAndCount(E[] array)
	{
		return collectAndCount(array, 0);
	}

	default int collectAndCount(E[] array, int offset)
	{
		Iterator<E> self = unwrap();
		int count = offset;
		while (self.hasNext()) {
			array[count++] = self.next();
		}
		return count - offset;
	}

	default boolean contains(Object obj)
	{
		Iterator<E> self = unwrap();
		if (obj == null) {
			while (self.hasNext()) {
				if (self.next() == null) {
					return true;
				}
			}
		} else {
			while (self.hasNext()) {
				if (self.next().equals(obj)) {
					return true;
				}
			}
		}
		return false;
	}

	default int count(Predicate<? super E> predicate)
	{
		return filter(predicate).count();
	}

	default int count()
	{
		Iterator<E> self = unwrap();
		int count=0;
		while (self.hasNext()) {
			self.next();
			count++;
		}
		return count;
	}

	default Optional<E> detect(Predicate<? super E> predicate)
	{
		Iterator<E> self = unwrap();
		while (self.hasNext()) {
			E next = self.next();
			if (predicate.test(next)) {
				return Optional.of(next);
			}
		}
		return Optional.empty();
	}

	default E head()
	{
		if(hasNext()) {
			throw new NoSuchElementException();
		}
		return next();
	}

	default Optional<E> reduce(BinaryOperator<E> accumulator)
	{
		if (! hasNext()) {
			return Optional.empty();
		}
		return Optional.of(reduce(next(), accumulator));
	}

	default <T> T reduce(T identity, BiFunction<T, ? super E, T> accumulator)
	{
		Iterator<E> self = unwrap();
		T result = identity;
		while(self.hasNext()) {
			result = accumulator.apply(result, self.next());
		}
		return result;
	}

	default FunctionalIterator<E> tail()
	{
		head();
		return this;
	}

	default String asString()
	{
		Iterator<E> self = unwrap();
		StringBuffer buffer = new StringBuffer("{");
		while (self.hasNext()) {
			buffer.append(self.next());
			if (self.hasNext()) {
				buffer.append(", ");
			}
		}
		buffer.append("}");
		return buffer.toString();
	}
}
