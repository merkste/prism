package param;

import common.StopWatch;
import prism.PrismPrintStreamLog;

import java.math.BigInteger;

import static param.ExactInteger.*;

// TODO: BigRational has bugs if fraction is not canceled, e.g., #isZero, #isOne, #compareTo, #equals, #hash
// TODO: Write generator for all rational numbers
// TODO: toDouble -> use round to denominator (expand to power of two)

public interface Fraction extends Comparable<Fraction>
{
	public static int hashCode(ExactInteger num, ExactInteger den, boolean cancel)
	{
		assert cancel || num.gcd(den).equals(1);
		if (cancel) {
			ExactInteger gcd = num.gcd(den);
			if (!gcd.equals(1)) {
				num = num.divide(gcd);
				den = den.divide(gcd);
			}
		}
		return 37 * num.hashCode() ^ den.hashCode();
	}

	default int compareFast(Fraction other)
	{
		if (this == (Fraction) other) {
			return 0;
		}
		int sigA = signum();
		int sigB = other.signum();
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
		if (numerator == Integer.MIN_VALUE || denominator == Integer.MIN_VALUE) {
			return valueOf((long)numerator, (long)denominator, cancel);
		}
		if (cancel) {
			int gcd = gcdExact(numerator, denominator);
			if (gcd > 1) {
				numerator /= gcd;
				denominator /= gcd;
			}
		}
		return new SmallFraction(numerator, denominator, cancel);
	}

	public static Fraction valueOf(long numerator, long denominator)
	{
		return valueOf(numerator, denominator, true);
	}

	public static Fraction valueOf(long numerator, long denominator, boolean cancel)
	{
		if (numerator == Long.MIN_VALUE || denominator == Long.MIN_VALUE) {
			return valueOf(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator), cancel);
		}
		if (cancel) {
			long gcd = gcdExact(numerator, denominator);
			if (gcd > 1) {
				numerator /= gcd;
				denominator /= gcd;
			}
		}
		if (isInvertibleIntValue(numerator) && isInvertibleIntValue(denominator))
		{
			// TODO: Sets wrong flag >> accept for now (penalty of canceling twice is significant)
			return valueOf((int)numerator, (int)denominator, false);
		}
		return new MediumFraction(numerator, denominator, cancel);
	}

	public static Fraction valueOf(BigInteger numerator, BigInteger denominator)
	{
		return valueOf(numerator, denominator, true);
	}

	public static Fraction valueOf(BigInteger numerator, BigInteger denominator, boolean cancel)
	{
		if (isInvertibleLongValue(numerator) && isInvertibleLongValue(denominator))
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
		if (isInvertibleLongValue(numerator) && isInvertibleLongValue(denominator))
		{
			// TODO: Sets wrong flag >> accept for now (penalty of canceling twice is significant)
			return valueOf(numerator.longValue(), denominator.longValue(), false);
		}
		return new LargeFraction(numerator, denominator, cancel);
	}

//	public static Fraction valueOf(BigInteger numerator, long denominator)
//	{
//		return valueOf(numerator, denominator, true);
//	}
//
//	public static Fraction valueOf(BigInteger numerator, long denominator, boolean cancel)
//	{
//		return valueOf(numerator, BigInteger.valueOf(denominator), cancel);
//	}
//
//	public static Fraction valueOf(long numerator, BigInteger denominator)
//	{
//		return valueOf(numerator, denominator, true);
//	}
//
//	public static Fraction valueOf(long numerator, BigInteger denominator, boolean cancel)
//	{
//		return valueOf(BigInteger.valueOf(numerator), denominator, true);
//	}

	// TODO: Exploit types
	public static Fraction valueOf(IntOrLong numerator, IntOrLong denominator)
	{
		return valueOf(numerator, denominator, true);
	}

	public static Fraction valueOf(IntOrLong numerator, IntOrLong denominator, boolean cancel)
	{
		ExactInteger num = numerator;
		ExactInteger den = numerator;
		if (cancel) {
			ExactInteger gcd = numerator.gcd(denominator);
			if (!gcd.equals(1)) {
				num = numerator.divide(gcd);
				den = denominator.divide(gcd);
			}
		}
		if (numerator.fitsInt() && denominator.fitsInt() && !denominator.equals(Integer.MIN_VALUE) && !numerator.equals(Integer.MIN_VALUE))
		{
			return valueOf(numerator.intValueExact(), denominator.intValueExact(), false);
		}
		return valueOf(numerator.longValueExact(), denominator.longValueExact(), false);
	}

	public static Fraction valueOf(ExactInteger numerator, ExactInteger denominator)
	{
		return valueOf(numerator, denominator, true);
	}

	public static Fraction valueOf(ExactInteger numerator, ExactInteger denominator, boolean cancel)
	{
		if (cancel) {
			ExactInteger gcd = numerator.gcd(denominator);
			if (!gcd.equals(1)) {
				numerator = numerator.divide(gcd);
				denominator = denominator.divide(gcd);
			}
		}
		if (numerator.fitsInt() && denominator.fitsInt() && !denominator.equals(Integer.MIN_VALUE) && !numerator.equals(Integer.MIN_VALUE))
		{
			return valueOf(numerator.intValueExact(), denominator.intValueExact(), false);
		}
		if (numerator.fitsLong() && denominator.fitsLong() && !numerator.equals(Long.MIN_VALUE) && !denominator.equals(Long.MIN_VALUE))
		{
			return valueOf(numerator.longValueExact(), denominator.longValueExact(), false);
		}
		return new LargeFraction(numerator.bigIntegerValue(), denominator.bigIntegerValue(), cancel);
	}


	public static final BigInteger INT_MIN_VALUE = BigInteger.valueOf(Integer.MIN_VALUE);
	public static final BigInteger INT_MAX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);
	public static final BigInteger LONG_MIN_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
	public static final BigInteger LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

	private static boolean isInvertibleLongValue(BigInteger n)
	{
		return LONG_MIN_VALUE.compareTo(n) < 0 && n.compareTo(LONG_MAX_VALUE) <= 0;
	}

	private static boolean isInvertibleIntValue(long n)
	{
		// TODO: extract long constant
		return Integer.MIN_VALUE < n && n <= Integer.MAX_VALUE;
	}

	ExactInteger getNumerator();

	ExactInteger getDenominator();

	Fraction cancel();

	ExactInteger gcd();

	int signum();

	int compareTo(Fraction other);

	int compareTo(SmallFraction other);

	int compareTo(MediumFraction other);

	int compareTo(LargeFraction other);

	boolean equals(Fraction other);

	boolean equals(SmallFraction other);

	boolean equals(MediumFraction other);

	boolean equals(LargeFraction other);

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

	default String printString()
	{
		String sign = (signum() < 0) ? "-" : "";
		ExactInteger absNum = getNumerator().abs();
		ExactInteger absDen = getDenominator().abs();
		ExactInteger gcd = gcd();
		if (absDen.equals(gcd)) {
			return sign + absNum.divide(gcd);
		}
		return sign + absNum + "/" + absDen;
	}

	public static class SmallFraction implements Fraction
	{
		protected final int numerator;
		protected final int denominator;
		protected final boolean canceled;

		private SmallFraction(int numerator, int denominator, boolean canceled)
		{
			assert numerator > Integer.MIN_VALUE && denominator > Integer.MIN_VALUE;
			assert !canceled || ExactInteger.gcd(numerator, denominator).equals(1);
			if (denominator == 0) {
				throw new ArithmeticException("Division by zero");
			}
			this.numerator = numerator;
			this.denominator = denominator;
			this.canceled = canceled;
		}

		@Override
		public ExactInt getNumerator()
		{
			return new ExactInt(numerator);
		}

		@Override
		public ExactInt getDenominator()
		{
			return new ExactInt(denominator);
		}

		@Override
		public Fraction cancel()
		{
			return canceled ? this : valueOf(numerator, denominator);
		}

		@Override
		public ExactInteger gcd()
		{
			return canceled ? ExactInteger.ONE : ExactInteger.gcd(numerator, denominator);
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
			int cmp = compareFast(other);
			if (cmp > -2) {
				return cmp;
			}
			// Expand both fractions before comparing
			long numA = numerator * other.denominator;
			long numB = other.numerator * denominator;
			// Either both numbers are negated or none
			return signum() == Long.signum(numA) ? Long.compare(numA, numB) : Long.compare(numB, numA);
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
			return (other instanceof Fraction) && equals((Fraction) other);
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
			return Fraction.hashCode(new ExactInt(numerator), new ExactInt(denominator), !canceled);
		}

		@Override
		public Fraction negate()
		{
			assert numerator > Integer.MIN_VALUE && denominator > Integer.MIN_VALUE;
			return new SmallFraction(-numerator, denominator, canceled);
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
			// Don't cancel beforehand, since multiplication will not overflow
			long sumA = (long)numA * (long)denB;
			long sumB = (long)numB * (long)denA;
			long den = (long)denA * (long)denB;
			// TODO: Check performance
			ExactInteger sum = subtract ? ExactInteger.subtract(sumA, sumB) : ExactInteger.add(sumA, sumB);
			return valueOf(sum, new ExactLong(den));
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
			return minuend.subtract(this);
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

		private Fraction multiply(int numA, int denA, int numB, int denB)
		{
			// Don't cancel beforehand, since multiplication will not overflow
			long num = (long)numA * (long)numB;
			long den = (long)denA * (long)denB;
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
			return printString();
		}
	}



	public static class MediumFraction implements Fraction
	{
		protected final long numerator;
		protected final long denominator;
		protected final boolean canceled;

		private MediumFraction(long numerator, long denominator, boolean canceled)
		{
			assert numerator > Long.MIN_VALUE && denominator > Long.MIN_VALUE;
			assert !canceled || ExactInteger.gcd(numerator, denominator).equals(1);
			if (denominator == 0L) {
				throw new ArithmeticException("Division by zero");
			}
			this.numerator = numerator;
			this.denominator = denominator;
			this.canceled = canceled;
		}

		@Override
		public ExactLong getNumerator()
		{
			return new ExactLong(numerator);
		}

		@Override
		public ExactLong getDenominator()
		{
			return new ExactLong(denominator);
		}

		@Override
		public Fraction cancel()
		{
			return canceled ? this : valueOf(numerator, denominator);
		}

		@Override
		public ExactInteger gcd()
		{
			return canceled ? ExactInteger.ONE : ExactInteger.gcd(numerator, denominator);
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
			int cmp = compareFast(other);
			return cmp > -2 ? cmp : compareTo(other.numerator, other.denominator);
		}

		@Override
		public int compareTo(MediumFraction other)
		{
			int cmp = compareFast(other);
			return cmp > -2 ? cmp : compareTo(other.numerator, other.denominator);
		}

		@Override
		public int compareTo(LargeFraction other)
		{
			return -other.compareTo(this);
		}

		private int compareTo(long numB, long denB)
		{
			long numA = numerator;
			long denA = denominator;
			// Cancel denumerators to increase chance to use long arithmetic
			// TODO: check penalty of gcd
//			long gcd = 1L;
			assert denA > Long.MIN_VALUE && denB > Long.MIN_VALUE;
			long gcd = gcdExact(denominator, denB);
			if (gcd > 1L) {
				denA /= gcd;
				denB /= gcd;
			}
			// Expand both fractions first
			ExactInteger sumA = ExactInteger.multiply(numA, denB);
			ExactInteger sumB = ExactInteger.multiply(numB, denA);
			// Either both numbers are negated or none
			assert signum() == Long.signum(numB) * Long.signum(denB);
			return signum() == sumA.signum()? sumA.compareTo(sumB) : sumB.compareTo(sumA);
		}

		@Override
		public boolean equals(Object other)
		{
			return (other instanceof Fraction) && equals((Fraction) other);
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
			return Fraction.hashCode(new ExactLong(numerator), new ExactLong(denominator), !canceled);
		}

		@Override
		public Fraction negate()
		{
			assert numerator > Long.MIN_VALUE && denominator > Long.MIN_VALUE;
			return new MediumFraction(-numerator, denominator, canceled);
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

		private Fraction add(long numA, long denA, long numB, long denB, boolean subtract)
		{
			// Cancel denumerators to increase chance to use long arithmetic
			// TODO: check penalty of gcd
			long gcd = 1L;
			assert denA > Long.MIN_VALUE && denB > Long.MIN_VALUE;
//			long gcd = gcdExact(denA, denB);
//			if (gcd > 1L) {
//				denA /= gcd;
//				denB /= gcd;
//			}
			// Expand both fractions first
			ExactInteger sumA = ExactInteger.multiply(numA, denB);
			ExactInteger sumB = ExactInteger.multiply(numB, denA);
			ExactInteger den = ExactInteger.multiply(denA, denB);
			ExactInteger num = subtract ? sumA.subtract(sumB) : sumA.add(sumB);
			return Fraction.valueOf(num, den);
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
			ExactInteger num = ExactInteger.multiply(numA, numB);
			ExactInteger den = ExactInteger.multiply(denA, denB);
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
			return printString();
		}
	}



	public static class LargeFraction  implements Fraction
	{
		protected final BigInteger numerator;
		protected final BigInteger denominator;
		protected final boolean canceled;

		private LargeFraction(BigInteger numerator, BigInteger denominator, boolean canceled)
		{
			assert !canceled || numerator.gcd(denominator).equals(BigInteger.ONE);
			if (denominator.signum() == 0) {
				throw new ArithmeticException("Division by zero");
			}
			this.numerator = numerator;
			this.denominator = denominator;
			this.canceled = canceled;
		}

		@Override
		public ExactBigInteger getNumerator()
		{
			return new ExactBigInteger(numerator);
		}

		@Override
		public ExactBigInteger getDenominator()
		{
			return new ExactBigInteger(denominator);
		}

		@Override
		public Fraction cancel()
		{
			return canceled ? this : valueOf(numerator, denominator);
		}

		@Override
		public ExactInteger gcd()
		{
			return canceled ? ExactInteger.ONE : new ExactBigInteger(numerator.gcd(denominator));
		}

		@Override
		public int signum()
		{
			return numerator.signum() * denominator.signum();
		}

		@Override
		public Fraction negate()
		{
			return new LargeFraction(numerator.negate(), denominator, canceled);
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
			int cmp = compareFast(other);
			return cmp > -2 ? cmp : compareTo(other.numerator, other.denominator);
		}

		@Override
		public int compareTo(MediumFraction other)
		{
			int cmp = compareFast(other);
			return cmp > -2 ? cmp : compareTo(other.numerator, other.denominator);
		}

		@Override
		public int compareTo(LargeFraction other)
		{
			int cmp = compareFast(other);
			return cmp > -2 ? cmp : compareTo(other.numerator, other.denominator);
		}

		private int compareTo(long otherNum, long otherDen)
		{
			return compareTo(BigInteger.valueOf(otherNum), BigInteger.valueOf(otherDen));
		}

		private int compareTo(BigInteger otherNum, BigInteger otherDen)
		{
			// Expand both fractions before comparing
			BigInteger sumA = numerator.multiply(otherDen);
			BigInteger sumB = otherNum.multiply(denominator);
			// Either both numbers are negated or none
			assert signum() == otherNum.signum() * otherDen.signum();
			return signum() == sumA.signum()? sumA.compareTo(sumB) : sumB.compareTo(sumA);
		}

		@Override
		public boolean equals(Object other)
		{
			return (other instanceof Fraction) && equals((Fraction) other);
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
			if (!canceled) {
				BigInteger gcd = numerator.gcd(denominator);
				if (!gcd.equals(BigInteger.ONE)) {
					num = num.divide(gcd) ;
					den = den.divide(gcd);
				}
			}
			return 37 * num.hashCode() ^ den.hashCode();
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
			return printString();
		}
	}



	public class FlexFraction implements Fraction
	{
		protected final ExactInteger numerator;
		protected final ExactInteger denominator;
		protected boolean canceled;

		public FlexFraction(ExactInteger numerator, ExactInteger denominator)
		{
			this(numerator, denominator, true);
		}

		public FlexFraction(ExactInteger numerator, ExactInteger denominator, boolean cancel)
		{
			if (denominator.equals(0)) {
				throw new ArithmeticException("Division by zero");
			}
			this.canceled = cancel;
			if (cancel) {
				ExactInteger gcd = numerator.gcd(denominator);
				if (!gcd.equals(1)) {
					this.numerator = numerator.divide(gcd);
					this.denominator = denominator.divide(gcd);
					return;
				}
			}
			this.numerator = numerator;
			this.denominator = denominator;
		}

		@Override
		public ExactInteger getNumerator()
		{
			return numerator;
		}

		@Override
		public ExactInteger getDenominator()
		{
			return denominator;
		}

		@Override
		public FlexFraction cancel()
		{
			return canceled ? this : new FlexFraction(numerator, denominator, true);
		}

		@Override
		public ExactInteger gcd()
		{
			return numerator.gcd(denominator);
		}

		@Override
		public int signum()
		{
			return numerator.signum() * denominator.signum();
		}

		@Override
		public int compareTo(Fraction other)
		{
			return 0;
		}

		@Override
		public int compareTo(SmallFraction other)
		{
			return 0;
		}

		@Override
		public int compareTo(MediumFraction other)
		{
			return 0;
		}

		@Override
		public int compareTo(LargeFraction other)
		{
			return 0;
		}

//		@Override
		public int compareTo(FlexFraction other)
		{
			int cmp = compareFast(other);
			return cmp > -2 ? cmp : compareTo(numerator, denominator, other.numerator, other.denominator);
		}

		//		@Override
		public int compareTo(ExactInteger numA, ExactInteger denA, ExactInteger numB, ExactInteger denB)
		{
			// Cancel denumerators to increase chance to use int/long arithmetic
			// TODO: check penalty of gcd
//			ExactInteger gcd = denominator.gcd(other.denominator);
//			if (!gcd.equals(1)) {
//				denA = denA.divide(gcd);
//				denB = denB.divide(gcd);
//			}
			// Expand both fractions first
			ExactInteger sumA = numA.multiply(denB);
			ExactInteger sumB = numB.multiply(denA);
			// Either both numbers are negated or none
			assert signum() == numB.signum() * denB.signum();
			return signum() == sumA.signum()? sumA.compareTo(sumB) : sumB.compareTo(sumA);
		}

		@Override
		public boolean equals(Fraction other)
		{
			return false;
		}

		@Override
		public boolean equals(SmallFraction other)
		{
			return false;
		}

		@Override
		public boolean equals(MediumFraction other)
		{
			return false;
		}

		@Override
		public boolean equals(LargeFraction other)
		{
			return false;
		}

//		@Override
		public boolean equals(FlexFraction other)
		{
			return other != null && compareTo(other) == 0;
		}

		@Override
		public int hashCode()
		{
			return Fraction.hashCode(numerator, denominator, !canceled);
		}

		@Override
		public FlexFraction negate()
		{
			// TODO: Sets wrong flag for canceled. Should inherit this.canceled
			return new FlexFraction(numerator.negate(), denominator, false);
		}

		@Override
		public FlexFraction reciprocal()
		{
			// TODO: Sets wrong flag for canceled. Should inherit this.canceled
			return numerator.equals(denominator) ? this : new FlexFraction(denominator, numerator, false);
		}

		@Override
		public Fraction add(Fraction summand)
		{
			return null;
		}

		@Override
		public Fraction add(SmallFraction summand)
		{
			return null;
		}

		@Override
		public Fraction add(MediumFraction summand)
		{
			return null;
		}

		@Override
		public Fraction add(LargeFraction summand)
		{
			return null;
		}

//		@Override
		public FlexFraction add(FlexFraction summand)
		{
			return add(numerator, denominator, summand.numerator, summand.denominator, false);
		}

		private FlexFraction add(ExactInteger numA, ExactInteger denA, ExactInteger numB, ExactInteger denB, boolean subtract)
		{
			// Cancel denumerators to increase chance to use int/long arithmetic
			// TODO: check penalty of gcd
			ExactInteger gcd = denA.gcd(denB);
			if (!gcd.equals(1)) {
				denA = denA.divide(gcd);
				denB = denB.divide(gcd);
			}
			// Expand both fractions first
			ExactInteger sumA = numA.multiply(denB);
			ExactInteger sumB = numB.multiply(denA);
			ExactInteger den = denA.multiply(denB);
			ExactInteger num = subtract ? sumA.subtract(sumB) : sumA.add(sumB);
			return new FlexFraction(num, den);
		}

		@Override
		public FlexFraction subtract(Fraction subtrahend)
		{
			return null;
		}

		@Override
		public Fraction subtract(SmallFraction subtrahend)
		{
			return null;
		}

		@Override
		public Fraction subtract(MediumFraction subtrahend)
		{
			return null;
		}

		@Override
		public Fraction subtract(LargeFraction subtrahend)
		{
			return null;
		}

//		@Override
		public FlexFraction subtract(FlexFraction subtrahend)
		{
			return add(numerator, denominator, subtrahend.numerator, subtrahend.denominator, true);
		}

		@Override
		public Fraction subtractFrom(Fraction minuend)
		{
			return null;
		}

		@Override
		public Fraction subtractFrom(SmallFraction minuend)
		{
			return null;
		}

		@Override
		public Fraction subtractFrom(MediumFraction minuend)
		{
			return null;
		}

		@Override
		public Fraction subtractFrom(LargeFraction minuend)
		{
			return null;
		}

//		@Override
		public FlexFraction subtractFrom(FlexFraction minuend)
		{
			return add(minuend.numerator, minuend.denominator, numerator, denominator, true);
		}

		@Override
		public Fraction multiply(Fraction factor)
		{
			return null;
		}

		@Override
		public Fraction multiply(SmallFraction factor)
		{
			return null;
		}

		@Override
		public Fraction multiply(MediumFraction factor)
		{
			return null;
		}

		@Override
		public Fraction multiply(LargeFraction factor)
		{
			return null;
		}

//		@Override
		public FlexFraction multiply(FlexFraction factor)
		{
			return multiply(numerator, denominator, factor.numerator, factor.denominator);
		}

		private FlexFraction multiply(ExactInteger numA, ExactInteger denA, ExactInteger numB, ExactInteger denB)
		{
			ExactInteger num = numA.multiply(numB);
			ExactInteger den = denA.multiply(denB);
			return new FlexFraction(num, den);
		}

		@Override
		public Fraction divide(Fraction divisor)
		{
			return null;
		}

		@Override
		public Fraction divide(SmallFraction divisor)
		{
			return null;
		}

		@Override
		public Fraction divide(MediumFraction divisor)
		{
			return null;
		}

		@Override
		public Fraction divide(LargeFraction divisor)
		{
			return null;
		}

//		@Override
		public FlexFraction divide(FlexFraction divisor)
		{
			return multiply(numerator, denominator, divisor.denominator, divisor.numerator);
		}

		@Override
		public Fraction divideDividend(Fraction dividend)
		{
			return null;
		}

		@Override
		public Fraction divideDividend(SmallFraction dividend)
		{
			return null;
		}

		@Override
		public Fraction divideDividend(MediumFraction dividend)
		{
			return null;
		}

		@Override
		public Fraction divideDividend(LargeFraction dividend)
		{
			return null;
		}

//		@Override
		public FlexFraction divideDividend(FlexFraction dividend)
		{
			return multiply(dividend.numerator, dividend.denominator, denominator, numerator);
		}

//		@Override
		public FlexFraction pow(int exponent)
		{
			if(exponent < 0) {
				exponent *= -1;
				return new FlexFraction(denominator.pow(exponent), numerator.pow(exponent), false);
			}
			return new FlexFraction(numerator.pow(exponent), denominator.pow(exponent), false);
		}

		@Override
		public String toString()
		{
			return printString();
		}
	}



	public static void main(String[] args)
	{
//		ExactInteger offset = ExactInteger.ZERO;
//		ExactInteger offset = ExactInteger.valueOf(INT_MAX_VALUE).subtract(10);
		ExactInteger offset = ExactInteger.valueOf(INT_MAX_VALUE).add(INT_MAX_VALUE);
// 		ExactInteger offset = ExactInteger.valueOf(LONG_MAX_VALUE).subtract(10);
//		ExactInteger offset = ExactInteger.valueOf(LONG_MAX_VALUE).add(LONG_MAX_VALUE);
		final int n = 40;
		final int m = 2 * n;
		Fraction[][] fractions = new Fraction[n][m];
		for (int i = 0; i < n; i++) {
			ExactInteger den = offset.add(i).add(1);
			for (int j = 0; j < n; j++) {
				ExactInteger num = offset.add(j);
				Fraction frac = Fraction.valueOf(num, den);
				fractions[i][j] = frac;
				fractions[i][n + j] = frac.negate();
			}
		}

		FlexFraction[][] flexFracs = new FlexFraction[n][m];
		for (int i = 0; i < n; i++) {
			ExactInteger den = offset.add(i).add(1);
			for (int j = 0; j < n; j++) {
				ExactInteger num = offset.add(j);
				FlexFraction frac = new FlexFraction(num, den);
				flexFracs[i][j] = frac;
				flexFracs[i][n + j] = frac.negate();
			}
		}

		BigRational[][] rationals = new BigRational[n][m];
		for (int i = 0; i < n; i++) {
			ExactInteger den = offset.add(i).add(1);
			for (int j = 0; j < n; j++) {
				ExactInteger num = offset.add(j);
				BigRational frac = new BigRational(num.bigIntegerValue(), den.bigIntegerValue());
				rationals[i][j] = frac;
				rationals[i][n + j] = frac.negate();
			}
		}

		StopWatch watch = new StopWatch(new PrismPrintStreamLog(System.out));

		long errorsBigRational = 0L;
		BigRational r = BigRational.ZERO;
		watch.start("Rational arithmetic");
		for (int i1 = 0; i1 < n; i1++) {
			for (int j1 = 0; j1 < m; j1++) {
				for (int i2 = 0; i2 < n; i2++) {
					for (int j2 = 0; j2 < m; j2++) {
						BigRational r1 = rationals[i1][j1];
//						BigRational r2 = rationals[i2][j2];
//						BigRational r = r1.add(r2);
						r = r1.pow(1).pow(2).pow(3).pow(4);
//						if (r1.cancel().equals(r2.cancel()) && !(r1.equals(r2) && r1.hashCode() == r2.hashCode())) {
//							errorsBigRational++;
//						};
					}
				}
			}
		}
		watch.stop("errors: " + r);

//		long errorsFraction = 0L;
//		watch.start("Fraction arithmetic");
//		for (int i1 = 0; i1 < n; i1++) {
//			for (int j1 = 0; j1 < m; j1++) {
//				for (int i2 = 0; i2 < n; i2++) {
//					for (int j2 = 0; j2 < m; j2++) {
//						Fraction f1 = fractions[i1][j1];
//						Fraction f2 = fractions[i2][j2];
//						Fraction f = f1.add(f2);
////						BigRational r1 = rationals[i1][j1];
////						BigRational r2 = rationals[i2][j2];
////						BigRational r = r1.add(r2);
////						assert r.toString().equals(f.toString());
////						if (f1.cancel().equals(f2.cancel()) && !(f1.equals(f2) && f1.hashCode() == f2.hashCode())) {
////							errorsFraction++;
////						};
//					}
//				}
//			}
//		}
//		watch.stop("errors: " + errorsFraction);

		long errorsFlexFracs = 0L;
		FlexFraction f = new FlexFraction(ZERO, ONE);
		watch.start("Flexible arithmetic");
		for (int i1 = 0; i1 < n; i1++) {
			for (int j1 = 0; j1 < m; j1++) {
				for (int i2 = 0; i2 < n; i2++) {
					for (int j2 = 0; j2 < m; j2++) {
						FlexFraction f1 = flexFracs[i1][j1];
//						FlexFraction f2 = flexFracs[i2][j2];
						f = f1.pow(1).pow(2).pow(3).pow(4);
//						FlexFraction f = f1.add(f2);
//						BigRational r1 = rationals[i1][j1];
//						BigRational r2 = rationals[i2][j2];
//						BigRational r = r1.add(r2);
//						BigRational r = r1.pow(1).pow(2).pow(3).pow(4);
//						assert r.toString().equals(f.toString());
//						if (f1.cancel().equals(f2.cancel()) && !(f1.equals(f2) && f1.hashCode() == f2.hashCode())) {
//							errorsFlexFracs++;
//						};
					}
				}
			}
		}
		watch.stop("errors: " + f);
	}
}