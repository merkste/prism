//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package common.iterable;

import java.util.Objects;
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

import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

/**
 * Abstract base class for Iterables that map elements using a function {@code f: S -> E}.
 * Yields the result of applying {@code f} to each each element of the underlying Iterable:
 * Iterable(S) -{@code f}-> Iterable(E)
 *
 * @param <S> type of the underlying Iterable's elements
 * @param <E> type of the Iterable's elements after mapping
 * @param <I> type of the underlying Iterable
 */
public abstract class MappingIterable<S, E, I extends Iterable<S>> implements FunctionalIterable<E>
{
	/** The Iterable which elements are mapped */
	protected final I iterable;

	/**
	 * Constructor for a mapping Iterable without a mapping function.
	 *
	 * @param iterable an Iterable to be mapped
	 */
	public MappingIterable(I iterable)
	{
		Objects.requireNonNull(iterable);
		this.iterable = iterable;
	}



	/**
	 * Generic implementation  using a function {@code f: S -> E}.
	 *
	 * @param <S> type of the underlying Iterable's elements
	 * @param <E> type of the Iterable's elements after mapping
	 */
	public static class From<S, E> extends MappingIterable<S, E, Iterable<S>>
	{
		/** The function the Iterable uses to map the elements */
		protected final Function<? super S, ? extends E> function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public From(Iterable<S> iterable, Function<? super S, ? extends E> function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new MappingIterator.ObjToObj<>(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: S -> double}.
	 *
	 * @param <S> type of the underlying Iterable's elements
	 */
	public static class ToDouble<S> extends MappingIterable<S, Double, Iterable<S>> implements IterableDouble
	{
		/** The function the Iterable uses to map the elements */
		protected final ToDoubleFunction<? super S> function;
	
		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public ToDouble(Iterable<S> iterable, ToDoubleFunction<? super S> function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}
	
		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new MappingIterator.ObjToDouble<>(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: S -> int}.
	 *
	 * @param <S> type of the underlying Iterable's elements
	 */
	public static class ToInt<S> extends MappingIterable<S, Integer, Iterable<S>> implements IterableInt
	{
		/** The function the Iterable uses to map the elements */
		protected final ToIntFunction<? super S> function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public ToInt(Iterable<S> iterable, ToIntFunction<? super S> function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new MappingIterator.ObjToInt<>(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: S -> long}.
	 *
	 * @param <S> type of the underlying Iterable's elements
	 */
	public static class ToLong<S> extends MappingIterable<S, Long, Iterable<S>> implements IterableLong
	{
		/** The function the Iterable uses to map the elements */
		protected final ToLongFunction<? super S> function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public ToLong(Iterable<S> iterable, ToLongFunction<? super S> function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new MappingIterator.ObjToLong<>(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: double -> E}.
	 *
	 * @param <E> type of the Iterable's elements after mapping
	 */
	public static class FromDouble<E> extends MappingIterable<Double, E, IterableDouble>
	{
		/** The function the Iterable uses to map the elements */
		protected final DoubleFunction<? extends E> function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public FromDouble(IterableDouble iterable, DoubleFunction<? extends E> function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new MappingIterator.DoubleToObj<>(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: double -> double}.
	 */
	public static class FromDoubleToDouble extends MappingIterable<Double, Double, IterableDouble> implements IterableDouble
	{
		/** The function the Iterable uses to map the elements */
		protected final DoubleUnaryOperator function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public FromDoubleToDouble(IterableDouble iterable, DoubleUnaryOperator function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new MappingIterator.DoubleToDouble(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: double -> int}.
	 */
	public static class FromDoubleToInt extends MappingIterable<Double, Integer, IterableDouble> implements IterableInt
	{
		/** The function the Iterable uses to map the elements */
		protected final DoubleToIntFunction function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public FromDoubleToInt(IterableDouble iterable, DoubleToIntFunction function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new MappingIterator.DoubleToInt(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: double -> long}.
	 */
	public static class FromDoubleToLong extends MappingIterable<Double, Long, IterableDouble> implements IterableLong
	{
		/** The function the Iterable uses to map the elements */
		protected final DoubleToLongFunction function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public FromDoubleToLong(IterableDouble iterable, DoubleToLongFunction function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new MappingIterator.DoubleToLong(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: int -> E}.
	 *
	 * @param <E> type of the Iterable's elements after mapping
	 */
	public static class FromInt<E> extends MappingIterable<Integer, E, IterableInt>
	{
		/** The function the Iterable uses to map the elements */
		protected final IntFunction<? extends E> function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public FromInt(IterableInt iterable, IntFunction<? extends E> function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new MappingIterator.IntToObj<E>(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: int -> double}.
	 */
	public static class FromIntToDouble extends MappingIterable<Integer, Double, IterableInt> implements IterableDouble
	{
		/** The function the Iterable uses to map the elements */
		protected final IntToDoubleFunction function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public FromIntToDouble(IterableInt iterable, IntToDoubleFunction function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new MappingIterator.IntToDouble(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: int -> int}.
	 */
	public static class FromIntToInt extends MappingIterable<Integer, Integer, IterableInt> implements IterableInt
	{
		/** The function the Iterable uses to map the elements */
		protected final IntUnaryOperator function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public FromIntToInt(IterableInt iterable, IntUnaryOperator function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new MappingIterator.IntToInt(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: int -> long}.
	 */
	public static class FromIntToLong extends MappingIterable<Integer, Long, IterableInt> implements IterableLong
	{
		/** The function the Iterable uses to map the elements */
		protected final IntToLongFunction function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public FromIntToLong(IterableInt iterable, IntToLongFunction function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new MappingIterator.IntToLong(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: long -> E}.
	 *
	 * @param <E> type of the Iterable's elements after mapping
	 */
	public static class FromLong<E> extends MappingIterable<Long, E, IterableLong>
	{
		/** The function the Iterable uses to map the elements */
		protected final LongFunction<? extends E> function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public FromLong(IterableLong iterable, LongFunction<? extends E> function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new MappingIterator.LongToObj<>(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: long -> double}.
	 */
	public static class FromLongToDouble extends MappingIterable<Long, Double, IterableLong> implements IterableDouble
	{
		/** The function the Iterable uses to map the elements */
		protected final LongToDoubleFunction function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public FromLongToDouble(IterableLong iterable, LongToDoubleFunction function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new MappingIterator.LongToDouble(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: long -> int}.
	 */
	public static class FromLongToInt extends MappingIterable<Long, Integer, IterableLong> implements IterableInt
	{
		/** The function the Iterable uses to map the elements */
		protected final LongToIntFunction function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public FromLongToInt(IterableLong iterable, LongToIntFunction function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new MappingIterator.LongToInt(iterable.iterator(), function);
		}
	}



	/**
	 * Primitive specialisation of a mapping Iterable using a function {@code f: long -> long}.
	 */
	public static class FromLongToLong extends MappingIterable<Long, Long, IterableLong> implements IterableLong
	{
		/** The function the Iterable uses to map the elements */
		protected final LongUnaryOperator function;

		/**
		 * Constructor for an Iterable that maps elements using a function.
		 *
		 * @param iterable an Iterable to be mapped
		 * @param function a function used to map the elements
		 */
		public FromLongToLong(IterableLong iterable, LongUnaryOperator function)
		{
			super(iterable);
			Objects.requireNonNull(function);
			this.function = function;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new MappingIterator.LongToLong(iterable.iterator(), function);
		}
	}
}