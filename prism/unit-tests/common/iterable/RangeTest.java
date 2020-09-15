package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RangeTest
{
	public static void assertEqualsClosedForLoop(int start, int stop, int step, Range actual)
	{
		assertEqualsClosedForLoop(start, stop, step, actual.iterator());
	}

	public static void assertEqualsOpenForLoop(int start, int stop, int step, Range actual)
	{
		assertEqualsOpenForLoop(start, stop, step, actual.iterator());
	}

	public static void assertEqualsClosedForLoop(int start, int stop, int step, PrimitiveIterator.OfInt actual)
	{
		if (step > 0) {
			// closed ascending loop: <=
			for (int i = start; i <= stop; i = Math.addExact(i, step)) {
				assertEquals(i, actual.nextInt());
			}
		} else if (step < 0) {
			// open descending loop: >=
			for (int i = start; i >= stop; i = Math.addExact(i, step)) {
				assertEquals(i, actual.nextInt());
			}
		} else {
			fail("expected step != 0");
		}
		assertFalse(actual.hasNext(), "Expected exhausted iterator");
	}

	public static void assertEqualsOpenForLoop(int start, int stop, int step, PrimitiveIterator.OfInt actual)
	{
		if (step > 0) {
			// open ascending loop: <
			for (int i = start; i < stop; i = Math.addExact(i, step)) {
				assertEquals(i, actual.nextInt());
			}
		} else if (step < 0) {
			// open descending loop: >
			for (int i = start; i > stop; i = Math.addExact(i, step)) {
				assertEquals(i, actual.nextInt());
			}
		} else {
			fail("expected step != 0");
		}
		assertFalse(actual.hasNext(), "Expected exhausted iterator");
	}

	/**
	 * Test method for {@link common.iterable.Range#closed(int)}.
	 */
	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments", "getSingletonRangeArguments", "getMultitonRangeArguments"})
	void testClosedInt(int start, int stop, int step)
	{
		if (step > 0 && start == 0) {
			// range with positive step width starting at 0
			// adjust stop to maybe be included
			int closed = (step > 0) ? stop - 1 : stop + 1;
			Range actual = Range.closed(closed);
			assertEqualsClosedForLoop(0, closed, 1, actual);
		}
	}

	/**
	 * Test method for {@link common.iterable.Range#closed(int, int)}.
	 */
	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments", "getSingletonRangeArguments", "getMultitonRangeArguments"})
	void testClosedIntInt(int start, int stop, int step)
	{
		if (step > 0) {
			// range with positive step width
			// adjust stop to maybe be included
			int closed = (step > 0) ? stop - 1 : stop + 1;
			Range actual = Range.closed(start, closed);
			assertEqualsClosedForLoop(start, closed, 1, actual);
		}
	}

	/**
	 * Test method for {@link common.iterable.Range#closed(int, int, int)}.
	 */
	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments", "getSingletonRangeArguments", "getMultitonRangeArguments"})
	void testClosedIntIntInt(int start, int stop, int step)
	{
		// adjust stop to maybe be included
		int closed = (step > 0) ? stop - 1 : stop + 1;
		Range actual = Range.closed(start, closed, step);
		assertEqualsClosedForLoop(start, closed, step, actual);
	}

	/**
	 * Test method for {@link common.iterable.Range#Range(int)}.
	 */
	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments", "getSingletonRangeArguments", "getMultitonRangeArguments"})
	void testRangeInt(int start, int stop, int step)
	{
		if (step > 0 && start == 0) {
			// range with positive step width starting at 0
			Range actual = new Range(stop);
			assertEqualsOpenForLoop(0, stop, 1, actual);
		}
	}

	/**
	 * Test method for {@link common.iterable.Range#Range(int, int)}.
	 */
	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments", "getSingletonRangeArguments", "getMultitonRangeArguments"})
	void testRangeIntInt(int start, int stop, int step)
	{
		if (step > 0) {
			// range with positive step width
			Range actual = new Range(start, stop);
			assertEqualsOpenForLoop(start, stop, 1, actual);
		}
	}

	/**
	 * Test method for {@link common.iterable.Range#Range(int, int, int, boolean)}.
	 */
	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments", "getSingletonRangeArguments", "getMultitonRangeArguments"})
	void testRangeIntIntInt(int start, int stop, int step)
	{
		Range actual = new Range(start, stop, step);
		assertEqualsOpenForLoop(start, stop, step, actual);
	}

	static Stream<Arguments> getAllRangeArguments()
	{
		return Stream.concat(Stream.concat(getEmptyRangeArguments(), getSingletonRangeArguments()), getMultitonRangeArguments());
	}

	static Stream<Arguments> getEmptyRangeArguments()
	{
		return Stream.of(Arguments.of(0, 0, 1),
		                 Arguments.of(0, 0, -1),
		                 Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE, 1),
		                 Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE, -1));
	}

	static Stream<Arguments> getSingletonRangeArguments()
	{
		return Stream.of(Arguments.of(0, 1, 1),
		                 Arguments.of(0, -1, -1),
		                 Arguments.of(Integer.MAX_VALUE-1, Integer.MAX_VALUE, 1),
		                 Arguments.of(Integer.MIN_VALUE+1, Integer.MIN_VALUE, -1));
	}

	static Stream<Arguments> getMultitonRangeArguments()
	{
		return Stream.of(Arguments.of(0, 10, 1),
		                 Arguments.of(0, -10, -1),
		                 Arguments.of(-10, 10, 3),
		                 Arguments.of(10, -10, -3),
		                 Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE+20, 7),
		                 Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE-20, -7));
	}

	@ParameterizedTest
	@MethodSource("getAllRangeArguments")
	void testRangeStepZero(int start, int stop)
	{
		assertThrows(IllegalArgumentException.class, () -> new Range(start, stop, 0));
	}


	@ParameterizedTest
	@MethodSource("getAllRangeArguments")
	void testIsAscending(int start, int stop, int step)
	{
		Range actual = new Range(start, stop, step);
		if (actual.isEmpty() || actual.isSingleton()) {
			assertTrue(actual.isAscending());
		} else if (step > 0) {
			assertTrue(actual.isAscending());
		} else if(step < 0) {
			assertFalse(actual.isAscending());
		} else {
			fail("expected step != 0");
		}
	}

	@ParameterizedTest
	@MethodSource("getAllRangeArguments")
	void testIsSingleton(int start, int stop, int step)
	{
		Range actual = new Range(start, stop, step);
		if (actual.count() == 1) {
			assertTrue(actual.isSingleton());
		} else {
			assertFalse(actual.isSingleton());
		}
	}

	@ParameterizedTest
	@MethodSource("getAllRangeArguments")
	void testReversed(int start, int stop, int step)
	{
		Range range = new Range(start, stop, step);
		if (range.first == Integer.MIN_VALUE && !range.isEmpty()) {
			// reverse() at min value throws
			assertThrows(ArithmeticException.class, range::reversed);
		} else if (range.first == Integer.MAX_VALUE && !range.isEmpty()) {
			// reverse() at min value throws
			assertThrows(ArithmeticException.class, range::reversed);
		} else {
			assertEqualsClosedForLoop(range.last, range.first, -range.step, range.reversed());
		}
	}

	@ParameterizedTest
	@MethodSource("getAllRangeArguments")
	void testIsEmpty(int start, int stop, int step)
	{
		Range actual = new Range(start, stop, step);
		if (actual.count() == 0) {
			assertTrue(actual.isEmpty());
		} else {
			assertFalse(actual.isEmpty());
		}
	}

	/**
	 * Test method for {@link common.iterable.Range#toString()}.
	 */
	@ParameterizedTest
	@MethodSource("getAllRangeArguments")
	void testToString(int start, int stop, int step)
	{
		Range range = new Range(start, stop, step);
		String expected = "Range.closed(" + range.first + ", " + range.last + ", " + range.step + ")";
		String actual = new Range(start, stop, step).toString();
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getAllRangeArguments")
	void testEqualsAndHash()
	{
		Range range = Range.closed(-2, 4, 3); // {-2, 1, 4}

		// equal to itself
		assertEquals(range, range);

		// equal to a clone
		Range clone = Range.closed(-2, 4, 3);
		assertEquals(range, clone);
		assertEquals(range.hashCode(), clone.hashCode());

		// not equal to null or other type
		assertFalse(range.equals(null));
		assertFalse(range.equals("no"));

		// not equal to an arbitrary range
		Range otherStart = Range.closed(-5, 4, 3); // {-5, -2, 1, 4}
		assertNotEquals(otherStart, range);
		Range otherStop = Range.closed(-2, 7, 3); // {-2, 1, 4, 7}
		assertNotEquals(otherStop, range);
		Range otherStep = Range.closed(-2, 4, 2); // {-2, 0, 2, 4}
		assertNotEquals(otherStep, range);
	}

	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments"})
	void testEqualsAndHashEmpty(int start, int stop, int step)
	{
		Range expected = new Range(0);
		Range actual = new Range(start, stop, step);
		assertEquals(expected, actual);
		assertEquals(expected.hashCode(), actual.hashCode());
	}

	@ParameterizedTest
	@MethodSource({"getSingletonRangeArguments"})
	void testEqualsAndHashSingleton(int start, int stop, int step)
	{
		Range expected = new Range(start, stop, step > 0 ? 1 : -1);
		Range actual = new Range(start, stop, step);
		assertEquals(expected, actual);
		assertEquals(expected.hashCode(), actual.hashCode());
	}



	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class RangeIteratorTest implements FunctionalPrimitiveIteratorTest.OfInt
	{
		public Range asRange(Arguments args)
		{
			Object[] params = args.get();
			return new Range((Integer) params[0], (Integer) params[1], (Integer) params[2]);
		}

		@Override
		public Stream<Range> getDuplicatesIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Range> getEmptyIterables()
		{
			return getEmptyRangeArguments().map(this::asRange);
		}

		@Override
		public Stream<Range> getSingletonIterables()
		{
			return getSingletonRangeArguments().map(this::asRange);
		}

		@Override
		public Stream<Range> getMultitonIterables()
		{
			return getMultitonRangeArguments().map(this::asRange);
		}

		@Override
		public PrimitiveIterable.OfInt getExcluded(FunctionalIterable<Integer> iterable)
		{
			if (!(iterable instanceof Range)) {
				return FunctionalPrimitiveIteratorTest.OfInt.super.getExcluded(iterable);
			}
			Range range = (Range) iterable;
			List<Integer> excluded = new ArrayList<>();
			// add lower and upper bounds
			range.min().ifPresent(min -> {if (min > Integer.MIN_VALUE) {excluded.add(min - 1); excluded.add(Integer.MIN_VALUE);}});
			range.max().ifPresent(max -> {if (max < Integer.MAX_VALUE) {excluded.add(max + 1); excluded.add(Integer.MAX_VALUE);}});
			// add ints between first and last that are no steps
			if (range.isAscending()) {
				for (int i = range.first; i <= range.last; i++) {
					if ((i - range.first) % range.step != 0) {
						excluded.add((Integer) i);
					}
				}
			} else {
				for (int i = range.first; i >= range.last; i--) {
					if ((i - range.first) % range.step != 0) {
						excluded.add((Integer) i);
					}
				}
			}
			return PrimitiveIterable.unboxInt(excluded);
		}
	}
}
