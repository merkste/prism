package param;

import common.StopWatch;
import prism.PrismLog;
import prism.PrismPrintStreamLog;

import java.math.BigInteger;

import static param.ExactInteger.*;

// TODO: benchmark pre- vs. postcondition testing
// -[x] runIsOverflowAddInt
// -[x] runIsOverflowAddLong
// -[x] runIsOverflowSubtractInt
// -[x] runIsOverflowSubtractLong
// -[x] runIsOverflowMultiplyInt
// -[x] runIsOverflowMultiplyLong

public class ExactIntegerBenchmark
{
	static final long LONG_INT_MIN_VALUE = Integer.MIN_VALUE;
	static final long LONG_INT_MAX_VALUE = Integer.MAX_VALUE;

	public static class Result
	{
		public int result;
		public int overflows;

		public Result(int result, int overflows)
		{
			this.result = result;
			this.overflows = overflows;
		}
	}

	public static void main(String[] args)
	{
		int n = 1000000000;
		int x = Integer.MAX_VALUE - (n/2);

		PrismLog log = new PrismPrintStreamLog(System.out);
		StopWatch watch = new StopWatch(log).start("Add");
		Result result = countOverflowsAdd(x, n);
		watch.stop();
		log.println("#overflows: " + result.overflows);
		log.println("result:     " + result.result);
	}

	public static final Result countOverflowsAdd(int x, int n)
	{
		int overflows = 0;
		int result = 0;
		// x + y
		for (int y=0; y<=n; y++) {
			if (isOverflowAdd(x, y)) {
				overflows++;
			}
			result = (y + x);
		}
		// x + (-y)
		int m = -n;
		for (int y=0; y>=m; y--) {
			if (isOverflowAdd(y, x)) {
				overflows++;
			}
			result = (x + y);
		}
		return new Result(result, overflows);
	}

//	public static void main(String[] arg)
//	{
//		PrismLog log = new PrismPrintStreamLog(System.out);
//
////		log.println("Benchmark power(base, exponent)");
////		StopWatch watch = new StopWatch(log);
////		watch.start("\"Is it worth the trouble?\"");
////		long result = 1;
////		for (long i=0L; i<10L*Integer.MAX_VALUE; i++) {
//////			result += pow(-4L, 2).longValueExact();
//////			result += pow(-4L, 3).longValueExact();
//////			result += pow(-4L, 30).longValueExact();
////			result += powExact(-4L, 2);
////			result += powExact(-4L, 3);
////			result += powExact(-4L, 30);
////		}
////		watch.stop("result = " + result);
////		log.println();
//
//		benchmarkIsOverflowAddInt(log);
////		benchmarkIsOverflowAddInt(log);
////		benchmarkIsOverflowAddLong(log);
////		benchmarkIsOverflowAddLong(log);
////		benchmarkIsOverflowSubtractInt(log);
////		benchmarkIsOverflowSubtractLong(log);
//		benchmarkIsOverflowMultiplyInt(log);
////		benchmarkIsOverflowMultiplyLong(log);
////		benchmarkFitsIntLong(log);
////		benchmarkFitsIntBigInteger(log);
////		benchmarkFitsLongBigInteger(log);
//	}

	public static void benchmarkIsOverflowAddInt(PrismLog log)
	{
		log.println("Benchmark isOverflowAdd(int, int) pre- vs. postcondition");

		StopWatch watch = new StopWatch(log);
		int b = 40000;
		int n = b * b * 1;

		int xSmallInt = 1;
		int xLargeInt = Integer.MAX_VALUE - n/2;

		double timePre = 0;
		double timePost = 0;
		for (int i=0; i<4; i++) {
			watch.run(() -> runIsOverflowAddInt(xSmallInt, n), "precondition add(int)", "small");
			timePre += watch.elapsedSeconds();
			watch.run(() -> runIsOverflowAddInt(xLargeInt, n), "precondition add(int)", "large");
			timePre += watch.elapsedSeconds();
			watch.run(() -> runIsOverflowPostAddInt(xSmallInt, n), "postcondition add(int)", "small");
			timePost += watch.elapsedSeconds();
			watch.run(() -> runIsOverflowPostAddInt(xLargeInt, n), "postcondition add(int)", "large");
			timePost += watch.elapsedSeconds();
		}
		log.println("Sum time pre:  " + timePre);
		log.println("Sum time post: " + timePost);
		log.println();
	}

	private static long runIsOverflowAddInt(int x, int n)
	{
		int r = 0;
		for (int i=0; i<n; i++) {
			int y = x + i;
			if (!isOverflowAdd(x, y)) {
				r = x + y;
			}
		}
		return r;
	}

	private static int runIsOverflowPostAddInt(int x, int n)
	{
		int r = 0;
		for (int i=0; i<n; i++) {
			int y = x + i;
			int m = x + y;
			if (!isOverflowPostAdd(x, y, m)) {
				r = m;
			}
		}
		return r;
	}

	public static void benchmarkIsOverflowAddLong(PrismLog log)
	{
		log.println("Benchmark isOverflowAdd(long, long) pre- vs. postcondition");

		StopWatch watch = new StopWatch(log);
		long b = 40000;
		long n = b * b * 1;

		long xSmallLong = 1L;
		long xLargeLong = Long.MAX_VALUE - n/2;

		double timePre = 0;
		double timePost = 0;
		for (int i=0; i<4; i++) {
			watch.run(() -> runIsOverflowAddLong(xSmallLong, n), "precondition add(long)", "small");
			timePre += watch.elapsedSeconds();
			watch.run(() -> runIsOverflowAddLong(xLargeLong, n), "precondition add(long)", "large");
			timePre += watch.elapsedSeconds();
			watch.run(() -> runIsOverflowPostAddLong(xSmallLong, n), "postcondition add(long)", "small");
			timePost += watch.elapsedSeconds();
			watch.run(() -> runIsOverflowPostAddLong(xLargeLong, n), "postcondition add(long)", "large");
			timePost += watch.elapsedSeconds();
		}
		log.println("Sum time pre:  " + timePre);
		log.println("Sum time post: " + timePost);
		log.println();
	}

	private static long runIsOverflowAddLong(long x, long n)
	{
		long r = 0L;
		for (long i=0L; i<n; i++) {
			long y = x + i;
			if (!isOverflowAdd(x, y)) {
				r = x + y;
			}
		}
		return r;
	}

	private static long runIsOverflowPostAddLong(long x, long n)
	{
		long r = 0L;
		for (long i=0L; i<n; i++) {
			long y = x + i;
			long m = x + y;
			if (!isOverflowPostAdd(x, y, m)) {
				r = m;
			}
		}
		return r;
	}

	public static void benchmarkIsOverflowSubtractInt(PrismLog log)
	{
		log.println("Benchmark isOverflowSubtract(int, int) pre- vs. postcondition");

		StopWatch watch = new StopWatch(log);
		int b = 40000;
		int n = b * b * 1;

		int xSmallInt = -1;
		int xLargeInt = Integer.MIN_VALUE + n/2;

		for (int i=0; i<4; i++) {
			watch.run(() -> runIsOverflowSubtractInt(xSmallInt, n), "precondition subtract(int)", "small");
			watch.run(() -> runIsOverflowSubtractInt(xLargeInt, n), "precondition subtract(int)", "large");
			watch.run(() -> runIsOverflowPostSubtractInt(xSmallInt, n), "postcondition subtract(int)", "small");
			watch.run(() -> runIsOverflowPostSubtractInt(xLargeInt, n), "postcondition subtract(int)", "large");
		}
		log.println();
	}

	private static long runIsOverflowSubtractInt(int x, int n)
	{
		int r = 0;
		for (int i=0; i<n; i++) {
			int y = x + i;
			if (!isOverflowSubtract(x, y)) {
				r = x - y;
			}
		}
		return r;
	}

	private static int runIsOverflowPostSubtractInt(int x, int n)
	{
		int r = 0;
		for (int i=0; i<n; i++) {
			int y = x + i;
			int m = x - y;
			if (!isOverflowPostSubtract(x, y, m)) {
				r = m;
			}
		}
		return r;
	}

	public static void benchmarkIsOverflowSubtractLong(PrismLog log)
	{
		log.println("Benchmark isOverflowSubtract(long, long) pre- vs. postcondition");

		StopWatch watch = new StopWatch(log);
		long b = 40000;
		long n = b * b * 1;

		long xSmallLong = -1L;
		long xLargeLong = Long.MIN_VALUE + n/2;

		for (int i=0; i<4; i++) {
			watch.run(() -> runIsOverflowSubtractLong(xSmallLong, n), "precondition subtract(long)", "small");
			watch.run(() -> runIsOverflowSubtractLong(xLargeLong, n), "precondition subtract(long)", "large");
			watch.run(() -> runIsOverflowPostSubtractLong(xSmallLong, n), "postcondition subtract(long)", "small");
			watch.run(() -> runIsOverflowPostSubtractLong(xLargeLong, n), "postcondition subtract(long)", "large");
		}
		log.println();
	}

	private static long runIsOverflowSubtractLong(long x, long n)
	{
		long r = 0L;
		for (long i=0L; i<n; i++) {
			long y = x + i;
			if (!isOverflowSubtract(x, y)) {
				r = x - y;
			}
		}
		return r;
	}

	private static long runIsOverflowPostSubtractLong(long x, long n)
	{
		long r = 0L;
		for (long i=0L; i<n; i++) {
			long y = x + i;
			long m = x - y;
			if (!isOverflowPostSubtract(x, y, m)) {
				r = m;
			}
		}
		return r;
	}

	public static void benchmarkIsOverflowMultiplyInt(PrismLog log)
	{
		log.println("Benchmark isOverflowMultiply(int, int) pre- vs. postcondition");

		StopWatch watch = new StopWatch(log);
		int b = 40000;
		int n = b * b * 1;

		int xSmallInt = 1;
		int xLargeInt = Integer.MAX_VALUE - n/2;

		double timePre = 0;
		double timePost = 0;
		for (int i=0; i<4; i++) {
			watch.run(() -> runIsOverflowMultiplyInt(xSmallInt, n), "precondition multiply(int)", "small");
			timePre += watch.elapsedSeconds();
			watch.run(() -> runIsOverflowMultiplyInt(xLargeInt, n), "precondition multiply(int)", "large");
			timePre += watch.elapsedSeconds();
			watch.run(() -> runIsOverflowPostMultiplyInt(xSmallInt, n), "postcondition multiply(int)", "small");
			timePost += watch.elapsedSeconds();
			watch.run(() -> runIsOverflowPostMultiplyInt(xLargeInt, n), "postcondition multiply(int)", "large");
			timePost += watch.elapsedSeconds();
		}
		log.println("Sum time pre:  " + timePre);
		log.println("Sum time post: " + timePost);
		log.println();
	}

	private static int runIsOverflowMultiplyInt(int x, int n)
	{
		int r = 0;
		for (int i=0; i<n; i++) {
			int y = x + i;
			if (!isOverflowMultiply(x, y)) {
				r = x * y;
			}
		}
		return r;
	}

	private static int runIsOverflowPostMultiplyInt(int x, int n)
	{
		int r = 0;
		for (int i=0; i<n; i++) {
			int y = x + i;
			int m = x * y;
			if (!isOverflowPostMultiply(x, y, m)) {
				r = m;
			}
		}
		return r;
	}

	public static void benchmarkIsOverflowMultiplyLong(PrismLog log)
	{
		log.println("Benchmark isOverflowMultiply(long, long) pre- vs. postcondition");

		StopWatch watch = new StopWatch(log);
		long b = 40000;
		long n = b*b*1;

		long xSmallLong = 1L;
		long xLargeLong = Long.MAX_VALUE - n/2;

		for (int i=0; i<4; i++) {
			watch.run(() -> runIsOverflowMultiplyLong(xSmallLong, n), "precondition multiply(long)", "small");
			watch.run(() -> runIsOverflowMultiplyLong(xLargeLong, n), "precondition multiply(long)", "large");
			watch.run(() -> runIsOverflowPostMultiplyLong(xSmallLong, n), "postcondition multiply(long)", "small");
			watch.run(() -> runIsOverflowPostMultiplyLong(xLargeLong, n), "postcondition multiply(long)", "large");
		}
		log.println();
	}

	private static long runIsOverflowMultiplyLong(long x, long n)
	{
		long r = 0L;
		for (long i=0L; i<n; i++) {
			long y = x + i;
			if (!isOverflowMultiply(x, y)) {
				r = x * y;
			}
		}
		return r;
	}

	private static long runIsOverflowPostMultiplyLong(long x, long n)
	{
		long r = 0L;
		for (long i=0L; i<n; i++) {
			long y = x + i;
			long m = x * y;
			if (!isOverflowPostMultiply(x, y, m)) {
				r = m;
			}
		}
		return r;
	}

	public static void benchmarkFitsIntLong(PrismLog log)
	{
		log.println("Benchmark isInt(long) range test vs. upcast");

		StopWatch watch = new StopWatch(log);
		int n = 8;
		Boolean bC = watch.run(() -> runFitsIntLongCompare(n), "isInt(long)", "(compare)");
		Boolean bE = watch.run(() -> runFitsIntLongEquals(n), "isInt(long)", "(equals)");
		log.println();
	}

	public static void benchmarkFitsIntBigInteger(PrismLog log)
	{
		log.println("Benchmark fitsInt(BigInteger) range test vs. upcast");

		StopWatch watch = new StopWatch(log);
		int n = 1;
		Boolean bC = watch.run(() -> runFitsIntBigIntegerCompare(n), "isInt(BigInteger)", "(compare)");
		Boolean bE = watch.run(() -> runFitsIntBigIntegerEquals(n), "isInt(BigInteger)", "(equals)");
		log.println();
	}

	public static void benchmarkFitsLongBigInteger(PrismLog log)
	{
		log.println("Benchmark fitsLong(BigInteger) range test vs. upcast");

		StopWatch watch = new StopWatch(log);
		int n = 1;
		Boolean bA = watch.run(() -> runFitsLongBigIntegerCompare(n), "isLong(BigInteger)", "(compare)");
		Boolean bE = watch.run(() -> runFitsLongBigIntegerEquals(n), "isLong(BigInteger)", "(equals)");
		log.println();
	}

	private static boolean runFitsIntLongCompare(int r)
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

	private static boolean runFitsIntLongEquals(int r)
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

	private static boolean runFitsIntBigIntegerCompare(int r)
	{
		boolean result = false;
		final long range = Integer.MAX_VALUE;
		final long start = Integer.MAX_VALUE - range;
		for (; r>0; r--) {
			for (long j=range; j >=0L; j--) {
				for (int s=-1; s<= 1; s+=2) {
					BigInteger bigN = s<0 ? BigInteger.valueOf(-(start + j))
							: BigInteger.valueOf(start + j);
					result = BIG_INT_MIN_VALUE.compareTo(bigN) <= 0 && bigN.compareTo(BIG_INT_MAX_VALUE) <= 0;
				}
			}
		}
		return result;
	}

	private static boolean runFitsIntBigIntegerEquals(int r)
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

	private static boolean runFitsLongBigIntegerCompare(int r)
	{
		boolean result = false;
		final long range = Integer.MAX_VALUE;
		final long start = Integer.MAX_VALUE - range;
		for (; r>0; r--) {
			for (long j=range; j >=0L; j--) {
				for (int s=-1; s<= 1; s+=2) {
					BigInteger bigN = s < 0 ? BigInteger.valueOf(-start).add(BigInteger.valueOf(-j))
							: BigInteger.valueOf(start).add(BigInteger.valueOf(j));
					result = BIG_LONG_MIN_VALUE.compareTo(bigN) <= 0 && bigN.compareTo(BIG_LONG_MAX_VALUE) <= 0;
				}
			}
		}
		return result;
	}

	private static boolean runFitsLongBigIntegerEquals(int r)
	{
		boolean result = false;
		final long range = Integer.MAX_VALUE;
		final long start = Integer.MAX_VALUE - range;
		for (; r>0; r--) {
			for (long j=range; j >= 0L; j--) {
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
