package param;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public interface ExactIntegerTest
{
	@Nested
	public class ExactIntTest implements ExactIntegerTest
	{
		@Test
		public void testFitsInt()
		{
			ExactInteger.ExactInt x = new ExactInteger.ExactInt(Integer.MAX_VALUE);
			assertTrue(x.fitsInt());
		}

		@Test
		public void testFitsLong()
		{
			ExactInteger.ExactInt x = new ExactInteger.ExactInt(Integer.MAX_VALUE);
			assertTrue(x.fitsLong());
		}

		@Test
		public void testIntValue()
		{
			int x = Integer.MAX_VALUE;
			assertEquals(x, new ExactInteger.ExactInt(x).intValue());
		}

		@Test
		public void testIntValueExact()
		{
			int x = Integer.MAX_VALUE;
			assertDoesNotThrow(() -> new ExactInteger.ExactInt(x).intValueExact());
		}

		@Test
		public void testLongValue()
		{
			int x = Integer.MAX_VALUE;
			assertEquals((long)x, new ExactInteger.ExactInt(x).longValue());
		}

		@Test
		public void testLongValueExact()
		{
			int x = Integer.MAX_VALUE;
			assertDoesNotThrow(() -> new ExactInteger.ExactInt(x).longValueExact());
		}

		@Test
		public void testBigIntegerValue()
		{
			int x = Integer.MAX_VALUE;
			assertEquals(BigInteger.valueOf(x), new ExactInteger.ExactInt(x).bigIntegerValue());
		}

		@Test
		public void testFloatValue()
		{
			int x = Integer.MAX_VALUE;
			assertEquals((float)x, new ExactInteger.ExactInt(x).floatValue());
		}

		@Test
		public void testFloatValueExact()
		{
			int x = Integer.MAX_VALUE;
			assertThrows(ArithmeticException.class, () -> new ExactInteger.ExactInt(x).floatValueExact());
		}

		@Test
		public void testDoubleValue()
		{
			int x = Integer.MAX_VALUE - 1;
			assertEquals((double)x, new ExactInteger.ExactInt(x).doubleValue());
		}

		@Test
		public void testDoubleValueExact()
		{
			int x = Integer.MAX_VALUE;
			assertDoesNotThrow(() -> new ExactInteger.ExactInt(x).doubleValueExact());
		}
	}

	@Nested
	public class ExactLongTest implements ExactIntegerTest
	{
		@Test
		public void testExactLong_Error()
		{
			assertThrows(IllegalArgumentException.class, () -> new ExactInteger.ExactLong(1));
		}

		@Test
		public void testFitsInt_Long()
		{
			ExactInteger.ExactLong x = new ExactInteger.ExactLong(1L + Integer.MAX_VALUE);
			assertFalse(x.fitsInt());
		}

		@Test
		public void testFitsLong()
		{
			ExactInteger.ExactLong x = new ExactInteger.ExactLong(1L + Integer.MAX_VALUE);
			assertTrue(x.fitsLong());
		}

		@Test
		public void testIntValue()
		{
			long x = Long.MAX_VALUE;
			assertEquals((int)x, new ExactInteger.ExactLong(x).intValue());

			long y = Long.MIN_VALUE;
			assertEquals((int)y, new ExactInteger.ExactLong(y).intValue());
		}

		@Test
		public void testIntValueExact()
		{
			long x = Long.MAX_VALUE;
			assertThrows(ArithmeticException.class, () -> new ExactInteger.ExactLong(x).intValueExact());
		}

		@Test
		public void testLongValue()
		{
			long x = Long.MAX_VALUE;
			assertEquals(x, new ExactInteger.ExactLong(x).longValue());
		}

		@Test
		public void testLongValueExact()
		{
			long x = Long.MAX_VALUE;
			assertDoesNotThrow(() -> new ExactInteger.ExactLong(x).longValueExact());
		}

		@Test
		public void testBigIntegerValue()
		{
			long x = Long.MAX_VALUE;
			assertEquals(BigInteger.valueOf(x), new ExactInteger.ExactLong(x).bigIntegerValue());
		}

		@Test
		public void testFloatValue()
		{
			long x = Long.MAX_VALUE;
			assertEquals((float)x, new ExactInteger.ExactLong(x).floatValue());
		}

		@Test
		public void testFloatValueExact()
		{
			long x = Long.MAX_VALUE;
			assertThrows(ArithmeticException.class, () -> new ExactInteger.ExactLong(x).floatValueExact());
		}

		@Test
		public void testDoubleValue()
		{
			long x = Long.MAX_VALUE;
			assertEquals((double)x, new ExactInteger.ExactLong(x).doubleValue());
		}

		@Test
		public void testDoubleValueExact()
		{
			long x = Long.MAX_VALUE;
			assertThrows(ArithmeticException.class, () -> new ExactInteger.ExactLong(x).doubleValueExact());
		}
	}

	@Nested
	public class ExactBigIntegerTest implements ExactIntegerTest
	{
		@Test
		public void testExactBigInteger_Error()
		{
			assertThrows(IllegalArgumentException.class, () -> new ExactInteger.ExactBigInteger(BigInteger.ONE));
			assertThrows(IllegalArgumentException.class, () -> new ExactInteger.ExactBigInteger(BigInteger.valueOf(1L + Integer.MAX_VALUE)));
		}

		@Test
		public void testFitsInt()
		{
			ExactInteger.ExactBigInteger x = new ExactInteger.ExactBigInteger(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
			assertFalse(x.fitsInt());
		}

		@Test
		public void testFitsLong()
		{
			ExactInteger.ExactBigInteger x = new ExactInteger.ExactBigInteger(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
			assertFalse(x.fitsLong());
		}

		@Test
		public void testIntValue()
		{
			BigInteger x = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
			assertEquals(x.intValue(), new ExactInteger.ExactBigInteger(x).intValue());

			BigInteger y = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE);
			assertEquals(y.intValue(), new ExactInteger.ExactBigInteger(y).intValue());
		}

		@Test
		public void testIntValueExact()
		{
			BigInteger x = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
			assertThrows(ArithmeticException.class, () -> new ExactInteger.ExactBigInteger(x).intValueExact());
		}

		@Test
		public void testLongValue()
		{
			BigInteger x = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
			assertEquals(x.longValue(), new ExactInteger.ExactBigInteger(x).longValue());

			BigInteger y = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE);
			assertEquals(y.longValue(), new ExactInteger.ExactBigInteger(y).longValue());
		}

		@Test
		public void testLongValueExact()
		{
			BigInteger x = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
			assertThrows(ArithmeticException.class, () -> new ExactInteger.ExactBigInteger(x).longValueExact());
		}

		@Test
		public void testBigIntegerValue()
		{
			BigInteger x = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
			assertEquals(x, new ExactInteger.ExactBigInteger(x).bigIntegerValue());
		}

		@Test
		public void testFloatValue()
		{
			BigInteger x = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
			assertEquals(x.floatValue(), new ExactInteger.ExactBigInteger(x).floatValue());
		}

		@Test
		public void testFloatValueExact()
		{
			BigInteger x = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.TWO);
			assertThrows(ArithmeticException.class, () -> new ExactInteger.ExactBigInteger(x).floatValueExact());
		}

		@Test
		public void testDoubleValue()
		{
			BigInteger x = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
			assertEquals((double)x.doubleValue(), new ExactInteger.ExactBigInteger(x).doubleValue());
		}

		@Test
		public void testDoubleValueExact()
		{
			BigInteger x = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.TWO);
			assertThrows(ArithmeticException.class, () -> new ExactInteger.ExactBigInteger(x).doubleValueExact());
		}
	}
}
