//==============================================================================
//	
//	Copyright (c) 2016-
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

package common.functions.primitive;

import java.util.Objects;

import common.functions.PairPredicate;

/**
 * Functional interface for a predicate (int, int) -> boolean.
 */
@FunctionalInterface
public interface PairPredicateInt extends PairPredicate<Integer, Integer>
{
	public static final PairPredicateInt TRUE  = (element1, element2) -> true;
	public static final PairPredicateInt FALSE = (element1, element2) -> false;

	public abstract boolean test(int element1, int element2);

	@Override
	default boolean test(Integer element1, Integer element2)
	{
		return test(element1.intValue(), element2.intValue());
	}

	@Override
	default PredicateInt curry(Integer element1)
	{
		return curry(element1.intValue());
	}

	default PredicateInt curry(int element1)
	{
		return element2 -> test(element1, element2);
	}

	@Override
	default PairPredicateInt negate()
	{
		return new PairPredicateInt()
		{
			@Override
			public boolean test(int element1, int element2)
			{
				return !PairPredicateInt.this.test(element1, element2);
			}

			@Override
			public PairPredicateInt negate()
			{
				return PairPredicateInt.this;
			}
		};
	}

	default PairPredicateInt and(PairPredicateInt predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> test(element1, element2) && predicate.test(element1, element2);
	}

	default PairPredicateInt or(PairPredicateInt predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> test(element1, element2) || predicate.test(element1, element2);
	}

	default PairPredicateInt implies(PairPredicateInt predicate)
	{
		Objects.requireNonNull(predicate);
		return (element1, element2) -> !test(element1, element2) || predicate.test(element1, element2);
	}

	/**
	 *  Overridden to ensure that the return type is PairPredicateDouble.
	 */
	@Override
	default PairPredicateInt inverse()
	{
		return (element1, element2) -> test(element2, element1);
	}
}