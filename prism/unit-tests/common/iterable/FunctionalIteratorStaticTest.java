package common.iterable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIteratorEquals;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.*;

class FunctionalIteratorStaticTest
{
	static Stream<Iterable<Object>> getIterables()
	{
		return Stream.of(Collections.singleton(null),
		                 Collections.emptyList(),
		                 Collections.singleton("one"),
		                 Arrays.asList("one", "two", "three"));
	}

	static <T> Stream<Iterable<T>> getIterablesNull()
	{
		return Stream.of(Collections.singleton(null));
	}

	static Stream<Iterable<Double>> getIterablesDouble()
	{
		return Stream.of(Collections.emptyList(),
		                 Collections.singleton(1.0),
		                 Arrays.asList(1.0, 2.0, 3.0));
	}

	static Stream<Iterable<Integer>> getIterablesInteger()
	{
		return Stream.of(Collections.emptyList(),
		                 Collections.singleton(1),
		                 Arrays.asList(1, 2, 3));
	}

	static Stream<Iterable<Long>> getIterablesLong()
	{
		return Stream.of(Collections.emptyList(),
		                 Collections.singleton(1L),
		                 Arrays.asList(1L, 2L, 3L));
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	@DisplayName("extend() yields same sequence as the underlying iterator.")
	void testExtend(Iterable<?> iterable)
	{
		Iterator<?> iterator = iterable.iterator();
		FunctionalIterator<?>functional = FunctionalIterator.extend(iterable);
		assertIteratorEquals(iterator, functional);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	@DisplayName("extend() does not extend a FunctionalIterator.")
	void testExtend_FunctionalIterator(Iterable<?> iterable)
	{
		Iterator<?> iterator = FunctionalIterator.extend(iterable); 
		FunctionalIterator<?> functional = FunctionalIterator.extend(iterator);
		assertSame(iterator, functional);
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	@DisplayName("extend() does not extend a FunctionalPrimitiveIterator.OfDouble.")
	void testExtend_FunctionalPrimitiveIteratorOfDouble(Iterable<Double> iterable)
	{
		Iterator<Double> iterator = FunctionalIterator.extend(unboxDouble(iterable));
		FunctionalIterator<Double> functional = FunctionalIterator.extend(iterator);
		assertSame(iterator, functional);
	}

	@ParameterizedTest
	@MethodSource("getIterablesInteger")
	@DisplayName("extend() does not extend a FunctionalPrimitiveIterator.OfInt.")
	void testExtend_FunctionalPrimitiveIteratorOfInt(Iterable<Integer> iterable)
	{
		Iterator<Integer> iterator = FunctionalIterator.extend(unboxInt(iterable));
		FunctionalIterator<Integer> functional = FunctionalIterator.extend(iterator);
		assertSame(iterator, functional);
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	@DisplayName("extend() does not extend a FunctionalPrimitiveIterator.OfLong.")
	void testExtend_FunctionalPrimitiveIteratorOfLong(Iterable<Long> iterable)
	{
		Iterator<Long> iterator = FunctionalIterator.extend(unboxLong(iterable));
		FunctionalIterator<Long> functional = FunctionalIterator.extend(iterator);
		assertSame(iterator, functional);
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	@DisplayName("extend() extends OfDouble to FunctionalPrimtiveIterator.")
	void testExtend_PrimitiveIteratorOfDouble(Iterable<Double> iterable)
	{
		Iterator<Double> iterator = unboxDouble(iterable).iterator(); 
		FunctionalIterator<Double> functional = FunctionalIterator.extend(iterator);
		assertTrue(functional instanceof FunctionalPrimitiveIterator);
	}

	@ParameterizedTest
	@MethodSource("getIterablesInteger")
	@DisplayName("extend() extends OfInt to FunctionalPrimtiveIterator")
	void testExtend_PrimitiveIteratorOfInt(Iterable<Integer> iterable)
	{
		Iterator<Integer> iterator = unboxInt(iterable).iterator();
		FunctionalIterator<Integer> functional = FunctionalIterator.extend(iterator);
		assertTrue(functional instanceof FunctionalPrimitiveIterator);
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	@DisplayName("extend() extends OfLong to FunctionalPrimtiveIterator")
	void testExtend_PrimitiveIteratorOfLong(Iterable<Long> iterable)
	{
		Iterator<Long> iterator = unboxLong(iterable).iterator();
		FunctionalIterator<Long> functional = FunctionalIterator.extend(iterator);
		assertTrue(functional instanceof FunctionalPrimitiveIterator);
	}

	@Test
	@DisplayName("extend() with null throws NullPointerException.")
	void testExtend_Null()
	{
		assertThrows(NullPointerException.class, () -> FunctionalIterator.extend((Iterator<?>) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	@DisplayName("unboxDouble() yields same sequence as the underlying iterator.")
	void testUnboxDouble(Iterable<Double> iterable)
	{
		PrimitiveIterator.OfDouble iterator = unboxDouble(iterable).iterator();
		FunctionalPrimitiveIterator.OfDouble functional = FunctionalIterator.unboxDouble(iterable);
		assertIteratorEquals(iterator, functional);
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	@DisplayName("unboxDouble() does not unbox a FunctionalPrimitiveIterator.OfDouble.")
	void testUnboxDouble_OfDouble(Iterable<Double> iterable)
	{
		PrimitiveIterator.OfDouble iterator = FunctionalIterator.extend(unboxDouble(iterable)); 
		FunctionalPrimitiveIterator.OfDouble functional = FunctionalIterator.unboxDouble(iterator);
		assertSame(iterator, functional);
	}

	@Test
	@DisplayName("unboxDouble() with null throws NullPointerException.")
	void testUnboxDouble_Null()
	{
		assertThrows(NullPointerException.class, () -> FunctionalIterator.unboxDouble((Iterable<Double>) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesNull")
	@DisplayName("unboxDouble() with an Iterable conatining null throws NullPointerException.")
	void testUnboxDouble_Iterable_Null(Iterable<Double> iterable)
	{
		FunctionalPrimitiveIterator.OfDouble functional = FunctionalIterator.unboxDouble(iterable);
		assertThrows(NullPointerException.class, functional::consume);
	}

	@ParameterizedTest
	@MethodSource("getIterablesInteger")
	@DisplayName("unboxInt() yields same sequence as the underlying iterator.")
	void testUnboxInt(Iterable<Integer> iterable)
	{
		PrimitiveIterator.OfInt iterator = unboxInt(iterable).iterator();
		FunctionalPrimitiveIterator.OfInt functional = FunctionalIterator.unboxInt(iterable);
		assertIteratorEquals(iterator, functional);
	}

	@ParameterizedTest
	@MethodSource("getIterablesInteger")
	@DisplayName("unboxInt() does not unbox a FunctionalPrimitiveIterator.OfInt.")
	void testUnboxInt_OfInt(Iterable<Integer> iterable)
	{
		PrimitiveIterator.OfInt iterator = FunctionalIterator.extend(unboxInt(iterable)); 
		FunctionalPrimitiveIterator.OfInt functional = FunctionalIterator.unboxInt(iterator);
		assertSame(iterator, functional);
	}

	@Test
	@DisplayName("unboxInt() with null throws NullPointerException.")
	void testUnboxInt_Null()
	{
		assertThrows(NullPointerException.class, () -> FunctionalIterator.unboxInt((Iterable<Integer>) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesNull")
	@DisplayName("unboxInt() with an Iterable conatining null throws NullPointerException.")
	void testUnboxInt_Iterable_Null(Iterable<Integer> iterable)
	{
		FunctionalPrimitiveIterator.OfInt functional = FunctionalIterator.unboxInt(iterable);
		assertThrows(NullPointerException.class, functional::consume);
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	@DisplayName("unboxLong() yields same sequence as the underlying iterator.")
	void testUnboxLong(Iterable<Long> iterable)
	{
		PrimitiveIterator.OfLong iterator = unboxLong(iterable).iterator();
		FunctionalPrimitiveIterator.OfLong functional = FunctionalIterator.unboxLong(iterable);
		assertIteratorEquals(iterator, functional);
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	@DisplayName("unboxLong() does not unbox a FunctionalPrimitiveIterator.OfDouble.")
	void testUnboxLong_OfLong(Iterable<Long> iterable)
	{
		PrimitiveIterator.OfLong iterator = FunctionalIterator.extend(unboxLong(iterable)); 
		FunctionalPrimitiveIterator.OfLong functional = FunctionalIterator.unboxLong(iterator);
		assertSame(iterator, functional);
	}

	@Test
	@DisplayName("unboxDouble() with null throws NullPointerException.")
	void testUnboxLong_Null()
	{
		assertThrows(NullPointerException.class, () -> FunctionalIterator.unboxLong((Iterable<Long>) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesNull")
	@DisplayName("unboxInt() with an Iterable conatining null throws NullPointerException.")
	void testUnboxLong_Iterable_Null(Iterable<Long> iterable)
	{
		FunctionalPrimitiveIterator.OfLong functional = FunctionalIterator.unboxLong(iterable);
		assertThrows(NullPointerException.class, functional::consume);
	}
}
