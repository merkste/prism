package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;

import java.util.stream.Stream;

interface EmptyIteratorTest<E> extends FunctionalIteratorTest<E>
{
	@Override
	default void testAllMatch(FunctionalIterable<E> iterable)
	{ /* empty iterators hold no value */ }

	@Override
	default void testAnyMatch(FunctionalIterable<E> iterable)
	{ /* empty iterators hold no value */ }

	@Override
	default void testDetect_Multiple(FunctionalIterable<E> iterable)
	{ /* empty iterators hold no value */ }

	@Override
	default void testDetect_Singleton(FunctionalIterable<E> iterable)
	{ /* empty iterators hold no value */ }

	@Override
	default void testFlatMapToNull(FunctionalIterable<E> iterable)
	{ /* empty iterators hold no value */ }

	@Override
	default void testNoneMatch(FunctionalIterable<E> iterable)
	{ /* empty iterators hold no value */ }

	@Override
	default void testReduceBinaryOperatorOfE_Singleton(FunctionalIterable<E> iterable)
	{ /* empty iterators hold no value */ }

	@Override
	default void testReduceBinaryOperatorOfE_Multiple(FunctionalIterable<E> iterable)
	{ /* empty iterators hold no value */ }

	@Override
	default void testReduceBinary_ResultNull(FunctionalIterable<E> iterable)
	{ /* empty iterators hold no value */ }



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements EmptyIteratorTest<Object>, FunctionalIteratorTest.Of<Object>
	{
		@Override
		public Stream<EmptyIterable<Object>> getDuplicatesIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<EmptyIterable<Object>> getEmptyIterables()
		{
			return Stream.of(EmptyIterable.Of());
		}

		@Override
		public Stream<EmptyIterable<Object>> getSingletonIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<EmptyIterable<Object>> getMultitonIterables()
		{
			return Stream.empty();
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements EmptyIteratorTest<Double>, FunctionalPrimitiveIteratorTest.OfDouble
	{
		@Override
		public Stream<EmptyIterable.OfDouble> getDuplicatesIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<EmptyIterable.OfDouble> getEmptyIterables()
		{
			return Stream.of(EmptyIterable.OfDouble());
		}

		@Override
		public Stream<EmptyIterable.OfDouble> getSingletonIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<EmptyIterable.OfDouble> getMultitonIterables()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatch(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testAnyMatch(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testDetect_Multiple(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testDetect_Singleton(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testFlatMapToNull(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testMax_Singleton(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testMax_Multiple(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testMin_Singleton(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testMin_Multiple(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testNoneMatch(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Singleton(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfDouble iterable)
		{ /* empty iterators hold no value */ }
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements EmptyIteratorTest<Integer>, FunctionalPrimitiveIteratorTest.OfInt
	{
		@Override
		public Stream<EmptyIterable.OfInt> getDuplicatesIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<EmptyIterable.OfInt> getEmptyIterables()
		{
			return Stream.of(EmptyIterable.OfInt());
		}

		@Override
		public Stream<EmptyIterable.OfInt> getSingletonIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<EmptyIterable.OfInt> getMultitonIterables()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatch(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testAnyMatch(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testDetect_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testDetect_Singleton(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testFlatMapToNull(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testMax_Singleton(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testMax_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testMin_Singleton(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testMin_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testNoneMatch(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Singleton(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testReduceIntBinaryOperator_Singleton(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testReduceIntBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testReduceLongBinaryOperator_Singleton(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testReduceLongBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfInt iterable)
		{ /* empty iterators hold no value */ }
	}


	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements EmptyIteratorTest<Long>, FunctionalPrimitiveIteratorTest.OfLong
	{
		@Override
		public Stream<EmptyIterable.OfLong> getDuplicatesIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<EmptyIterable.OfLong> getEmptyIterables()
		{
			return Stream.of(EmptyIterable.OfLong());
		}

		@Override
		public Stream<EmptyIterable.OfLong> getSingletonIterables()
		{
			return Stream.empty();
		}

		@Override
		public Stream<EmptyIterable.OfLong> getMultitonIterables()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatch(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testAnyMatch(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testDetect_Multiple(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testDetect_Singleton(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testFlatMapToNull(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testMax_Singleton(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testMax_Multiple(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testMin_Singleton(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testMin_Multiple(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testNoneMatch(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testReduceLongBinaryOperator_Singleton(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* empty iterators hold no value */ }

		@Override
		public void testReduceLongBinaryOperator_Multiple(FunctionalPrimitiveIterable.OfLong iterable)
		{ /* empty iterators hold no value */ }
	}
}