package common;

import java.util.Iterator;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator.OfDouble;
import java.util.PrimitiveIterator.OfInt;
import java.util.PrimitiveIterator.OfLong;

import common.iterable.FunctionalIterator;
import common.iterable.MappingIterator;

public class IteratorTools
{
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

	public static OptionalDouble max(final OfDouble numbers)
	{
		return FunctionalIterator.extend(numbers).max();
	}

	public static OptionalInt max(final OfInt numbers)
	{
		return FunctionalIterator.extend(numbers).max();
	}

	public static OptionalLong max(final OfLong numbers)
	{
		return FunctionalIterator.extend(numbers).max();
	}

	public static OptionalDouble maxDouble(final Iterator<Double> numbers)
	{
		return MappingIterator.toDouble(FunctionalIterator.extend(numbers).nonNull()).max();
	}

	public static OptionalInt maxInt(final Iterator<Integer> numbers)
	{
		return MappingIterator.toInt(FunctionalIterator.extend(numbers).nonNull()).max();
	}

	public static OptionalLong maxLong(final Iterator<Long> numbers)
	{
		return MappingIterator.toLong(FunctionalIterator.extend(numbers).nonNull()).max();
	}

	public static OptionalInt min(final OfInt numbers)
	{
		return FunctionalIterator.extend(numbers).min();
	}

	public static OptionalLong min(final OfLong numbers)
	{
		return FunctionalIterator.extend(numbers).min();
	}

	public static OptionalDouble min(final OfDouble numbers)
	{
		return FunctionalIterator.extend(numbers).min();
	}

	public static OptionalDouble minDouble(final Iterator<Double> numbers)
	{
		return MappingIterator.toDouble(FunctionalIterator.extend(numbers).nonNull()).min();
	}

	public static OptionalInt minInt(final Iterator<Integer> numbers)
	{
		return MappingIterator.toInt(FunctionalIterator.extend(numbers).nonNull()).min();
	}

	public static OptionalLong minLong(final Iterator<Long> numbers)
	{
		return MappingIterator.toLong(FunctionalIterator.extend(numbers).nonNull()).min();
	}

	public static double sum(final OfDouble numbers)
	{
		return FunctionalIterator.extend(numbers).sum();
	}

	public static int sum(final OfInt numbers)
	{
		return FunctionalIterator.extend(numbers).sum();
	}

	public static long sum(final OfLong numbers)
	{
		return FunctionalIterator.extend(numbers).sum();
	}

	public static double sumDouble(final Iterator<Double> numbers)
	{
		return MappingIterator.toDouble(FunctionalIterator.extend(numbers).nonNull()).sum();
	}

	public static int sumInt(final Iterator<Integer> numbers)
	{
		return MappingIterator.toInt(FunctionalIterator.extend(numbers).nonNull()).sum();
	}

	public static long sumLong(final Iterator<Long> numbers)
	{
		return MappingIterator.toLong(FunctionalIterator.extend(numbers).nonNull()).sum();
	}

	public static <T> void printIterator(final String name, final Iterator<T> iterator)
	{
		System.out.print(name + " = ");
		System.out.print(FunctionalIterator.extend(iterator).asString());
		System.out.println();
	}
}