package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIteratorEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

interface ChainedIteratorTest<E> extends FunctionalIteratorTest<E>
{
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements ChainedIteratorTest<Object>, FunctionalIteratorTest.Of<Object>
	{
		Stream<? extends Iterable<? extends Iterable<Object>>> split(Object[] nums)
		{
			// 1. whole sequence
			List<Iterable<Object>> complete = List.of(new IterableArray.Of(nums));
			// 2. split & pad with empty
			int l1 = nums.length / 2;
			int l2 = nums.length - l1;
			Object[] first = new Object[l1];
			System.arraycopy(nums, 0, first, 0, l1);
			Object[] second = new Object[l2];
			System.arraycopy(nums, l1, second, 0, l2);
			List<Iterable<Object>> splitted = new ArrayList<>();
			splitted.add(EmptyIterable.Of());
			splitted.add(new IterableArray.Of(first));
			splitted.add(EmptyIterable.Of());
			splitted.add(new IterableArray.Of(second));
			splitted.add(EmptyIterable.Of());
			return Stream.of(complete, splitted);
		}

		public Stream<? extends Iterable<? extends Iterable<Object>>> getChains()
		{
			return getArraysOfObject().flatMap(this::split);
		}

		@Override
		public Stream<ChainedIterable.Of<Object>> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfObject().flatMap(objects -> split(objects)).map(ChainedIterable.Of::new);
		}

		@Override
		public Stream<ChainedIterable.Of<Object>> getEmptyIterables()
		{
			return getEmptyArraysOfObject().flatMap(objects -> split(objects)).map(ChainedIterable.Of::new);
		}

		@Override
		public Stream<ChainedIterable.Of<Object>> getSingletonIterables()
		{
			return getSingletonArraysOfObject().flatMap(objects -> split(objects)).map(ChainedIterable.Of::new);
		}

		@Override
		public Stream<ChainedIterable.Of<Object>> getMultitonIterables()
		{
			return getMultitonArraysOfObject().flatMap(objects -> split(objects)).map(ChainedIterable.Of::new);
		}

		@ParameterizedTest
		@MethodSource("getChains")
		public void testOf(Iterable<? extends Iterable<Object>> chain)
		{
			List<Object> expected = new ArrayList<>();
			for (Iterable<? extends Object> iterable : chain) {
				for (Object each : iterable) {
					expected.add(each);
				}
			}
			Iterator<Iterator<Object>> iterators = FunctionalIterator.extend(chain).map(Iterable::iterator);
			ChainedIterator.Of<Object> actual = new ChainedIterator.Of<>(iterators);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOf_Null()
		{
			assertThrows(NullPointerException.class, () -> new ChainedIterator.Of(null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements ChainedIteratorTest<Double>, FunctionalPrimitiveIteratorTest.OfDouble
	{
		Stream<? extends Iterable<? extends PrimitiveIterable.OfDouble>> split(double[] nums)
		{
			// 1. whole sequence
			List<PrimitiveIterable.OfDouble> complete = List.of(new IterableArray.OfDouble(nums));
			// 2. split & pad with empty
			int l1 = nums.length / 2;
			int l2 = nums.length - l1;
			double[] first = new double[l1];
			System.arraycopy(nums, 0, first, 0, l1);
			double[] second = new double[l2];
			System.arraycopy(nums, l1, second, 0, l2);
			List<PrimitiveIterable.OfDouble> splitted = new ArrayList<>();
			splitted.add(EmptyIterable.OfDouble());
			splitted.add(new IterableArray.OfDouble(first));
			splitted.add(EmptyIterable.OfDouble());
			splitted.add(new IterableArray.OfDouble(second));
			splitted.add(EmptyIterable.OfDouble());
			return Stream.of(complete, splitted);
		}

		public Stream<? extends Iterable<? extends PrimitiveIterable.OfDouble>> getChains()
		{
			return getArraysOfDouble().flatMap(this::split);
		}

		@Override
		public Stream<ChainedIterable.OfDouble> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfDouble().flatMap(nums -> split(nums)).map(ChainedIterable.OfDouble::new);
		}

		@Override
		public Stream<ChainedIterable.OfDouble> getEmptyIterables()
		{
			return getEmptyArraysOfDouble().flatMap(nums -> split(nums)).map(ChainedIterable.OfDouble::new);
		}

		@Override
		public Stream<ChainedIterable.OfDouble> getSingletonIterables()
		{
			return getSingletonArraysOfDouble().flatMap(nums -> split(nums)).map(ChainedIterable.OfDouble::new);
		}

		@Override
		public Stream<ChainedIterable.OfDouble> getMultitonIterables()
		{
			return getMultitonArraysOfDouble().flatMap(nums -> split(nums)).map(ChainedIterable.OfDouble::new);
		}

		@ParameterizedTest
		@MethodSource("getChains")
		public void testOf(Iterable<? extends PrimitiveIterable.OfDouble> chain)
		{
			List<Double> expected = new ArrayList<>();
			for (Iterable<Double> iterable : chain) {
				for (Double each : iterable) {
					expected.add(each);
				}
			}
			Iterator<PrimitiveIterator.OfDouble> iterators = FunctionalIterator.extend(chain).map(PrimitiveIterable.OfDouble::iterator);
			ChainedIterator.OfDouble actual = new ChainedIterator.OfDouble(iterators);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOfDouble_Null()
		{
			assertThrows(NullPointerException.class, () -> new ChainedIterator.OfDouble(null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements ChainedIteratorTest<Integer>, FunctionalPrimitiveIteratorTest.OfInt
	{
		Stream<? extends Iterable<? extends PrimitiveIterable.OfInt>> split(int[] nums)
		{
			// 1. whole sequence
			List<PrimitiveIterable.OfInt> complete = List.of(new IterableArray.OfInt(nums));
			// 2. split & pad with empty
			int l1 = nums.length / 2;
			int l2 = nums.length - l1;
			int[] first = new int[l1];
			System.arraycopy(nums, 0, first, 0, l1);
			int[] second = new int[l2];
			System.arraycopy(nums, l1, second, 0, l2);
			List<PrimitiveIterable.OfInt> splitted = new ArrayList<>();
			splitted.add(EmptyIterable.OfInt());
			splitted.add(new IterableArray.OfInt(first));
			splitted.add(EmptyIterable.OfInt());
			splitted.add(new IterableArray.OfInt(second));
			splitted.add(EmptyIterable.OfInt());
			return Stream.of(complete, splitted);
		}

		public Stream<? extends Iterable<? extends PrimitiveIterable.OfInt>> getChains()
		{
			return getArraysOfInt().flatMap(this::split);
		}

		@Override
		public Stream<ChainedIterable.OfInt> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfInt().flatMap(nums -> split(nums)).map(ChainedIterable.OfInt::new);
		}

		@Override
		public Stream<ChainedIterable.OfInt> getEmptyIterables()
		{
			return getEmptyArraysOfInt().flatMap(nums -> split(nums)).map(ChainedIterable.OfInt::new);
		}

		@Override
		public Stream<ChainedIterable.OfInt> getSingletonIterables()
		{
			return getSingletonArraysOfInt().flatMap(nums -> split(nums)).map(ChainedIterable.OfInt::new);
		}

		@Override
		public Stream<ChainedIterable.OfInt> getMultitonIterables()
		{
			return getMultitonArraysOfInt().flatMap(nums -> split(nums)).map(ChainedIterable.OfInt::new);
		}

		@ParameterizedTest
		@MethodSource("getChains")
		public void testOf(Iterable<? extends PrimitiveIterable.OfInt> chain)
		{
			List<Integer> expected = new ArrayList<>();
			for (Iterable<Integer> iterable : chain) {
				for (Integer each : iterable) {
					expected.add(each);
				}
			}
			Iterator<PrimitiveIterator.OfInt> iterators = FunctionalIterator.extend(chain).map(iterable -> iterable.iterator());
			ChainedIterator.OfInt actual = new ChainedIterator.OfInt(iterators);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOfDouble_Null()
		{
			assertThrows(NullPointerException.class, () -> new ChainedIterator.OfInt(null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements ChainedIteratorTest<Long>, FunctionalPrimitiveIteratorTest.OfLong
	{
		Stream<? extends Iterable<? extends PrimitiveIterable.OfLong>> split(long[] nums)
		{
			// 1. whole sequence
			List<PrimitiveIterable.OfLong> complete = List.of(new IterableArray.OfLong(nums));
			// 2. split & pad with empty
			int l1 = nums.length / 2;
			int l2 = nums.length - l1;
			long[] first = new long[l1];
			System.arraycopy(nums, 0, first, 0, l1);
			long[] second = new long[l2];
			System.arraycopy(nums, l1, second, 0, l2);
			List<PrimitiveIterable.OfLong> splitted = new ArrayList<>();
			splitted.add(EmptyIterable.OfLong());
			splitted.add(new IterableArray.OfLong(first));
			splitted.add(EmptyIterable.OfLong());
			splitted.add(new IterableArray.OfLong(second));
			splitted.add(EmptyIterable.OfLong());
			return Stream.of(complete, splitted);
		}

		public Stream<? extends Iterable<? extends PrimitiveIterable.OfLong>> getChains()
		{
			return getArraysOfLong().flatMap(this::split);
		}

		@Override
		public Stream<ChainedIterable.OfLong> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfLong().flatMap(nums -> split(nums)).map(ChainedIterable.OfLong::new);
		}

		@Override
		public Stream<ChainedIterable.OfLong> getEmptyIterables()
		{
			return getEmptyArraysOfLong().flatMap(nums -> split(nums)).map(ChainedIterable.OfLong::new);
		}

		@Override
		public Stream<ChainedIterable.OfLong> getSingletonIterables()
		{
			return getSingletonArraysOfLong().flatMap(nums -> split(nums)).map(ChainedIterable.OfLong::new);
		}

		@Override
		public Stream<ChainedIterable.OfLong> getMultitonIterables()
		{
			return getMultitonArraysOfLong().flatMap(nums -> split(nums)).map(ChainedIterable.OfLong::new);
		}

		@ParameterizedTest
		@MethodSource("getChains")
		public void testOf(Iterable<? extends PrimitiveIterable.OfLong> chain)
		{
			List<Long> expected = new ArrayList<>();
			for (Iterable<Long> iterable : chain) {
				for (Long each : iterable) {
					expected.add(each);
				}
			}
			Iterator<PrimitiveIterator.OfLong> iterators = FunctionalIterator.extend(chain).map(iterable -> iterable.iterator());
			ChainedIterator.OfLong actual = new ChainedIterator.OfLong(iterators);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOfDouble_Null()
		{
			assertThrows(NullPointerException.class, () -> new ChainedIterator.OfLong(null));
		}
	}
}
