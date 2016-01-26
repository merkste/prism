package common.iterable;

import java.util.PrimitiveIterator.OfInt;
import java.util.stream.IntStream;

public class Interval implements IterableInt
{
	final int lowerBound;
	final int upperBound;
	final int step;

	public Interval(int upperBound)
	{
		this(0, upperBound);
	}

	public Interval(final int lowerBound, final int upperBound)
	{
		this(lowerBound, upperBound, 1);
	}

	public Interval(final int lowerBound, final int upperBound, final int step)
	{
		assert step > 0 : "positive step width expected";
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.step = step;
	}

	@Override
	public OfInt iterator()
	{
		return stream().iterator();
	}

	public IntStream stream()
	{
		final int width = upperBound-lowerBound;
		final IntStream range = IntStream.range(0, (int) Math.ceil((double) width / step));
		return range.map(x -> step * x + lowerBound);
	}

	public String toString()
	{
		return getClass().getSimpleName() + "(" + lowerBound + ", " + upperBound + ", " + step + ")";
	}

	public static void main(final String[] args)
	{
		Interval interval = new Interval(-3, 5);
		System.out.print(interval + "  = [");
		for (int i : interval) {
			System.out.print(i + ",");
		}
		System.out.println("]");

		interval = new Interval(-3, 3, 2);
		System.out.print(interval + "  = [");
		for (int i : interval) {
			System.out.print(i + ",");
		}
		System.out.println("]");

		interval = new Interval(-3, 5, 3);
		System.out.print(interval + "  = [");
		for (int i : interval) {
			System.out.print(i + ",");
		}
		System.out.println("]");

		interval = new Interval(-3, -3);
		System.out.print(interval + " = [");
		for (int i : interval) {
			System.out.print(i + ",");
		}
		System.out.println("]");
	}
}