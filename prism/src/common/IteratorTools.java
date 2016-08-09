package common;

import java.util.Iterator;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfDouble;
import java.util.PrimitiveIterator.OfInt;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

import common.functions.Predicate;
import common.iterable.FunctionalIterator;
import common.iterable.FunctionalPrimitiveIterator;
import common.iterable.FilteringIterator;
import common.iterable.MappingIterator;

public class IteratorTools
{
	@Deprecated
	public static int count(final Iterable<?> iterable)
	{
		return count(iterable.iterator());
	}

	@Deprecated
	public static int count(final Iterator<?> iterator)
	{
		return FunctionalIterator.extend(iterator).count();
	}

	@Deprecated
	public static int count(PrimitiveIterator<?, ?> iterator)
	{
		return FunctionalIterator.extend(iterator).count();
	}

	@Deprecated
	public static <T> int count(Iterable<T> iterable, Predicate<? super T> predicate)
	{
		return count(iterable.iterator(), predicate);
	}

	@Deprecated
	public static <T> int count(Iterator<T> iterator, Predicate<? super T> predicate)
	{
		return FunctionalIterator.extend(iterator).filter(predicate).count();
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

	public static OptionalInt maxInt(final Iterator<Integer> numbers)
	{
		return max(MappingIterator.toInt(FilteringIterator.nonNull(numbers)));
	}

	public static OptionalInt max(final OfInt numbers)
	{
		if(! numbers.hasNext()) {
			return OptionalInt.empty();
		}
		int max = numbers.nextInt();
		while (numbers.hasNext()) {
			final int next = numbers.nextInt();
			max = next > max ? next : max; 
		}
		return OptionalInt.of(max);
	}

	public static OptionalLong maxLong(final Iterator<Long> numbers)
	{
		return max(MappingIterator.toLong(FilteringIterator.nonNull(numbers)));
	}

	public static OptionalLong max(final OfLong numbers)
	{
		if(! numbers.hasNext()) {
			return OptionalLong.empty();
		}
		long max = numbers.nextLong();
		while (numbers.hasNext()) {
			final long next = numbers.next();
			max = next > max ? next : max; 
		}
		return OptionalLong.of(max);
	}

	public static OptionalDouble maxDouble(final Iterator<Double> numbers)
	{
		return max(MappingIterator.toDouble(FilteringIterator.nonNull(numbers)));
	}

	public static OptionalDouble max(final OfDouble numbers)
	{
		if(! numbers.hasNext()) {
			return OptionalDouble.empty();
		}
		double max = numbers.nextDouble();
		while (numbers.hasNext()) {
			final double next = numbers.next();
			max = next > max ? next : max; 
		}
		return OptionalDouble.of(max);
	}

	public static OptionalInt minInt(final Iterator<Integer> numbers)
	{
		return min(MappingIterator.toInt(FilteringIterator.nonNull(numbers)));
	}

	public static OptionalInt min(final OfInt numbers)
	{
		if(! numbers.hasNext()) {
			return OptionalInt.empty();
		}
		int min = numbers.nextInt();
		while (numbers.hasNext()) {
			final int next = numbers.nextInt();
			min = next < min ? next : min;
		}
		return OptionalInt.of(min);
	}

	public static OptionalLong minLong(final Iterator<Long> numbers)
	{
		return min(MappingIterator.toLong(FilteringIterator.nonNull(numbers)));
	}

	public static OptionalLong min(final OfLong numbers)
	{
		if(! numbers.hasNext()) {
			return OptionalLong.empty();
		}
		long min = numbers.nextLong();
		while (numbers.hasNext()) {
			final long next = numbers.nextLong();
			min = next < min ? next : min; 
		}
		return OptionalLong.of(min);
	}

	public static OptionalDouble minDouble(final Iterator<Double> numbers)
	{
		return min(MappingIterator.toDouble(FilteringIterator.nonNull(numbers)));
	}

	public static OptionalDouble min(final OfDouble numbers)
	{
		if(! numbers.hasNext()) {
			return OptionalDouble.empty();
		}
		double min = numbers.nextDouble();
		while (numbers.hasNext()) {
			final double next = numbers.nextDouble();
			min = next < min ? next : min; 
		}
		return OptionalDouble.of(min);
	}

	public static int sumInt(final Iterator<Integer> numbers)
	{
		return sum(MappingIterator.toInt(numbers));
	}

	public static int sum(final OfInt numbers)
	{
		int sum = 0;
		while (numbers.hasNext()) {
			sum += numbers.nextInt();
		}
		return sum;
	}

	public static long sumLong(final Iterator<Long> numbers)
	{
		return sum(MappingIterator.toLong(numbers));
	}

	public static long sum(final OfLong numbers)
	{
		long sum = 0;
		while (numbers.hasNext()) {
			sum += numbers.nextLong();
		}
		return sum;
	}

	public static double sumDouble(final Iterator<Double> numbers)
	{
		return sum(MappingIterator.toDouble(numbers));
	}

	public static double sum(final OfDouble numbers)
	{
		double sum = 0;
		while (numbers.hasNext()) {
			sum += numbers.nextDouble();
		}
		return sum;
	}

	@Deprecated
	public static <T> Optional<T> reduce(Iterable<T> iterable, BinaryOperator<T> accumulator)
	{
		return reduce(iterable.iterator(), accumulator);
	}

	@Deprecated
	public static <T> Optional<T> reduce(Iterator<T> iterator, BinaryOperator<T> accumulator)
	{
		return FunctionalIterator.extend(iterator).reduce(accumulator);
	}

	@Deprecated
	public static int reduce(OfInt iterator, int identity, IntBinaryOperator accumulator)
	{
		return ((FunctionalPrimitiveIterator.OfInt) FunctionalIterator.extend(iterator)).reduce(identity, accumulator);
	}

	@Deprecated
	public static long reduce(OfLong iterator, long identity, LongBinaryOperator accumulator)
	{
		return ((FunctionalPrimitiveIterator.OfLong) FunctionalIterator.extend(iterator)).reduce(identity, accumulator);
	}

	@Deprecated
	public static double reduce(OfDouble iterator, double identity, DoubleBinaryOperator accumulator)
	{
		return ((FunctionalPrimitiveIterator.OfDouble) FunctionalIterator.extend(iterator)).reduce(identity, accumulator);
	}

	public static <T> void printIterator(final String name, final Iterator<T> iterator)
	{
		System.out.print(name + " = ");
		System.out.print(FunctionalIterator.extend(iterator).asString());
		System.out.println();
	}
}