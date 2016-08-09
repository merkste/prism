package common.iterable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
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
import java.util.function.Predicate;

public interface FunctionalPrimitiveIterator<E, E_CONS> extends FunctionalIterator<E>, PrimitiveIterator<E, E_CONS>
{
	public interface OfDouble extends FunctionalPrimitiveIterator<Double, DoubleConsumer>, PrimitiveIterator.OfDouble
	{
		@Override
		default PrimitiveIterator.OfDouble unwrap()
		{
			return this;
		}

		// Transforming Methods
		default FunctionalPrimitiveIterator.OfDouble chain(PrimitiveIterator.OfDouble... iterators)
		{
			return new ChainedIterator.OfDouble(unwrap(), new ChainedIterator.OfDouble(iterators));
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble dedupe()
		{
			return FilteringIterator.dedupe(unwrap());
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble drop(int n)
		{
			PrimitiveIterator.OfDouble iterator = unwrap();
			return new FunctionalPrimitiveIterator.OfDouble()
			{
				int count=n;

				@Override
				public double nextDouble()
				{
					while (count>0) {
						count--;
						iterator.nextDouble();
					}
					return iterator.nextDouble();
				}

				@Override
				public boolean hasNext()
				{
					while (count>0) {
						if (iterator.hasNext()) {
							count--;
							iterator.nextDouble();
						} else {
							count = 0;
						}
					}
					return iterator.hasNext();
				}
			};
		}

		@Override
		default FunctionalIterator<Double> filter(Predicate<? super Double> predicate)
		{
			if (predicate instanceof DoublePredicate) {
				return filter((DoublePredicate) predicate);
			}
			return FunctionalPrimitiveIterator.super.filter(predicate);
		}

		default FunctionalPrimitiveIterator.OfDouble filter(DoublePredicate predicate)
		{
			return new FilteringIterator.OfDouble(unwrap(), predicate);
		}

		@SuppressWarnings("unchecked")
		default <T> FunctionalIterator<T> flatMap(Function<? super Double, ? extends Iterator<? extends T>> function)
		{
			if (function instanceof DoubleFunction) {
				return flatMap((DoubleFunction<? extends Iterator<? extends T>>) function);
			}
			return new ChainedIterator.Of<>(map(function));
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

		@SuppressWarnings("unchecked")
		@Override
		default <T> FunctionalIterator<T> map(Function<? super Double, ? extends T> function)
		{
			if (function instanceof DoubleFunction) {
				return map((DoubleFunction<? extends T>) function);
			}
			if (function instanceof DoubleUnaryOperator) {
				return (FunctionalIterator<T>) map((DoubleUnaryOperator) function);
			}
			if (function instanceof DoubleToIntFunction) {
				return (FunctionalIterator<T>) map((DoubleToIntFunction) function);
			}
			if (function instanceof DoubleToLongFunction) {
				return (FunctionalIterator<T>) map((DoubleToLongFunction) function);
			}
			return FunctionalPrimitiveIterator.super.map(function);
		}

		default <T> FunctionalIterator<T> map(DoubleFunction<? extends T> function)
		{
			return new MappingIterator.FromDouble<>(unwrap(), function);
		}

		default PrimitiveIterator.OfDouble map(DoubleUnaryOperator function)
		{
			return new MappingIterator.FromDoubleToDouble(unwrap(), function);
		}

		default PrimitiveIterator.OfInt map(DoubleToIntFunction function)
		{
			return new MappingIterator.FromDoubleToInt(unwrap(), function);
		}

		default PrimitiveIterator.OfLong map(DoubleToLongFunction function)
		{
			return new MappingIterator.FromDoubleToLong(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble take(int n)
		{
			PrimitiveIterator.OfDouble iterator = unwrap();
			return new FunctionalPrimitiveIterator.OfDouble()
			{
				int count=n;

				@Override
				public double nextDouble()
				{
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					count--;
					return iterator.nextDouble();
				}

				@Override
				public boolean hasNext()
				{
					return count > 0 && iterator.hasNext();
				}
			};
		}

		// Accumulations Methods (Consuming)
		@Override
		default boolean allMatch(Predicate<? super Double> predicate)
		{
			if (predicate instanceof DoublePredicate) {
				return allMatch((DoublePredicate) predicate);
			}
			return FunctionalPrimitiveIterator.super.allMatch(predicate);
		}

		default boolean allMatch(DoublePredicate predicate)
		{
			PrimitiveIterator.OfDouble self = unwrap();
			while (self.hasNext()) {
				if (! predicate.test(self.nextDouble())) {
					return false;
				}
			}
			return true;
		}

		@Override
		default boolean anyMatch(Predicate<? super Double> predicate)
		{
			if (predicate instanceof DoublePredicate) {
				return anyMatch((DoublePredicate) predicate);
			}
			return FunctionalPrimitiveIterator.super.anyMatch(predicate);
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
			PrimitiveIterator.OfDouble self = unwrap();
			int count = offset;
			while (self.hasNext()) {
				array[count++] = self.nextDouble();
			}
			return array;
		}

		default int collectAndCount(double[] array)
		{
			return collectAndCount(array, 0);
		}

		default int collectAndCount(double[] array, int offset)
		{
			PrimitiveIterator.OfDouble self = unwrap();
			int count = offset;
			while (self.hasNext()) {
				array[count++] = self.nextDouble();
			}
			return count - offset;
		}

		@Override
		default boolean contains(Object obj)
		{
			return (obj instanceof Double) && contains(((Double) obj).doubleValue());
		}

		default boolean contains(double d)
		{
			PrimitiveIterator.OfDouble self = unwrap();
			while (self.hasNext()) {
				if (d == self.nextDouble()) {
					return true;
				}
			}
			return false;
		}

		/*
		 * Overridden to avoid boxing
		 * @see common.iterable.FunctionalIterator#count()
		 */
		@Override
		default int count()
		{
			PrimitiveIterator.OfDouble self = unwrap();
			int count=0;
			while (self.hasNext()) {
				self.nextDouble();
				count++;
			}
			return count;
		}

		@Override
		default int count(Predicate<? super Double> predicate)
		{
			if (predicate instanceof DoublePredicate) {
				return count((DoublePredicate) predicate);
			}
			return filter(predicate).count();
		}

		default int count(DoublePredicate predicate)
		{
			return filter(predicate).count();
		}

		@Override
		default Optional<Double> detect(Predicate<? super Double> predicate)
		{
			if (predicate instanceof DoublePredicate) {
				OptionalDouble result = detect((DoublePredicate) predicate);
				return result.isPresent() ? Optional.of(result.getAsDouble()): Optional.empty();
			}
			return FunctionalPrimitiveIterator.super.detect(predicate);
		}

		default OptionalDouble detect(DoublePredicate predicate)
		{
			PrimitiveIterator.OfDouble self = unwrap();
			while (self.hasNext()) {
				double next = self.nextDouble();
				if (predicate.test(next)) {
					return OptionalDouble.of(next);
				}
			}
			return OptionalDouble.empty();
		}

		default double headDouble()
		{
			if(hasNext()) {
				throw new NoSuchElementException();
			}
			return nextDouble();
		}

		@Override
		default Optional<Double> reduce(BinaryOperator<Double> accumulator)
		{
			if (accumulator instanceof DoubleBinaryOperator) {
				OptionalDouble result = reduce((DoubleBinaryOperator) accumulator);
				return result.isPresent() ? Optional.of(result.getAsDouble()): Optional.empty();
			}
			return FunctionalPrimitiveIterator.super.reduce(accumulator);
		}

		default OptionalDouble reduce(DoubleBinaryOperator accumulator)
		{
			if (! hasNext()) {
				return OptionalDouble.empty();
			}
			return OptionalDouble.of(reduce(nextDouble(), accumulator));
		}

		default double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			PrimitiveIterator.OfDouble self = unwrap();
			double result = identity;
			while(self.hasNext()) {
				result = accumulator.applyAsDouble(result, self.nextDouble());
			}
			return result;
		}

		default double sum()
		{
			PrimitiveIterator.OfDouble self = unwrap();
			double sum = 0;
			while (self.hasNext()) {
				sum += self.nextDouble();
			}
			return sum;
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble tail()
		{
			headDouble();
			return this;
		}
	}



	public interface OfInt extends FunctionalPrimitiveIterator<Integer, IntConsumer>, PrimitiveIterator.OfInt
	{
		@Override
		default PrimitiveIterator.OfInt unwrap()
		{
			return this;
		}

		// Transforming Methods
		default FunctionalPrimitiveIterator.OfInt chain(PrimitiveIterator.OfInt... iterators)
		{
			return new ChainedIterator.OfInt(unwrap(), new ChainedIterator.OfInt(iterators));
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt dedupe()
		{
			return FilteringIterator.dedupe(unwrap());
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt drop(int n)
		{
			PrimitiveIterator.OfInt iterator = unwrap();
			return new FunctionalPrimitiveIterator.OfInt()
			{
				int count=n;

				@Override
				public int nextInt()
				{
					while (count>0) {
						count--;
						iterator.nextInt();
					}
					return iterator.nextInt();
				}

				@Override
				public boolean hasNext()
				{
					while (count>0) {
						if (iterator.hasNext()) {
							count--;
							iterator.nextInt();
						} else {
							count = 0;
						}
					}
					return iterator.hasNext();
				}
			};
		}

		@Override
		default FunctionalIterator<Integer> filter(Predicate<? super Integer> predicate)
		{
			if (predicate instanceof IntPredicate) {
				return filter((IntPredicate) predicate);
			}
			return FunctionalPrimitiveIterator.super.filter(predicate);
		}

		default FunctionalPrimitiveIterator.OfInt filter(IntPredicate predicate)
		{
			return new FilteringIterator.OfInt(unwrap(), predicate);
		}

		@SuppressWarnings("unchecked")
		default <T> FunctionalIterator<T> flatMap(Function<? super Integer, ? extends Iterator<? extends T>> function)
		{
			if (function instanceof IntFunction) {
				return flatMap((IntFunction<? extends Iterator<? extends T>>) function);
			}
			return new ChainedIterator.Of<>(map(function));
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

		@SuppressWarnings("unchecked")
		@Override
		default <T> FunctionalIterator<T> map(Function<? super Integer, ? extends T> function)
		{
			if (function instanceof IntFunction) {
				return map((IntFunction<? extends T>) function);
			}
			if (function instanceof IntToDoubleFunction) {
				return (FunctionalIterator<T>) map((IntToDoubleFunction) function);
			}
			if (function instanceof IntUnaryOperator) {
				return (FunctionalIterator<T>) map((IntUnaryOperator) function);
			}
			if (function instanceof IntToLongFunction) {
				return (FunctionalIterator<T>) map((IntToLongFunction) function);
			}

			return FunctionalPrimitiveIterator.super.map(function);
		}

		default <T> FunctionalIterator<T> map(IntFunction<? extends T> function)
		{
			return new MappingIterator.FromInt<>(unwrap(), function);
		}

		default PrimitiveIterator.OfDouble map(IntToDoubleFunction function)
		{
			return new MappingIterator.FromIntToDouble(unwrap(), function);
		}

		default PrimitiveIterator.OfInt map(IntUnaryOperator function)
		{
			return new MappingIterator.FromIntToInt(unwrap(), function);
		}

		default PrimitiveIterator.OfLong map(IntToLongFunction function)
		{
			return new MappingIterator.FromIntToLong(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt take(int n)
		{
			PrimitiveIterator.OfInt iterator = unwrap();
			return new FunctionalPrimitiveIterator.OfInt()
			{
				int count=n;

				@Override
				public int nextInt()
				{
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					count--;
					return iterator.nextInt();
				}

				@Override
				public boolean hasNext()
				{
					return count > 0 && iterator.hasNext();
				}
			};
		}

		// Accumulations Methods (Consuming)
		@Override
		default boolean allMatch(Predicate<? super Integer> predicate)
		{
			if (predicate instanceof IntPredicate) {
				return allMatch((IntPredicate) predicate);
			}
			return FunctionalPrimitiveIterator.super.allMatch(predicate);
		}

		default boolean allMatch(IntPredicate predicate)
		{
			PrimitiveIterator.OfInt self = unwrap();
			while (self.hasNext()) {
				if (! predicate.test(self.nextInt())) {
					return false;
				}
			}
			return true;
		}

		@Override
		default boolean anyMatch(Predicate<? super Integer> predicate)
		{
			if (predicate instanceof IntPredicate) {
				return anyMatch((IntPredicate) predicate);
			}
			return FunctionalPrimitiveIterator.super.anyMatch(predicate);
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
			PrimitiveIterator.OfInt self = unwrap();
			int count = offset;
			while (self.hasNext()) {
				array[count++] = self.nextInt();
			}
			return array;
		}

		default int collectAndCount(int[] array)
		{
			return collectAndCount(array, 0);
		}

		default int collectAndCount(int[] array, int offset)
		{
			PrimitiveIterator.OfInt self = unwrap();
			int count = offset;
			while (self.hasNext()) {
				array[count++] = self.nextInt();
			}
			return count - offset;
		}

		@Override
		default boolean contains(Object obj)
		{
			return (obj instanceof Integer) && contains(((Integer) obj).intValue());
		}

		default boolean contains(int i)
		{
			PrimitiveIterator.OfInt self = unwrap();
			while (self.hasNext()) {
				if (i == self.nextInt()) {
					return true;
				}
			}
			return false;
		}

		/*
		 * Overridden to avoid boxing
		 * @see common.iterable.FunctionalIterator#count()
		 */
		@Override
		default int count()
		{
			PrimitiveIterator.OfInt self = unwrap();
			int count=0;
			while (self.hasNext()) {
				self.nextInt();
				count++;
			}
			return count;
		}

		@Override
		default int count(Predicate<? super Integer> predicate)
		{
			if (predicate instanceof IntPredicate) {
				return count((IntPredicate) predicate);
			}
			return filter(predicate).count();
		}

		default int count(IntPredicate predicate)
		{
			return filter(predicate).count();
		}

		@Override
		default Optional<Integer> detect(Predicate<? super Integer> predicate)
		{
			if (predicate instanceof IntPredicate) {
				OptionalInt result = detect((IntPredicate) predicate);
				return result.isPresent() ? Optional.of(result.getAsInt()): Optional.empty();
			}
			return FunctionalPrimitiveIterator.super.detect(predicate);
		}

		default OptionalInt detect(IntPredicate predicate)
		{
			PrimitiveIterator.OfInt self = unwrap();
			while (self.hasNext()) {
				int next = self.nextInt();
				if (predicate.test(next)) {
					return OptionalInt.of(next);
				}
			}
			return OptionalInt.empty();
		}

		default int headInt()
		{
			if(hasNext()) {
				throw new NoSuchElementException();
			}
			return nextInt();
		}

		@Override
		default Optional<Integer> reduce(BinaryOperator<Integer> accumulator)
		{
			if (accumulator instanceof IntBinaryOperator) {
				OptionalInt result = reduce((IntBinaryOperator) accumulator);
				return result.isPresent() ? Optional.of(result.getAsInt()): Optional.empty();
			}
			return FunctionalPrimitiveIterator.super.reduce(accumulator);
		}

		default OptionalInt reduce(IntBinaryOperator accumulator)
		{
			if (! hasNext()) {
				return OptionalInt.empty();
			}
			return OptionalInt.of(reduce(nextInt(), accumulator));
		}

		default int reduce(int identity, IntBinaryOperator accumulator)
		{
			PrimitiveIterator.OfInt self = unwrap();
			int result = identity;
			while(self.hasNext()) {
				result = accumulator.applyAsInt(result, self.nextInt());
			}
			return result;
		}

		default int sum()
		{
			PrimitiveIterator.OfInt self = unwrap();
			int sum = 0;
			while (self.hasNext()) {
				sum += self.nextInt();
			}
			return sum;
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt tail()
		{
			headInt();
			return this;
		}
	}



	public interface OfLong extends FunctionalPrimitiveIterator<Long, LongConsumer>, PrimitiveIterator.OfLong
	{
		@Override
		default PrimitiveIterator.OfLong unwrap()
		{
			return this;
		}

		// Transforming Methods
		default FunctionalPrimitiveIterator.OfLong chain(PrimitiveIterator.OfLong... iterators)
		{
			return new ChainedIterator.OfLong(unwrap(), new ChainedIterator.OfLong(iterators));
		}

		@Override
		default FunctionalPrimitiveIterator.OfLong dedupe()
		{
			return FilteringIterator.dedupe(unwrap());
		}

		@Override
		default FunctionalPrimitiveIterator.OfLong drop(int n)
		{
			PrimitiveIterator.OfLong iterator = unwrap();
			return new FunctionalPrimitiveIterator.OfLong()
			{
				int count=n;

				@Override
				public long nextLong()
				{
					while (count>0) {
						count--;
						iterator.nextLong();
					}
					return iterator.nextLong();
				}

				@Override
				public boolean hasNext()
				{
					while (count>0) {
						if (iterator.hasNext()) {
							count--;
							iterator.nextLong();
						} else {
							count = 0;
						}
					}
					return iterator.hasNext();
				}
			};
		}

		@Override
		default FunctionalIterator<Long> filter(Predicate<? super Long> predicate)
		{
			if (predicate instanceof LongPredicate) {
				return filter((LongPredicate) predicate);
			}
			return FunctionalPrimitiveIterator.super.filter(predicate);
		}

		default FunctionalPrimitiveIterator.OfLong filter(LongPredicate predicate)
		{
			return new FilteringIterator.OfLong(unwrap(), predicate);
		}

		@SuppressWarnings("unchecked")
		default <T> FunctionalIterator<T> flatMap(Function<? super Long, ? extends Iterator<? extends T>> function)
		{
			if (function instanceof LongFunction) {
				return flatMap((LongFunction<? extends Iterator<? extends T>>) function);
			}
			return new ChainedIterator.Of<>(map(function));
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

		@SuppressWarnings("unchecked")
		@Override
		default <T> FunctionalIterator<T> map(Function<? super Long, ? extends T> function)
		{
			if (function instanceof LongFunction) {
				return map((LongFunction<? extends T>) function);
			}
			if (function instanceof LongToDoubleFunction) {
				return (FunctionalIterator<T>) map((LongToDoubleFunction) function);
			}
			if (function instanceof LongToIntFunction) {
				return (FunctionalIterator<T>) map((LongToIntFunction) function);
			}
			if (function instanceof LongUnaryOperator) {
				return (FunctionalIterator<T>) map((LongUnaryOperator) function);
			}
			return FunctionalPrimitiveIterator.super.map(function);
		}

		default <T> FunctionalIterator<T> map(LongFunction<? extends T> function)
		{
			return new MappingIterator.FromLong<>(unwrap(), function);
		}

		default PrimitiveIterator.OfDouble map(LongToDoubleFunction function)
		{
			return new MappingIterator.FromLongToDouble(unwrap(), function);
		}

		default PrimitiveIterator.OfInt map(LongToIntFunction function)
		{
			return new MappingIterator.FromLongToInt(unwrap(), function);
		}

		default PrimitiveIterator.OfLong map(LongUnaryOperator function)
		{
			return new MappingIterator.FromLongToLong(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfLong take(int n)
		{
			PrimitiveIterator.OfLong iterator = unwrap();
			return new FunctionalPrimitiveIterator.OfLong()
			{
				int count=n;

				@Override
				public long nextLong()
				{
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					count--;
					return iterator.nextLong();
				}

				@Override
				public boolean hasNext()
				{
					return count > 0 && iterator.hasNext();
				}
			};
		}

		// Accumulations Methods (Consuming)
		@Override
		default boolean allMatch(Predicate<? super Long> predicate)
		{
			if (predicate instanceof LongPredicate) {
				return allMatch((LongPredicate) predicate);
			}
			return FunctionalPrimitiveIterator.super.allMatch(predicate);
		}

		default boolean allMatch(LongPredicate predicate)
		{
			PrimitiveIterator.OfLong self = unwrap();
			while (self.hasNext()) {
				if (! predicate.test(self.nextLong())) {
					return false;
				}
			}
			return true;
		}

		@Override
		default boolean anyMatch(Predicate<? super Long> predicate)
		{
			if (predicate instanceof LongPredicate) {
				return anyMatch((LongPredicate) predicate);
			}
			return FunctionalPrimitiveIterator.super.anyMatch(predicate);
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
			PrimitiveIterator.OfLong self = unwrap();
			int count = offset;
			while (self.hasNext()) {
				array[count++] = self.nextLong();
			}
			return array;
		}

		default int collectAndCount(long[] array)
		{
			return collectAndCount(array, 0);
		}

		default int collectAndCount(long[] array, int offset)
		{
			PrimitiveIterator.OfLong self = unwrap();
			int count = offset;
			while (self.hasNext()) {
				array[count++] = self.nextLong();
			}
			return count - offset;
		}

		@Override
		default boolean contains(Object obj)
		{
			return (obj instanceof Long) && contains(((Long) obj).longValue());
		}

		default boolean contains(long l)
		{
			PrimitiveIterator.OfLong self = unwrap();
			while (self.hasNext()) {
				if (l == self.nextLong()) {
					return true;
				}
			}
			return false;
		}

		/*
		 * Overridden to avoid boxing
		 * @see common.iterable.FunctionalIterator#count()
		 */
		@Override
		default int count()
		{
			PrimitiveIterator.OfLong self = unwrap();
			int count=0;
			while (self.hasNext()) {
				self.nextLong();
				count++;
			}
			return count;
		}

		@Override
		default int count(Predicate<? super Long> predicate)
		{
			if (predicate instanceof LongPredicate) {
				return count((LongPredicate) predicate);
			}
			return filter(predicate).count();
		}

		default int count(LongPredicate predicate)
		{
			return filter(predicate).count();
		}

		@Override
		default Optional<Long> detect(Predicate<? super Long> predicate)
		{
			if (predicate instanceof LongPredicate) {
				OptionalLong result = detect((LongPredicate) predicate);
				return result.isPresent() ? Optional.of(result.getAsLong()): Optional.empty();
			}
			return FunctionalPrimitiveIterator.super.detect(predicate);
		}

		default OptionalLong detect(LongPredicate predicate)
		{
			PrimitiveIterator.OfLong self = unwrap();
			while (self.hasNext()) {
				long next = self.nextLong();
				if (predicate.test(next)) {
					return OptionalLong.of(next);
				}
			}
			return OptionalLong.empty();
		}

		default long headLong()
		{
			if(hasNext()) {
				throw new NoSuchElementException();
			}
			return nextLong();
		}

		@Override
		default Optional<Long> reduce(BinaryOperator<Long> accumulator)
		{
			if (accumulator instanceof LongBinaryOperator) {
				OptionalLong result = reduce((LongBinaryOperator) accumulator);
				return result.isPresent() ? Optional.of(result.getAsLong()): Optional.empty();
			}
			return FunctionalPrimitiveIterator.super.reduce(accumulator);
		}

		default OptionalLong reduce(LongBinaryOperator accumulator)
		{
			if (! hasNext()) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(reduce(nextLong(), accumulator));
		}

		default long reduce(long identity, LongBinaryOperator accumulator)
		{
			PrimitiveIterator.OfLong self = unwrap();
			long result = identity;
			while(self.hasNext()) {
				result = accumulator.applyAsLong(result, self.nextLong());
			}
			return result;
		}

		default long sum()
		{
			PrimitiveIterator.OfLong self = unwrap();
			long sum = 0;
			while (self.hasNext()) {
				sum += self.nextLong();
			}
			return sum;
		}

		@Override
		default FunctionalPrimitiveIterator.OfLong tail()
		{
			headLong();
			return this;
		}
	}
}
