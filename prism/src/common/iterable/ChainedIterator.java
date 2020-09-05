//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;

import common.functions.ObjDoubleFunction;
import common.functions.ObjIntFunction;
import common.functions.ObjLongFunction;

/**
 * Abstract base class implementing an Iterator that chains a sequence of Iterators.
 * Returns all the elements of the first Iterator, then the elements of the
 * second Iterator and so on.
 * <p>
 * The calls to {@code next()} of the underlying Iterator happen on-the-fly,
 * i.e., only when {@code next()} is called for this Iterator.
 * <p>
 * Implementations should release the underlying Iterators after iteration.
 * <p>
 * This Iterator does not support the {@code remove()} method, even if the underlying
 * Iterators support it.
 *
 * @param <E> type of the Iterator's elements
 * @param <I> type of the underlying Iterators
 */
public abstract class ChainedIterator<E, I extends Iterator<E>> implements FunctionalIterator<E>
{
	/** The Iterator over the sequence of Iterators that are chained */
	protected Iterator<? extends I> iterators;
	/** The current Iterator in the sequence of Iterators */
	protected I current;

	/**
	 * Constructor for an Iterator that chains Iterators provided in an array.
	 *
	 * @param iterators an array of Iterators to be chained
	 */
	@SafeVarargs
	public ChainedIterator(I... iterators)
	{
		Objects.requireNonNull(iterators);
		if (iterators.length > 0) {
			this.iterators = new ArrayIterator.Of<>(iterators, 1, iterators.length);
			this.current   = iterators[0];
		} else {
			// set empty instances;
			release();
		}
	}

	/**
	 * Constructor for an Iterator that chains Iterators provided in an Iterator.
	 *
	 * @param iterators an Iterator over Iterators to be chained
	 */
	public ChainedIterator(Iterator<? extends I>iterators)
	{
		Objects.requireNonNull(iterators);
		if (iterators.hasNext()) {
			this.iterators = iterators;
			this.current   = iterators.next();
		} else {
			// set empty instances;
			release();
		}
	}

	/**
	 * Constructor for chaining an Iterator and a number of Iterators provided in an array.
	 *
	 * @param iterator  an Iterator to prepend the chain
	 * @param iterators an array of Iterators to be chained
	 */
	@SafeVarargs
	public ChainedIterator(I iterator, I... iterators)
	{
		this(iterator, new ArrayIterator.Of<>(iterators));
	}

	/**
	 * Constructor for chaining an Iterator and a number of Iterators provided in an Iterator.
	 *
	 * @param iterator  an Iterator to prepend the chain
	 * @param iterators an Iterator over Iterators to be chained
	 */
	public ChainedIterator(I iterator, Iterator<? extends I> iterators)
	{
		Objects.requireNonNull(iterators);
		Objects.requireNonNull(iterator);
		this.iterators = iterators;
		this.current   = iterator;
	}

	@Override
	public boolean hasNext()
	{
		if (current.hasNext()) {
			// the current iterator has another element
			return true;
		}

		// the current iterator has no more elements,
		// search for the next iterator that as an element
		while (iterators.hasNext()) {
			// consider the next iterator
			current = iterators.next();
			if (current.hasNext()) {
				// iterator has element, keep current and return true
				return true;
			}
		}
		// there are no more iterators / elements
		release();
		return false;
	}

	@Override
	public void forEachRemaining(Consumer<? super E> action)
	{
		current.forEachRemaining(action);
		iterators.forEachRemaining(iter -> iter.forEachRemaining(action));
		release();
	}

	@Override
	public long count()
	{
		long count = 0;
		while (hasNext()) {
			if (current instanceof FunctionalIterator) {
				count += ((FunctionalIterator<E>) current).count();
				continue;
			}
			while (current.hasNext()) {
				current.next();
				count++;
			}
		}
		release();
		return count;
	}

	@Override
	public <T> T reduce(T identity, BiFunction<T, ? super E, T> accumulator)
	{
		Objects.requireNonNull(accumulator);
		T result = identity;
		while (hasNext()) {
			if (current instanceof FunctionalIterator) {
				result = ((FunctionalIterator<? extends E>) current).reduce(result, accumulator);
				continue;
			}
			while (current.hasNext()) {
				E next = current.next();
				result = accumulator.apply(result, next);
			}
		}
		return result;
	}

	@Override
	public void release()
	{
		iterators = EmptyIterator.Of();
	}

	/**
	 * Check that there is a next element, throw if not.
	 *
	 * @throws NoSuchElementException if Iterator is exhausted
	 */
	protected void requireNext()
	{
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
	}



	/**
	 * Generic implementation of a chained Iterator.
	 *
	 * @param <E> type of the Iterator's elements
	 */
	public static class Of<E> extends ChainedIterator<E, Iterator<E>>
	{
		/**
		 * Constructor for an Iterator that chains Iterators provided in an array.
		 *
		 * @param iterators an array of Iterators to be chained
		 */
		@SuppressWarnings("unchecked")
		@SafeVarargs
		public Of(Iterator<? extends E>... iterators)
		{
			super((Iterator<E>[]) iterators);
		}

		/**
		 * Constructor for an Iterator that chains Iterators provided in an Iterator.
		 *
		 * @param iterators an Iterator over Iterators to be chained
		 */
		@SuppressWarnings("unchecked")
		public Of(Iterator<? extends Iterator<? extends E>> iterators)
		{
			super((Iterator<? extends Iterator<E>>) iterators);
		}

		/**
		 * Constructor for chaining an Iterator and a number of Iterators provided in an array.
		 *
		 * @param iterator  an Iterator to prepend the chain
		 * @param iterators an array of Iterators to be chained
		 */
		@SuppressWarnings("unchecked")
		@SafeVarargs
		public Of(Iterator<? extends E> iterator, Iterator<? extends E>... iterators)
		{
			super((Iterator<E>) iterator, (Iterator<E>[]) iterators);
		}

		/**
		 * Constructor for chaining an Iterator and a number of Iterators provided in an Iterator.
		 *
		 * @param iterator  an Iterator to prepend the chain
		 * @param iterators an Iterator over Iterators to be chained
		 */
		@SuppressWarnings("unchecked")
		public Of(Iterator<? extends E> iterator, Iterator<? extends Iterator<? extends E>> iterators)
		{
			super((Iterator<E>) iterator, (Iterator<? extends Iterator<E>>) iterators);
		}

		@Override
		public E next()
		{
			requireNext();
			return current.next();
		}

		@Override
		public boolean contains(Object obj)
		{
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					if(((FunctionalPrimitiveIterator.OfDouble) current).contains(obj)) {
						release();
						return true;
					}
					continue;
				}
				while (current.hasNext()) {
					if ((obj == null) ? (current.next() == null) : obj.equals(current.next())) {
						release();
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public void release()
		{
			current   = EmptyIterator.Of();
			iterators = EmptyIterator.Of();
		}
	}



	/**
	 * Primitive specialisation for {@code double} of a chained Iterator.
	 */
	public static class OfDouble extends ChainedIterator<Double, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfDouble
	{
		/**
		 * Constructor for an Iterator that chains Iterators provided in an array.
		 *
		 * @param iterators an array of Iterators to be chained
		 */
		@SafeVarargs
		public OfDouble(PrimitiveIterator.OfDouble... iterators)
		{
			super(iterators);
		}

		/**
		 * Constructor for an Iterator that chains Iterators provided in an Iterator.
		 *
		 * @param iterators an Iterator over Iterators to be chained
		 */
		public OfDouble(Iterator<? extends PrimitiveIterator.OfDouble> iterators)
		{
			super(iterators);
		}

		/**
		 * Constructor for chaining an Iterator and a number of Iterators provided in an array.
		 *
		 * @param iterator  an Iterator to prepend the chain
		 * @param iterators an array of Iterators to be chained
		 */
		@SafeVarargs
		public OfDouble(PrimitiveIterator.OfDouble iterator, PrimitiveIterator.OfDouble... iterators)
		{
			super(iterator, iterators);
		}

		/**
		 * Constructor for chaining an Iterator and a number of Iterators provided in an Iterator.
		 *
		 * @param iterator  an Iterator to prepend the chain
		 * @param iterators an Iterator over Iterators to be chained
		 */
		public OfDouble(PrimitiveIterator.OfDouble iterator, Iterator<PrimitiveIterator.OfDouble> iterators)
		{
			super(iterator, iterators);
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			return current.nextDouble();
		}

		@Override
		public void forEachRemaining(DoubleConsumer action)
		{
			current.forEachRemaining(action);
			iterators.forEachRemaining(iter -> iter.forEachRemaining(action));
			release();
		}

		@Override
		public boolean contains(double d)
		{
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					if(((FunctionalPrimitiveIterator.OfDouble) current).contains(d)) {
						release();
						return true;
					}
					continue;
				}
				while (current.hasNext()) {
					if (d == current.nextDouble()) {
						release();
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public long count()
		{
			long count = 0;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					count += ((FunctionalPrimitiveIterator.OfDouble) current).count();
					continue;
				}
				while (current.hasNext()) {
					// just consume, avoid auto-boxing
					current.nextDouble();
					count++;
				}
			}
			return count;
		}

		@Override
		public OptionalDouble max()
		{
			if (!hasNext()) {
				return OptionalDouble.empty();
			}
			double max = nextDouble();
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					OptionalDouble opt = ((FunctionalPrimitiveIterator.OfDouble) current).max();
					max = Math.max(max, opt.orElse(max));
					continue;
				}
				while (current.hasNext()) {
					max = Math.max(max, current.next());
				}
			}
			return OptionalDouble.of(max);
		}

		@Override
		public OptionalDouble min()
		{
			if (!hasNext()) {
				return OptionalDouble.empty();
			}
			double min = nextDouble();
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					OptionalDouble opt = ((FunctionalPrimitiveIterator.OfDouble) current).min();
					min = Math.min(min, opt.orElse(min));
					continue;
				}
				while (current.hasNext()) {
					min = Math.min(min, current.next());
				}
			}
			return OptionalDouble.of(min);
		}

		@Override
		public <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					result = ((FunctionalPrimitiveIterator.OfDouble) current).reduce(result, accumulator);
					continue;
				}
				while (current.hasNext()) {
					double next = current.nextDouble();
					result      = accumulator.apply(result, next);
				}
			}
			return result;
		}

		@Override
		public double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			double result = identity;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					result = ((FunctionalPrimitiveIterator.OfDouble) current).reduce(result, accumulator);
					continue;
				}
				while (current.hasNext()) {
					double next = current.nextDouble();
					result      = accumulator.applyAsDouble(result, next);
				}
			}
			return result;
		}

		@Override
		public double sum()
		{
			double sum = 0;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					sum += ((FunctionalPrimitiveIterator.OfDouble) current).sum();
					continue;
				}
				while (current.hasNext()) {
					// just consume, avoid auto-boxing
					sum += current.nextDouble();
				}
			}
			return sum;
		}

		@Override
		public void release()
		{
			current   = EmptyIterator.OfDouble();
			iterators = EmptyIterator.Of();
		}
	}



	/**
	 * Primitive specialisation for {@code int} of a chained Iterator.
	 */
	public static class OfInt extends ChainedIterator<Integer, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfInt
	{
		/**
		 * Constructor for an Iterator that chains Iterators provided in an array.
		 *
		 * @param iterators an array of Iterators to be chained
		 */
		@SafeVarargs
		public OfInt(PrimitiveIterator.OfInt... iterators)
		{
			super(iterators);
		}

		/**
		 * Constructor for an Iterator that chains Iterators provided in an Iterator.
		 *
		 * @param iterators an Iterator over Iterators to be chained
		 */
		public OfInt(Iterator<? extends PrimitiveIterator.OfInt> iterators)
		{
			super(iterators);
		}

		/**
		 * Constructor for chaining an Iterator and a number of Iterators provided in an array.
		 *
		 * @param iterator  an Iterator to prepend the chain
		 * @param iterators an array of Iterators to be chained
		 */
		@SafeVarargs
		public OfInt(PrimitiveIterator.OfInt iterator, PrimitiveIterator.OfInt... iterators)
		{
			super(iterator, iterators);
		}

		/**
		 * Constructor for chaining an Iterator and a number of Iterators provided in an Iterator.
		 *
		 * @param iterator  an Iterator to prepend the chain
		 * @param iterators an Iterator over Iterators to be chained
		 */
		public OfInt(PrimitiveIterator.OfInt iterator, Iterator<PrimitiveIterator.OfInt> iterators)
		{
			super(iterator, iterators);
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return current.nextInt();
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			current.forEachRemaining(action);
			iterators.forEachRemaining(iter -> iter.forEachRemaining(action));
			release();
		}

		@Override
		public boolean contains(int d)
		{
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					if(((FunctionalPrimitiveIterator.OfInt) current).contains(d)) {
						release();
						return true;
					}
					continue;
				}
				while (current.hasNext()) {
					if (d == current.nextInt()) {
						release();
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public long count()
		{
			long count = 0;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					count += ((FunctionalPrimitiveIterator.OfInt) current).count();
					continue;
				}
				while (current.hasNext()) {
					// just consume, avoid auto-boxing
					current.nextInt();
					count++;
				}
			}
			return count;
		}

		@Override
		public OptionalInt max()
		{
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			int max = nextInt();
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					OptionalInt opt = ((FunctionalPrimitiveIterator.OfInt) current).max();
					max = Math.max(max, opt.orElse(max));
					continue;
				}
				while (current.hasNext()) {
					max = Math.max(max, current.next());
				}
			}
			return OptionalInt.of(max);
		}

		@Override
		public OptionalInt min()
		{
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			int min = nextInt();
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					OptionalInt opt = ((FunctionalPrimitiveIterator.OfInt) current).min();
					min = Math.min(min, opt.orElse(min));
					continue;
				}
				while (current.hasNext()) {
					min = Math.min(min, current.next());
				}
			}
			return OptionalInt.of(min);
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					result = ((FunctionalPrimitiveIterator.OfInt) current).reduce(result, accumulator);
					continue;
				}
				while (current.hasNext()) {
					int next = current.nextInt();
					result      = accumulator.apply(result, next);
				}
			}
			return result;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = identity;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					result = ((FunctionalPrimitiveIterator.OfInt) current).reduce(result, accumulator);
					continue;
				}
				while (current.hasNext()) {
					int next = current.nextInt();
					result   = accumulator.applyAsInt(result, next);
				}
			}
			return result;
		}

		@Override
		public long sum()
		{
			long sum = 0;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					sum += ((FunctionalPrimitiveIterator.OfInt) current).sum();
					continue;
				}
				while (current.hasNext()) {
					// just consume, avoid auto-boxing
					sum += current.nextInt();
				}
			}
			return sum;
		}

		@Override
		public void release()
		{
			current   = EmptyIterator.OfInt();
			iterators = EmptyIterator.Of();
		}
	}



	/**
	 * Primitive specialisation for {@code long} of a chained iterator.
	 */
	public static class OfLong extends ChainedIterator<Long, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfLong
	{
		/**
		 * Constructor for an Iterator that chains Iterators provided in an array.
		 *
		 * @param iterators an array of Iterators to be chained
		 */
		@SafeVarargs
		public OfLong(PrimitiveIterator.OfLong... iterators)
		{
			super(iterators);
		}

		/**
		 * Constructor for an Iterator that chains Iterators provided in an Iterator.
		 *
		 * @param iterators an Iterator over Iterators to be chained
		 */
		public OfLong(Iterator<? extends PrimitiveIterator.OfLong> iterators)
		{
			super(iterators);
		}

		/**
		 * Constructor for chaining an Iterator and a number of Iterators provided in an array.
		 *
		 * @param iterator  an Iterator to prepend the chain
		 * @param iterators an array of Iterators to be chained
		 */
		@SafeVarargs
		public OfLong(PrimitiveIterator.OfLong iterator, PrimitiveIterator.OfLong... iterators)
		{
			super(iterator, iterators);
		}

		/**
		 * Constructor for chaining an Iterator and a number of Iterators provided in an Iterator.
		 *
		 * @param iterator  an Iterator to prepend the chain
		 * @param iterators an Iterator over Iterators to be chained
		 */
		public OfLong(PrimitiveIterator.OfLong iterator, Iterator<PrimitiveIterator.OfLong> iterators)
		{
			super(iterator, iterators);
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return current.nextLong();
		}

		@Override
		public void forEachRemaining(LongConsumer action)
		{
			current.forEachRemaining(action);
			iterators.forEachRemaining(iter -> iter.forEachRemaining(action));
			release();
		}

		@Override
		public boolean contains(long d)
		{
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					if(((FunctionalPrimitiveIterator.OfLong) current).contains(d)) {
						release();
						return true;
					}
					continue;
				}
				while (current.hasNext()) {
					if (d == current.nextLong()) {
						release();
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public long count()
		{
			long count = 0;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					count += ((FunctionalPrimitiveIterator.OfLong) current).count();
					continue;
				}
				while (current.hasNext()) {
					// just consume, avoid auto-boxing
					current.nextLong();
					count++;
				}
			}
			return count;
		}

		@Override
		public OptionalLong max()
		{
			if (!hasNext()) {
				return OptionalLong.empty();
			}
			long max = nextLong();
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					OptionalLong opt = ((FunctionalPrimitiveIterator.OfLong) current).max();
					max = Math.max(max, opt.orElse(max));
					continue;
				}
				while (current.hasNext()) {
					max = Math.max(max, current.next());
				}
			}
			return OptionalLong.of(max);
		}

		@Override
		public OptionalLong min()
		{
			if (!hasNext()) {
				return OptionalLong.empty();
			}
			long min = nextLong();
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					OptionalLong opt = ((FunctionalPrimitiveIterator.OfLong) current).min();
					min = Math.min(min, opt.orElse(min));
					continue;
				}
				while (current.hasNext()) {
					min = Math.min(min, current.next());
				}
			}
			return OptionalLong.of(min);
		}

		@Override
		public <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					result = ((FunctionalPrimitiveIterator.OfLong) current).reduce(result, accumulator);
					continue;
				}
				while (current.hasNext()) {
					long next = current.nextLong();
					result      = accumulator.apply(result, next);
				}
			}
			return result;
		}

		@Override
		public long reduce(long identity, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			long result = identity;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					result = ((FunctionalPrimitiveIterator.OfLong) current).reduce(result, accumulator);
					continue;
				}
				while (current.hasNext()) {
					long next = current.nextLong();
					result    = accumulator.applyAsLong(result, next);
				}
			}
			return result;
		}

		@Override
		public long sum()
		{
			long sum = 0;
			while (hasNext()) {
				if (current instanceof FunctionalPrimitiveIterator) {
					sum += ((FunctionalPrimitiveIterator.OfLong) current).sum();
					continue;
				}
				while (current.hasNext()) {
					// just consume, avoid auto-boxing
					sum += current.nextLong();
				}
			}
			return sum;
		}

		@Override
		public void release()
		{
			current   = EmptyIterator.OfLong();
			iterators = EmptyIterator.Of();
		}
	}
}
