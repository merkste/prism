package common;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import common.iterable.FunctionalIterable;

public class PrimitiveArrayConverter
{
	public static double[] convertToDoubleArray(final Double[] numbers)
	{
		return convertToDoubleArray(Arrays.asList(numbers));
	}

	public static double[] convertToDoubleArray(final Collection<Double> numbers)
	{
		return convertToDoubleArray(numbers, numbers.size());
	}

	public static double[] convertToDoubleArray(final Iterable<Double> numbers)
	{
		if (numbers instanceof Collection) {
			return convertToDoubleArray((Collection<Double>) numbers);
		}
		return convertToDoubleArray(numbers, FunctionalIterable.extend(numbers).count());
	}

	public static double[] convertToDoubleArray(final Iterable<Double> numbers, final int size)
	{
		return FunctionalIterable.extend(numbers).map((ToDoubleFunction<Double>) Double::doubleValue).collect(new double[size]);
	}

	public static int[] convertToIntegerArray(final Integer[] numbers)
	{
		return convertToIntegerArray(Arrays.asList(numbers));
	}

	public static int[] convertToIntegerArray(final Collection<Integer> numbers)
	{
		return convertToIntegerArray(numbers, numbers.size());
	}

	public static int[] convertToIntegerArray(final Iterable<Integer> numbers)
	{
		if (numbers instanceof Collection) {
			return convertToIntegerArray((Collection<Integer>) numbers);
		}
		return convertToIntegerArray(numbers, FunctionalIterable.extend(numbers).count());
	}

	public static int[] convertToIntegerArray(final Iterable<Integer> numbers, final int size)
	{
		return FunctionalIterable.extend(numbers).map((ToIntFunction<Integer>) Integer::intValue).collect(new int[size]);
	}

	public static long[] convertToLongArray(final Long[] numbers)
	{
		return convertToLongArray(Arrays.asList(numbers));
	}

	public static long[] convertToLongArray(final Collection<Long> numbers)
	{
		return convertToLongArray(numbers, numbers.size());
	}

	public static long[] convertToLongArray(final Iterable<Long> numbers)
	{
		if (numbers instanceof Collection) {
			return convertToLongArray((Collection<Long>) numbers);
		}
		return convertToLongArray(numbers, FunctionalIterable.extend(numbers).count());
	}

	public static long[] convertToLongArray(final Iterable<Long> numbers, final int size)
	{
		return FunctionalIterable.extend(numbers).map((ToLongFunction<Long>) Long::longValue).collect(new long[size]);
	}

}