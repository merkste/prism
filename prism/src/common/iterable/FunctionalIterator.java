//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package common.iterable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.Set;
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

import common.iterable.MappingIterator.ObjToDouble;
import common.iterable.MappingIterator.ObjToInt;
import common.iterable.MappingIterator.ObjToLong;

//TODO: add stream support ?

public interface FunctionalIterator<E> extends Iterator<E>
{
	/**
	 * Abstract base class of decorators that extend non-functional Iterators with the methods provided by {@link FunctionalIterator}.
	 * Implementations should release the underlying Iterator after iteration.
	 *
	 * @param <E> type of the {@link Iterator}'s elements
	 * @param <I> type of the decorated non-functional Iterator
	 */
	public abstract class IteratorDecorator<E, I extends Iterator<E>> implements FunctionalIterator<E>
	{
		/** the iterator that is decorated */
		protected I iterator;

		/**
		 * Generic constructor that wraps an Iterator.
		 *
		 * @param iterator the {@link Iterator} to be decorated
		 */
		public IteratorDecorator(I iterator)
		{
			Objects.requireNonNull(iterator);
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext()
		{
			if(iterator.hasNext()) {
				return true;
			}
			release();
			return false;
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

		/**
		 * Unwrap a nested iterator if this instance is a decorator wrapping an Iterator.
		 * Use to avoid repeated indirections, especially in loops.
		 *
		 * @return the wrapped iterator.
		 */
		@Override
		public I unwrap()
		{
			return iterator;
		}



		/**
		 * Generic implementation of an {@link IteratorDecorator}.
		 *
		 * @param <E> type of the Iterator's elements
		 */
		public static class Of<E> extends IteratorDecorator<E, Iterator<E>>
		{
			public Of(Iterator<E> iterator)
			{
				super(iterator);
			}

			@Override
			public void release()
			{
				iterator = EmptyIterator.Of();
			}
		}



		/**
		 * Primitive specialisation for {@code double} of an {@link IteratorDecorator}.
		 */
		public static class OfDouble extends IteratorDecorator<Double, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfDouble
		{
			/**
			 * Generic constructor that wraps a PrimitiveIterator.OfDouble.
			 *
			 * @param iterator the {@link PrimitiveIterator.OfDouble} to be decorated
			 */
			public OfDouble(PrimitiveIterator.OfDouble iterator)
			{
				super(iterator);
			}

			@Override
			public double nextDouble()
			{
				return iterator.nextDouble();
			}

			@Override
			public void forEachRemaining(DoubleConsumer action)
			{
				iterator.forEachRemaining(action);
				release();
			}

			@Override
			public PrimitiveIterator.OfDouble unwrap()
			{
				return iterator;
			}

			@Override
			public void release()
			{
				iterator = EmptyIterator.OfDouble();
			}
		}



		/**
		 * Primitive specialisation for {@code int} of an {@link IteratorDecorator}.
		 */
		public static class OfInt extends IteratorDecorator<Integer, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfInt
		{
			/**
			 * Generic constructor that wraps a PrimitiveIterator.OfInt.
			 *
			 * @param iterator the {@link PrimitiveIterator.OfInt} to be decorated
			 */
			public OfInt(PrimitiveIterator.OfInt iterator)
			{
				super(iterator);
			}

			@Override
			public int nextInt()
			{
				return iterator.nextInt();
			}

			@Override
			public void forEachRemaining(IntConsumer action)
			{
				iterator.forEachRemaining(action);
				release();
			}

			@Override
			public PrimitiveIterator.OfInt unwrap()
			{
				return iterator;
			}

			@Override
			public void release()
			{
				iterator = EmptyIterator.OfInt();
			}
		}



		/**
		 * Primitive specialisation for {@code long} of an {@link IteratorDecorator}.
		 */
		public static class OfLong extends IteratorDecorator<Long, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfLong
		{
			/**
			 * Generic constructor that wraps a PrimitiveIterator.OfLong.
			 *
			 * @param iterator the {@link PrimitiveIterator.OfLong} to be decorated
			 */
			public OfLong(PrimitiveIterator.OfLong iterator)
			{
				super(iterator);
			}

			@Override
			public long nextLong()
			{
				return iterator.nextLong();
			}

			@Override
			public void forEachRemaining(LongConsumer action)
			{
				iterator.forEachRemaining(action);
				release();
			}

			@Override
			public PrimitiveIterator.OfLong unwrap()
			{
				return iterator;
			}

			@Override
			public void release()
			{
				iterator = EmptyIterator.OfLong();
			}
		}
	}



	/**
	 * Extend the Iterator of an Iterable with the methods provided by FunctionalIterator.
	 * Return the Iterator if it already implements of FunctionalIterator.
	 *
	 * @param iterable the {@link Iterable} providing the {@link Iterator} to extend
	 * @return a {@link FunctionalIterator} that is either the Iterable's Iterator or a decorator on the Iterator
	 */
	public static <E> FunctionalIterator<E> extend(Iterable<E> iterable)
	{
		return extend(iterable.iterator());
	}

	/**
	 * Extend an Iterator with the methods provided by FunctionalIterator.
	 * If the Iterator is a PrimitiveIterator, the extension is a FunctionalPrimitiveIterator.
	 * Answer the Iterator if it already implements of FunctionalIterator.
	 *
	 * @param iterator the {@link Iterator} to extend
	 * @return a {@link FunctionalIterator} that is either the argument or a decorator on the argument
	 */
	@SuppressWarnings("unchecked")
	public static <E> FunctionalIterator<E> extend(Iterator<E> iterator)
	{
		if (iterator instanceof FunctionalIterator) {
			return (FunctionalIterator<E>) iterator;
		}
		if (iterator instanceof PrimitiveIterator.OfDouble) {
			return (FunctionalIterator<E>) extend((PrimitiveIterator.OfDouble) iterator);
		}
		if (iterator instanceof PrimitiveIterator.OfInt) {
			return (FunctionalIterator<E>) extend((PrimitiveIterator.OfInt) iterator);
		}
		if (iterator instanceof PrimitiveIterator.OfLong) {
			return (FunctionalIterator<E>) extend((PrimitiveIterator.OfLong) iterator);
		}
		return new IteratorDecorator.Of<>(iterator);
	}

	/**
	 * Extend a PrimitiveIterator.OfDouble with the methods provided by FunctionalPrimitiveIterator.
	 * Answer the Iterator if it already implements of FunctionalPrimitiveIterator.
	 *
	 * @param iterator the {@link PrimitiveIterator.OfDouble} to extend
	 * @return a {@link FunctionalPrimitiveIterator.OfDouble} that is either the argument or a decorator on the argument
	 */
	public static FunctionalPrimitiveIterator.OfDouble extend(PrimitiveIterator.OfDouble iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator.OfDouble) {
			return (FunctionalPrimitiveIterator.OfDouble) iterator;
		}
		return new IteratorDecorator.OfDouble(iterator);
	}

	/**
	 * Extend a PrimitiveIterator.OfInt with the methods provided by FunctionalPrimitiveIterator.
	 * Answer the Iterator if it already implements of FunctionalPrimitiveIterator.
	 *
	 * @param iterator the {@link PrimitiveIterator.OfInt} to extend
	 * @return a {@link FunctionalPrimitiveIterator.OfInt} that is either the argument or a decorator on the argument
	 */
	public static FunctionalPrimitiveIterator.OfInt extend(PrimitiveIterator.OfInt iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator.OfInt) {
			return (FunctionalPrimitiveIterator.OfInt) iterator;
		}
		return new IteratorDecorator.OfInt(iterator);
	}

	/**
	 * Extend a PrimitiveIterator.OfLong with the methods provided by FunctionalPrimitiveIterator.
	 * Answer the Iterator if it already implements of FunctionalPrimitiveIterator.
	 *
	 * @param iterator the {@link PrimitiveIterator.OfLong} to extend
	 * @return a {@link FunctionalPrimitiveIterator.OfLong} that is either the argument or a decorator on the argument
	 */
	public static FunctionalPrimitiveIterator.OfLong extend(PrimitiveIterator.OfLong iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator.OfLong) {
			return (FunctionalPrimitiveIterator.OfLong) iterator;
		}
		return new IteratorDecorator.OfLong(iterator);
	}

	/**
	 * Convert an Iterator&lt;Double&gt; to a FunctionalPrimitiveItator.OfDouble by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterator if it already implements of FunctionalPrimitiveIterator.OfDouble.
	 *
	 * @param iterator the {@link Iterator}&lt;Double&gt; to extend
	 * @return a {@link FunctionalPrimitiveIterator.OfDouble} that is either the argument or a decorator on the argument
	 */
	public static FunctionalPrimitiveIterator.OfDouble unboxDouble(Iterator<Double> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfDouble) {
			return FunctionalIterator.extend((PrimitiveIterator.OfDouble) iterator);
		}
		return new ObjToDouble<>(iterator, Double::doubleValue);
	}

	/**
	 * Convert an Iterator&lt;Integer&gt; to a FunctionalPrimitiveItator.OfInt by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterator if it already implements of FunctionalPrimitiveIterator.OfInt.
	 *
	 * @param iterator the {@link Iterator}&lt;Integer&gt; to extend
	 * @return a {@link FunctionalPrimitiveIterator.OfInt} that is either the argument or a decorator on the argument
	 */
	public static FunctionalPrimitiveIterator.OfInt unboxInt(Iterator<Integer> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfInt) {
			return FunctionalIterator.extend((PrimitiveIterator.OfInt) iterator);
		}
		return new ObjToInt<>(iterator, Integer::intValue);
	}

	/**
	 * Convert an Iterator&lt;Long&gt; to a FunctionalPrimitiveItator.OfLong by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterator if it already implements of FunctionalPrimitiveIterator.OfLong.
	 *
	 * @param iterator the {@link Iterator}&lt;Long&gt; to extend
	 * @return a {@link FunctionalPrimitiveIterator.OfLong} that is either the argument or a decorator on the argument
	 */
	public static FunctionalPrimitiveIterator.OfLong unboxLong(Iterator<Long> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfLong) {
			return FunctionalIterator.extend((PrimitiveIterator.OfLong) iterator);
		}
		return new ObjToLong<>(iterator, Long::longValue);
	}



	/**
	 * Unwrap a nested iterator if this instance is a decorating wrapper.
	 * Use to avoid repeated indirections, especially in loops.
	 * 
	 * @return {@code this} instance
	 */
	default Iterator<E> unwrap()
	{
		return this;
	}

	/**
	 * Release resources such as wrapped Iterators making this Iterator empty.
	 * Should be called internally after an Iterator is exhausted.
	 */
	default void release()
	{
		// no-op
	}



	// Transforming Methods

	@SuppressWarnings("unchecked")
	default FunctionalIterator<E> concat(Iterator<? extends E>... iterators)
	{
		Objects.requireNonNull(iterators);
		if (iterators.length == 0) {
			return this;
		}
		return new ChainedIterator.Of<>(unwrap(), iterators);
	}

	default FunctionalIterator<E> concat(Iterator<Iterator<? extends E>> iterators)
	{
		return new ChainedIterator.Of<>(unwrap(), iterators);
	}

	/**
	 * Obtain filtering iterator for the given iterator,
	 * filtering duplicate elements (via HashSet, requires
	 * that {@code equals()} and {@code hashCode()} are properly
	 * implemented).
	 */
	default FunctionalIterator<E> distinct()
	{
		Set<E> set = new HashSet<>();
		return new FilteringIterator.Of<>(unwrap(), set::add);
	}

	/**
	 * Obtain filtering iterator for the given iterator,
	 * filtering consecutive duplicate elements.
	 */
	default FunctionalIterator<E> dedupe()
	{
		Predicate<E> unseen = new Predicate<E>()
		{
			Object previous = new Object();

			@Override
			public boolean test(E obj)
			{
				if (Objects.equals(previous, obj)) {
					return false;
				}
				previous = obj;
				return true;
			}
		};
		return new FilteringIterator.Of<>(unwrap(), unseen);
	}

	default FunctionalIterator<E> filter(Predicate<? super E> function)
	{
		return new FilteringIterator.Of<>(unwrap(), function);
	}

	default <T> FunctionalIterator<T> flatMap(Function<? super E, ? extends Iterator<? extends T>> function)
	{
		return new ChainedIterator.Of<>(map(function));
	}

	default <T> FunctionalPrimitiveIterator.OfDouble flatMapToDouble(Function<? super E, PrimitiveIterator.OfDouble> function)
	{
		return new ChainedIterator.OfDouble(map(function));
	}

	default <T> FunctionalPrimitiveIterator.OfInt flatMapToInt(Function<? super E, PrimitiveIterator.OfInt> function)
	{
		return new ChainedIterator.OfInt(map(function));
	}

	default <T> FunctionalPrimitiveIterator.OfLong flatMapToLong(Function<? super E, PrimitiveIterator.OfLong> function)
	{
		return new ChainedIterator.OfLong(map(function));
	}

	default <T> FunctionalIterator<T> map(Function<? super E, ? extends T> function)
	{
		return new MappingIterator.ObjToObj<>(unwrap(), function);
	}

	default FunctionalPrimitiveIterator.OfDouble mapToDouble(ToDoubleFunction<? super E> function)
	{
		return new MappingIterator.ObjToDouble<>(unwrap(), function);
	}

	default FunctionalPrimitiveIterator.OfInt mapToInt(ToIntFunction<? super E> function)
	{
		return new MappingIterator.ObjToInt<>(unwrap(), function);
	}

	default FunctionalPrimitiveIterator.OfLong mapToLong(ToLongFunction<? super E> function)
	{
		return new MappingIterator.ObjToLong<>(unwrap(), function);
	}

	default FunctionalIterator<E> nonNull()
	{
		return new FilteringIterator.Of<>(unwrap(), Objects::nonNull);
	}



	// Accumulations Methods (Consuming)

	default boolean allMatch(Predicate<? super E> predicate)
	{
		return ! anyMatch(predicate.negate());
	}

	default boolean anyMatch(Predicate<? super E> predicate)
	{
		return detect(predicate).isPresent();
	}

	default String asString()
	{
		StringBuffer buffer = new StringBuffer("{");
		if (hasNext()) {
			buffer.append(next());
		}
		forEachRemaining(each -> buffer.append(", ").append(each));
		release();
		return buffer.append("}").toString();
	}

	default <C extends Collection<? super E>> C collect(Supplier<? extends C> constructor)
	{
		Objects.requireNonNull(constructor);
		C collection = constructor.get();
		collect(collection);
		return collection;
	}

	default <C extends Collection<? super E>> C collect(C collection)
	{
		Objects.requireNonNull(collection);
		unwrap().forEachRemaining(collection::add);
		release();
		return collection;
	}

	default E[] collect(E[] array)
	{
		return collect(array, 0);
	}

	default E[] collect(E[] array, int offset)
	{
		collectAndCount(array, offset);
		return array;
	}

	default long collectAndCount(Collection<? super E> collection)
	{
		Objects.requireNonNull(collection);
		// avoid redirection in wrappers
		Iterator<E> local = unwrap();
		// exploit arithmetic operator
		long count = 0;
		while (local.hasNext()) {
			collection.add(local.next());
			count++;
		}
		release();
		return count;
	}

	default int collectAndCount(E[] array)
	{
		return collectAndCount(array, 0);
	}

	default int collectAndCount(E[] array, int offset)
	{
		Objects.requireNonNull(array);
		// avoid redirection in wrappers
		Iterator<E> local = unwrap();
		// avoid auto-boxing of array index
		int count = offset;
		while (local.hasNext()) {
			array[count++] = local.next();
		}
		release();
		return count - offset;
	}

	default boolean contains(Object obj)
	{
		return anyMatch((obj == null) ? Objects::isNull : obj::equals);
	}

	default long count()
	{
		// avoid redirection in wrappers
		Iterator<E> local = unwrap();
		// exploit arithmetic operator
		long count = 0;
		while (local.hasNext()) {
			local.next();
			count++;
		}
		release();
		return count;
	}

	default long count(Predicate<? super E> predicate)
	{
		return filter(predicate).count();
	}

	default Optional<E> detect(Predicate<? super E> predicate)
	{
		Objects.requireNonNull(predicate);
		FunctionalIterator<E> filtered = filter(predicate);
		Optional<E> result = filtered.hasNext() ? Optional.of(filtered.next()) : Optional.empty();
		release();
		return result;
	}

	default Optional<E> reduce(BinaryOperator<E> accumulator)
	{
		Objects.requireNonNull(accumulator);
		if (! hasNext()) {
			return Optional.empty();
		}
		return Optional.of(reduce(next(), accumulator));
	}

	default <T> T reduce(T identity, BiFunction<T, ? super E, T> accumulator)
	{
		Objects.requireNonNull(accumulator);
		// avoid redirection in wrappers
		Iterator<E> local = unwrap();
		T result          = identity;
		while(local.hasNext()) {
			result = accumulator.apply(result, local.next());
		}
		release();
		return result;
	}
}
