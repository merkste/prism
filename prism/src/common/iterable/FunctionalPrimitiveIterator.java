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

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;

import common.functions.ObjDoubleFunction;
import common.functions.ObjIntFunction;
import common.functions.ObjLongFunction;
import common.functions.primitive.DoubleIntOperator;
import common.functions.primitive.LongIntOperator;

//TODO: add stream support ?

public interface FunctionalPrimitiveIterator<E, E_CONS> extends FunctionalIterator<E>, PrimitiveIterator<E, E_CONS>
{
	default FunctionalIterator<E> boxed()
	{
		return this;
	}



	public interface OfDouble extends FunctionalPrimitiveIterator<Double, DoubleConsumer>, PrimitiveIterator.OfDouble
	{
		// Transforming Methods

		default FunctionalPrimitiveIterator.OfDouble concat(PrimitiveIterator.OfDouble... iterators)
		{
			if (iterators.length == 0) {
				return this;
			}
			return new ChainedIterator.OfDouble(unwrap(), iterators);
		}

		/**
		 * Obtain filtering iterator for the given primitive double iterator,
		 * filtering duplicate elements (via HashSet).
		 */
		@Override
		default FunctionalPrimitiveIterator.OfDouble distinct()
		{
			Set<Double> set = new HashSet<>();
			return new FilteringIterator.OfDouble(unwrap(), set::add);
		}

		/**
		 * Obtain filtering iterator for the given primitive double iterator,
		 * filtering consecutive duplicate elements.
		 */
		@Override
		default FunctionalPrimitiveIterator.OfDouble dedupe()
		{
			DoublePredicate unseen = new DoublePredicate()
			{
				boolean isFirst = true;
				double previous = 0;

				@Override
				public boolean test(double d)
				{
					if (isFirst) {
						isFirst = false;
					} else if (previous == d) {
						return false;
					}
					previous = d;
					return true;
				}
			};
			return new FilteringIterator.OfDouble(unwrap(), unseen);
		}

		default FunctionalPrimitiveIterator.OfDouble filter(DoublePredicate predicate)
		{
			return new FilteringIterator.OfDouble(unwrap(), predicate);
		}

		default <T> FunctionalIterator<T> flatMap(DoubleFunction<? extends Iterator<? extends T>> function)
		{
			return new ChainedIterator.Of<>(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfDouble flatMapToDouble(DoubleFunction<PrimitiveIterator.OfDouble> function)
		{
			return new ChainedIterator.OfDouble(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfInt flatMapToInt(DoubleFunction<PrimitiveIterator.OfInt> function)
		{
			return new ChainedIterator.OfInt(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfLong flatMapToLong(DoubleFunction<PrimitiveIterator.OfLong> function)
		{
			return new ChainedIterator.OfLong(map(function));
		}

		default <T> FunctionalIterator<T> map(DoubleFunction<? extends T> function)
		{
			return new MappingIterator.DoubleToObj<>(unwrap(), function);
		}

		default PrimitiveIterator.OfDouble mapToDouble(DoubleUnaryOperator function)
		{
			return new MappingIterator.DoubleToDouble(unwrap(), function);
		}

		default PrimitiveIterator.OfInt mapToInt(DoubleToIntFunction function)
		{
			return new MappingIterator.DoubleToInt(unwrap(), function);
		}

		default PrimitiveIterator.OfLong mapToLong(DoubleToLongFunction function)
		{
			return new MappingIterator.DoubleToLong(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble nonNull()
		{
			return this;
		}

		@Override
		default PrimitiveIterator.OfDouble unwrap()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		default boolean allMatch(DoublePredicate predicate)
		{
			return ! anyMatch(predicate.negate());
		}

		default boolean anyMatch(DoublePredicate predicate)
		{
			return detect(predicate).isPresent();
		}

		default double[] collect(double[] array)
		{
			return collect(array, 0);
		}

		default double[] collect(double[] array, int offset)
		{
			collectAndCount(array, offset);
			return array;
		}

		default int collectAndCount(double[] array)
		{
			return collectAndCount(array, 0);
		}

		default int collectAndCount(double[] array, int offset)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			// avoid auto-boxing of array index
			int count = offset;
			while (local.hasNext()) {
				array[count++] = local.nextDouble();
			}
			release();
			return count - offset;
		}

		@Override
		default boolean contains(Object obj)
		{
			// exploit identity test
			return (obj instanceof Double) && contains(((Double) obj).doubleValue());
		}

		default boolean contains(double d)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			// exploit identity test
			while (local.hasNext()) {
				if (d == local.nextDouble()) {
					release();
					return true;
				}
			}
			release();
			return false;
		}

		@Override
		default long count()
		{
			// do not use reduce to avoid auto-boxing of count variable
			PrimitiveIterator.OfDouble local = unwrap();
			// exploit arithmetic operator
			long count = 0;
			while (local.hasNext()) {
				// just consume, avoid auto-boxing
				local.nextDouble();
				count++;
			}
			release();
			return count;
		}

		default long count(DoublePredicate predicate)
		{
			return filter(predicate).count();
		}

		default OptionalDouble detect(DoublePredicate predicate)
		{
			FunctionalPrimitiveIterator.OfDouble filtered = filter(predicate);
			OptionalDouble result = filtered.hasNext() ? OptionalDouble.of(filtered.next()) : OptionalDouble.empty();
			release();
			return result;
		}

		default OptionalDouble max()
		{
			return reduce((DoubleBinaryOperator) Math::max);
		}

		default OptionalDouble min()
		{
			return reduce((DoubleBinaryOperator) Math::min);
		}

		default OptionalDouble reduce(DoubleBinaryOperator accumulator)
		{
			if (! hasNext()) {
				return OptionalDouble.empty();
			}
			return OptionalDouble.of(reduce(nextDouble(), accumulator));
		}

		default <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			T result                         = identity;
			while(local.hasNext()) {
				result = accumulator.apply(result, local.nextDouble());
			}
			release();
			return result;
		}

		default double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			double result                    = identity;
			while(local.hasNext()) {
				result = accumulator.applyAsDouble(result, local.nextDouble());
			}
			release();
			return result;
		}

		default double sum()
		{
			return reduce(0, Double::sum);
		}
	}



	public interface OfInt extends FunctionalPrimitiveIterator<Integer, IntConsumer>, PrimitiveIterator.OfInt
	{
		// Transforming Methods

		default FunctionalPrimitiveIterator.OfInt concat(PrimitiveIterator.OfInt... iterators)
		{
			if (iterators.length == 0) {
				return this;
			}
			return new ChainedIterator.OfInt(unwrap(), iterators);
		}

		/**
		 * Obtain filtering iterator for the given primitive int iterator,
		 * filtering duplicate elements (via BitSet).
		 */
		@Override
		default FunctionalPrimitiveIterator.OfInt distinct()
		{
			BitSet bits          = new BitSet();
			IntPredicate isFirst = i -> {if (bits.get(i)) return false; else bits.set(i); return true;};
			return new FilteringIterator.OfInt(unwrap(), isFirst);
		}

		/**
		 * Obtain filtering iterator for the given primitive int iterator,
		 * filtering consecutive duplicate elements.
		 */
		@Override
		default FunctionalPrimitiveIterator.OfInt dedupe()
		{
			IntPredicate unseen = new IntPredicate()
			{
				boolean isFirst = true;
				int previous    = 0;

				@Override
				public boolean test(int i)
				{
					if (isFirst) {
						isFirst = false;
					} else if (previous == i) {
						return false;
					}
					previous = i;
					return true;
				}
			};
			return new FilteringIterator.OfInt(unwrap(), unseen);
		}

		default FunctionalPrimitiveIterator.OfInt filter(IntPredicate predicate)
		{
			return new FilteringIterator.OfInt(unwrap(), predicate);
		}

		default <T> FunctionalIterator<T> flatMap(IntFunction<? extends Iterator<? extends T>> function)
		{
			return new ChainedIterator.Of<>(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfDouble flatMapToDouble(IntFunction<PrimitiveIterator.OfDouble> function)
		{
			return new ChainedIterator.OfDouble(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfInt flatMapToInt(IntFunction<PrimitiveIterator.OfInt> function)
		{
			return new ChainedIterator.OfInt(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfLong flatMapToLong(IntFunction<PrimitiveIterator.OfLong> function)
		{
			return new ChainedIterator.OfLong(map(function));
		}

		default <T> FunctionalIterator<T> map(IntFunction<? extends T> function)
		{
			return new MappingIterator.IntToObj<>(unwrap(), function);
		}

		default FunctionalPrimitiveIterator.OfDouble mapToDouble(IntToDoubleFunction function)
		{
			return new MappingIterator.IntToDouble(unwrap(), function);
		}

		default FunctionalPrimitiveIterator.OfInt mapToInt(IntUnaryOperator function)
		{
			return new MappingIterator.IntToInt(unwrap(), function);
		}

		default FunctionalPrimitiveIterator.OfLong mapToLong(IntToLongFunction function)
		{
			return new MappingIterator.IntToLong(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt nonNull()
		{
			return this;
		}

		@Override
		default PrimitiveIterator.OfInt unwrap()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		default boolean allMatch(IntPredicate predicate)
		{
			return ! anyMatch(predicate.negate());
		}

		default boolean anyMatch(IntPredicate predicate)
		{
			return detect(predicate).isPresent();
		}

		default int[] collect(int[] array)
		{
			return collect(array, 0);
		}

		default int[] collect(int[] array, int offset)
		{
			collectAndCount(array, offset);
			return array;
		}

		default BitSet collect(BitSet indices)
		{
			forEachRemaining((IntConsumer) indices::set);
			return indices;
		}

		default int collectAndCount(int[] array)
		{
			return collectAndCount(array, 0);
		}

		default int collectAndCount(int[] array, int offset)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			// avoid auto-boxing of array index
			int count = offset;
			while (local.hasNext()) {
				array[count++] = local.nextInt();
			}
			release();
			return count - offset;
		}

		default int collectAndCount(BitSet indices)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			// avoid auto-boxing of array index
			int count = 0;
			while (local.hasNext()) {
				indices.set(local.nextInt());
				count++;
			}
			release();
			return count;
		}

		@Override
		default boolean contains(Object obj)
		{
			// exploit identity test
			return (obj instanceof Integer) && contains(((Integer) obj).intValue());
		}

		default boolean contains(int i)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			// exploit identity test
			while (local.hasNext()) {
				if (i == local.nextInt()) {
					release();
					return true;
				}
			}
			release();
			return false;
		}

		@Override
		default long count()
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			// exploit arithmetic operator
			long count = 0;
			while (local.hasNext()) {
				local.nextInt();
				count++;
			}
			release();
			return count;
		}

		default long count(IntPredicate predicate)
		{
			return filter(predicate).count();
		}

		default OptionalInt detect(IntPredicate predicate)
		{
			FunctionalPrimitiveIterator.OfInt filtered = filter(predicate);
			OptionalInt result = filtered.hasNext() ? OptionalInt.of(filtered.next()) : OptionalInt.empty();
			release();
			return result;
		}

		default OptionalInt max()
		{
			return reduce((IntBinaryOperator) Math::max);
		}

		default OptionalInt min()
		{
			return reduce((IntBinaryOperator) Math::min);
		}

		default OptionalDouble reduce(DoubleIntOperator accumulator)
		{
			if (! hasNext()) {
				return OptionalDouble.empty();
			}
			return OptionalDouble.of(reduce(nextInt(), accumulator));
		}

		default OptionalInt reduce(IntBinaryOperator accumulator)
		{
			if (! hasNext()) {
				return OptionalInt.empty();
			}
			return OptionalInt.of(reduce(nextInt(), accumulator));
		}

		default OptionalLong reduce(LongIntOperator accumulator)
		{
			if (! hasNext()) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(reduce(nextInt(), accumulator));
		}

		default <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			T result                      = identity;
			while(local.hasNext()) {
				result = accumulator.apply(result, local.nextInt());
			}
			release();
			return result;
		}

		default double reduce(double identity, DoubleIntOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			double result                 = identity;
			while(local.hasNext()) {
				result = accumulator.applyAsDouble(result, local.nextInt());
			}
			release();
			return result;
		}

		default int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			int result                    = identity;
			while(local.hasNext()) {
				result = accumulator.applyAsInt(result, local.nextInt());
			}
			release();
			return result;
		}

		default long reduce(long identity, LongIntOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			long result                   = identity;
			while(local.hasNext()) {
				result = accumulator.applyAsLong(result, local.nextInt());
			}
			release();
			return result;
		}

		default long sum()
		{
			return reduce(0, Long::sum);
		}
	}



	public interface OfLong extends FunctionalPrimitiveIterator<Long, LongConsumer>, PrimitiveIterator.OfLong
	{
		// Transforming Methods

		default FunctionalPrimitiveIterator.OfLong concat(PrimitiveIterator.OfLong... iterators)
		{
			if (iterators.length == 0) {
				return this;
			}
			return new ChainedIterator.OfLong(unwrap(), iterators);
		}

		/**
		 * Obtain filtering iterator for the given primitive long iterator,
		 * filtering duplicate elements (via HashSet).
		 */
		@Override
		default FunctionalPrimitiveIterator.OfLong distinct()
		{
			Set<Long> set = new HashSet<>();
			return new FilteringIterator.OfLong(unwrap(), (LongPredicate) set::add);
		}

		/**
		 * Obtain filtering iterator for the given primitive long iterator,
		 * filtering consecutive duplicate elements.
		 */
		@Override
		default FunctionalPrimitiveIterator.OfLong dedupe()
		{
			LongPredicate unseen = new LongPredicate()
			{
				boolean isFirst = true;
				long previous   = 0;

				@Override
				public boolean test(long l)
				{
					if (isFirst) {
						isFirst = false;
					} else if (previous == l) {
						return false;
					}
					previous = l;
					return true;
				}
			};
			return new FilteringIterator.OfLong(unwrap(), unseen);
		}

		default FunctionalPrimitiveIterator.OfLong filter(LongPredicate predicate)
		{
			return new FilteringIterator.OfLong(unwrap(), predicate);
		}

		default <T> FunctionalIterator<T> flatMap(LongFunction<? extends Iterator<? extends T>> function)
		{
			return new ChainedIterator.Of<>(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfDouble flatMapToDouble(LongFunction<PrimitiveIterator.OfDouble> function)
		{
			return new ChainedIterator.OfDouble(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfInt flatMapToInt(LongFunction<PrimitiveIterator.OfInt> function)
		{
			return new ChainedIterator.OfInt(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfLong flatMapToLong(LongFunction<PrimitiveIterator.OfLong> function)
		{
			return new ChainedIterator.OfLong(map(function));
		}

		default <T> FunctionalIterator<T> map(LongFunction<? extends T> function)
		{
			return new MappingIterator.LongToObj<>(unwrap(), function);
		}

		default PrimitiveIterator.OfDouble mapToDouble(LongToDoubleFunction function)
		{
			return new MappingIterator.LongToDouble(unwrap(), function);
		}

		default PrimitiveIterator.OfInt mapToInt(LongToIntFunction function)
		{
			return new MappingIterator.LongToInt(unwrap(), function);
		}

		default PrimitiveIterator.OfLong mapToLong(LongUnaryOperator function)
		{
			return new MappingIterator.LongToLong(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfLong nonNull()
		{
			return this;
		}

		@Override
		default PrimitiveIterator.OfLong unwrap()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		default boolean allMatch(LongPredicate predicate)
		{
			return ! anyMatch(predicate.negate());
		}

		default boolean anyMatch(LongPredicate predicate)
		{
			return detect(predicate).isPresent();
		}

		default long[] collect(long[] array)
		{
			return collect(array, 0);
		}

		default long[] collect(long[] array, int offset)
		{
			collectAndCount(array, offset);
			return array;
		}

		default int collectAndCount(long[] array)
		{
			return collectAndCount(array, 0);
		}

		default int collectAndCount(long[] array, int offset)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			// avoid auto-boxing of array index
			int count = offset;
			while (local.hasNext()) {
				array[count++] = local.nextLong();
			}
			release();
			return count - offset;
		}

		@Override
		default boolean contains(Object obj)
		{
			// exploit identity test
			return (obj instanceof Long) && contains(((Long) obj).longValue());
		}

		default boolean contains(long l)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			// exploit identity test
			while (local.hasNext()) {
				if (l == local.nextLong()) {
					release();
					return true;
				}
			}
			release();
			return false;
		}

		@Override
		default long count()
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			// exploit arithmetic operator
			long count = 0;
			while (local.hasNext()) {
				local.nextLong();
				count++;
			}
			release();
			return count;
		}

		default long count(LongPredicate predicate)
		{
			return filter(predicate).count();
		}

		default OptionalLong detect(LongPredicate predicate)
		{
			FunctionalPrimitiveIterator.OfLong filtered = filter(predicate);
			OptionalLong result = filtered.hasNext() ? OptionalLong.of(filtered.next()) : OptionalLong.empty();
			release();
			return result;

		}

		default OptionalLong max()
		{
			return reduce((LongBinaryOperator) Math::max);
		}

		default OptionalLong min()
		{
			return reduce((LongBinaryOperator) Math::min);
		}

		default OptionalLong reduce(LongBinaryOperator accumulator)
		{
			if (! hasNext()) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(reduce(nextLong(), accumulator));
		}


		default <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			T result                       = identity;
			while(local.hasNext()) {
				result = accumulator.apply(result, local.nextLong());
			}
			release();
			return result;
		}

		default long reduce(long identity, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			long result = identity;
			while(local.hasNext()) {
				result = accumulator.applyAsLong(result, local.nextLong());
			}
			release();
			return result;
		}

		default long sum()
		{
			return reduce(0, Long::sum);
		}
	}
}
