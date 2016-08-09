package common.iterable;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public abstract class SingletonIterator<T> implements FunctionalIterator<T>
{
	public static class Of<T> extends SingletonIterator<T>
	{
		private Optional<T> element;

		public Of(T element)
		{
			this.element = Optional.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public T next()
		{
			Optional<T> result = element;
			element = Optional.empty();
			return result.get();
		}
	}

	public static class OfInt extends SingletonIterator<Integer> implements FunctionalPrimitiveIterator.OfInt
	{
		private OptionalInt element;

		public OfInt(int element)
		{
			this.element = OptionalInt.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public int nextInt()
		{
			OptionalInt result = element;
			element = OptionalInt.empty();
			return result.getAsInt();
		}
	}

	public static class OfLong extends SingletonIterator<Long> implements FunctionalPrimitiveIterator.OfLong
	{
		private OptionalLong element;

		public OfLong(Long element)
		{
			this.element = OptionalLong.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public long nextLong()
		{
			OptionalLong result = element;
			element = OptionalLong.empty();
			return result.getAsLong();
		}
	}

	public static class OfDouble extends SingletonIterator<Double> implements FunctionalPrimitiveIterator.OfDouble
	{
		private OptionalDouble element;

		public OfDouble(double element)
		{
			this.element = OptionalDouble.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public double nextDouble()
		{
			OptionalDouble result = element;
			element = OptionalDouble.empty();
			return result.getAsDouble();
		}
	}
}