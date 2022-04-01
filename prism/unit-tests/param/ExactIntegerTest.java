package param;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExactIntegerTest
{
	@ParameterizedTest
	@ValueSource(longs = {Integer.MIN_VALUE - 1L, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L})
	public void testFitsInt_Long(long n)
	{
		int nInt = (int) n; // narrowing conversion
		assertEquals((long)nInt == n, ExactInteger.fitsInt(n));
	}

	@ParameterizedTest
	@ValueSource(longs = {Integer.MIN_VALUE - 1L, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L})
	public void testFitsInt_BigInteger(long nLong)
	{
		BigInteger n = BigInteger.valueOf(nLong);
		int nInt = n.intValue(); // narrowing conversion
		assertEquals(BigInteger.valueOf(nInt).equals(n), ExactInteger.fitsInt(n));
	}

	@ParameterizedTest
	@MethodSource("testFitsLong_BigInteger")
	public void testFitsLong_BigInteger(BigInteger n)
	{
		long nLong = n.longValue(); // narrowing conversion
		assertEquals(BigInteger.valueOf(nLong).equals(n), ExactInteger.fitsLong(n));
	}

	static Stream<BigInteger> testFitsLong_BigInteger()
	{
		return Stream.of(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE),
		                 BigInteger.valueOf(Long.MIN_VALUE),
		                 BigInteger.valueOf(Long.MAX_VALUE),
		                 BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
	}

	@ParameterizedTest
	@ValueSource(ints = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, 0, Integer.MAX_VALUE - 1, Integer.MAX_VALUE})
	public void testValueOf_Int(int n)
	{
		ExactInteger.ExactInt nX = ExactInteger.valueOf(n);
		assertEquals(n, nX.intValueExact());
	}

	@ParameterizedTest
	@ValueSource(longs = {Long.MIN_VALUE, Long.MIN_VALUE + 1, Integer.MIN_VALUE, 0, Long.MAX_VALUE - 1, Integer.MAX_VALUE, Long.MAX_VALUE})
	public void testValueOf_Long(long n)
	{
		ExactInteger.IntOrLong nX = ExactInteger.valueOf(n);
		assertCompact(nX);
		assertEquals(n, nX.longValueExact());
	}

	@ParameterizedTest
	@MethodSource("testValueOf_BigInteger")
	public void testValueOf_BigInteger(BigInteger n)
	{
		ExactInteger nX = ExactInteger.valueOf(n);
		assertCompact(nX);
		assertEquals(n, nX.bigIntegerValue());
	}

	static Stream<BigInteger> testValueOf_BigInteger()
	{
		return Stream.of(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE),
				BigInteger.valueOf(Long.MIN_VALUE),
				BigInteger.valueOf(Integer.MIN_VALUE),
				BigInteger.ZERO,
				BigInteger.valueOf(Integer.MAX_VALUE),
				BigInteger.valueOf(Long.MAX_VALUE),
				BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
	}

	public static void assertCompact(ExactInteger n)
	{
		// fitsInt -> ExactInt
		assertTrue((n instanceof ExactInteger.ExactInt) || !n.fitsInt());
		// !fitsInt & fitsLong -> ExactLong
		assertTrue((n instanceof ExactInteger.ExactLong) || !(n.fitsLong() && !n.fitsInt()));
	}
}
