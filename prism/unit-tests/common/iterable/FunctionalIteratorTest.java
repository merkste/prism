package common.iterable;

import common.functions.DoubleObjToDoubleFunction;
import common.functions.IntObjToIntFunction;
import common.functions.LongObjToLongFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIteratorEquals;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.*;

interface FunctionalIteratorTest<E>
{
	default FunctionalIterator<E> getAnyIterator()
	{
		return getIterables().findAny().get().iterator();
	}

	default Stream<? extends FunctionalIterable<E>> getIterables()
	{
		return Stream.concat(Stream.concat(getEmptyIterables(), getSingletonIterables()), getMultitonIterables());
	}

	Stream<? extends FunctionalIterable<E>> getDuplicatesIterables();

	Stream<? extends FunctionalIterable<E>> getEmptyIterables();

	Stream<? extends FunctionalIterable<E>> getSingletonIterables();

	Stream<? extends FunctionalIterable<E>> getMultitonIterables();

	Iterable<? extends Object> getExcluded(FunctionalIterable<E> iterable);

	@ParameterizedTest
	@MethodSource("getIterables")
	@DisplayName("release() empties the iterator.")
	default void testRelease(FunctionalIterable<E> iterable)
	{
		FunctionalIterator<?> iterator = iterable.iterator();
		iterator.release();
		assertFalse(iterator.hasNext(), "Expected no next element after release()");
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testUnwrap(FunctionalIterable<E> iterable)
	{
		FunctionalIterator<?> iterator = iterable.iterator();
		assertIteratorEquals(iterable.iterator(), iterator.unwrap());
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testRequireNext(FunctionalIterable<E> iterable)
	{
		FunctionalIterator<E> iterator = iterable.iterator().consume();
		assertThrows(NoSuchElementException.class, iterator::requireNext);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testCollectArray(FunctionalIterable<E> iterable)
	{
		int n = (int) iterable.count();
		Object[] expected = new Object[n];
		int c = 0;
		for (E each: iterable) {
			expected[c++] = each;
		}
		Object[] actual = iterable.iterator().collect((E[]) new Object[n]);
		assertArrayEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testCollectArrayOffset(FunctionalIterable<E> iterable)
	{
		int offset = 2, tail = 3;
		int n = (int) iterable.count() + offset + tail;
		Object[] expected = new Object[n];
		int c = offset;
		for (E each: iterable) {
			expected[c++] = each;
		}
		Object[] actual = iterable.iterator().collect((E[]) new Object[n], offset);
		assertArrayEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testCollectCollection(FunctionalIterable<E> iterable)
	{
		List<E> expected = new ArrayList<>();
		for (E each: iterable) {
			expected.add(each);
		}
		List<E> actual = iterable.iterator().collect(new ArrayList<>());
		assertIteratorEquals(expected.iterator(), actual.iterator());
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testCollectSupplier(FunctionalIterable<E> iterable)
	{
		List<E> expected = new ArrayList<>();
		for (E each: iterable) {
			expected.add(each);
		}
		List<E> actual = iterable.iterator().collect((Supplier<? extends List<E>>) ArrayList::new);
		assertIteratorEquals(expected.iterator(), actual.iterator());
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testCollectAndCountArray(FunctionalIterable<E> iterable)
	{
		int n = (int) iterable.count();
		Object[] expected = new Object[n];
		int c = 0;
		for (E each: iterable) {
			expected[c++] = each;
		}
		Object[] actual = new Object[n];
		long count = iterable.iterator().collectAndCount((E[]) actual);
		assertEquals(n, count);
		assertArrayEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testCollectAndCountArrayOffset(FunctionalIterable<E> iterable)
	{
		int offset = 2, tail = 3, n = (int) iterable.count();
		int size = n + offset + tail;
		Object[] expected = new Object[size];
		int c = offset;
		for (E each: iterable) {
			expected[c++] = each;
		}
		Object[] actual = new Object[size];
		long count = iterable.iterator().collectAndCount((E[]) actual, offset);
		assertEquals(n, count);
		assertArrayEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testCollectAndCountCollection(FunctionalIterable<E> iterable)
	{
		List<E> expected = new ArrayList<>();
		for (E each: iterable) {
			expected.add(each);
		}
		List<E> actual = new ArrayList<>();
		long count = iterable.iterator().collectAndCount(actual);
		assertEquals(iterable.count(), count);
		assertIteratorEquals(expected.iterator(), actual.iterator());
	}

	@Test
	default void testCollect_Null()
	{
		FunctionalIterator<E> iterator = getAnyIterator();
		assertThrows(NullPointerException.class, () -> iterator.collect((E[]) null));
		assertThrows(NullPointerException.class, () -> iterator.collect((E[]) null, 0));
		assertThrows(NullPointerException.class, () -> iterator.collect((Collection<? super E>) null));
		assertThrows(NullPointerException.class, () -> iterator.collect((Supplier<? extends Collection<? super E>>) null));
		assertThrows(NullPointerException.class, () -> iterator.collectAndCount((E[]) null));
		assertThrows(NullPointerException.class, () -> iterator.collectAndCount((E[]) null, 0));
		assertThrows(NullPointerException.class, () -> iterator.collectAndCount((Collection<? super E>) null));
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testCollectDistinct(FunctionalIterable<E> iterable)
	{
		Set<E> expected = new HashSet<>();
		for (E each : iterable) {
			expected.add(each);
		}
		List<E> actual = iterable.iterator().collectDistinct().collect(new ArrayList<E>());
		assertTrue(expected.containsAll(actual), "actual =< expected");
		assertTrue(actual.containsAll(expected), "actual >= expected");
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testConsume(FunctionalIterable<E> iterable)
	{
		FunctionalIterator<E> iterator = iterable.iterator();
		assertFalse(iterator.consume().hasNext(), "Expected no next element after consume()");
	}

	@ParameterizedTest
	@MethodSource({"getIterables"})
	default void testConcatNone(FunctionalIterable<E> iterable)
	{
		FunctionalIterator<E> expected = iterable.iterator();
		FunctionalIterator<E> actual = expected.concat();
		assertSame(expected, actual);
	}

	@ParameterizedTest
	@MethodSource({"getIterables"})
	default void testConcatSingle(FunctionalIterable<E> iterable)
	{
		for (Iterator<? extends FunctionalIterable<E>> iter = getIterables().iterator(); iter.hasNext();) {
			FunctionalIterable<E> tail = iter.next();
			ArrayList<E> expected = iterable.collect(new ArrayList<E>());
			tail.collect(expected);
			FunctionalIterator<E> actual = iterable.iterator().concat(tail.iterator());
			assertIteratorEquals(expected.iterator(), actual);
		}
	}

	@ParameterizedTest
	@MethodSource({"getIterables"})
	default void testConcatMultiple(FunctionalIterable<E> iterable)
	{
		for (Iterator<? extends FunctionalIterable<E>> iter = getIterables().iterator(); iter.hasNext();) {
			FunctionalIterable<E> tail = iter.next();
			ArrayList<E> expected = iterable.collect(new ArrayList<E>());
			tail.collect(expected);
			tail.collect(expected);
			FunctionalIterator<E> actual = iterable.iterator().concat(tail.iterator(), tail.iterator());
			assertIteratorEquals(expected.iterator(), actual);
		}
	}

	@ParameterizedTest
	@MethodSource({"getIterables", "getDuplicatesIterables"})
	default void testDistinct(FunctionalIterable<E> iterable)
	{
		List<E> expected = new ArrayList<>();
		for (E each : iterable) {
			if (! expected.contains(each)) {
				expected.add(each);
			}
		}
		List<E> actual = iterable.iterator().distinct().collect(new ArrayList<E>());
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource({"getIterables", "getDuplicatesIterables"})
	default void testDedupe(FunctionalIterable<E> iterable)
	{
		List<E> expected = new ArrayList<>();
		for (E each : iterable) {
			if (expected.isEmpty()) {
				expected.add(each);
			} else {
				E last = expected.get(expected.size() - 1);
				if (! Objects.equals(last, each)) {
					expected.add(each);
				}
			}
		}
		List<Object> actual = iterable.iterator().dedupe().collect(new ArrayList<>());
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testFilter(FunctionalIterable<E> iterable)
	{
		ArrayList<E> expected = new ArrayList<>();
		int count = 0;
		for (E each : iterable) {
			if (count++ % 2 == 0) {
				expected.add(each);
			}
		}
		Range.RangeIterator index = new Range(0, (int) iterable.count()).iterator();
		FunctionalIterator<E> actual = iterable.iterator().filter(e -> index.nextInt() % 2 == 0);
		assertIteratorEquals(expected.iterator(), actual);
	}

	@Test
	default void testFilter_Null()
	{
		FunctionalIterator<E> iterator = getAnyIterator();
		assertThrows(NullPointerException.class, () -> iterator.filter(null));
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testFlatMap(FunctionalIterable<E> iterable)
	{
		FunctionalIterable<String> expected = iterable.map(String::valueOf);
		Iterator<String> actual = iterable.iterator().flatMap(e -> List.of(String.valueOf(e)).iterator());
		assertIteratorEquals(expected.iterator(), actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testFlatMapToDouble(FunctionalIterable<E> iterable)
	{
		Range range = new Range((int) iterable.count());
		PrimitiveIterator.OfDouble expected = unboxDouble(range.iterator().map((int i) -> (double) i));
		Range.RangeIterator index = range.iterator();
		PrimitiveIterator.OfDouble actual = iterable.iterator().flatMapToDouble(e -> new SingletonIterator.OfDouble(index.next()));
		assertIteratorEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testFlatMapToInt(FunctionalIterable<E> iterable)
	{
		Range range = new Range((int) iterable.count());
		PrimitiveIterator.OfInt expected = unboxInt(range.iterator());
		Range.RangeIterator index = range.iterator();
		PrimitiveIterator.OfInt actual = iterable.iterator().flatMapToInt(e -> new SingletonIterator.OfInt(index.next()));
		assertIteratorEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testFlatMapToLong(FunctionalIterable<E> iterable)
	{
		Range range = new Range((int) iterable.count());
		PrimitiveIterator.OfLong expected = unboxLong(range.iterator().map((int i) -> (long) i));
		Range.RangeIterator index = range.iterator();
		PrimitiveIterator.OfLong actual = iterable.iterator().flatMapToLong(e -> new SingletonIterator.OfLong(index.next()));
		assertIteratorEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getSingletonIterables")
	default void testFlatMapToNull(FunctionalIterable<E> iterable)
	{
		assertThrows(NullPointerException.class, () -> iterable.iterator().flatMap(e -> null).consume());
	}

	@Test
	default void testFlatMap_Null()
	{
		FunctionalIterator<E> iterator = getAnyIterator();
		assertThrows(NullPointerException.class, () -> iterator.flatMap(null));
		assertThrows(NullPointerException.class, () -> iterator.flatMapToDouble(null));
		assertThrows(NullPointerException.class, () -> iterator.flatMapToInt(null));
		assertThrows(NullPointerException.class, () -> iterator.flatMapToLong(null));
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testMap(FunctionalIterable<E> iterable)
	{
		Range expected = new Range((int) iterable.count());
		Range.RangeIterator index = expected.iterator();
		Iterator<Integer> actual = iterable.iterator().map(e -> index.next());
		assertIteratorEquals(expected.iterator(), actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testMapToDouble(FunctionalIterable<E> iterable)
	{
		Range range = new Range((int) iterable.count());
		PrimitiveIterator.OfDouble expected = unboxDouble(range.iterator().map((int i) -> (double) i));
		Range.RangeIterator index = range.iterator();
		PrimitiveIterator.OfDouble actual = iterable.iterator().mapToDouble(e -> index.next());
		assertIteratorEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testMapToInt(FunctionalIterable<E> iterable)
	{
		Range range = new Range((int) iterable.count());
		PrimitiveIterator.OfInt expected = unboxInt(range.iterator());
		Range.RangeIterator index = range.iterator();
		PrimitiveIterator.OfInt actual = iterable.iterator().mapToInt(e -> index.next());
		assertIteratorEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testMapToLong(FunctionalIterable<E> iterable)
	{
		Range range = new Range((int) iterable.count());
		PrimitiveIterator.OfLong expected = unboxLong(range.iterator().map((int i) -> (long) i));
		Range.RangeIterator index = range.iterator();
		PrimitiveIterator.OfLong actual = iterable.iterator().mapToLong(e -> index.next());
		assertIteratorEquals(expected, actual);
	}

	@Test
	default void testMap_Null()
	{
		FunctionalIterator<E> iterator = getAnyIterator();
		assertThrows(NullPointerException.class, () -> iterator.map(null));
		assertThrows(NullPointerException.class, () -> iterator.mapToDouble(null));
		assertThrows(NullPointerException.class, () -> iterator.mapToInt(null));
		assertThrows(NullPointerException.class, () -> iterator.mapToLong(null));
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	@DisplayName("forEachRemaining() yields same sequence as the underlying iterator.")
	default void testForEachRemaining(FunctionalIterable<E> iterable)
	{
		List<Object> actual = new ArrayList<>();
		FunctionalIterator<E> iterator = iterable.iterator();
		iterator.forEachRemaining((each) -> actual.add(each));
		assertIterableEquals(iterable, actual);
	}

	@Test
	default void testForEachRemaining_Null()
	{
		FunctionalIterator<E> iterator = getAnyIterator();
		assertThrows(NullPointerException.class, () -> iterator.forEachRemaining(null));
	}

	@ParameterizedTest
	@MethodSource("getEmptyIterables")
	default void testReduceBinaryOperatorOfE_Empty(FunctionalIterable<E> iterable)
	{
		BinaryOperator<E> nop = (res, each) -> (E) new Object();
		Optional<E> actual = iterable.iterator().reduce(nop);
		assertEquals(Optional.empty(), actual);
	}

	@ParameterizedTest
	@MethodSource("getSingletonIterables")
	default void testReduceBinaryOperatorOfE_Singleton(FunctionalIterable<E> iterable)
	{
		BinaryOperator<E> nop = (res, each) -> (E) new Object();;
		E expected = iterable.iterator().next();
		if (expected == null) {
			assertThrows(NullPointerException.class, () -> iterable.iterator().reduce(nop));
		} else {
			Optional<E> actual = iterable.iterator().reduce(nop);
			assertEquals(expected, iterable.reduce(nop).get());
		}
	}

	@ParameterizedTest
	@MethodSource("getMultitonIterables")
	default void testReduceBinaryOperatorOfE_Multiple(FunctionalIterable<E> iterable)
	{
		List<E> actual = new ArrayList<>();
		E probe = (E) new Object(); // "unique" value, exploit that E is Object at runtime
		BinaryOperator<E> collect = new BinaryOperator<>()
		{
			@Override
			public E apply(E res, E each)
			{
				if (actual.isEmpty()) {
					actual.add(res);
					actual.add(each);
					return (E) probe;
				} else {
					actual.add(each);
					return res;
				}
			}
		};
		Optional<E> result = iterable.iterator().reduce(collect);
		assertEquals(probe, result.get());
		assertIterableEquals(iterable, actual);
	}

	@ParameterizedTest
	@MethodSource("getMultitonIterables")
	default void testReduceBinary_ResultNull(FunctionalIterable<E> iterable)
	{
		FunctionalIterator<E> iterator = iterable.iterator();
		assertThrows(NullPointerException.class, () -> iterator.reduce((res, each) -> null));
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testReduce(FunctionalIterable<E> iterable)
	{
		BiFunction<List<E>, E, List<E>> collect = (seq, each) -> {seq.add(each); return seq;};
		List<E> actual = iterable.iterator().reduce(new ArrayList<>(), collect);
		assertIterableEquals(iterable, actual);
		assertDoesNotThrow(() -> iterable.iterator().reduce(null, (obj, each) -> null));
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testReduceDouble(FunctionalIterable<E> iterable)
	{
		List<E> actual = new ArrayList<>();
		DoubleObjToDoubleFunction<E> collect = (res, each) -> {actual.add(each); return res;};
		FunctionalIterator<E> iterator = iterable.iterator();
		double result = iterator.reduce(Double.MIN_VALUE, collect);
		assertEquals(Double.MIN_VALUE, result);
		assertIterableEquals(iterable, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testReduceInt(FunctionalIterable<E> iterable)
	{
		List<E> actual = new ArrayList<>();
		IntObjToIntFunction<E> collect = (res, each) -> {actual.add(each); return res;};
		FunctionalIterator<E> iterator = iterable.iterator();
		int result = iterator.reduce(Integer.MIN_VALUE, collect);
		assertEquals(Integer.MIN_VALUE, result);
		assertIterableEquals(iterable, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testReduceLong(FunctionalIterable<E> iterable)
	{
		List<E> actual = new ArrayList<>();
		LongObjToLongFunction<E> collect = (res, each) -> {actual.add(each); return res;};
		FunctionalIterator<E> iterator = iterable.iterator();
		long result = iterator.reduce(Long.MIN_VALUE, collect);
		assertEquals(Long.MIN_VALUE, result);
		assertIterableEquals(iterable, actual);
	}

	@Test
	default void testReduce_Null()
	{
		FunctionalIterator<E> iterator = getAnyIterator();
		assertThrows(NullPointerException.class, () -> iterator.reduce(null));
		assertThrows(NullPointerException.class, () -> iterator.reduce(new Object(),null));
		assertThrows(NullPointerException.class, () -> iterator.reduce(0.0, (DoubleObjToDoubleFunction<? super E>) null));
		assertThrows(NullPointerException.class, () -> iterator.reduce(0, (IntObjToIntFunction<? super E>) null));
		assertThrows(NullPointerException.class, () -> iterator.reduce(0L, (LongObjToLongFunction<? super E>) null));
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testNonNull(FunctionalIterable<E> iterable)
	{
		List<Object> nonNull = new ArrayList<>();
		FunctionalIterator<E> iterator = iterable.iterator();
		iterator.forEachRemaining((each) -> {if (each != null) nonNull.add(each);});
		assertIteratorEquals(nonNull.iterator(), iterable.iterator().nonNull());
	}

	@ParameterizedTest
	@MethodSource({"getSingletonIterables", "getMultitonIterables"})
	default void testAllMatch(FunctionalIterable<E> iterable)
	{
		// match all elements
		assertTrue(iterable.iterator().allMatch(each -> true), "Expected allMatch() == true");
		// match not all elements
		Predicate<E> matchNotAll = new Predicate<E>() {
			// match: no element if singleton, otherwise every odd element
			boolean flag = iterable.iterator().count() == 1;
			@Override
			public boolean test(E t)
			{
				flag = !flag;
				return flag;
			};
		};
		assertFalse(iterable.iterator().allMatch(matchNotAll), "Expected allMatch() == false");
	}

	@ParameterizedTest
	@MethodSource("getEmptyIterables")
	default void testAllMatch_Empty(FunctionalIterable<E> iterable)
	{
		FunctionalIterator<E> iterator = iterable.iterator();
		assertTrue(iterator.allMatch(each -> false), "Exepted allMatch() == true if iterator is empty");
	}

	@Test
	default void testAllMatch_Null()
	{
		FunctionalIterator<E> iterator = getAnyIterator();
		assertThrows(NullPointerException.class, () -> iterator.allMatch(null));
	}

	@ParameterizedTest
	@MethodSource({"getSingletonIterables", "getMultitonIterables"})
	default void testAnyMatch(FunctionalIterable<E> iterable)
	{
		// match no element
		assertFalse(iterable.iterator().anyMatch(each -> false), "Expected anyMatch() == false");
		// match some elements
		Predicate<E> matchSome = new Predicate<E>() {
			// match: first element if singleton, otherwise every even element
			boolean flag = iterable.iterator().count() > 1;
			@Override
			public boolean test(E t)
			{
				flag = !flag;
				return flag;
			};
		};
		assertTrue(iterable.iterator().anyMatch(matchSome), "Expected anyMatch() == true");
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testAnyMatch_Empty(FunctionalIterable<E> iterable)
	{
		FunctionalIterator<E> iterator = iterable.iterator();
		assertTrue(iterator.allMatch(each -> true), "Exepted anyMatch() == false if iterator is empty");
	}

	@Test
	default void testAnyMatch_Null()
	{
		FunctionalIterator<E> iterator = getAnyIterator();
		assertThrows(NullPointerException.class, () -> iterator.anyMatch(null));
	}

	@ParameterizedTest
	@MethodSource({"getSingletonIterables", "getMultitonIterables"})
	default void testNoneMatch(FunctionalIterable<E> iterable)
	{
		// match no element
		assertTrue(iterable.iterator().noneMatch(each -> false), "Expected noneMatch() == true");
		// match some elements
		Predicate<E> matchSome = new Predicate<E>() {
			// match: first element if singleton, otherwise every even element
			boolean flag = iterable.iterator().count() > 1;
			@Override
			public boolean test(E t)
			{
				flag = !flag;
				return flag;
			};
		};
		assertFalse(iterable.iterator().noneMatch(matchSome), "Expected noneMatch() == false");
	}

	@ParameterizedTest
	@MethodSource("getEmptyIterables")
	default void testNoneMatch_Empty(FunctionalIterable<E> iterable)
	{
		FunctionalIterator<E> iterator = iterable.iterator();
		assertTrue(iterator.allMatch(each -> false), "Exepted noneMatch() == false if iterator is empty");
	}

	@Test
	default void testNoneMatch_Null()
	{
		FunctionalIterator<E> iterator = getAnyIterator();
		assertThrows(NullPointerException.class, () -> iterator.noneMatch(null));
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testAsString(FunctionalIterable<E> iterable)
	{
		String string = "[";
		FunctionalIterator<E> iterator = iterable.iterator();
		while (iterator.hasNext()) {
			string += iterator.next();
			if (iterator.hasNext()) {
				string += ", ";
			}
		}
		string += "]";
		assertEquals(string, iterable.iterator().asString());
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testContains(FunctionalIterable<E> iterable)
	{
		for (E each : iterable) {
			assertTrue(iterable.iterator().contains(each), "Expected contains(" + each + ") == true");
		}
		for (Object each : getExcluded(iterable)) {
			assertFalse(iterable.iterator().contains(each), "Expected contains(" + each + ") == false");
		}
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testCount(FunctionalIterable<E> iterable)
	{
		long expected = 0L;
		for (FunctionalIterator<E> iterator = iterable.iterator(); iterator.hasNext();) {
			iterator.next();
			expected++;
		}
		long actual = iterable.iterator().count();
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	default void testCountPredicate(FunctionalIterable<E> iterable)
	{
		long expected =0L;
		for (FunctionalIterator<E> iterator = iterable.iterator(); iterator.hasNext();) {
			iterator.next();
			expected++;
			if (iterator.hasNext()) {
				iterator.next();
			}
		}
		Predicate<E> odd = new Predicate<E>() {
			boolean flag = false;
			@Override
			public boolean test(E t)
			{
				flag = !flag;
				return flag;
			};
		};
		long actual = iterable.iterator().count(odd);
		assertEquals(expected, actual);
	}

	@Test
	default void testCountPredicate_Null()
	{
		FunctionalIterator<E> iterator = getAnyIterator();
		assertThrows(NullPointerException.class, () -> iterator.count(null));
	}


	@ParameterizedTest
	@MethodSource("getIterables")
	default void testDetect(FunctionalIterable<E> iterable)
	{
		FunctionalIterator<E> iterator = iterable.iterator();
		assertThrows(NoSuchElementException.class, () -> iterator.detect(each -> false));
	}

	@ParameterizedTest
	@MethodSource("getEmptyIterables")
	default void testDetect_Empty(FunctionalIterable<E> iterable)
	{
		FunctionalIterator<E> iterator = iterable.iterator();
		assertThrows(NoSuchElementException.class, () -> iterator.detect(each -> true));
	}

	@ParameterizedTest
	@MethodSource("getSingletonIterables")
	default void testDetect_Singleton(FunctionalIterable<E> iterable)
	{
		// match first element
		E expected = iterable.iterator().next();
		E actual = iterable.iterator().detect(each -> true);
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getMultitonIterables")
	default void testDetect_Multiple(FunctionalIterable<E> iterable)
	{
		// match second element
		FunctionalIterator<E> temp = iterable.iterator();
		temp.next();
		E expected = temp.next();
		Predicate<E> second = new Predicate<E>() {
			boolean flag = true;
			@Override
			public boolean test(E each)
			{
				flag = !flag;
				return flag;
			}
		};
		E actual = iterable.iterator().detect(second);
		assertEquals(expected, actual);
	}

	@Test
	default void testDetect_Null()
	{
		FunctionalIterator<E> iterator = getAnyIterator();
		assertThrows(NullPointerException.class, () -> iterator.detect(null));
	}



	interface Of<E> extends FunctionalIteratorTest<E>
	{
		@Override
		default Iterable<? extends Object> getExcluded(FunctionalIterable<E> iterable)
		{
			return getExclusionListOfObject();
		}
	}



	// Test-data sets

	default Stream<Object[]> getArraysOfObject()
	{
		return Stream.concat(Stream.concat(getEmptyArraysOfObject(), getSingletonArraysOfObject()), getMultitonArraysOfObject());
	}

	/* Workaround to pass Object[] as argument */
	default Stream<Arguments> getArraysAsArguments()
	{
		return getArraysOfObject().map(array -> Arguments.of((Object) array));
	}

	default Stream<Object[]> getDuplicatesArraysOfObject()
	{
		return Stream.of(
				new Object[] {null, null,
						"first", "first",
						"second", "second"},
				new Object[] {null, null,
						"first", "first",
						"third", "third",
						"first", "first",
						null, null});
	}

	/* Workaround to pass Object[] as argument */
	default Stream<Arguments> getDuplicatesArraysAsArguments()
	{
		return getDuplicatesArraysOfObject().map(array -> Arguments.of((Object) array));
	}

	default Stream<Object[]> getEmptyArraysOfObject()
	{
		return Stream.<Object[]>of(new Object[] {});
	}


	/* Workaround to pass Object[] as argument */
	default Stream<Arguments> getEmptyArraysAsArguments()
	{
		return getEmptyArraysOfObject().map(array -> Arguments.of((Object) array));
	}

	default Stream<Object[]> getSingletonArraysOfObject()
	{
		return Stream.of(new Object[] {"first"},
				new Object[] {null});
	}

	/* Workaround to pass Object[] as argument */
	default Stream<Arguments> getSingletonArraysAsArguments()
	{
		return getSingletonArraysOfObject().map(array -> Arguments.of((Object) array));
	}

	default Stream<Object[]> getMultitonArraysOfObject()
	{
		return Stream.of(new Object[] {"first", "second", "third"},
				new Object[] {null, "first", null, "second", null, "third", null});
	}

	/* Workaround to pass Object[] as argument */
	default Stream<Arguments> getMultitonArraysAsArguments()
	{
		return getMultitonArraysOfObject().map(array -> Arguments.of((Object) array));
	}

	default Iterable<Object> getExclusionListOfObject()
	{
		return List.of(new Object(), new Object(), new Object());
	}

	default Stream<double[]> getArraysOfDouble()
	{
		return Stream.concat(Stream.concat(getEmptyArraysOfDouble(), getSingletonArraysOfDouble()), getMultitonArraysOfDouble());
	}

	default Stream<double[]> getDuplicatesArraysOfDouble()
	{
		return Stream.of(new double[] {-3.5, -3.5,
						-2.0, -2.0,
						-1.0, -1.0,
						-0.0, +0.0,
						+1.0, +1.0,
						+2.0, +2.0,
						+3.5, +3.5},
				new double[] {Double.NaN, Double.NaN,
						Double.MIN_VALUE, Double.MIN_VALUE,
						Double.MIN_NORMAL, Double.MIN_NORMAL,
						Double.MAX_VALUE, Double.MAX_VALUE,
						Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
						Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
						Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
						Double.MAX_VALUE, Double.MAX_VALUE,
						Double.MIN_NORMAL, Double.MIN_NORMAL,
						Double.MIN_VALUE, Double.MIN_VALUE,
						Double.NaN, Double.NaN});
	}

	default Stream<double[]> getEmptyArraysOfDouble()
	{
		return Stream.of(new double[] {});
	}

	default Stream<double[]> getSingletonArraysOfDouble()
	{
		return Stream.of(new double[] {1.0});
	}

	default Stream<double[]> getMultitonArraysOfDouble()
	{
		return Stream.of(new double[] {-3.5, -2.0, -1.0, 0.0, 1.0, 2.0, 3.5},
				new double[] {Double.NaN,
						Double.MIN_VALUE,
						Double.MIN_NORMAL,
						Double.MAX_VALUE,
						Double.NEGATIVE_INFINITY,
						Double.POSITIVE_INFINITY,});
	}

	default List<Double> getExclusionListOfDouble()
	{
		List<Double> excluded = new ArrayList<>();
		excluded.add(Double.NaN);
		excluded.add(Double.NEGATIVE_INFINITY);
		excluded.add(-100000000.0);
		excluded.add(-10000.0);
		excluded.add(-100.0);
		excluded.add(-10.0);
		excluded.add(-2.0);
		excluded.add(-1.0);
		excluded.add(-0.0);
		excluded.add(+0.0);
		excluded.add(Double.MIN_VALUE);
		excluded.add(Double.MIN_NORMAL);
		excluded.add(1.0);
		excluded.add(2.0);
		excluded.add(10.0);
		excluded.add(100.0);
		excluded.add(10000.0);
		excluded.add(100000000.0);
		excluded.add(Double.MAX_VALUE);
		excluded.add(Double.POSITIVE_INFINITY);
		return excluded;
	}

	default Stream<int[]> getArraysOfInt()
	{
		return Stream.concat(Stream.concat(getEmptyArraysOfInt(), getSingletonArraysOfInt()), getMultitonArraysOfInt());
	}

	default Stream<int[]> getDuplicatesArraysOfInt()
	{
		return Stream.of(new int[] {-3, -3,
						-1, -1,
						-2, -2,
						-0, +0,
						+1, +1,
						+2, +2,
						+3, +3},
				new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE,
						Integer.MAX_VALUE, Integer.MAX_VALUE,
						Integer.MIN_VALUE, Integer.MIN_VALUE});
	}

	default Stream<int[]> getEmptyArraysOfInt()
	{
		return Stream.of(new int[] {});
	}

	default Stream<int[]> getSingletonArraysOfInt()
	{
		return Stream.of(new int[] {1});
	}

	default Stream<int[]> getMultitonArraysOfInt()
	{
		return Stream.of(new int[] {-3, -2, -1, 0, 1, 2, 3},
				new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE});
	}

	default List<Integer> getExclusionListOfInt()
	{
		List<Integer> excluded = new ArrayList<>();
		excluded.add(Integer.MIN_VALUE);
		excluded.add(-100000000);
		excluded.add(-10000);
		excluded.add(-100);
		excluded.add(-10);
		excluded.add(-2);
		excluded.add(-1);
		excluded.add(0);
		excluded.add(1);
		excluded.add(2);
		excluded.add(10);
		excluded.add(100);
		excluded.add(10000);
		excluded.add(100000000);
		excluded.add(Integer.MAX_VALUE);
		return excluded;
	}

	default Stream<long[]> getArraysOfLong()
	{
		return Stream.concat(Stream.concat(getEmptyArraysOfLong(), getSingletonArraysOfLong()), getMultitonArraysOfLong());
	}

	default Stream<long []> getDuplicatesArraysOfLong()
	{
		return Stream.of(new long [] {-3L, -3L,
						-2L, -2L,
						-1L, -1L,
						-0L, +0L,
						+1L, +1L,
						+2L, +2L,
						+3L, +3L},
				new long[] {Long.MIN_VALUE, Long.MIN_VALUE,
						Long.MAX_VALUE, Long.MAX_VALUE,
						Long.MIN_VALUE, Long.MIN_VALUE});
	}

	default Stream<long []> getEmptyArraysOfLong()
	{
		return Stream.of(new long [] {});
	}

	default Stream<long []> getSingletonArraysOfLong()
	{
		return Stream.of(new long [] {1L});
	}

	default Stream<long []> getMultitonArraysOfLong()
	{
		return Stream.of(new long [] {-3L, -2L, -1L, 0L, 1L, 2L, 3L},
				new long[] {Long.MIN_VALUE, Long.MAX_VALUE});
	}

	default List<Long> getExclusionListOfLong()
	{
		List<Long> excluded = new ArrayList<>();
		excluded.add(Long.MIN_VALUE);
		excluded.add(-100000000L);
		excluded.add(-10000L);
		excluded.add(-100L);
		excluded.add(-10L);
		excluded.add(-2L);
		excluded.add(-1L);
		excluded.add(0L);
		excluded.add(1L);
		excluded.add(2L);
		excluded.add(10L);
		excluded.add(100L);
		excluded.add(10000L);
		excluded.add(100000000L);
		excluded.add(Long.MAX_VALUE);
		return excluded;
	}
}
