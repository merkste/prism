package param;

import common.StopWatch;
import prism.PrismPrintStreamLog;

import java.math.BigInteger;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

// TODO: benchmark pre- vs. postcondition testing
/**
 * https://wiki.sei.cmu.edu/confluence/display/java/NUM00-J.+Detect+or+prevent+integer+overflow
 */
public interface ExactInteger
{
	static final long LONG_INT_MIN_VALUE = Integer.MIN_VALUE;
	static final long LONG_INT_MAX_VALUE = Integer.MAX_VALUE;
	static final BigInteger INT_MIN_VALUE = BigInteger.valueOf(LONG_INT_MIN_VALUE);
	static final BigInteger INT_MAX_VALUE = BigInteger.valueOf(LONG_INT_MAX_VALUE);
	static final BigInteger LONG_MIN_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
	static final BigInteger LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

	static final ExactInt ZERO = new ExactInt(0);
	static final ExactInt ONE = new ExactInt(1);

	static boolean fitsInt(long x)
	{
		// equally fast to comparision with range
		return (int)x == x;
	}

	static boolean fitsInt(BigInteger x)
	{
		// faster to x.equals(x.intValue());
		return INT_MIN_VALUE.compareTo(x) <= 0 && x.compareTo(INT_MAX_VALUE) <= 0;
	}

	private static boolean fitsLong(BigInteger x)
	{
		// faster to x.equals(x.intValue());
		return LONG_MIN_VALUE.compareTo(x) <= 0 && x.compareTo(LONG_MAX_VALUE) <= 0;
	}

	public static ExactInteger valueOf(int x)
	{
		return new ExactInt(x);
	}

	public static ExactInteger valueOf(long x)
	{
		int intX = (int)x;
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

	default boolean fitsBigInteger()
	{
		return true;
	}

	ExactInteger compact();

	int intValueExact();

	long longValueExact();

	BigInteger bigIntegerValue();

	default ExactInteger ifInt(IntConsumer intAction)
	{
		return this;
	}

	default ExactInteger ifLong(LongConsumer longAction)
	{
		return this;
	}

	default ExactInteger ifBigInteger(Consumer<BigInteger> bigIntegerAction)
	{
		return this;
	}

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
		return fitsLong(x) && !x.equals(LONG_MIN_VALUE) ? Long.hashCode(x.longValue()) : x.hashCode();
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

	public class ExactInt implements IntOrLong
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
		public ExactInteger compact()
		{
			return this;
		}

		@Override
		public int intValueExact()
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
		public ExactInt ifInt(IntConsumer intAction)
		{
			intAction.accept(x);
			return this;
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
		public LongOrBigInteger add(long summand)
		{
			// TODO: Fit smaller class?
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
		public LongOrBigInteger subtract(long subtrahend)
		{
			// TODO: Fit smaller class?
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
		public LongOrBigInteger subtractFrom(long minuend)
		{
			// TODO: Fit smaller class?
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
		public LongOrBigInteger multiply(long factor)
		{
			// TODO: Fit smaller class?
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
		public LongOrBigInteger divide(long divisor)
		{
			// TODO: Fit smaller class?
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
		public LongOrBigInteger divideDividend(long dividend)
		{
			// TODO: Fit smaller class?
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
		public LongOrBigInteger gcd(long y)
		{
			// TODO: Fit smaller class?
			return ExactInteger.gcd(x, y);
		}

		@Override
		public ExactInteger gcd(ExactInteger y)
		{
			return y.gcd(x);
		}

		@Override
		public String toString()
		{
			return String.valueOf(x);
		}
	}



	public class ExactLong implements IntOrLong, LongOrBigInteger
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
		public ExactInteger compact()
		{
			return fitsInt() ? valueOf((int)x) : this;
		}

		@Override
		public int intValueExact()
		{
			return Math.toIntExact(x);
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
		public ExactLong ifLong(LongConsumer longAction)
		{
			longAction.accept(x);
			return this;
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
		public LongOrBigInteger add(int summand)
		{
			// TODO: Fit smaller class?
			return ExactInteger.add(x, summand);
		}

		@Override
		public LongOrBigInteger add(long summand)
		{
			// TODO: Fit smaller class?
			return ExactInteger.add(x, summand);
		}

		@Override
		public ExactInteger add(ExactInteger summand)
		{
			return summand.add(x);
		}

		@Override
		public LongOrBigInteger subtract(int subtrahend)
		{
			// TODO: Fit smaller class?
			return ExactInteger.subtract(x, subtrahend);
		}

		@Override
		public LongOrBigInteger subtract(long subtrahend)
		{
			// TODO: Fit smaller class?
			return ExactInteger.subtract(x, subtrahend);
		}

		@Override
		public ExactInteger subtract(ExactInteger subtrahend)
		{
			return subtrahend.subtractFrom(x);
		}

		@Override
		public LongOrBigInteger subtractFrom(int minuend)
		{
			// TODO: Fit smaller class?
			return ExactInteger.subtract(minuend, x);
		}

		@Override
		public LongOrBigInteger subtractFrom(long minuend)
		{
			// TODO: Fit smaller class?
			return ExactInteger.subtract(minuend, x);
		}

		@Override
		public ExactInteger subtractFrom(ExactInteger minuend)
		{
			return minuend.subtract(x);
		}

		@Override
		public LongOrBigInteger multiply(int factor)
		{
			// TODO: Fit smaller class?
			return ExactInteger.multiply(x, factor);
		}

		@Override
		public LongOrBigInteger multiply(long factor)
		{
			// TODO: Fit smaller class?
			return ExactInteger.multiply(x, factor);
		}

		@Override
		public ExactInteger multiply(ExactInteger factor)
		{
			return factor.multiply(x);
		}

		@Override
		public LongOrBigInteger divide(int divisor)
		{
			// TODO: Fit smaller class?
			return ExactInteger.divide(x, divisor);
		}

		@Override
		public LongOrBigInteger divide(long divisor)
		{
			// TODO: Fit smaller class?
			return ExactInteger.divide(x, divisor);
		}

		@Override
		public ExactInteger divide(ExactInteger divisor)
		{
			return divisor.divideDividend(x);
		}

		@Override
		public LongOrBigInteger divideDividend(int dividend)
		{
			// TODO: Fit smaller class?
			return ExactInteger.divide(dividend, x);
		}

		@Override
		public LongOrBigInteger divideDividend(long dividend)
		{
			// TODO: Fit smaller class?
			return ExactInteger.divide(dividend, x);
		}

		@Override
		public ExactInteger divideDividend(ExactInteger dividend)
		{
			return dividend.divide(x);
		}

		@Override
		public LongOrBigInteger negate()
		{
			return ExactInteger.negate(x);
		}

		@Override
		public LongOrBigInteger abs()
		{
			return ExactInteger.abs(x);
		}

		@Override
		public LongOrBigInteger gcd(int y)
		{
			// TODO: Fit smaller class?
			return ExactInteger.gcd(x, y);
		}

		@Override
		public LongOrBigInteger gcd(long y)
		{
			// TODO: Fit smaller class?
			return ExactInteger.gcd(x, y);
		}

		@Override
		public ExactInteger gcd(ExactInteger y)
		{
			return y.gcd(x);
		}

		@Override
		public String toString()
		{
			return String.valueOf(x);
		}
	}



	public class ExactBigInteger implements LongOrBigInteger
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
		public ExactInteger compact()
		{
			return fitsLong() ? valueOf(x.longValue()) : this;
		}

		@Override
		public int intValueExact()
		{
			return x.intValueExact();
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
		public ExactBigInteger ifBigInteger(Consumer<BigInteger> bigIntegerAction)
		{
			bigIntegerAction.accept(x);
			return this;
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
		public String toString()
		{
			return String.valueOf(x);
		}
	}



	// Arithmetic

	static IntOrLong add(int x, int y)
	{
		long r = (long)x + (long)y;
		return ExactInteger.fitsInt(r) ? new ExactInt((int)r) : new ExactLong(r);
	}

	static LongOrBigInteger add(long x, long y)
	{
		long r = x + y;
		return isOverflowPostAdd(x, y, r) ? new ExactBigInteger(BigInteger.valueOf(x).add(BigInteger.valueOf(y)))
		                                  : new ExactLong(r);
	}

	static IntOrLong subtract(int x, int y)
	{
		long r = (long)x - (long)y;
		return ExactInteger.fitsInt(r) ? new ExactInt((int)r) : new ExactLong(r);
	}

	static LongOrBigInteger subtract(long x, long y)
	{
		long r = x - y;
		return isOverflowPostSubtract(x, y, r) ? new ExactBigInteger(BigInteger.valueOf(x).subtract(BigInteger.valueOf(y)))
		                                       : new ExactLong(r);
	}

	static IntOrLong multiply(int x, int y)
	{
		long r = (long)x * (long)y;
		return ExactInteger.fitsInt(r) ? new ExactInt((int)r) : new ExactLong(r);
	}

	static LongOrBigInteger multiply(long x, long y)
	{
		long r = x * y;
		return isOverflowPostMultiply(x, y, r) ? new ExactBigInteger(BigInteger.valueOf(x).multiply(BigInteger.valueOf(y)))
		                                       : new ExactLong(r);
	}

	static IntOrLong divide(int x, int y)
	{
		return isOverflowDivide(x, y) ? new ExactLong((long)x / (long)y)
		                              : new ExactInt(x / y);
	}

	static LongOrBigInteger divide(long x, long y)
	{
		return isOverflowDivide(x, y) ? new ExactBigInteger(BigInteger.valueOf(x).divide(BigInteger.valueOf(y)))
		                              : new ExactLong(x / y);
	}

	static IntOrLong negate(int x)
	{
		return isOverflowNegate(x) ? new ExactLong(-(long)x)
		                           : new ExactInt(-x);
	}

	static LongOrBigInteger negate(long x)
	{
		return isOverflowNegate(x) ? new ExactBigInteger(BigInteger.valueOf(x).negate())
		                           : new ExactLong(-x);
	}

	static IntOrLong abs(int x)
	{
		return (x < 0) ? negate(x) : new ExactInt(x);
	}

	static LongOrBigInteger abs(long x)
	{
		return (x < 0) ? negate(x) : new ExactLong(x);
	}

	public static IntOrLong gcd(int x, int y)
	{
		return abs(gcdUnsafe(x, y));
	}

	public static LongOrBigInteger gcd(long x, long y)
	{
		return abs(gcdUnsafe(x, y));
	}

	static OptionalInt addOptional(int x, int y)
	{
		// TODO: compare to #isInt(long);
		int r = x + y;
		return isOverflowPostAdd(x, y, r) ? OptionalInt.empty() : OptionalInt.of(r);
	}

	static OptionalLong addOptional(long x, long y)
	{
		long r = x + y;
		return isOverflowPostAdd(x, y, r) ? OptionalLong.empty() : OptionalLong.of(r);
	}

	static OptionalInt subtractOptional(int x, int y)
	{
		// TODO: compare to #isInt(long);
		int r = x - y;
		return isOverflowPostSubtract(x, y, r) ? OptionalInt.empty() : OptionalInt.of(r);
	}

	static OptionalLong subtractOptional(long x, long y)
	{
		long r = x - y;
		return isOverflowPostSubtract(x, y, r) ? OptionalLong.empty() : OptionalLong.of(r);
	}

	static OptionalInt multiplyOptional(int x, int y)
	{
		// Inline #isOverflowPostMultiply to avoid additional multiplication
		long l = (long)x * (long)y;
		int r = (int)l;
		return l != r ? OptionalInt.empty() : OptionalInt.of(r);
	}

	static OptionalLong multiplyOptional(long x, long y)
	{
		long r = x * y;
		return isOverflowPostMultiply(x, y, r) ? OptionalLong.empty() : OptionalLong.of(r);
	}

	static OptionalInt divideOptional(int x, int y)
	{
		return isOverflowDivide(x, y) ? OptionalInt.empty() : OptionalInt.of(x / y);
	}

	static OptionalLong divideOptional(long x, long y)
	{
		return isOverflowDivide(x, y) ? OptionalLong.empty() : OptionalLong.of(x / y);
	}

	static OptionalInt negateOptional(int x)
	{
		return isOverflowNegate(x) ? OptionalInt.empty() : OptionalInt.of(-x);
	}

	static OptionalLong negateOptional(long x)
	{
		return isOverflowNegate(x) ? OptionalLong.empty() : OptionalLong.of(-x);
	}

	static OptionalInt absOptional(int x)
	{
		return (x < 0) ? negateOptional(x) : OptionalInt.of(x);
	}

	static OptionalLong absOptional(long x)
	{
		return (x < 0) ? negateOptional(x) : OptionalLong.of(x);
	}

	public static OptionalInt gcdOptional(int x, int y)
	{
		return absOptional(gcdUnsafe(x, y));
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

	public static OptionalLong gcdOptional(long x, long y)
	{
		return absOptional(gcdUnsafe(x, y));
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

	/**
	 * @see Math#addExact(int, int)
	 */
	static int addExact(int x, int y)
	{
		// TODO: compare to #isInt(long);
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
		return ((x ^ y) & (x ^ r)) < 0;
	}

	/**
	 * @see Math#subtractExact(long, long)
	 */
	static boolean isOverflowPostSubtract(long x, long y, long r)
	{
		return ((x ^ y) & (x ^ r)) < 0L;
	}

	/**
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



	public static void main(String[] arg)
	{
		benchmarkIsOverflow();
//		benchmarkIsIntLong();
//		benchmarkIsIntBigInteger();
//		benchmarkIsLongBigInteger();
	}

	public static void benchmarkIsOverflow()
	{
		StopWatch watch = new StopWatch(new PrismPrintStreamLog(System.out));
		long b = 40000;
		long n = b*b*1;

		long xSmallLong = 1;
//		long ySmallLong = 2;
		long xLargeLong = Long.MAX_VALUE - n/2;
//		long yLargeLong = Long.MIN_VALUE + n/2;

		for (int i=0; i<4; i++) {
			watch.run(() -> runIsOverflowMultiply(xSmallLong, n), "precondition multiply(long)", "small");
			watch.run(() -> runIsOverflowMultiply(xLargeLong, n), "precondition multiply(long)", "large");
			watch.run(() -> runIsOverflowPostMultiply(xSmallLong, n), "postcondition multiply(long)", "small");
			watch.run(() -> runIsOverflowPostMultiply(xLargeLong, n), "postcondition multiply(long)", "large");
		}
	}

	public static void benchmarkIsIntLong()
	{
		StopWatch watch = new StopWatch(new PrismPrintStreamLog(System.out));
		int n = 8;
		Boolean bC = watch.run(() -> runIsIntLongCompare(n), "isInt(long)", "(compare)");
		Boolean bE = watch.run(() -> runIsIntLongEquals(n), "isInt(long)", "(equals)");
	}

	public static void benchmarkIsIntBigInteger()
	{
		StopWatch watch = new StopWatch(new PrismPrintStreamLog(System.out));
		int n = 1;
		Boolean bC = watch.run(() -> runIsIntBigIntegerCompare(n), "isInt(BigInteger)", "(compare)");
		Boolean bE = watch.run(() -> runIsIntBigIntegerEquals(n), "isInt(BigInteger)", "(equals)");
	}

	public static void benchmarkIsLongBigInteger()
	{
		StopWatch watch = new StopWatch(new PrismPrintStreamLog(System.out));
		int n = 1;
		Boolean bA = watch.run(() -> runIsLongBigIntegerCompare(n), "isLong(BigInteger)", "(compare)");
		Boolean bE = watch.run(() -> runIsLongBigIntegerEquals(n), "isLong(BigInteger)", "(equals)");
	}

	private static long runIsOverflowMultiply(long x, long n)
	{
		long r = 0L;
		for (long i = 0; i< n; i++) {
			long y=x+i;
			isOverflowMultiply(x, y);
			r = x * y;
		}
		return r;
	}

	private static long runIsOverflowPostMultiply(long x, long n)
	{
		long r = 0L;
		for (long i=0; i<n; i++) {
			long y=x+i;
			r = x * y;
			isOverflowPostMultiply(x, y, r);
		}
		return r;
	}

	private static boolean runIsIntLongCompare(int r)
	{
		boolean result = false;
		final long range = Integer.MAX_VALUE;
		final long start = Integer.MAX_VALUE - range;
		for (; r>0; r--) {
			for (long j=range; j >=0L; j--) {
				for (int s=-1; s<= 1; s+=2){
					long longN = s<0 ? -(start + j) : (start + j);
					result = LONG_INT_MIN_VALUE <= longN && longN <= LONG_INT_MAX_VALUE;
				}
			}
		}
		return result;
	}


	private static boolean runIsIntLongEquals(int r)
	{
		boolean result = false;
		final long range = Integer.MAX_VALUE;
		final long start = Integer.MAX_VALUE - range;
		for (; r>0; r--) {
			for (long j=range; j >=0L; j--) {
				for (int s=-1; s<= 1; s+=2){
					long longN = s<0 ? -(start + j) : (start + j);
					int intN = (int)longN;
					result = intN == longN;
				}
			}
		}
		return result;
	}

	private static boolean runIsIntBigIntegerCompare(int r)
	{
		boolean result = false;
		final long range = Integer.MAX_VALUE;
		final long start = Integer.MAX_VALUE - range;
		for (; r>0; r--) {
			for (long j=range; j >=0L; j--) {
				for (int s=-1; s<= 1; s+=2) {
					BigInteger bigN = s<0 ? BigInteger.valueOf(-(start + j))
							: BigInteger.valueOf(start + j);
//					result = INT_MIN_VALUE.compareTo(bigN) <= 0 && bigN.compareTo(INT_MAX_VALUE) <= 0;
					result = INT_MIN_VALUE.compareTo(bigN) <= 0 && INT_MAX_VALUE.compareTo(bigN) >= 0;
//					result = bigN.compareTo(INT_MIN_VALUE) >= 0 && bigN.compareTo(INT_MAX_VALUE) <= 0;
				}
			}
		}
		return result;
	}

	private static boolean runIsIntBigIntegerEquals(int r)
	{
		boolean result = false;
		final long range = Integer.MAX_VALUE;
		final long start = Integer.MAX_VALUE - range;
		for (; r>0; r--) {
			for (long j=range; j >=0L; j--) {
				for (int s=-1; s<= 1; s+=2) {
					BigInteger bigN = s<0 ? BigInteger.valueOf(-(start + j))
							: BigInteger.valueOf(start + j);
					int intN = bigN.intValue();
					result = bigN.equals(BigInteger.valueOf(intN));
				}
			}
		}
		return result;
	}

	private static boolean runIsLongBigIntegerCompare(int r)
	{
		boolean result = false;
		final long range = Integer.MAX_VALUE;
		final long start = Integer.MAX_VALUE - range;
		for (; r>0; r--) {
			for (long j=range; j >=0L; j--) {
				for (int s=-1; s<= 1; s+=2) {
					BigInteger bigN = s < 0 ? BigInteger.valueOf(-start).add(BigInteger.valueOf(-j))
							: BigInteger.valueOf(start).add(BigInteger.valueOf(j));
					result = LONG_MIN_VALUE.compareTo(bigN) <= 0 && bigN.compareTo(LONG_MAX_VALUE) <= 0;
				}
			}
		}
		return result;
	}

	private static boolean runIsLongBigIntegerEquals(int r)
	{
		boolean result = false;
		final long range = Integer.MAX_VALUE;
		final long start = Integer.MAX_VALUE - range;
		for (; r>0; r--) {
			for (long j=range; j >=0L; j--) {
				for (int s=-1; s<= 1; s+=2) {
					BigInteger bigN = s < 0 ? BigInteger.valueOf(-start).add(BigInteger.valueOf(-j))
							: BigInteger.valueOf(start).add(BigInteger.valueOf(j));
					long longX = bigN.longValue();
					result = bigN.equals(BigInteger.valueOf(longX));
				}
			}
		}
		return result;
	}
}
