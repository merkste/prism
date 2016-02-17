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

	public static int sumIntegers(final Iterator<Integer> numbers)
	{
		int sum = 0;
		while (numbers.hasNext()) {
			sum += numbers.next();
		}
		return sum;
	}

	public static double sumDoubles(final Iterator<Double> numbers)
	{
		double sum = 0;
		while (numbers.hasNext()) {
			sum += numbers.next();
		}
		return sum;
	}

	public static float sumFloats(final Iterator<Float> numbers)
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