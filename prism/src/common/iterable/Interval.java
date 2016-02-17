package common.iterable;

import java.util.NoSuchElementException;

import common.iterable.primitive.IterableInteger;
import common.iterable.primitive.IteratorInteger;

public class Interval implements IterableInteger
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
	public IteratorInteger iterator()
	{
		return new IteratorInteger()
		{
			private int next = lowerBound;

			@Override
			public boolean hasNext()
			{
				return next < upperBound;
			}

			@Override
			public Integer next()
			{
				return nextInteger();
			}

			@Override
			public int nextInteger()
			{
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				final int current = next;
				next += step;
				return current;
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException("removing not supported");
			}
		};
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