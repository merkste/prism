package common.iterable.collections;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A set that chooses dynamically an appropriate class to store its data.
 * If there is a need to store just one element it makes use of <code>SingletonSet</code>.
 * If there is a need to store more elements it makes use of <code>BitSetWrapper</code>.
 * @author Marcus Daum (mdaum@tcs.inf.tu-dresden.de)
 */
public class AdaptiveSingletonSet implements Set<Integer>
{
	BitSetWrapper bitSet = null;
	SortedSingletonSet<Integer> singleton = null;

	public AdaptiveSingletonSet()
	{
		singleton = new SortedSingletonSet<>();
	}

	public AdaptiveSingletonSet(final BitSet aBitSet)
	{
		this((aBitSet == null) ? null : new BitSetWrapper(aBitSet));
	}

	public AdaptiveSingletonSet(final BitSetWrapper aBitSetWrapper)
	{
		if (aBitSetWrapper == null) {
			throw new NullPointerException("The argument for the constructor must not be null.");
		}
		if (aBitSetWrapper.size() > 1) {
			bitSet = aBitSetWrapper;
			return;
		}
		singleton = new SortedSingletonSet<>();
		singleton.addAll(aBitSetWrapper);
	}

	public AdaptiveSingletonSet(final SortedSingletonSet<Integer> aSingleton)
	{
		if (aSingleton == null) {
			throw new NullPointerException("The argument for the constructor must not be null.");
		}
		singleton = aSingleton;
	}

	public AdaptiveSingletonSet(final AdaptiveSingletonSet aSet)
	{
		if (aSet.singleton != null){
			singleton = new SortedSingletonSet<>();
			if (!aSet.singleton.isEmpty()){
				singleton.add(aSet.singleton.element);
			}
			return;
		}
		bitSet = aSet.bitSet.clone();
	}

	@Override
	public int size()
	{
		return (singleton == null) ? bitSet.size() : singleton.size();
	}

	@Override
	public boolean isEmpty()
	{
		return (singleton == null) ? bitSet.isEmpty() : singleton.isEmpty();
	}

	@Override
	public boolean contains(final Object o)
	{
		return (singleton == null) ? bitSet.contains(o) : singleton.contains(o);
	}

	@Override
	public Iterator<Integer> iterator()
	{
		return (singleton == null) ? bitSet.iterator() : singleton.iterator();
	}

	@Override
	public Object[] toArray()
	{
		return (singleton == null) ? bitSet.toArray() : singleton.toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a)
	{
		return (singleton == null) ? bitSet.toArray(a) : singleton.toArray(a);
	}

	private void convertSingletonToBitSet()
	{
		bitSet = new BitSetWrapper(new BitSet());
		if (!singleton.isEmpty()){
			bitSet.add(singleton.getElement());
		}
		singleton = null;
	}

	@Override
	public boolean add(final Integer i)
	{
		if (bitSet != null) {
			return bitSet.add(i);
		}
		try {
			return singleton.add(i);
		} catch (IllegalArgumentException e) {
			convertSingletonToBitSet();
			return bitSet.add(i);
		}
	}

	private void convertBitSetToSingleton()
	{
		convertBitSetToSingleton(bitSet);
	}

	private void convertBitSetToSingleton(BitSetWrapper aBitSet)
	{
		assert (aBitSet.size() < 2) : "Call only when there is at most one value in the BitSet.";
		singleton = new SortedSingletonSet<>();
		singleton.addAll(aBitSet);
		bitSet = null;
	}

	@Override
	public boolean remove(final Object o)
	{
		if (!(o instanceof Integer)) {
			return false;
		}
		if (singleton != null) {
			return singleton.remove(o);
		}
		final boolean modified = bitSet.remove(o);
		if (bitSet.size() < 2) {
			convertBitSetToSingleton();
		}
		return modified;
	}

	@Override
	public boolean containsAll(final Collection<?> c)
	{
		return (singleton == null) ? bitSet.containsAll(c) : singleton.containsAll(c);
	}

	@Override
	public boolean addAll(final Collection<? extends Integer> c)
	{
		if (bitSet != null) {
			return bitSet.addAll(c);
		}
		try {
			return singleton.addAll(c);
		} catch (IllegalArgumentException e) {
			convertSingletonToBitSet();
			return bitSet.addAll(c);
		}
	}

	@Override
	public boolean retainAll(final Collection<?> c)
	{
		if (singleton != null) {
			return singleton.retainAll(c);
		}
		final boolean modified = bitSet.retainAll(c);
		if (bitSet.size() < 2) {
			convertBitSetToSingleton();
		}
		return modified;
	}

	@Override
	public boolean removeAll(final Collection<?> c)
	{
		if (singleton != null) {
			return singleton.removeAll(c);
		}
		final boolean modified = bitSet.removeAll(c);
		if (bitSet.size() < 2){
			convertBitSetToSingleton();
		}
		return modified;
	}

	@Override
	public void clear()
	{
		singleton = new SortedSingletonSet<>();
		bitSet = null;
	}

	@Override
	public String toString()
	{
		return (singleton == null) ? bitSet.toString() : singleton.toString();
	}

	@Override
	public boolean equals(final Object o)
	{
		if (! (o instanceof AdaptiveSingletonSet)) {
			return false;
		}
		return (singleton == null) ? bitSet.equals(((AdaptiveSingletonSet) o).bitSet) : singleton.equals(((AdaptiveSingletonSet) o).singleton);
	}

	@Override
	public int hashCode()
	{
		return (singleton == null) ? bitSet.hashCode() : singleton.hashCode();
	}

	@Override
	public AdaptiveSingletonSet clone()
	{
		return new AdaptiveSingletonSet(this);
	}
}