package param;

import common.StopWatch;
import prism.PrismPrintStreamLog;

import java.math.BigInteger;

// TODO: BigRational has bugs if fraction is not canceled, e.g., #isZero, #isOne, #compareTo, #equals, #hash
// TODO: Write generator for all rational numbers
// TODO: Check how to speed up #equals and #hash
// TODO: toDouble -> use round to denominator

public interface Fraction extends Comparable<Fraction>
{
	public static int gcd(int a, int b)
	{
		while (0 != b) {
			int tmp = b;
			b = a % b;
			a = tmp;
		}
		return Math.abs(a);
	}

	public static long gcd(long a, long b)
	{
		while (0L != b) {
			long tmp = b;
			b = a % b;
			a = tmp;
		}
		return Math.abs(a);
	}

	default int compareSignum(int sigB)
	{
		int sigA = signum();
		if (sigA < sigB) {// A < B
			return -1;
		}
		if (sigA > sigB) {// A > B
			return +1;
		}
		if (sigA == 0) {// A = B = 0
			return 0;
		}
		// A, B < 0 or 0 < A, B
		return -2;
	}

	public static Fraction valueOf(int numerator, int denominator)
	{
		return valueOf(numerator, denominator, true);
	}

	public static Fraction valueOf(int numerator, int denominator, boolean cancel)
	{
		if (cancel) {
			int gcd = gcd(numerator, denominator);
			if (gcd > 1) {
				numerator /= gcd;
				denominator /= gcd;
			}
		}
		return new SmallFraction(numerator, denominator);
	}

	public static Fraction valueOf(long numerator, long denominator)
	{
		return valueOf(numerator, denominator, true);
	}

	public static Fraction valueOf(long numerator, long denominator, boolean cancel)
	{
		if (cancel) {
			long gcd = gcd(numerator, denominator);
			if (gcd > 1) {
				numerator /= gcd;
				denominator /= gcd;
			}
		}
		if (isIntValue(numerator) && isIntValue(denominator))
		{
			return valueOf((int) numerator, (int) denominator, false);
		}
		return new MediumFraction(numerator, denominator);
	}

	private static boolean isIntValue(BigInteger n)
	{
		return INT_MIN_VALUE.compareTo(n) <= 0 && n.compareTo(INT_MAX_VALUE) <= 0;
	}

	private static boolean isIntValue(long n)
	{
		return Integer.MIN_VALUE <= n && n <= Integer.MAX_VALUE;
	}

	public static Fraction valueOf(BigInteger numerator, BigInteger denominator)
	{
		return valueOf(numerator, denominator, true);
	}

	public static Fraction valueOf(BigInteger numerator, BigInteger denominator, boolean cancel)
	{
//		if (isIntValue(numerator) && isIntValue(denominator))
//		{
//			return valueOf(numerator.intValue(), denominator.intValue(), cancel);
//		}
		if (isLongValue(numerator) && isLongValue(denominator))
		{
			return valueOf(numerator.longValue(), denominator.longValue(), cancel);
		}
		if (cancel) {
			BigInteger gcd = numerator.gcd(denominator);
			if (!gcd.equals(BigInteger.ONE)) {
				numerator = numerator.divide(gcd);
				denominator = denominator.divide(gcd);
			}
		}
//		if (isIntValue(numerator) && isIntValue(denominator))
//		{
//			return valueOf(numerator.intValue(), denominator.intValue(), cancel);
//		}
		if (isLongValue(numerator) && isLongValue(denominator))
		{
			return valueOf(numerator.longValue(), denominator.longValue(), false);
		}
		return new LargeFraction(numerator, denominator);
	}

	public static final BigInteger INT_MIN_VALUE = BigInteger.valueOf(Integer.MIN_VALUE);
	public static final BigInteger INT_MAX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);
	public static final BigInteger LONG_MIN_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
	public static final BigInteger LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

	private static boolean isLongValue(BigInteger n)
	{
		return LONG_MIN_VALUE.compareTo(n) <= 0 && n.compareTo(LONG_MAX_VALUE) <= 0;
	}

	private static boolean isInvertibleLongValue(BigInteger n)
	{
		return LONG_MIN_VALUE.compareTo(n) < 0 && n.compareTo(LONG_MAX_VALUE) <= 0;
	}

	int compareTo(Fraction other);

	int compareTo(SmallFraction other);

	int compareTo(MediumFraction other);

	int compareTo(LargeFraction other);

	boolean equals(Fraction other);

	boolean equals(SmallFraction other);

	boolean equals(MediumFraction other);

	boolean equals(LargeFraction other);

	Fraction cancel();

	int signum();

	Fraction negate();

	Fraction reciprocal();

	Fraction add(Fraction summand);

	Fraction add(SmallFraction summand);

	Fraction add(MediumFraction summand);

	Fraction add(LargeFraction summand);

	Fraction subtract(Fraction subtrahend);

	Fraction subtract(SmallFraction subtrahend);

	Fraction subtract(MediumFraction subtrahend);

	Fraction subtract(LargeFraction subtrahend);

	Fraction subtractFrom(Fraction minuend);

	Fraction subtractFrom(SmallFraction minuend);

	Fraction subtractFrom(MediumFraction minuend);

	Fraction subtractFrom(LargeFraction minuend);

	Fraction multiply(Fraction factor);

	Fraction multiply(SmallFraction factor);

	Fraction multiply(MediumFraction factor);

	Fraction multiply(LargeFraction factor);

	Fraction divide(Fraction divisor);

	Fraction divide(SmallFraction divisor);

	Fraction divide(MediumFraction divisor);

	Fraction divide(LargeFraction divisor);

	Fraction divideDividend(Fraction dividend);

	Fraction divideDividend(SmallFraction dividend);

	Fraction divideDividend(MediumFraction dividend);

	Fraction divideDividend(LargeFraction dividend);



	public static class SmallFraction implements Fraction
	{
		protected final int numerator;
		protected final int denominator;

		public SmallFraction(int numerator, int denominator)
		{
			if (denominator == 0) {
				throw new ArithmeticException("Division by zero");
			}
			this.numerator = numerator;
			this.denominator = denominator;
		}

		public SmallFraction cancel()
		{
			int gcd = gcd(numerator, denominator);
			return gcd == 1 ? this : new SmallFraction(numerator/gcd, denominator/gcd);
		}

		@Override
		public int signum()
		{
			return Integer.signum(numerator) * Integer.signum(denominator);
		}

		@Override
		public int compareTo(Fraction other)
		{
			return -other.compareTo(this);
		}

		@Override
		public int compareTo(SmallFraction other)
		{
			if (this == other) {
				return 0;
			}
			int cmp = compareSignum(other.signum());
			if (cmp > -2) {
				return cmp;
			}
			// Expand both fractions before comparing
			long numA = numerator * other.denominator;
			long numB = other.numerator * denominator;
			// Either both numbers are negated or none
			return Long.signum(numA) == signum() ? Long.compare(numA, numB) : Long.compare(numB, numA);
		}

		@Override
		public int compareTo(MediumFraction other)
		{
			return -other.compareTo(this);
		}

		@Override
		public int compareTo(LargeFraction other)
		{
			return -other.compareTo(this);
		}

		@Override
		public boolean equals(Object other)
		{
			if (other instanceof Fraction) {
				return equals((Fraction) other);
			}
			return false;
		}

		@Override
		public boolean equals(Fraction other)
		{
			return other != null && other.equals(this);
		}

		@Override
		public boolean equals(SmallFraction other)
		{
			return other != null && compareTo(other) == 0;
		}

		@Override
		public boolean equals(MediumFraction other)
		{
			return other != null && other.equals(this);
		}

		@Override
		public boolean equals(LargeFraction other)
		{
			return other != null && other.equals(this);
		}

		@Override
		public int hashCode()
		{
			return Fraction.hashCode(numerator, denominator, true);
		}

		@Override
		public Fraction negate()
		{
			if (numerator != Integer.MIN_VALUE) {
				return new SmallFraction(-numerator, denominator);
			}
			if (denominator != Integer.MIN_VALUE) {
				return new SmallFraction(numerator, -denominator);
			}
			return valueOf(- (long) numerator, (long) denominator, false);
		}

		@Override
		public Fraction reciprocal()
		{
			return numerator == denominator ? this : valueOf(numerator, denominator, false);
		}

		@Override
		public Fraction add(Fraction summand)
		{
			return summand.add(this);
		}

		@Override
		public Fraction add(SmallFraction summand)
		{
			return add(numerator, denominator, summand.numerator, summand.denominator, false);
		}

		private Fraction add(int numA, int denA, int numB, int denB, boolean subtract)
		{
			// Don't cancel beforehand, since none of the operations will overflow
			long sumA = (long) numA * denB;
			long sumB = (long) numB * denA;
			long den = (long) denA * denB;
			long sum = subtract ? sumA - sumB : sumA + sumB;
			return valueOf(sum, den);
		}

		@Override
		public Fraction add(MediumFraction summand)
		{
			return summand.add(this);
		}

		@Override
		public Fraction add(LargeFraction summand)
		{
			return summand.add(this);
		}

		@Override
		public Fraction subtract(Fraction subtrahend)
		{
			return subtrahend.subtractFrom(this);
		}

		@Override
		public Fraction subtract(SmallFraction subtrahend)
		{
			return add(numerator, denominator, subtrahend.numerator, subtrahend.denominator, true);
		}

		@Override
		public Fraction subtract(MediumFraction subtrahend)
		{
			return subtrahend.subtractFrom(this);
		}

		@Override
		public Fraction subtract(LargeFraction subtrahend)
		{
			return subtrahend.subtractFrom(this);
		}

		@Override
		public Fraction subtractFrom(Fraction minuend)
		{
			return minuend.subtract(this);
		}

		@Override
		public Fraction subtractFrom(SmallFraction minuend)
		{
			return add(minuend.numerator, minuend.denominator, numerator, denominator, true);
		}

		@Override
		public Fraction subtractFrom(MediumFraction minuend)
		{
			return minuend.subtractFrom(this);
		}

		@Override
		public Fraction subtractFrom(LargeFraction minuend)
		{
			return minuend.subtractFrom(this);
		}

		@Override
		public Fraction multiply(Fraction factor)
		{
			return factor.multiply(this);
		}

		@Override
		public Fraction multiply(SmallFraction factor)
		{
			return multiply(numerator, denominator, factor.numerator, factor.denominator);
		}

		private Fraction multiply(int numA, int denA, int numB, int denB)
		{
			// Don't cancel beforehand, since none of the operations will overflow
			long num = (long) numA * numB;
			long den = (long) denA * denB;
			return valueOf(num, den);
		}

		@Override
		public Fraction multiply(MediumFraction factor)
		{
			return factor.multiply(this);
		}

		@Override
		public Fraction multiply(LargeFraction factor)
		{
			return factor.multiply(this);
		}

		@Override
		public Fraction divide(Fraction divisor)
		{
			return divisor.divideDividend(this);
		}

		@Override
		public Fraction divide(SmallFraction divisor)
		{
			return multiply(numerator, denominator, divisor.denominator, divisor.numerator);
		}

		@Override
		public Fraction divide(MediumFraction divisor)
		{
			return divisor.divideDividend(this);
		}

		@Override
		public Fraction divide(LargeFraction divisor)
		{
			return divisor.divideDividend(this);
		}

		@Override
		public Fraction divideDividend(Fraction dividend)
		{
			return dividend.divide(this);
		}

		@Override
		public Fraction divideDividend(SmallFraction dividend)
		{
			return multiply(dividend.numerator, dividend.denominator, denominator, numerator);
		}

		@Override
		public Fraction divideDividend(MediumFraction dividend)
		{
			return dividend.divide(this);
		}

		@Override
		public Fraction divideDividend(LargeFraction dividend)
		{
			return dividend.divide(this);
		}

		@Override
		public String toString()
		{
			String sign = (signum() < 0) ? "-" : "";
			long absNum = Math.abs((long) numerator);
			long absDen = Math.abs((long) denominator);
			long gcd = gcd(absNum, absDen);
			if (gcd == absDen) {
				return sign + absNum / gcd;
			}
			return sign + absNum + "/" + absDen;
		}
	}



	public static class MediumFraction implements Fraction
	{
		protected final long numerator;
		protected final long denominator;

		public MediumFraction(long numerator, long denominator)
		{
			if (denominator == 0L) {
				throw new ArithmeticException("Division by zero");
			}
			this.numerator = numerator;
			this.denominator = denominator;
		}

		@Override
		public Fraction cancel()
		{
			long gcd = gcd(numerator, denominator);
			return gcd == 1L ? this : valueOf(numerator/gcd, denominator/gcd, false);
		}

		@Override
		public int signum()
		{
			return Long.signum(numerator) * Long.signum(denominator);
		}

		@Override
		public int compareTo(Fraction other)
		{
			return -other.compareTo(this);
		}

		@Override
		public int compareTo(SmallFraction other)
		{
			if (this == (Fraction) other) {
				return 0;
			}
			int cmp = compareSignum(other.signum());
			return cmp > -2 ? cmp : compareTo(other.numerator, other.denominator);
		}

		@Override
		public int compareTo(MediumFraction other)
		{
			if (this == other) {
				return 0;
			}
			int cmp = compareSignum(other.signum());
			return cmp > -2 ? cmp : compareTo(other.numerator, other.denominator);
		}

		@Override
		public int compareTo(LargeFraction other)
		{
			return -other.compareTo(this);
		}

		private int compareTo(long otherNum, long otherDen)
		{
			int sigA = signum();
			if (sigA != Long.signum(otherNum) * Long.signum(otherDen)) {
				throw new IllegalArgumentException("Signum of argument must match signum of receiver");
			}
			// Expand both fractions before comparing
			// TODO: check penalty of gcd
			long facA = otherDen;
			long facB = denominator;
//			long gcd = 1L;
			long gcd = gcd(denominator, otherDen);
			if (gcd > 1L) {
				facA /= gcd;
				facB /= gcd;
			}
			if (!(overflowMultiply(numerator, facA) || overflowMultiply(otherNum, facB))) {
				// Use long arithmetics
				long numA = numerator * facA;
				long numB = otherNum * facB;
				// Either both numbers are negated or none
				return Long.signum(numA) == sigA ? Long.compare(numA, numB) : Long.compare(numB, numA);
			}
			// Fall back to BigInteger arithmetic
			BigInteger numA = BigInteger.valueOf(numerator).multiply(BigInteger.valueOf(facA));
			BigInteger numB = BigInteger.valueOf(otherNum).multiply(BigInteger.valueOf(facB));
			// Either both numbers are negated or none
			return numA.signum() == sigA ? numA.compareTo(numB) : numB.compareTo(numA);
		}


		@Override
		public boolean equals(Object other)
		{
			if (other instanceof Fraction) {
				return equals((Fraction) other);
			}
			return false;
		}

		@Override
		public boolean equals(Fraction other)
		{
			return other != null && other.equals(this);
		}

		@Override
		public boolean equals(SmallFraction other)
		{
			return other != null && compareTo(other) == 0;
		}

		@Override
		public boolean equals(MediumFraction other)
		{
			return other != null && compareTo(other) == 0;
		}

		@Override
		public boolean equals(LargeFraction other)
		{
			return other != null && other.equals(this);
		}

		@Override
		public int hashCode()
		{
			return Fraction.hashCode(numerator, denominator, true);
		}

		@Override
		public Fraction negate()
		{
			if (numerator != Long.MIN_VALUE) {
				return new MediumFraction(-numerator, denominator);
			}
			if (denominator != Long.MIN_VALUE) {
				return new MediumFraction(numerator, -denominator);
			}
			return valueOf(BigInteger.valueOf(numerator).negate(), BigInteger.valueOf(denominator), false);
		}

		@Override
		public Fraction reciprocal()
		{
			return numerator == denominator ? this : valueOf(numerator, denominator, false);
		}

		@Override
		public Fraction add(Fraction summand)
		{
			return summand.add(this);
		}

		@Override
		public Fraction add(SmallFraction summand)
		{
			return add(numerator, denominator, summand.numerator, summand.denominator, false);
		}

		@Override
		public Fraction add(MediumFraction summand)
		{
			return add(numerator, denominator, summand.numerator, summand.denominator, false);
		}

		@Override
		public Fraction add(LargeFraction summand)
		{
			return summand.add(this);
		}

		// https://wiki.sei.cmu.edu/confluence/display/java/NUM00-J.+Detect+or+prevent+integer+overflow
		static final boolean overflowAdd(long left, long right)
		{
			return right > 0 ? left > Long.MAX_VALUE - right
			                 : left < Long.MIN_VALUE - right;
		}

		static final boolean overflowSubtract(long left, long right) {
			return right > 0 ? left < Long.MIN_VALUE + right
			                 : left > Long.MAX_VALUE + right;
		}

		static final boolean overflowMultiply(long left, long right)
		{
			return right > 0 ? left > Long.MAX_VALUE / right ||
			                   left < Long.MIN_VALUE / right
			                 : (right < -1 ? left > Long.MIN_VALUE / right ||
			                                 left < Long.MAX_VALUE / right
			                               : right == -1 &&
			                                 left == Long.MIN_VALUE);
		}

		private Fraction add(long numA, long denA, long numB, long denB, boolean subtract)
		{
			// Expand both fractions first
			// TODO: check penalty of gcd
			long facA = denA;
			long facB = denB;
//			long gcd = 1L;
			long gcd = gcd(denA, denB);
			if (gcd > 1L) {
				facA /= gcd;
				facB /= gcd;
			}
			if (!(overflowMultiply(denA, facB) || overflowMultiply(numA, facB) || overflowMultiply(numB, facA))) {
				long sumA = numA * facB;
				long sumB = numB * facA;
				if (subtract) {
					if (!overflowSubtract(sumA, sumB)) {
						// Use long arithmetics
						long sum = sumA - sumB;
						long den = denA * facB;
						return valueOf(sum, den);
					}
				} else {
					if (!(overflowAdd(sumA, sumB))) {
						// Use long arithmetics
						long sum = sumA + sumB;
						long den = denA * facB;
						return valueOf(sum, den);
					}
				}
			}
			// Fall back to BigInteger arithmetic
			BigInteger bigFacA = BigInteger.valueOf(facA);
			BigInteger bigFacB = BigInteger.valueOf(facB);
			BigInteger sumA = BigInteger.valueOf(numA).multiply(bigFacA);
			BigInteger sumB = BigInteger.valueOf(numB).multiply(bigFacB);
			BigInteger sum = subtract ? sumA.subtract(sumB) : sumA.add(sumB);
			BigInteger den = BigInteger.valueOf(denA).multiply(bigFacB);
			return valueOf(sum, den);
		}

		@Override
		public Fraction subtract(Fraction subtrahend)
		{
			return subtrahend.subtractFrom(this);
		}

		@Override
		public Fraction subtract(SmallFraction subtrahend)
		{
			return add(numerator, denominator, subtrahend.numerator, subtrahend.denominator, true);
		}

		@Override
		public Fraction subtract(MediumFraction subtrahend)
		{
			return add(numerator, denominator, subtrahend.numerator, subtrahend.denominator, true);
		}

		@Override
		public Fraction subtract(LargeFraction subtrahend)
		{
			return subtrahend.subtractFrom(this);
		}

		@Override
		public Fraction subtractFrom(Fraction minuend)
		{
			return minuend.subtract(this);
		}

		@Override
		public Fraction subtractFrom(SmallFraction minuend)
		{
			return add(minuend.numerator, minuend.denominator, numerator, denominator, true);
		}

		@Override
		public Fraction subtractFrom(MediumFraction minuend)
		{
			return add(minuend.numerator, minuend.denominator, numerator, denominator, true);
		}

		@Override
		public Fraction subtractFrom(LargeFraction minuend)
		{
			return minuend.subtract(this);
		}

		@Override
		public Fraction multiply(Fraction factor)
		{
			return factor.multiply(this);
		}

		@Override
		public Fraction multiply(SmallFraction factor)
		{
			return multiply(numerator, denominator, factor.numerator, factor.denominator);
		}

		@Override
		public Fraction multiply(MediumFraction factor)
		{
			return multiply(numerator, denominator, factor.numerator, factor.denominator);
		}

		@Override
		public Fraction multiply(LargeFraction factor)
		{
			return factor.multiply(this);
		}

		private Fraction multiply(long numA, long denA, long numB, long denB)
		{
			if (!(overflowMultiply(numA, numB) || overflowMultiply(denA, denB))) {
				// Use long arithmetics
				long num = numA * numB;
				long den = denA * denB;
				return valueOf(num, den);
			}
			// Fall back to BigInteger arithmetic
			BigInteger num = BigInteger.valueOf(numA).multiply(BigInteger.valueOf(numB));
			BigInteger den = BigInteger.valueOf(denA).multiply(BigInteger.valueOf(denB));
			return valueOf(num, den);
		}

		@Override
		public Fraction divide(Fraction divisor)
		{
			return divisor.divideDividend(this);
		}

		@Override
		public Fraction divide(SmallFraction divisor)
		{
			return multiply(numerator, denominator, divisor.denominator, divisor.numerator);
		}

		@Override
		public Fraction divide(MediumFraction divisor)
		{
			return multiply(numerator, denominator, divisor.denominator, divisor.numerator);
		}

		@Override
		public Fraction divide(LargeFraction divisor)
		{
			return divisor.divideDividend(this);
		}

		@Override
		public Fraction divideDividend(Fraction dividend)
		{
			return dividend.divide(this);
		}

		@Override
		public Fraction divideDividend(SmallFraction dividend)
		{
			return multiply(dividend.numerator, dividend.denominator, denominator, numerator);
		}

		@Override
		public Fraction divideDividend(MediumFraction dividend)
		{
			return multiply(dividend.numerator, dividend.denominator, denominator, numerator);
		}

		@Override
		public Fraction divideDividend(LargeFraction dividend)
		{
			return dividend.divide(this);
		}

		@Override
		public String toString()
		{
			String sign = (signum() < 0) ? "-" : "";
			BigInteger absNum = BigInteger.valueOf(numerator).abs();
			BigInteger absDen = BigInteger.valueOf(denominator).abs();
			BigInteger gcd = absNum.gcd(absDen);
			if (gcd.equals(absDen)) {
				return sign + absNum.divide(gcd);
			}
			return sign + absNum + "/" + absDen;
		}
	}

	public static int hashCode(long num, long den, boolean cancel)
	{
		if (cancel) {
			long gcd = gcd(num, den);
			if (gcd > 1L) {
				num /= gcd;
				den /= gcd;
			}
		}
		if (num != Long.MIN_VALUE && den != Long.MIN_VALUE) {
			return Long.hashCode(num) ^ Long.hashCode(den);
		}
		// Fall back to BigInteger since -MIN_VALUE == MIN_VALUE causes wrong hash value
		return BigInteger.valueOf(num).hashCode() ^ BigInteger.valueOf(den).hashCode();
	}



	public static class LargeFraction  implements Fraction
	{
		protected final BigInteger numerator;
		protected final BigInteger denominator;

		public LargeFraction(BigInteger numerator, BigInteger denominator)
		{
			if (denominator.signum() == 0) {
				throw new ArithmeticException("Division by zero");
			}
			this.numerator = numerator;
			this.denominator = denominator;
		}

		@Override
		public Fraction cancel()
		{
			BigInteger gcd = numerator.gcd(denominator);
			return gcd.equals(BigInteger.ONE) ? this : valueOf(numerator.divide(gcd), denominator.divide(gcd), false);
		}

		@Override
		public int signum()
		{
			return numerator.signum() * denominator.signum();
		}

		@Override
		public Fraction negate()
		{
			return new LargeFraction(numerator.negate(), denominator);
		}

		@Override
		public Fraction reciprocal()
		{
			return numerator.equals(denominator) ? this : valueOf(denominator, numerator, false);
		}

		@Override
		public int compareTo(Fraction other)
		{
			return -other.compareTo(this);
		}

		@Override
		public int compareTo(SmallFraction other)
		{
			if (this == (Fraction) other) {
				return 0;
			}
			int cmp = compareSignum(other.signum());
			return cmp > -2 ? cmp : compareTo(other.numerator, other.denominator);
		}

		@Override
		public int compareTo(MediumFraction other)
		{
			if (this == (Fraction) other) {
				return 0;
			}
			int cmp = compareSignum(other.signum());
			return cmp > -2 ? cmp : compareTo(other.numerator, other.denominator);
		}

		@Override
		public int compareTo(LargeFraction other)
		{
			if (this == (Fraction) other) {
				return 0;
			}
			int cmp = compareSignum(other.signum());
			return cmp > -2 ? cmp : compareTo(other.numerator, other.denominator);
		}

		private int compareTo(long otherNum, long otherDen)
		{
			return compareTo(BigInteger.valueOf(otherNum), BigInteger.valueOf(otherDen));
		}

		private int compareTo(BigInteger otherNum, BigInteger otherDen)
		{
			int sigA = signum();
			if (sigA != otherNum.signum() * otherDen.signum()) {
				throw new IllegalArgumentException("Signum of argument must match signum of receiver");
			}
			// Expand both fractions before comparing
			BigInteger numA = numerator.multiply(otherDen);
			BigInteger numB = otherNum.multiply(denominator);
			// Either both numbers are negated or none
			return numA.signum() == sigA ? numA.compareTo(numB) : numB.compareTo(numA);
		}


		@Override
		public boolean equals(Object other)
		{
			if (other instanceof Fraction) {
				return equals((Fraction) other);
			}
			return false;
		}

		@Override
		public boolean equals(Fraction other)
		{
			return other != null && other.equals(this);
		}


		@Override
		public boolean equals(SmallFraction other)
		{
			return other != null && compareTo(other) == 0;
		}

		@Override
		public boolean equals(MediumFraction other)
		{
			return other != null && compareTo(other) == 0;
		}

		@Override
		public boolean equals(LargeFraction other)
		{
			return other != null && compareTo(other) == 0;
		}

		@Override
		public int hashCode()
		{
			BigInteger num = numerator;
			BigInteger den = denominator;
			BigInteger gcd = numerator.gcd(denominator);
			if (!gcd.equals(BigInteger.ONE)) {
				num = numerator.divide(gcd);
				den = denominator.divide(gcd);
			}
			if (Fraction.isInvertibleLongValue(num) && Fraction.isInvertibleLongValue(den)) {
				// Use same hash function as for long values
				long longNum = num.longValue();
				long longDen = den.longValue();
				return Fraction.hashCode(longNum, longDen, false);
			}
			// Fall back to BigInteger since -MIN_VALUE == MIN_VALUE causes wrong hash value
			return num.hashCode() ^ den.hashCode();
		}

		@Override
		public Fraction add(Fraction summand)
		{
			return summand.add(this);
		}

		@Override
		public Fraction add(SmallFraction summand)
		{
			return add(summand.numerator, summand.denominator);
		}

		@Override
		public Fraction add(MediumFraction summand)
		{
			return add(summand.numerator, summand.denominator);
		}

		@Override
		public Fraction add(LargeFraction summand)
		{
			return add(numerator, denominator, summand.numerator, summand.denominator, false);
		}

		private Fraction add(long numB, long denB)
		{
			return add(numerator, denominator, BigInteger.valueOf(numB), BigInteger.valueOf(denB), false);
		}

		private Fraction add(BigInteger numA, BigInteger denA, BigInteger numB, BigInteger denB, boolean subtract)
		{
			// Don't cancel beforehand, since none of the operations will overflow
			BigInteger sumA = numA.multiply(denB);
			BigInteger sumB = numB.multiply(denA);
			BigInteger sum = subtract ? sumA.subtract(sumB) : sumA.add(sumB);
			BigInteger den = denA.multiply(denB);
			return valueOf(sum, den);

//			No problem if same signum?
//			A) both positive
//			1/2 + 3/4 = (4 + 6) / ( 2 *  4) = 10/8;
//			-1/-2 + -3/-4 = (4 + 6) / (-2 * -4) = -10/8;
//			B) both negative
//			-1/2 + -3/4 = (-4 + -6) / ( 2 *  4) = -10/8;
//			1/-2 + 3/-4 = (-4 + -6) / (-2 * -4) = -10/8;
//			1/-2 + -3/4 = (4 + 6) / (-2 * 4)    = 10/-8;
//			-1/2 + 3/-4 = (4 + 6) / (2 * -4)    = 10/-8;
//
//			Problem if different signum?
//			1/2 + -3/4 = 4 + -6 = -2 / 8 OK
//			1/2 + 3/-4 = -4 + 6 = 2 / -8 OK
		}

		@Override
		public Fraction subtract(Fraction subtrahend)
		{
			return subtrahend.subtractFrom(this);
		}

		@Override
		public Fraction subtract(SmallFraction subtrahend)
		{
			return subtract(subtrahend.numerator, subtrahend.denominator);
		}

		@Override
		public Fraction subtract(MediumFraction subtrahend)
		{
			return subtract(subtrahend.numerator, subtrahend.denominator);
		}

		@Override
		public Fraction subtract(LargeFraction subtrahend)
		{
			return add(numerator, denominator, subtrahend.numerator, subtrahend.denominator, true);
		}

		private Fraction subtract(long subtrahendNum, long subtrahend)
		{
			return add(numerator, denominator, BigInteger.valueOf(subtrahendNum), BigInteger.valueOf(subtrahend), true);
		}

		@Override
		public Fraction subtractFrom(Fraction minuend)
		{
			return minuend.subtract(this);
		}

		@Override
		public Fraction subtractFrom(SmallFraction minuend)
		{
			return subtractFrom(minuend.numerator, minuend.denominator);
		}

		@Override
		public Fraction subtractFrom(MediumFraction minuend)
		{
			return subtractFrom(minuend.numerator, minuend.denominator);
		}

		@Override
		public Fraction subtractFrom(LargeFraction minuend)
		{
			return add(minuend.numerator, minuend.denominator, numerator, denominator, true);
		}

		private Fraction subtractFrom(long minuendNum, long minuendDen)
		{
			return add(BigInteger.valueOf(minuendNum), BigInteger.valueOf(minuendDen), numerator, denominator, true);
		}

		@Override
		public Fraction multiply(Fraction factor)
		{
			return factor.multiply(this);
		}

		@Override
		public Fraction multiply(SmallFraction factor)
		{
			return multiply(factor.numerator, factor.denominator);
		}

		@Override
		public Fraction multiply(MediumFraction factor)
		{
			return multiply(factor.numerator, factor.denominator);
		}

		@Override
		public Fraction multiply(LargeFraction factor)
		{
			return multiply(numerator, denominator, factor.numerator, factor.denominator);
		}

		public Fraction multiply(long factorNum, long factorDen)
		{
			return multiply(numerator, denominator, BigInteger.valueOf(factorNum), BigInteger.valueOf(factorDen));
		}

		private Fraction multiply(BigInteger numA, BigInteger denA, BigInteger numB, BigInteger denB)
		{
			BigInteger num = numA.multiply(numB);
			BigInteger den = denA.multiply(denB);
			return valueOf(num, den);
		}

		@Override
		public Fraction divide(Fraction divisor)
		{
			return divisor.divideDividend(this);
		}

		@Override
		public Fraction divide(SmallFraction divisor)
		{
			return multiply(divisor.denominator, divisor.numerator);
		}

		@Override
		public Fraction divide(MediumFraction divisor)
		{
			return multiply(divisor.denominator, divisor.numerator);
		}

		@Override
		public Fraction divide(LargeFraction divisor)
		{
			return multiply(numerator, denominator, divisor.denominator, divisor.numerator);
		}

		@Override
		public Fraction divideDividend(Fraction dividend)
		{
			return dividend.divide(this);
		}

		@Override
		public Fraction divideDividend(SmallFraction dividend)
		{
			return divideDivided(dividend.numerator, dividend.denominator);
		}

		@Override
		public Fraction divideDividend(MediumFraction dividend)
		{
			return divideDivided(dividend.numerator, dividend.denominator);
		}

		@Override
		public Fraction divideDividend(LargeFraction dividend)
		{
			return multiply(dividend.numerator, dividend.denominator, denominator, numerator);
		}

		public Fraction divideDivided(long dividendNum, long dividendDen)
		{
			return multiply(BigInteger.valueOf(dividendNum), BigInteger.valueOf(dividendDen), denominator, numerator);
		}

		@Override
		public String toString()
		{
			String sign = (signum() < 0) ? "-" : "";
			BigInteger absNum = numerator.abs();
			BigInteger absDen = denominator.abs();
			BigInteger gcd = absNum.gcd(absDen);
			if (gcd.equals(absDen)) {
				return sign + absNum.divide(gcd);
			}
			return sign + absNum + "/" + absDen;
		}
	}



	public static void main(String[] args)
	{
//		BigInteger offset = BigInteger.ZERO;
//		BigInteger offset = INT_MAX_VALUE;
//		BigInteger offset = INT_MAX_VALUE.add(INT_MAX_VALUE);
// 		BigInteger offset = LONG_MAX_VALUE;
		BigInteger offset = LONG_MAX_VALUE.add(LONG_MAX_VALUE);
		final int n = 30;
		final int m = 2 * n;
		Fraction[][] fractions = new Fraction[n][m];
		for (int i = 0; i < n; i++) {
			BigInteger den = offset.add(BigInteger.valueOf(i).add(BigInteger.ONE));
			for (int j = 0; j < n; j++) {
				BigInteger num = offset.add(BigInteger.valueOf(j));
//				Fraction frac = new SmallFraction(num, den);
//				Fraction frac = new MediumFraction(num, den);
//				Fraction frac = new LargeFraction(BigInteger.valueOf(num), BigInteger.valueOf(den));
				Fraction frac = Fraction.valueOf(num, den);
				fractions[i][j] = frac;
				fractions[i][n + j] = frac.negate();
			}
		}

		BigRational[][] rationals = new BigRational[n][m];
		for (int i = 0; i < n; i++) {
			BigInteger den = offset.add(BigInteger.valueOf(i).add(BigInteger.ONE));
			for (int j = 0; j < n; j++) {
				BigInteger num = offset.add(BigInteger.valueOf(j));
//				BigRational frac = new BigRational(num, den, false);
				BigRational frac = new BigRational(num, den);
				rationals[i][j] = frac;
				rationals[i][n + j] = frac.negate();
			}
		}

		StopWatch watch = new StopWatch(new PrismPrintStreamLog(System.out));

		long errorsBigRational = 0L;
		watch.start("Rational arithmetic");
		for (int i1 = 0; i1 < n; i1++) {
			for (int j1 = 0; j1 < m; j1++) {
				for (int i2 = 0; i2 < n; i2++) {
					for (int j2 = 0; j2 < m; j2++) {
						BigRational f1 = rationals[i1][j1];
						BigRational f2 = rationals[i2][j2];
//						f1.add(f2);
						f1.multiply(f2);
//						if (f1.cancel().equals(f2.cancel()) && !(f1.equals(f2) && f1.hashCode() == f2.hashCode())) {
//							errorsBigRational++;
//						};
					}
				}
			}
		}
		watch.stop("errors: " + errorsBigRational);

		long errorsFraction = 0L;
		watch.start("Fraction arithmetic");
		for (int i1 = 0; i1 < n; i1++) {
			for (int j1 = 0; j1 < m; j1++) {
				for (int i2 = 0; i2 < n; i2++) {
					for (int j2 = 0; j2 < m; j2++) {
						Fraction f1 = fractions[i1][j1];
						Fraction f2 = fractions[i2][j2];
//						f1.add(f2);
						f1.multiply(f2);
//						if (f1.cancel().equals(f2.cancel()) && !(f1.equals(f2) && f1.hashCode() == f2.hashCode())) {
//							errorsFraction++;
//						};
					}
				}
			}
		}
		watch.stop("errors: " + errorsFraction);
	}
}