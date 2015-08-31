package explicit.conditional.transformer;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.functions.primitive.MappingInt;

public class ProbabilisticRedistribution implements MappingInt<List<Iterator<Entry<Integer, Double>>>>
{
	static final List<Iterator<Entry<Integer, Double>>> EMPTY_DISTRIBUTION = Collections.emptyList();

	private final double[] probabilitiesA;
	private final BitSet states;
	private final int stateA;
	private final int stateB;

	public ProbabilisticRedistribution(final BitSet states, final int stateA, final int stateB, final double[] probabilitiesA)
	{
		this.states = states;
		this.probabilitiesA = probabilitiesA;
		this.stateA = stateA;
		this.stateB = stateB;
	}

	@Override
	public List<Iterator<Entry<Integer, Double>>> apply(final int state)
	{
		if (states.get(state)) {
			return Collections.singletonList(probabilisticChoice(probabilitiesA[state]));
		}
		return EMPTY_DISTRIBUTION;
	}

	@SuppressWarnings("unchecked")
	public Iterator<Entry<Integer, Double>> probabilisticChoice(final double probabilityA)
	{
		Entry<Integer, Double>[] transitions;
		if (probabilityA == 1.0) {
			transitions = (Entry<Integer, Double>[]) new Entry[] {new AbstractMap.SimpleImmutableEntry<>(stateA, 1.0)};
		} else if (probabilityA == 0.0) {
			transitions = (Entry<Integer, Double>[]) new Entry[] {new AbstractMap.SimpleImmutableEntry<>(stateB, 1.0)};
		} else {
			transitions = (Entry<Integer, Double>[]) new Entry[] {new AbstractMap.SimpleImmutableEntry<>(stateA, probabilityA), new AbstractMap.SimpleImmutableEntry<>(stateB, 1.0 - probabilityA)};
		}
		return Arrays.stream(transitions).iterator();
	}
}