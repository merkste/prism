package common.iterable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class IterableAdaptorTest
{
	static Stream<Iterable<Object>> getIterables()
	{
		return Stream.of(Collections.singleton(null),
		                 Collections.emptyList(),
		                 Collections.singleton("one"),
		                 Arrays.asList("one", "two", "three"));
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	@DisplayName("Adaptor yields same sequence as the underlying iterable.")
	void testIterableAdaptor(Iterable<?> iterable)
	{
		assertIterableEquals(iterable, new IterableAdaptor.Of<>(iterable));
	}

	@Test
	@DisplayName("Adapter on null throws NullPointerException.")
	void testIterableAdaptor_Null()
	{
		assertThrows(NullPointerException.class, () -> new IterableAdaptor.Of<>(null));
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	@DisplayName("iterator() answers a FunctionalIterator.")
	void testIterator(Iterable<?> iterable)
	{
		IterableAdaptor.Of<?> adaptor = new IterableAdaptor.Of<>(iterable);
		assertTrue(adaptor.iterator() instanceof FunctionalIterator);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	@DisplayName("forEach() yields same sequence as the underlying iterable.")
	void testForEach(Iterable<?> iterable)
	{
		IterableAdaptor.Of<?> adaptor = new IterableAdaptor.Of<>(iterable);
		List<Object> sequence = new ArrayList<>();
		adaptor.forEach((each) -> sequence.add(each));
		assertIterableEquals(iterable, sequence);
	}
}
