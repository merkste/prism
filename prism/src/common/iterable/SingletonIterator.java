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
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.BiFunction;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

import common.functions.ObjDoubleFunction;
import common.functions.ObjIntFunction;
import common.functions.ObjLongFunction;

/**
 * Abstract base class for Iterators ranging over a single element.
 *
 * @param <E> type of the Iterators's elements
 */
public abstract class SingletonIterator<E> implements FunctionalIterator<E>
{
	@Override
	public SingletonIterator<E> dedupe()
	{
		return this;
	}

	@Override
	public SingletonIterator<E> distinct()
	{
		return this;
	}

	@Override
	public void forEachRemaining(Consumer<? super E> action)
	{
		if (hasNext()) {
			action.accept(next());
		}
	}


	@Override
	public long count()
	{
		if (hasNext()) {
			next();
			return 1;
		}
		return 0;
	}

	@Override
	public long collectAndCount(Collection<? super E> collection)
	{
		if (hasNext()) {
			collection.add(next());
			return 1;
		}
		return 0;
	}

	@Override
	public int collectAndCount(E[] array, int offset)
	{
		if (hasNext()) {
			array[offset] = next();
			return 1;
		}
		return 0;
	}

	@Override
	public boolean contains(Object obj)
	{
		return hasNext() && next().equals(obj);
	}

	@Override
	public <T> T reduce(T identity, BiFunction<T, ? super E, T> accumulator)
	{
		Objects.requireNonNull(accumulator);
		return hasNext() ? accumulator.apply(identity, next()) : identity;
	}



	/**
	 * Generic implementation of an singleton Iterator.
	 *
	 * @param <E> type of the Iterator's elements
	 */
	public static class Of<E> extends SingletonIterator<E>
	{
		/** The single element */
		protected Optional<E> element;

		/**
		 * Constructor for an Iterator ranging over a single element.
		 *
		 * @param element the single element of the Iterator
		 */
		public Of(E element)
		{
			this.element = Optional.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public E next()
		{
			E next = element.get();
			release();
			return next;
		}

		@Override
		public void release()
		{
			element = Optional.empty();
		}
	}



	/**
	 * Primitive specialisation for {@code double} of an singleoton Iterator.
	 */
	public static class OfDouble extends SingletonIterator<Double> implements FunctionalPrimitiveIterator.OfDouble
	{
		/** The single element */
		protected OptionalDouble element;

		/**
		 * Constructor for an Iterator ranging over a single element.
		 *
		 * @param element the single element of the Iterator
		 */
		public OfDouble(double element)
		{
			this.element = OptionalDouble.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public double nextDouble()
		{
			double next = element.getAsDouble();
			release();
			return next;
		}

		@Override
		public void forEachRemaining(DoubleConsumer action)
		{
			if (hasNext()) {
				action.accept(nextDouble());
			}
		}

		@Override
		public SingletonIterator.OfDouble dedupe()
		{
			return this;
		}

		@Override
		public SingletonIterator.OfDouble distinct()
		{
			return this;
		}

		@Override
		public int collectAndCount(Double[] array, int offset)
		{
			if (hasNext()) {
				array[offset] = nextDouble();
				return 1;
			}
			return 0;
		}

		@Override
		public int collectAndCount(double[] array, int offset)
		{
			if (hasNext()) {
				array[offset] = nextDouble();
				return 1;
			}
			return 0;
		}

		@Override
		public boolean contains(double d)
		{
			return hasNext() && nextDouble() == d;
		}

		@Override
		public OptionalDouble max()
		{
			OptionalDouble max = element;
			release();
			return max;
		}

		@Override
		public OptionalDouble min()
		{
			return max();
		}

		@Override
		public <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			return hasNext() ? accumulator.apply(identity, nextDouble()) : identity;
		}

		@Override
		public double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			return hasNext() ? accumulator.applyAsDouble(identity, nextDouble()) : identity;
		}

		@Override
		public double sum()
		{
			return max().orElse(0.0);
		}

		@Override
		public void release()
		{
			element = OptionalDouble.empty();
		}
	}



	/**
	 * Primitive specialisation for {@code int} of an singleoton Iterator.
	 */
	public static class OfInt extends SingletonIterator<Integer> implements FunctionalPrimitiveIterator.OfInt
	{
		/** The single element */
		protected OptionalInt element;

		/**
		 * Constructor for an Iterator ranging over a single element.
		 *
		 * @param element the single element of the Iterator
		 */
		public OfInt(int element)
		{
			this.element = OptionalInt.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public int nextInt()
		{
			int next = element.getAsInt();
			release();
			return next;
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			if (hasNext()) {
				action.accept(nextInt());
			}
		}

		@Override
		public SingletonIterator.OfInt dedupe()
		{
			return this;
		}

		@Override
		public SingletonIterator.OfInt distinct()
		{
			return this;
		}

		@Override
		public int collectAndCount(Integer[] array, int offset)
		{
			if (hasNext()) {
				array[offset] = nextInt();
				return 1;
			}
			return 0;
		}

		@Override
		public int collectAndCount(int[] array, int offset)
		{
			if (hasNext()) {
				array[offset] = nextInt();
				return 1;
			}
			return 0;
		}

		@Override
		public boolean contains(int i)
		{
			return hasNext() && nextInt() == i;
		}

		@Override
		public OptionalInt max()
		{
			OptionalInt max = element;
			release();
			return max;
		}

		@Override
		public OptionalInt min()
		{
			return max();
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			return hasNext() ? accumulator.apply(identity, nextInt()) : identity;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			return hasNext() ? accumulator.applyAsInt(identity, nextInt()) : identity;
		}

		@Override
		public long sum()
		{
			return max().orElse(0);
		}

		@Override
		public void release()
		{
			element = OptionalInt.empty();
		}
	}



	/**
	 * Primitive specialisation for {@code long} of an singleoton Iterator.
	 */
	public static class OfLong extends SingletonIterator<Long> implements FunctionalPrimitiveIterator.OfLong
	{
		/** The single element */
		protected OptionalLong element;

		/**
		 * Constructor for an Iterator ranging over a single element.
		 *
		 * @param element the single element of the Iterator
		 */
		public OfLong(Long element)
		{
			this.element = OptionalLong.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public long nextLong()
		{
			long next = element.getAsLong();
			release();
			return next;
		}

		@Override
		public void forEachRemaining(LongConsumer action)
		{
			if (hasNext()) {
				action.accept(nextLong());
			}
		}

		@Override
		public SingletonIterator.OfLong dedupe()
		{
			return this;
		}

		@Override
		public SingletonIterator.OfLong distinct()
		{
			return this;
		}

		@Override
		public int collectAndCount(Long[] array, int offset)
		{
			if (hasNext()) {
				array[offset] = nextLong();
				return 1;
			}
			return 0;
		}

		@Override
		public int collectAndCount(long[] array, int offset)
		{
			if (hasNext()) {
				array[offset] = nextLong();
				return 1;
			}
			return 0;
		}

		@Override
		public boolean contains(long l)
		{
			return hasNext() && nextLong() == l;
		}

		@Override
		public OptionalLong max()
		{
			OptionalLong max = element;
			release();
			return max;

		}

		@Override
		public OptionalLong min()
		{
			return element;
		}

		@Override
		public <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			return hasNext() ? accumulator.apply(identity, nextLong()) : identity;
		}

		@Override
		public long reduce(long identity, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			return hasNext() ? accumulator.applyAsLong(identity, nextLong()) : identity;
		}

		@Override
		public long sum()
		{
			return element.orElse(0);
		}

		@Override
		public void release()
		{
			element = OptionalLong.empty();
		}
	}
}
