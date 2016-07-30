package explicit.quantile.dataStructure.previousValues;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SpecificStates
{
	/**
	 * mapping from a <i>state of the model</i> to an <i>index in the array</i>
	 */
	private final Map<Integer, Integer> stateToArrayIndex;

	public SpecificStates(final BitSet states, final boolean considerParallelComputations)
	{
		if (considerParallelComputations){
			stateToArrayIndex = new ConcurrentHashMap<>(states.cardinality());
		} else {
			stateToArrayIndex = new HashMap<>(states.cardinality());
		}
		int index = 0;
		for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state+1)){
			stateToArrayIndex.put(state, index++);
		}
	}

	public int getIndexOfState(final int state)
	{
		return stateToArrayIndex.get(state);
	}

	public int getNumberOfStates()
	{
		return stateToArrayIndex.size();
	}

	public Set<Integer> getStates()
	{
		return stateToArrayIndex.keySet();
	}

	public String toString()
	{
		return stateToArrayIndex.toString();
	}
}