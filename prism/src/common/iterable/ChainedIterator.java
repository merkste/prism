//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

/**
 * A helper class implementing an Iterator that chains a sequence of iterators.
 * Returns all the elements of the first iterator, then the elements of the
 * second iterator and so on.
 * <p>
 * The calls to {@code next()} of the underlying iterator happen on-the-fly,
 * i.e., only when {@code next()} is called for this Iterator.
 * <p>
 * This iterator does not support the {@code remove()} method, even if the underlying
 * iterators support it.
 */

public abstract class ChainedIterator<T> implements Iterator<T>
{
	/** An iterator for the sequence of iterators that will be chained */
	protected Iterator<? extends Iterator<? extends T>> iterators;
	/** The current iterator in the sequence of iterators */
	protected Iterator<? extends T> current;

	/**
	 * Constructor for chaining a variable number of Iterators.
	 * @param iterators a variable number of Iterator to be chained */
	@SafeVarargs
	public ChainedIterator(Iterator<? extends T>... iterators)
	{
		this(Arrays.asList(iterators));
	}

	/**
	 * Constructor for chaining a sequence of Iterables.
	 * @param an Iterable over the sequence of Iterators to be chained
	 **/
	public ChainedIterator(Iterable<? extends Iterator<? extends T>> iterators)
	{
		this(iterators.iterator());
	}

	/**
	 * Constructor for chaining Iterator, with the sequence provided by an Iterators.
	 * @param iterators an Iterator the provides the sequence of Iterator to be chained
	 **/
	public ChainedIterator(Iterator<? extends Iterator<? extends T>> iterators)
	{
		this.iterators = iterators;
		current = iterators.hasNext() ? iterators.next() : EmptyIterator.Of();
	}

	@Override
	public boolean hasNext()
	{
		if (current.hasNext()) {
			// the current iterator has another element
			return true;
		}

		// the current iterator has no more elements,
		// search for the next iterator that as an element
		while (iterators.hasNext()) {
			// consider the next iterator
			current = iterators.next();
			if (current.hasNext()) {
				// iterator has element, keep current and return true
				return true;
			}
		}
		// there are no more iterators / elements
		// free resources
		iterators = EmptyIterator.Of();
		current = EmptyIterator.Of();
		return false;
	}

	protected void requireNext()
	{
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
	}

	public static class Of<T> extends ChainedIterator<T>
	{
		@SafeVarargs
		public Of(Iterator<? extends T>... iterators)
		{
			super(iterators);
		}

		public Of(Iterable<? extends Iterator<? extends T>> iterators)
		{
			super(iterators);
		}

		public Of(Iterator<? extends Iterator<? extends T>> iterators)
		{
			super(iterators);
		}

		@Override
		public T next()
		{
			requireNext();
			return current.next();
		}
	}

	public static class OfInt extends ChainedIterator<Integer> implements PrimitiveIterator.OfInt
	{
		@SafeVarargs
		public OfInt(PrimitiveIterator.OfInt... iterators)
		{
			super(iterators);
		}

		public OfInt(Iterable<? extends PrimitiveIterator.OfInt> iterators)
		{
			super(iterators);
		}

		public OfInt(Iterator<? extends PrimitiveIterator.OfInt> iterators)
		{
			super(iterators);
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return ((PrimitiveIterator.OfInt) current).nextInt();
		}
	}

	public static class OfLong extends ChainedIterator<Long> implements PrimitiveIterator.OfLong
	{
		@SafeVarargs
		public OfLong(PrimitiveIterator.OfLong... iterators)
		{
			super(iterators);
		}

		public OfLong(Iterable<? extends PrimitiveIterator.OfLong> iterators)
		{
			super(iterators);
		}

		public OfLong(Iterator<? extends PrimitiveIterator.OfLong> iterators)
		{
			super(iterators);
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return ((PrimitiveIterator.OfLong) current).nextLong();
		}
	}


	public static class OfDouble extends ChainedIterator<Double> implements PrimitiveIterator.OfDouble
	{
		@SafeVarargs
		public OfDouble(PrimitiveIterator.OfDouble... iterators)
		{
			super(iterators);
		}

		public OfDouble(Iterable<? extends PrimitiveIterator.OfDouble> iterators)
		{
			super(iterators);
		}

		public OfDouble(Iterator<? extends PrimitiveIterator.OfDouble> iterators)
		{
			super(iterators);
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			return ((PrimitiveIterator.OfDouble) current).nextDouble();
		}
	}
}