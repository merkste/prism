//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
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

import java.util.BitSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import common.IterableBitSet;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;

/**
 * Abstract base class for Iterables that yield each distinct element (according to {@code equals}) exactly once.
 * The set of distinct elements is kept after the first iteration for future reuse.
 * <p>
 * Attention: Changes to the underlying Iterable are not reflected after the first iteration.
 * <p>
 * Implementations should release the underlying Iterable after the first iteration.
 * 
 * @param <E> type of the iterator elements
 * @param <I> type of the underlying Iterables
 */
public abstract class DistinctIterable<E, I extends FunctionalIterable<E>> implements FunctionalIterable<E>
{
	/** The original Iterable to be filtered */
	protected I source;
	/** An Iterable containing each element only once */ 
	protected I distinct;

	@SuppressWarnings("unchecked")
	public DistinctIterable(Iterable<E> source)
	{
		Objects.requireNonNull(source);
		this.source   = (I) FunctionalIterable.extend(source);
		this.distinct = null;
	}



	/**
	 * Generic implementation of a distinct Iterable using a {@code HashSet} to store distinct elements.
	 *
	 * @param <E> type of the Iterable's elements
	 */
	public static class Of<E> extends DistinctIterable<E, FunctionalIterable<E>>
	{
		/**
		 * Constructor for an Iterable that yields each distinct element exactly once.
		 * <p>
		 * Attention: Changes to the underlying Iterable are not reflected after the first iteration.
		 *
		 * @param iterable an Iterable to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		public Of(Iterable<E> source)
		{
			super(source);
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			if (source == null) {
				assert distinct != null : "Either source or distinct has to be non-null";
				return distinct.iterator();
			}
			Set<E> set                 = new HashSet<E>();
			Predicate<E> isFirst       = set::add;
			FunctionalIterable<E> iter = source.filter(isFirst);
			distinct                   = FunctionalIterable.extend(set);
			source                     = null;
			return iter.iterator();
		}
	}



	/**
	 * Primitive specialisation for {@code int} of a distinct Iterable that uses a {@code BitSet} to store distinct elements.
	 */
	public static class OfInt extends DistinctIterable<Integer, IterableInt> implements IterableInt
	{
		/**
		 * Constructor for an Iterable that yields each distinct integer exactly once.
		 * <p>
		 * Attention: Changes to the underlying Iterable are not reflected after the first iteration.
		 *
		 * @param iterable an Iterable to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		public OfInt(IterableInt source)
		{
			super(source);
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			if (source == null) {
				assert distinct != null : "Either source or distinct has to be non-null";
				return distinct.iterator();
			}
			BitSet bits          = new BitSet();
			IntPredicate isFirst = i -> {if (bits.get(i)) return false; else bits.set(i); return true;};
			IterableInt iter     = source.filter(isFirst);
			distinct             = IterableBitSet.getSetBits(bits);
			source               = null;
			return iter.iterator();
		}
	}
}
