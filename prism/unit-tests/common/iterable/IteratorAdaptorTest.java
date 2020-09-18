package common.iterable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIteratorEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IteratorAdaptorTest
{
	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class Of implements FunctionalIteratorTest.Of<Object>
	{
		@Override
		public Stream<IterableAdaptor.Of<Object>> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfObject()
					.map(Arrays::asList)
					.map(IterableAdaptor.Of::new);
		}

		@Override
		public Stream<IterableAdaptor.Of<Object>> getEmptyIterables()
		{
			return getEmptyArraysOfObject()
					.map(Arrays::asList)
					.map(IterableAdaptor.Of::new);
		}

		@Override
		public Stream<IterableAdaptor.Of<Object>> getSingletonIterables()
		{
			return getSingletonArraysOfObject()
					.map(Arrays::asList)
					.map(IterableAdaptor.Of::new);
		}

		@Override
		public Stream<IterableAdaptor.Of<Object>> getMultitonIterables()
		{
			return getMultitonArraysOfObject()
					.map(Arrays::asList)
					.map(IterableAdaptor.Of::new);
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		@DisplayName("Adaptor yields same sequence as the underlying iterator.")
		public void testOf(Object[] objects)
		{
			Iterable<Object> iterable = asNonFunctionalIterable(objects);
			IteratorAdaptor.Of actual = new IteratorAdaptor.Of(iterable.iterator());
			assertIteratorEquals(iterable.iterator(), actual);
		}

		@Test
		@DisplayName("Adapter on null throws NullPointerException.")
		public void testOf_Null()
		{
			assertThrows(NullPointerException.class, () -> new IteratorAdaptor.Of<>(null));
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		@DisplayName("unwrap() answers the underlying iterator.")
		public void testUnwrap(Object[] objects)
		{
			Iterator<Object> expected = asNonFunctionalIterable(objects).iterator();
			Iterator actual = new IteratorAdaptor.Of(expected).unwrap();
			assertSame(expected, actual);
		}
	}



	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class OfDouble implements FunctionalPrimitiveIteratorTest.OfDouble
	{
		@Override
		public Stream<IterableAdaptor.OfDouble> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfDouble()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(PrimitiveIterable::unboxDouble)
					.map(IterableAdaptor.OfDouble::new);
		}

		@Override
		public Stream<IterableAdaptor.OfDouble> getEmptyIterables()
		{
			return getEmptyArraysOfDouble()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(PrimitiveIterable::unboxDouble)
					.map(IterableAdaptor.OfDouble::new);
		}

		@Override
		public Stream<IterableAdaptor.OfDouble> getSingletonIterables()
		{
			return getSingletonArraysOfDouble()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(PrimitiveIterable::unboxDouble)
					.map(IterableAdaptor.OfDouble::new);
		}

		@Override
		public Stream<IterableAdaptor.OfDouble> getMultitonIterables()
		{
			return getMultitonArraysOfDouble()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(PrimitiveIterable::unboxDouble)
					.map(IterableAdaptor.OfDouble::new);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		@DisplayName("Adaptor yields same sequence as the underlying iterator.")
		public void testOfDouble(double[] nums)
		{
			PrimitiveIterable.OfDouble iterable = asNonFunctionalIterable(nums);
			IteratorAdaptor.OfDouble actual = new IteratorAdaptor.OfDouble(iterable.iterator());
			assertIteratorEquals(iterable.iterator(), actual);
		}

		@Test
		@DisplayName("Adapter on null throws NullPointerException.")
		public void testOfDouble_Null()
		{
			assertThrows(NullPointerException.class, () -> new IteratorAdaptor.OfDouble(null));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		@DisplayName("unwrap() answers the underlying iterator.")
		public void testUnwrap(double[] nums)
		{
			PrimitiveIterator.OfDouble expected = asNonFunctionalIterable(nums).iterator();
			PrimitiveIterator.OfDouble actual = new IteratorAdaptor.OfDouble(expected).unwrap();
			assertSame(expected, actual);
		}
	}

	private PrimitiveIterable.OfDouble asNonFunctionalIterable(double[] nums)
	{
		return PrimitiveIterable.unboxDouble(Arrays.stream(nums).boxed().collect(Collectors.toList()));
	}

	private PrimitiveIterable.OfInt asNonFunctionalIterable(int[] nums)
	{
		return PrimitiveIterable.unboxInt(Arrays.stream(nums).boxed().collect(Collectors.toList()));
	}

	private PrimitiveIterable.OfLong asNonFunctionalIterable(long[] nums)
	{
		return PrimitiveIterable.unboxLong(Arrays.stream(nums).boxed().collect(Collectors.toList()));
	}

	private <T> Iterable<T> asNonFunctionalIterable(T[] objects)
	{
		return Arrays.asList(objects);
	}

	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class OfInt implements FunctionalPrimitiveIteratorTest.OfInt
	{
		@Override
		public Stream<IterableAdaptor.OfInt> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfInt()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(PrimitiveIterable::unboxInt)
					.map(IterableAdaptor.OfInt::new);
		}

		@Override
		public Stream<IterableAdaptor.OfInt> getEmptyIterables()
		{
			return getEmptyArraysOfInt()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(PrimitiveIterable::unboxInt)
					.map(IterableAdaptor.OfInt::new);
		}

		@Override
		public Stream<IterableAdaptor.OfInt> getSingletonIterables()
		{
			return getSingletonArraysOfInt()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(PrimitiveIterable::unboxInt)
					.map(IterableAdaptor.OfInt::new);
		}

		@Override
		public Stream<IterableAdaptor.OfInt> getMultitonIterables()
		{
			return getMultitonArraysOfInt()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(PrimitiveIterable::unboxInt)
					.map(IterableAdaptor.OfInt::new);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		@DisplayName("Adaptor yields same sequence as the underlying iterator.")
		public void testOfInt(int[] nums)
		{
			PrimitiveIterable.OfInt iterable = asNonFunctionalIterable(nums);
			IteratorAdaptor.OfInt actual = new IteratorAdaptor.OfInt(iterable.iterator());
			assertIteratorEquals(iterable.iterator(), actual);
		}

		@Test
		@DisplayName("Adapter on null throws NullPointerException.")
		public void testOfInt_Null()
		{
			assertThrows(NullPointerException.class, () -> new IteratorAdaptor.OfInt(null));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		@DisplayName("unwrap() answers the underlying iterator.")
		public void testUnwrap(int[] nums)
		{
			PrimitiveIterator.OfInt expected = asNonFunctionalIterable(nums).iterator();
			PrimitiveIterator.OfInt actual = new IteratorAdaptor.OfInt(expected).unwrap();
			assertSame(expected, actual);
		}
	}



	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class OfLong implements FunctionalPrimitiveIteratorTest.OfLong
	{
		@Override
		public Stream<IterableAdaptor.OfLong> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfLong()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(PrimitiveIterable::unboxLong)
					.map(IterableAdaptor.OfLong::new);
		}

		@Override
		public Stream<IterableAdaptor.OfLong> getEmptyIterables()
		{
			return getEmptyArraysOfLong()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(PrimitiveIterable::unboxLong)
					.map(IterableAdaptor.OfLong::new);
		}

		@Override
		public Stream<IterableAdaptor.OfLong> getSingletonIterables()
		{
			return getSingletonArraysOfLong()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(PrimitiveIterable::unboxLong)
					.map(IterableAdaptor.OfLong::new);

		}

		@Override
		public Stream<IterableAdaptor.OfLong> getMultitonIterables()
		{
			return getMultitonArraysOfLong()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(PrimitiveIterable::unboxLong)
					.map(IterableAdaptor.OfLong::new);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		@DisplayName("Adaptor yields same sequence as the underlying iterator.")
		public void testOfLong(long[] nums)
		{
			PrimitiveIterable.OfLong iterable = asNonFunctionalIterable(nums);
			IteratorAdaptor.OfLong actual = new IteratorAdaptor.OfLong(iterable.iterator());
			assertIteratorEquals(iterable.iterator(), actual);
		}

		@Test
		@DisplayName("Adapter on null throws NullPointerException.")
		public void testOfLong_Null()
		{
			assertThrows(NullPointerException.class, () -> new IteratorAdaptor.OfLong(null));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		@DisplayName("unwrap() answers the underlying iterator.")
		public void testUnwrap(long[] nums)
		{
			PrimitiveIterator.OfLong expected = asNonFunctionalIterable(nums).iterator();
			PrimitiveIterator.OfLong actual = new IteratorAdaptor.OfLong(expected).unwrap();
			assertSame(expected, actual);
		}
	}
}
