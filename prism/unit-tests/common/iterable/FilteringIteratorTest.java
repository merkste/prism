package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIteratorEquals;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

interface FilteringIteratorTest<E> extends FunctionalIteratorTest<E>
{
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements FilteringIteratorTest<Object>, FunctionalIteratorTest.Of<Object>
	{
		@Override
		public Stream<FilteringIterable.Of<Object>> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfObject()
					.map(IterableArray.Of::new)
					.map(iterable -> new FilteringIterable.Of<>(iterable, each -> true));
		}

		@Override
		public Stream<FilteringIterable.Of<Object>> getEmptyIterables()
		{
			return getEmptyArraysOfObject()
					.map(IterableArray.Of::new)
					.map(iterable -> new FilteringIterable.Of<>(iterable, each -> true));
		}

		@Override
		public Stream<FilteringIterable.Of<Object>> getSingletonIterables()
		{
			return getSingletonArraysOfObject()
					.map(IterableArray.Of::new)
					.map(iterable -> new FilteringIterable.Of<>(iterable, each -> true));
		}

		@Override
		public Stream<FilteringIterable.Of<Object>> getMultitonIterables()
		{
			return getMultitonArraysOfObject()
					.map(IterableArray.Of::new)
					.map(iterable -> new FilteringIterable.Of<>(iterable, each -> true));
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		void testOf(Object[] objects)
		{
			ArrayList<Object> expected = new ArrayList<>();
			int c = 0;
			for (Object each : objects) {
				if (c++ % 2 == 0) {
					expected.add(each);
				}
			}
			Iterator<Object> iterator = Arrays.asList(objects).iterator();
			FunctionalIterator<Object> actual = new FilteringIterator.Of(iterator, expected::contains);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOf_Null()
		{
			Iterator<Object> iterator = EmptyIterator.Of();
			assertThrows(NullPointerException.class, () -> new FilteringIterator.Of(null, each -> true));
			assertThrows(NullPointerException.class, () -> new FilteringIterator.Of(iterator, null));
		}
	}


	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements FilteringIteratorTest<Double>, FunctionalPrimitiveIteratorTest.OfDouble
	{
		@Override
		public Stream<FilteringIterable.OfDouble> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new FilteringIterable.OfDouble(iterable, each -> true));
		}

		@Override
		public Stream<FilteringIterable.OfDouble> getEmptyIterables()
		{
			return getEmptyArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new FilteringIterable.OfDouble(iterable, each -> true));
		}

		@Override
		public Stream<FilteringIterable.OfDouble> getSingletonIterables()
		{
			return getSingletonArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new FilteringIterable.OfDouble(iterable, each -> true));
		}

		@Override
		public Stream<FilteringIterable.OfDouble> getMultitonIterables()
		{
			return getMultitonArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new FilteringIterable.OfDouble(iterable, each -> true));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testOfDouble(double[] nums)
		{
			ArrayList<Double> expected = new ArrayList<>();
			int c = 0;
			for (double d : nums) {
				if (c++ % 2 == 0) {
					expected.add(d);
				}
			}
			ArrayIterator.OfDouble iterator = new ArrayIterator.OfDouble(nums);
			FunctionalPrimitiveIterator.OfDouble actual = new FilteringIterator.OfDouble(iterator, d -> expected.contains(d));
			assertIteratorEquals(unboxDouble(expected.iterator()), actual);
		}

		@Test
		public void testOfDouble_Null()
		{
			PrimitiveIterator.OfDouble iterator = EmptyIterator.OfDouble();
			assertThrows(NullPointerException.class, () -> new FilteringIterator.OfDouble(null, each -> true));
			assertThrows(NullPointerException.class, () -> new FilteringIterator.OfDouble(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements FilteringIteratorTest<Integer>, FunctionalPrimitiveIteratorTest.OfInt
	{
		@Override
		public Stream<FilteringIterable.OfInt> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new FilteringIterable.OfInt(iterable, each -> true));
		}

		@Override
		public Stream<FilteringIterable.OfInt> getEmptyIterables()
		{
			return getEmptyArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new FilteringIterable.OfInt(iterable, each -> true));
		}

		@Override
		public Stream<FilteringIterable.OfInt> getSingletonIterables()
		{
			return getSingletonArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new FilteringIterable.OfInt(iterable, each -> true));
		}

		@Override
		public Stream<FilteringIterable.OfInt> getMultitonIterables()
		{
			return getMultitonArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new FilteringIterable.OfInt(iterable, each -> true));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testOfInt(int[] nums)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			int c = 0;
			for (int i : nums) {
				if (c++ % 2 == 0) {
					expected.add(i);
				}
			}
			ArrayIterator.OfInt iterator = new ArrayIterator.OfInt(nums);
			FunctionalPrimitiveIterator.OfInt actual = new FilteringIterator.OfInt(iterator, i -> expected.contains(i));
			assertIteratorEquals(unboxInt(expected.iterator()), actual);
		}

		@Test
		public void testOfInt_Null()
		{
			PrimitiveIterator.OfInt iterator = EmptyIterator.OfInt();
			assertThrows(NullPointerException.class, () -> new FilteringIterator.OfInt(null, each -> true));
			assertThrows(NullPointerException.class, () -> new FilteringIterator.OfInt(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements FilteringIteratorTest<Long>, FunctionalPrimitiveIteratorTest.OfLong
	{
		@Override
		public Stream<FilteringIterable.OfLong> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new FilteringIterable.OfLong(iterable, each -> true));
		}

		@Override
		public Stream<FilteringIterable.OfLong> getEmptyIterables()
		{
			return getEmptyArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new FilteringIterable.OfLong(iterable, each -> true));
		}

		@Override
		public Stream<FilteringIterable.OfLong> getSingletonIterables()
		{
			return getSingletonArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new FilteringIterable.OfLong(iterable, each -> true));
		}

		@Override
		public Stream<FilteringIterable.OfLong> getMultitonIterables()
		{
			return getMultitonArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new FilteringIterable.OfLong(iterable, each -> true));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testOfLong(long[] nums)
		{
			ArrayList<Long> expected = new ArrayList<>();
			int c = 0;
			for (Long i : nums) {
				if (c++ % 2 == 0) {
					expected.add(i);
				}
			}
			ArrayIterator.OfLong iterator = new ArrayIterator.OfLong(nums);
			FunctionalPrimitiveIterator.OfLong actual = new FilteringIterator.OfLong(iterator, i -> expected.contains(i));
			assertIteratorEquals(unboxLong(expected.iterator()), actual);
		}

		@Test
		public void testOfLong_Null()
		{
			PrimitiveIterator.OfLong iterator = EmptyIterator.OfLong();
			assertThrows(NullPointerException.class, () -> new FilteringIterator.OfLong(null, each -> true));
			assertThrows(NullPointerException.class, () -> new FilteringIterator.OfLong(iterator, null));
		}
	}
}
