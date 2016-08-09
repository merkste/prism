package common.iterable;

import java.util.function.DoubleFunction;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongFunction;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public abstract class MappingIterable<S, T> implements FunctionalIterable<T>
{
	protected final Iterable<S> iterable;

	public MappingIterable(Iterable<S> iterable)
	{
		this.iterable = iterable;
	}

	public static class From<S, T> extends MappingIterable<S, T>
	{
		protected final Function<? super S, ? extends T> function;

		public From(Iterable<S> iterable, Function<? super S, ? extends T> function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalIterator<T> iterator()
		{
			return new MappingIterator.From<>(iterable, function);
		}
	}

	public static IterableInt toInt(Iterable<Integer> iterable)
	{
		if (iterable instanceof IterableInt) {
			return (IterableInt) iterable;
		}
		return new ToInt<>(iterable, Integer::intValue);
	}

	public static class ToInt<S> extends MappingIterable<S, Integer> implements IterableInt
	{
		protected ToIntFunction<? super S> function;

		public ToInt(Iterable<S> iterable, ToIntFunction<? super S> function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new MappingIterator.ToInt<>(iterable, function);
		}

	}

	public static IterableDouble toDouble(Iterable<Double> iterable)
	{
		if (iterable instanceof IterableDouble) {
			return (IterableDouble) iterable;
		}
		return new ToDouble<>(iterable, Double::intValue);
	}

	public static class ToDouble<S> extends MappingIterable<S, Double> implements IterableDouble
	{
		protected ToDoubleFunction<? super S> function;

		public ToDouble(Iterable<S> iterable, ToDoubleFunction<? super S> function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new MappingIterator.ToDouble<>(iterable, function);
		}
	}

	public static IterableLong toLong(Iterable<Long> iterable)
	{
		if (iterable instanceof IterableLong) {
			return (IterableLong) iterable;
		}
		return new ToLong<>(iterable, Long::intValue);
	}

	public static class ToLong<S> extends MappingIterable<S, Long> implements IterableLong
	{
		protected ToLongFunction<? super S> function;

		public ToLong(Iterable<S> iterable, ToLongFunction<? super S> function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new MappingIterator.ToLong<>(iterable, function);
		}
	}

	public static class FromInt<T> extends MappingIterable<Integer, T>
	{
		protected IntFunction<? extends T> function;

		public FromInt(IterableInt iterable, IntFunction<? extends T> function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalIterator<T> iterator()
		{
			return new MappingIterator.FromInt<T>((IterableInt) iterable, function);
		}
	}

	public static class FromIntToInt extends MappingIterable<Integer, Integer> implements IterableInt
	{
		protected IntUnaryOperator function;

		public FromIntToInt(IterableInt iterable, IntUnaryOperator function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new MappingIterator.FromIntToInt((IterableInt) iterable, function);
		}
	}

	public static class FromIntToDouble extends MappingIterable<Integer, Double> implements IterableDouble
	{
		protected IntToDoubleFunction function;

		public FromIntToDouble(IterableInt iterable, IntToDoubleFunction function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new MappingIterator.FromIntToDouble((IterableInt) iterable, function);
		}
	}

	public static class FromIntToLong extends MappingIterable<Integer, Long> implements IterableLong
	{
		protected IntToLongFunction function;

		public FromIntToLong(IterableInt iterable, IntToLongFunction function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new MappingIterator.FromIntToLong((IterableInt) iterable, function);
		}
	}

	public static class FromDouble<T> extends MappingIterable<Double, T>
	{
		protected DoubleFunction<? extends T> function;

		public FromDouble(IterableDouble iterable, DoubleFunction<? extends T> function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalIterator<T> iterator()
		{
			return new MappingIterator.FromDouble<>((IterableDouble) iterable, function);
		}
	}

	public static class FromDoubleToInt extends MappingIterable<Double, Integer> implements IterableInt
	{
		protected DoubleToIntFunction function;

		public FromDoubleToInt(IterableDouble iterable, DoubleToIntFunction function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new MappingIterator.FromDoubleToInt((IterableDouble) iterable, function);
		}
	}

	public static class FromDoubleToDouble extends MappingIterable<Double, Double> implements IterableDouble
	{
		protected DoubleUnaryOperator function;

		public FromDoubleToDouble(IterableDouble iterable, DoubleUnaryOperator function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new MappingIterator.FromDoubleToDouble((IterableDouble) iterable, function);
		}
	}

	public static class FromDoubleToLong extends MappingIterable<Double, Long> implements IterableLong
	{
		protected DoubleToLongFunction function;

		public FromDoubleToLong(IterableDouble iterable, DoubleToLongFunction function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new MappingIterator.FromDoubleToLong((IterableDouble) iterable, function);
		}
	}

	public static class FromLong<T> extends MappingIterable<Long, T>
	{
		protected LongFunction<? extends T> function;

		public FromLong(IterableLong iterable, LongFunction<? extends T> function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalIterator<T> iterator()
		{
			return new MappingIterator.FromLong<>((IterableLong) iterable, function);
		}
	}

	public static class FromLongToInt extends MappingIterable<Long, Integer> implements IterableInt
	{
		protected LongToIntFunction function;

		public FromLongToInt(IterableLong iterable, LongToIntFunction function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new MappingIterator.FromLongToInt((IterableLong) iterable, function);
		}
	}

	public static class FromLongToDouble extends MappingIterable<Long, Double> implements IterableDouble
	{
		protected LongToDoubleFunction function;

		public FromLongToDouble(IterableLong iterable, LongToDoubleFunction function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new MappingIterator.FromLongToDouble((IterableLong) iterable, function);
		}
	}

	public static class FromLongToLong extends MappingIterable<Long, Long> implements IterableLong
	{
		protected LongUnaryOperator function;

		public FromLongToLong(IterableLong iterable, LongUnaryOperator function)
		{
			super(iterable);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new MappingIterator.FromLongToLong((IterableLong) iterable, function);
		}
	}
}