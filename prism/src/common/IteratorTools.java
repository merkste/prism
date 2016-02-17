package common;

import java.util.Iterator;

import common.functions.Predicate;

public class IteratorTools
{
	public static int count(final Iterable<?> iterable)
	{
		return count(iterable.iterator());
	}

	public static int count(final Iterator<?> iterator)
	{
		int count = 0;
		while (iterator.hasNext()) {
			iterator.next();
			count++;
		}
		return count;
	}

	public static boolean and(final Iterator<Boolean> booleans)
	{
		while (booleans.hasNext()) {
			if (!booleans.next()) {
				return false;
			}
		}
		return true;
	}

	public static boolean or(final Iterator<Boolean> booleans)
	{
		while (booleans.hasNext()) {
			if (booleans.next()) {
				return true;
			}
		}
		return false;
	}

	public static int maxInteger(final Iterator<Integer> numbers)
	{
		return maxInteger(numbers, Integer.MIN_VALUE);
	}

	public static int maxInteger(final Iterator<Integer> numbers, final int value)
	{
		int max = value;
		while (numbers.hasNext()) {
			final int next = numbers.next();
			max = next > max ? next : max; 
		}
		return max;
	}

	public static double maxDouble(final Iterator<Double> numbers)
	{
		return maxDouble(numbers, Double.MIN_VALUE);
	}

	public static double maxDouble(final Iterator<Double> numbers, final double value)
	{
		double max = value;
		while (numbers.hasNext()) {
			final double next = numbers.next();
			max = next > max ? next : max; 
		}
		return max;
	}

	public static float maxFloat(final Iterator<Float> numbers)
	{
		return maxFloat(numbers, Float.MIN_VALUE);
	}

	public static float maxFloat(final Iterator<Float> numbers, final float value)
	{
		float max = value;
		while (numbers.hasNext()) {
			final float next = numbers.next();
			max = next > max ? next : max; 
		}
		return max;
	}

	public static int minInteger(final Iterator<Integer> numbers)
	{
		return minInteger(numbers, Integer.MAX_VALUE);
	}

	public static int minInteger(final Iterator<Integer> numbers, final int value)
	{
		int min = value;
		while (numbers.hasNext()) {
			final int next = numbers.next();
			min = next < min ? next : min; 
		}
		return min;
	}

	public static double minDouble(final Iterator<Double> numbers)
	{
		return minDouble(numbers, Double.MAX_VALUE);
	}

	public static double minDouble(final Iterator<Double> numbers, final double value)
	{
		double min = value;
		while (numbers.hasNext()) {
			final double next = numbers.next();
			min = next < min ? next : min; 
		}
		return min;
	}

	public static float minFloat(final Iterator<Float> numbers)
	{
		return minFloat(numbers, Float.MAX_VALUE);
	}

	public static float minFloat(final Iterator<Float> numbers, final float value)
	{
		float min = value;
		while (numbers.hasNext()) {
			final float next = numbers.next();
			min = next < min ? next : min; 
		}
		return min;
	}

	public static int sumInteger(final Iterator<Integer> numbers)
	{
		int sum = 0;
		while (numbers.hasNext()) {
			sum += numbers.next();
		}
		return sum;
	}

	public static double sumDouble(final Iterator<Double> numbers)
	{
		double sum = 0;
		while (numbers.hasNext()) {
			sum += numbers.next();
		}
		return sum;
	}

	public static float sumFloat(final Iterator<Float> numbers)
	{
		float sum = 0;
		while (numbers.hasNext()) {
			sum += numbers.next();
		}
		return sum;
	}

	public static <T> int count(final Iterable<T> iterable, final Predicate<T> predicate)
	{
		return count(iterable.iterator(), predicate);
	}

	public static <T> int count(final Iterator<T> iterator, final Predicate<T> predicate)
	{
		int count = 0;
		while (iterator.hasNext()) {
			if (predicate.getBoolean(iterator.next())) {
				count++;
			}
			;
		}
		return count;
	}

	public static <T> void printIterator(final String name, final Iterator<T> iter)
	{
		System.out.print(name + " = [");
		while (iter.hasNext()) {
			System.out.print(iter.next());
			if (iter.hasNext()) {
				System.out.print(", ");
			}
		}
		System.out.println("]");
	}
}