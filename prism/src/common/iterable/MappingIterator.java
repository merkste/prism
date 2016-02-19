package common.iterable;

import java.util.Iterator;
import java.util.PrimitiveIterator.OfDouble;
import java.util.PrimitiveIterator.OfInt;
import java.util.PrimitiveIterator.OfLong;
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

public abstract class MappingIterator<S, T> implements Iterator<T>
{
	protected final Iterator<S> iterator;

	public MappingIterator(Iterable<S> iterable)
	{
		this(iterable.iterator());
	}

	public MappingIterator(Iterator<S> iterator)
	{
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext()
	{
		return iterator.hasNext();
	}

	@Override
	public void remove()
	{
		iterator.remove();
	}

	public static class From<S, T> extends MappingIterator<S, T>
	{
		protected final Function<? super S, T> function;

		public From(Iterable<S> iterable, Function<? super S, T> function)
		{
			this(iterable.iterator(), function);
		}

		public From(Iterator<S> iterator, Function<? super S, T> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public T next()
		{
			return function.apply(iterator.next());
		}
	}

	public static OfInt toInt(Iterable<Integer> iterable)
	{
		return toInt(iterable.iterator());
	}

	public static OfInt toInt(Iterator<Integer> iterator)
	{
		if (iterator instanceof OfInt) {
			return (OfInt) iterator;
		}
		return new ToInt<>(iterator, Integer::intValue);
	}

	public static class ToInt<S> extends MappingIterator<S, Integer> implements OfInt
	{
		protected ToIntFunction<? super S> function;

		public ToInt(Iterable<S> iterable, ToIntFunction<? super S> function)
		{
			this(iterable.iterator(), function);
		}

		public ToInt(Iterator<S> iterator, ToIntFunction<? super S> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public int nextInt()
		{
			return function.applyAsInt(iterator.next());
		}
	}

	public static OfDouble toDouble(Iterable<Double> iterable)
	{
		return toDouble(iterable.iterator());
	}

	public static OfDouble toDouble(Iterator<Double> iterator)
	{
		if (iterator instanceof OfDouble) {
			return (OfDouble) iterator;
		}
		return new ToDouble<>(iterator, Double::doubleValue);
	}

	public static class ToDouble<S> extends MappingIterator<S, Double> implements OfDouble
	{
		protected ToDoubleFunction<? super S> function;

		public ToDouble(Iterable<S> iterable, ToDoubleFunction<? super S> function)
		{
			this(iterable.iterator(), function);
		}

		public ToDouble(Iterator<S> iterator, ToDoubleFunction<? super S> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public double nextDouble()
		{
			return function.applyAsDouble(iterator.next());
		}
	}

	public static OfLong toLong(Iterable<Long> iterable)
	{
		return toLong(iterable.iterator());
	}

	public static OfLong toLong(Iterator<Long> iterator)
	{
		if (iterator instanceof OfLong) {
			return (OfLong) iterator;
		}
		return new ToLong<>(iterator, Long::longValue);
	}

	public static class ToLong<S> extends MappingIterator<S, Long> implements OfLong
	{
		protected ToLongFunction<? super S> function;

		public ToLong(Iterable<S> iterable, ToLongFunction<? super S> function)
		{
			this(iterable.iterator(), function);
		}

		public ToLong(Iterator<S> iterator, ToLongFunction<? super S> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public long nextLong()
		{
			return function.applyAsLong(iterator.next());
		}
	}

	public static class FromInt<T> extends MappingIterator<Integer, T>
	{
		protected IntFunction<T> function;

		public FromInt(IterableInt iterable, IntFunction<T> function)
		{
			this(iterable.iterator(), function);
		}

		public FromInt(OfInt iterator, IntFunction<T> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public T next()
		{
			return function.apply(((OfInt) iterator).nextInt());
		}
	}

	public static class FromIntToInt extends MappingIterator<Integer, Integer> implements OfInt
	{
		protected IntUnaryOperator function;

		public FromIntToInt(IterableInt iterable, IntUnaryOperator function)
		{
			this(iterable.iterator(), function);
		}

		public FromIntToInt(OfInt iterator, IntUnaryOperator function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public int nextInt()
		{
			return function.applyAsInt(((OfInt) iterator).nextInt());
		}
	}

	public static class FromIntToDouble extends MappingIterator<Integer, Double> implements OfDouble
	{
		protected IntToDoubleFunction function;

		public FromIntToDouble(IterableInt iterable, IntToDoubleFunction function)
		{
			this(iterable.iterator(), function);
		}

		public FromIntToDouble(OfInt iterator, IntToDoubleFunction function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public double nextDouble()
		{
			return function.applyAsDouble(((OfInt) iterator).nextInt());
		}
	}

	public static class FromIntToLong extends MappingIterator<Integer, Long> implements OfLong
	{
		protected IntToLongFunction function;

		public FromIntToLong(IterableInt iterable, IntToLongFunction function)
		{
			this(iterable.iterator(), function);
		}

		public FromIntToLong(OfInt iterator, IntToLongFunction function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public long nextLong()
		{
			return function.applyAsLong(((OfInt) iterator).nextInt());
		}
	}

	public static class FromDouble<T> extends MappingIterator<Double, T>
	{
		protected DoubleFunction<T> function;

		public FromDouble(IterableDouble iterable, DoubleFunction<T> function)
		{
			this(iterable.iterator(), function);
		}

		public FromDouble(OfDouble iterator, DoubleFunction<T> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public T next()
		{
			return function.apply(((OfDouble) iterator).nextDouble());
		}
	}

	public static class FromDoubleToInt extends MappingIterator<Double, Integer> implements OfInt
	{
		protected DoubleToIntFunction function;

		public FromDoubleToInt(IterableDouble iterable, DoubleToIntFunction function)
		{
			this(iterable.iterator(), function);
		}

		public FromDoubleToInt(OfDouble iterator, DoubleToIntFunction function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public int nextInt()
		{
			return function.applyAsInt(((OfDouble) iterator).nextDouble());
		}
	}

	public static class FromDoubleToDouble extends MappingIterator<Double, Double> implements OfDouble
	{
		protected DoubleUnaryOperator function;

		public FromDoubleToDouble(IterableDouble iterable, DoubleUnaryOperator function)
		{
			this(iterable.iterator(), function);
		}

		public FromDoubleToDouble(OfDouble iterator, DoubleUnaryOperator function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public double nextDouble()
		{
			return function.applyAsDouble(((OfDouble) iterator).nextDouble());
		}
	}

	public static class FromDoubleToLong extends MappingIterator<Double, Long> implements OfLong
	{
		protected DoubleToLongFunction function;

		public FromDoubleToLong(IterableDouble iterable, DoubleToLongFunction function)
		{
			this(iterable.iterator(), function);
		}

		public FromDoubleToLong(OfDouble iterator, DoubleToLongFunction function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public long nextLong()
		{
			return function.applyAsLong(((OfDouble) iterator).nextDouble());
		}
	}

	public static class FromLong<T> extends MappingIterator<Long, T>
	{
		protected LongFunction<T> function;

		public FromLong(IterableLong iterable, LongFunction<T> function)
		{
			this(iterable.iterator(), function);
		}

		public FromLong(OfLong iterator, LongFunction<T> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public T next()
		{
			return function.apply(((OfLong) iterator).nextLong());
		}
	}

	public static class FromLongToInt extends MappingIterator<Long, Integer> implements OfInt
	{
		protected LongToIntFunction function;

		public FromLongToInt(IterableLong iterable, LongToIntFunction function)
		{
			this(iterable.iterator(), function);
		}

		public FromLongToInt(OfLong iterator, LongToIntFunction function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public int nextInt()
		{
			return function.applyAsInt(((OfLong) iterator).nextLong());
		}
	}

	public static class FromLongToDouble extends MappingIterator<Long, Double> implements OfDouble
	{
		protected LongToDoubleFunction function;

		public FromLongToDouble(IterableLong iterable, LongToDoubleFunction function)
		{
			this(iterable.iterator(), function);
		}

		public FromLongToDouble(OfLong iterator, LongToDoubleFunction function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public double nextDouble()
		{
			return function.applyAsDouble(((OfLong) iterator).nextLong());
		}
	}

	public static class FromLongToLong extends MappingIterator<Long, Long> implements OfLong
	{
		protected LongUnaryOperator function;

		public FromLongToLong(IterableLong iterable, LongUnaryOperator function)
		{
			this(iterable.iterator(), function);
		}

		public FromLongToLong(OfLong iterator, LongUnaryOperator function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public long nextLong()
		{
			return function.applyAsLong(((OfLong) iterator).nextLong());
		}
	}
}