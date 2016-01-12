package explicit.conditional.transformer;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.functions.primitive.MappingInt;
import common.iterable.IterableArray;
import explicit.DiracDistribution;

public class ProbabilisticRedistribution implements MappingInt<List<Iterator<Entry<Integer, Double>>>>
{
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
		return null;
	}

	public Iterator<Entry<Integer, Double>> probabilisticChoice(final double probabilityA)
	{
		if (probabilityA == 1.0) {
			return DiracDistribution.iterator(stateA);
		}
		if (probabilityA == 0.0) {
			return DiracDistribution.iterator(stateB);
		}
		final Entry<Integer, Double> transitionA = new SimpleImmutableEntry<>(stateA, probabilityA);
		final Entry<Integer, Double> transitionB = new SimpleImmutableEntry<>(stateB, 1.0 - probabilityA);
		return new IterableArray.Of<>(transitionA, transitionB).iterator();
	}
}