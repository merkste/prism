package explicit.modelviews;

import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.function.Function;

public class TransitionShift implements Function<Entry<Integer, Double>, Entry<Integer, Double>>
{
	private final int offset;

	public TransitionShift(final int offset)
	{
		this.offset = offset;
	}

	@Override
	public final Entry<Integer, Double> apply(Entry<Integer, Double> transition)
	{
		return new AbstractMap.SimpleImmutableEntry<>(transition.getKey() + offset, transition.getValue());
	}
}