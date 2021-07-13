package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIteratorEquals;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

interface MappingIteratorTest<E> extends FunctionalIteratorTest<E>
{
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ObjToObj implements MappingIteratorTest<Object>, FunctionalIteratorTest.Of<Object>
	{
		@Override
		public Stream<MappingIterable.ObjToObj<Object,Object>> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfObject()
					.map(IterableArray.Of::new)
					.map(iterable -> new MappingIterable.ObjToObj<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.ObjToObj<Object,Object>> getEmptyIterables()
		{
			return Stream.of(Collections.emptyList())
			             .map(iterable -> new MappingIterable.ObjToObj<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.ObjToObj<Object,Object>> getSingletonIterables()
		{
			return Stream.of(Collections.<Object>singleton("first"),
			                 Collections.<Object>singleton(null))
			             .map(iterable -> new MappingIterable.ObjToObj<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.ObjToObj<Object,Object>> getMultitonIterables()
		{
			return Stream.of(Arrays.<Object>asList("first", "second", "third"),
			                 Arrays.<Object>asList(null, "first", null, "second", null, "third", null))
					     .map(iterable -> new MappingIterable.ObjToObj<>(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		void testOf(Object[] objects)
		{
			ArrayList<String> expected = new ArrayList<>();
			for (Object each : objects) {
				expected.add(Objects.toString(each));
			}
			Iterator<Object> iterator = Arrays.asList(objects).iterator();
			Iterator<String> actual = new MappingIterator.ObjToObj<>(iterator, Objects::toString);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOf_Null()
		{
			Iterator<Object> iterator = EmptyIterator.Of();
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToObj<>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToObj<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ObjToDouble implements MappingIteratorTest<Double>, FunctionalPrimitiveIteratorTest.OfDouble
	{
		@Override
		public Stream<MappingIterable.ObjToDouble<Double>> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfDouble()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(iterable -> new MappingIterable.ObjToDouble<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.ObjToDouble<Double>> getEmptyIterables()
		{
			return getEmptyArraysOfDouble()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(iterable -> new MappingIterable.ObjToDouble<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.ObjToDouble<Double>> getSingletonIterables()
		{
			return getSingletonArraysOfDouble()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(iterable -> new MappingIterable.ObjToDouble<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.ObjToDouble<Double>> getMultitonIterables()
		{
			return getMultitonArraysOfDouble()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(iterable -> new MappingIterable.ObjToDouble<>(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testObjToDouble(double[] nums)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (double d : nums) {
				expected.add(d);
			}
			Iterator<Double> iterator = expected.iterator();
			FunctionalPrimitiveIterator.OfDouble actual = new MappingIterator.ObjToDouble<>(iterator, each -> each);
			assertIteratorEquals(unboxDouble(expected.iterator()), actual);
		}

		@Test
		public void testObjToDouble_Null()
		{
			Iterator<Double> iterator = EmptyIterator.Of();
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToDouble<Double>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToDouble<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ObjToInt implements MappingIteratorTest<Integer>, FunctionalPrimitiveIteratorTest.OfInt
	{
		@Override
		public Stream<MappingIterable.ObjToInt<Integer>> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfInt()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(iterable -> new MappingIterable.ObjToInt<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.ObjToInt<Integer>> getEmptyIterables()
		{
			return getEmptyArraysOfInt()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(iterable -> new MappingIterable.ObjToInt<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.ObjToInt<Integer>> getSingletonIterables()
		{
			return getSingletonArraysOfInt()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(iterable -> new MappingIterable.ObjToInt<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.ObjToInt<Integer>> getMultitonIterables()
		{
			return getMultitonArraysOfInt()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(iterable -> new MappingIterable.ObjToInt<>(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testObjToInt(int[] nums)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (int i : nums) {
				expected.add(i);
			}
			Iterator<Integer> iterator = expected.iterator();
			FunctionalPrimitiveIterator.OfInt actual = new MappingIterator.ObjToInt<>(iterator, each -> each);
			assertIteratorEquals(unboxInt(expected.iterator()), actual);
		}

		@Test
		public void testObjToInt_Null()
		{
			Iterator<Integer> iterator = EmptyIterator.Of();
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToInt<Integer>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToInt<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ObjToLong implements MappingIteratorTest<Long>, FunctionalPrimitiveIteratorTest.OfLong
	{
		@Override
		public Stream<MappingIterable.ObjToLong<Long>> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfLong()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(iterable -> new MappingIterable.ObjToLong<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.ObjToLong<Long>> getEmptyIterables()
		{
			return getEmptyArraysOfLong()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(iterable -> new MappingIterable.ObjToLong<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.ObjToLong<Long>> getSingletonIterables()
		{
			return getSingletonArraysOfLong()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(iterable -> new MappingIterable.ObjToLong<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.ObjToLong<Long>> getMultitonIterables()
		{
			return getMultitonArraysOfLong()
					.map(nums -> Arrays.stream(nums).boxed().collect(Collectors.toList()))
					.map(iterable -> new MappingIterable.ObjToLong<>(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testObjToLong(long[] nums)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (long l : nums) {
				expected.add(l);
			}
			Iterator<Long> iterator = expected.iterator();
			FunctionalPrimitiveIterator.OfLong actual = new MappingIterator.ObjToLong<>(iterator, each -> each);
			assertIteratorEquals(unboxLong(expected.iterator()), actual);
		}

		@Test
		public void testObjToLong_Null()
		{
			Iterator<Long> iterator = EmptyIterator.Of();
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToLong<Long>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToLong<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DoubleToObj implements MappingIteratorTest<Object>, FunctionalIteratorTest.Of<Object>
	{
		@Override
		public Stream<MappingIterable.DoubleToObj<Object>> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToObj<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.DoubleToObj<Object>> getEmptyIterables()
		{
			return getEmptyArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToObj<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.DoubleToObj<Object>> getSingletonIterables()
		{
			return getSingletonArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToObj<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.DoubleToObj<Object>> getMultitonIterables()
		{
			return getMultitonArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToObj<>(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testDoubleToObj(double[] nums)
		{
			ArrayList<String> expected = new ArrayList<>();
			for (double d : nums) {
				expected.add(Objects.toString(d));
			}
			ArrayIterator.OfDouble iterator = new ArrayIterator.OfDouble(nums);
			Iterator<String> actual = new MappingIterator.DoubleToObj(iterator, d -> Objects.toString(d));
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testDoubleToObj_Null()
		{
			PrimitiveIterator.OfDouble iterator = EmptyIterator.OfDouble();
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToObj<Double>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToObj<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DoubleToDouble implements MappingIteratorTest<Double>, FunctionalPrimitiveIteratorTest.OfDouble
	{
		@Override
		public Stream<MappingIterable.DoubleToDouble> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToDouble(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.DoubleToDouble> getEmptyIterables()
		{
			return getEmptyArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToDouble(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.DoubleToDouble> getSingletonIterables()
		{
			return getSingletonArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToDouble(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.DoubleToDouble> getMultitonIterables()
		{
			return getMultitonArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToDouble(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testDoubleToDouble(double[] nums)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (double d : nums) {
				expected.add(d + 1.0);
			}
			ArrayIterator.OfDouble iterator = new ArrayIterator.OfDouble(nums);
			PrimitiveIterator.OfDouble actual = new MappingIterator.DoubleToDouble(iterator, d -> d + 1.0);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testDoubleToDouble_Null()
		{
			PrimitiveIterator.OfDouble iterator = EmptyIterator.OfDouble();
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToDouble(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToDouble(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DoubleToInt implements MappingIteratorTest<Integer>, FunctionalPrimitiveIteratorTest.OfInt
	{
		@Override
		public Stream<MappingIterable.DoubleToInt> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToInt(iterable, each -> (int) each));
		}

		@Override
		public Stream<MappingIterable.DoubleToInt> getEmptyIterables()
		{
			return getEmptyArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToInt(iterable, each -> (int) each));
		}

		@Override
		public Stream<MappingIterable.DoubleToInt> getSingletonIterables()
		{
			return getSingletonArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToInt(iterable, each -> (int) each));
		}

		@Override
		public Stream<MappingIterable.DoubleToInt> getMultitonIterables()
		{
			return getMultitonArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToInt(iterable, each -> (int) each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testDoubleToInt(double[] nums)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (double d : nums) {
				expected.add((int) d + 1);
			}
			ArrayIterator.OfDouble iterator = new ArrayIterator.OfDouble(nums);
			PrimitiveIterator.OfInt actual = new MappingIterator.DoubleToInt(iterator, d -> (int) d + 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testDoubleToInt_Null()
		{
			PrimitiveIterator.OfDouble iterator = EmptyIterator.OfDouble();
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToInt(null, each -> (int) each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToInt(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DoubleToLong implements MappingIteratorTest<Long>, FunctionalPrimitiveIteratorTest.OfLong
	{
		@Override
		public Stream<MappingIterable.DoubleToLong> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToLong(iterable, each -> (long) each));
		}

		@Override
		public Stream<MappingIterable.DoubleToLong> getEmptyIterables()
		{
			return getEmptyArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToLong(iterable, each -> (long) each));
		}

		@Override
		public Stream<MappingIterable.DoubleToLong> getSingletonIterables()
		{
			return getSingletonArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToLong(iterable, each -> (long) each));
		}

		@Override
		public Stream<MappingIterable.DoubleToLong> getMultitonIterables()
		{
			return getMultitonArraysOfDouble()
					.map(IterableArray.OfDouble::new)
					.map(iterable -> new MappingIterable.DoubleToLong(iterable, each -> (long) each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testDoubleToLong(double[] nums)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (double d : nums) {
				expected.add((long) d + 1);
			}
			ArrayIterator.OfDouble iterator = new ArrayIterator.OfDouble(nums);
			PrimitiveIterator.OfLong actual = new MappingIterator.DoubleToLong(iterator, d -> (long) d + 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testDoubleToLong_Null()
		{
			PrimitiveIterator.OfDouble iterator = EmptyIterator.OfDouble();
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToLong(null, each -> (long) each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToLong(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class IntToObj implements MappingIteratorTest<Object>, FunctionalIteratorTest.Of<Object>
	{
		@Override
		public Stream<MappingIterable.IntToObj<Object>> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToObj<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.IntToObj<Object>> getEmptyIterables()
		{
			return getEmptyArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToObj<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.IntToObj<Object>> getSingletonIterables()
		{
			return getSingletonArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToObj<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.IntToObj<Object>> getMultitonIterables()
		{
			return getMultitonArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToObj<>(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testIntToObj(int[] nums)
		{
			ArrayList<String> expected = new ArrayList<>();
			for (int i : nums) {
				expected.add(Objects.toString(i));
			}
			ArrayIterator.OfInt iterator = new ArrayIterator.OfInt(nums);
			Iterator<String> actual = new MappingIterator.IntToObj<>(iterator, i -> Objects.toString(i));
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testIntToObj_Null()
		{
			PrimitiveIterator.OfInt iterator = EmptyIterator.OfInt();
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToObj<Integer>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToObj<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class IntToDouble implements MappingIteratorTest<Double>, FunctionalPrimitiveIteratorTest.OfDouble
	{
		@Override
		public Stream<MappingIterable.IntToDouble> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToDouble(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.IntToDouble> getEmptyIterables()
		{
			return getEmptyArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToDouble(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.IntToDouble> getSingletonIterables()
		{
			return getSingletonArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToDouble(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.IntToDouble> getMultitonIterables()
		{
			return getMultitonArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToDouble(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testIntToDouble(int[] nums)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (int i : nums) {
				expected.add(i + 1.0);
			}
			ArrayIterator.OfInt iterator = new ArrayIterator.OfInt(nums);
			PrimitiveIterator.OfDouble actual = new MappingIterator.IntToDouble(iterator, i -> i + 1.0);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testIntToDouble_Null()
		{
			PrimitiveIterator.OfInt iterator = EmptyIterator.OfInt();
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToDouble(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToDouble(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class IntToInt implements MappingIteratorTest<Integer>, FunctionalPrimitiveIteratorTest.OfInt
	{
		@Override
		public Stream<MappingIterable.IntToInt> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToInt(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.IntToInt> getEmptyIterables()
		{
			return getEmptyArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToInt(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.IntToInt> getSingletonIterables()
		{
			return getSingletonArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToInt(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.IntToInt> getMultitonIterables()
		{
			return getMultitonArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToInt(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testIntToInt(int[] nums)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (int i : nums) {
				expected.add((int) i + 1);
			}
			ArrayIterator.OfInt iterator = new ArrayIterator.OfInt(nums);
			PrimitiveIterator.OfInt actual = new MappingIterator.IntToInt(iterator, i -> i + 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testIntToInt_Null()
		{
			PrimitiveIterator.OfInt iterator = EmptyIterator.OfInt();
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToInt(null, each -> (int) each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToInt(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class IntToLong implements MappingIteratorTest<Long>, FunctionalPrimitiveIteratorTest.OfLong
	{
		@Override
		public Stream<MappingIterable.IntToLong> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToLong(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.IntToLong> getEmptyIterables()
		{
			return getEmptyArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToLong(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.IntToLong> getSingletonIterables()
		{
			return getSingletonArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToLong(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.IntToLong> getMultitonIterables()
		{
			return getMultitonArraysOfInt()
					.map(IterableArray.OfInt::new)
					.map(iterable -> new MappingIterable.IntToLong(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testIntToLong(int[] nums)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (int i : nums) {
				expected.add((long) i + 1);
			}
			ArrayIterator.OfInt iterator = new ArrayIterator.OfInt(nums);
			PrimitiveIterator.OfLong actual = new MappingIterator.IntToLong(iterator, i -> (long) i + 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testIntToLong_Null()
		{
			PrimitiveIterator.OfInt iterator = EmptyIterator.OfInt();
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToLong(null, each -> (long) each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToLong(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LongToObj implements MappingIteratorTest<Object>, FunctionalIteratorTest.Of<Object>
	{
		@Override
		public Stream<MappingIterable.LongToObj<Object>> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToObj<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.LongToObj<Object>> getEmptyIterables()
		{
			return getEmptyArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToObj<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.LongToObj<Object>> getSingletonIterables()
		{
			return getSingletonArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToObj<>(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.LongToObj<Object>> getMultitonIterables()
		{
			return getMultitonArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToObj<>(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testLongToObj(long[] nums)
		{
			ArrayList<String> expected = new ArrayList<>();
			for (long l : nums) {
				expected.add(Objects.toString(l));
			}
			ArrayIterator.OfLong iterator = new ArrayIterator.OfLong(nums);
			Iterator<String> actual = new MappingIterator.LongToObj<>(iterator, l -> Objects.toString(l));
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testLongToObj_Null()
		{
			PrimitiveIterator.OfLong iterator = EmptyIterator.OfLong();
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToObj<Long>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToObj<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LongToDouble implements MappingIteratorTest<Double>, FunctionalPrimitiveIteratorTest.OfDouble
	{
		@Override
		public Stream<MappingIterable.LongToDouble> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToDouble(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.LongToDouble> getEmptyIterables()
		{
			return getEmptyArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToDouble(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.LongToDouble> getSingletonIterables()
		{
			return getSingletonArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToDouble(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.LongToDouble> getMultitonIterables()
		{
			return getMultitonArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToDouble(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testLongToDouble(long[] nums)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (long l : nums) {
				expected.add(l + 1.0);
			}
			ArrayIterator.OfLong iterator = new ArrayIterator.OfLong(nums);
			PrimitiveIterator.OfDouble actual = new MappingIterator.LongToDouble(iterator, l -> l + 1.0);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testLongToDouble_Null()
		{
			PrimitiveIterator.OfLong iterator = EmptyIterator.OfLong();
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToDouble(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToDouble(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LongToInt implements MappingIteratorTest<Integer>, FunctionalPrimitiveIteratorTest.OfInt
	{
		@Override
		public Stream<MappingIterable.LongToInt> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToInt(iterable, each -> (int) each));
		}

		@Override
		public Stream<MappingIterable.LongToInt> getEmptyIterables()
		{
			return getEmptyArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToInt(iterable, each -> (int) each));
		}

		@Override
		public Stream<MappingIterable.LongToInt> getSingletonIterables()
		{
			return getSingletonArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToInt(iterable, each -> (int) each));
		}

		@Override
		public Stream<MappingIterable.LongToInt> getMultitonIterables()
		{
			return getMultitonArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToInt(iterable, each -> (int) each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testLongToInt(long[] nums)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (long l : nums) {
				expected.add((int) l + 1);
			}
			ArrayIterator.OfLong iterator = new ArrayIterator.OfLong(nums);
			PrimitiveIterator.OfInt actual = new MappingIterator.LongToInt(iterator, l -> (int) l + 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testLongToInt_Null()
		{
			PrimitiveIterator.OfLong iterator = EmptyIterator.OfLong();
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToInt(null, each -> (int) each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToInt(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LongToLong implements MappingIteratorTest<Long>, FunctionalPrimitiveIteratorTest.OfLong
	{
		@Override
		public Stream<MappingIterable.LongToLong> getDuplicatesIterables()
		{
			return getDuplicatesArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToLong(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.LongToLong> getEmptyIterables()
		{
			return getEmptyArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToLong(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.LongToLong> getSingletonIterables()
		{
			return getSingletonArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToLong(iterable, each -> each));
		}

		@Override
		public Stream<MappingIterable.LongToLong> getMultitonIterables()
		{
			return getMultitonArraysOfLong()
					.map(IterableArray.OfLong::new)
					.map(iterable -> new MappingIterable.LongToLong(iterable, each -> each));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testLongToLong(long[] nums)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (long l : nums) {
				expected.add(l + 1);
			}
			ArrayIterator.OfLong iterator = new ArrayIterator.OfLong(nums);
			PrimitiveIterator.OfLong actual = new MappingIterator.LongToLong(iterator, d -> (long) d + 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testLongToLong_Null()
		{
			PrimitiveIterator.OfLong iterator = EmptyIterator.OfLong();
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToLong(null, each -> (long) each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToLong(iterator, null));
		}
	}
}
