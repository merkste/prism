package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;

import java.util.stream.Stream;

interface SingletonIteratorTest<E> extends FunctionalIteratorTest<E>
{
	@Override
	default void testAllMatch_Empty(FunctionalIterable<E> iterable)
	{ /* singleton iterators hold exactly one value */ }

	@Override
	default void testDetect_Empty(FunctionalIterable<E> iterable)
	{ /* singleton iterators hold exactly one value */ }

	@Override
	default void testDetect_Multiple(FunctionalIterable<E> iterable)
	{ /* singleton iterators hold exactly one value */ }

	@Override
	default void testNoneMatch_Empty(FunctionalIterable<E> iterable)
	{ /* singleton iterators hold exactly one value */ }

	@Override
	default void testReduceBinary_ResultNull(FunctionalIterable<E> iterable)
	{ /* singleton iterators hold exactly one value */}

	@Override
	default void testReduceBinaryOperatorOfE_Empty(FunctionalIterable<E> iterable)
	{ /* singleton iterators hold exactly one value */ }

	@Override
	default void testReduceBinaryOperatorOfE_Multiple(FunctionalIterable<E> iterable)
	{ /* singleton iterators hold exactly one value */ }



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements SingletonIteratorTest<Object>, FunctionalIteratorTest.Of<Object>
	{
		@Override
		public Stream<SingletonIterable.Of<Object>> getDuplicatesIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<SingletonIterable.Of<Object>> getEmptyIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<SingletonIterable.Of<Object>> getSingletonIterables()
		{
			return getSingletonArraysOfObject().map(objects -> new SingletonIterable.Of(objects[0]));
		}

		@Override
		public Stream<SingletonIterable.Of<Object>> getMultitonIterables()
		{
			return Stream.empty();
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements SingletonIteratorTest<Double>, FunctionalPrimitiveIteratorTest.OfDouble
	{
		@Override
		public Stream<SingletonIterable.OfDouble> getDuplicatesIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<SingletonIterable.OfDouble> getEmptyIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<SingletonIterable.OfDouble> getSingletonIterables()
		{
			return getSingletonArraysOfDouble().map(nums -> new SingletonIterable.OfDouble(nums[0]));
		}

		@Override
		public Stream<SingletonIterable.OfDouble> getMultitonIterables()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatch_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testDetect_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testDetect_Multiple(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testMax_Multiple(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testMin_Multiple(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testNoneMatch_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testSum_Empty(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* singleton iterators hold exactly one value */ }
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements SingletonIteratorTest<Integer>, FunctionalPrimitiveIteratorTest.OfInt
	{
		@Override
		public Stream<SingletonIterable.OfInt> getDuplicatesIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<SingletonIterable.OfInt> getEmptyIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<SingletonIterable.OfInt> getSingletonIterables()
		{
			return getSingletonArraysOfInt().map(nums -> new SingletonIterable.OfInt(nums[0]));
		}

		@Override
		public Stream<SingletonIterable.OfInt> getMultitonIterables()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatch_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testDetect_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testDetect_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testMax_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testMin_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testNoneMatch_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testReduceIntBinaryOperator_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testReduceIntBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testReduceLongBinaryOperator_Empty(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testReduceLongBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* singleton iterators hold exactly one value */ }
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements SingletonIteratorTest<Long>, FunctionalPrimitiveIteratorTest.OfLong
	{
		@Override
		public Stream<SingletonIterable.OfLong> getDuplicatesIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<SingletonIterable.OfLong> getEmptyIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<SingletonIterable.OfLong> getSingletonIterables()
		{
			return getSingletonArraysOfLong().map(nums -> new SingletonIterable.OfLong(nums[0]));
		}

		@Override
		public Stream<SingletonIterable.OfLong> getMultitonIterables()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatch_Empty(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testDetect_Empty(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testDetect_Multiple(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testMax_Multiple(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testMin_Multiple(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testNoneMatch_Empty(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testReduceLongBinaryOperator_Empty(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* singleton iterators hold exactly one value */ }

		@Override
		public void testReduceLongBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* singleton iterators hold exactly one value */ }
	}
}
