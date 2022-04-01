package param;

import common.StopWatch;
import prism.PrismLog;
import prism.PrismPrintStreamLog;

import java.math.BigInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

// TODO: extract constants
// TODO: make constructors private/protected
// TODO: consider cache similar to integer cache and using #== in equals

/**
 * https://wiki.sei.cmu.edu/confluence/display/java/NUM00-J.+Detect+or+prevent+integer+overflow
 */
public interface ExactInteger
{
	static final BigInteger BIG_INT_MIN_VALUE = BigInteger.valueOf(Integer.MIN_VALUE);
	static final BigInteger BIG_INT_MAX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);
	static final BigInteger BIG_LONG_MIN_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
	static final BigInteger BIG_LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);
	static final BigInteger BIG_NEGATIVE_ONE = BigInteger.ONE.negate();

	static final ExactInt ZERO = new ExactInt(0);
	static final ExactInt ONE = new ExactInt(1);
	static final ExactInt NEGATIVE_ONE = new ExactInt(-1);


	static boolean fitsInt(long x)
	{
		// equally fast to comparision with range
		return (int)x == x;
	}

	static boolean fitsInt(BigInteger x)
	{
		// faster than x.equals(x.intValue());
		return BIG_INT_MIN_VALUE.compareTo(x) <= 0 && x.compareTo(BIG_INT_MAX_VALUE) <= 0;
	}

	static boolean fitsLong(BigInteger x)
	{
		// faster than x.equals(x.intValue());
		return BIG_LONG_MIN_VALUE.compareTo(x) <= 0 && x.compareTo(BIG_LONG_MAX_VALUE) <= 0;
	}

	public static ExactInt valueOf(int x)
	{
		return new ExactInt(x);
	}

	public static IntOrLong valueOf(long x)
	{
		int intX = (int)x; // inline #fitsInt to avoid repeating narrowing conversion
		return x == intX ? new ExactInt(intX) : new ExactLong(x);
	}

	public static ExactInteger valueOf(BigInteger x)
	{
		return ExactInteger.fitsLong(x) ? valueOf(x.longValue()): new ExactBigInteger(x);
	}

	default boolean fitsInt()
	{
		return false;
	}

	default boolean fitsLong()
	{
		return false;
	}

	int intValue();

	int intValueExact();

	long longValue();

	long longValueExact();

	BigInteger bigIntegerValue();

	float floatValue();

	double doubleValue();

	// Comparison

	int compareTo(int other);

	int compareTo(long other);

	default int compareTo(BigInteger other)
	{
		return bigIntegerValue().compareTo(other);
	}

	int compareTo(ExactInteger other);

	boolean equals(int other);

	boolean equals(long other);

	default boolean equals(BigInteger other)
	{
		return bigIntegerValue().equals(other);
	}

	boolean equals(ExactInteger other);

	public static int hashCode(int x)
	{
		return Long.hashCode(x);
	}

	public static int hashCode(long x)
	{
		// Fall back to BigInteger for MIN_VALUE, since -MIN_VALUE causes a wrong hash value
		return (x == Long.MIN_VALUE) ? BigInteger.valueOf(x).hashCode() : Long.hashCode(x);
	}

	public static int hashCode(BigInteger x)
	{
		// For long values, except for MIN_VALUE, use hash function of Long
		return fitsLong(x) && !x.equals(BIG_LONG_MIN_VALUE) ? Long.hashCode(x.longValue()) : x.hashCode();
	}

	int signum();

	ExactInteger add(int summand);

	ExactInteger add(long summand);

	default ExactInteger add(BigInteger summand)
	{
		return valueOf(bigIntegerValue().add(summand));
	}

	ExactInteger add(ExactInteger summand);

	ExactInteger subtract(ExactInteger subtrahend);

	ExactInteger subtract(int subtrahend);

	ExactInteger subtract(long subtrahend);

	default ExactInteger subtract(BigInteger subtrahend)
	{
		return valueOf(bigIntegerValue().subtract(subtrahend));
	}

	ExactInteger subtractFrom(ExactInteger minuend);

	ExactInteger subtractFrom(int minuend);

	ExactInteger subtractFrom(long minuend);

	default ExactInteger subtractFrom(BigInteger minuend)
	{
		return valueOf(minuend.subtract(bigIntegerValue()));
	}

	ExactInteger multiply(int factor);

	ExactInteger multiply(long factor);

	default ExactInteger multiply(BigInteger factor)
	{
		return valueOf(bigIntegerValue().multiply(factor));
	}

	ExactInteger multiply(ExactInteger factor);

	ExactInteger divide(int divisor);

	ExactInteger divide(long divisor);

	default ExactInteger divide(BigInteger divisor)
	{
		return valueOf(bigIntegerValue().divide(divisor));
	}

	ExactInteger divide(ExactInteger divisor);

	ExactInteger divideDividend(int dividend);

	ExactInteger divideDividend(long dividend);

	default ExactInteger divideDividend(BigInteger dividend)
	{
		return valueOf(dividend.divide(bigIntegerValue()));
	}

	ExactInteger divideDividend(ExactInteger dividend);

	ExactInteger negate();

	ExactInteger abs();

	ExactInteger gcd(int y);

	ExactInteger gcd(long y);

	default ExactInteger gcd(BigInteger y)
	{
		return valueOf(bigIntegerValue().gcd(y));
	}

	ExactInteger gcd(ExactInteger factor);

	ExactInteger pow(int exponent);

	/**
	 * Marker interface
	 */
	interface IntOrLong extends ExactInteger
	{
	}

	/**
	 * Marker interface
	 */
	interface LongOrBigInteger extends ExactInteger
	{
	}

	public class ExactInt extends Number implements IntOrLong
	{
		public final int x;

		public ExactInt(int x)
		{
			this.x = x;
		}

		@Override
		public boolean fitsInt()
		{
			return true;
		}

		@Override
		public boolean fitsLong()
		{
			return true;
		}

		@Override

		public int intValue()
		{
			return x;
		}

		@Override
		public int intValueExact()
		{
			return x;
		}

		@Override
		public long longValue()
		{
			return x;
		}

		@Override
		public long longValueExact()
		{
			return x;
		}

		@Override
		public BigInteger bigIntegerValue()
		{
			return BigInteger.valueOf(x);
		}

		@Override
		public float floatValue()
		{
			return x;
		}

		@Override
		public double doubleValue()
		{
			return x;
		}

		@Override
		public int compareTo(int other)
		{
			return Integer.compare(x, other);
		}

		@Override
		public int compareTo(long other)
		{
			return Long.compare(x, other);
		}

		@Override
		public int compareTo(ExactInteger other)
		{
			return -other.compareTo(x);
		}

		@Override
		public boolean equals(int other)
		{
			return x == other;
		}

		@Override
		public boolean equals(long other)
		{
			return x == other;
		}

		@Override
		public boolean equals(Object other)
		{
			return (other instanceof ExactInteger) && equals((ExactInteger) other);
		}

		@Override
		public boolean equals(ExactInteger other)
		{
			return other.equals(x);
		}

		@Override
		public int signum()
		{
			return Integer.signum(x);
		}

		@Override
		public int hashCode()
		{
			return ExactInteger.hashCode(x);
		}

		@Override
		public IntOrLong add(int summand)
		{
			return ExactInteger.add(x, summand);
		}

		@Override
		public ExactInteger add(long summand)
		{
			return ExactInteger.add(x, summand);
		}

		@Override
		public ExactInteger add(ExactInteger summand)
		{
			return summand.add(x);
		}

		@Override
		public IntOrLong subtract(int subtrahend)
		{
			return ExactInteger.subtract(x, subtrahend);
		}

		@Override
		public ExactInteger subtract(long subtrahend)
		{
			return ExactInteger.subtract(x, subtrahend);
		}

		@Override
		public ExactInteger subtract(ExactInteger subtrahend)
		{
			return subtrahend.subtractFrom(x);
		}

		@Override
		public IntOrLong subtractFrom(int minuend)
		{
			return ExactInteger.subtract(minuend, x);
		}

		@Override
		public ExactInteger subtractFrom(long minuend)
		{
			return ExactInteger.subtract(minuend, x);
		}

		@Override
		public ExactInteger subtractFrom(ExactInteger minuend)
		{
			return minuend.subtract(x);
		}

		@Override
		public IntOrLong multiply(int factor)
		{
			return ExactInteger.multiply(x, factor);
		}

		@Override
		public ExactInteger multiply(long factor)
		{
			return ExactInteger.multiply(x, factor);
		}

		@Override
		public ExactInteger multiply(ExactInteger factor)
		{
			return factor.multiply(x);
		}

		@Override
		public IntOrLong divide(int divisor)
		{
			return ExactInteger.divide(x, divisor);
		}

		@Override
		public ExactInteger divide(long divisor)
		{
			return ExactInteger.divide(x, divisor);
		}

		@Override
		public ExactInteger divide(ExactInteger divisor)
		{
			return divisor.divideDividend(x);
		}

		@Override
		public IntOrLong divideDividend(int dividend)
		{
			return ExactInteger.divide(dividend, x);
		}

		@Override
		public ExactInteger divideDividend(long dividend)
		{
			return ExactInteger.divide(dividend, x);
		}

		@Override
		public ExactInteger divideDividend(ExactInteger dividend)
		{
			return dividend.divide(x);
		}

		@Override
		public IntOrLong negate()
		{
			return ExactInteger.negate(x);
		}

		@Override
		public IntOrLong abs()
		{
			return ExactInteger.abs(x);
		}

		@Override
		public IntOrLong gcd(int y)
		{
			return ExactInteger.gcd(x, y);
		}

		@Override
		public ExactInteger gcd(long y)
		{
			return ExactInteger.gcd(x, y);
		}

		@Override
		public ExactInteger gcd(ExactInteger y)
		{
			return y.gcd(x);
		}

		@Override
		public ExactInteger pow(int exponent)
		{
			return exponent == 1 ? this : ExactInteger.pow(x, exponent);
		}

		@Override
		public String toString()
		{
			return String.valueOf(x);
		}
	}



	public class ExactLong extends Number implements IntOrLong, LongOrBigInteger
	{
		public final long x;

		public ExactLong(long x)
		{
			this.x = x;
		}

		@Override
		public boolean fitsInt()
		{
			return ExactInteger.fitsInt(x);
		}

		@Override
		public boolean fitsLong()
		{
			return true;
		}

		@Override
		public int intValue()
		{
			return (int)x;
		}

		@Override
		public int intValueExact()
		{
			return Math.toIntExact(x);
		}

		@Override
		public long longValue()
		{
			return x;
		}

		@Override
		public long longValueExact()
		{
			return x;
		}

		@Override
		public BigInteger bigIntegerValue()
		{
			return BigInteger.valueOf(x);
		}

		@Override
		public float floatValue()
		{
			return x;
		}

		@Override
		public double doubleValue()
		{
			return x;
		}

		@Override
		public int compareTo(int other)
		{
			return Long.compare(x, other);
		}

		@Override
		public int compareTo(long other)
		{
			return Long.compare(x, other);
		}

		@Override
		public int compareTo(ExactInteger other)
		{
			return -other.compareTo(x);
		}

		@Override
		public boolean equals(int other)
		{
			return x == other;
		}

		@Override
		public boolean equals(long other)
		{
			return x == other;
		}

		@Override
		public boolean equals(Object other)
		{
			return (other instanceof ExactInteger) && equals((ExactInteger) other);
		}

		@Override
		public boolean equals(ExactInteger other)
		{
			return other.equals(x);
		}

		@Override
		public int signum()
		{
			return Long.signum(x);
		}

		@Override
		public int hashCode()
		{
			return ExactInteger.hashCode(x);
		}

		@Override
		public ExactInteger add(int summand)
		{
			return ExactInteger.add(x, summand);
		}

		@Override
		public ExactInteger add(long summand)
		{
			return ExactInteger.add(x, summand);
		}

		@Override
		public ExactInteger add(ExactInteger summand)
		{
			return summand.add(x);
		}

		@Override
		public ExactInteger subtract(int subtrahend)
		{
			return ExactInteger.subtract(x, subtrahend);
		}

		@Override
		public ExactInteger subtract(long subtrahend)
		{
			return ExactInteger.subtract(x, subtrahend);
		}

		@Override
		public ExactInteger subtract(ExactInteger subtrahend)
		{
			return subtrahend.subtractFrom(x);
		}

		@Override
		public ExactInteger subtractFrom(int minuend)
		{
			return ExactInteger.subtract(minuend, x);
		}

		@Override
		public ExactInteger subtractFrom(long minuend)
		{
			return ExactInteger.subtract(minuend, x);
		}

		@Override
		public ExactInteger subtractFrom(ExactInteger minuend)
		{
			return minuend.subtract(x);
		}

		@Override
		public ExactInteger multiply(int factor)
		{
			return ExactInteger.multiply(x, factor);
		}

		@Override
		public ExactInteger multiply(long factor)
		{
			return ExactInteger.multiply(x, factor);
		}

		@Override
		public ExactInteger multiply(ExactInteger factor)
		{
			return factor.multiply(x);
		}

		@Override
		public ExactInteger divide(int divisor)
		{
			return ExactInteger.divide(x, divisor);
		}

		@Override
		public ExactInteger divide(long divisor)
		{
			return ExactInteger.divide(x, divisor);
		}

		@Override
		public ExactInteger divide(ExactInteger divisor)
		{
			return divisor.divideDividend(x);
		}

		@Override
		public ExactInteger divideDividend(int dividend)
		{
			return ExactInteger.divide(dividend, x);
		}

		@Override
		public ExactInteger divideDividend(long dividend)
		{
			return ExactInteger.divide(dividend, x);
		}

		@Override
		public ExactInteger divideDividend(ExactInteger dividend)
		{
			return dividend.divide(x);
		}

		@Override
		public ExactInteger negate()
		{
			return ExactInteger.negate(x);
		}

		@Override
		public ExactInteger abs()
		{
			return ExactInteger.abs(x);
		}

		@Override
		public ExactInteger gcd(int y)
		{
			return ExactInteger.gcd(x, y);
		}

		@Override
		public ExactInteger gcd(long y)
		{
			return ExactInteger.gcd(x, y);
		}

		@Override
		public ExactInteger gcd(ExactInteger y)
		{
			return y.gcd(x);
		}

		@Override
		public ExactInteger pow(int exponent)
		{
			return exponent == 1 ? this : ExactInteger.pow(x, exponent);
		}

		@Override
		public String toString()
		{
			return String.valueOf(x);
		}
	}



	public class ExactBigInteger extends Number implements LongOrBigInteger
	{
		public final BigInteger x;

		public ExactBigInteger(BigInteger x)
		{
			this.x = x;
		}

		@Override
		public boolean fitsInt()
		{
			return ExactInteger.fitsInt(x);
		}

		@Override
		public boolean fitsLong()
		{
			return ExactInteger.fitsLong(x);
		}

		@Override
		public int intValue()
		{
			return x.intValue();
		}

		@Override
		public int intValueExact()
		{
			return x.intValueExact();
		}

		@Override
		public long longValue()
		{
			return x.longValue();
		}

		@Override
		public long longValueExact()
		{
			return x.longValueExact();
		}

		@Override
		public BigInteger bigIntegerValue()
		{
			return x;
		}

		@Override
		public float floatValue()
		{
			return x.floatValue();
		}

		@Override
		public double doubleValue()
		{
			return x.doubleValue();
		}

		@Override
		public int compareTo(int other)
		{
			return compareTo(BigInteger.valueOf(other));
		}

		@Override
		public int compareTo(long other)
		{
			return compareTo(BigInteger.valueOf(other));
		}

		@Override
		public int compareTo(ExactInteger other)
		{
			return -other.compareTo(x);
		}

		@Override
		public boolean equals(int other)
		{
			return equals(BigInteger.valueOf(other));
		}

		@Override
		public boolean equals(long other)
		{
			return equals(BigInteger.valueOf(other));
		}

		@Override
		public boolean equals(Object other)
		{
			return (other instanceof ExactInteger) && equals((ExactInteger) other);
		}

		@Override
		public boolean equals(ExactInteger other)
		{
			return other.equals(x);
		}

		@Override
		public int signum()
		{
			return x.signum();
		}

		@Override
		public int hashCode()
		{
			return ExactInteger.hashCode(x);
		}

		@Override
		public ExactInteger add(int summand)
		{
			return add(BigInteger.valueOf(summand));
		}

		@Override
		public ExactInteger add(long summand)
		{
			return add(BigInteger.valueOf(summand));
		}

		@Override
		public ExactInteger add(ExactInteger summand)
		{
			return summand.add(x);
		}

		@Override
		public ExactInteger subtract(int subtrahend)
		{
			return subtract(BigInteger.valueOf(subtrahend));
		}

		@Override
		public ExactInteger subtract(long subtrahend)
		{
			return subtract(BigInteger.valueOf(subtrahend));
		}

		@Override
		public ExactInteger subtract(ExactInteger subtrahend)
		{
			return subtrahend.subtractFrom(x);
		}

		@Override
		public ExactInteger subtractFrom(int minuend)
		{
			return subtractFrom(BigInteger.valueOf(minuend));
		}

		@Override
		public ExactInteger subtractFrom(long minuend)
		{
			return subtractFrom(BigInteger.valueOf(minuend));
		}

		@Override
		public ExactInteger subtractFrom(ExactInteger minuend)
		{
			return minuend.subtract(x);
		}

		@Override
		public ExactInteger multiply(int factor)
		{
			return multiply(BigInteger.valueOf(factor));
		}

		@Override
		public ExactInteger multiply(long factor)
		{
			return multiply(BigInteger.valueOf(factor));
		}

		@Override
		public ExactInteger multiply(ExactInteger factor)
		{
			return factor.multiply(x);
		}

		@Override
		public ExactInteger divide(int divisor)
		{
			return divide(BigInteger.valueOf(divisor));
		}

		@Override
		public ExactInteger divide(long divisor)
		{
			return divide(BigInteger.valueOf(divisor));
		}

		@Override
		public ExactInteger divide(ExactInteger divisor)
		{
			return divisor.divideDividend(x);
		}

		@Override
		public ExactInteger divideDividend(int dividend)
		{
			return divideDividend(BigInteger.valueOf(dividend));
		}

		@Override
		public ExactInteger divideDividend(long dividend)
		{
			return divideDividend(BigInteger.valueOf(dividend));
		}

		@Override
		public ExactInteger divideDividend(ExactInteger dividend)
		{
			return dividend.divide(x);

		}

		@Override
		public ExactInteger negate()
		{
			return new ExactBigInteger(x.negate());
		}

		@Override
		public ExactInteger abs()
		{
			return new ExactBigInteger(x.abs());
		}

		@Override
		public ExactInteger gcd(int y)
		{
			return gcd(BigInteger.valueOf(y));
		}

		@Override
		public ExactInteger gcd(long y)
		{
			return gcd(BigInteger.valueOf(y));
		}

		@Override
		public ExactInteger gcd(ExactInteger y)
		{
			return y.gcd(x);
		}

		@Override
		public ExactInteger pow(int exponent)
		{
			return exponent == 1 ? this : valueOf(x.pow(exponent));
		}

		@Override
		public String toString()
		{
			return String.valueOf(x);
		}
	}



	// Arithmetic

	static IntOrLong add(int x, int y)
	{
		// TODO: check performance of pre- vs. postcondition
		long r = (long)x + (long)y;
		return valueOf(r);
	}

	static ExactInteger add(long x, long y)
	{
		// TODO: check performance of pre- vs. postcondition
		long r = x + y;
		return isOverflowPostAdd(x, y, r) ? new ExactBigInteger(BigInteger.valueOf(x).add(BigInteger.valueOf(y)))
		                                  : valueOf(r);
	}

	static IntOrLong subtract(int x, int y)
	{
		// TODO: check performance of pre- vs. postcondition
		long r = (long)x - (long)y;
		return valueOf(r);
	}

	static ExactInteger subtract(long x, long y)
	{
		// TODO: check performance of pre- vs. postcondition
		long r = x - y;
		return isOverflowPostSubtract(x, y, r) ? new ExactBigInteger(BigInteger.valueOf(x).subtract(BigInteger.valueOf(y)))
		                                       : valueOf(r);
	}

	static IntOrLong multiply(int x, int y)
	{
		long r = (long)x * (long)y;
		return valueOf(r);
	}

	static ExactInteger multiply(long x, long y)
	{
		long r = x * y;
		return isOverflowPostMultiply(x, y, r) ? new ExactBigInteger(BigInteger.valueOf(x).multiply(BigInteger.valueOf(y)))
		                                       : valueOf(r);
	}

	static IntOrLong divide(int x, int y)
	{
		return isOverflowDivide(x, y) ? new ExactLong((long)x / (long)y)
		                              : valueOf(x / y);
	}

	static ExactInteger divide(long x, long y)
	{
		return isOverflowDivide(x, y) ? new ExactBigInteger(BigInteger.valueOf(x).divide(BigInteger.valueOf(y)))
		                              : valueOf(x / y);
	}

	static IntOrLong negate(int x)
	{
		return isOverflowNegate(x) ? new ExactLong(-(long)x)
		                           : valueOf(-x);
	}

	static ExactInteger negate(long x)
	{
		return isOverflowNegate(x) ? new ExactBigInteger(BigInteger.valueOf(x).negate())
		                           : valueOf(-x);
	}

	static IntOrLong abs(int x)
	{
		return (x < 0) ? negate(x) : new ExactInt(x);
	}

	static ExactInteger abs(long x)
	{
		return (x < 0) ? negate(x) : valueOf(x);
	}

	public static IntOrLong gcd(int x, int y)
	{
		return abs(gcdUnsafe(x, y));
	}

	public static ExactInteger gcd(long x, long y)
	{
		return abs(gcdUnsafe(x, y));
	}

	private static int gcdUnsafe(int x, int y)
	{
		while (0 != y) {
			int tmp = y;
			y = x % y;
			x = tmp;
		}
		return x;
	}

	private static long gcdUnsafe(long x, long y)
	{
		while (0L != y) {
			long tmp = y;
			y = x % y;
			x = tmp;
		}
		return x;
	}

	public static ExactInteger pow(long base, int exponent)
	{
		if (exponent < 0) {
			throw new ArithmeticException("negative exponent");
		}
		if (base == -1L) {
			return (exponent & 1) != exponent ? NEGATIVE_ONE : ONE;
		}
		if (base == 0L) {
			return ZERO;
		}
		if (base == 1L || exponent == 0) {
			return ONE;
		}
		if (exponent == 1) {
			return ExactInteger.valueOf(base);
		}
		if ((base & (base - 1L)) == 0L || (-base & (-base - 1L)) == 0L) {
			return powOfTwo(base, exponent);
		}
		return powIterative(base, exponent);
	}

	private static ExactInteger powOfTwo(long base, int exponent)
	{
		assert (base & (base - 1)) == 0L || (-base & (-base - 1L)) == 0L : "base must be a power of two";
		// power of two: (2^n)^e = 2^(n*e)
		long exp = (long)Long.numberOfTrailingZeros(base) * (long) exponent;
		if (exp > Integer.MAX_VALUE) {
			return ExactInteger.valueOf(BigInteger.TWO.pow(exponent));
		}
		if (base < 0L && (exponent & 1) == 1) {
			// negative result
			if (exp <= 63) {
				return ExactInteger.valueOf(-1L << exp);
			}
			return new ExactBigInteger(BIG_NEGATIVE_ONE.shiftLeft((int)exp));
		} else if (exp <= 62) {
			// positive result
			return ExactInteger.valueOf(1L << exp);
		}
		return new ExactBigInteger(BigInteger.ONE.shiftLeft((int)exp));
	}

	private static ExactInteger powIterative(long base, int exponent)
	{
		assert Math.abs(base) > 1L && exponent > 0;
		long result = (exponent & 1) == 1 ? base : 1L;
		exponent >>= 1;
		while (exponent > 0) {
			long maybe = base * base;
			if (isOverflowPostMultiply(base, base, maybe)) {
				BigInteger bigResult = BigInteger.valueOf(result);
				BigInteger bigPower = BigInteger.valueOf(base);
				return new ExactBigInteger(bigResult.multiply(bigPower.pow(exponent <<1)));
			} else {
				base = maybe;
			}
			if ((exponent & 1) == 1) {
				maybe = result * base;
				if (isOverflowPostMultiply(result, base, maybe)) {
					BigInteger bigPower = BigInteger.valueOf(base);
					BigInteger bigResult = BigInteger.valueOf(result).multiply(bigPower);
					return new ExactBigInteger(bigResult.multiply(bigPower.pow(exponent >>1)));
				} else {
					result = maybe;
				}
			}
			exponent >>= 1;
		}
		return ExactInteger.valueOf(result);
	}

	static ExactInteger shiftLeft(long x, int n)
	{
		if (x == 0 | n < -64) {
			return ExactInteger.ZERO;
		}
		if (n < 0) {
			return ExactInteger.valueOf(x >> -n);
		}
		if (n <= 63) { // cover x=-1
			// TODO: is number of leading zeros slow? Alternative:
			//    unsigned n = 0;
			//    if (x == 0) return sizeof(x) * 8;
			//    while (1) {
			//        if (x < 0) break;
			//        n ++;
			//        x <<= 1;
			//    }
			//    return n;
			int maxShift = Long.numberOfLeadingZeros((x > 0) ? x : x ^ -1L) - 1;
			int diff = maxShift - n;
			if (32 <= diff) {
				return new ExactInt((int)x << n);
			}
			if (0 <= diff) {
				return new ExactLong(x << n);
			}
		}
		return new ExactBigInteger(BigInteger.valueOf(x).shiftLeft(n));
	}

	/**
	 * @see Math#addExact(int, int)
	 */
	static int addExact(int x, int y)
	{
		// TODO: compare to performance of #isInt(long);
		return Math.addExact(x, y);
	}

	/**
	 * @see Math#addExact(long, long)
	 */
	static long addExact(long x, long y)
	{
		return Math.addExact(x, y);
	}

	/**
	 * @see Math#subtractExact(int, int)
	 */
	static int subtractExact(int x, int y)
	{
		// TODO: Compare to performance of #isInt(long);
		return Math.subtractExact(x, y);
	}

	/**
	 * @see Math#subtractExact(long, long)
	 */
	static long subtractExact(long x, long y)
	{
		return Math.subtractExact(x, y);
	}

	/**
	 * @see Math#multiplyExact(int, int)
	 */
	static int multiplyExact(int x, int y)
	{
		return Math.multiplyExact(x, y);
	}

	/**
	 * @see Math#multiplyExact(long, long)
	 */
	static long multiplyExact(long x, long y)
	{
		return Math.multiplyExact(x, y);
	}

	static int divideExact(int x, int y)
	{
		if (isOverflowDivide(x, y)) {
			throw new ArithmeticException("integer Overflow");
		}
		return x / y;
	}

	static long divideExact(long x, long y)
	{
		if (isOverflowDivide(x, y)) {
			throw new ArithmeticException("long Overflow");
		}
		return x / y;
	}

	/**
	 * @see Math#negateExact(int)
	 */
	static int negateExact(int x)
	{
		return Math.negateExact(x);
	}

	/**
	 * @see Math#negateExact(long)
	 */
	static long negateExact(long x)
	{
		return Math.negateExact(x);
	}

	/**
	 * Reimplement {@link Math#absExact(int)} to throw common exception.
	 */
	static int absExact(int x)
	{
		return (x < 0) ? negateExact(x) : x;
	}

	/**
	 * Reimplement {@link Math#absExact(long)} to throw common exception.
	 */
	static long absExact(long x)
	{
		return (x < 0) ? negateExact(x) : x;
	}

	public static int gcdExact(int x, int y)
	{
		return absExact(gcdUnsafe(x, y));
	}

	public static long gcdExact(long x, long y)
	{
		return absExact(gcdUnsafe(x, y));
	}

	public static int powExact(int base, int exponent)
	{
		if (exponent < 0) {
			throw new ArithmeticException("negative exponent");
		}
		if (base == -1) {
			return (exponent & 1) != exponent ? -1 : 1;
		}
		if (base == 0) {
			return 0;
		}
		if (base == 1 || exponent == 0) {
			return 1;
		}
		if ((base & (base - 1)) == 0 || (-base & (-base - 1)) == 0) {
			return powOfTwoExact(base, exponent);
		}
		return powIterativeExact(base, exponent);
	}

	private static int powOfTwoExact(int base, int exponent)
	{
		assert (base & (base - 1)) == 0 || (-base & (-base - 1)) == 0 : "base must be a power of two";
		// power of two: (2^n)^e = 2^(n*e)
		long exp = (long)Integer.numberOfTrailingZeros(base) * (long) exponent;
		if (base < 0 && (exponent & 1) == 1) {
			// negative result
			if (exp <= 31) {
				return -1 << (int)exp;
			}
		} else if (exp <= 30) {
			// positive result
			return 1 << (int)exp;
		}
		throw new ArithmeticException("integer overflow");
	}

	private static int powIterativeExact(int base, int exponent)
	{
		assert Math.abs(base) > 1 && exponent > 0;
		long result = (exponent & 1) == 1 ? base : 1L;
		exponent >>= 1;
		while (exponent > 0) {
			base *= base;
			if ((int)base != base) {
				throw new ArithmeticException("integer overflow");
			}
			if ((exponent & 1) == 1) {
				result *= base;
				if ((int)result != result) {
					throw new ArithmeticException("integer overflow");
				}
			}
			exponent >>= 1;
		}
		return (int)result;
	}

	public static long powExact(long base, int exponent)
	{
		if (exponent < 0) {
			throw new ArithmeticException("negative exponent");
		}
		if (base == -1L) {
			return (exponent & 1) != exponent ? -1 : 1;
		}
		if (base == 0L) {
			return 0L;
		}
		if (base == 1L || exponent == 0) {
			return 1L;
		}
		if ((base & (base - 1L)) == 0L || (-base & (-base - 1L)) == 0L) {
			return powOfTwoExact(base, exponent);
		}
		return powIterativeExact(base, exponent);
	}

	private static long powOfTwoExact(long base, int exponent)
	{
		assert (base & (base - 1L)) == 0L || (-base & (-base - 1L)) == 0L : "base must be a power of two";
		// power of two: (2^n)^e = 2^(n*e)
		long exp = (long)Long.numberOfTrailingZeros(base) * (long) exponent;
		if (base < 0L && (exp & 1) == 1) {
			// negative result
			if (exp <= 63) {
				return -1L << exp;
			}
		} else if (exp <= 62) {
			// positive result
			return 1L << exp;
		}
		throw new ArithmeticException("long overflow");
	}

	private static long powIterativeExact(long base, int exponent)
	{
		assert Math.abs(base) > 1L && exponent > 0;
		long result = (exponent & 1) == 1 ? base : 1L;
		exponent >>= 1;
		while (exponent > 0) {
			base = multiplyExact(base, base);
			if ((exponent & 1) == 1) {
				result = multiplyExact(result, base);
			}
			exponent >>= 1;
		}
		return result;
	}

	public static int shiftLeftExact(int x, int n)
	{
		if (x == 0 | n < -32) {
			return 0;
		}
		if (n < 0) {
			return x >> -n;
		}
		if (n <= 30) { // cover x=-1
			long l = (long)x << n;
			int r = (int)l;
			if (l == r) {
				return r;
			}
		}
		throw new ArithmeticException("integer overflow");
	}

	public static long shiftLeftExact(long x, int n)
	{
		if (x == 0 | n < -64) {
			return 0L;
		}
		if (n <= 63) { // cover x=-1
			int maxShift = Long.numberOfLeadingZeros((x > 0) ? x : x ^ -1L) - 1;
			if (n <= maxShift) {
				return x << n;
			}
		}
		throw new ArithmeticException("long overflow");
	}

	// Preconditions

	static boolean isOverflowAdd(int x, int y)
	{
		return y > 0 ? x > Integer.MAX_VALUE - y
                     : x < Integer.MIN_VALUE - y;
	}

	static boolean isOverflowAdd(long x, long y)
	{
		return y > 0L ? x > Long.MAX_VALUE - y
		              : x < Long.MIN_VALUE - y;
	}

	static boolean isOverflowSubtract(int x, int y)
	{
		return y > 0 ? x < Integer.MIN_VALUE + y
		             : x > Integer.MAX_VALUE + y;
	}

	static boolean isOverflowSubtract(long x, long y)
	{
		return y > 0L ? x < Long.MIN_VALUE + y
		              : x > Long.MAX_VALUE + y;
	}

	/**
	 * Slower than {@link ExactInteger#isOverflowPostMultiply(int, int, int)}.
	 *
	 * @see Math#multiplyExact(int, int)
	 */
	static boolean isOverflowMultiply(int x, int y)
	{
		return y > 0 ? x > Integer.MAX_VALUE / y ||
		               x < Integer.MIN_VALUE / y
		             : (y < -1 ? x > Integer.MIN_VALUE / y ||
		                         x < Integer.MAX_VALUE / y
		                       : y == -1 &&
		                         x == Integer.MIN_VALUE);
	}

	/**
	 * Slower than {@link ExactInteger#isOverflowPostMultiply(long, long, long)}.
	 *
	 * @see Math#multiplyExact(long, long)
	 */
	static boolean isOverflowMultiply(long x, long y)
	{
		return y > 0L ? x > Long.MAX_VALUE / y ||
		                x < Long.MIN_VALUE / y
		              : (y < -1L ? x > Long.MIN_VALUE / y ||
		                           x < Long.MAX_VALUE / y
		                         : y == -1L &&
		                           x == Long.MIN_VALUE);
	}

	static boolean isOverflowDivide(int x, int y)
	{
		return (x == Integer.MIN_VALUE) && (y == -1);
	}

	static boolean isOverflowDivide(long x, long y)
	{
		return (x == Long.MIN_VALUE) && (y == -1L);
	}

	static boolean isOverflowNegate(int x)
	{
		return x == Integer.MIN_VALUE;
	}

	static boolean isOverflowNegate(long x)
	{
		return x == Long.MIN_VALUE;
	}

	static boolean isOverflowAbs(int x)
	{
		return x == Integer.MIN_VALUE;
	}

	static boolean isOverflowAbs(long x)
	{
		return x == Long.MIN_VALUE;
	}

	// Postconditions

	/**
	 * @see Math#addExact(int, int)
	 */
	static boolean isOverflowPostAdd(int x, int y, int r)
	{
		// HD 2-12 Overflow iff both arguments have the opposite sign of the result
		return ((x ^ r) & (y ^ r)) < 0;
	}

	/**
	 * @see Math#addExact(long, long)
	 */
	static boolean isOverflowPostAdd(long x, long y, long r)
	{
		// HD 2-12 Overflow iff both arguments have the opposite sign of the result
		return ((x ^ r) & (y ^ r)) < 0;
	}

	/**
	 * @see Math#subtractExact(int, int)
	 */
	static boolean isOverflowPostSubtract(int x, int y, int r)
	{
		// HD 2-12 Overflow iff the arguments have different signs and
		// the sign of the result is different from the sign of x
		return ((x ^ y) & (x ^ r)) < 0;
	}

	/**
	 * @see Math#subtractExact(long, long)
	 */
	static boolean isOverflowPostSubtract(long x, long y, long r)
	{
		// HD 2-12 Overflow iff the arguments have different signs and
		// the sign of the result is different from the sign of x
		return ((x ^ y) & (x ^ r)) < 0L;
	}

	/**
	 * Faster than {@link ExactInteger#isOverflowMultiply(int, int)}.
	 *
	 * @see Math#multiplyExact(int, int)
	 */
	static boolean isOverflowPostMultiply(int x, int y, int r)
	{
		long l = (long)x * (long)y;
		return l != r;
	}

	/**
	 * Faster than {@link ExactInteger#isOverflowMultiply(long, long)}.
	 *
	 * @see Math#multiplyExact(long, long)
	 */
	static boolean isOverflowPostMultiply(long x, long y, long r)
	{
		long ax = Math.abs(x);
		long ay = Math.abs(y);
		if (((ax | ay) >>> 31 != 0)) {
			// Some bits greater than 2^31 that might cause overflow
			// Check the result using the divide operator
			// and check for the special case of Long.MIN_VALUE * -1
			if (((y != 0) && (r / y != x)) ||
					(x == Long.MIN_VALUE && y == -1)) {
				return true;
			}
		}
		return false;
	}
}