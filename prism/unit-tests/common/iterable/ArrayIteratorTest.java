package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIteratorEquals;
import static org.junit.jupiter.api.Assertions.*;

interface ArrayIteratorTest<E> extends FunctionalIteratorTest<E>
{
	@ParameterizedTest
	@MethodSource("getIterables")
	default void testNextIndex(IterableArray iterable)
	{
		ArrayIterator iter = iterable.iterator();
		for (int i=0; iter.hasNext(); i++) {
			assertEquals(i, iter.nextIndex());
			iter.next();
		}
		assertThrows(NoSuchElementException.class, iter::nextIndex);
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements ArrayIteratorTest<Object>, FunctionalIteratorTest.Of<Object>
	{
		@Override
		public Stream<IterableArray.Of<Object>> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfObject().map(IterableArray.Of::new);
		}

		@Override
		public Stream<IterableArray.Of<Object>> getEmptyIterables()
		{
			return getEmptyArraysOfObject().map(IterableArray.Of::new);
		}

		@Override
		public Stream<IterableArray.Of<Object>> getSingletonIterables()
		{
			return getSingletonArraysOfObject().map(IterableArray.Of::new);
		}

		@Override
		public Stream<IterableArray.Of<Object>> getMultitonIterables()
		{
			return getMultitonArraysOfObject().map(IterableArray.Of::new);
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		public void testOfArray(Object[] array)
		{
			ArrayIterator.Of iterator = new ArrayIterator.Of(array);
			Object[] actual = iterator.collect(new Object[array.length]);
			assertArrayEquals(array, actual);
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		public void testOfArrayIntInt_All(Object[] array)
		{
			ArrayIterator.Of expected = new ArrayIterator.Of(array);
			ArrayIterator.Of actual = new ArrayIterator.Of(array, 0, array.length);
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getMultitonArraysAsArguments"})
		public void testOfArrayIntInt_Range(Object[] array)
		{
			FunctionalIterable<Object> expected = new Range(1, array.length - 1).map((int i) -> array[i]);
			ArrayIterator.Of actual = new ArrayIterator.Of(array, 1, array.length - 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOf_Errors()
		{
			Object[] array = getMultitonArraysOfObject().findAny().get();
			int length = array.length;
			assertThrows(NullPointerException.class, () -> new ArrayIterator.Of(null));
			assertThrows(NullPointerException.class, () -> new ArrayIterator.Of(null, 0, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.Of(array, -1, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.Of(array, -1, length));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.Of(array, 1, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.Of(array, 0, length+1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.Of(array, length+1, length+1));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements ArrayIteratorTest<Double>, FunctionalPrimitiveIteratorTest.OfDouble
	{
		@Override
		public Stream<IterableArray.OfDouble> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfDouble().map(IterableArray.OfDouble::new);
		}

		@Override
		public Stream<IterableArray.OfDouble> getEmptyIterables()
		{
			return getEmptyArraysOfDouble().map(IterableArray.OfDouble::new);
		}

		@Override
		public Stream<IterableArray.OfDouble> getSingletonIterables()
		{
			return getSingletonArraysOfDouble().map(IterableArray.OfDouble::new);
		}

		@Override
		public Stream<IterableArray.OfDouble> getMultitonIterables()
		{
			return getMultitonArraysOfDouble().map(IterableArray.OfDouble::new);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfDouble", "getSingletonArraysOfDouble", "getMultitonArraysOfDouble"})
		public void testOfDouble(double[] array)
		{
			ArrayIterator.OfDouble iterator = new ArrayIterator.OfDouble(array);
			double[] actual = iterator.collect(new double[array.length]);
			assertArrayEquals(array, actual);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfDouble", "getSingletonArraysOfDouble", "getMultitonArraysOfDouble"})
		public void testOfDoubleArrayIntInt_All(double[] array)
		{
			ArrayIterator.OfDouble expected = new ArrayIterator.OfDouble(array);
			ArrayIterator.OfDouble actual = new ArrayIterator.OfDouble(array, 0, array.length);
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getMultitonArraysOfDouble"})
		public void testOfDoubleArrayIntInt_Range(double[] array)
		{
			FunctionalPrimitiveIterable.OfDouble expected = new Range(1, array.length - 1).mapToDouble((int i) -> array[i]);
			ArrayIterator.OfDouble actual = new ArrayIterator.OfDouble(array, 1, array.length - 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOfDouble_Errors()
		{
			double[] array = getMultitonArraysOfDouble().findAny().get();
			int length = array.length;
			assertThrows(NullPointerException.class, () -> new ArrayIterator.OfDouble(null));
			assertThrows(NullPointerException.class, () -> new ArrayIterator.OfDouble(null, 0, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfDouble(array, -1, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfDouble(array, -1, length));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfDouble(array, 1, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfDouble(array, 0, length+1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfDouble(array, length+1, length+1));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements ArrayIteratorTest<Integer>, FunctionalPrimitiveIteratorTest.OfInt
	{
		@Override
		public Stream<IterableArray.OfInt> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfInt().map(IterableArray.OfInt::new);
		}

		@Override
		public Stream<IterableArray.OfInt> getEmptyIterables()
		{
			return getEmptyArraysOfInt().map(IterableArray.OfInt::new);
		}

		@Override
		public Stream<IterableArray.OfInt> getSingletonIterables()
		{
			return getSingletonArraysOfInt().map(IterableArray.OfInt::new);
		}

		@Override
		public Stream<IterableArray.OfInt> getMultitonIterables()
		{
			return getMultitonArraysOfInt().map(IterableArray.OfInt::new);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfInt", "getSingletonArraysOfInt", "getMultitonArraysOfInt"})
		public void testOfInt(int[] array)
		{
			ArrayIterator.OfInt iterator = new ArrayIterator.OfInt(array);
			int[] actual = iterator.collect(new int[array.length]);
			assertArrayEquals(array, actual);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfInt", "getSingletonArraysOfInt", "getMultitonArraysOfInt"})
		public void testOfIntArrayIntInt_All(int[] array)
		{
			ArrayIterator.OfInt expected = new ArrayIterator.OfInt(array);
			ArrayIterator.OfInt actual = new ArrayIterator.OfInt(array, 0, array.length);
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getMultitonArraysOfInt"})
		public void testOfIntArrayIntInt_Range(int[] array)
		{
			FunctionalPrimitiveIterable.OfInt expected = new Range(1, array.length - 1).mapToInt((int i) -> array[i]);
			ArrayIterator.OfInt actual = new ArrayIterator.OfInt(array, 1, array.length - 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOfInt_Errors()
		{
			int[] array = getMultitonArraysOfInt().findAny().get();
			int length = array.length;
			assertThrows(NullPointerException.class, () -> new ArrayIterator.OfInt(null));
			assertThrows(NullPointerException.class, () -> new ArrayIterator.OfInt(null, 0, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfInt(array, -1, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfInt(array, -1, length));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfInt(array, 1, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfInt(array, 0, length+1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfInt(array, length+1, length+1));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements ArrayIteratorTest<Long>, FunctionalPrimitiveIteratorTest.OfLong
	{
		@Override
		public Stream<IterableArray.OfLong> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfLong().map(IterableArray.OfLong::new);
		}

		@Override
		public Stream<IterableArray.OfLong> getEmptyIterables()
		{
			return getEmptyArraysOfLong().map(IterableArray.OfLong::new);
		}

		@Override
		public Stream<IterableArray.OfLong> getSingletonIterables()
		{
			return getSingletonArraysOfLong().map(IterableArray.OfLong::new);
		}

		@Override
		public Stream<IterableArray.OfLong> getMultitonIterables()
		{
			return getMultitonArraysOfLong().map(IterableArray.OfLong::new);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfLong", "getSingletonArraysOfLong", "getMultitonArraysOfLong"})
		public void testOfLong(long[] array)
		{
			ArrayIterator.OfLong iterator = new ArrayIterator.OfLong(array);
			long[] actual = iterator.collect(new long[array.length]);
			assertArrayEquals(array, actual);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfLong", "getSingletonArraysOfLong", "getMultitonArraysOfLong"})
		public void testOfLongArrayIntInt_All(long[] array)
		{
			ArrayIterator.OfLong expected = new ArrayIterator.OfLong(array);
			ArrayIterator.OfLong actual = new ArrayIterator.OfLong(array, 0, array.length);
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getMultitonArraysOfLong"})
		public void testOfLongArrayIntInt_Range(long[] array)
		{
			FunctionalPrimitiveIterable.OfLong expected = new Range(1, array.length - 1).mapToLong((int i) -> array[i]);
			ArrayIterator.OfLong actual = new ArrayIterator.OfLong(array, 1, array.length - 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOfLong_Errors()
		{
			long[] array = getMultitonArraysOfLong().findAny().get();
			int length = array.length;
			assertThrows(NullPointerException.class, () -> new ArrayIterator.OfLong(null));
			assertThrows(NullPointerException.class, () -> new ArrayIterator.OfLong(null, 0, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfLong(array, -1, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfLong(array, -1, length));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfLong(array, 1, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfLong(array, 0, length+1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfLong(array, length+1, length+1));
		}
	}
}