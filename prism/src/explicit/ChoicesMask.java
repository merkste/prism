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

package explicit;

import java.util.BitSet;
import java.util.Iterator;


/**
 * A data structure storing, for a given NondeterministicModel,
 * which of the individual choices should be considered enabled
 * or disabled.
 * <br>
 * This is implemented similar to the sparse data structures:
 * A mask BitSet stores a bit where 'true' means that the corresponding
 * choice is disabled.
 * The indizes are calculated by means of an choiceStart array,
 * that stores the overall index of the first choice for each state.
 * Given a state s and choice index c, this allows the calculation of
 * the index i into the mask via i = choiceStart[s] + c. 
 */
public class ChoicesMask
{
	/** The mask bits (set = choice is disabled) */
	private BitSet mask;
	
	/** The overall index of the first choice (i.e., index 0) for the given state */
	private int[] choiceStart;

	/** The overall number of states */
	int numStates;

	/** Constructor. Initially, all choices are enabled. */
	public ChoicesMask(NondetModel model)
	{
		numStates = model.getNumStates();

		// initialise choiceStart array
		// plus one to allow storing the last choice
		// and provide an easy way to determine the end index
		// for a given state by looking at choiceStart[s+1]
		choiceStart = new int[numStates + 1];

		// store the global choice index for the first choice
		// of each state
		int curChoice = 0;
		for (int s = 0; s < numStates; s++) {
			choiceStart[s] = curChoice;
			curChoice += model.getNumChoices(s);
		}
		// store the last choice index at the end
		choiceStart[numStates] = curChoice;

		// initialise the mask to be empty, i.e, all choices are enabled
		mask = new BitSet();
	}

	/** Enable all choices */
	public void enableAll()
	{
		mask.clear();
	}

	/** Disable all choices */
	public void disableAll()
	{
		mask.set(0, getNumChoices());
	}
	
	private void setChoiceBit(int state, int choice, boolean value)
	{
		assert(state < numStates);
		int start = choiceStart[state];
		int end = choiceStart[state+1];
		assert(start + choice < end);

		mask.set(start + choice, value);
	}

	private boolean getChoiceBit(int state, int choice)
	{
		assert(state < numStates);
		int start = choiceStart[state];
		int end = choiceStart[state+1];
		assert(start + choice < end);

		return mask.get(start + choice);
	}

	/** Enable the given choice for the given state */
	public void enableChoice(int state, int choice)
	{
		// set to false => enabled
		setChoiceBit(state, choice, false);
	}

	/** Disable the given choice for the given state */
	public void disableChoice(int state, int choice)
	{
		// set to true => disabled
		setChoiceBit(state, choice, true);
	}

	/** Return true if the given choice in the given state is enabled */
	public boolean isEnabled(int state, int choice)
	{
		// set bit => disabled, so we negate
		return !getChoiceBit(state, choice);
	}

	/** Return the overall number of choices (enabled and disabled) */
	public int getNumChoices()
	{
		return choiceStart[numStates];
	}

	/** Return the number of choices for the given state (enabled and disabled) */
	public int getNumChoices(int state)
	{
		assert(state < numStates);
		int start = choiceStart[state];
		int end = choiceStart[state+1];

		return end - start;
	}

	/** Return the number of enabled choices for the given state */
	public int getNumEnabledChoices(int state)
	{
		int enabled = 0;
		for (@SuppressWarnings("unused") int c : enabledChoices(state)) {
			enabled++;
		}
		return enabled;
	}

	/** Return true if the given state has enabled choices */
	public boolean hasEnabledChoices(int state)
	{
		for (@SuppressWarnings("unused") int c : enabledChoices(state)) {
			return true;
		}
		return false;
	}

	/** Provide an Iterable over the indizes (local) that are enabled in the given state */
	public Iterable<Integer> enabledChoices(final int state)
	{
		final int start = choiceStart[state];
		final int end = choiceStart[state+1];

		return new Iterable<Integer>() {
			public Iterator<Integer> iterator() {
				return new Iterator<Integer>() {
					private int cur = start;

					@Override
					public boolean hasNext()
					{
						cur = mask.nextClearBit(cur);
						if (cur >= 0 && cur < end) {
							return true;
						} else {
							return false;
						}
					}

					@Override
					public Integer next()
					{
						cur = mask.nextClearBit(cur);
						if (cur >= 0 && cur < end) {
							int rv = cur - start;
							cur++;
							return rv;
						}
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
}
