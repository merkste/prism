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

import static org.junit.jupiter.api.Assertions.*;

public class ExactIntegerStaticTest
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
	@ValueSource(ints = {Integer.MIN_VALUE, 0, 16777215, 16777216, 16777218})
	public void testFitsFloat_Int_True(int x)
	{
		assertTrue(ExactInteger.fitsFloat(x));
		assertTrue(ExactInteger.fitsFloat(-x));
	}

	@ParameterizedTest
	@ValueSource(ints = {16777217, 16777219})
	public void testFitsFloat_Int_False(int x)
	{
		assertFalse(ExactInteger.fitsFloat(x));
		assertFalse(ExactInteger.fitsFloat(-x));
	}

	@ParameterizedTest
	@ValueSource(longs = {Long.MIN_VALUE, 0L, 16777215L, 16777216L, 16777218L})
	public void testFitsFloat_Long_True(long x)
	{
		assertTrue(ExactInteger.fitsFloat(x));
		assertTrue(ExactInteger.fitsFloat(-x));
	}

	@ParameterizedTest
	@ValueSource(longs = {16777217L, 16777219L})
	public void testFitsFloat_Int_False(long x)
	{
		assertFalse(ExactInteger.fitsFloat(x));
		assertFalse(ExactInteger.fitsFloat(-x));
	}

	@ParameterizedTest
	@ValueSource(longs = {Long.MIN_VALUE, 0L, 16777215L, 16777216L, 16777218L})
	public void testFitsFloat_BigInteger_True(long x)
	{
		BigInteger b = BigInteger.valueOf(x);
		assertTrue(ExactInteger.fitsFloat(b));
		assertTrue(ExactInteger.fitsFloat(b.negate()));
	}

	@ParameterizedTest
	@ValueSource(longs = {16777217L, 16777219L})
	public void testFitsFloat_BigInteger_False(long x)
	{
		BigInteger b = BigInteger.valueOf(x);
		assertFalse(ExactInteger.fitsFloat(b));
		assertFalse(ExactInteger.fitsFloat(b.negate()));
	}

	@ParameterizedTest
	@ValueSource(longs = {0, 1, 3, 16777215L})
	public void testFitsFloat_BigInteger_Exponent_True(long x)
	{
		BigInteger b = BigInteger.valueOf(x).shiftLeft(127);
		assertTrue(ExactInteger.fitsFloat(b));
		assertTrue(ExactInteger.fitsFloat(b.negate()));
	}

	@ParameterizedTest
	@ValueSource(longs = {2, 4, 16777216L, 16777217L})
	public void testFitsFloat_BigInteger_Exponent_False(long x)
	{
		BigInteger b = BigInteger.valueOf(x).shiftLeft(127);
		assertFalse(ExactInteger.fitsFloat(b));
		assertFalse(ExactInteger.fitsFloat(b.negate()));
	}


	@ParameterizedTest
	@ValueSource(ints = {Integer.MIN_VALUE, 0, 1, 16777217, Integer.MAX_VALUE})
	public void testFitsDouble_Int(int x)
	{
		assertTrue(ExactInteger.fitsDouble(x));
		assertTrue(ExactInteger.fitsDouble(-x));
	}

	@ParameterizedTest
	@ValueSource(longs = {Long.MIN_VALUE, 0L, 9007199254740991L, 9007199254740992L, 9007199254740994L})
	public void testFitsDouble_Long_True(long x)
	{
		assertTrue(ExactInteger.fitsDouble(x));
		assertTrue(ExactInteger.fitsDouble(-x));
	}

	@ParameterizedTest
	@ValueSource(longs = {9007199254740993L, 9007199254740995L})
	public void testFitsDouble_Int_False(long x)
	{
		assertFalse(ExactInteger.fitsDouble(x));
		assertFalse(ExactInteger.fitsDouble(-x));
	}

	@ParameterizedTest
	@ValueSource(longs = {Long.MIN_VALUE, 0L, 9007199254740991L, 9007199254740992L, 9007199254740994L})
	public void testFitsDouble_BigInteger_True(long x)
	{
		BigInteger b = BigInteger.valueOf(x);
		assertTrue(ExactInteger.fitsDouble(b));
		assertTrue(ExactInteger.fitsDouble(b.negate()));
	}

	@ParameterizedTest
	@ValueSource(longs = {9007199254740993L, 9007199254740995L})
	public void testFitsDouble_BigInteger_False(long x)
	{
		BigInteger b = BigInteger.valueOf(x);
		assertFalse(ExactInteger.fitsDouble(b));
		assertFalse(ExactInteger.fitsDouble(b.negate()));
	}

	@ParameterizedTest
	@ValueSource(longs = {0, 1, 3, 9007199254740991L})
	public void testFitsDouble_BigInteger_Exponent_True(long x)
	{
		BigInteger b = BigInteger.valueOf(x).shiftLeft(1023);
		assertTrue(ExactInteger.fitsDouble(b));
		assertTrue(ExactInteger.fitsDouble(b.negate()));
	}

	@ParameterizedTest
	@ValueSource(longs = {2, 4, 9007199254740992L, 9007199254740993L})
	public void testFitsDouble_BigInteger_Exponent_False(long x)
	{
		BigInteger b = BigInteger.valueOf(x).shiftLeft(1023);
		assertFalse(ExactInteger.fitsDouble(b));
		assertFalse(ExactInteger.fitsDouble(b.negate()));
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
