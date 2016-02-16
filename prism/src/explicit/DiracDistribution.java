package explicit;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import common.iterable.SingletonIterator;

public class DiracDistribution implements Iterable<Entry<Integer, Double>>
{
	private final Entry<Integer, Double> transition;

	public DiracDistribution(final int state)
	{
		this.transition = new Transition(state);
	}

	public Iterator<Entry<Integer, Double>> iterator()
	{
		return new SingletonIterator.Of<>(transition);
	}

	public static Iterator<Entry<Integer, Double>> iterator(final int state)
	{
		return new SingletonIterator.Of<>((Entry<Integer, Double>) new Transition(state));
	}

	public static class Transition implements Map.Entry<Integer, Double>
	{
		private final Integer state;

		public Transition(final int state)
		{
			this.state = state;
		}

		@Override
		public Integer getKey()
		{
			return state;
		}

		@Override
		public Double getValue()
		{
			return 1.0;
		}

		@Override
		public Double setValue(final Double value)
		{
			throw new UnsupportedOperationException("immutable entry");
		}
	}
}