//==============================================================================
//	
//	Copyright (c) 2016-
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
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.function.BiFunction;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;
import java.util.function.LongBinaryOperator;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

import common.functions.ObjDoubleFunction;
import common.functions.ObjIntFunction;
import common.functions.ObjLongFunction;
import common.functions.primitive.DoubleIntOperator;
import common.functions.primitive.LongIntOperator;

/**
 * Abstract base class implementing an Iterator that filters elements by a predicate.
 * Returns only those elements for which the filter predicate evaluates to {@code true}.
 * <p>
 * The calls to {@code next()} of the underlying Iterator happen on-the-fly,
 * i.e., only when {@code next()} is called for this Iterator.
 * <p>
 * Implementations should release the underlying Iterator after iteration.
 * <p>
 * This Iterator does not support the {@code remove()} method, even if the underlying
 * Iterator support it.
 *
 * @param <E> type of the Iterator's elements
 * @param <I> type of the underlying Iterator
 */
public abstract class FilteringIterator<E, I extends Iterator<E>> implements FunctionalIterator<E>
{
	/** The Iterator which elements are filtered */
	protected I iterator;
	/** A flag indicating whether another element exists */
	protected boolean hasNext;

	/**
	 * Constructor for a filtering Iterator without a predicate.
	 *
	 * @param iterator an Iterator to be filtered
	 */
	public FilteringIterator(I iterator)
	{
		Objects.requireNonNull(iterator);
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext()
	{
		return hasNext;
	}

	@Override
	public void release()
	{
		hasNext = false;
	}

	/**
	 * Check that there is a next element, throw if not.
	 *
	 * @throws NoSuchElementException if Iterator is exhausted
	 */
	protected void requireNext()
	{
		if (!hasNext) {
			throw new NoSuchElementException();
		}
	}

	/**
	 * Seek and store the next element for which the filter evaluates to {@code true}.
	 */
	protected abstract void seekNext();




	/**
	 * Generic implementation of a filtering Iterator.
	 *
	 * @param <E> type of the Iterable's elements
	 */
	public static class Of<E> extends FilteringIterator<E, Iterator<E>>
	{
		/** The predicate the Iterator uses to filter the elements */
		protected final Predicate<E> filter;
		/** The next element for which the filter predicates evaluates to {@code true} */
		protected E next;

		/**
		 * Constructor for an Iterator that filters elements by a predicate.
		 *
		 * @param iterator an Iterator to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		@SuppressWarnings("unchecked")
		public Of(Iterator<E> iterator, Predicate<? super E> predicate)
		{
			super(iterator);
			Objects.requireNonNull(predicate);
			this.filter = (Predicate<E>) predicate;
			seekNext();
		}

		@Override
		public E next()
		{
			requireNext();
			E current = next;
			seekNext();
			return current;
		}

		@Override
		public long count()
		{
			if (!hasNext) {
				return 0;
			}
			// count current element
			long count = 1;
			// count remaining elements
			while (iterator.hasNext()) {
				if (filter.test(iterator.next())) {
					count++;
				}
			}
			release();
			return count;
		}

		@Override
		public Optional<E> detect(Predicate<? super E> predicate)
		{
			if (!hasNext) {
				return Optional.empty();
			}
			// test current element
			if (predicate.test(next)) {
				return Optional.of(next);
			}
			// test remaining elements
			if (iterator instanceof FunctionalIterator) {
				return ((FunctionalIterator<E>) iterator).detect(filter.and(predicate));
			}
			while(iterator.hasNext()) {
				next = iterator.next();
				if (filter.test(next) && predicate.test(next)) {
					release();
					return Optional.of(next);
				}
			}
			release();
			return Optional.empty();
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super E, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext) {
				return identity;
			}
			// consume current element
			T result = accumulator.apply(identity, next);
			// consume remaining elements
			if (iterator instanceof FunctionalIterator) {
				result = ((FunctionalIterator<E>) iterator).reduce(result, (r, e) -> filter.test(e) ? accumulator.apply(r, e) : r);
			} else {
				while (iterator.hasNext()) {
					next = iterator.next();
					if (filter.test(next)) {
						result = accumulator.apply(result, next);
					}
				}
			}
			release();
			return result;
		}

		@Override
		public void release()
		{
			super.release();
			iterator = EmptyIterator.Of();
			next     = null;
		}

		@Override
		protected void seekNext()
		{
			while (iterator.hasNext()) {
				next = iterator.next();
				if (filter.test(next)) {
					hasNext = true;
					return;
				}
			}
			release();
		}
	}



	/**
	 * Primitive specialisation for {@code double} of a filtering Iterator.
	 */
	public static class OfDouble extends FilteringIterator<Double, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfDouble
	{
		/** The predicate the Iterator uses to filter the elements */
		protected final DoublePredicate filter;
		/** The next element for which the filter predicates evaluates to {@code true} */
		protected double next;

		/**
		 * Constructor for an Iterator that filters elements by a predicate.
		 *
		 * @param iterator an Iterator to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		public OfDouble(PrimitiveIterator.OfDouble iterator, DoublePredicate predicate)
		{
			super(iterator);
			Objects.requireNonNull(predicate);
			this.filter = predicate;
			seekNext();
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			double current = next;
			seekNext();
			return current;
		}

		@Override
		public boolean contains(double d)
		{
			if (!hasNext) {
				return false;
			}
			// test current element
			if (next == d) {
				return true;
			}
			// test remaining elements
			while(iterator.hasNext()) {
				next = iterator.nextDouble();
				if (next == d && filter.test(next)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public long count()
		{
			if (!hasNext) {
				return 0;
			}
			// count current element
			long count = 1;
			// count remaining elements
			while (iterator.hasNext()) {
				if (filter.test(iterator.nextDouble())) {
					count++;
				}
			}
			release();
			return count;
		}

		@Override
		public OptionalDouble detect(DoublePredicate predicate)
		{
			if (!hasNext) {
				return OptionalDouble.empty();
			}
			// test current element
			if (predicate.test(next)) {
				return OptionalDouble.of(next);
			}
			// test remaining elements
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfDouble) iterator).detect(filter.and(predicate));
			}
			while(iterator.hasNext()) {
				next = iterator.nextDouble();
				if (filter.test(next) && predicate.test(next)) {
					release();
					return OptionalDouble.of(next);
				}
			}
			release();
			return OptionalDouble.empty();
		}

		@Override
		public <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext) {
				return identity;
			}
			// consume current element
			T result = accumulator.apply(identity, next);
			// consume remaining elements
			if (iterator instanceof FunctionalPrimitiveIterator) {
				result = ((FunctionalPrimitiveIterator.OfDouble) iterator).reduce(result, (T r, double e) -> filter.test(e) ? accumulator.apply(r, e) : r);
			} else {
				while (iterator.hasNext()) {
					next = iterator.nextDouble();
					if (filter.test(next)) {
						result = accumulator.apply(result, next);
					}
				}
			}
			release();
			return result;
		}

		@Override
		public double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext) {
				return identity;
			}
			// consume current element
			double result = accumulator.applyAsDouble(identity, next);
			// consume remaining elements
			if (iterator instanceof FunctionalPrimitiveIterator) {
				result = ((FunctionalPrimitiveIterator.OfDouble) iterator).reduce(result, (r, e) -> filter.test(e) ? accumulator.applyAsDouble(r, e) : r);
			} else {
				while (iterator.hasNext()) {
					next = iterator.nextDouble();
					if (filter.test(next)) {
						result = accumulator.applyAsDouble(result, next);
					}
				}
			}
			release();
			return result;
		}

		@Override
		public void release()
		{
			super.release();
			iterator = EmptyIterator.OfDouble();
			next     = 0.0;
		}

		@Override
		protected void seekNext()
		{
			while (iterator.hasNext()) {
				next = iterator.nextDouble();
				if (filter.test(next)) {
					hasNext = true;
					return;
				}
			}
			release();
		}
	}



	/**
	 * Primitive specialisation for {@code int} of a filtering Iterator.
	 */
	public static class OfInt extends FilteringIterator<Integer, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfInt
	{
		/** The predicate the Iterator uses to filter the elements */
		protected final IntPredicate filter;
		/** The next element for which the filter predicates evaluates to {@code true} */
		protected int next;

		/**
		 * Constructor for an Iterator that filters elements by a predicate.
		 *
		 * @param iterator an Iterator to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		public OfInt(PrimitiveIterator.OfInt iterator, IntPredicate predicate)
		{
			super(iterator);
			Objects.requireNonNull(predicate);
			this.filter = predicate;
			seekNext();
		}

		@Override
		public int nextInt()
		{
			requireNext();
			int current = next;
			seekNext();
			return current;
		}

		@Override
		public boolean contains(int i)
		{
			if (!hasNext) {
				return false;
			}
			// test current element
			if (next == i) {
				return true;
			}
			// test remaining elements
			while(iterator.hasNext()) {
				next = iterator.nextInt();
				if (next == i && filter.test(next)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public long count()
		{
			if (!hasNext) {
				return 0;
			}
			// count current element
			long count = 1;
			// count remaining elements
			while (iterator.hasNext()) {
				if (filter.test(iterator.nextInt())) {
					count++;
				}
			}
			release();
			return count;
		}

		@Override
		public OptionalInt detect(IntPredicate predicate)
		{
			if (!hasNext) {
				return OptionalInt.empty();
			}
			// test current element
			if (predicate.test(next)) {
				return OptionalInt.of(next);
			}
			// test remaining elements
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfInt) iterator).detect(filter.and(predicate));
			}
			while(iterator.hasNext()) {
				next = iterator.nextInt();
				if (filter.test(next) && predicate.test(next)) {
					release();
					return OptionalInt.of(next);
				}
			}
			release();
			return OptionalInt.empty();
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext) {
				return identity;
			}
			// consume current element
			T result = accumulator.apply(identity, next);
			// consume remaining elements
			if (iterator instanceof FunctionalPrimitiveIterator) {
				result = ((FunctionalPrimitiveIterator.OfInt) iterator).reduce(result, (T r, int e) -> filter.test(e) ? accumulator.apply(r, e) : r);
			} else {
				while (iterator.hasNext()) {
					next = iterator.nextInt();
					if (filter.test(next)) {
						result = accumulator.apply(result, next);
					}
				}
			}
			release();
			return result;
		}

		@Override
		public double reduce(double identity, DoubleIntOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext) {
				return identity;
			}
			// consume current element
			double result = accumulator.applyAsDouble(identity, next);
			// consume remaining elements
			if (iterator instanceof FunctionalPrimitiveIterator) {
				result = ((FunctionalPrimitiveIterator.OfInt) iterator).reduce(result, (double r, int e) -> filter.test(e) ? accumulator.applyAsDouble(r, e) : r);
			} else {
				while (iterator.hasNext()) {
					next = iterator.nextInt();
					if (filter.test(next)) {
						result = accumulator.applyAsDouble(result, next);
					}
				}
			}
			release();
			return result;
		}


		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext) {
				return identity;
			}
			// consume current element
			int result = accumulator.applyAsInt(identity, next);
			// consume remaining elements
			if (iterator instanceof FunctionalPrimitiveIterator) {
				result = ((FunctionalPrimitiveIterator.OfInt) iterator).reduce(result, (int r, int e) -> filter.test(e) ? accumulator.applyAsInt(r, e) : r);
			} else {
				while (iterator.hasNext()) {
					next = iterator.nextInt();
					if (filter.test(next)) {
						result = accumulator.applyAsInt(result, next);
					}
				}
			}
			release();
			return result;
		}

		@Override
		public long reduce(long identity, LongIntOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext) {
				return identity;
			}
			// consume current element
			long result = accumulator.applyAsLong(identity, next);
			// consume remaining elements
			if (iterator instanceof FunctionalPrimitiveIterator) {
				result = ((FunctionalPrimitiveIterator.OfInt) iterator).reduce(result, (long r, int e) -> filter.test(e) ? accumulator.applyAsLong(r, e) : r);
			} else {
				while (iterator.hasNext()) {
					next = iterator.nextInt();
					if (filter.test(next)) {
						result = accumulator.applyAsLong(result, next);
					}
				}
			}
			release();
			return result;
		}

		@Override
		public void release()
		{
			super.release();
			iterator = EmptyIterator.OfInt();
			next     = 0;
		}

		@Override
		protected void seekNext()
		{
			while (iterator.hasNext()) {
				next = iterator.nextInt();
				if (filter.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}
	}


	/**
	 * Primitive specialisation for {@code long} of a filtering Iterator.
	 */
	public static class OfLong extends FilteringIterator<Long, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfLong
	{
		/** The predicate the Iterator uses to filter the elements */
		protected final LongPredicate filter;
		/** The next element for which the filter predicates evaluates to {@code true} */
		protected long next;

		/**
		 * Constructor for an Iterator that filters elements by a predicate.
		 *
		 * @param iterator an Iterator to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		public OfLong(PrimitiveIterator.OfLong iterator, LongPredicate predicate)
		{
			super(iterator);
			Objects.requireNonNull(predicate);
			this.filter = predicate;
			seekNext();
		}

		@Override
		public long nextLong()
		{
			requireNext();
			long current = next;
			seekNext();
			return current;
		}

		@Override
		public boolean contains(long l)
		{
			if (!hasNext) {
				return false;
			}
			// test current element
			if (next == l) {
				return true;
			}
			// test remaining elements
			while(iterator.hasNext()) {
				next = iterator.nextLong();
				if (next == l && filter.test(next)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public long count()
		{
			if (!hasNext) {
				return 0;
			}
			// count current element
			long count = 1;
			// count remaining elements
			while (iterator.hasNext()) {
				if (filter.test(iterator.nextLong())) {
					count++;
				}
			}
			release();
			return count;
		}

		@Override
		public OptionalLong detect(LongPredicate predicate)
		{
			if (!hasNext) {
				return OptionalLong.empty();
			}
			// test current element
			if (predicate.test(next)) {
				return OptionalLong.of(next);
			}
			// test remaining elements
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfLong) iterator).detect(filter.and(predicate));
			}
			while(iterator.hasNext()) {
				next = iterator.nextLong();
				if (filter.test(next) && predicate.test(next)) {
					release();
					return OptionalLong.of(next);
				}
			}
			release();
			return OptionalLong.empty();
		}

		@Override
		public <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext) {
				return identity;
			}
			// consume current element
			T result = accumulator.apply(identity, next);
			// consume remaining elements
			if (iterator instanceof FunctionalPrimitiveIterator) {
				result = ((FunctionalPrimitiveIterator.OfLong) iterator).reduce(result, (T r, long e) -> filter.test(e) ? accumulator.apply(r, e) : r);
			} else {
				while (iterator.hasNext()) {
					next = iterator.nextLong();
					if (filter.test(next)) {
						result = accumulator.apply(result, next);
					}
				}
			}
			release();
			return result;
		}

		@Override
		public long reduce(long identity, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext) {
				return identity;
			}
			// consume current element
			long result = accumulator.applyAsLong(identity, next);
			// consume remaining elements
			if (iterator instanceof FunctionalPrimitiveIterator) {
				result = ((FunctionalPrimitiveIterator.OfLong) iterator).reduce(result, (r, e) -> filter.test(e) ? accumulator.applyAsLong(r, e) : r);
			} else {
				while (iterator.hasNext()) {
					next = iterator.nextLong();
					if (filter.test(next)) {
						result = accumulator.applyAsLong(result, next);
					}
				}
			}
			release();
			return result;
		}

		@Override
		public void release()
		{
			super.release();
			iterator = EmptyIterator.OfLong();
			next     = 0;
		}

		@Override
		protected void seekNext()
		{
			while (iterator.hasNext()) {
				next = iterator.nextLong();
				if (filter.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}
	}
}
