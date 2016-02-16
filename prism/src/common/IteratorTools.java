package common;

import java.util.Iterator;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;

import common.iterable.FilteringIterator;
import common.iterable.MappingIterator;

import java.util.PrimitiveIterator.OfDouble;
import java.util.PrimitiveIterator.OfInt;

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

	public static <T> int count(final Iterable<T> iterable, final Predicate<? super T> predicate)
	{
		return count(iterable.iterator(), predicate);
	}

	public static <T> int count(final Iterator<T> iterator, final Predicate<? super T> predicate)
	{
		return count(new FilteringIterator.Of<>(iterator, predicate));
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

	public static <T> Optional<T> reduce(Iterator<T> iterator, BinaryOperator<T> accumulator)
	{
		if (! iterator.hasNext()) {
			return Optional.empty();
		}
		return Optional.of(reduce(iterator, iterator.next(), accumulator));
	}

	public static <T> T reduce(Iterator<T> iterator, T identity, BinaryOperator<T> accumulator)
	{
		T result = identity;
		while(iterator.hasNext()) {
			result = accumulator.apply(result, iterator.next());
		}
		return result;
	}

	public static OptionalInt reduce(OfInt iterator, IntBinaryOperator accumulator)
	{
		if (! iterator.hasNext()) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(reduce(iterator, iterator.nextInt(), accumulator));
	}

	public static int reduce(OfInt iterator, int identity, IntBinaryOperator accumulator)
	{
		int result = identity;
		while(iterator.hasNext()) {
			result = accumulator.applyAsInt(result, iterator.nextInt());
		}
		return result;
	}

	public static OptionalDouble reduce(OfDouble iterator, DoubleBinaryOperator accumulator)
	{
		if (! iterator.hasNext()) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of(reduce(iterator, iterator.nextDouble(), accumulator));
	}

	public static double reduce(OfDouble iterator, double identity, DoubleBinaryOperator accumulator)
	{
		double result = identity;
		while(iterator.hasNext()) {
			result = accumulator.applyAsDouble(result, iterator.nextDouble());
		}
		return result;
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