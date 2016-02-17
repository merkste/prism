package common;

import java.util.Arrays;
import java.util.Collection;

public class PrimitiveArrayConverter
{
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
		return convertToIntegerArray(numbers, IteratorTools.count(numbers));
	}

	public static int[] convertToIntegerArray(final Iterable<Integer> numbers, final int size)
	{
		final int[] primitiveArray = new int[size];
		int index = 0;
		for (int n : numbers) {
			primitiveArray[index++] = n;
		}
		return primitiveArray;
	}

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
		return convertToDoubleArray(numbers, IteratorTools.count(numbers));
	}

	public static double[] convertToDoubleArray(final Iterable<Double> numbers, final int size)
	{
		final double[] primitiveArray = new double[size];
		int index = 0;
		for (double n : numbers) {
			primitiveArray[index++] = n;
		}
		return primitiveArray;
	}
}