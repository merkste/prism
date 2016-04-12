//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
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

package common;

import java.util.BitSet;
import java.util.LinkedList;

/**
 * A data structure for iterating over the indizes (e.g. state indizes)
 * of a BitSet, while additionally allowing additional indizes to be
 * remembered for iteration as well (stored in an extra queue).
 *<br>
 * The underlying BitSet should not change values during the iteration
 * and will not be changed by the iteration.
 *<br>
 * The rationale of this data structure is to provide an easy iteration
 * in a situation where initially a set of states is given by a BitSet
 * and during the course of iteration more states are discovered that
 * have to be iterated as well. Keeping these dynamically changing indizes
 * in the BitSet would lead to sub par performance, as nextSetBit for
 * the BitSet would have to be restarted from the 0 index all the time.
 */
public class BitSetAndQueue
{
	/** The underlying BitSet (not changed) */
	private BitSet bitset;
	/** The additional indizes that should be iterated */
	private LinkedList<Integer> queue;
	/** The index of the next set bit in the BitSet (-1 = no more indizes in the BitSet) */
	private int curBitSetIndex;

	/** Constructor. */
	public BitSetAndQueue(BitSet bitset)
	{
		this.bitset = bitset;
		this.queue = new LinkedList<Integer>();
		this.curBitSetIndex = bitset.nextSetBit(0);
	}

	/** Are there more indizes for iteration? */
	public boolean isEmpty()
	{
		if (curBitSetIndex >= 0)
			return false;
		return queue.isEmpty();
	}

	/** Get the next index and remove from further consideration */
	public int dequeue()
	{
		if (curBitSetIndex >= 0) {
			// there are more indizes in the BitSet
			int result = curBitSetIndex;
			// calculate next set bit for the next call, will be -1 when
			// there are no more
			curBitSetIndex = bitset.nextSetBit(result + 1);
			return result;
		}

		// all indizes in the BitSet have been "dequeued",
		// now we just remove and return the first element
		// of the list
		return queue.pollFirst();
	}

	/** Add an additional index for further consideration */
	public void enqueue(int i)
	{
		// add element to the queue
		queue.add(i);
	}
}

