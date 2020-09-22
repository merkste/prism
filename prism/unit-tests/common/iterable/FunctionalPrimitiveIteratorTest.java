package common.iterable;

import common.functions.DoubleLongToDoubleFunction;
import common.functions.IntDoubleToIntFunction;
import common.functions.IntLongToIntFunction;
import common.functions.LongDoubleToLongFunction;
import common.functions.ObjDoubleFunction;
import common.functions.ObjIntFunction;
import common.functions.ObjLongFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIterableEquals;
import static common.iterable.Assertions.*;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.*;

abstract interface FunctionalPrimitiveIteratorTest
{
	interface OfDouble extends FunctionalIteratorTest<Double>
	{
		@Override
		default FunctionalPrimitiveIterator.OfDouble getAnyIterator()
		{
			return getIterables().findAny().get().iterator();
		}

		@Override
		default Stream<? extends FunctionalPrimitiveIterable.OfDouble> getIterables()
		{
			return Stream.concat(Stream.concat(getEmptyIterables(), getSingletonIterables()), getMultitonIterables());
		}

		@Override
		Stream<? extends FunctionalPrimitiveIterable.OfDouble> getDuplicatesIterables();

		@Override
		Stream<? extends FunctionalPrimitiveIterable.OfDouble> getEmptyIterables();

		@Override
		Stream<? extends FunctionalPrimitiveIterable.OfDouble> getSingletonIterables();

		@Override
		Stream<? extends FunctionalPrimitiveIterable.OfDouble> getMultitonIterables();

		@Override
		default PrimitiveIterable.OfDouble getExcluded(FunctionalIterable<Double> iterable)
		{
			List<Double> excluded = getExclusionListOfDouble();
			for (Double each : iterable) {
				excluded.remove(each);
				if (each == 0.0) {
					excluded. remove(-1 * each);
				}
			}
			return PrimitiveIterable.unboxDouble(excluded);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectArray(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			int n = (int) iterable.count();
			double[] expected = new double[n];
			int c = 0;
			for (double d: iterable) {
				expected[c++] = d;
			}
			double[] actual = iterable.iterator().collect(new double[n]);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectArrayOffset(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			int offset = 2, tail = 3;
			int n = (int) iterable.count() + offset + tail;
			double[] expected = new double[n];
			int c = offset;
			for (double d: iterable) {
				expected[c++] = d;
			}
			double[] actual = iterable.iterator().collect(new double[n], offset);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectAndCountArray(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			int n = (int) iterable.count();
			double[] expected = new double[n];
			int c = 0;
			for (double d: iterable) {
				expected[c++] = d;
			}
			double[] actual = new double[n];
			long count = iterable.iterator().collectAndCount(actual);
			assertEquals(n, count);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectAndCountArrayOffset(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			int offset = 2, tail = 3, n = (int) iterable.count();
			int size = n + offset + tail;
			double[] expected = new double[size];
			int c = offset;
			for (double d: iterable) {
				expected[c++] = d;
			}
			double[] actual = new double[size];
			long count = iterable.iterator().collectAndCount(actual, offset);
			assertEquals(n, count);
			assertArrayEquals(expected, actual);
		}

		@Test
		default void testCollect_Null()
		{
			FunctionalPrimitiveIterator.OfDouble iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.collect((double[]) null));
			assertThrows(NullPointerException.class, () -> iterator.collect((double[]) null, 0));
			assertThrows(NullPointerException.class, () -> iterator.collectAndCount((double[]) null));
			assertThrows(NullPointerException.class, () -> iterator.collectAndCount((double[]) null, 0));
		}

		@ParameterizedTest
		@MethodSource({"getIterables", "getDuplicatesIterables"})
		@Override
		default void testCollectDistinct(FunctionalIterable<Double> iterable)
		{
			assertTrue(iterable instanceof FunctionalPrimitiveIterable.OfDouble);
			Set<Double> expected = new HashSet<>();
			for (double each : iterable) {
				if (! expected.contains(each) && !(each == 0.0 && expected.contains(-1.0 * each))) {
					expected.add(each);
				}
			}
			List<Double> actual = iterable.iterator().collectDistinct().collect(new ArrayList<>());
			assertTrue(expected.containsAll(actual), "actual =< expected");
			assertTrue(actual.containsAll(expected), "actual >= expected");

		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFilter(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (double d : iterable) {
				if (d % 2 == 0) {
					expected.add(d);
				}
			}
			FunctionalPrimitiveIterator.OfDouble actual = iterable.iterator().filter((double d) -> d % 2 == 0);
			assertIteratorEquals(unboxDouble(expected.iterator()), actual);
		}

		@Test
		default void testFilter_Null()
		{
			FunctionalPrimitiveIterator.OfDouble iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.filter((DoublePredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFlatMap(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			FunctionalIterable<String> expected = iterable.map((double d) -> String.valueOf(d));
			Iterator<String> actual = iterable.iterator().flatMap((double d) -> List.of(String.valueOf(d)).iterator());
			assertIteratorEquals(expected.iterator(), actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFlatMapToDouble(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfDouble expected = unboxDouble(range.iterator().map((int i) -> (double) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfDouble actual = iterable.iterator().flatMapToDouble((double d) -> new SingletonIterator.OfDouble(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFlatMapToInt(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfInt expected = unboxInt(range.iterator());
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfInt actual = iterable.iterator().flatMapToInt((double d) -> new SingletonIterator.OfInt(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFlatMapToLong(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfLong expected = unboxLong(range.iterator().map((int i) -> (long) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfLong actual = iterable.iterator().flatMapToLong((double d) -> new SingletonIterator.OfLong(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testFlatMapToNull(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			assertThrows(NullPointerException.class, () -> iterable.iterator().flatMap((double d) -> null).consume());
		}

		@Test
		default void testFlatMap_Null()
		{
			FunctionalPrimitiveIterator.OfDouble iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.flatMap((DoubleFunction<? extends Iterator<?>>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToDouble((DoubleFunction<PrimitiveIterator.OfDouble>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToInt((DoubleFunction<PrimitiveIterator.OfInt>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToLong((DoubleFunction<PrimitiveIterator.OfLong>) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMap(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			Range expected = new Range((int) iterable.count());
			Range.RangeIterator index = expected.iterator();
			Iterator<Integer> actual = iterable.iterator().map((double d) -> index.next());
			assertIteratorEquals(expected.iterator(), actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMapToDouble(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfDouble expected = unboxDouble(range.iterator().map((int i) -> (double) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfDouble actual = iterable.iterator().mapToDouble((double d) -> index.next());
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMapToInt(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfInt expected = unboxInt(range.iterator());
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfInt actual = iterable.iterator().mapToInt((double d) -> index.next());
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMapToLong(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfLong expected = unboxLong(range.iterator().map((int i) -> (long) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfLong actual = iterable.iterator().mapToLong((double d) -> index.next());
			assertIteratorEquals(expected, actual);
		}

		@Test
		default void testMap_Null()
		{
			FunctionalPrimitiveIterator.OfDouble iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.map((DoubleFunction) null));
			assertThrows(NullPointerException.class, () -> iterator.mapToDouble((DoubleUnaryOperator) null));
			assertThrows(NullPointerException.class, () -> iterator.mapToInt((DoubleToIntFunction) null));
			assertThrows(NullPointerException.class, () -> iterator.mapToLong((DoubleToLongFunction) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		@DisplayName("forEachRemaining() yields same sequence as the underlying iterator.")
		default void testForEachRemainingDoubleConsumer(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			List<Double> actual = new ArrayList<>();
			FunctionalPrimitiveIterator.OfDouble iterator = iterable.iterator();
			iterator.forEachRemaining((double each) -> actual.add(each));
			assertIterableEquals(unboxDouble(iterable), unboxDouble(actual));
		}

		@Test
		default void testForEachRemainingDoubleConsumer_Null()
		{
			FunctionalPrimitiveIterator.OfDouble iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.forEachRemaining((DoubleConsumer) null));
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testReduceDoubleBinaryOperator_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			DoubleBinaryOperator dummy = (res, each) -> Double.MIN_VALUE;
			OptionalDouble actual = iterable.iterator().reduce(dummy);
			assertEquals(OptionalDouble.empty(), actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testReduceDoubleBinaryOperator_Singleton(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			DoubleBinaryOperator dummy = (res, each) -> Double.MIN_VALUE;
			double expected = iterable.iterator().next();
			OptionalDouble actual = iterable.iterator().reduce(dummy);
			assertEquals(expected, actual.getAsDouble());
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testReduceDoubleBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			List<Double> actual = new ArrayList<>();
			double probe = -31; // "unique" value
			DoubleBinaryOperator collect = new DoubleBinaryOperator()
			{
				@Override
				public double applyAsDouble(double res, double each)
				{
					if (actual.isEmpty()) {
						actual.add(res);
						actual.add(each);
						return probe;
					} else {
						actual.add(each);
						return res;
					}
				}
			};
			OptionalDouble result = iterable.iterator().reduce(collect);
			assertEquals(probe, result.getAsDouble());
			assertIterableEquals(unboxDouble(iterable), unboxDouble(actual));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testReduceIntDoubleToIntFunction(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			int init = Integer.MIN_VALUE;
			List<Double> actual = new ArrayList<>();
			IntDoubleToIntFunction collect = (res, each) -> {actual.add(each); return res;};
			int result = iterable.iterator().reduce(init, collect);
			assertEquals(init, result);
			assertIterableEquals(unboxDouble(iterable), unboxDouble(actual));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testReduceLongDoubleToLongFunction(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			long init = Long.MIN_VALUE;
			List<Double> actual = new ArrayList<>();
			LongDoubleToLongFunction collect = (res, each) -> {actual.add(each); return res;};
			long result = iterable.iterator().reduce(init, collect);
			assertEquals(init, result);
			assertIterableEquals(unboxDouble(iterable), unboxDouble(actual));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testReduceObjDouble(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			ObjDoubleFunction<List<Double>, List<Double>> collect = (seq, each) -> {seq.add(each); return seq;};
			List<Double> actual = iterable.iterator().reduce(new ArrayList<>(), collect);
			assertIterableEquals(unboxDouble(iterable), unboxDouble(actual));
			assertDoesNotThrow(() -> iterable.iterator().reduce(null, (Object obj, double each) -> null));
		}

		@Test
		default void testReduce_Null()
		{
			FunctionalPrimitiveIterator.OfDouble iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.reduce((DoubleBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce(0.0, (DoubleBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce(0, (IntDoubleToIntFunction) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce(0L, (LongDoubleToLongFunction) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce(new Object(), (ObjDoubleFunction<Object, Object>) null));
		}

		@Test
		default void testConcatTypes()
		{
			// primitive
			FunctionalIterator<Double> primitive = getAnyIterator().concat(getAnyIterator());
			assertTrue(primitive instanceof FunctionalPrimitiveIterator.OfDouble);
			// boxed
			FunctionalIterator<Double> boxed = getAnyIterator().concat(getAnyIterator(), getAnyIterator().map((double d) -> Double.valueOf(d)));
			assertTrue(primitive instanceof FunctionalPrimitiveIterator.OfDouble);
		}

		@ParameterizedTest
		@MethodSource({"getIterables", "getDuplicatesIterables"})
		@Override
		default void testDistinct(FunctionalIterable<Double> iterable)
		{
			assertTrue(iterable instanceof FunctionalPrimitiveIterable.OfDouble);
			List<Double> expected = new ArrayList<>();
			for (double each : iterable) {
				if (! expected.contains(each) && !(each == 0.0 && expected.contains(-1.0 * each))) {
					expected.add(each);
				}
			}
			List<Double> actual = iterable.iterator().distinct().collect(new ArrayList<>());
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getIterables", "getDuplicatesIterables"})
		default void testDedupe(FunctionalIterable<Double> iterable)
		{
			assertTrue(iterable instanceof FunctionalPrimitiveIterable.OfDouble);
			List<Double> expected = new ArrayList<>();
			for (double each : iterable) {
				if (expected.isEmpty()) {
					expected.add(each);
				} else {
					double last = expected.get(expected.size() - 1);
					if (last != each && !(Double.isNaN(last) && Double.isNaN(each))) {
						expected.add(each);
					}
				}
			}
			List<Double> actual = iterable.iterator().dedupe().collect(new ArrayList<>());
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getSingletonIterables", "getMultitonIterables"})
		default void testAllMatch(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			// match all elements
			assertTrue(iterable.iterator().allMatch((double each) -> true), "Expected allMatch() == true");
			// match not all elements
			DoublePredicate matchNotAll = new DoublePredicate() {
				// match: no element if singleton, otherwise every odd element
				boolean flag = iterable.iterator().count() == 1;
				@Override
				public boolean test(double d)
				{
					flag = !flag;
					return flag;
				};
			};
			assertFalse(iterable.iterator().allMatch(matchNotAll), "Expected allMatch() == false");
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testAllMatch_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			FunctionalPrimitiveIterator.OfDouble iterator = iterable.iterator();
			assertTrue(iterator.allMatch((double each) -> false), "Exepted allMatch() == true if iterator is empty");
		}

		@Test
		default void testAllMatch_Null()
		{
			FunctionalPrimitiveIterator.OfDouble iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.allMatch((DoublePredicate) null));
		}

		@ParameterizedTest
		@MethodSource({"getSingletonIterables", "getMultitonIterables"})
		default void testAnyMatch(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			// match no element
			assertFalse(iterable.iterator().anyMatch((double each) -> false), "Expected anyMatch() == false");
			// match some elements
			DoublePredicate matchSome = new DoublePredicate() {
				// match: first element if singleton, otherwise every even element
				boolean flag = iterable.iterator().count() > 1;
				@Override
				public boolean test(double d)
				{
					flag = !flag;
					return flag;
				};
			};
			assertTrue(iterable.iterator().anyMatch(matchSome), "Expected anyMatch() == true");
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testAnyMatch_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			FunctionalPrimitiveIterator.OfDouble iterator = iterable.iterator();
			assertTrue(iterator.allMatch((double each) -> true), "Exepted anyMatch() == false if iterator is empty");
		}

		@Test
		default void testAnyMatch_Null()
		{
			FunctionalPrimitiveIterator.OfDouble iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.anyMatch((DoublePredicate) null));
		}

		@ParameterizedTest
		@MethodSource({"getSingletonIterables", "getMultitonIterables"})
		default void testNoneMatch(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			// match no element
			assertTrue(iterable.iterator().noneMatch((double each) -> false), "Expected noneMatch() == true");
			// match some elements
			DoublePredicate matchSome = new DoublePredicate() {
				// match: first element if singleton, otherwise every even element
				boolean flag = iterable.iterator().count() > 1;
				@Override
				public boolean test(double d)
				{
					flag = !flag;
					return flag;
				};
			};
			assertFalse(iterable.iterator().noneMatch(matchSome), "Expected noneMatch() == false");
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testNoneMatch_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			FunctionalPrimitiveIterator.OfDouble iterator = iterable.iterator();
			assertTrue(iterator.allMatch((double each) -> false), "Exepted noneMatch() == false if iterator is empty");
		}

		@Test
		default void testNoneMatch_Null()
		{
			FunctionalPrimitiveIterator.OfDouble iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.noneMatch((DoublePredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		@Override
		default void testContains(FunctionalIterable<Double> iterable)
		{
			assertFalse(iterable.iterator().contains(null));
			testContains(FunctionalIterable.unboxDouble(iterable));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testContains(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			for (Double each : iterable) { // boxed double to trigger contains(Double d)
				assertTrue(iterable.iterator().contains(each), "Expected contains(" + each + ") == true");
				if (each == 0.0) {
					assertTrue(iterable.iterator().contains(-1.0 * each), "Expected contains(" + (-1.0 * each) + ") == true");
				}
			}
			for (Object each : getExcluded(iterable)) { // boxed double to trigger contains(Double d)
				assertFalse(iterable.iterator().contains(each), "Expected contains(" + each + ") == false");
			}
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCountPredicate(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			long expected =0L;
			for (FunctionalPrimitiveIterator.OfDouble iterator = iterable.iterator(); iterator.hasNext();) {
				if (iterator.nextDouble() % 2 == 1) {
					expected++;
				}
			}
			DoublePredicate odd = d -> d % 2 == 1;
			long actual = iterable.iterator().count(odd);
			assertEquals(expected, actual);
		}

		@Test
		default void testCountPredicate_Null()
		{
			FunctionalPrimitiveIterator.OfDouble iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.count((DoublePredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testDetect(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			FunctionalPrimitiveIterator.OfDouble iterator = iterable.iterator();
			assertThrows(NoSuchElementException.class, () -> iterator.detect((double each) -> false));
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testDetect_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			FunctionalPrimitiveIterator.OfDouble iterator = iterable.iterator();
			assertThrows(NoSuchElementException.class, () -> iterator.detect((double each) -> true));
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testDetect_Singleton(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			// match first element
			double expected = iterable.iterator().nextDouble();
			double actual = iterable.iterator().detect((double each) -> true);
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testDetect_Multiple(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			// match second element
			FunctionalPrimitiveIterator.OfDouble temp = iterable.iterator();
			temp.nextDouble();
			double expected = temp.nextDouble();
			DoublePredicate second = new DoublePredicate() {
				boolean flag = true;
				@Override
				public boolean test(double d)
				{
					flag = !flag;
					return flag;
				}
			};
			double actual = iterable.iterator().detect(second);
			assertEquals(expected, actual);
		}

		@Test
		default void testDetect_Null()
		{
			FunctionalPrimitiveIterator.OfDouble iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.detect((DoublePredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMin_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			OptionalDouble expected = OptionalDouble.empty();
			OptionalDouble actual = iterable.iterator().consume().min();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testMin_Singleton(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			OptionalDouble expected = OptionalDouble.of(iterable.iterator().next());
			OptionalDouble actual = iterable.iterator().min();
			assertDoubleEquals(expected.getAsDouble(), actual.getAsDouble());
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testMin_Multiple(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			FunctionalPrimitiveIterator.OfDouble iterator = iterable.iterator();
			double expected = iterator.next();
			while (iterator.hasNext()) {
				expected = Math.min(expected, iterator.nextDouble());
			}
			OptionalDouble actual = iterable.iterator().min();
			assertDoubleEquals(expected, actual.getAsDouble());
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMax_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			OptionalDouble expected = OptionalDouble.empty();
			OptionalDouble actual = iterable.iterator().consume().max();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testMax_Singleton(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			OptionalDouble expected = OptionalDouble.of(iterable.iterator().next());
			OptionalDouble actual = iterable.iterator().max();
			assertDoubleEquals(expected.getAsDouble(), actual.getAsDouble());
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testMax_Multiple(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			FunctionalPrimitiveIterator.OfDouble iterator = iterable.iterator();
			double expected = iterator.next();
			while (iterator.hasNext()) {
				expected = Math.max(expected, iterator.nextDouble());
			}
			OptionalDouble actual = iterable.iterator().max();
			assertDoubleEquals(expected, actual.getAsDouble());
		}

		@ParameterizedTest
		@MethodSource({"getIterables"})
		default void testSum(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			double expected = 0.0;
			for (FunctionalPrimitiveIterator.OfDouble iterator = iterable.iterator(); iterator.hasNext();) {
				expected += iterator.nextDouble();
			}
			double actual = iterable.iterator().sum();
			assertEquals(expected, actual, 1e-15);
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testSum_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{
			double expected = 0.0;
			double actual = iterable.iterator().sum();
			assertDoubleEquals(expected, actual);
		}
	}



	interface OfInt extends FunctionalIteratorTest<Integer>
	{
		@Override
		default FunctionalPrimitiveIterator.OfInt getAnyIterator()
		{
			return getIterables().findAny().get().iterator();
		}

		@Override
		default Stream<? extends FunctionalPrimitiveIterable.OfInt> getIterables()
		{
			return Stream.concat(Stream.concat(getEmptyIterables(), getSingletonIterables()), getMultitonIterables());
		}

		@Override
		Stream<? extends FunctionalPrimitiveIterable.OfInt> getDuplicatesIterables();

		@Override
		Stream<? extends FunctionalPrimitiveIterable.OfInt> getEmptyIterables();

		@Override
		Stream<? extends FunctionalPrimitiveIterable.OfInt> getSingletonIterables();

		@Override
		Stream<? extends FunctionalPrimitiveIterable.OfInt> getMultitonIterables();

		@Override
		default PrimitiveIterable.OfInt getExcluded(FunctionalIterable<Integer> iterable)
		{
			List<Integer> excluded = getExclusionListOfInt();
			for (Integer each : iterable) {
				excluded.remove(each);
			}
			return PrimitiveIterable.unboxInt(excluded);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectArray(FunctionalPrimitiveIterable.OfInt iterable)
		{
			int n = (int) iterable.count();
			int[] expected = new int[n];
			int c = 0;
			for (int i: iterable) {
				expected[c++] = i;
			}
			int[] actual = iterable.iterator().collect(new int[n]);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectArrayOffset(FunctionalPrimitiveIterable.OfInt iterable)
		{
			int offset = 2, tail = 3;
			int n = (int) iterable.count() + offset + tail;
			int[] expected = new int[n];
			int c = offset;
			for (int i: iterable) {
				expected[c++] = i;
			}
			int[] actual = iterable.iterator().collect(new int[n], offset);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectBitSet(FunctionalPrimitiveIterable.OfInt iterable)
		{
			BitSet expected = new BitSet();
			for (int i: iterable) {
				if (i >= 0) {
					expected.set(i);
				}
			}
			BitSet actual = iterable.iterator().filter((int i) -> i >= 0).collect(new BitSet());
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectAndCountArray(FunctionalPrimitiveIterable.OfInt iterable)
		{
			int n = (int) iterable.count();
			int[] expected = new int[n];
			int c = 0;
			for (int i: iterable) {
				expected[c++] = i;
			}
			int[] actual = new int[n];
			long count = iterable.iterator().collectAndCount(actual);
			assertEquals(n, count);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectAndCountArrayOffset(FunctionalPrimitiveIterable.OfInt iterable)
		{
			int offset = 2, tail = 3, n = (int) iterable.count();
			int size = n + offset + tail;
			int[] expected = new int[size];
			int c = offset;
			for (int i: iterable) {
				expected[c++] = i;
			}
			int[] actual = new int[size];
			long count = iterable.iterator().collectAndCount(actual, offset);
			assertEquals(n, count);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectAndCountBitSet(FunctionalPrimitiveIterable.OfInt iterable)
		{
			BitSet expected = new BitSet();
			int expectedCount = 0;
			for (int i: iterable) {
				if (i >= 0) {
					expectedCount++;
					expected.set(i);
				}
			}
			BitSet actual = new BitSet();
			long actualCount = iterable.iterator().filter((int i) -> i >= 0).collectAndCount(actual);
			assertEquals(expectedCount, actualCount);
			assertEquals(expected, actual);
		}

		@Test
		default void testCollect_Null()
		{
			FunctionalPrimitiveIterator.OfInt iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.collect((int[]) null));
			assertThrows(NullPointerException.class, () -> iterator.collect((int[]) null, 0));
			assertThrows(NullPointerException.class, () -> iterator.collectAndCount((int[]) null));
			assertThrows(NullPointerException.class, () -> iterator.collectAndCount((int[]) null, 0));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFilter(FunctionalPrimitiveIterable.OfInt iterable)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (int i : iterable) {
				if (i % 2 == 0) {
					expected.add(i);
				}
			}
			FunctionalPrimitiveIterator.OfInt actual = iterable.iterator().filter((int i) -> i % 2 == 0);
			assertIteratorEquals(unboxInt(expected.iterator()), actual);
		}

		@Test
		default void testFilter_Null()
		{
			FunctionalPrimitiveIterator.OfInt iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.filter((IntPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFlatMap(FunctionalPrimitiveIterable.OfInt iterable)
		{
			FunctionalIterable<String> expected = iterable.map((int i) -> String.valueOf(i));
			Iterator<String> actual = iterable.iterator().flatMap((int i) -> List.of(String.valueOf(i)).iterator());
			assertIteratorEquals(expected.iterator(), actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFlatMapToDouble(FunctionalPrimitiveIterable.OfInt iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfDouble expected = unboxDouble(range.iterator().map((int i) -> (double) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfDouble actual = iterable.iterator().flatMapToDouble((int i) -> new SingletonIterator.OfDouble(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFlatMapToInt(FunctionalPrimitiveIterable.OfInt iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfInt expected = unboxInt(range.iterator());
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfInt actual = iterable.iterator().flatMapToInt((int i) -> new SingletonIterator.OfInt(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFlatMapToLong(FunctionalPrimitiveIterable.OfInt iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfLong expected = unboxLong(range.iterator().map((int i) -> (long) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfLong actual = iterable.iterator().flatMapToLong((int i) -> new SingletonIterator.OfLong(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testFlatMapToNull(FunctionalPrimitiveIterable.OfInt iterable)
		{
			assertThrows(NullPointerException.class, () -> iterable.iterator().flatMap((int i) -> null).consume());
		}

		@Test
		default void testFlatMap_Null()
		{
			FunctionalPrimitiveIterator.OfInt iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.flatMap((IntFunction<? extends Iterator<?>>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToDouble((IntFunction<PrimitiveIterator.OfDouble>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToInt((IntFunction<PrimitiveIterator.OfInt>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToLong((IntFunction<PrimitiveIterator.OfLong>) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMap(FunctionalPrimitiveIterable.OfInt iterable)
		{
			Range expected = new Range((int) iterable.count());
			Range.RangeIterator index = expected.iterator();
			Iterator<Integer> actual = iterable.iterator().map((int i) -> index.next());
			assertIteratorEquals(expected.iterator(), actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMapToDouble(FunctionalPrimitiveIterable.OfInt iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfDouble expected = range.iterator().mapToDouble((int i) -> i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfDouble actual = iterable.iterator().mapToDouble((int i) -> index.next());
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMapToInt(FunctionalPrimitiveIterable.OfInt iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfInt expected = unboxInt(range.iterator());
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfInt actual = iterable.iterator().mapToInt((int i) -> index.next());
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMapToLong(FunctionalPrimitiveIterable.OfInt iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfLong expected = range.iterator().mapToLong((int i) -> i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfLong actual = iterable.iterator().mapToLong((int i) -> index.next());
			assertIteratorEquals(expected, actual);
		}

		@Test
		default void testMap_Null()
		{
			FunctionalPrimitiveIterator.OfInt iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.map((IntFunction) null));
			assertThrows(NullPointerException.class, () -> iterator.mapToDouble((IntToDoubleFunction) null));
			assertThrows(NullPointerException.class, () -> iterator.mapToInt((IntUnaryOperator) null));
			assertThrows(NullPointerException.class, () -> iterator.mapToLong((IntToLongFunction) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		@DisplayName("forEachRemaining() yields same sequence as the underlying iterator.")
		default void testForEachRemainingIntConsumer(FunctionalPrimitiveIterable.OfInt iterable)
		{
			List<Integer> actual = new ArrayList<>();
			FunctionalPrimitiveIterator.OfInt iterator = iterable.iterator();
			iterator.forEachRemaining((int each) -> actual.add(each));
			assertIterableEquals(unboxInt(iterable), unboxInt(actual));
		}

		@Test
		default void testForEachRemainingIntConsumer_Null()
		{
			FunctionalPrimitiveIterator.OfInt iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.forEachRemaining((IntConsumer) null));
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testReduceIntBinaryOperator_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{
			IntBinaryOperator dummy = (res, each) -> Integer.MIN_VALUE;
			OptionalInt actual = iterable.iterator().reduce(dummy);
			assertEquals(OptionalInt.empty(), actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testReduceIntBinaryOperator_Singleton(FunctionalPrimitiveIterable.OfInt iterable)
		{
			IntBinaryOperator dummy = (res, each) -> Integer.MIN_VALUE;
			int expected = iterable.iterator().next();
			OptionalInt actual = iterable.iterator().reduce(dummy);
			assertEquals(expected, actual.getAsInt());
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testReduceIntBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{
			List<Integer> actual = new ArrayList<>();
			int probe = -31; // "unique" value
			IntBinaryOperator collect = new IntBinaryOperator()
			{
				@Override
				public int applyAsInt(int res, int each)
				{
					if (actual.isEmpty()) {
						actual.add(res);
						actual.add(each);
						return probe;
					} else {
						actual.add(each);
						return res;
					}
				}
			};
			OptionalInt result = iterable.iterator().reduce(collect);
			assertEquals(probe, result.getAsInt());
			assertIterableEquals(unboxInt(iterable), unboxInt(actual));
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testReduceDoubleBinaryOperator_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{
			DoubleBinaryOperator dummy = (res, each) -> Double.MIN_VALUE;
			OptionalDouble actual = iterable.iterator().reduce(dummy);
			assertEquals(OptionalDouble.empty(), actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testReduceDoubleBinaryOperator_Singleton(FunctionalPrimitiveIterable.OfInt iterable)
		{
			DoubleBinaryOperator dummy = (res, each) -> Double.MIN_VALUE;
			double expected = iterable.iterator().next();
			OptionalDouble actual = iterable.iterator().reduce(dummy);
			assertEquals(expected, actual.getAsDouble());
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testReduceDoubleBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{
			List<Double> actual = new ArrayList<>();
			double probe = -31; // "unique" value
			DoubleBinaryOperator collect = new DoubleBinaryOperator()
			{
				@Override
				public double applyAsDouble(double res, double each)
				{
					if (actual.isEmpty()) {
						actual.add(res);
						actual.add(each);
						return probe;
					} else {
						actual.add(each);
						return res;
					}
				}
			};
			OptionalDouble result = iterable.iterator().reduce(collect);
			assertEquals(probe, result.getAsDouble());
			assertIterableEquals(unboxDouble(iterable.mapToDouble((int i) -> i)), unboxDouble(actual));
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testReduceLongBinaryOperator_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{
			LongBinaryOperator dummy = (res, each) -> Long.MIN_VALUE;
			OptionalLong actual = iterable.iterator().reduce(dummy);
			assertEquals(OptionalLong.empty(), actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testReduceLongBinaryOperator_Singleton(FunctionalPrimitiveIterable.OfInt iterable)
		{
			LongBinaryOperator dummy = (res, each) -> Long.MIN_VALUE;
			long expected = iterable.iterator().next();
			OptionalLong actual = iterable.iterator().reduce(dummy);
			assertEquals(expected, actual.getAsLong());
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testReduceLongBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{
			List<Long> actual = new ArrayList<>();
			long probe = -31; // "unique" value
			LongBinaryOperator collect = new LongBinaryOperator()
			{
				@Override
				public long applyAsLong(long res, long each)
				{
					if (actual.isEmpty()) {
						actual.add(res);
						actual.add(each);
						return probe;
					} else {
						actual.add(each);
						return res;
					}
				}
			};
			OptionalLong result = iterable.iterator().reduce(collect);
			assertEquals(probe, result.getAsLong());
			assertIterableEquals(unboxLong(iterable.mapToLong((int i) -> i)), unboxLong(actual));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testReduceObjIntFunction(FunctionalPrimitiveIterable.OfInt iterable)
		{
			ObjIntFunction<List<Integer>, List<Integer>> collect = (seq, each) -> {seq.add(each); return seq;};
			List<Integer> actual = iterable.iterator().reduce(new ArrayList<>(), collect);
			assertIterableEquals(unboxInt(iterable), unboxInt(actual));
			assertDoesNotThrow(() -> iterable.iterator().reduce(null, (Object obj, int each) -> null));
		}

		@Test
		default void testReduce_Null()
		{
			FunctionalPrimitiveIterator.OfInt iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.reduce((DoubleBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce((IntBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce((LongBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce(0.0, (DoubleBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce(0, (IntBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce(0L, (LongBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce(new Object(), (ObjIntFunction<Object, Object>) null));
		}

		@Test
		default void testConcatTypes()
		{
			// primitive
			FunctionalIterator<Integer> primitive = getAnyIterator().concat(getAnyIterator());
			assertTrue(primitive instanceof FunctionalPrimitiveIterator.OfInt);
			// boxed
			FunctionalIterator<Integer> boxed = getAnyIterator().concat(getAnyIterator(), getAnyIterator().map((int i) -> Integer.valueOf(i)));
			assertTrue(primitive instanceof FunctionalPrimitiveIterator.OfInt);
		}

		@ParameterizedTest
		@MethodSource({"getSingletonIterables", "getMultitonIterables"})
		default void testAllMatch(FunctionalPrimitiveIterable.OfInt iterable)
		{
			// match all elements
			assertTrue(iterable.iterator().allMatch((int each) -> true), "Expected allMatch() == true");
			// match not all elements
			IntPredicate matchNotAll = new IntPredicate() {
				// match: no element if singleton, otherwise every odd element
				boolean flag = iterable.iterator().count() == 1;
				@Override
				public boolean test(int i)
				{
					flag = !flag;
					return flag;
				};
			};
			assertFalse(iterable.iterator().allMatch(matchNotAll), "Expected allMatch() == false");
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testAllMatch_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{
			FunctionalPrimitiveIterator.OfInt iterator = iterable.iterator();
			assertTrue(iterator.allMatch((int each) -> false), "Exepted allMatch() == true if iterator is empty");
		}

		@Test
		default void testAllMatch_Null()
		{
			FunctionalPrimitiveIterator.OfInt iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.allMatch((IntPredicate) null));
		}

		@ParameterizedTest
		@MethodSource({"getSingletonIterables", "getMultitonIterables"})
		default void testAnyMatch(FunctionalPrimitiveIterable.OfInt iterable)
		{
			// match no element
			assertFalse(iterable.iterator().anyMatch((int each) -> false), "Expected anyMatch() == false");
			// match some elements
			IntPredicate matchSome = new IntPredicate() {
				// match: first element if singleton, otherwise every even element
				boolean flag = iterable.iterator().count() > 1;
				@Override
				public boolean test(int i)
				{
					flag = !flag;
					return flag;
				};
			};
			assertTrue(iterable.iterator().anyMatch(matchSome), "Expected anyMatch() == true");
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testAnyMatch_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{
			FunctionalPrimitiveIterator.OfInt iterator = iterable.iterator();
			assertTrue(iterator.allMatch((int each) -> true), "Exepted anyMatch() == false if iterator is empty");
		}

		@Test
		default void testAnyMatch_Null()
		{
			FunctionalPrimitiveIterator.OfInt iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.anyMatch((IntPredicate) null));
		}

		@ParameterizedTest
		@MethodSource({"getSingletonIterables", "getMultitonIterables"})
		default void testNoneMatch(FunctionalPrimitiveIterable.OfInt iterable)
		{
			// match no element
			assertTrue(iterable.iterator().noneMatch((int each) -> false), "Expected noneMatch() == true");
			// match some elements
			IntPredicate matchSome = new IntPredicate() {
				// match: first element if singleton, otherwise every even element
				boolean flag = iterable.iterator().count() > 1;
				@Override
				public boolean test(int i)
				{
					flag = !flag;
					return flag;
				};
			};
			assertFalse(iterable.iterator().noneMatch(matchSome), "Expected noneMatch() == false");
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testNoneMatch_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{
			FunctionalPrimitiveIterator.OfInt iterator = iterable.iterator();
			assertTrue(iterator.allMatch((int each) -> false), "Exepted noneMatch() == false if iterator is empty");
		}

		@Test
		default void testNoneMatch_Null()
		{
			FunctionalPrimitiveIterator.OfInt iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.noneMatch((IntPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		@Override
		default void testContains(FunctionalIterable<Integer> iterable)
		{
			assertFalse(iterable.iterator().contains(null), "Expected contains(null) == false");
			testContains(FunctionalIterable.unboxInt(iterable));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testContains(FunctionalPrimitiveIterable.OfInt iterable)
		{
			for (Integer each : iterable) { // boxed int to trigger contains(Integer i)
				assertTrue(iterable.iterator().contains(each), "Expected contains(" + each + ") == true");
			}
			for (Object each : getExcluded(iterable)) { // boxed int to trigger contains(Integer i)
				assertFalse(iterable.iterator().contains(each), "Expected contains(" + each + ") == false");
			}
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCountPredicate(FunctionalPrimitiveIterable.OfInt iterable)
		{
			long expected =0L;
			for (FunctionalPrimitiveIterator.OfInt iterator = iterable.iterator(); iterator.hasNext();) {
				if (iterator.nextInt() % 2 == 1) {
					expected++;
				}
			}
			IntPredicate odd = i -> i % 2 == 1;
			long actual = iterable.iterator().count(odd);
			assertEquals(expected, actual);
		}

		@Test
		default void testCountPredicate_Null()
		{
			FunctionalPrimitiveIterator.OfInt iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.count((IntPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testDetect(FunctionalPrimitiveIterable.OfInt iterable)
		{
			FunctionalPrimitiveIterator.OfInt iterator = iterable.iterator();
			assertThrows(NoSuchElementException.class, () -> iterator.detect((int each) -> false));
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testDetect_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{
			FunctionalPrimitiveIterator.OfInt iterator = iterable.iterator();
			assertThrows(NoSuchElementException.class, () -> iterator.detect((int each) -> true));
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testDetect_Singleton(FunctionalPrimitiveIterable.OfInt iterable)
		{
			// match first element
			int expected = iterable.iterator().nextInt();
			int actual = iterable.iterator().detect((int each) -> true);
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testDetect_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{
			// match second element
			FunctionalPrimitiveIterator.OfInt temp = iterable.iterator();
			temp.nextInt();
			int expected = temp.nextInt();
			IntPredicate second = new IntPredicate() {
				boolean flag = true;
				@Override
				public boolean test(int i)
				{
					flag = !flag;
					return flag;
				}
			};
			int actual = iterable.iterator().detect(second);
			assertEquals(expected, actual);
		}

		@Test
		default void testDetect_Null()
		{
			FunctionalPrimitiveIterator.OfInt iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.detect((IntPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMin_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{
			OptionalInt expected = OptionalInt.empty();
			OptionalInt actual = iterable.iterator().consume().min();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testMin_Singleton(FunctionalPrimitiveIterable.OfInt iterable)
		{
			OptionalInt expected = OptionalInt.of(iterable.iterator().next());
			OptionalInt actual = iterable.iterator().min();
			assertEquals(expected.getAsInt(), actual.getAsInt());
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testMin_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{
			FunctionalPrimitiveIterator.OfInt iterator = iterable.iterator();
			int expected = iterator.next();
			while (iterator.hasNext()) {
				expected = Math.min(expected, iterator.nextInt());
			}
			OptionalInt actual = iterable.iterator().min();
			assertEquals(expected, actual.getAsInt());
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMax_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{
			OptionalInt expected = OptionalInt.empty();
			OptionalInt actual = iterable.iterator().consume().max();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testMax_Singleton(FunctionalPrimitiveIterable.OfInt iterable)
		{
			OptionalInt expected = OptionalInt.of(iterable.iterator().next());
			OptionalInt actual = iterable.iterator().max();
			assertEquals(expected.getAsInt(), actual.getAsInt());
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testMax_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{
			FunctionalPrimitiveIterator.OfInt iterator = iterable.iterator();
			int expected = iterator.next();
			while (iterator.hasNext()) {
				expected = Math.max(expected, iterator.nextInt());
			}
			OptionalInt actual = iterable.iterator().max();
			assertEquals(expected, actual.getAsInt());
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testSum(FunctionalPrimitiveIterable.OfInt iterable)
		{
			long expected = 0L;
			for (FunctionalPrimitiveIterator.OfInt iterator = iterable.iterator(); iterator.hasNext();) {
				expected += iterator.nextInt();
			}
			long actual = iterable.iterator().sum();
			assertEquals(expected, actual);
		}
	}



	interface OfLong extends FunctionalIteratorTest<Long>
	{
		@Override
		default FunctionalPrimitiveIterator.OfLong getAnyIterator()
		{
			return getIterables().findAny().get().iterator();
		}

		@Override
		default Stream<? extends FunctionalPrimitiveIterable.OfLong> getIterables()
		{
			return Stream.concat(Stream.concat(getEmptyIterables(), getSingletonIterables()), getMultitonIterables());
		}

		@Override
		Stream<? extends FunctionalPrimitiveIterable.OfLong> getDuplicatesIterables();

		@Override
		Stream<? extends FunctionalPrimitiveIterable.OfLong> getEmptyIterables();

		@Override
		Stream<? extends FunctionalPrimitiveIterable.OfLong> getSingletonIterables();

		@Override
		Stream<? extends FunctionalPrimitiveIterable.OfLong> getMultitonIterables();

		@Override
		default PrimitiveIterable.OfLong getExcluded(FunctionalIterable<Long> iterable)
		{
			List<Long> excluded = getExclusionListOfLong();
			for (Long each : iterable) {
				excluded.remove(each);
			}
			return PrimitiveIterable.unboxLong(excluded);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectArray(FunctionalPrimitiveIterable.OfLong iterable)
		{
			int n = (int) iterable.count();
			long[] expected = new long[n];
			int c = 0;
			for (long l: iterable) {
				expected[c++] = l;
			}
			long[] actual = iterable.iterator().collect(new long[n]);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectArrayOffset(FunctionalPrimitiveIterable.OfLong iterable)
		{
			int offset = 2, tail = 3;
			int n = (int) iterable.count() + offset + tail;
			long[] expected = new long[n];
			int c = offset;
			for (long l: iterable) {
				expected[c++] = l;
			}
			long[] actual = iterable.iterator().collect(new long[n], offset);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectAndCountArray(FunctionalPrimitiveIterable.OfLong iterable)
		{
			int n = (int) iterable.count();
			long[] expected = new long[n];
			int c = 0;
			for (long l: iterable) {
				expected[c++] = l;
			}
			long[] actual = new long[n];
			long count = iterable.iterator().collectAndCount(actual);
			assertEquals(n, count);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCollectAndCountArrayOffset(FunctionalPrimitiveIterable.OfLong iterable)
		{
			int offset = 2, tail = 3, n = (int) iterable.count();
			int size = n + offset + tail;
			long[] expected = new long[size];
			int c = offset;
			for (long l: iterable) {
				expected[c++] = l;
			}
			long[] actual = new long[size];
			long count = iterable.iterator().collectAndCount(actual, offset);
			assertEquals(n, count);
			assertArrayEquals(expected, actual);
		}

		@Test
		default void testCollect_Null()
		{
			FunctionalPrimitiveIterator.OfLong iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.collect((long[]) null));
			assertThrows(NullPointerException.class, () -> iterator.collect((long[]) null, 0));
			assertThrows(NullPointerException.class, () -> iterator.collectAndCount((long[]) null));
			assertThrows(NullPointerException.class, () -> iterator.collectAndCount((long[]) null, 0));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFilter(FunctionalPrimitiveIterable.OfLong iterable)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (long l : iterable) {
				if (l % 2 == 0) {
					expected.add(l);
				}
			}
			FunctionalPrimitiveIterator.OfLong actual = iterable.iterator().filter((long l) ->  l % 2 == 0);
			assertIteratorEquals(unboxLong(expected.iterator()), actual);
		}

		@Test
		default void testFilter_Null()
		{
			FunctionalPrimitiveIterator.OfLong iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.filter((LongPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFlatMap(FunctionalPrimitiveIterable.OfLong iterable)
		{
			FunctionalIterable<String> expected = iterable.map((long l) -> String.valueOf(l));
			Iterator<String> actual = iterable.iterator().flatMap((long l) -> List.of(String.valueOf(l)).iterator());
			assertIteratorEquals(expected.iterator(), actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFlatMapToDouble(FunctionalPrimitiveIterable.OfLong iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfDouble expected = unboxDouble(range.iterator().map((int i) -> (double) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfDouble actual = iterable.iterator().flatMapToDouble((long l) -> new SingletonIterator.OfDouble(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFlatMapToInt(FunctionalPrimitiveIterable.OfLong iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfInt expected = unboxInt(range.iterator());
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfInt actual = iterable.iterator().flatMapToInt((long l) -> new SingletonIterator.OfInt(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testFlatMapToLong(FunctionalPrimitiveIterable.OfLong iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfLong expected = unboxLong(range.iterator().map((int i) -> (long) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfLong actual = iterable.iterator().flatMapToLong((long l) -> new SingletonIterator.OfLong(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testFlatMapToNull(FunctionalPrimitiveIterable.OfLong iterable)
		{
			assertThrows(NullPointerException.class, () -> iterable.iterator().flatMap((long l) -> null).consume());
		}

		@Test
		default void testFlatMap_Null()
		{
			FunctionalPrimitiveIterator.OfLong iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.flatMap((LongFunction<? extends Iterator<?>>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToDouble((LongFunction<PrimitiveIterator.OfDouble>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToInt((LongFunction<PrimitiveIterator.OfInt>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToLong((LongFunction<PrimitiveIterator.OfLong>) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMap(FunctionalPrimitiveIterable.OfLong iterable)
		{
			Range expected = new Range((int) iterable.count());
			Range.RangeIterator index = expected.iterator();
			Iterator<Integer> actual = iterable.iterator().map((long l) -> index.next());
			assertIteratorEquals(expected.iterator(), actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMapToDouble(FunctionalPrimitiveIterable.OfLong iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfDouble expected = range.iterator().mapToDouble((int i) -> i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfDouble actual = iterable.iterator().mapToDouble((long l) -> index.next());
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMapToInt(FunctionalPrimitiveIterable.OfLong iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfInt expected = unboxInt(range.iterator());
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfInt actual = iterable.iterator().mapToInt((long l) -> index.next());
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMapToLong(FunctionalPrimitiveIterable.OfLong iterable)
		{
			Range range = new Range((int) iterable.count());
			PrimitiveIterator.OfLong expected = range.iterator().mapToLong((int i) -> i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfLong actual = iterable.iterator().mapToLong((long l) -> index.next());
			assertIteratorEquals(expected, actual);
		}

		@Test
		default void testMap_Null()
		{
			FunctionalPrimitiveIterator.OfLong iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.map((LongFunction) null));
			assertThrows(NullPointerException.class, () -> iterator.mapToDouble((LongToDoubleFunction) null));
			assertThrows(NullPointerException.class, () -> iterator.mapToInt((LongToIntFunction) null));
			assertThrows(NullPointerException.class, () -> iterator.mapToLong((LongUnaryOperator) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		@DisplayName("forEachRemaining() yields same sequence as the underlying iterator.")
		default void testForEachRemainingLongConsumer(FunctionalPrimitiveIterable.OfLong iterable)
		{
			FunctionalPrimitiveIterator.OfLong iterator = iterable.iterator();
			List<Long> actual = new ArrayList<>();
			iterator.forEachRemaining((long each) -> actual.add(each));
			assertIterableEquals(unboxLong(iterable), unboxLong(actual));
		}

		@Test
		default void testForEachRemainingLongConsumer_Null()
		{
			FunctionalPrimitiveIterator.OfLong iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.forEachRemaining((LongConsumer) null));
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testReduceLongBinaryOperator_Empty(FunctionalPrimitiveIterable.OfLong iterable)
		{
			LongBinaryOperator dummy = (res, each) -> Long.MIN_VALUE;
			OptionalLong actual = iterable.iterator().reduce(dummy);
			assertEquals(OptionalLong.empty(), actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testReduceLongBinaryOperator_Singleton(FunctionalPrimitiveIterable.OfLong iterable)
		{
			LongBinaryOperator dummy = (res, each) -> Long.MIN_VALUE;
			long expected = iterable.iterator().next();
			OptionalLong actual = iterable.iterator().reduce(dummy);
			assertEquals(expected, actual.getAsLong());
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testReduceLongBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfLong iterable)
		{
			List<Long> actual = new ArrayList<>();
			long probe = -31; // "unique" value
			LongBinaryOperator collect = new LongBinaryOperator()
			{
				@Override
				public long applyAsLong(long res, long each)
				{
					if (actual.isEmpty()) {
						actual.add(res);
						actual.add(each);
						return probe;
					} else {
						actual.add(each);
						return res;
					}
				}
			};
			OptionalLong result = iterable.iterator().reduce(collect);
			assertEquals(probe, result.getAsLong());
			assertIterableEquals(unboxLong(iterable), unboxLong(actual));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testReduceDoubleLongToDoubleFunction(FunctionalPrimitiveIterable.OfLong iterable)
		{
			double init = Double.MIN_VALUE;
			List<Long> actual = new ArrayList<>();
			DoubleLongToDoubleFunction collect = (res, each) -> {actual.add(each); return res;};
			double result = iterable.iterator().reduce(init, collect);
			assertEquals(init, result);
			assertIterableEquals(unboxLong(iterable), unboxLong(actual));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testReduceIntLongToIntFunction(FunctionalPrimitiveIterable.OfLong iterable)
		{
			int init = Integer.MIN_VALUE;
			List<Long> actual = new ArrayList<>();
			IntLongToIntFunction collect = (res, each) -> {actual.add(each); return res;};
			int result = iterable.iterator().reduce(init, collect);
			assertEquals(init, result);
			assertIterableEquals(unboxLong(iterable), unboxLong(actual));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testReduceObjLong(FunctionalPrimitiveIterable.OfLong iterable)
		{
			ObjLongFunction<List<Long>, List<Long>> collect = (seq, each) -> {seq.add(each); return seq;};
			List<Long> actual = iterable.iterator().reduce(new ArrayList<>(), collect);
			assertIterableEquals(unboxLong(iterable), unboxLong(actual));
			assertDoesNotThrow(() -> iterable.iterator().reduce(null, (Object obj, long each) -> null));
		}

		@Test
		default void testReduce_Null()
		{
			FunctionalPrimitiveIterator.OfLong iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.reduce((LongBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce(0.0, (DoubleLongToDoubleFunction) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce(0, (IntLongToIntFunction) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce(0L, (LongBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> iterator.reduce(new Object(), (ObjLongFunction<Object, Object>) null));
		}

		@Test
		default void testConcatTypes()
		{
			// primitive
			FunctionalIterator<Long> primitive = getAnyIterator().concat(getAnyIterator());
			assertTrue(primitive instanceof FunctionalPrimitiveIterator.OfLong);
			// boxed
			FunctionalIterator<Long> boxed = getAnyIterator().concat(getAnyIterator(), getAnyIterator().map((long l) -> Long.valueOf(l)));
			assertTrue(primitive instanceof FunctionalPrimitiveIterator.OfLong);
		}

		@ParameterizedTest
		@MethodSource({"getSingletonIterables", "getMultitonIterables"})
		default void testAllMatch(FunctionalPrimitiveIterable.OfLong iterable)
		{
			// match all elements
			assertTrue(iterable.iterator().allMatch((long each) -> true), "Expected allMatch() == true");
			// match not all elements
			LongPredicate matchNotAll = new LongPredicate() {
				// match: no element if singleton, otherwise every odd element
				boolean flag = iterable.iterator().count() == 1;
				@Override
				public boolean test(long i)
				{
					flag = !flag;
					return flag;
				};
			};
			assertFalse(iterable.iterator().allMatch(matchNotAll), "Expected allMatch() == false");
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testAllMatch_Empty(FunctionalPrimitiveIterable.OfLong iterable)
		{
			FunctionalPrimitiveIterator.OfLong iterator = iterable.iterator();
			assertTrue(iterator.allMatch((long each) -> false), "Exepted allMatch() == true if iterator is empty");
		}

		@Test
		default void testAllMatch_Null()
		{
			FunctionalPrimitiveIterator.OfLong iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.allMatch((LongPredicate) null));
		}

		@ParameterizedTest
		@MethodSource({"getSingletonIterables", "getMultitonIterables"})
		default void testAnyMatch(FunctionalPrimitiveIterable.OfLong iterable)
		{
			// match no element
			assertFalse(iterable.iterator().anyMatch((long each) -> false), "Expected anyMatch() == false");
			// match some elements
			LongPredicate matchSome = new LongPredicate() {
				// match: first element if singleton, otherwise every even element
				boolean flag = iterable.iterator().count() > 1;
				@Override
				public boolean test(long i)
				{
					flag = !flag;
					return flag;
				};
			};
			assertTrue(iterable.iterator().anyMatch(matchSome), "Expected anyMatch() == true");
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testAnyMatch_Empty(FunctionalPrimitiveIterable.OfLong iterable)
		{
			FunctionalPrimitiveIterator.OfLong iterator = iterable.iterator();
			assertTrue(iterator.allMatch((long each) -> true), "Exepted anyMatch() == false if iterator is empty");
		}

		@Test
		default void testAnyMatch_Null()
		{
			FunctionalPrimitiveIterator.OfLong iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.anyMatch((LongPredicate) null));
		}

		@ParameterizedTest
		@MethodSource({"getSingletonIterables", "getMultitonIterables"})
		default void testNoneMatch(FunctionalPrimitiveIterable.OfLong iterable)
		{
			// match no element
			assertTrue(iterable.iterator().noneMatch((long each) -> false), "Expected noneMatch() == true");
			// match some elements
			LongPredicate matchSome = new LongPredicate() {
				// match: first element if singleton, otherwise every even element
				boolean flag = iterable.iterator().count() > 1;
				@Override
				public boolean test(long i)
				{
					flag = !flag;
					return flag;
				};
			};
			assertFalse(iterable.iterator().noneMatch(matchSome), "Expected noneMatch() == false");
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testNoneMatch_Empty(FunctionalPrimitiveIterable.OfLong iterable)
		{
			FunctionalPrimitiveIterator.OfLong iterator = iterable.iterator();
			assertTrue(iterator.allMatch((long each) -> false), "Exepted noneMatch() == false if iterator is empty");
		}

		@Test
		default void testNoneMatch_Null()
		{
			FunctionalPrimitiveIterator.OfLong iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.noneMatch((LongPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		@Override
		default void testContains(FunctionalIterable<Long> iterable)
		{
			assertFalse(iterable.iterator().contains(null), "Expected contains(null) == false");
			testContains(FunctionalIterable.unboxLong(iterable));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testContains(FunctionalPrimitiveIterable.OfLong iterable)
		{
			for (Long each : iterable) { // boxed long to trigger contains(Long l)
				assertTrue(iterable.iterator().contains(each), "Expected contains(" + each + ") == true");
			}
			for (Object each : getExcluded(iterable)) { // boxed long to trigger contains(Long l)
				assertFalse(iterable.iterator().contains(each), "Expected contains(" + each + ") == false");
			}
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testCountPredicate(FunctionalPrimitiveIterable.OfLong iterable)
		{
			long expected =0L;
			for (FunctionalPrimitiveIterator.OfLong iterator = iterable.iterator(); iterator.hasNext();) {
				if (iterator.nextLong() % 2 == 1) {
					expected++;
				}
			}
			LongPredicate odd = l -> l % 2 == 1;
			long actual = iterable.iterator().count(odd);
			assertEquals(expected, actual);
		}

		@Test
		default void testCountPredicate_Null()
		{
			FunctionalPrimitiveIterator.OfLong iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.count((LongPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testDetect(FunctionalPrimitiveIterable.OfLong iterable)
		{
			FunctionalPrimitiveIterator.OfLong iterator = iterable.iterator();
			assertThrows(NoSuchElementException.class, () -> iterator.detect((long each) -> false));
		}

		@ParameterizedTest
		@MethodSource("getEmptyIterables")
		default void testDetect_Empty(FunctionalPrimitiveIterable.OfLong iterable)
		{
			FunctionalPrimitiveIterator.OfLong iterator = iterable.iterator();
			assertThrows(NoSuchElementException.class, () -> iterator.detect((long each) -> true));
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testDetect_Singleton(FunctionalPrimitiveIterable.OfLong iterable)
		{
			// match first element
			long expected = iterable.iterator().nextLong();
			long actual = iterable.iterator().detect((long each) -> true);
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testDetect_Multiple(FunctionalPrimitiveIterable.OfLong iterable)
		{
			// match second element
			FunctionalPrimitiveIterator.OfLong temp = iterable.iterator();
			temp.nextLong();
			long expected = temp.nextLong();
			LongPredicate second = new LongPredicate() {
				boolean flag = true;
				@Override
				public boolean test(long l)
				{
					flag = !flag;
					return flag;
				}
			};
			long actual = iterable.iterator().detect(second);
			assertEquals(expected, actual);
		}

		@Test
		default void testDetect_Null()
		{
			FunctionalPrimitiveIterator.OfLong iterator = getAnyIterator();
			assertThrows(NullPointerException.class, () -> iterator.detect((LongPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMin_Empty(FunctionalPrimitiveIterable.OfLong iterable)
		{
			OptionalLong expected = OptionalLong.empty();
			OptionalLong actual = iterable.iterator().consume().min();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testMin_Singleton(FunctionalPrimitiveIterable.OfLong iterable)
		{
			OptionalLong expected = OptionalLong.of(iterable.iterator().next());
			OptionalLong actual = iterable.iterator().min();
			assertEquals(expected.getAsLong(), actual.getAsLong());
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testMin_Multiple(FunctionalPrimitiveIterable.OfLong iterable)
		{
			FunctionalPrimitiveIterator.OfLong iterator = iterable.iterator();
			long expected = iterator.next();
			while (iterator.hasNext()) {
				expected = Math.min(expected, iterator.nextLong());
			}
			OptionalLong actual = iterable.iterator().min();
			assertEquals(expected, actual.getAsLong());
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testMax_Empty(FunctionalPrimitiveIterable.OfLong iterable)
		{
			OptionalLong expected = OptionalLong.empty();
			OptionalLong actual = iterable.iterator().consume().max();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonIterables")
		default void testMax_Singleton(FunctionalPrimitiveIterable.OfLong iterable)
		{
			OptionalLong expected = OptionalLong.of(iterable.iterator().next());
			OptionalLong actual = iterable.iterator().max();
			assertEquals(expected.getAsLong(), actual.getAsLong());
		}

		@ParameterizedTest
		@MethodSource("getMultitonIterables")
		default void testMax_Multiple(FunctionalPrimitiveIterable.OfLong iterable)
		{
			FunctionalPrimitiveIterator.OfLong iterator = iterable.iterator();
			long expected = iterator.next();
			while (iterator.hasNext()) {
				expected = Math.max(expected, iterator.nextLong());
			}
			OptionalLong actual = iterable.iterator().max();
			assertEquals(expected, actual.getAsLong());
		}

		@ParameterizedTest
		@MethodSource("getIterables")
		default void testSum(FunctionalPrimitiveIterable.OfLong iterable)
		{
			long expected = 0L;
			for (FunctionalPrimitiveIterator.OfLong iterator = iterable.iterator(); iterator.hasNext();) {
				expected += iterator.nextLong();
			}
			long actual = iterable.iterator().sum();
			assertEquals(expected, actual);
		}
	}
}
