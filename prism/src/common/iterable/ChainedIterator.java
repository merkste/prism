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
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

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

public class ChainedIterator<T> implements Iterator<T>
{
	/** An iterator for the sequence of iterators that will be chained */
	private Iterator<? extends Iterator<? extends T>> iterators;
	/** The current iterator in the sequence of iterators */
	private Iterator<? extends T> current;

	/**
	 * Constructor for chaining a variable number of Iterator.
	 * @param iterators a variable number of Iterator to be chained */
	@SafeVarargs
	public ChainedIterator(final Iterator<? extends T>... iterators)
	{
		this(Arrays.asList(iterators));
	}

	/**
	 * Constructor for chaining a sequence of Iterable.
	 * @param an Iterable over the sequence of Iterators to be chained
	 **/
	public ChainedIterator(final Iterable<Iterator<? extends T>> iterators)
	{
		this(iterators.iterator());
	}

	/**
	 * Constructor for chaining Iterator, with the sequence provided by an Iterator.
	 * @param iterators an Iterator the provides the sequence of Iterator to be chained
	 **/
	public ChainedIterator(final Iterator<? extends Iterator<? extends T>> iterators)
	{
		this.iterators = iterators;
		current = iterators.hasNext() ? iterators.next() : Collections.<T> emptyIterator();
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
		iterators = Collections.emptyIterator();
		current = Collections.emptyIterator();
		return false;
	}

	@Override
	public T next()
	{
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		// return the next element
		return current.next();
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
}