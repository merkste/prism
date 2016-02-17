package common.iterable.collections;

import java.util.BitSet;
import java.util.Set;

public class SetFactory
{
	public enum SupportedImplementations {
		ADAPTIVE_SINGLETON_SET {
			@Override
			protected Set<Integer> getSet(BitSet set)
			{
				return new AdaptiveSingletonSet(set);
			}
			@Override
			protected Set<Integer> getSet()
			{
				return new AdaptiveSingletonSet();
			}
		},
		ADAPTIVE_SET {
			@Override
			protected Set<Integer> getSet(BitSet set)
			{
				return new AdaptiveSet(adaptiveSetThreshold, set);
			}
			@Override
			protected Set<Integer> getSet()
			{
				return new AdaptiveSet(adaptiveSetThreshold);
			}
		},
		ADAPTIVE_SET_USING_HASHSET {
			@Override
			protected Set<Integer> getSet(BitSet set)
			{
				return new AdaptiveSetUsingHashSet(adaptiveSetThreshold, set);
			}
			@Override
			protected Set<Integer> getSet()
			{
				return new AdaptiveSetUsingHashSet(adaptiveSetThreshold);
			}
		},
		BIT_SET {
			@Override
			protected Set<Integer> getSet(BitSet set)
			{
				return new BitSetWrapper(set);
			}
			@Override
			protected Set<Integer> getSet()
			{
				return new BitSetWrapper(new BitSet());
			}
		};
		protected int adaptiveSetThreshold;
		protected abstract Set<Integer> getSet(BitSet set);
		protected abstract Set<Integer> getSet();
	}
	
	private final SupportedImplementations implementation;
	
	public SetFactory(final SupportedImplementations chosenImplementation, final int adaptiveSetThreshold)
	{
		implementation = chosenImplementation;
		implementation.adaptiveSetThreshold = adaptiveSetThreshold;
	}
	
	public Set<Integer> getSet()
	{
		return implementation.getSet();
	}
	public Set<Integer> getSet(final BitSet states)
	{
		final int size = states.cardinality();
		//XXX: check which bound is best suited
		if (size < 100){
			if (size <= 1){
				return new AdaptiveSingletonSet(states);
			}
			return new AdaptiveSet(size, states);
		}
		return implementation.getSet(states);
	}
}