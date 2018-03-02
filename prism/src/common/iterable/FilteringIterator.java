package common.iterable;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

public abstract class FilteringIterator<E, I extends Iterator<E>> implements FunctionalIterator<E>
{
	protected I iterator;
	protected boolean hasNext;

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

	protected abstract void seekNext();

	protected void release()
	{
		hasNext  = false;
	}

	protected void requireNext()
	{
		if (!hasNext) {
			throw new NoSuchElementException();
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> FunctionalIterator<T> dedupe(Iterator<T> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfDouble) {
			return (FunctionalIterator<T>) dedupe((PrimitiveIterator.OfDouble) iterator);
		}
		if (iterator instanceof PrimitiveIterator.OfInt) {
			return (FunctionalIterator<T>) dedupe((PrimitiveIterator.OfInt) iterator);
		}
		if (iterator instanceof PrimitiveIterator.OfLong) {
			return (FunctionalIterator<T>) dedupe((PrimitiveIterator.OfLong) iterator);
		}
		Set<T> set = new HashSet<>();
		return new FilteringIterator.Of<>(iterator, set::add);
	}

	public static FunctionalPrimitiveIterator.OfDouble dedupe( PrimitiveIterator.OfDouble iterator)
	{
	Set<Double> set = new HashSet<>();
		return new FilteringIterator.OfDouble(iterator, set::add);
	}

	public static FunctionalPrimitiveIterator.OfInt dedupe(PrimitiveIterator.OfInt iterator)
	{
		BitSet bits      = new BitSet();
		IntPredicate set = (int i) -> {if (bits.get(i)) return false; else bits.set(i); return true;};
		return new FilteringIterator.OfInt(iterator, set);
	}

	public static FunctionalPrimitiveIterator.OfLong dedupe(PrimitiveIterator.OfLong iterator)
	{
		Set<Long> set = new HashSet<>();
		return new FilteringIterator.OfLong(iterator, (LongPredicate) set::add);
	}

	@SuppressWarnings("unchecked")
	public static <T> Iterator<T> isNull(Iterator<T> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfDouble) {
			return (Iterator<T>) EmptyIterator.OfDouble();
		} else if (iterator instanceof PrimitiveIterator.OfInt) {
			return (Iterator<T>) EmptyIterator.OfInt();
		} else if (iterator instanceof PrimitiveIterator.OfLong) {
			return(Iterator<T>) EmptyIterator.OfLong();
		}
		return new FilteringIterator.Of<>(iterator, Objects::isNull);
	}

	public static <T> Iterator<T> nonNull(Iterator<T> iterator)
	{
		if (iterator instanceof PrimitiveIterator) {
			return iterator;
		}
		return new FilteringIterator.Of<>(iterator, Objects::nonNull);
	}



	public static class Of<E> extends FilteringIterator<E, Iterator<E>>
	{
		protected Predicate<E> filter;
		protected E next;

		@SuppressWarnings("unchecked")
		public Of(Iterator<E> iterator, Predicate<? super E> predicate)
		{
			super(iterator);
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
		public void forEachRemaining(Consumer<? super E> action)
		{
			Objects.requireNonNull(action);
			if (!hasNext) {
				return;
			}
			// consume current element
			action.accept(next);
			// consume remaining elements
			iterator.forEachRemaining(each -> {if (filter.test(each)) action.accept(each);});
			release();
		}

		@Override
		public int count()
		{
			if (!hasNext) {
				return 0;
			}
			// count current element
			int count = 1;
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
					return Optional.of(next);
				}
			}
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

		@Override
		protected void release()
		{
			super.release();
			iterator = EmptyIterator.Of();
			filter   = x -> false;
			next     = null;
		}
	}



	public static class OfDouble extends FilteringIterator<Double, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfDouble
	{
		protected DoublePredicate filter;
		protected double next;

		public OfDouble(PrimitiveIterator.OfDouble iterator, DoublePredicate predicate)
		{
			super(iterator);
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
		public void forEachRemaining(DoubleConsumer action)
		{
			Objects.requireNonNull(action);
			if (!hasNext) {
				return;
			}
			// consume current element
			action.accept(next);
			// consume remaining elements
			iterator.forEachRemaining((double each) -> {if (filter.test(each)) action.accept(each);});
			release();
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
		public int count()
		{
			if (!hasNext) {
				return 0;
			}
			// count current element
			int count = 1;
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
					return OptionalDouble.of(next);
				}
			}
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
		protected void seekNext()
		{
			while (iterator.hasNext()) {
				next = ((PrimitiveIterator.OfDouble) iterator).nextDouble();
				if (filter.test(next)) {
					hasNext = true;
					return;
				}
			}
			release();
		}

		@Override
		protected void release()
		{
			super.release();
			iterator = EmptyIterator.OfDouble();
			filter   = x -> false;
			next     = 0.0;
		}
	}



	public static class OfInt extends FilteringIterator<Integer, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfInt
	{
		protected IntPredicate filter;
		protected int next;

		public OfInt(PrimitiveIterator.OfInt iterator, IntPredicate predicate)
		{
			super(iterator);
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
		public void forEachRemaining(IntConsumer action)
		{
			Objects.requireNonNull(action);
			if (!hasNext) {
				return;
			}
			// consume current element
			action.accept(next);
			// consume remaining elements
			iterator.forEachRemaining((int each) -> {if (filter.test(each)) action.accept(each);});
			release();
		}

		@Override
		public boolean contains(int d)
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
				next = iterator.nextInt();
				if (next == d && filter.test(next)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (!hasNext) {
				return 0;
			}
			// count current element
			int count = 1;
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
					return OptionalInt.of(next);
				}
			}
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
				result = ((FunctionalPrimitiveIterator.OfInt) iterator).reduce(result, (r, e) -> filter.test(e) ? accumulator.applyAsInt(r, e) : r);
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
		protected void seekNext()
		{
			while (iterator.hasNext()) {
				next = ((PrimitiveIterator.OfInt) iterator).nextInt();
				if (filter.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}

		@Override
		protected void release()
		{
			super.release();
			iterator = EmptyIterator.OfInt();
			filter   = x -> false;
			next     = 0;
		}
	}


	public static class OfLong extends FilteringIterator<Long, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfLong
	{
		protected LongPredicate filter;
		protected long next;

		public OfLong(PrimitiveIterator.OfLong iterator, LongPredicate predicate)
		{
			super(iterator);
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
		public void forEachRemaining(LongConsumer action)
		{
			Objects.requireNonNull(action);
			if (!hasNext) {
				return;
			}
			// consume current element
			action.accept(next);
			// consume remaining elements
			iterator.forEachRemaining((long each) -> {if (filter.test(each)) action.accept(each);});
			release();
		}

		@Override
		public boolean contains(long d)
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
				next = iterator.nextLong();
				if (next == d && filter.test(next)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (!hasNext) {
				return 0;
			}
			// count current element
			int count = 1;
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
					return OptionalLong.of(next);
				}
			}
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
		protected void seekNext()
		{
			while (iterator.hasNext()) {
				next = ((PrimitiveIterator.OfLong) iterator).nextLong();
				if (filter.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}

		@Override
		protected void release()
		{
			super.release();
			iterator = EmptyIterator.OfLong();
			filter   = x -> false;
			next     = 0;
		}
	}
}
